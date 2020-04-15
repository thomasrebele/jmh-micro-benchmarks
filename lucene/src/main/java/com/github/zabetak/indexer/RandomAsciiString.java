package com.github.zabetak.indexer;

import java.util.Random;

/**
 * A generator of random strings of a given length comprised from ASCII characters in the numeric
 * range [65, 122].
 * <p>
 * The range [65, 122] contains the capital and small letters of the latin alphaphet along with a
 * few symbols.
 * </p>
 */
public class RandomAsciiString {
    public static final char LOWER_BOUND = 65;
    public static final char UPPER_BOUND = 122;

    private final Random random;
    private final int length;

    public RandomAsciiString(int seed, int length) {
        this.random = new Random(seed);
        this.length = length;
    }

    public String next() {
        char[] buffer = new char[length];
        for (int i = 0; i < length; i++)
            buffer[i] = (char) (LOWER_BOUND + random.nextInt(58));
        return new String(buffer);
    }
}
