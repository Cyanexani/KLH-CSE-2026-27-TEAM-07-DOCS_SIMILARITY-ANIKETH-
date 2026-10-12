package com.docs_similarity.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.docs_similarity.config.AppProperties;
import com.docs_similarity.dsa.CombinedText;
import com.docs_similarity.dsa.CommonSubstringAnalyzer;
import com.docs_similarity.dsa.KasaiLCP;
import com.docs_similarity.dsa.PairRanker;
import com.docs_similarity.dsa.SimilarityCalculator;
import com.docs_similarity.dsa.SimilarityCalculator.PairScore;
import com.docs_similarity.dsa.SuffixArray;
import com.docs_similarity.dsa.SuffixArrayVerifier;
import com.docs_similarity.exception.ApiException;
import com.docs_similarity.model.AlgorithmInfo;
import com.docs_similarity.model.AnalysisParameters;
import com.docs_similarity.model.AnalysisResponse;
import com.docs_similarity.model.AnalysisStage;
import com.docs_similarity.model.AnalysisSummary;
import com.docs_similarity.model.CommonSubstring;
import com.docs_similarity.model.DemoResponse;
import com.docs_similarity.model.DocumentInput;
import com.docs_similarity.model.DocumentResult;
import com.docs_similarity.model.SimilarityResult;
import com.docs_similarity.model.SuffixArrayPage;
import com.docs_similarity.model.SuffixArrayRow;
import com.docs_similarity.util.TextPreprocessor;
import com.docs_similarity.util.TextPreprocessor.ProcessedText;

/**
 * Runs the analysis pipeline:
 * <pre>
 *   documents -&gt; preprocessing -&gt; combined text -&gt; Suffix Array -&gt; Kasai LCP
 *             -&gt; common substrings -&gt; similarity scores -&gt; ranking
 * </pre>
 * Every step is timed and reported back as an {@link AnalysisStage}. The algorithms themselves
 * live in the {@code dsa} package, this class only connects them and builds the response.
 */
@Service
public class DocumentSimilarityService {

    static final String FORMULA =
            "Similarity(A,B) = (Shared(A,B) + Shared(B,A)) / (|A| + |B|) x 100%";

    private static final int SA_PREVIEW_SYMBOLS = 48;
    private static final int MAX_PAGE_SIZE = 200;
    private static final int MAX_DEMO_LENGTH = 64;

    private final AppProperties.Analysis settings;
    private final DocumentValidator validator;
    private final AnalysisStore store;

    public DocumentSimilarityService(AppProperties properties, DocumentValidator validator, AnalysisStore store) {
        this.settings = properties.analysis();
        this.validator = validator;
        this.store = store;
    }

    // =========================================================================================
    // Full analysis
    // =========================================================================================

