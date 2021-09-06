/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.awt.WobbleStroke;
import pixelitor.AppContext;
import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.filters.gui.StrokeParam;
import pixelitor.filters.gui.StrokeSettings;
import pixelitor.filters.painters.AreaEffects;
import pixelitor.gui.View;
import pixelitor.history.History;
import pixelitor.history.PartialImageEdit;
import pixelitor.layers.Drawable;
import pixelitor.tools.Tools;
import pixelitor.tools.shapes.history.FinalizeShapeEdit;
import pixelitor.tools.shapes.history.StyledShapeEdit;
import pixelitor.tools.transform.TransformBox;
import pixelitor.tools.transform.Transformable;
import pixelitor.tools.util.Drag;
import pixelitor.utils.Shapes;
import pixelitor.utils.debug.DebugNode;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.lang.String.format;
import static pixelitor.Composition.UpdateActions.REPAINT;
import static pixelitor.colors.FgBgColors.getBGColor;
import static pixelitor.colors.FgBgColors.getFGColor;
import static pixelitor.tools.shapes.TwoPointPaintType.NONE;

/**
 * A shape with associated stroke, fill and effects
 * that can paint itself on a given {@link Graphics2D},
 * and can be transformed by a {@link TransformBox}
 */
public class StyledShape implements Cloneable, Transformable {
    private static final BasicStroke STROKE_FOR_OPEN_SHAPES = new BasicStroke(1);

    private ShapeType shapeType;
    private ShapeTypeSettings shapeTypeSettings;

    // The styled shape is initially defined by a user drag,
    // and then by a transform box
    private boolean insideBox;

    private Shape unTransformedShape; // the original shape, in image-space
    private Shape shape; // the current shape, in image-space

    // this doesn't change after the transform box appears,
    // so that another untransformed shape can be generated
    private Drag origDrag;

    // this is transformed as the box is manipulated, so that
    // the gradients move together with the box
    private Drag transformedDrag;

    private TwoPointPaintType fillPaintType;
    private TwoPointPaintType strokePaintType;
    private AreaEffects effects;

    private Stroke stroke;

    // Not needed for the rendering, but needed
    // to restore the the stroke GUI after undo
    private StrokeSettings strokeSettings;

    private Color fgColor;
    private Color bgColor;

    public StyledShape(ShapesTool tool) {
        setType(tool.getSelectedType(), tool);

        setFillPaintType(tool.getSelectedFillPaint());
        setStrokePaintType(tool.getSelectedStrokePaint());
        setStroke(tool.getStroke());
        setEffects(tool.getEffects());
        strokeSettings = tool.getStrokeSettings();
        insideBox = false;

        fgColor = getFGColor();
        bgColor = getBGColor();
    }

    /**
     * Paints this object on the given Graphics2D, which is expected to be
     * in image space.
     */
    public void paint(Graphics2D g) {
        if (transformedDrag == null) {
            // this object is created when the mouse is pressed, but
            // it can be painted only after the first drag events arrive
            return;
        }
        if (transformedDrag.isImClick()) {
            return;
        }
        if (shape == null) { // should not happen
            if (AppContext.isDevelopment()) {
                throw new IllegalStateException();
            }
            return;
        }

        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        if (hasFill()) {
            paintFill(g);
        }

        if (hasStroke()) {
            paintStroke(g);
        }

        if (effects.isNotEmpty()) {
            paintEffects(g);
        }
    }

    private void paintFill(Graphics2D g) {
        if (shapeType.isClosed()) {
            fillPaintType.prepare(g, transformedDrag);
            g.fill(shape);
            fillPaintType.finish(g);
        } else if (!hasStroke()) {
            // Special case: an open shape can't be filled,
            // it can be only stroked, even if stroke is disabled.
            // So use the default stroke and the fill paint.
            g.setStroke(STROKE_FOR_OPEN_SHAPES);
            fillPaintType.prepare(g, transformedDrag);
            g.draw(shape);
            fillPaintType.finish(g);
        }
    }

