package com.docs_similarity.service;

import com.docs_similarity.dsa.CommonWordSequenceAnalyzer;
import com.docs_similarity.extraction.DocumentTextExtractor;
import com.docs_similarity.extraction.ExtractionException;
import com.docs_similarity.model.*;
import com.docs_similarity.util.TokenCombiner;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * DocumentSimilarityService
 * ------------------------------------------------------------------
 * Orchestrates the full pipeline:
 *
 *   Documents -> Extraction (TXT/PDF/DOCX) -> Normalization -> Word tokenization
 *   -> Token -> integer ID mapping -> Combined token sequence with unique
 *   document sentinels -> Suffix Array -> Kasai Algorithm -> LCP Array (in words)
 *   -> Common word sequences -> Overlap merging -> Similarity Scores -> Ranking
 *
 * The extraction step (see com.docs_similarity.extraction) converts
 * whatever format a document arrives in into plain text. From that point
 * on, the pipeline is completely format-agnostic.
 *
 * SIMILARITY FORMULA
 * -------------------
 * For a document pair (A, B), after merging overlapping matches
 * (CommonSequenceMerger):
 *
 *   sharedWordsInA = number of words of A inside at least one merged common
 *                    word sequence with B (union of intervals, so a word
 *                    covered by several overlapping matches counts once)
 *   sharedWordsInB = the same, computed within B
 *   sharedWords    = min(sharedWordsInA, sharedWordsInB)
 *
 *   Similarity(A, B) = (2 x sharedWords / (wordCountA + wordCountB)) x 100
 *
 * Raw LCP values are never summed: overlapping suffixes of one passage
 * ("machine learning algorithms", "learning algorithms", ...) would count
 * the same words again and again. Taking the smaller of the two sides means
 * a phrase that occurs once in A and three times in B contributes its words
 * once, not three times. The result is in [0, 100]: 100% when every word of
 * both documents is part of a shared sequence (identical documents), 0% when
 * no common sequence of at least MIN_SEQUENCE_WORDS words exists.
 */
@Service
public class DocumentSimilarityService {

    /** Minimum length (in words) for a common word sequence to count. */
    public static final int MIN_SEQUENCE_WORDS = 3;

    /** Maximum number of common word sequences shown per pair in the UI (longest first). */
    private static final int MAX_SEQUENCES_PER_PAIR = 15;

    /** Maximum number of Suffix Array / LCP rows returned for the technical inspection view. */
    private static final int MAX_SA_SAMPLE_ROWS = 300;

    /** Number of words shown in each suffix preview of the technical view. */
    private static final int SUFFIX_PREVIEW_WORDS = 6;

    private final DocumentTextExtractor textExtractor = new DocumentTextExtractor();

    public AnalysisResponse analyze(List<MultipartFile> files) throws IOException, ExtractionException {
        validateInput(files);

        List<String> stages = new ArrayList<>();

        // Stage 1: load documents
        stages.add("Documents loaded");

        // Stage 2: extract plain text from each document (TXT / PDF / DOCX),
        // then normalize and split it into words
        List<Document> documents = preprocess(files);
        stages.add("Text extracted from all documents (TXT/PDF/DOCX)");
        stages.add("Text normalized and tokenized into word IDs");

        // Stage 3-4: word IDs + sentinels -> Suffix Array -> Kasai -> LCP (all inside the analyzer)
        CommonWordSequenceAnalyzer analyzer = new CommonWordSequenceAnalyzer(documents, MIN_SEQUENCE_WORDS);
        stages.add("Suffix Array constructed");
        stages.add("LCP Array constructed");

        // Stage 5: common word sequences (suffix-array-derived matches)
        List<CommonWordSequence> rawMatches = analyzer.findRawMatches();
        stages.add("Common word sequences identified");

        // Stage 6: overlap merging + similarity scoring
        List<SimilarityResult> similarityResults =
                computeSimilarities(documents, rawMatches, analyzer.getCombinedTokens());
        stages.add("Similarity scores calculated");

        // Stage 7: ranking
        rank(similarityResults);
        stages.add("Documents ranked");

        // Build response payloads
        List<DocumentResult> documentResults = new ArrayList<>();
        for (Document d : documents) {
            documentResults.add(new DocumentResult(d.getId(), d.getFilename(), d.length(), d.wordCount(),
                    d.getSourceFormat(), "SUCCESS"));
        }

        double highest = similarityResults.stream()
                .mapToDouble(SimilarityResult::getSimilarityPercent)
                .max().orElse(0.0);

        int totalSequences = similarityResults.stream()
                .mapToInt(SimilarityResult::getTotalCommonSequences)
                .sum();

        AnalysisResponse.Summary summary = new AnalysisResponse.Summary(
                documents.size(),
                similarityResults.size(),
                totalSequences,
                round2(highest)
        );

        AlgorithmDetails algorithmDetails = buildAlgorithmDetails(documents, analyzer, totalSequences, similarityResults);

        return new AnalysisResponse(documentResults, similarityResults, summary, algorithmDetails, stages);
    }

