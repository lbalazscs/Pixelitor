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

package pixelitor.tools.shapes;

import com.jhlabs.awt.WobbleStroke;
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.filters.gui.ParamState;
import pixelitor.filters.gui.StrokeParam;
import pixelitor.filters.gui.StrokeSettings;
import pixelitor.filters.painters.AreaEffects;
import pixelitor.gui.View;
import pixelitor.history.History;
import pixelitor.history.PartialImageEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.layers.Drawable;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerGUI;
import pixelitor.layers.SmartFilter;
import pixelitor.tools.Tools;
import pixelitor.tools.shapes.history.RasterizeShapeEdit;
import pixelitor.tools.shapes.history.StyledShapeEdit;
import pixelitor.tools.transform.TransformBox;
import pixelitor.tools.transform.Transformable;
import pixelitor.tools.util.Drag;
import pixelitor.utils.Shapes;
import pixelitor.utils.TaperingStroke;
import pixelitor.utils.Thumbnails;
import pixelitor.utils.debug.DebugNode;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.Objects;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.colors.FgBgColors.getBGColor;
import static pixelitor.colors.FgBgColors.getFGColor;
import static pixelitor.tools.shapes.TwoPointPaintType.NONE;
import static pixelitor.tools.shapes.TwoPointPaintType.TRANSPARENT;

/**
 * A shape with associated stroke, fill, and effects
 * that can render itself on a given {@link Graphics2D},
 * and can be transformed by a {@link TransformBox}.
 */
public class StyledShape implements Transformable, Serializable, Cloneable {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final boolean DEBUG_PAINTING = false;

    /**
     * The state of a {@link StyledShape}.
     */
    enum State {
        EMPTY, // has no actual shape yet
        DESERIALIZED, // after being deserialized
        INITIAL_DRAG, // when it has a shape after the first drag event
        TRANSFORMABLE; // final state after transform box is created

        boolean canChangeTo(State next) {
            return switch (this) {
                case EMPTY -> next == INITIAL_DRAG;
                case DESERIALIZED -> next == TRANSFORMABLE;
                case INITIAL_DRAG -> next == INITIAL_DRAG || next == TRANSFORMABLE;
                case TRANSFORMABLE -> false;
            };
        }
    }

    private transient State state = State.EMPTY;

    private ShapeType shapeType;
    private List<ParamState<?>> typeSettings;

    private Shape origShape; // the original shape, in image-space
    private Shape shape; // the current transformed shape, in image-space

    // the original drag is kept even after the transform box appears
    // because it could be needed when regenerating the original shape
    // (if the user changes the shape type)
    private Drag origDrag;

    // this is transformed as the box is manipulated, so that
    // the gradients move together with the box
    private Drag transformedDrag;

    private TwoPointPaintType fillPaint;
    private TwoPointPaintType strokePaint;
    private AreaEffects effects;

    private transient Stroke stroke;

    // not needed for the rendering, but needed
    // to restore the stroke GUI state
    private StrokeSettings strokeSettings;

    private Color fgColor;
    private Color bgColor;

    // TODO this enables a shapes layer to invalidate the image cache
    //  when the styled shape changes. Instead of this, either the need for an
    //  image cache should be eliminated or the image should be cached in this class.
    private transient Runnable changeListener;

    public StyledShape(ShapesTool tool) {
        updateShapeType(tool);
        updateFillPaint(tool);
        updateStrokePaint(tool);
        updateStroke(tool);
        updateEffects(tool);
        updateColors();
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        if (hasStroke()) {
            // a stroke might be needed before this object
            // has a chance to load it from the tool
            StrokeParam p = new StrokeParam("");
            p.loadStateFrom(strokeSettings, false);
            stroke = p.createStroke();
        } else {
            stroke = null;
        }
        state = State.DESERIALIZED;
        changeListener = null;
        assert checkInvariants();
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        assert checkInvariants();
        out.defaultWriteObject();
    }

