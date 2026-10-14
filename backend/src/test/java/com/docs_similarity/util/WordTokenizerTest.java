package com.docs_similarity.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WordTokenizerTest {

    @Test
    void lowercasesAndStripsPunctuation() {
        assertEquals(List.of("machine", "learning", "algorithms", "work"),
                WordTokenizer.tokenize("Machine-Learning ALGORITHMS... work!"));
    }

    @Test
    void normalizesWhitespace() {
        assertEquals(List.of("one", "two", "three"),
                WordTokenizer.tokenize("  one\t\ttwo \r\n\n  three  "));
    }

    @Test
    void keepsContractionsAsOneWord() {
        assertEquals(List.of("don", "t", "vs", "dont"), WordTokenizer.tokenize("'don' 't' vs don't"));
        assertEquals(WordTokenizer.tokenize("The company's plan"), WordTokenizer.tokenize("THE COMPANY’S PLAN"));
    }

    @Test
    void keepsNumbersAndAccentedLetters() {
        assertEquals(List.of("café", "opened", "in", "2024"), WordTokenizer.tokenize("Café opened in 2024."));
    }

    @Test
    void unicodeLigaturesAreNormalized() {
        // PDF text often contains the single "ﬁ" ligature character
        assertEquals(List.of("financial"), WordTokenizer.tokenize("ﬁnancial"));
    }

    @Test
    void punctuationOnlyTextHasNoWords() {
        assertTrue(WordTokenizer.tokenize(" ... --- !!! ").isEmpty());
    }

    @Test
    void differentPunctuationAndCaseGiveIdenticalTokens() {
        assertEquals(WordTokenizer.tokenize("The Quick, Brown Fox!"),
                WordTokenizer.tokenize("the quick brown fox"));
    }
}
