/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.utils;

/**
 * Test implementation of {@link ProgressHandler}.
 */
public class TestProgressHandler implements ProgressHandler {
    private final int maxValue;
    private boolean completed = false;
    private int currentValue;

    TestProgressHandler(int maxValue) {
        this.maxValue = maxValue;
    }

    @Override
    public void updateProgress(int value) {
        if (completed) {
            throw new IllegalStateException();
        }
        if (value < 0 || (maxValue > 0 && value > maxValue)) {
            throw new IllegalArgumentException("value = " + value);
        }
        currentValue = value;
    }

    @Override
    public void stopProgress() {
        completed = true;
    }

    public int getCurrentValue() {
        return currentValue;
    }

    public boolean isCompleted() {
        return completed;
    }
}
