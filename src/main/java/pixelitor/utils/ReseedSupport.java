/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.FilterAction;
import pixelitor.filters.gui.ReseedNoiseFilterAction;

import java.util.Random;

/**
 * Support for "reseed" buttons in filters. The idea is
 * that the filter uses the random number generator defined
 * here, and the actions reseed it.
 */
public class ReseedSupport {
    private static long seed = System.nanoTime();
    private static final Random rand = new Random();

    private ReseedSupport() {
    }

    /**
     * Reinitializes the random number generator in order to
     * make sure that the filter runs with the same random numbers
     * as before when the re-run is NOT caused by pressing
     * the "reseed" button.
     * Returns the random number generator which is reseeded.
     * This must be called at the beginning of the filter.
     */
    public static Random reInitialize() {
        rand.setSeed(seed);
        return rand;
    }

    /**
     * Called then the user presses the "reseed" button
     */
    private static void reseed() {
        seed = System.nanoTime();
    }

    public static FilterAction createAction() {
        return new ReseedNoiseFilterAction(
                e -> reseed());
    }

    public static FilterAction createAction(String name, String toolTipText) {
        return new ReseedNoiseFilterAction(
                name, toolTipText,
                e -> reseed());
    }
}
