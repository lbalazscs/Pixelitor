/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.CompositionView;
import pixelitor.gui.ImageFrame;
import pixelitor.gui.ImageWindow;

/**
 * A debugging node for a CompositionView
 */
public class CompositionViewNode extends DebugNode {
    public CompositionViewNode(String name, CompositionView cv) {
        super(name, cv);

        Composition comp = cv.getComp();
        add(new CompositionNode(comp));

        addQuotedString("name", comp.getName());

        addQuotedString("mask view mode", cv.getMaskViewMode().toString());

        int width = cv.getWidth();
        addInt("cv width", width);
        int height = cv.getHeight();
        addInt("cv height", height);

        ImageWindow imageWindow = cv.getImageWindow();
        if (imageWindow instanceof ImageFrame) {
            ImageFrame frame = (ImageFrame) imageWindow;
            int frameWidth = frame.getWidth();
            addInt("frameWidth", frameWidth);
            int frameHeight = frame.getHeight();
            addInt("frameHeight", frameHeight);
        }

        addString("zoom level", cv.getZoomLevel().toString());
        Canvas canvas = cv.getCanvas();
        int zoomedCanvasWidth = canvas.getCoWidth();
        addInt("zoomedCanvasWidth", zoomedCanvasWidth);
        int zoomedCanvasHeight = canvas.getCoHeight();
        addInt("zoomedCanvasHeight", zoomedCanvasHeight);
//        boolean bigCanvas = cv.isBigCanvas();
//        addBooleanChild("bigCanvas", bigCanvas);
//        boolean optimizedDrawingEnabled = cv.getImageWindow().isOptimizedDrawingEnabled();
//        addBoolean("optimizedDrawingEnabled", optimizedDrawingEnabled);
    }
}
