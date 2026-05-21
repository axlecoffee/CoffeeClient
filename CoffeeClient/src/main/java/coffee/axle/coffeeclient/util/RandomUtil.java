/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package coffee.axle.coffeeclient.util;

import java.util.Random;

public class RandomUtil {
    private static final Random _random = new Random();

    public static long nextLong(long min, long max) {
        return (long) nextDouble((double) min, (double) (max + 1L));
    }

    public static float nextFloat(float min, float max) {
        return _random.nextFloat() * (max - min) + min;
    }

    public static double nextDouble(double min, double max) {
        return _random.nextDouble() * (max - min) + min;
    }
}
