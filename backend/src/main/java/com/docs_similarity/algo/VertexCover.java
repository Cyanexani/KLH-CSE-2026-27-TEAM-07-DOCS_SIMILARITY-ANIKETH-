package com.docs_similarity.algo;

/**
 * Module 5: the classic 2-approximation for minimum vertex cover. Take both endpoints of
 * every edge of a greedily built maximal matching; the result is at most twice the optimum
 * because every cover must contain at least one endpoint of each matched edge.
 */
public final class VertexCover {

    private VertexCover() {
    }

    /** edges[i] = {u, v}. Returns the vertices of a cover of size at most 2 * OPT. */
    public static int[] approximate(int vertexCount, int[][] edges) {
        boolean[] inCover = new boolean[vertexCount];
        int size = 0;
        for (int[] e : edges) {
            if (!inCover[e[0]] && !inCover[e[1]]) {
                inCover[e[0]] = true;
                inCover[e[1]] = true;
                size += 2;
            }
        }
        int[] cover = new int[size];
        for (int v = 0, k = 0; v < vertexCount; v++) {
            if (inCover[v]) {
                cover[k++] = v;
            }
        }
        return cover;
    }
}
