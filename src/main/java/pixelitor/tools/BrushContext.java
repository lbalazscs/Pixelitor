/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools;

import pixelitor.Composition;
import pixelitor.layers.Drawable;
import pixelitor.layers.LayerMask;
import pixelitor.tools.brushes.Brush;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static java.awt.AlphaComposite.DST_OUT;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * Manages the graphics context and state for a single brush stroke.
 */
public class BrushContext {
    private final Graphics2D graphics;

    // a brush stroke begins, operates, and finishes on the same Drawable, even
    // if the adctive layer changes via keyboard shortcut while the mouse is dragged
    private final Drawable dr;

    private final DrawTarget drawTarget;
    private final BufferedImage originalImage; // individual stroke backup

    /**
     * Creates and initializes a graphics context for a new brush stroke.
     */
    public BrushContext(Drawable dr, DrawTarget drawTarget, Brush brush, Composite composite) {
        this.dr = dr;
        this.drawTarget = drawTarget;

        // prepare the target drawable and store the backup image
        this.originalImage = drawTarget.prepareForBrushStroke(dr);

        Composition comp = dr.getComp();

        // checking that masks are only drawn DIRECTly
        assert !(dr instanceof LayerMask)
            || drawTarget == DrawTarget.DIRECT :
            "dr is " + dr.getClass().getSimpleName()
                + ", comp = " + comp.getName()
                + ", tool = " + Tools.getActive().getName()
                + ", drawTarget = " + drawTarget;

        graphics = drawTarget.createGraphics(dr, composite);
        comp.applySelectionClipping(graphics);

        graphics.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        brush.setTarget(dr, graphics);
    }

    public BufferedImage getOriginalImage() {
        return originalImage;
    }

    public void setColor(Color color) {
        graphics.setColor(color);
    }

    public void setErasing() {
        // the color doesn't matter as long as AlphaComposite.DST_OUT is used
        graphics.setComposite(AlphaComposite.getInstance(DST_OUT));
    }

    /**
     * Disposes graphics resources and finalizes the drawing on the target drawable.
     */
    public void finish() {
        graphics.dispose();

        drawTarget.finishBrushStroke(dr, originalImage);
        dr.update();
        dr.updateIconImage();
    }

    /**
     * Returns the {@link Drawable} on which the brush stroke started.
     */
    public Drawable getDrawable() {
        return dr;
    }
}
