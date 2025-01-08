/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.gui.utils;

import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * An ImageIcon with theme-based image rendering.
 */
public class ThemedImageIcon extends ImageIcon {
    // color constants for dark theme pixel replacement
    public static final int GREEN = 0x00428F4C;
    public static final int RED = 0x00CC555D;
    public static final int BLUE = 0x004488AD;
    public static final int BLACK = 0x00000000;

    // if the dark-themed image has to be generated,
    // based on the alpha of the original, this
    // is the color of the new pixels, without the alpha
    private int darkThemePixelColor;

    private BufferedImage lightThemeImage;
    private BufferedImage darkThemeImage;

    public ThemedImageIcon(String iconFileName, int darkThemePixelColor) {
        lightThemeImage = ImageUtils.loadResourceImage(iconFileName);
        this.darkThemePixelColor = darkThemePixelColor;
    }

    public ThemedImageIcon(String ltIconFileName, String dtIconFileName) {
        lightThemeImage = ImageUtils.loadResourceImage(ltIconFileName);
        darkThemeImage = ImageUtils.loadResourceImage(dtIconFileName);

        assert lightThemeImage.getWidth() == darkThemeImage.getWidth();
        assert lightThemeImage.getHeight() == darkThemeImage.getHeight();
    }

    @Override
    public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
        g.drawImage(getImage(), x, y, null);
    }

    /**
     * Generates a dark theme image by replacing pixel colors while preserving alpha.
     */
    private void generateDarkThemedImage() {
        darkThemeImage = new BufferedImage(
            lightThemeImage.getWidth(), lightThemeImage.getHeight(), TYPE_INT_ARGB);
        if (!ImageUtils.hasPackedIntArray(lightThemeImage)) {
            lightThemeImage = ImageUtils.convertToARGB(lightThemeImage, true);
        }

        int[] lightThemePixels = ImageUtils.getPixels(lightThemeImage);
        int[] darkThemePixels = ImageUtils.getPixels(darkThemeImage);
        for (int i = 0; i < lightThemePixels.length; i++) {
            int pixel = lightThemePixels[i];
            int alpha = pixel & 0xFF000000;
            darkThemePixels[i] = alpha | darkThemePixelColor;
        }
    }

    @Override
    public int getIconWidth() {
        return lightThemeImage.getWidth();
    }

    @Override
    public int getIconHeight() {
        return lightThemeImage.getHeight();
    }

    // this class extends ImageIcon and overrides this method so that
    // the look and feel automatically generates the disabled icons
    @Override
    public Image getImage() {
        if (Themes.getActive().isDark()) {
            if (darkThemeImage == null) {
                generateDarkThemedImage();
            }
            return darkThemeImage;
        } else {
            return lightThemeImage;
        }
    }
}