    private void paintStroke(Graphics2D g) {
        g.setStroke(stroke);
        strokePaintType.prepare(g, transformedDrag);
        g.draw(shape);
        strokePaintType.finish(g);
    }

    private void paintEffects(Graphics2D g) {
        if (hasStroke()) {
            if (hasFill() && shapeType.isClosed()) {
                paintEffectsForFilledStrokedShape(g);
            } else {
                paintStrokeOutlineEffects(g);
            }
        } else {
            paintEffectsNoStroke(g);
        }
    }

    private void paintEffectsForFilledStrokedShape(Graphics2D g) {
        // add the outline area of the stroke to the shape area
        // to get the shape for the effects, but these Area operations
        // could be too slow for the WobbleStroke
        if (stroke instanceof WobbleStroke) {
            // give up, just draw something
            effects.drawOn(g, shape);
        } else {
            // do the correct thing
            Shape strokeOutline = stroke.createStrokedShape(shape);
            Area strokeOutlineArea = new Area(strokeOutline);
            Area combined = new Area(shape);
            combined.add(strokeOutlineArea);
            effects.drawOn(g, combined);
        }
    }

    private void paintStrokeOutlineEffects(Graphics2D g) {
        if (stroke instanceof WobbleStroke) {
            // be careful and consistent with the behavior above
            effects.drawOn(g, shape);
        } else {
            // apply the effects on the stroke outline
            Shape outline = stroke.createStrokedShape(shape);
            effects.drawOn(g, outline);
        }
    }

    private void paintEffectsNoStroke(Graphics2D g) {
        if (shapeType.isClosed()) {
            effects.drawOn(g, shape); // simplest case
        } else {
            Shape defaultOutline = STROKE_FOR_OPEN_SHAPES.createStrokedShape(shape);
            effects.drawOn(g, defaultOutline);
        }
    }

    private boolean hasStroke() {
        return strokePaintType != NONE;
    }

    private boolean hasFill() {
        return fillPaintType != NONE;
    }

    // called during the initial drag, when there is no transform box yet
    public void updateFromDrag(Drag drag) {
        assert !insideBox;

        if (drag.isClick()) {
            return;
        }

        origDrag = drag;
        unTransformedShape = shapeType.createShape(drag, shapeTypeSettings);

        // since there is no transform box yet
        transformedDrag = drag;
        shape = unTransformedShape;
    }

    @Override
    public void transform(AffineTransform at) {
        shape = at.createTransformedShape(unTransformedShape);
        transformedDrag = origDrag.transformedCopy(at);
    }

    @Override
    public void updateUI(View view) {
        view.getComp().update(REPAINT);
    }

    private void setFillPaintType(TwoPointPaintType fillPaintType) {
        this.fillPaintType = fillPaintType;
    }

    private void setStrokePaintType(TwoPointPaintType strokePaintType) {
        this.strokePaintType = strokePaintType;
    }

    private void setStroke(Stroke stroke) {
        this.stroke = stroke;
    }

    private void setEffects(AreaEffects effects) {
        this.effects = effects;
    }

    private void changeTypeInBox(ShapesTool tool) {
        assert insideBox;
        ShapeType selectedType = tool.getSelectedType();
        setType(selectedType, tool);

        if (shapeType.isDirectional()) {
            // make sure that the new directional shape is drawn
            // along the direction of the existing box
            // TODO this still ignores the height of the current box
            unTransformedShape = shapeType.createShape(origDrag.getCenterHorizontalDrag(), shapeTypeSettings);
        } else {
            unTransformedShape = shapeType.createShape(origDrag, shapeTypeSettings);
        }
        // the new transformed shape will be calculated later,
        // after the other parameters have been set
    }

    private void setType(ShapeType shapeType, ShapesTool tool) {
        boolean typeChanged = this.shapeType != shapeType;
        this.shapeType = shapeType;

        if (typeChanged) {
            if (shapeType.hasSettings()) {
                // a copy is made so that each StyledShape can have independent settings
                ShapeTypeSettings copyOfDefaults = tool.getDefaultSettingsFor(shapeType).copy();
                setTypeSettings(copyOfDefaults);
            } else {
                setTypeSettings(null);
            }
        }
    }

