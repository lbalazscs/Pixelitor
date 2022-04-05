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
import pixelitor.filters.gui.StrokeParam;
import pixelitor.filters.gui.StrokeSettings;
import pixelitor.filters.painters.AreaEffects;
import pixelitor.gui.View;
import pixelitor.history.History;
import pixelitor.history.PartialImageEdit;
import pixelitor.layers.Drawable;
import pixelitor.layers.LayerButton;
import pixelitor.tools.Tools;
import pixelitor.tools.shapes.history.RasterizeShapeEdit;
import pixelitor.tools.shapes.history.StyledShapeEdit;
import pixelitor.tools.transform.TransformBox;
import pixelitor.tools.transform.Transformable;
import pixelitor.tools.util.Drag;
import pixelitor.utils.Shapes;
import pixelitor.utils.TaperingStroke;
import pixelitor.utils.debug.DebugNode;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
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

    enum State {CREATED, DESERIALIZED, SHAPE_SET, BOX_CREATED}

    private transient State state;

    private static final boolean DEBUG_PAINT = false;

    private ShapeType shapeType;
    private List<ParamState<?>> typeSettings;

    private Shape origShape; // the original shape, in image-space
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

    // TODO this is a hack to enable a shapes layer to invalidate the image cache
    //  when the styled shape changes. Instead of this, either the need for an
    //  image cache should be eliminated or the image should be cached in this class.
    private transient Runnable changeListener;

    public StyledShape(ShapesTool tool) {
        reloadType(tool);
        reloadFillPaint(tool);
        reloadStrokePaint(tool);
        reloadStroke(tool);
        reloadEffects(tool);

        reloadColors();
        state = State.CREATED;
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        if (strokePaint != NONE) {
            // A stroke might be needed before this object
            // has a chance to load it from the tool.
            StrokeParam p = new StrokeParam("");
            p.loadStateFrom(strokeSettings, false);
            stroke = p.createStroke();
        } else {
            stroke = null;
        }
        state = State.DESERIALIZED;
        changeListener = null;
        assert checkConsistency();
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        assert checkConsistency();
        out.defaultWriteObject();
    }

    /**
     * Paints this object on the given Graphics2D, which is expected to be in image space.
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

        if (DEBUG_PAINT) {
            // draw the original shape with a semitransparent black
            Composite oldComposite = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            g.setColor(Color.BLACK);
            g.fill(origShape);
            g.setComposite(oldComposite);
        }

        if (fillPaint != NONE) {
            paintFill(g);
        }

        if (strokePaint != NONE) {
            paintStroke(g);
        }

        if (effects.isNotEmpty()) {
            paintEffects(g);
        }

        if (DEBUG_PAINT) {
            origDrag.drawImDirectionArrow(g);
            transformedDrag.drawImDirectionArrow(g);
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
        fillPaint.prepare(g, transformedDrag, fgColor, bgColor);
        g.fill(shape);
        fillPaint.finish(g);
    }

    private void paintStroke(Graphics2D g) {
        g.setStroke(stroke);
        strokePaint.prepare(g, transformedDrag, fgColor, bgColor);
        g.draw(shape);
        strokePaint.finish(g);
    }

    private void paintEffects(Graphics2D g) {
        if (strokePaint != NONE) {
            if (fillPaint != NONE) {
                paintEffectsForFilledStrokedShape(g);
            } else {
                paintStrokeOutlineEffects(g);
            }
        } else {
            paintEffectsNoStroke(g);
        }
    }

    private void paintEffectsForFilledStrokedShape(Graphics2D g) {
        // Add the outline area of the stroke to the shape area
        // to get the shape for the effects, but these Area operations
        // could be too slow for the WobbleStroke.
        // TODO Also some shapes (Line, Cat, Bat + sometimes rounded rectangle) trigger
        //  https://bugs.openjdk.java.net/browse/JDK-6357341
        //  with Tapering stroke + neon border
        var strokeClass = stroke.getClass();
        if (strokeClass == WobbleStroke.class || (strokeClass == TaperingStroke.class && shapeType.hasAreaProblem())) {
            // give up, draw the effect directly on the original shape,
            // ignoring the stroke's width
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
        effects.drawOn(g, shape); // simplest case
    }

    // called during the initial drag, when there is no transform box yet
    public void updateFromDrag(Drag drag, boolean altDown, boolean shiftDown) {
        assert !Tools.SHAPES.hasBox();
        assert !drag.isImClick();

        drag.setStartFromCenter(altDown);
        if (shapeType.isDirectional()) {
            // For directional shapes it's useful to have the
            // ability to drag exactly horizontally or vertically.
            drag.setConstrained(shiftDown);
        } else {
            drag.setEquallySized(shiftDown);
        }

        origDrag = drag;

        // during the initial drag it can use the tool's settings directly
        ShapeTypeSettings settings = Tools.SHAPES.getSettingsFor(shapeType);
        origShape = shapeType.createShape(drag, settings);

        // since there is no transform box yet
        this.transformedDrag = drag;
        shape = origShape;

        state = State.SHAPE_SET;
        assert checkConsistency();
    }

    @Override
    public void imTransform(AffineTransform at) {
        shape = at.createTransformedShape(origShape);
        transformedDrag = origDrag.imTransformedCopy(at);

        notifyChangeListener();
        assert checkConsistency();
    }

    @Override
    public void updateUI(View view) {
        view.getComp().update(REPAINT);
    }

    private void reloadType(ShapesTool tool) {
        ShapeType newType = tool.getSelectedType();
        boolean typeChanged = this.shapeType != newType;
        this.shapeType = newType;

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
        if (stroke == null) { // can happen after deserialization
            // This ignores the deserialized strokeSettings, but it's not a problem:
            // if there is no stroke, then the disabled stroke settings aren't important.
            reloadStroke(tool);
        }
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
    private void recreateShape(ShapesTool tool, TransformBox box, String editName) {
        assert origDrag != null : "no origDrag, edit = " + editName;

        ShapeTypeSettings settings = tool.getSettingsFor(shapeType);
        if (shapeType.isDirectional()) {
            // make sure that the new directional shape is drawn
            // along the direction of the existing box
            // TODO this still ignores the height of the current box
            origShape = shapeType.createShape(origDrag.getCenterHorizontalDrag(), settings);
        } else {
            origShape = shapeType.createShape(origDrag, settings);
        }
        assert origShape != null;

        // Should always be non-null, unless the user somehow manages to change the
        // type via keyboard during the initial drag, but leave the check for safety.
        if (box != null) {
            // also recreate the transformed shape
            box.applyTransform();
        }
    }

    public void resetTransform() {
        assert transformedDrag != null : "state = " + state;
        origDrag = transformedDrag;
        origShape = shape;

        assert checkConsistency();
    }

    /**
     * Creates a transform box suitable for this styled shape.
     */
    public TransformBox createBox(View view) {
        assert isInitialized();
        if (origDrag != transformedDrag) {
            // we aren't at the end of the first user drag
            resetTransform();
        }

        TransformBox box;
        if (shapeType.isDirectional()) {
            // for directional shapes, zero-width or zero-height drags are allowed
            box = createRotatedBox(view, origDrag.calcAngle());
        } else {
            if (origDrag.hasZeroImWidth() || origDrag.hasZeroImHeight()) {
                return null;
            }
            Rectangle2D origImRect = origDrag.toPosImRect();
            assert !origImRect.isEmpty() : "drag = " + origDrag;
            box = new TransformBox(origImRect, view, this);
        }
        state = State.BOX_CREATED;
        return box;
    }

    private TransformBox createRotatedBox(View view, double angle) {
//        System.out.printf("StyledShape::createRotatedBox: angle = %.2f, transformedDrag = %s%n",
//            angle, transformedDrag.toString());

        ShapeTypeSettings settings = null;
        if (shapeType.hasSettings()) {
            // this instance is independent of the tool's type settings
            // because this code must work when deserializing inactive shape layers
            settings = shapeType.createSettings();
            settings.loadStateFrom(typeSettings);
        }

        // The rotated box is created in 3 steps:
        // 1) Rotate the original shape backwards.
        // 2) Create a horizontal box for it.
        // 3) Rotate the box (an implicitly the shape) forwards.

        double imDist = transformedDrag.calcImDist();
        double dragStartX = transformedDrag.getStartXFromCenter();
        double dragStartY = transformedDrag.getStartYFromCenter();
        Rectangle2D horBoxImBounds = new Rectangle.Double(
            dragStartX,
            dragStartY - imDist * Shapes.UNIT_ARROW_HEAD_WIDTH / 2.0,
            imDist,
            imDist * Shapes.UNIT_ARROW_HEAD_WIDTH);
        assert !horBoxImBounds.isEmpty();

        double rotCenterImX = horBoxImBounds.getX();
        double rotCenterImY = horBoxImBounds.getY() + horBoxImBounds.getHeight() / 2.0;

        // Set the original shape to the horizontal shape.
        // It could also be rotated backwards with an AffineTransform.
        origShape = shapeType.createShape(transformedDrag, settings);
        assert origShape != null;
        AffineTransform rotBack = AffineTransform.getRotateInstance(-angle, rotCenterImX, rotCenterImY);
        origShape = rotBack.createTransformedShape(origShape);
        assert origShape != null;

        // Set the original drag to the diagonal of the back-rotated transform box,
        // so that after a shape-type change the new shape is created correctly
        double halfImHeight = imDist * Shapes.UNIT_ARROW_HEAD_WIDTH / 2.0;
        origDrag = new Drag(
            dragStartX,
            dragStartY - halfImHeight,
            dragStartX + imDist,
            dragStartY + halfImHeight);

        TransformBox box = new TransformBox(horBoxImBounds, view, this);

        // rotate the horizontal box into place
        box.saveImState(); // so that transform works
        box.setAngle(angle);

        // this was coTransform before
        box.imTransform(AffineTransform.getRotateInstance(
            angle, rotCenterImX, rotCenterImY));
        return box;
    }

    public void rasterizeTo(Composition comp, TransformBox transformBox, ShapesTool tool) {
        assert checkConsistency();

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
        History.add(new RasterizeShapeEdit(comp, imageEdit, transformBox, this));

        if (imageEdit != null) {
            paintOnDrawable(dr);
            comp.update();
            dr.updateIconImage();
        } else {
            // a repaint is necessary even if the box is outside the canvas
            comp.repaint();
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

    @Override
    public final StyledShape clone() {
        // this is used only for undo, it should be OK to share
        // all the references
        assert checkConsistency();
        try {
            StyledShape clone = (StyledShape) super.clone();
            assert clone.checkConsistency();
            return clone;
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
                recreateShape(tool, box, editName);
            }
            case ShapesTool.CHANGE_SHAPE_TYPE_SETTINGS -> {
                reloadTypeSettings(tool);
                recreateShape(tool, box, editName);
            }
            case ShapesTool.CHANGE_SHAPE_FILL -> reloadFillPaint(tool);
            case ShapesTool.CHANGE_SHAPE_STROKE -> reloadStrokePaint(tool);
            case ShapesTool.CHANGE_SHAPE_STROKE_SETTINGS -> reloadStroke(tool);
            case ShapesTool.CHANGE_SHAPE_EFFECTS -> reloadEffects(tool);
            case ShapesTool.CHANGE_SHAPE_COLORS -> reloadColors();
            default -> throw new IllegalStateException("Unexpected edit: " + editName);
        }

        var comp = Views.getActiveComp();
        History.add(new StyledShapeEdit(editName, comp, backup));
        comp.update();

        notifyChangeListener();
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

    public Shape getShapeForSelection() {
        return shape;
    }

    public boolean isInitialized() {
        boolean initialized = state != State.CREATED;
        assert initialized == (origDrag != null);
        assert initialized == (shape != null);
        return initialized;
    }

    public Rectangle getContentBounds() {
        return shape.getBounds();
    }

    public boolean hasBlendingIssue() {
        // for some reason the JDK built-in gradients
        // don't blend with the custom blending modes
        return fillPaint.hasBlendingIssue() || strokePaint.hasBlendingIssue();
    }

    public void setChangeListener(Runnable changeListener) {
        this.changeListener = changeListener;
    }

    private void notifyChangeListener() {
        if (changeListener != null) {
            changeListener.run();
        }
    }

    public boolean containsPoint(Point p) {
        return shape.contains(p);
    }

    public boolean checkConsistency() {
        // TODO CREATED, but not initialized styled shapes
        //  shouldn't be put in shape layers
        if (state == State.CREATED) {
            return true;
        }

        if (origShape == null) {
            throw new IllegalStateException("state = " + state);
        }
        if (shape == null) {
            throw new IllegalStateException("state = " + state);
        }
        return true;
    }

    @Override
    public DebugNode createDebugNode() {
        var node = new DebugNode("styled shape", this);

        node.addAsString("state", state);

        node.addAsClass("origShape", origShape);
        node.addAsClass("shape", shape);

        node.addAsString("origDrag", origDrag);
        node.addAsString("transformedDrag", transformedDrag);

        node.addAsString("type", shapeType);
        node.addAsString("fillPaint", fillPaint);
        node.addAsString("strokePaint", strokePaint);
        node.add(effects.createDebugNode("effects"));
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