    /**
     * Sorts by similarity, highest first. Ties are broken by shared words and
     * then by document IDs so the ranking is deterministic.
     */
    static void rank(List<SimilarityResult> results) {
        results.sort(Comparator.comparingDouble(SimilarityResult::getSimilarityPercent).reversed()
                .thenComparing(Comparator.comparingInt(SimilarityResult::getSharedWords).reversed())
                .thenComparingInt(SimilarityResult::getDocAId)
                .thenComparingInt(SimilarityResult::getDocBId));
        for (int i = 0; i < results.size(); i++) {
            results.get(i).setRank(i + 1);
        }
    }

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    private void validateInput(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException(
                    "No documents were provided. Please upload at least 2 documents (TXT, PDF, or DOCX).");
        }
        if (files.size() < 2) {
            throw new IllegalArgumentException("At least 2 documents are required for similarity analysis.");
        }
        for (MultipartFile f : files) {
            String name = f.getOriginalFilename();
            if (f.isEmpty()) {
                throw new IllegalArgumentException("Document '" + name + "' is empty.");
            }
        }
    }

    // ------------------------------------------------------------------
    // Extraction + preprocessing
    // ------------------------------------------------------------------

    /**
     * Extracts plain text from each uploaded file (dispatching to the TXT, PDF,
     * or DOCX extractor by extension) and applies light whitespace cleanup.
     * Format-specific concerns (unsupported extension, corrupt file,
     * scanned/image-only PDF with no text layer) are all resolved here, before
     * anything reaches the DSA pipeline. Each Document then tokenizes its text
     * into normalized words (WordTokenizer).
     */
    private List<Document> preprocess(List<MultipartFile> files) throws IOException, ExtractionException {
        List<Document> documents = new ArrayList<>();
        int id = 0;
        for (MultipartFile f : files) {
            String filename = f.getOriginalFilename();
            String extractedText = textExtractor.extract(f.getBytes(), filename);
            String sourceFormat = textExtractor.formatOf(filename);

            String cleaned = preprocessText(extractedText);
            if (cleaned.isEmpty()) {
                throw new IllegalArgumentException(
                        "Document '" + filename + "' has no usable text content after preprocessing.");
            }
            Document document = new Document(id, filename, cleaned, sourceFormat);
            if (document.wordCount() == 0) {
                throw new IllegalArgumentException(
                        "Document '" + filename + "' contains no words to compare (only punctuation or symbols).");
            }
            documents.add(document);
            id++;
        }
        return documents;
    }

    /**
     * Normalizes line endings and collapses runs of whitespace. Case,
     * punctuation and word splitting are handled by WordTokenizer.
     */
    private String preprocessText(String raw) {
        String normalized = raw.replace("\r\n", "\n").replace('\r', '\n');
        normalized = normalized.replaceAll("[ \\t]+", " ");
        normalized = normalized.replaceAll("\\n{3,}", "\n\n");
        return normalized.trim();
    }

    // ------------------------------------------------------------------
    // Similarity computation
    // ------------------------------------------------------------------

    private List<SimilarityResult> computeSimilarities(List<Document> documents, List<CommonWordSequence> rawMatches,
                                                       TokenCombiner.CombinedTokens combined) {
        Map<String, List<CommonWordSequence>> byPair = new LinkedHashMap<>();

        // Ensure every pair is present, even with zero matches (similarity 0%)
        for (int i = 0; i < documents.size(); i++) {
            for (int j = i + 1; j < documents.size(); j++) {
                byPair.put(pairKey(documents.get(i).getId(), documents.get(j).getId()), new ArrayList<>());
            }
        }
        for (CommonWordSequence m : rawMatches) {
            byPair.get(pairKey(m.getDocAId(), m.getDocBId())).add(m);
        }

        Map<Integer, Document> docById = new HashMap<>();
        for (Document d : documents) {
            docById.put(d.getId(), d);
        }

        List<SimilarityResult> results = new ArrayList<>();

        for (Map.Entry<String, List<CommonWordSequence>> entry : byPair.entrySet()) {
            String[] parts = entry.getKey().split("-");
            Document docA = docById.get(Integer.parseInt(parts[0]));
            Document docB = docById.get(Integer.parseInt(parts[1]));

            // Merge overlapping matches on the same alignment into maximal sequences.
            List<CommonWordSequence> merged = CommonSequenceMerger.mergeOnDiagonals(entry.getValue(), combined);

            // Count each shared word once: union of the merged intervals, per side.
            int sharedA = mergedCoverage(merged, true);
            int sharedB = mergedCoverage(merged, false);
            int sharedWords = Math.min(sharedA, sharedB);

            double similarity = similarityPercent(sharedWords, docA.wordCount(), docB.wordCount());

            List<CommonWordSequence> distinct = CommonSequenceMerger.dropContained(merged);
            List<CommonWordSequence> top = distinct.size() > MAX_SEQUENCES_PER_PAIR
                    ? new ArrayList<>(distinct.subList(0, MAX_SEQUENCES_PER_PAIR))
                    : distinct;

            results.add(new SimilarityResult(
                    docA.getId(), docB.getId(),
                    docA.getFilename(), docB.getFilename(),
                    round2(similarity),
                    docA.wordCount(), docB.wordCount(),
                    sharedA, sharedB, sharedWords,
                    distinct.size(),
                    top
            ));
        }

        return results;
    }

    /** Similarity(A,B) = (2 x sharedWords / (wordCountA + wordCountB)) x 100 */
    static double similarityPercent(int sharedWords, int wordCountA, int wordCountB) {
        int total = wordCountA + wordCountB;
        return total == 0 ? 0.0 : (2.0 * sharedWords / total) * 100.0;
    }

    /**
     * Merges the [offset, offset + wordCount) intervals of every sequence (in
     * either A's or B's word coordinates, per the useA flag) and returns the
     * number of distinct words covered. A word covered by several overlapping
     * sequences is counted once.
     */
    static int mergedCoverage(List<CommonWordSequence> sequences, boolean useA) {
        if (sequences.isEmpty()) return 0;

        List<int[]> intervals = new ArrayList<>();
        for (CommonWordSequence m : sequences) {
            int start = useA ? m.getOffsetInA() : m.getOffsetInB();
            intervals.add(new int[]{start, start + m.getWordCount()});
        }
        intervals.sort((a, b) -> Integer.compare(a[0], b[0]));

        int total = 0;
        int curStart = intervals.get(0)[0];
        int curEnd = intervals.get(0)[1];

        for (int i = 1; i < intervals.size(); i++) {
            int[] iv = intervals.get(i);
            if (iv[0] <= curEnd) {
                curEnd = Math.max(curEnd, iv[1]);
            } else {
                total += (curEnd - curStart);
                curStart = iv[0];
                curEnd = iv[1];
            }
        }
        total += (curEnd - curStart);
        return total;
    }

    private String pairKey(int a, int b) {
        int lo = Math.min(a, b);
        int hi = Math.max(a, b);
        return lo + "-" + hi;
    }

    // ------------------------------------------------------------------
    // Algorithm details / technical view
    // ------------------------------------------------------------------

    private AlgorithmDetails buildAlgorithmDetails(List<Document> documents, CommonWordSequenceAnalyzer analyzer,
                                                   int totalSequences, List<SimilarityResult> results) {
        int[] sa = analyzer.getSuffixArray().getSuffixArray();
        int[] lcp = analyzer.getKasaiLCP().getLCPArray();
        TokenCombiner.CombinedTokens combined = analyzer.getCombinedTokens();

        Map<Integer, String> nameById = new HashMap<>();
        for (Document d : documents) nameById.put(d.getId(), d.getFilename());

        List<SuffixArrayEntry> sample = new ArrayList<>();
        int limit = Math.min(sa.length, MAX_SA_SAMPLE_ROWS);
        for (int i = 0; i < limit; i++) {
            int pos = sa[i];
            int docId = combined.docIdAtPosition[pos];
            String docName = (docId == -1) ? "(boundary)" : nameById.get(docId);
            int offset = combined.offsetAtPosition[pos];

            // Word-based preview: the suffix's first words, rebuilt from token IDs,
            // stopping after the document's sentinel.
            StringBuilder preview = new StringBuilder();
            for (int p = pos; p < combined.length() && p < pos + SUFFIX_PREVIEW_WORDS; p++) {
                if (preview.length() > 0) preview.append(' ');
                preview.append(combined.wordAt(p));
                if (combined.isSentinel(p)) break;
            }

            sample.add(new SuffixArrayEntry(i, pos, docName, offset, lcp[i], preview.toString()));
        }

        return new AlgorithmDetails(
                documents.size(),
                combined.length(),
                combined.vocabulary.size(),
                true,
                true,
                true,
                analyzer.getRawSuffixMatchCount(),
                totalSequences,
                results.size(),
                MIN_SEQUENCE_WORDS,
                sa.length,
                sample
        );
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
