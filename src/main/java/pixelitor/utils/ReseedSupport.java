/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.FilterButtonModel;

import java.util.Random;

import static pixelitor.filters.gui.ReseedActions.reseedByCalling;

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
     * Reinitializes and returns the random number generator in order to
     * make sure that the filter runs with the same random numbers
     * as before (when the filter execution is not started from
     * the "reseed" button).
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

    public static FilterButtonModel createAction() {
        return reseedByCalling(ReseedSupport::reseed);
    }

    public static FilterButtonModel createAction(String name, String toolTipText) {
        return reseedByCalling(ReseedSupport::reseed, name, toolTipText);
    }
}
