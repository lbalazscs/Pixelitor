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

package pixelitor.tools.shapes;

import com.jhlabs.awt.WobbleStroke;
import pixelitor.AppContext;
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.filters.gui.ParamState;
import pixelitor.filters.gui.StrokeSettings;
import pixelitor.filters.painters.AreaEffects;
import pixelitor.gui.View;
import pixelitor.history.History;
import pixelitor.history.PartialImageEdit;
import pixelitor.layers.Drawable;
import pixelitor.layers.LayerButton;
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
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.Composition.UpdateActions.REPAINT;
import static pixelitor.colors.FgBgColors.getBGColor;
import static pixelitor.colors.FgBgColors.getFGColor;
import static pixelitor.tools.shapes.TwoPointPaintType.NONE;

/**
 * A shape with associated stroke, fill and effects
 * that can paint itself on a given {@link Graphics2D},
 * and can be transformed by a {@link TransformBox}
 */
public class StyledShape implements Transformable, Serializable, Cloneable {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final BasicStroke STROKE_FOR_OPEN_SHAPES = new BasicStroke(1);

    private ShapeType shapeType;
    private List<ParamState<?>> typeSettings;

    private Shape unTransformedShape; // the original shape, in image-space
    private Shape shape; // the current shape, in image-space

    // this doesn't change after the transform box appears,
    // so that another untransformed shape can be generated
    private Drag origDrag;

    // this is transformed as the box is manipulated, so that
    // the gradients move together with the box
    private Drag transformedDrag;

    private TwoPointPaintType fillPaint;
    private TwoPointPaintType strokePaint;
    private AreaEffects effects;

    private transient Stroke stroke;

    // Not needed for the rendering, but needed
    // to restore the stroke GUI state
    private StrokeSettings strokeSettings;

    private Color fgColor;
    private Color bgColor;

    public StyledShape(ShapesTool tool) {
        reloadType(tool);
        reloadFillPaint(tool);
        reloadStrokePaint(tool);
        reloadStroke(tool);
        reloadEffects(tool);

        reloadColors();
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

        if (fillPaint != NONE) {
            paintFill(g);
        }

        if (strokePaint != NONE) {
            paintStroke(g);
        }

        if (effects.isNotEmpty()) {
            paintEffects(g);
        }
    }

    public void paintIconThumbnail(Graphics2D g2, int thumbSize) {
        g2.setColor(LayerButton.UNSELECTED_COLOR);
        g2.fillRect(0, 0, thumbSize, thumbSize);
        g2.setColor(LayerButton.SELECTED_COLOR);
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        Drag drag;
        if (shapeType.isDirectional()) {
            double halfHeight = thumbSize / 2.0;
            drag = new Drag(0, halfHeight, thumbSize, halfHeight);
        } else {
            drag = new Drag(0, 0, thumbSize, thumbSize);
        }
        g2.fill(shapeType.createShape(drag, null));
    }

    private void paintFill(Graphics2D g) {
        if (shapeType.isClosed()) {
            fillPaint.prepare(g, transformedDrag, fgColor, bgColor);
            g.fill(shape);
            fillPaint.finish(g);
        } else if (strokePaint == NONE) {
            // Special case: an open shape can't be filled,
            // it can be only stroked, even if stroke is disabled.
            // So use the default stroke and the fill paint.
            g.setStroke(STROKE_FOR_OPEN_SHAPES);
            fillPaint.prepare(g, transformedDrag, fgColor, bgColor);
            g.draw(shape);
            fillPaint.finish(g);
        }
    }

    private void paintStroke(Graphics2D g) {
        g.setStroke(stroke);
        strokePaint.prepare(g, transformedDrag, fgColor, bgColor);
        g.draw(shape);
        strokePaint.finish(g);
    }

