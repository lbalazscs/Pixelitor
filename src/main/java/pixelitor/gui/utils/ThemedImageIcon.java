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

package pixelitor.gui.utils;

import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

public class ThemedImageIcon extends ImageIcon {
    private BufferedImage imageForLightTheme;

    public static final int WHITE = 0x00BEBEBE;
    public static final int GREEN = 0x00428F4C;
    public static final int RED = 0x00CC555D;
    public static final int BLUE = 0x004488AD;
    public static final int BLACK = 0x00000000;

    // if the dark-themed image has to be generated,
    // based on the alpha of the original, this
    // is the color of the new pixels, without the alpha
    private int newPixelColor;

    private BufferedImage imageForDarkTheme;

    public ThemedImageIcon(String iconFileName, int newPixelColor) {
        imageForLightTheme = ImageUtils.loadJarImageFromImagesFolder(iconFileName);
        this.newPixelColor = newPixelColor;
    }

    public ThemedImageIcon(String ltIconFileName, String dtIconFileName) {
        imageForLightTheme = ImageUtils.loadJarImageFromImagesFolder(ltIconFileName);
        imageForDarkTheme = ImageUtils.loadJarImageFromImagesFolder(dtIconFileName);
        assert imageForLightTheme.getWidth() == imageForDarkTheme.getWidth();
        assert imageForLightTheme.getHeight() == imageForDarkTheme.getHeight();
    }

    @Override
    public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
        if (Themes.getCurrent().isDark()) {
            if (imageForDarkTheme == null) {
                createDarkThemedImage();
            }
            g.drawImage(imageForDarkTheme, x, y, null);
        } else {
            g.drawImage(imageForLightTheme, x, y, null);
        }
    }

    /**
     * Automatically creates a dark themed icon based on the light
     * themed icon by setting all pixel values to a light color.
     */
    private void createDarkThemedImage() {
        imageForDarkTheme = new BufferedImage(
            imageForLightTheme.getWidth(), imageForLightTheme.getHeight(), TYPE_INT_ARGB);
        if (!ImageUtils.hasPackedIntArray(imageForLightTheme)) {
            imageForLightTheme = ImageUtils.convertToARGB(imageForLightTheme, true);
        }

        int[] srcData = ImageUtils.getPixelsAsArray(imageForLightTheme);
        int[] dstData = ImageUtils.getPixelsAsArray(imageForDarkTheme);
        for (int i = 0; i < srcData.length; i++) {
            int pixel = srcData[i];
            int a = pixel & 0xFF000000;
            dstData[i] = a | newPixelColor;
        }
    }

    @Override
    public int getIconWidth() {
        return imageForLightTheme.getWidth();
    }

    @Override
    public int getIconHeight() {
        return imageForLightTheme.getHeight();
    }

    // this class extends ImageIcon and overrides this method so that
    // the look and feel automatically generates the disabled icons
    @Override
    public Image getImage() {
        if (Themes.getCurrent().isDark()) {
            if (imageForDarkTheme == null) {
                createDarkThemedImage();
            }
            return imageForDarkTheme;
        } else {
            return imageForLightTheme;
        }
    }
}
