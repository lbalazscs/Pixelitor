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

import pixelitor.Composition;
import pixelitor.filters.gui.StrokeParam;
import pixelitor.filters.gui.StrokeSettings;
import pixelitor.filters.painters.AreaEffects;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.View;
import pixelitor.history.History;
import pixelitor.history.PartialImageEdit;
import pixelitor.layers.Drawable;
import pixelitor.tools.shapes.history.FinalizeShapeEdit;
import pixelitor.tools.shapes.history.StyledShapeEdit;
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
import java.awt.image.BufferedImage;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.tools.shapes.ShapesTool.STROKE_FOR_OPEN_SHAPES;
import static pixelitor.tools.shapes.TwoPointBasedPaint.NONE;

/**
 * A shape with associated stroke, fill and effects
 * that can paint itself on a given {@link Graphics2D}
 */
public class StyledShape implements Cloneable {
    private final ShapeSettings settings;
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

    // Not needed for the rendering, but needed
    // to restore the the stroke GUI after undo
    private StrokeSettings strokeSettings;

    private boolean insideBox;

    public StyledShape(ShapeSettings settings) {
        this.settings = settings;
        shapeType = settings.getSelectedType();
        setFillPaint(settings.getSelectedFillPaint());
        setStrokePaint(settings.getSelectedStrokePaint());
        setStroke(settings.getStroke());
        setEffects(settings.getEffects());
        this.strokeSettings = settings.getStrokeSettings();
        this.insideBox = false;
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
        if (transformedImDrag.isClick()) {
            return;
        }

        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        if (hasFillPaint()) {
            if (shapeType.isClosed()) {
                fillPaint.prepare(g, transformedImDrag);
                g.fill(shape);
                fillPaint.finish(g);
            } else if (!hasStrokePaint()) {
                // Special case: an open shape cannot be filled,
                // it can be only stroked, even if stroke is disabled.
                // So use the default stroke and the fill paint.
                g.setStroke(STROKE_FOR_OPEN_SHAPES);
                fillPaint.prepare(g, transformedImDrag);
                g.draw(shape);
                fillPaint.finish(g);
            }
        }

        if (hasStrokePaint()) {
            g.setStroke(stroke);
            strokePaint.prepare(g, transformedImDrag);
            g.draw(shape);
            strokePaint.finish(g);
        }

        if (effects != null) {
            if (hasFillPaint()) {
                effects.drawOn(g, shape);
            } else if (hasStrokePaint()) {
                // special case if there is only stroke:
                // apply the effect on the stroke outline
                Shape outline = stroke.createStrokedShape(shape);
                effects.drawOn(g, outline);
            } else { // "effects only"
                effects.drawOn(g, shape);
            }
        }
    }

    private boolean hasStrokePaint() {
        return strokePaint != NONE;
    }

    private boolean hasFillPaint() {
        return fillPaint != NONE;
    }

    public void setImDrag(ImDrag imDrag) {
        assert !insideBox;

        assert !imDrag.isClick() : "imDrag = " + imDrag.toString();

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

    private void setFillPaint(TwoPointBasedPaint fillPaint) {
        this.fillPaint = fillPaint;
    }

    private void setStrokePaint(TwoPointBasedPaint strokePaint) {
        this.strokePaint = strokePaint;
    }

    private void setStroke(Stroke stroke) {
        this.stroke = stroke;
    }

    private void setEffects(AreaEffects effects) {
        this.effects = effects;
    }

    private void setType(ShapeType shapeType) {
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

    public void finalizeTo(Composition comp, TransformBox transformBox) {
        Drawable dr = comp.getActiveDrawableOrNull();
        if (dr == null) { // can happen because this could be called via activeImageHasChanged
            // TODO how to handle changing the image when there is a box on a text layer?
            return;
        }

        Rectangle shapeBounds = shape.getBounds();
        int thickness = calcThickness();
        shapeBounds.grow(thickness, thickness);

        if (!shapeBounds.isEmpty()) {
            BufferedImage originalImage = dr.getImage();
            PartialImageEdit imageEdit = History.createPartialImageEdit(
                    shapeBounds, originalImage, dr, false, "Shape");
            if (imageEdit != null) {
                History.addEdit(new FinalizeShapeEdit(comp,
                        imageEdit, transformBox, this));
            }
        }

        paintShape(dr);

        comp.imageChanged();
        dr.updateIconImage();
    }

    private void paintShape(Drawable dr) {
        int tx = -dr.getTX();
        int ty = -dr.getTY();

        BufferedImage bi = dr.getImage();
        Graphics2D g2 = bi.createGraphics();
        g2.translate(tx, ty);

//        Composition comp = dr.getComp();
//        comp.applySelectionClipping(g2);

        paint(g2);
        g2.dispose();
    }

    private int calcThickness() {
        int thickness = 0;
        int extraStrokeThickness = 0;
        if (hasStrokePaint()) {
            StrokeParam strokeParam = settings.getStrokeParam();

            thickness = strokeParam.getStrokeWidth();

            StrokeType strokeType = strokeParam.getStrokeType();
            extraStrokeThickness = strokeType.getExtraThickness(thickness);
            thickness += extraStrokeThickness;
        }
        if (effects != null) {
            int effectThickness = effects.getMaxEffectThickness();
            // the extra stroke thickness must be added
            // because the effect can be on the stroke
            effectThickness += extraStrokeThickness;
            if (effectThickness > thickness) {
                thickness = effectThickness;
            }
        }

        return thickness;
    }

    @Override
    protected final StyledShape clone() {
        // this is used only for undo, it should be OK to share
        // all the references
        try {
            return (StyledShape) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); // can't happen
        }
    }

    public void regenerate(TransformBox transformBox) {
        StyledShape backup = clone();

        setType(settings.getSelectedType());
        setFillPaint(settings.getSelectedFillPaint());
        setStrokePaint(settings.getSelectedStrokePaint());

        setStroke(settings.getStroke());
        strokeSettings = settings.getStrokeSettings();
        settings.invalidateStroke();

        setEffects(settings.getEffects());


        transformBox.applyTransformation();

        Composition comp = ImageComponents.getActiveCompOrNull();
        History.addEdit(new StyledShapeEdit(comp, backup));
        comp.imageChanged();
    }

    public ShapeType getShapeType() {
        return shapeType;
    }

    public TwoPointBasedPaint getFillPaint() {
        return fillPaint;
    }

    public TwoPointBasedPaint getStrokePaint() {
        return strokePaint;
    }

    public StrokeSettings getStrokeSettings() {
        return strokeSettings;
    }

    public AreaEffects getEffects() {
        return effects;
    }

    public DebugNode getDebugNode() {
        DebugNode node = new DebugNode("StyledShape", this);
        node.addString("ShapeType", shapeType.toString());
        return node;
    }

    @Override
    public String toString() {
        return String.format("StyledShape, width = %.2f", strokeSettings.getWidth());
    }
}