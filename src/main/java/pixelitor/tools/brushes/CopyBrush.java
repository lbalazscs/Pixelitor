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

package pixelitor.tools.brushes;

import pixelitor.utils.Utils;
import pixelitor.utils.debug.DebugNode;

import java.awt.image.BufferedImage;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static pixelitor.tools.brushes.AngleSettings.NOT_ANGLE_AWARE;

/**
 * A superclass for the clone and smudge brushes.
 */
public abstract class CopyBrush extends DabsBrush {
    protected BufferedImage sourceImage;
    protected BufferedImage brushImage;
    protected CopyBrushType type;
    private static boolean debugBrushImage = false;

    protected CopyBrush(int radius, CopyBrushType type, SpacingStrategy spacingStrategy) {
        super(radius, spacingStrategy, NOT_ANGLE_AWARE, true);
        this.type = type;
    }

    @Override
    public void setRadius(int radius) {
        super.setRadius(radius);
        if (type != null) { // cannot initialize properly when called from superclass constructor
            brushImage = new BufferedImage(diameter, diameter, TYPE_INT_ARGB);
            type.setSize(diameter);
        }
    }

    public void typeChanged(CopyBrushType type) {
        this.type = type;
        type.setSize(diameter);
    }

    public void debugImage() {
        if (debugBrushImage) {
            Utils.debugImage(brushImage, "Copy Brush");
        }
    }

    public static void setDebugBrushImage(boolean debugBrushImage) {
        CopyBrush.debugBrushImage = debugBrushImage;
    }

    public CopyBrushType getType() {
        return type;
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = super.getDebugNode();

        node.addStringChild("Type", type.toString());

        return node;
    }
}