    public AnalysisResponse analyze(List<DocumentInput> input, Integer requestedMinMatchLength) {
        final long analysisStart = System.nanoTime();
        final int minMatchLength = validator.resolveMinMatchLength(requestedMinMatchLength);
        final List<DocumentInput> documents = validator.validate(input);
        final int k = documents.size();
        final List<AnalysisStage> stages = new ArrayList<>();

        // 1. Documents loaded -------------------------------------------------------------------
        long t = System.nanoTime();
        long rawChars = documents.stream().mapToLong(d -> d.content().length()).sum();
        stages.add(stage("load", "Documents loaded", t,
                fmt("%d documents received, %,d characters in total", k, rawChars)));

        // 2. Preprocessing ----------------------------------------------------------------------
        t = System.nanoTime();
        List<ProcessedText> processed = new ArrayList<>(k);
        for (DocumentInput doc : documents) {
            ProcessedText p = TextPreprocessor.process(doc.content());
            if (p.text().isEmpty()) {
                throw ApiException.badRequest("Document '" + doc.name()
                        + "' contains no letters or digits, so there is no text to compare.");
            }
            processed.add(p);
        }
        long processedChars = processed.stream().mapToLong(p -> p.text().length()).sum();
        stages.add(stage("preprocess", "Text preprocessing completed", t,
                fmt("Lower case, punctuation removed, whitespace collapsed: %,d to %,d characters",
                        rawChars, processedChars)));

        // 3. Combined text with unique separators --------------------------------------------------
        t = System.nanoTime();
        CombinedText combined = CombinedText.of(processed.stream().map(ProcessedText::text).toList());
        final int n = combined.length();
        stages.add(stage("combine", "Combined text built", t,
                fmt("n = %,d symbols: %,d characters plus %d unique separators, alphabet size %d",
                        n, processedChars, k, combined.alphabetSize())));

        // 4. Suffix Array ------------------------------------------------------------------------
        t = System.nanoTime();
        SuffixArray suffixArray = SuffixArray.build(combined.symbols());
        int[] sa = suffixArray.getArray();
        stages.add(stage("suffix-array", "Suffix Array constructed", t,
                fmt("%,d suffixes sorted by prefix doubling in %d rounds", n, suffixArray.getDoublingRounds())));

        // 5. LCP Array with Kasai, followed by the consistency check ---------------------------------
        t = System.nanoTime();
        int[] lcp = KasaiLCP.compute(suffixArray);
        boolean verified = SuffixArrayVerifier.verify(combined.symbols(), sa, lcp);
        if (!verified) {
            throw new IllegalStateException("Suffix Array or LCP Array failed the consistency check");
        }
        int maxLcp = 0;
        long lcpSum = 0;
        int crossDocumentNeighbours = 0;
        for (int r = 0; r < n; r++) {
            maxLcp = Math.max(maxLcp, lcp[r]);
            lcpSum += lcp[r];
            if (r > 0 && lcp[r] >= minMatchLength
                    && combined.documentOf(sa[r]) != combined.documentOf(sa[r - 1])) {
                crossDocumentNeighbours++;
            }
        }
        stages.add(stage("lcp", "LCP Array constructed (Kasai)", t,
                fmt("Kasai algorithm, max LCP %,d. Suffix Array and LCP consistency check passed", maxLcp)));

        // 6. Common substrings -------------------------------------------------------------------
        t = System.nanoTime();
        CommonSubstringAnalyzer.Result common = new CommonSubstringAnalyzer(combined, sa, lcp)
                .analyze(minMatchLength, settings.maxSegmentsPerPair());
        stages.add(stage("common-substrings", "Common substrings identified", t,
                fmt("%,d maximal common segments of at least %d characters across %d pairs",
                        common.totalSegments(), minMatchLength, common.pairs().size())));

        // 7. Similarity --------------------------------------------------------------------------
        t = System.nanoTime();
        List<PairScore> scores = new ArrayList<>();
        for (CommonSubstringAnalyzer.PairMatches pair : common.pairs()) {
            scores.add(SimilarityCalculator.score(pair,
                    combined.documentLength(pair.documentA()), combined.documentLength(pair.documentB())));
        }
        stages.add(stage("similarity", "Similarity scores calculated", t,
                fmt("%d pairs scored with (Shared(A,B) + Shared(B,A)) / (|A| + |B|)", scores.size())));

        // 8. Ranking -----------------------------------------------------------------------------
        t = System.nanoTime();
        List<PairScore> ranked = PairRanker.rank(scores);
        PairScore best = ranked.get(0);
        stages.add(stage("ranking", "Documents ranked", t,
                fmt("%d pairs ordered by similarity, highest %.2f%%", ranked.size(), best.similarity() * 100)));

        // Keep the arrays for the paged Suffix Array view.
        String id = UUID.randomUUID().toString();
        List<String> names = documents.stream().map(DocumentInput::name).toList();
        store.save(new AnalysisStore.StoredAnalysis(id, combined, sa, lcp, names, minMatchLength));

        // Build the response ----------------------------------------------------------------------
        List<DocumentResult> documentResults = new ArrayList<>(k);
        for (int d = 0; d < k; d++) {
            ProcessedText p = processed.get(d);
            documentResults.add(new DocumentResult(d, names.get(d), documents.get(d).content().length(),
                    p.text().length(), p.wordCount(), p.lineCount(),
                    combined.documentStart(d), combined.separatorPosition(d)));
        }

        List<SimilarityResult> ranking = new ArrayList<>(ranked.size());
        double[][] matrix = new double[k][k];
        for (int d = 0; d < k; d++) {
            matrix[d][d] = 100.0;
        }
        for (int i = 0; i < ranked.size(); i++) {
            PairScore score = ranked.get(i);
            ranking.add(toSimilarityResult(i + 1, score, names, combined, processed));
            matrix[score.documentA()][score.documentB()] = score.similarity() * 100;
            matrix[score.documentB()][score.documentA()] = score.similarity() * 100;
        }

        double totalMs = (System.nanoTime() - analysisStart) / 1_000_000.0;
        AnalysisSummary summary = new AnalysisSummary(k, ranked.size(), common.totalSegments(),
                best.similarity() * 100, names.get(best.documentA()), names.get(best.documentB()), n, totalMs);

        AlgorithmInfo algorithm = new AlgorithmInfo(n, combined.alphabetSize(), k,
                "Prefix doubling with radix sort", "O(n log n)", suffixArray.getDoublingRounds(), verified,
                "Kasai", "O(n)", maxLcp, n == 0 ? 0 : (double) lcpSum / n, crossDocumentNeighbours, FORMULA);

        return new AnalysisResponse(id,
                new AnalysisParameters(minMatchLength, settings.maxSegmentsPerPair(), settings.segmentPreviewChars()),
                documentResults, summary, stages, ranking, matrix, algorithm);
    }

