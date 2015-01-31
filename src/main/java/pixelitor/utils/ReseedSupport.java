/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

import pixelitor.filters.gui.ActionParam;
import pixelitor.filters.gui.ReseedNoiseActionParam;

import java.util.Random;

public class ReseedSupport {
    private static long seed = System.nanoTime();
    private static final Random rand = new Random();

    private ReseedSupport() {
    }

    /**
     * Reinitializes the random number generator in order to
     * make sure that the filter runs with the same random numbers
     */
    public static void reInitialize() {
        rand.setSeed(seed);
    }

    private static void reseed() {
        seed = System.nanoTime();
    }


    public static ActionParam createParam() {
        return new ReseedNoiseActionParam(
                e -> {
                    reseed();
                });
    }

    public static ActionParam createParam(String name, String toolTipText) {
        return new ReseedNoiseActionParam(
                name, toolTipText,
                e -> reseed());
    }


    public static Random getRand() {
        return rand;
    }
}
