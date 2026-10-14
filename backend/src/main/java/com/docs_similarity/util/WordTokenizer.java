package com.docs_similarity.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic word tokenizer used before the Suffix Array is built.
 *
 * Normalization, in order:
 *  1. Unicode NFKC normalization (so e.g. the PDF ligature "ﬁ" becomes "fi"
 *     and full-width characters become their plain forms).
 *  2. Lower-casing with Locale.ROOT, so "Machine" and "machine" are the same
 *     word regardless of the server's locale.
 *  3. Apostrophes INSIDE a word are dropped ("don't" / "don’t" -&gt; "dont"),
 *     so contractions and possessives stay one word.
 *  4. A word is a maximal run of letters, combining marks and digits. Every
 *     other character (whitespace, punctuation, symbols, hyphens, dashes,
 *     quotes) is a separator. Runs of separators never produce empty words,
 *     which also normalizes all whitespace.
 *
 * The same input always yields the same list of words, and nothing here
 * depends on other documents, so tokenization is independent of upload order.
 */
public final class WordTokenizer {

    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{M}\\p{N}]+");
    private static final Pattern INNER_APOSTROPHE =
            Pattern.compile("(?<=[\\p{L}\\p{M}\\p{N}])['’‘ʼ`´](?=[\\p{L}\\p{N}])");

    private WordTokenizer() {
    }

    /** Applies steps 1-3 above: the normalized text the words are cut from. */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFKC);
        normalized = normalized.toLowerCase(Locale.ROOT);
        return INNER_APOSTROPHE.matcher(normalized).replaceAll("");
    }

    /** Splits text into normalized words, in document order. */
    public static List<String> tokenize(String raw) {
        List<String> words = new ArrayList<>();
        Matcher m = WORD.matcher(normalize(raw));
        while (m.find()) {
            words.add(m.group());
        }
        return words;
    }
}
