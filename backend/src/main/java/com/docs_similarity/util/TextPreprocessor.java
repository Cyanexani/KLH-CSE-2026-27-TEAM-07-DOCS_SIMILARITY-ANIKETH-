package com.docs_similarity.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Normalizes a raw document before it enters the Suffix Array.
 *
 * <p>Rules, applied character by character:
 * <ol>
 *   <li>Letters and digits are kept and converted to lower case.</li>
 *   <li>An apostrophe between two letters is removed ("don't" becomes "dont").</li>
 *   <li>Every other character (punctuation, symbols, line breaks, tabs) acts as a word gap.</li>
 *   <li>A run of word gaps becomes a single space, and leading or trailing gaps are dropped.</li>
 * </ol>
 * Result: lower case words separated by single spaces. Changes in case, punctuation, line wrapping
 * or spacing therefore do not hide copied text.
 *
 * <p>For every character of the normalized text the index of the character it came from in the
 * original text is stored, so results can be reported with original line numbers.
 */
public final class TextPreprocessor {

    private TextPreprocessor() {
    }

    public static ProcessedText process(String raw) {
        StringBuilder normalized = new StringBuilder(raw.length());
        int[] toOriginal = new int[raw.length()];
        int written = 0;
        int pendingGapAt = -1; // original index where the current gap started, -1 if none

        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                if (pendingGapAt >= 0 && written > 0) {
                    normalized.append(' ');
                    toOriginal[written++] = pendingGapAt;
                }
                pendingGapAt = -1;
                normalized.append(Character.toLowerCase(c));
                toOriginal[written++] = i;
            } else if (isApostrophe(c) && i > 0 && i + 1 < raw.length()
                    && Character.isLetter(raw.charAt(i - 1)) && Character.isLetter(raw.charAt(i + 1))) {
                // skip: joins the two halves of a contraction
            } else if (pendingGapAt < 0) {
                pendingGapAt = i;
            }
        }

        int[] mapping = new int[written];
        System.arraycopy(toOriginal, 0, mapping, 0, written);
        return new ProcessedText(normalized.toString(), mapping, lineStarts(raw));
    }

    private static boolean isApostrophe(char c) {
        return c == '\'' || c == '’';
    }

    /** Start index of every line in the original text (handles \n, \r\n and \r). */
    private static int[] lineStarts(String raw) {
        List<Integer> starts = new ArrayList<>();
        starts.add(0);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\n' || (c == '\r' && (i + 1 >= raw.length() || raw.charAt(i + 1) != '\n'))) {
                starts.add(i + 1);
            }
        }
        return starts.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * A preprocessed document.
     *
     * @param text               normalized text used by the algorithms
     * @param normalizedToOriginal for every character of {@code text}, its index in the original text
     * @param lineStarts         start index of every line of the original text
     */
    public record ProcessedText(String text, int[] normalizedToOriginal, int[] lineStarts) {

        public int lineCount() {
            return lineStarts.length;
        }

        public int wordCount() {
            return text.isEmpty() ? 0 : (int) text.chars().filter(ch -> ch == ' ').count() + 1;
        }

        /** 1-based line number (in the original file) of a character of the normalized text. */
        public int lineOf(int normalizedOffset) {
            int original = normalizedToOriginal[normalizedOffset];
            int lo = 0;
            int hi = lineStarts.length - 1;
            while (lo < hi) {                       // binary search: last line start <= original
                int mid = (lo + hi + 1) >>> 1;
                if (lineStarts[mid] <= original) {
                    lo = mid;
                } else {
                    hi = mid - 1;
                }
            }
            return lo + 1;
        }
    }
}
