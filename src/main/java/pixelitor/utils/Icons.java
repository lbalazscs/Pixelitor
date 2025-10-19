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
package pixelitor.utils;

import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.ThemedImageIcon;
import pixelitor.gui.utils.VectorIcon;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

/**
 * Icon-related static utility methods
 */
public final class Icons {
    private static final Shape TEXT_LAYER_ICON_SHAPE = createTextLayerIconShape();
    private static final Shape ADJ_LAYER_ICON_SHAPE = createAdjLayerIconShape();
    private static final Shape SMART_FILTER_ICON_SHAPE = createSmartFilterIconShape();

    private static final Icon resetIcon = loadThemed("west_arrow.gif", ThemedImageIcon.BLUE);
    private static final Icon randomizeIcon = load("dice.png");
    private static final Icon reseedIcon = load("dice2.png");
    private static final Icon upArrowIcon = loadThemed("north_arrow.gif", ThemedImageIcon.BLUE);
    private static final Icon downArrowIcon = loadThemed("south_arrow.gif", ThemedImageIcon.BLUE);

    private static final Icon textLayerIcon = VectorIcon.createNonTransparentThemed(TEXT_LAYER_ICON_SHAPE);
    private static final Icon adjLayerIcon = VectorIcon.createNonTransparentThemed(ADJ_LAYER_ICON_SHAPE);
    private static final Icon smartFilterIcon = VectorIcon.createNonTransparentThemed(SMART_FILTER_ICON_SHAPE);
    private static final Icon undoIcon = loadThemed("undo.png", ThemedImageIcon.BLUE);
    private static final Icon redoIcon = loadThemed("redo.png", ThemedImageIcon.BLUE);

    private Icons() {
        // should not be instantiated
    }

    public static Icon getResetIcon() {
        return resetIcon;
    }

    public static Icon getRandomizeIcon() {
        return randomizeIcon;
    }

    public static Icon getReseedIcon() {
        return reseedIcon;
    }

    public static Icon loadThemed(String iconFileName, int newPixelColor) {
        assert iconFileName != null;
        return new ThemedImageIcon(iconFileName, newPixelColor);
    }

    public static Icon load(String ltIconFileName, String dtIconFileName) {
        return new ThemedImageIcon(ltIconFileName, dtIconFileName);
    }

    public static Icon load(String iconFileName) {
        assert iconFileName != null;
        URL imgURL = ImageUtils.findImageURL(iconFileName);
        return new ImageIcon(imgURL);
    }

    public static Icon loadMultiRes(String baseIconFileName, String bigIconFileName) {
        try {
            URL imgURL1 = ImageUtils.findImageURL(baseIconFileName);
            BufferedImage img1 = ImageIO.read(imgURL1);

            URL imgURL2 = ImageUtils.findImageURL(bigIconFileName);
            BufferedImage img2 = ImageIO.read(imgURL2);

            return new ImageIcon(new BaseMultiResolutionImage(img1, img2));
        } catch (IOException e) {
            Dialogs.showExceptionDialog(e);
            return null;
        }
    }

    public static Icon getUpArrowIcon() {
        return upArrowIcon;
    }

    public static Icon getDownArrowIcon() {
        return downArrowIcon;
    }

    public static Icon getTextLayerIcon() {
        return textLayerIcon;
    }

    public static Icon getAdjLayerIcon() {
        return adjLayerIcon;
    }

    public static Icon getSmartFilterIcon() {
        return smartFilterIcon;
    }

    public static Icon getUndoIcon() {
        return undoIcon;
    }

    public static Icon getRedoIcon() {
        return redoIcon;
    }

    private static Shape createTextLayerIconShape() {
        Path2D path = new Path2D.Double();

        path.moveTo(6, 4);
        path.lineTo(18, 4);
        path.lineTo(18, 8);
        path.lineTo(14, 8);
        path.lineTo(14, 21);
        path.lineTo(10, 21);
        path.lineTo(10, 8);
        path.lineTo(6, 8);
        path.closePath();

        return path;
    }

    private static Shape createAdjLayerIconShape() {
        Path2D path = new Path2D.Double();

        path.moveTo(9.5, 4);
        path.lineTo(14.5, 4);
        path.lineTo(20.5, 21);
        path.lineTo(16, 21);
        path.lineTo(15.5, 18);
        path.lineTo(8.5, 18);
        path.lineTo(8, 21);
        path.lineTo(3.5, 21);
        path.closePath();

        // go in the counter-clockwise direction
        // to cut a WIND_NON_ZERO hole
        path.moveTo(11, 10);
        path.lineTo(10, 15);
        path.lineTo(14, 15);
        path.lineTo(13, 10);
        path.closePath();

        return path;
    }

    private static Shape createSmartFilterIconShape() {
        Path2D path = new Path2D.Double();

        path.moveTo(7, 4);
        path.lineTo(18, 4);
        path.lineTo(18, 8);
        path.lineTo(11, 8);
        path.lineTo(11, 11);
        path.lineTo(17, 11);
        path.lineTo(17, 15);
        path.lineTo(11, 15);
        path.lineTo(11, 20);
        path.lineTo(7, 20);
        path.closePath();

        return path;
    }
}