    private SimilarityResult toSimilarityResult(int rank, PairScore score, List<String> names,
                                                CombinedText combined, List<ProcessedText> processed) {
        CommonSubstringAnalyzer.PairMatches m = score.matches();
        int a = m.documentA();
        int b = m.documentB();
        List<CommonSubstring> segments = new ArrayList<>(m.topSegments().size());
        for (CommonSubstringAnalyzer.Segment s : m.topSegments()) {
            int shown = Math.min(s.length(), settings.segmentPreviewChars());
            String text = combined.text(combined.documentStart(a) + s.offsetA(), shown);
            segments.add(new CommonSubstring(text, s.length(), s.offsetA(), s.offsetB(),
                    processed.get(a).lineOf(s.offsetA()), processed.get(b).lineOf(s.offsetB()),
                    shown < s.length()));
        }
        return new SimilarityResult(rank, a, names.get(a), b, names.get(b),
                score.similarity() * 100, m.sharedCharsA(), m.sharedCharsB(),
                score.lengthA(), score.lengthB(), score.coverageA() * 100, score.coverageB() * 100,
                m.effectiveMinLength(), m.segmentCount(), m.longestSegment(), segments);
    }

    // =========================================================================================
    // Paged Suffix Array and LCP table
    // =========================================================================================

    public SuffixArrayPage suffixArrayPage(String analysisId, int page, int pageSize) {
        AnalysisStore.StoredAnalysis stored = store.find(analysisId).orElseThrow(() -> ApiException.notFound(
                "Analysis '" + analysisId + "' was not found. It may have expired, please run the analysis again."));
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw ApiException.badRequest("Page size must be between 1 and " + MAX_PAGE_SIZE + ".");
        }
        CombinedText text = stored.text();
        int[] sa = stored.suffixArray();
        int[] lcp = stored.lcp();
        int n = sa.length;
        int totalPages = (n + pageSize - 1) / pageSize;
        if (page < 0 || page >= totalPages) {
            throw ApiException.badRequest("Page must be between 0 and " + (totalPages - 1) + ".");
        }

        List<SuffixArrayRow> rows = new ArrayList<>(pageSize);
        for (int r = page * pageSize; r < Math.min(n, (page + 1) * pageSize); r++) {
            int pos = sa[r];
            int doc = text.documentOf(pos);
            // The common prefix never contains a separator (separators are unique), so it is plain text.
            int prefixLength = Math.min(lcp[r], SA_PREVIEW_SYMBOLS);
            String commonPrefix = text.text(pos, prefixLength);
            String rest = text.preview(pos + prefixLength, SA_PREVIEW_SYMBOLS - prefixLength);
            boolean cross = r > 0 && lcp[r] >= stored.minMatchLength()
                    && text.documentOf(sa[r - 1]) != doc;
            rows.add(new SuffixArrayRow(r, pos, doc, stored.documentNames().get(doc),
                    text.offsetInDocument(pos), text.isSeparator(pos), lcp[r], commonPrefix, rest, cross));
        }
        return new SuffixArrayPage(analysisId, page, pageSize, n, totalPages, stored.minMatchLength(), rows);
    }

    // =========================================================================================
    // Suffix Array playground for a short string
    // =========================================================================================

    public DemoResponse demo(String text) {
        if (text == null || text.isEmpty()) {
            throw ApiException.badRequest("Enter a string to build its Suffix Array, for example 'banana'.");
        }
        if (text.length() > MAX_DEMO_LENGTH) {
            throw ApiException.badRequest("The playground accepts at most " + MAX_DEMO_LENGTH + " characters.");
        }
        for (int i = 0; i < text.length(); i++) {
            if (Character.isISOControl(text.charAt(i))) {
                throw ApiException.badRequest("The playground string must not contain control characters.");
            }
        }
        SuffixArray suffixArray = SuffixArray.fromString(text);
        int[] sa = suffixArray.getArray();
        int[] lcp = KasaiLCP.compute(suffixArray);
        List<DemoResponse.Row> rows = new ArrayList<>(sa.length);
        for (int r = 0; r < sa.length; r++) {
            rows.add(new DemoResponse.Row(r, sa[r], text.substring(sa[r]), lcp[r]));
        }
        return new DemoResponse(text, text.length(), suffixArray.getDoublingRounds(), sa, lcp, rows);
    }

    // =========================================================================================

    private static AnalysisStage stage(String key, String name, long startNanos, String detail) {
        double ms = (System.nanoTime() - startNanos) / 1_000_000.0;
        return new AnalysisStage(key, name, "completed", ms, detail);
    }

    private static String fmt(String pattern, Object... args) {
        return String.format(Locale.US, pattern, args);
    }
}
