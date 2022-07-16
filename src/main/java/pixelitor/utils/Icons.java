/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.utils.Themes;
import pixelitor.gui.utils.VectorIcon;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

/**
 * Icon-related static utility methods
 */
public final class Icons {
    private static final Icon westArrowIcon = loadThemed("west_arrow.gif", ThemedImageIcon.BLUE);
    private static final Icon diceIcon = load("dice.png");
    private static final Icon dice2Icon = load("dice2.png");
    private static final Icon northArrowIcon = loadThemed("north_arrow.gif", ThemedImageIcon.BLUE);
    private static final Icon southArrowIcon = loadThemed("south_arrow.gif", ThemedImageIcon.BLUE);
    //    private static final Icon textLayerIcon = load("text_layer.png", "text_layer_icon_dark.png");
    private static final Icon textLayerIcon = createTextLayerIcon();
    private static final Icon adjLayerIcon = createAdjLayerIcon();
    private static final Icon undoIcon = loadThemed("undo.png", ThemedImageIcon.BLUE);
    private static final Icon redoIcon = loadThemed("redo.png", ThemedImageIcon.BLUE);

    private static Color LIGHT_BG = new Color(214, 217, 223);
    private static Color DARK_BG = new Color(42, 42, 42);
    private static Color LIGHT_FG = new Color(19, 30, 43);

    private Icons() {
        // should not be instantiated
    }

    public static Icon getWestArrowIcon() {
        return westArrowIcon;
    }

    public static Icon getDiceIcon() {
        return diceIcon;
    }

    public static Icon getTwoDicesIcon() {
        return dice2Icon;
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
        URL imgURL = ImageUtils.imagePathToURL(iconFileName);
        return new ImageIcon(imgURL);
    }

    public static Icon loadMultiRes(String baseIconFileName, String bigIconFileName) {
        try {
            URL imgURL1 = ImageUtils.imagePathToURL(baseIconFileName);
            BufferedImage img1 = ImageIO.read(imgURL1);

            URL imgURL2 = ImageUtils.imagePathToURL(bigIconFileName);
            BufferedImage img2 = ImageIO.read(imgURL2);

            return new ImageIcon(new BaseMultiResolutionImage(img1, img2));
        } catch (IOException e) {
            Dialogs.showExceptionDialog(e);
            return null;
        }
    }

    public static Icon getNorthArrowIcon() {
        return northArrowIcon;
    }

    public static Icon getSouthArrowIcon() {
        return southArrowIcon;
    }

    public static Icon getTextLayerIcon() {
        return textLayerIcon;
    }

    public static Icon getAdjLayerIcon() {
        return adjLayerIcon;
    }

    public static Icon getUndoIcon() {
        return undoIcon;
    }

    public static Icon getRedoIcon() {
        return redoIcon;
    }

    private static Icon createTextLayerIcon() {
        return new VectorIcon(Color.WHITE, 24, 24) {
            @Override
            protected void paintIcon(Graphics2D g) {
                boolean darkTheme = Themes.getCurrent().isDark();
                g.setColor(darkTheme ? DARK_BG : LIGHT_BG);
                g.fillRect(0, 0, 24, 24);

                g.setColor(darkTheme ? Themes.LIGHT_ICON_COLOR : LIGHT_FG);
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
                g.fill(path);
            }
        };
    }

    private static Icon createAdjLayerIcon() {
        return new VectorIcon(Color.WHITE, 24, 24) {
            @Override
            protected void paintIcon(Graphics2D g) {
                boolean darkTheme = Themes.getCurrent().isDark();
                g.setColor(darkTheme ? DARK_BG : LIGHT_BG);
                g.fillRect(0, 0, 24, 24);

                g.setColor(darkTheme ? Themes.LIGHT_ICON_COLOR : LIGHT_FG);
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

                g.fill(path);
            }
        };
    }
}