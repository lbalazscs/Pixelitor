/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.gui;

import com.jhlabs.math.Noise;
import pixelitor.utils.Icons;

/**
 * Static factory methods for creating randomness-reseeding
 * {@link FilterButtonModel}s
 */
public class ReseedActions {
    private ReseedActions() {
        // utility class, should not be instantiated
    }

    public static FilterButtonModel reseedNoise() {
        return reseedNoise("Reseed", "Reinitialize the randomness.");
    }

    public static FilterButtonModel reseedNoise(String text, String toolTip) {
        return reseedByCalling(Noise::reseed, text, toolTip);
    }

    public static FilterButtonModel reseedByCalling(Runnable beforeTriggeringTask) {
        return reseedByCalling(beforeTriggeringTask, "Reseed");
    }

    public static FilterButtonModel reseedByCalling(Runnable beforeTriggeringTask,
                                                    String text) {
        return reseedByCalling(beforeTriggeringTask, text,
            "Reinitialize the randomness");
    }

    public static FilterButtonModel reseedByCalling(Runnable beforeTriggeringTask,
                                                    String text, String toolTip) {
        var filterAction = new FilterButtonModel(text, beforeTriggeringTask,
            Icons.getTwoDicesIcon(), toolTip, "reseed");
        filterAction.setIgnoreFinalAnimationSettingMode(false);
        return filterAction;
    }

    /**
     * The returned action only re-runs the filter
     * (can be useful when using ThreadLocalRandom)
     */
    public static FilterButtonModel noOpReseed() {
        return reseedByCalling(() -> {});
    }
}
