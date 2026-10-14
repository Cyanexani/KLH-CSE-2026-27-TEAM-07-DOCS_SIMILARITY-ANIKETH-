package com.docs_similarity.dsa;

import com.docs_similarity.model.CommonWordSequence;
import com.docs_similarity.model.Document;
import com.docs_similarity.service.CommonSequenceMerger;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class CommonWordSequenceAnalyzerTest {

    /** Distinct maximal sequences, merged separately for each document pair (as the service does). */
    private static List<CommonWordSequence> distinct(CommonWordSequenceAnalyzer analyzer) {
        Map<String, List<CommonWordSequence>> byPair = new TreeMap<>();
        for (CommonWordSequence m : analyzer.findRawMatches()) {
            byPair.computeIfAbsent(m.getDocAId() + "-" + m.getDocBId(), k -> new ArrayList<>()).add(m);
        }
        List<CommonWordSequence> out = new ArrayList<>();
        for (List<CommonWordSequence> pairMatches : byPair.values()) {
            out.addAll(CommonSequenceMerger.distinctMaximalSequences(pairMatches, analyzer.getCombinedTokens()));
        }
        return out;
    }

    @Test
    void findsSharedWordSequenceWithWordOffsets() {
        Document d0 = new Document(0, "doc1.txt", "The quick brown fox jumps over the lazy dog");
        Document d1 = new Document(1, "doc2.txt", "A completely different sentence about the quick brown fox running");

        CommonWordSequenceAnalyzer analyzer = new CommonWordSequenceAnalyzer(List.of(d0, d1), 3);
        List<CommonWordSequence> sequences = distinct(analyzer);

        assertEquals(1, sequences.size());
        CommonWordSequence s = sequences.get(0);
        assertEquals("the quick brown fox", s.getText());
        assertEquals(4, s.getWordCount());
        assertEquals(0, s.getOffsetInA(), "word offset in doc1");
        assertEquals(5, s.getOffsetInB(), "word offset in doc2");
    }

    @Test
    void identicalDocumentsProduceOneFullLengthSequence() {
        String text = "this is an identical piece of text used for testing purposes";
        Document d0 = new Document(0, "doc1.txt", text);
        Document d1 = new Document(1, "doc2.txt", text);

        List<CommonWordSequence> sequences = distinct(new CommonWordSequenceAnalyzer(List.of(d0, d1), 3));

        assertEquals(1, sequences.size());
        assertEquals(11, sequences.get(0).getWordCount());
        assertEquals(text, sequences.get(0).getText());
    }

    @Test
    void unrelatedDocumentsProduceNoMatches() {
        Document d0 = new Document(0, "doc1.txt", "stock markets rallied after the central bank announcement");
        Document d1 = new Document(1, "doc2.txt", "local football club wins regional championship final");

        CommonWordSequenceAnalyzer analyzer = new CommonWordSequenceAnalyzer(List.of(d0, d1), 3);
        assertTrue(analyzer.findRawMatches().isEmpty());
    }

    @Test
    void characterFragmentsAreNotWordMatches() {
        // At character level "environment" and "nvironment" share 10 characters;
        // at word level they are different words and nothing is shared.
        Document d0 = new Document(0, "doc1.txt", "environment environment environment");
        Document d1 = new Document(1, "doc2.txt", "nvironment vironment ironment");

        CommonWordSequenceAnalyzer analyzer = new CommonWordSequenceAnalyzer(List.of(d0, d1), 1);
        assertTrue(analyzer.findRawMatches().isEmpty());
    }

    @Test
    void partiallySimilarDocumentsShareOnlyTheCommonPassage() {
        Document d0 = new Document(0, "doc1.txt",
                "Researchers said machine learning algorithms can detect fraud in banking data quickly");
        Document d1 = new Document(1, "doc2.txt",
                "The report argues that machine learning algorithms can detect fraud but need oversight");

        List<CommonWordSequence> sequences = distinct(new CommonWordSequenceAnalyzer(List.of(d0, d1), 3));

        assertEquals(1, sequences.size());
        assertEquals("machine learning algorithms can detect fraud", sequences.get(0).getText());
        assertEquals(6, sequences.get(0).getWordCount());
        assertEquals(2, sequences.get(0).getOffsetInA());
        assertEquals(4, sequences.get(0).getOffsetInB());
    }

    @Test
    void punctuationAndCaseDoNotPreventAMatch() {
        Document d0 = new Document(0, "doc1.txt", "Climate change, according to SCIENTISTS, is accelerating.");
        Document d1 = new Document(1, "doc2.txt", "climate change according to scientists is accelerating");

        List<CommonWordSequence> sequences = distinct(new CommonWordSequenceAnalyzer(List.of(d0, d1), 3));

        assertEquals(1, sequences.size());
        assertEquals("climate change according to scientists is accelerating", sequences.get(0).getText());
    }

    @Test
    void matchesNeverCrossDocumentBoundaries() {
        // Concatenated WITHOUT boundaries, doc1 + doc2 would read "... red green | blue sky today",
        // which contains doc3's full text "red green blue sky". The unique sentinel after doc1
        // must stop that: doc1 and doc3 share only "red green".
        Document d0 = new Document(0, "doc1.txt", "alpha beta red green");
        Document d1 = new Document(1, "doc2.txt", "blue sky today");
        Document d2 = new Document(2, "doc3.txt", "red green blue sky");

        CommonWordSequenceAnalyzer analyzer = new CommonWordSequenceAnalyzer(List.of(d0, d1, d2), 2);
        List<CommonWordSequence> sequences = distinct(analyzer);

        CommonWordSequence doc1doc3 = sequences.stream()
                .filter(s -> s.getDocAId() == 0 && s.getDocBId() == 2).findFirst().orElseThrow();
        assertEquals("red green", doc1doc3.getText());
        assertEquals(2, doc1doc3.getWordCount());

        int longest = sequences.stream().mapToInt(CommonWordSequence::getWordCount).max().orElse(0);
        assertEquals(2, longest, "No match may extend across a document boundary");
        for (CommonWordSequence s : sequences) {
            assertFalse(s.getText().contains("⟨END⟩"), "A sentinel can never be part of a match");
        }
    }

    @Test
    void threeIdenticalDocumentsMatchInEveryPair() {
        // With three copies, a suffix of doc3 does not sort next to doc1's suffix;
        // the nearest-suffix sweep must still find the doc1 <-> doc3 match.
        String text = "suffix arrays make repeated passages easy to find";
        List<Document> docs = List.of(new Document(0, "a.txt", text),
                new Document(1, "b.txt", text), new Document(2, "c.txt", text));

        List<CommonWordSequence> raw = new CommonWordSequenceAnalyzer(docs, 3).findRawMatches();
        for (int[] pair : new int[][]{{0, 1}, {0, 2}, {1, 2}}) {
            int longest = raw.stream()
                    .filter(m -> m.getDocAId() == pair[0] && m.getDocBId() == pair[1])
                    .mapToInt(CommonWordSequence::getWordCount).max().orElse(0);
            assertEquals(8, longest, "pair " + pair[0] + "-" + pair[1]);
        }
    }

    @Test
    void repeatedWordsAreHandledWithoutDuplicates() {
        Document d0 = new Document(0, "doc1.txt", "the the the the the");
        Document d1 = new Document(1, "doc2.txt", "the the the");

        List<CommonWordSequence> sequences = distinct(new CommonWordSequenceAnalyzer(List.of(d0, d1), 3));

        assertEquals(1, sequences.size(), "The repeated phrase is shown once, not once per alignment");
        assertEquals("the the the", sequences.get(0).getText());
    }
}