    /**
     * Renders this object on the given Graphics2D, which is expected to be in image space.
     */
    public void paint(Graphics2D g) {
        if (!hasShape()) {
            return;
        }

        if (transformedDrag.isImClick()) {
            // if the mouse dragging comes back exactly to the starting
            // point, then there is a shape object, but it's empty
            return;
        }

        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        if (DEBUG_PAINTING) {
            // draw the original shape with a semitransparent black
            Composite origComposite = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            g.setColor(Color.BLACK);
            g.fill(origShape);
            g.setComposite(origComposite);
        }

        if (hasFill()) {
            paintFill(g);
        }

        if (hasStroke()) {
            paintStroke(g);
        }

        if (effects.hasEnabledEffects()) {
            paintEffects(g);
        }

        if (DEBUG_PAINTING) {
            origDrag.drawImDirectionArrow(g);
            transformedDrag.drawImDirectionArrow(g);
        }
    }

    private void paintFill(Graphics2D g) {
        fillPaint.prepareGraphics(g, transformedDrag, fgColor, bgColor);
        g.fill(shape);
        fillPaint.cleanupGraphics(g);
    }

    private void paintStroke(Graphics2D g) {
        g.setStroke(stroke);
        strokePaint.prepareGraphics(g, transformedDrag, fgColor, bgColor);
        g.draw(shape);
        strokePaint.cleanupGraphics(g);
    }

    private void paintEffects(Graphics2D g) {
        effects.apply(g, getShapeForEffects());
    }

    private Shape getShapeForEffects() {
        if (!hasStroke()) {
            return shape; // simplest case
        }

        // using Area operations could be too slow for the WobbleStroke
        // TODO Also some shapes (Line, Cat, Bat + sometimes rounded rectangle) trigger
        //  https://bugs.openjdk.java.net/browse/JDK-6357341
        //  with Tapering stroke + neon border
        var strokeClass = stroke.getClass();
        if (strokeClass == WobbleStroke.class || (strokeClass == TaperingStroke.class && shapeType.hasAreaProblem())) {
            // give up, use the original shape, ignoring the stroke's width
            return shape;
        }

        Shape strokeOutline = stroke.createStrokedShape(shape);

        if (hasFill()) {
            // combine the fill area and the stroke outline area
            Area combined = new Area(shape);
            combined.add(new Area(strokeOutline));
            return combined;
        } else {
            // just use the stroke outline
            return strokeOutline;
        }
    }

    /**
     * Paints a thumbnail icon of the shape.
     */
    public void paintIconThumbnail(Graphics2D g2) {
        int thumbSize = Thumbnails.getMaxSize();
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        Drag drag;
        if (shapeType.isDirectional()) {
            double halfHeight = thumbSize / 2.0;
            drag = new Drag(0, halfHeight, thumbSize, halfHeight);
        } else {
            drag = new Drag(0, 0, thumbSize, thumbSize);
        }
        if (fillPaint == NONE || fillPaint == TRANSPARENT) {
            g2.setColor(LayerGUI.UNSELECTED_COLOR);
            g2.fillRect(0, 0, thumbSize, thumbSize);
            g2.setColor(LayerGUI.SELECTED_COLOR);
        } else {
            g2.setPaint(fillPaint.createPaint(drag, fgColor, bgColor));
        }
        g2.fill(shapeType.createShape(drag, null));
    }

    /**
     * Updates the shape based on the current initial drag.
     */
    public void updateFromDrag(Drag drag, boolean altDown, boolean shiftDown) {
        assert !Tools.SHAPES.hasBox();

        if (drag.isImClick()) {
            // can happen with pixel snapping
            return;
        }

        drag.setExpandFromCenter(altDown);
        if (shapeType.isDirectional()) {
            // for directional shapes it's useful to have the
            // ability to drag exactly horizontally or vertically
            drag.setAngleConstrained(shiftDown);
        } else {
            drag.setEnforceEqualDimensions(shiftDown);
        }

        origDrag = drag;

        // during the initial drag it can use the tool's settings directly
        ShapeTypeSettings settings = Tools.SHAPES.getSettingsOf(shapeType);
        origShape = shapeType.createShape(drag, settings);

        // since there is no transform box yet
        this.transformedDrag = drag;
        shape = origShape;

        setState(State.INITIAL_DRAG);
        assert checkInvariants();
    }

