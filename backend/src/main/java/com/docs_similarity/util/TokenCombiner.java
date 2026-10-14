package com.docs_similarity.util;

import com.docs_similarity.model.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Combines the WORDS of multiple documents into a single integer token
 * sequence for Suffix Array construction, while guaranteeing that no
 * suffix match can silently cross from the end of one document into the
 * start of another.
 *
 * TOKEN IDS
 * Every distinct normalized word (see WordTokenizer) gets one integer ID,
 * assigned in order of first appearance: 0, 1, 2, ... The same word
 * always maps to the same ID in every document, so equal word sequences
 * become equal ID sequences. The reverse mapping ID -&gt; word is kept in
 * {@link CombinedTokens#vocabulary} so any matched ID run can be turned
 * back into readable words.
 *
 * DOCUMENT BOUNDARIES
 * Each document is followed by a UNIQUE sentinel token, -(docId + 1).
 * Sentinels are negative, so they can never equal a word ID (which are
 * &gt;= 0), and no two documents share a sentinel. Any suffix comparison
 * that would need to "cross" a document boundary is therefore forced to
 * compare two DIFFERENT tokens at that position, which immediately ends
 * the common prefix. This makes cross-boundary matches structurally
 * impossible without special-case logic in the Suffix Array or Kasai.
 * (Being negative, sentinels also sort before every word, like the
 * textbook "$" terminator.)
 *
 * Alongside the token sequence, this class produces a position -&gt; document
 * lookup so that any position in the combined sequence can be mapped back
 * to (documentId, wordOffsetWithinThatDocument).
 */
public class TokenCombiner {

    private TokenCombiner() {
    }

    public static class CombinedTokens {
        public final int[] tokens;             // word IDs, with one sentinel after each document
        public final int[] docIdAtPosition;    // which document position i belongs to (-1 for sentinel)
        public final int[] offsetAtPosition;   // word offset within that document (-1 for sentinel)
        public final int[] documentStart;      // documentStart[docId] = position of that document's first word
        public final List<String> vocabulary;  // reverse mapping: vocabulary.get(id) = normalized word

        CombinedTokens(int[] tokens, int[] docIdAtPosition, int[] offsetAtPosition,
                       int[] documentStart, List<String> vocabulary) {
            this.tokens = tokens;
            this.docIdAtPosition = docIdAtPosition;
            this.offsetAtPosition = offsetAtPosition;
            this.documentStart = documentStart;
            this.vocabulary = vocabulary;
        }

        public int length() {
            return tokens.length;
        }

        public boolean isSentinel(int position) {
            return tokens[position] < 0;
        }

        /** The normalized word at a position (reverse token-ID mapping), or a marker for a boundary. */
        public String wordAt(int position) {
            int id = tokens[position];
            return id < 0 ? SENTINEL_LABEL : vocabulary.get(id);
        }

        /** The words [wordOffset, wordOffset + wordCount) of one document, rebuilt from token IDs. */
        public String wordsOf(int docId, int wordOffset, int wordCount) {
            StringBuilder sb = new StringBuilder();
            int start = documentStart[docId] + wordOffset;
            for (int p = start; p < start + wordCount; p++) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(wordAt(p));
            }
            return sb.toString();
        }
    }

    /** How a document boundary is shown in word previews. */
    public static final String SENTINEL_LABEL = "⟨END⟩";

    public static int sentinelFor(int docId) {
        return -(docId + 1);
    }

    public static CombinedTokens combine(List<Document> documents) {
        int total = 0;
        for (Document doc : documents) {
            total += doc.wordCount() + 1;
        }

        int[] tokens = new int[total];
        int[] docIdAt = new int[total];
        int[] offsetAt = new int[total];
        int maxDocId = documents.stream().mapToInt(Document::getId).max().orElse(-1);
        int[] documentStart = new int[maxDocId + 1];

        Map<String, Integer> idByWord = new HashMap<>();
        List<String> vocabulary = new ArrayList<>();

        int pos = 0;
        for (Document doc : documents) {
            documentStart[doc.getId()] = pos;
            List<String> words = doc.getWords();
            for (int k = 0; k < words.size(); k++) {
                String word = words.get(k);
                Integer id = idByWord.get(word);
                if (id == null) {
                    id = vocabulary.size();
                    idByWord.put(word, id);
                    vocabulary.add(word);
                }
                tokens[pos] = id;
                docIdAt[pos] = doc.getId();
                offsetAt[pos] = k;
                pos++;
            }
            // unique sentinel position
            tokens[pos] = sentinelFor(doc.getId());
            docIdAt[pos] = -1;
            offsetAt[pos] = -1;
            pos++;
        }

        return new CombinedTokens(tokens, docIdAt, offsetAt, documentStart,
                Collections.unmodifiableList(vocabulary));
    }
}
