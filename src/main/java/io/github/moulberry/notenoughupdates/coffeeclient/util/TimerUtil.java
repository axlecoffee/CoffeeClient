/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package io.github.moulberry.notenoughupdates.coffeeclient.util;

public class TimerUtil {
    private long _lastMS = 0L;

    public void reset() {
        this._lastMS = System.currentTimeMillis();
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - this._lastMS;
    }

    public boolean hasTimeElapsed(long ms) {
        return this.getElapsedTime() >= ms;
    }

    public void setTime() {
        this._lastMS = 0L;
    }
}
