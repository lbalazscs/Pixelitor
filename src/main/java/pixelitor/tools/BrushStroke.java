/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.layers.Drawable;
import pixelitor.layers.LayerMask;
import pixelitor.tools.brushes.Brush;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;

import static java.awt.AlphaComposite.DST_OUT;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

public class BrushStroke {
    private final Graphics2D graphics;
    private final Drawable dr;
    private final DrawDestination drawDestination;

    public BrushStroke(Drawable dr, DrawDestination drawDestination, Brush brush, Composite composite) {
        this.dr = dr;
        this.drawDestination = drawDestination;

        drawDestination.prepareBrushStroke(dr);

        var comp = dr.getComp();

        // when editing masks, no tmp drawing layer should be used
        assert !(dr instanceof LayerMask)
            || drawDestination == DrawDestination.DIRECT :
            "dr is " + dr.getClass().getSimpleName()
                + ", comp = " + comp.getName()
                + ", tool = " + getClass().getSimpleName()
                + ", drawDestination = " + drawDestination;

        graphics = drawDestination.createGraphics(dr, composite);
        comp.applySelectionClipping(graphics);

        graphics.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        brush.setTarget(dr, graphics);
    }

    public void setColor(Color color) {
        graphics.setColor(color);
    }

    public void setTransparentDrawing() {
        // the color doesn't matter as long as AlphaComposite.DST_OUT is used
        graphics.setComposite(AlphaComposite.getInstance(DST_OUT));
    }

    public void finish(Drawable dr) {
        assert this.dr == dr;

        graphics.dispose();

        drawDestination.finishBrushStroke(dr);
        dr.update();
        dr.updateIconImage();
    }
}