    @Override
    public void imTransform(AffineTransform at) {
        shape = at.createTransformedShape(origShape);
        transformedDrag = origDrag.imTransformedCopy(at);

        notifyChangeListener();
        assert checkInvariants();
    }

    @Override
    public void updateUI(View view) {
        Layer activeLayer = view.getComp().getActiveLayer();
        if (activeLayer.isMaskEditing() && activeLayer instanceof SmartFilter smartFilter) {
            smartFilter.shapeDraggedOnMask();
        }
        activeLayer.update();
    }

    @Override
    public void prepareForTransform() {
        // TODO implement the new Transformable methods
    }

    @Override
    public PixelitorEdit finalizeTransform() {
        // TODO implement the new Transformable methods
        return null;
    }

    @Override
    public void cancelTransform() {
        // TODO implement the new Transformable methods
    }

    private void updateShapeType(ShapesTool tool) {
        ShapeType newType = tool.getSelectedType();
        boolean typeChanged = this.shapeType != newType;
        this.shapeType = newType;

        if (typeChanged) {
            reloadTypeSettings(tool);
        }
    }

    private void reloadTypeSettings(ShapesTool tool) {
        if (shapeType.hasSettings()) {
            typeSettings = tool.getSettingsOf(shapeType).copyState();
        } else {
            typeSettings = null;
        }
    }

    private void updateFillPaint(ShapesTool tool) {
        this.fillPaint = tool.getSelectedFillPaint();
    }

    private void updateStrokePaint(ShapesTool tool) {
        this.strokePaint = tool.getSelectedStrokePaint();
        if (stroke == null) { // can happen after deserialization
            // this ignores the deserialized strokeSettings, but it's not a problem:
            // if there is no stroke, then the disabled stroke settings aren't important
            updateStroke(tool);
        }
    }

    private void updateStroke(ShapesTool tool) {
        this.stroke = tool.getStroke();
        strokeSettings = tool.getStrokeSettings();
    }

    private void updateEffects(ShapesTool tool) {
        this.effects = tool.getEffects();
    }

    private void updateColors() {
        this.fgColor = getFGColor();
        this.bgColor = getBGColor();
    }

    // called after a change in the shape type or settings
    private void recreateShape(ShapesTool tool, TransformBox box, String editName) {
        assert origDrag != null : "no origDrag, edit = " + editName;

        ShapeTypeSettings settings = tool.getSettingsOf(shapeType);
        if (shapeType.isDirectional()) {
            // make sure that the new directional shape is drawn
            // along the direction of the existing box
            // TODO this still ignores the height of the current box
            origShape = shapeType.createShape(origDrag.getCenterHorizontalDrag(), settings);
        } else {
            origShape = shapeType.createShape(origDrag, settings);
        }
        assert origShape != null;

        // should always be non-null, unless the user somehow manages to change the
        // type via keyboard during the initial drag, but leave the check for safety
        if (box != null) {
            // also recreate the transformed shape
            box.applyTransform();
        }
    }

    /**
     * Captures the current state as the original state.
     * Subsequent transformations will be relative to this captured state.
     */
    public void captureOrigState() {
        assert transformedDrag != null : "state = " + state;
        origDrag = transformedDrag;
        origShape = shape;

        assert checkInvariants();
    }

