package com.docs_similarity.algo;

import java.util.Arrays;

/**
 * Module 4: Edmonds-Karp max-flow (Ford-Fulkerson with BFS augmenting paths, O(V E^2))
 * on an adjacency-matrix residual graph, plus bipartite matching as a flow problem.
 */
public final class MaxFlow {

    private MaxFlow() {
    }

    /** Max flow from s to t. capacity[u][v] is the edge capacity; the array is not modified. */
    public static int maxFlow(int[][] capacity, int s, int t) {
        int n = capacity.length;
        int[][] residual = new int[n][];
        for (int i = 0; i < n; i++) {
            residual[i] = capacity[i].clone();
        }
        int flow = 0;
        int[] parent = new int[n];
        int[] queue = new int[n];
        while (true) {
            Arrays.fill(parent, -1);
            parent[s] = s;
            int head = 0;
            int tail = 0;
            queue[tail++] = s;
            while (head < tail && parent[t] == -1) {
                int u = queue[head++];
                for (int v = 0; v < n; v++) {
                    if (parent[v] == -1 && residual[u][v] > 0) {
                        parent[v] = u;
                        queue[tail++] = v;
                    }
                }
            }
            if (parent[t] == -1) {
                return flow;
            }
            int bottleneck = Integer.MAX_VALUE;
            for (int v = t; v != s; v = parent[v]) {
                bottleneck = Math.min(bottleneck, residual[parent[v]][v]);
            }
            for (int v = t; v != s; v = parent[v]) {
                residual[parent[v]][v] -= bottleneck;
                residual[v][parent[v]] += bottleneck;
            }
            flow += bottleneck;
        }
    }

    /**
     * Maximum bipartite matching size. allowed[l][r] says left node l may be paired with
     * right node r, for example "document l is the best source for passage r".
     */
    public static int bipartiteMatching(boolean[][] allowed) {
        int left = allowed.length;
        int right = left == 0 ? 0 : allowed[0].length;
        int n = left + right + 2;
        int s = n - 2;
        int t = n - 1;
        int[][] cap = new int[n][n];
        for (int l = 0; l < left; l++) {
            cap[s][l] = 1;
            for (int r = 0; r < right; r++) {
                if (allowed[l][r]) {
                    cap[l][left + r] = 1;
                }
            }
        }
        for (int r = 0; r < right; r++) {
            cap[left + r][t] = 1;
        }
        return maxFlow(cap, s, t);
    }
}
