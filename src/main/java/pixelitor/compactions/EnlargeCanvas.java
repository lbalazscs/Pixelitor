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

package pixelitor.compactions;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.filters.gui.DialogMenuBar;
import pixelitor.gui.View;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.ViewEnabledAction;
import pixelitor.guides.Guides;
import pixelitor.layers.ContentLayer;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.geom.AffineTransform;

/**
 * Enlarges the canvas for all layers of a composition
 * by adding empty space around the existing content
 * or by revealing previously hidden parts of layers.
 * Conceptually it's the opposite of cropping.
 */
public class EnlargeCanvas extends SimpleCompAction {
    private static final String NAME = "Enlarge Canvas";

    private final Outsets enlargement;
    private Dimension newCanvasSize;

    public EnlargeCanvas(int north, int east, int south, int west) {
        this(new Outsets(north, west, south, east));
    }

    public EnlargeCanvas(Outsets outsets) {
        super(NAME, true);

        this.enlargement = outsets;
    }

    @Override
    protected void updateCanvasSize(Canvas canvas, View view) {
        enlargement.resizeCanvas(canvas, view);
        newCanvasSize = canvas.getSize();
    }

    @Override
    protected String getEditName() {
        return NAME;
    }

    @Override
    protected void transform(ContentLayer contentLayer) {
        contentLayer.enlargeCanvas(enlargement);
    }

    @Override
    protected AffineTransform createCanvasTransform(Canvas canvas) {
        return AffineTransform.getTranslateInstance(enlargement.left, enlargement.top);
    }

    @Override
    protected Guides createTransformedGuides(Guides srcGuides, View view, Canvas srcCanvas) {
        return srcGuides.copyEnlarged(enlargement, view, srcCanvas);
    }

    @Override
    protected String getStatusBarMessage() {
        return "The canvas was enlarged to "
            + newCanvasSize.width + " x " + newCanvasSize.height + " pixels.";
    }

    /**
     * Creates an action that shows the canvas enlargement dialog.
     */
    public static Action createDialogAction(String name) {
        return new ViewEnabledAction(name + "...",
            EnlargeCanvas::showInDialog);
    }

    private static void showInDialog(Composition comp) {
        EnlargeCanvasPanel panel = new EnlargeCanvasPanel();
        new DialogBuilder()
            .title(NAME)
            .menuBar(new DialogMenuBar(panel))
            .content(panel)
            .okAction(() -> panel.createCompAction(comp.getCanvas()).process(comp))
            .show();
    }
}