    /**
     * Creates a transform box suitable for this styled shape.
     */
    public TransformBox createBox(View view) {
        assert hasShape();
        if (origDrag != transformedDrag) {
            // we aren't at the end of the first user drag
            captureOrigState();
        }

        TransformBox box;
        if (shapeType.isDirectional()) {
            // for directional shapes, zero-width or zero-height drags are allowed
            box = createRotatedBox(view, origDrag.calcAngle());
        } else {
            if (origDrag.isEmptyImRect()) {
                return null;
            }
            Rectangle2D origImRect = origDrag.toPosImRect();
            assert !origImRect.isEmpty() : "drag = " + origDrag;
            box = new TransformBox(origImRect, view, this);
        }
        setState(State.TRANSFORMABLE);
        return box;
    }

    // creates a transform box for directional shapes
    private TransformBox createRotatedBox(View view, double angle) {
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
        // 3) Rotate the box (and implicitly the shape) forwards.

        double dragLength = transformedDrag.calcImLength();
        double dragStartX = transformedDrag.getOriginX();
        double dragStartY = transformedDrag.getOriginY();
        Rectangle2D horBoxImBounds = new Rectangle2D.Double(
            dragStartX,
            dragStartY - dragLength * Shapes.UNIT_ARROW_HEAD_WIDTH / 2.0,
            dragLength,
            dragLength * Shapes.UNIT_ARROW_HEAD_WIDTH);
        assert !horBoxImBounds.isEmpty();

        double rotCenterImX = horBoxImBounds.getX();
        double rotCenterImY = horBoxImBounds.getY() + horBoxImBounds.getHeight() / 2.0;

        // Set the original shape to the horizontal shape.
        // It could also be rotated backwards with an AffineTransform.
        origShape = shapeType.createShape(transformedDrag, settings);
        // rotate back
        origShape = Shapes.rotate(origShape, -angle, rotCenterImX, rotCenterImY);

        // Set the original drag to the diagonal of the back-rotated transform box,
        // so that after a shape-type change the new shape is created correctly
        double halfImHeight = dragLength * Shapes.UNIT_ARROW_HEAD_WIDTH / 2.0;
        origDrag = new Drag(
            dragStartX,
            dragStartY - halfImHeight,
            dragStartX + dragLength,
            dragStartY + halfImHeight);

        TransformBox box = new TransformBox(horBoxImBounds, view, this);

        // rotate the horizontal box into place
        box.saveImRefPoints(); // so that transform works
        box.setAngle(angle);

        // this was coTransform before
        box.imTransform(AffineTransform.getRotateInstance(
            angle, rotCenterImX, rotCenterImY));
        return box;
    }

    /**
     * Rasterizes the shape, making it part of the drawable's pixels.
     */
    public void rasterize(Composition comp, TransformBox transformBox, ShapesTool tool) {
        assert checkInvariants();

        PartialImageEdit imageEdit = null;
        Drawable dr = comp.getActiveDrawable();
        if (dr != null) { // a text layer could be active
            Rectangle shapeBounds = shape.getBounds();
            int padding = 1 + (int) tool.calcExtraPadding();
            shapeBounds.grow(padding, padding);

            if (!shapeBounds.isEmpty()) {
                BufferedImage originalImage = dr.getImage();
                imageEdit = PartialImageEdit.create(
                    shapeBounds, originalImage, dr, false, "Shape");
            }
        }

        // must be added even if there is no image edit
        // to manage the shapes tool state changes
        History.add(new RasterizeShapeEdit(comp, imageEdit, transformBox, this));

        if (imageEdit != null) {
            paintOnDrawable(dr);
            dr.update();
            dr.updateIconImage();
        } else {
            // a repaint is necessary even if the box is outside the canvas
            comp.repaint();
        }
    }

    private void paintOnDrawable(Drawable dr) {
        BufferedImage image = dr.getImage();
        Graphics2D g2 = image.createGraphics();
        g2.translate(-dr.getTx(), -dr.getTy());
        dr.getComp().applySelectionClipping(g2);

        paint(g2);

        g2.dispose();
    }