    private void paintEffects(Graphics2D g) {
        if (strokePaint != NONE) {
            if (fillPaint != NONE && shapeType.isClosed()) {
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

    // called during the initial drag, when there is no transform box yet
    public void updateFromDrag(Drag drag, ShapesTool tool) {
        assert !tool.hasBox();

        if (drag.isClick()) {
            return;
        }

        origDrag = drag;

        // during the initial drag it can use the tool's settings directly
        ShapeTypeSettings settings = tool.getSettingsFor(shapeType);
        unTransformedShape = shapeType.createShape(drag, settings);

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

    private void reloadType(ShapesTool tool) {
        ShapeType type = tool.getSelectedType();
        boolean typeChanged = this.shapeType != type;
        this.shapeType = type;

        if (typeChanged) {
            reloadTypeSettings(tool);
        }
    }

    private void reloadTypeSettings(ShapesTool tool) {
        if (shapeType.hasSettings()) {
            typeSettings = tool.getSettingsFor(shapeType).copyState();
        } else {
            typeSettings = null;
        }
    }

    private void reloadFillPaint(ShapesTool tool) {
        this.fillPaint = tool.getSelectedFillPaint();
    }

    private void reloadStrokePaint(ShapesTool tool) {
        this.strokePaint = tool.getSelectedStrokePaint();
    }

    private void reloadStroke(ShapesTool tool) {
        this.stroke = tool.getStroke();
        strokeSettings = tool.getStrokeSettings();
    }

    private void reloadEffects(ShapesTool tool) {
        this.effects = tool.getEffects();
    }

    private void reloadColors() {
        this.fgColor = getFGColor();
        this.bgColor = getBGColor();
    }

    // called after a change in the shape type or settings
    private void recreateShapes(ShapesTool tool, TransformBox box, String editName) {
        assert origDrag != null : "no origDrag, edit = " + editName;

        ShapeTypeSettings settings = tool.getSettingsFor(shapeType);
        if (shapeType.isDirectional()) {
            // make sure that the new directional shape is drawn
            // along the direction of the existing box
            // TODO this still ignores the height of the current box
            unTransformedShape = shapeType.createShape(origDrag.getCenterHorizontalDrag(), settings);
        } else {
            unTransformedShape = shapeType.createShape(origDrag, settings);
        }

        // Should always be non-null, unless the user somehow manages to change the
        // type via keyboard during the initial drag, but leave the check for safety.
        if (box != null) {
            // also recreate the transformed shape
            box.applyTransform();
        }
    }

    /**
     * Creates a transform box suitable for this styled shape.
     */
    public TransformBox createBox(View view) {
        // This method works correctly only at the end of the first user drag.
        assert origDrag == transformedDrag;

        TransformBox box;
        if (shapeType.isDirectional()) {
            // for directional shapes, zero-width or zero-height drags are allowed
            box = createRotatedBox(origDrag, view);
        } else {
            if (origDrag.hasZeroWidth() || origDrag.hasZeroHeight()) {
                return null;
            }
            Rectangle origCoRect = origDrag.toPosCoRect();
            assert !origCoRect.isEmpty() : "drag = " + origDrag;
            box = new TransformBox(origCoRect, view, this);
        }
        return box;
    }

    private TransformBox createRotatedBox(Drag drag, View view) {
        ShapeTypeSettings settings = null;
        if (shapeType.hasSettings()) {
            // this instance is independent of the tool's type settings
            // because this code must work when deserializing inactive shape layers
            settings = shapeType.createSettings();
            settings.loadStateFrom(typeSettings);
        }

        // Set the original shape to the horizontal shape.
        // It could also be rotated backwards with an AffineTransform.
        unTransformedShape = shapeType.createHorizontalShape(drag, settings);

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
        Rectangle2D horBoxBounds = new Rectangle.Double(
            drag.getCoStartX(),
            drag.getCoStartY() - coDist * Shapes.UNIT_ARROW_HEAD_WIDTH / 2.0,
            coDist,
            coDist * Shapes.UNIT_ARROW_HEAD_WIDTH);
        assert !horBoxBounds.isEmpty();
        TransformBox box = new TransformBox(horBoxBounds, view, this);

        // rotate the horizontal box into place
        double angle = drag.calcAngle();
        double rotCenterCoX = horBoxBounds.getX();
        double rotCenterCoY = horBoxBounds.getY() + horBoxBounds.getHeight() / 2.0;
        box.saveState(); // so that transform works
        box.setAngle(angle);
        box.coTransform(AffineTransform.getRotateInstance(
            angle, rotCenterCoX, rotCenterCoY));
        return box;
    }

    public void rasterizeTo(Composition comp, TransformBox transformBox, ShapesTool tool) {
        assert shape != null;
        assert !tool.isEditingShapesLayer();

        PartialImageEdit imageEdit = null;
        Drawable dr = comp.getActiveDrawable();
        if (dr != null) { // a text layer could be active
            Rectangle shapeBounds = shape.getBounds();
            int thickness = 1 + (int) tool.calcThickness();
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
            paintOnDrawable(dr, tool);
            comp.update();
            dr.updateIconImage();
        } else {
            // a repaint is necessary even if the box is outside the canvas
            comp.repaint();
        }
    }

    private void paintOnDrawable(Drawable dr, ShapesTool tool) {
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

    @Override
    public final StyledShape clone() {
        // this is used only for undo, it should be OK to share
        // all the references
        try {
            return (StyledShape) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); // can't happen
        }
    }

    // TODO a hack method
    public void regenerateAll(TransformBox box, ShapesTool tool) {
        regenerate(box, tool, ShapesTool.CHANGE_SHAPE_TYPE);
        regenerate(box, tool, ShapesTool.CHANGE_SHAPE_TYPE_SETTINGS);
        regenerate(box, tool, ShapesTool.CHANGE_SHAPE_FILL);
        regenerate(box, tool, ShapesTool.CHANGE_SHAPE_STROKE);
        regenerate(box, tool, ShapesTool.CHANGE_SHAPE_STROKE_SETTINGS);
        regenerate(box, tool, ShapesTool.CHANGE_SHAPE_EFFECTS);
        regenerate(box, tool, ShapesTool.CHANGE_SHAPE_COLORS);
        regenerate(box, tool, ShapesTool.CHANGE_SHAPE_STROKE_SETTINGS);
    }

    public void regenerate(TransformBox box, ShapesTool tool, String editName) {
        StyledShape backup = clone();

        // calculate the new transformed shape
        switch (editName) {
            case ShapesTool.CHANGE_SHAPE_TYPE -> {
                reloadType(tool);
                recreateShapes(tool, box, editName);
            }
            case ShapesTool.CHANGE_SHAPE_TYPE_SETTINGS -> {
                reloadTypeSettings(tool);
                recreateShapes(tool, box, editName);
            }
            case ShapesTool.CHANGE_SHAPE_FILL -> reloadFillPaint(tool);
            case ShapesTool.CHANGE_SHAPE_STROKE -> reloadStrokePaint(tool);
            case ShapesTool.CHANGE_SHAPE_STROKE_SETTINGS -> {
                reloadStroke(tool);
//                tool.invalidateStroke();
            }
            case ShapesTool.CHANGE_SHAPE_EFFECTS -> reloadEffects(tool);
            case ShapesTool.CHANGE_SHAPE_COLORS -> reloadColors();
            default -> throw new IllegalStateException("Unexpected edit: " + editName);
        }

        var comp = Views.getActiveComp();
        History.add(new StyledShapeEdit(editName, comp, backup));
        comp.update();
    }

    public ShapeType getShapeType() {
        return shapeType;
    }

    public List<ParamState<?>> getShapeTypeSettings() {
        return typeSettings;
    }

    public TwoPointPaintType getFillPaint() {
        return fillPaint;
    }

    public TwoPointPaintType getStrokePaint() {
        return strokePaint;
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

    public Color getBgColor() {
        return bgColor;
    }

    /**
     * Return a shape that is guaranteed to be closed and corresponds
     * to the displayed pixels. The effects are ignored and the stroke
     * is considered only for open shapes.
     *
     * @param tool
     */
    public Shape getShapeForSelection(ShapesTool tool) {
        if (tool.getSelectedType().isClosed()) {
            return shape;
        } else if (tool.hasStroke()) {
            // the shape is not closed, but there is a stroke
            return tool.getStroke().createStrokedShape(shape);
        } else {
            // the shape is not closed, and there is no stroke
            return STROKE_FOR_OPEN_SHAPES.createStrokedShape(shape);
        }
    }

    @Override
    public DebugNode createDebugNode() {
        var node = new DebugNode("styled shape", this);

        node.addString("origDrag",
            origDrag == null ? "null" : origDrag.toString());
        node.addString("transformedDrag",
            transformedDrag == null ? "null" : transformedDrag.toString());

        node.addString("type", shapeType.toString());
        node.addString("fillPaint", fillPaint.toString());
        node.addString("strokePaint", strokePaint.toString());
        node.add(effects.createDebugNode());
        node.addColor("FG", fgColor);
        node.addColor("BG", bgColor);

        return node;
    }

    // used only for debugging
    @Override
    public String toString() {
        return "StyledShape, type = " + shapeType;
    }
}