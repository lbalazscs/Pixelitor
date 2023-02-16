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

package pixelitor.compactions;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.filters.gui.DialogMenuBar;
import pixelitor.gui.View;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.OpenViewEnabledAction;
import pixelitor.guides.Guides;
import pixelitor.layers.ContentLayer;

import javax.swing.*;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

/**
 * Enlarges the canvas for all layers of a composition. It's the opposite of cropping:
 * it adds empty space around a layer or makes the hidden part of the layer visible.
 */
public class EnlargeCanvas extends SimpleCompAction {
    private static final String NAME = "Enlarge Canvas";
    private int north;
    private int east;
    private int south;
    private int west;
    private int newCanvasWidth;
    private int newCanvasHeight;

    public EnlargeCanvas(int north, int east, int south, int west) {
        super(NAME, true);
        this.north = north;
        this.east = east;
        this.south = south;
        this.west = west;
    }

    public void setupToFitContentOf(ContentLayer contentLayer) {
        Rectangle contentBounds = contentLayer.getContentBounds();
        if (contentBounds == null) {
            // can happen for gradient layers and for uninitialized shape layers
            return;
        }

        Canvas canvas = contentLayer.getComp().getCanvas();

        if (contentBounds.x < -west) {
            west = -contentBounds.x;
        }

        if (contentBounds.y < -north) {
            north = -contentBounds.y;
        }

        int contentMaxX = contentBounds.x + contentBounds.width;
        if (contentMaxX > canvas.getWidth() + east) {
            east = contentMaxX - canvas.getWidth();
        }

        int contentMaxY = contentBounds.y + contentBounds.height;
        if (contentMaxY > canvas.getHeight() + south) {
            south = contentMaxY - canvas.getHeight();
        }
    }

    public boolean doesNothing() {
        return north == 0 && east == 0 && south == 0 && west == 0;
    }

    @Override
    protected void resizeNewCanvas(Canvas newCanvas, View view) {
        newCanvasWidth = newCanvas.getWidth() + east + west;
        newCanvasHeight = newCanvas.getHeight() + north + south;
        newCanvas.resize(newCanvasWidth, newCanvasHeight, view, false);
    }

    @Override
    protected String getEditName() {
        return NAME;
    }

    @Override
    protected void transform(ContentLayer contentLayer) {
        contentLayer.enlargeCanvas(north, east, south, west);
    }

    @Override
    protected AffineTransform createCanvasTransform(Canvas canvas) {
        return AffineTransform.getTranslateInstance(west, north);
    }

    @Override
    protected Guides createGuidesCopy(Guides oldGuides, View view, Canvas oldCanvas) {
        return oldGuides.copyForEnlargedCanvas(north, east, south, west, view, oldCanvas);
    }

    @Override
    protected String getStatusBarMessage() {
        return "The canvas was enlarged to "
            + newCanvasWidth + " x " + newCanvasHeight + " pixels.";
    }

    public static Action getAction() {
        return new OpenViewEnabledAction("Enlarge Canvas...",
            EnlargeCanvas::showInDialog);
    }

    private static void showInDialog(Composition comp) {
        var p = new EnlargeCanvasPanel();
        new DialogBuilder()
            .title(NAME)
            .menuBar(new DialogMenuBar(p))
            .content(p)
            .okAction(() -> p.getCompAction(comp.getCanvas()).process(comp))
            .show();
    }
}
