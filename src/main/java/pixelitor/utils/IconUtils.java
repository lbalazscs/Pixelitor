/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import javax.swing.*;
import java.net.URL;

/**
 * Icon-related utility methods
 */
public final class IconUtils {
    private static final Icon westArrowIcon = loadIcon("west_arrow.gif");
    private static final Icon diceIcon = loadIcon("dice.png");
    private static final Icon dice2Icon = loadIcon("dice2.png");
    private static final Icon northArrowIcon = loadIcon("north_arrow.gif");
    private static final Icon southArrowIcon = loadIcon("south_arrow.gif");
    private static final Icon textLayerIcon = loadIcon("text_layer_icon.png");
    private static final Icon adjLayerIcon = loadIcon("adj_layer_icon.png");
    private static final Icon undoIcon = loadIcon("undo.png");
    private static final Icon redoIcon = loadIcon("redo.png");

    /**
     * Utility class with static methods
     */
    private IconUtils() {
    }

    public static Icon getWestArrowIcon() {
        return westArrowIcon;
    }

    public static Icon getDiceIcon() {
        return diceIcon;
    }

    public static Icon getDice2Icon() {
        return dice2Icon;
    }

    public static Icon loadIcon(String iconFileName) {
        assert iconFileName != null;

        URL imgURL = ImageUtils.resourcePathToURL(iconFileName);
        ImageIcon icon = new ImageIcon(imgURL);
        return icon;
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
}