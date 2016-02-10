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

package pixelitor.utils.debug;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.InternalImageFrame;

/**
 * A debugging node for an ImageComponent
 */
public class ImageComponentNode extends DebugNode {
    public ImageComponentNode(String name, ImageComponent ic) {
        super(name, ic);

        Composition comp = ic.getComp();
        add(new CompositionNode(comp));

        addQuotedStringChild("name", comp.getName());

        addQuotedStringChild("mask view mode", ic.getMaskViewMode().toString());

        int width = ic.getWidth();
        addIntChild("ic width", width);
        int height = ic.getHeight();
        addIntChild("ic height", height);

        InternalImageFrame internalFrame = ic.getInternalFrame();
        int internalFrameWidth = internalFrame.getWidth();
        addIntChild("internalFrameWidth", internalFrameWidth);
        int internalFrameHeight = internalFrame.getHeight();
        addIntChild("internalFrameHeight", internalFrameHeight);

        addStringChild("zoom level", ic.getZoomLevel().toString());
        Canvas canvas = ic.getCanvas();
        int zoomedCanvasWidth = canvas.getZoomedWidth();
        addIntChild("zoomedCanvasWidth", zoomedCanvasWidth);
        int zoomedCanvasHeight = canvas.getZoomedHeight();
        addIntChild("zoomedCanvasHeight", zoomedCanvasHeight);
//        boolean bigCanvas = ic.isBigCanvas();
//        addBooleanChild("bigCanvas", bigCanvas);
        boolean optimizedDrawingEnabled = ic.getInternalFrame().isOptimizedDrawingEnabled();
        addBooleanChild("optimizedDrawingEnabled", optimizedDrawingEnabled);
    }
}