    @Override
    public final StyledShape clone() {
        // TODO initially this was used only for undo, so it was
        //   OK to share all the references, but now it's also
        //   used by ShapesLayer.createTypeSpecificCopy
        assert checkInvariants();
        try {
            StyledShape clone = (StyledShape) super.clone();
            assert clone.checkInvariants();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); // can't happen
        }
    }

    /**
     * Regenerates all aspects of the shape.
     */
    public void regenerateAll(TransformBox box, ShapesTool tool) {
        regenerate(box, tool, ShapesTool.EDIT_TYPE);
        regenerate(box, tool, ShapesTool.EDIT_TYPE_SETTINGS);
        regenerate(box, tool, ShapesTool.EDIT_FILL);
        regenerate(box, tool, ShapesTool.EDIT_STROKE);
        regenerate(box, tool, ShapesTool.EDIT_STROKE_SETTINGS);
        regenerate(box, tool, ShapesTool.EDIT_EFFECTS);
        regenerate(box, tool, ShapesTool.EDIT_COLORS);
        regenerate(box, tool, ShapesTool.EDIT_STROKE_SETTINGS);
    }

    /**
     * Regenerates a specific aspect of the shape.
     */
    public void regenerate(TransformBox box, ShapesTool tool, String editName) {
        StyledShape backup = clone();

        // calculate the new transformed shape
        switch (editName) {
            case ShapesTool.EDIT_TYPE -> {
                updateShapeType(tool);
                recreateShape(tool, box, editName);
            }
            case ShapesTool.EDIT_TYPE_SETTINGS -> {
                reloadTypeSettings(tool);
                recreateShape(tool, box, editName);
            }
            case ShapesTool.EDIT_FILL -> updateFillPaint(tool);
            case ShapesTool.EDIT_STROKE -> updateStrokePaint(tool);
            case ShapesTool.EDIT_STROKE_SETTINGS -> updateStroke(tool);
            case ShapesTool.EDIT_EFFECTS -> updateEffects(tool);
            case ShapesTool.EDIT_COLORS -> updateColors();
            default -> throw new IllegalStateException("Unexpected edit: " + editName);
        }

        if (this.equals(backup)) {
            // there was no actual change in state
            return;
        }

        Composition comp = Views.getActiveComp();
        History.add(new StyledShapeEdit(editName, comp, backup));
        comp.getActiveLayer().update();

        notifyChangeListener();
    }

    public ShapeType getShapeType() {
        return shapeType;
    }

    public List<ParamState<?>> getTypeSettings() {
        return typeSettings;
    }

    public TwoPointPaintType getFillPaint() {
        return fillPaint;
    }

    public TwoPointPaintType getStrokePaint() {
        return strokePaint;
    }

    private boolean hasFill() {
        return fillPaint != NONE;
    }

    private boolean hasStroke() {
        return strokePaint != NONE;
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

    public Shape getShape() {
        return shape;
    }

    // this object is created when the mouse is pressed, but
    // it has a shape only after the first drag events arrive
    public boolean hasShape() {
        boolean hasShape = state != State.EMPTY;

        assert hasShape == (shape != null);
        assert hasShape == (origDrag != null);
        assert hasShape == (transformedDrag != null);

        return hasShape;
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

    private void setState(State next) {
        assert state.canChangeTo(next) : state + " => " + next;
        state = next;
    }

    public boolean checkInvariants() {
        // TODO CREATED, but not initialized styled shapes
        //  shouldn't be put in shape layers
        if (state == State.EMPTY) {
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StyledShape that = (StyledShape) o;

        return shapeType == that.shapeType &&
            Objects.equals(typeSettings, that.typeSettings) &&
            fillPaint == that.fillPaint &&
            strokePaint == that.strokePaint &&
            Objects.equals(effects, that.effects) &&
            Objects.equals(strokeSettings, that.strokeSettings) &&
            Objects.equals(fgColor, that.fgColor) &&
            Objects.equals(bgColor, that.bgColor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shapeType, typeSettings, fillPaint, strokePaint, effects, strokeSettings, fgColor, bgColor);
    }

    @Override
    public DebugNode createDebugNode(String key) {
        var node = new DebugNode(key, this);

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