    private void setTypeSettings(ShapeTypeSettings settings) {
        shapeTypeSettings = settings;
        if (settings != null) {
            settings.setAdjustmentListener(() ->
                regenerate(Tools.SHAPES.getTransformBox(), Tools.SHAPES, ShapesTool.CHANGE_SHAPE_TYPE));
        }
    }

    public TransformBox createBox(Drag drag, View view) {
        assert !insideBox;
        insideBox = true;

        TransformBox box;
        if (shapeType.isDirectional()) {
            // for directional shapes, zero-width or zero-height drags are allowed
            box = createRotatedBox(drag, view);
        } else {
            if (drag.hasZeroWidth() || drag.hasZeroHeight()) {
                return null;
            }
            Rectangle origCoRect = drag.toPosCoRect();
            assert !origCoRect.isEmpty() : "drag = " + drag;
            box = new TransformBox(origCoRect, view, this);
        }
        return box;
    }

    private TransformBox createRotatedBox(Drag drag, View view) {
        // First calculate the settings for a horizontal box.
        // The box is in component space, everything else is in image space.

        // Set the original shape to the horizontal shape.
        // It could also be rotated backwards with an AffineTransform.
        unTransformedShape = shapeType.createHorizontalShape(drag, shapeTypeSettings);

        // Set the original drag to the diagonal of the back-rotated transform box,
        // so that after a shape-type change the new shape is created correctly
        double imDragDist = drag.calcImDist();
        double halfImHeight = imDragDist * Shapes.UNIT_ARROW_HEAD_WIDTH / 2.0;
        origDrag = new Drag(
            drag.getStartX(),
            drag.getStartY() - halfImHeight,
            drag.getStartX() + imDragDist,
            drag.getStartY() + halfImHeight);

        // create the horizontal box
        double coDist = drag.calcCoDist();
        Rectangle2D horizontalBoxBounds = new Rectangle.Double(
            drag.getCoStartX(),
            drag.getCoStartY() - coDist * Shapes.UNIT_ARROW_HEAD_WIDTH / 2.0,
            coDist,
            coDist * Shapes.UNIT_ARROW_HEAD_WIDTH);
        assert !horizontalBoxBounds.isEmpty();
        TransformBox box = new TransformBox(horizontalBoxBounds, view, this);

        // rotate the horizontal box into place
        double angle = drag.calcAngle();
        double rotCenterCoX = horizontalBoxBounds.getX();
        double rotCenterCoY = horizontalBoxBounds.getY() + horizontalBoxBounds.getHeight() / 2.0;
        box.saveState(); // so that transform works
        box.setAngle(angle);
        box.transform(AffineTransform.getRotateInstance(
            angle, rotCenterCoX, rotCenterCoY));
        return box;
    }

    public void rasterizeTo(Composition comp, TransformBox transformBox, ShapesTool tool) {
        assert shape != null;

        PartialImageEdit imageEdit = null;
        Drawable dr = comp.getActiveDrawable();
        if (dr != null) { // a text layer could be active
            Rectangle shapeBounds = shape.getBounds();
            int thickness = calcThickness(tool);
            shapeBounds.grow(thickness, thickness);

            if (!shapeBounds.isEmpty()) {
                BufferedImage originalImage = dr.getImage();
                imageEdit = History.createPartialImageEdit(
                    shapeBounds, originalImage, dr, false, "Shape");
            }
        }

        // must be added even if there is no image edit
        // to manage the shapes tool state changes
        History.add(new FinalizeShapeEdit(comp,
            imageEdit, transformBox, this));

        if (imageEdit != null) {
            paintOnDrawable(dr);
            comp.update();
            dr.updateIconImage();
        } else {
            // a repaint is necessary even if the box is outside the canvas
            comp.repaint();
        }

        saveShapeTypeSettingsAsDefault(tool);
    }

