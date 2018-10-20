/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.shapes;

import pixelitor.filters.painters.AreaEffects;
import pixelitor.tools.util.ImDrag;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.tools.shapes.ShapesTool.STROKE_FOR_OPEN_SHAPES;

/**
 * A shape with associated stroke, fill and effects
 * that can paint itself on a given {@link Graphics2D}
 */
public class StyledShape {
    private final ShapesAction action;
    private final ShapeType shapeType;
    private Shape shape; // the shape, in image-space
    private Shape unTransformedShape;

    private ImDrag imDrag;

    private TwoPointBasedPaint fillPaint;
    private TwoPointBasedPaint strokePaint;
    private AreaEffects effects;

    private Stroke stroke;

    public StyledShape(ShapeType shapeType, ShapesAction action, ShapesTool tool) {
        this.shapeType = shapeType;
        this.action = action;
        if (action.hasFillPaint()) {
            this.fillPaint = tool.getSelectedFillPaint();
        }
        if (action.hasStrokePaint()) {
            this.strokePaint = tool.getSelectedStrokePaint();
            this.stroke = tool.getStroke();
        }
        if (action.canHaveEffects()) {
            this.effects = tool.getEffects();
        }
    }

    /**
     * Paints this object on the given Graphics2D, which is expected to be
     * in image space.
     */
    public void paint(Graphics2D g) {
        if(imDrag == null) {
            // this object is created when the mouse is pressed, but
            // it can be painted only after the first drag events arrive
            return;
        }

        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        if (action.hasFillPaint()) {
            if (shapeType.isClosed()) {
                fillPaint.setupPaint(g, imDrag);
                g.fill(shape);
                fillPaint.finish(g);
            } else if (!action.hasStrokePaint()) {
                // Special case: an open shape cannot be filled,
                // it can be only stroked, even if stroke is disabled.
                // So use the default stroke and the fill paint.
                g.setStroke(STROKE_FOR_OPEN_SHAPES);
                fillPaint.setupPaint(g, imDrag);
                g.draw(shape);
                fillPaint.finish(g);
            }
        }

        if (action.hasStrokePaint()) {
            g.setStroke(stroke);
            strokePaint.setupPaint(g, imDrag);
            g.draw(shape);
            strokePaint.finish(g);
        }

        if (effects != null) {
            assert action.canHaveEffects();

            if (action.hasFillPaint()) {
                effects.drawOn(g, shape);
            } else if (action.hasStrokePaint()) {
                // special case if there is only stroke:
                // apply the effect on the stroke outline
                Shape outline = stroke.createStrokedShape(shape);
                effects.drawOn(g, outline);
            } else { // "effects only"
                effects.drawOn(g, shape);
            }
        }
    }

    public void setImDrag(ImDrag imDrag) {
        this.imDrag = imDrag;
        shape = shapeType.getShape(imDrag);
        unTransformedShape = shape;
    }

    public void transform(AffineTransform at) {
        shape = at.createTransformedShape(unTransformedShape);
    }
}
