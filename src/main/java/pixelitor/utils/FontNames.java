/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FontNames {
    private FontNames() {
    }

    private static final CountDownLatch fontLoadingLatch = new CountDownLatch(1);
    private static String[] fontNames;

    public static void preloadFontNames() {
        assert fontLoadingLatch.getCount() == 1;
        try {
            fontNames = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();

            // it's almost sorted already, but not completely
            Arrays.sort(fontNames);
        } finally {
            fontLoadingLatch.countDown();
        }
    }

    public static void preloadUnitTestFontNames() {
        assert fontLoadingLatch.getCount() == 1;
        fontNames = new String[]{Font.DIALOG, Font.DIALOG_INPUT,
            Font.MONOSPACED, Font.SANS_SERIF, Font.SERIF};
        fontLoadingLatch.countDown();
    }

    public static String[] getAvailableFontNames() {
        // wait until all font names are loaded
        try {
            boolean ok = fontLoadingLatch.await(5, TimeUnit.MINUTES);
            if (!ok) {
                throw new RuntimeException("Timeout: the font names could not be loaded");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return fontNames;
    }

    public static boolean fontExists(String fontName) {
        String[] sortedFontNames = getAvailableFontNames();
        int index = Arrays.binarySearch(sortedFontNames, fontName);
        return index >= 0;
    }

    public static String getFirstFontName() {
        return getAvailableFontNames()[0];
    }
}