    private void saveShapeTypeSettingsAsDefault(ShapesTool tool) {
        if (shapeTypeSettings != null) {
            shapeTypeSettings.setAdjustmentListener(null);
            tool.getDefaultShapeTypeSettings().put(shapeType, shapeTypeSettings);
        }
    }

    private void paintOnDrawable(Drawable dr) {
        int tx = -dr.getTx();
        int ty = -dr.getTy();

        BufferedImage bi = dr.getImage();
        Graphics2D g2 = bi.createGraphics();
        g2.translate(tx, ty);

        var comp = dr.getComp();
        comp.applySelectionClipping(g2);

        paint(g2);
        g2.dispose();
    }

    /**
     * Calculate the extra thickness around the shape for the undo area
     */
    private int calcThickness(ShapesTool tool) {
        int thickness = 0;
        int extraStrokeThickness = 0;
        if (hasStroke()) {
            StrokeParam strokeParam = tool.getStrokeParam();

            thickness = strokeParam.getStrokeWidth();

            StrokeType strokeType = strokeParam.getStrokeType();
            extraStrokeThickness = strokeType.getExtraThickness(thickness);
            thickness += extraStrokeThickness;
        }
        if (effects.isNotEmpty()) {
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

    public void regenerate(TransformBox box, ShapesTool tool, String editName) {
        StyledShape backup = clone();

        // calculate the new transformed shape
        switch (editName) {
            case ShapesTool.CHANGE_SHAPE_TYPE -> {
                changeTypeInBox(tool);
                box.applyTransform();
            }
            case ShapesTool.CHANGE_SHAPE_FILL -> setFillPaintType(tool.getSelectedFillPaint());
            case ShapesTool.CHANGE_SHAPE_STROKE -> setStrokePaintType(tool.getSelectedStrokePaint());
            case ShapesTool.CHANGE_SHAPE_STROKE_SETTINGS -> {
                setStroke(tool.getStroke());
                strokeSettings = tool.getStrokeSettings();
                tool.invalidateStroke();
            }
            case ShapesTool.CHANGE_SHAPE_EFFECTS -> setEffects(tool.getEffects());
            case ShapesTool.CHANGE_SHAPE_COLORS -> {
                setFgColor(getFGColor());
                setBgColor(getBGColor());
            }
            default -> throw new IllegalStateException("Unexpected edit: " + editName);
        }

        var comp = OpenImages.getActiveComp();
        History.add(new StyledShapeEdit(editName, comp, backup));
        comp.update();
    }

    public ShapeType getShapeType() {
        return shapeType;
    }

    public ShapeTypeSettings getShapeTypeSettings() {
        return shapeTypeSettings;
    }

    public TwoPointPaintType getFillPaintType() {
        return fillPaintType;
    }

    public TwoPointPaintType getStrokePaintType() {
        return strokePaintType;
    }

    public StrokeSettings getStrokeSettings() {
        return strokeSettings;
    }

    public AreaEffects getEffects() {
        return effects;
    }

    public Color getFgColor() {
        return fgColor;
    }

    private void setFgColor(Color fgColor) {
        this.fgColor = fgColor;
    }

    public Color getBgColor() {
        return bgColor;
    }

    private void setBgColor(Color bgColor) {
        this.bgColor = bgColor;
    }

    /**
     * Return a shape that is guaranteed to be closed and corresponds
     * to the displayed pixels. The effects are ignored and the stroke
     * is considered only for open shapes.
     */
    public Shape getShapeForSelection() {
        if (shapeType.isClosed()) {
            return shape;
        } else if (hasStroke()) {
            // the shape is not closed, but there is a stroke
            return stroke.createStrokedShape(shape);
        } else {
            // the shape is not closed, and there is no stroke
            return STROKE_FOR_OPEN_SHAPES.createStrokedShape(shape);
        }
    }

    @Override
    public DebugNode createDebugNode() {
        var node = new DebugNode("styled shape", this);
        node.addString("type", shapeType.toString());
        return node;
    }

    @Override
    public String toString() {
        return format("StyledShape, width = %.2f", strokeSettings.width());
    }
}