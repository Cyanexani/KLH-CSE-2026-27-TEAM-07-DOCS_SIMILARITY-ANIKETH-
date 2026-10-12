package com.docs_similarity.algo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class AlgoToolkitTest {

    @Test
    void kmpFindsOverlappingMatches() {
        assertArrayEquals(new int[]{0, 2, 4}, Kmp.search("ababab", "ab"));
        assertArrayEquals(new int[]{0, 1, 2}, Kmp.search("aaaa", "aa"));
        assertArrayEquals(new int[]{0, 0, 0, 0, 1, 2, 3, 1, 2}, Kmp.failure("abcdabcab"));
    }

    @Test
    void editDistanceMatchesKnownValues() {
        assertEquals(3, EditDistance.levenshtein("kitten", "sitting"));
        assertEquals(0, EditDistance.levenshtein("", ""));
        assertEquals(4, EditDistance.levenshtein("", "abcd"));
    }

    @Test
    void flowAndMatching() {
        int[][] cap = new int[4][4];
        cap[0][1] = 3;
        cap[0][2] = 2;
        cap[1][3] = 2;
        cap[2][3] = 3;
        assertEquals(4, MaxFlow.maxFlow(cap, 0, 3));
        boolean[][] ok = {{true, true, false}, {true, false, false}, {false, true, true}};
        assertEquals(3, MaxFlow.bipartiteMatching(ok));
    }

    @Test
    void vertexCoverCoversEveryEdgeWithinFactorTwo() {
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 4}};
        int[] cover = VertexCover.approximate(5, edges);
        boolean[] in = new boolean[5];
        for (int v : cover) {
            in[v] = true;
        }
        for (int[] e : edges) {
            assertTrue(in[e[0]] || in[e[1]]);
        }
        assertTrue(cover.length <= 2 * 2); // optimum for the path on 5 vertices is 2
    }

    @Test
    void millerRabinHandlesCarmichaelAndLargePrimes() {
        assertFalse(Randomized.isPrime(561));
        assertTrue(Randomized.isPrime(2));
        assertTrue(Randomized.isPrime(1_000_000_007L));
        assertTrue(Randomized.isPrime(9_223_372_036_854_775_783L));
        assertFalse(Randomized.isPrime(1_000_000_007L * 3));
    }

    @Test
    void reservoirKeepsRequestedSizeOfStreamItems() {
        int[] stream = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        int[] sample = Randomized.reservoirSample(stream, 4, new Random(1));
        assertEquals(4, sample.length);
        for (int x : sample) {
            assertTrue(x >= 1 && x <= 10);
        }
    }
}
