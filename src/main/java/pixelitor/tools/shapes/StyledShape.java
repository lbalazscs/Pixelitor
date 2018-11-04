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
import pixelitor.gui.View;
import pixelitor.tools.transform.TransformBox;
import pixelitor.tools.util.ImDrag;
import pixelitor.tools.util.UserDrag;
import pixelitor.utils.Shapes;
import pixelitor.utils.debug.DebugNode;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.tools.shapes.ShapesTool.STROKE_FOR_OPEN_SHAPES;

/**
 * A shape with associated stroke, fill and effects
 * that can paint itself on a given {@link Graphics2D}
 */
public class StyledShape {
    private ShapesAction action;
    private final ShapesTool tool;
    private ShapeType shapeType;
    private Shape shape; // the current shape, in image-space
    private Shape unTransformedShape; // the original shape, in image-space

    // this doesn't change after the transform box appears,
    // so that another untransformed shape can be generated
    private ImDrag origImDrag;

    // this is transformed as the box is manipulated, so that
    // the gradients move together with the box
    private ImDrag transformedImDrag;

    private TwoPointBasedPaint fillPaint;
    private TwoPointBasedPaint strokePaint;
    private AreaEffects effects;

    private Stroke stroke;
    private boolean insideBox;

    public StyledShape(ShapeType shapeType, ShapesAction action,
                       ShapesTool tool) {
        this.shapeType = shapeType;
        this.action = action;
        this.tool = tool;
        configureBasedOnAction(action, tool);
        this.insideBox = false;
    }

    private void configureBasedOnAction(ShapesAction action, ShapesTool tool) {
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
        if (transformedImDrag == null) {
            // this object is created when the mouse is pressed, but
            // it can be painted only after the first drag events arrive
            return;
        }

        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        if (action.hasFillPaint()) {
            if (shapeType.isClosed()) {
                fillPaint.setupPaint(g, transformedImDrag);
                g.fill(shape);
                fillPaint.finish(g);
            } else if (!action.hasStrokePaint()) {
                // Special case: an open shape cannot be filled,
                // it can be only stroked, even if stroke is disabled.
                // So use the default stroke and the fill paint.
                g.setStroke(STROKE_FOR_OPEN_SHAPES);
                fillPaint.setupPaint(g, transformedImDrag);
                g.draw(shape);
                fillPaint.finish(g);
            }
        }

        if (action.hasStrokePaint()) {
            g.setStroke(stroke);
            strokePaint.setupPaint(g, transformedImDrag);
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
        assert !insideBox;

        this.origImDrag = imDrag;
        unTransformedShape = shapeType.getShape(imDrag);

        // this method should be called only during the
        // initial drag, when there is no transform box yet
        this.transformedImDrag = imDrag;
        shape = unTransformedShape;
    }

    public void transform(AffineTransform at) {
        shape = at.createTransformedShape(unTransformedShape);
        transformedImDrag = origImDrag.createTransformed(at);
    }

    public void setAction(ShapesAction action) {
        this.action = action;
        configureBasedOnAction(action, tool);
    }

    public void setFillPaint(TwoPointBasedPaint fillPaint) {
        this.fillPaint = fillPaint;
    }

    public void setStrokePaint(TwoPointBasedPaint strokePaint) {
        this.strokePaint = strokePaint;
    }

    public void setStroke(Stroke stroke) {
        this.stroke = stroke;
    }

    public void setEffects(AreaEffects effects) {
        this.effects = effects;
    }

    public void setType(ShapeType shapeType) {
        assert insideBox;
        this.shapeType = shapeType;

        if (shapeType.isDirectional()) {
            // TODO this ignores the height of the current box
            unTransformedShape = shapeType.getShape(origImDrag.getCenterHorizontalDrag());
        } else {
            unTransformedShape = shapeType.getShape(origImDrag);
        }

        // must be created before the next call to paint()
        shape = null;
    }

    public TransformBox createBox(UserDrag userDrag, View view) {
        assert !insideBox;
        insideBox = true;

        TransformBox box;
        if (shapeType.isDirectional()) {
            box = createRotatedBox(userDrag, view);
        } else {
            box = new TransformBox(userDrag.toCoRect(), view, this::transform);
        }
        return box;
    }

    private TransformBox createRotatedBox(UserDrag userDrag, View view) {
        // First calculate the settings for a horizontal box.
        // The box is in component space, everything else is in image space.

        // Set the original shape to the horizontal shape.
        // It could also be rotated backwards with an AffineTransform.
        ImDrag imDrag = userDrag.toImDrag();
        unTransformedShape = shapeType.getHorizontalShape(imDrag);

        // Set the original drag to the diagonal of the back-rotated transform box,
        // so that after a shape-type change the new shape is created correctly
        double imDragDist = imDrag.getDistance();
        double halfImHeight = imDragDist * Shapes.UNIT_ARROW_HEAD_WIDTH / 2.0;
        origImDrag = new ImDrag(
                imDrag.getStartX(),
                imDrag.getStartY() - halfImHeight,
                imDrag.getStartX() + imDragDist,
                imDrag.getStartY() + halfImHeight);
//            transformedImDrag = origImDrag;

        // create the horizontal box
        double coDist = userDrag.calcCoDist();
        Rectangle2D horizontalBoxBounds = new Rectangle.Double(
                userDrag.getCoStartX(),
                userDrag.getCoStartY() - coDist * Shapes.UNIT_ARROW_HEAD_WIDTH / 2.0,
                coDist,
                coDist * Shapes.UNIT_ARROW_HEAD_WIDTH);
        TransformBox box = new TransformBox(horizontalBoxBounds, view, this::transform);

        // rotate the horizontal box into place
        double angle = userDrag.calcAngle();
        double rotCenterCoX = horizontalBoxBounds.getX();
        double rotCenterCoY = horizontalBoxBounds.getY() + horizontalBoxBounds.getHeight() / 2.0;
        box.saveState(); // so that transform works
        box.setAngle(angle);
        box.transform(AffineTransform.getRotateInstance(
                angle, rotCenterCoX, rotCenterCoY));
        return box;
    }

    public DebugNode getDebugNode() {
        DebugNode node = new DebugNode("StyledShape", this);
        node.addString("ShapeType", shapeType.toString());
        return node;
    }
}
