package com.docs_similarity.algo;

import java.math.BigInteger;
import java.util.Random;

/** Module 6: Miller-Rabin primality (Monte Carlo family) and reservoir sampling (streaming). */
public final class Randomized {

    private static final long[] WITNESSES = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37};

    private Randomized() {
    }

    private static long mulMod(long a, long b, long m) {
        return BigInteger.valueOf(a).multiply(BigInteger.valueOf(b)).mod(BigInteger.valueOf(m)).longValue();
    }

    private static long powMod(long base, long exp, long m) {
        long result = 1;
        base %= m;
        while (exp > 0) {
            if ((exp & 1) == 1) {
                result = mulMod(result, base, m);
            }
            base = mulMod(base, base, m);
            exp >>= 1;
        }
        return result;
    }

    /**
     * Miller-Rabin with the first 12 primes as witnesses, which is deterministic for every
     * n below 3.3 * 10^24, so it is exact for all 64-bit inputs. Carmichael numbers such as
     * 561 are rejected because the test checks the square-root chain, not just Fermat.
     */
    public static boolean isPrime(long n) {
        if (n < 2) {
            return false;
        }
        for (long p : WITNESSES) {
            if (n % p == 0) {
                return n == p;
            }
        }
        long d = n - 1;
        int r = 0;
        while ((d & 1) == 0) {
            d >>= 1;
            r++;
        }
        for (long a : WITNESSES) {
            long x = powMod(a, d, n);
            if (x == 1 || x == n - 1) {
                continue;
            }
            boolean composite = true;
            for (int i = 1; i < r; i++) {
                x = mulMod(x, x, n);
                if (x == n - 1) {
                    composite = false;
                    break;
                }
            }
            if (composite) {
                return false;
            }
        }
        return true;
    }

    /** Uniform sample of k items from a stream of unknown length in one pass, O(k) memory. */
    public static int[] reservoirSample(int[] stream, int k, Random rng) {
        int[] reservoir = new int[Math.min(k, stream.length)];
        for (int i = 0; i < stream.length; i++) {
            if (i < reservoir.length) {
                reservoir[i] = stream[i];
            } else {
                int j = rng.nextInt(i + 1);
                if (j < reservoir.length) {
                    reservoir[j] = stream[i];
                }
            }
        }
        return reservoir;
    }
}
