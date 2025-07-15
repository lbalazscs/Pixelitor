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

package pixelitor.selection;

import pixelitor.AppMode;
import pixelitor.Composition;
import pixelitor.gui.View;
import pixelitor.history.DeselectEdit;
import pixelitor.history.History;
import pixelitor.history.PixelitorEdit;
import pixelitor.history.SelectionShapeChangeEdit;
import pixelitor.tools.move.MoveMode;
import pixelitor.tools.transform.Transformable;
import pixelitor.tools.util.ArrowKey;
import pixelitor.utils.Shapes;
import pixelitor.utils.Threads;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_ROUND;
import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;

/**
 * Represents a selection area on an image with an animated "marching ants" border.
 */
public class Selection implements Transformable {
    private static final double DASH_WIDTH = 1.0;
    private static final float DASH_LENGTH = 4.0f;
    private static final float[] MARCHING_ANTS_DASH = {DASH_LENGTH, DASH_LENGTH};
    private float dashPhase;
    private Timer marchingAntsTimer;

    // the shape of the selection, in image-space coordinates relative to the canvas
    private Shape shape;

    // backup of the shape before a selection drag started
    private Shape shapeBeforeDrag;

    // the view displaying this selection
    private View view;

    // if true, the "marching ants" are not marching
    private boolean frozen = false;

    // if true, the "marching ants" are not painted at all
    private boolean hidden = false;

    // if true, this object should not be used anymore
    private boolean disposed = false;

    public Selection(Shape shape, View view) {
        // the shape can be null, because this Selection
        // object can be created after a mouse press
        assert view != null;

        this.shape = shape;
        this.view = view;

        // prevent marching in unit tests
        if (AppMode.isUnitTesting()) {
            frozen = true;
        }

        if (shape != null) {
            startMarching();
        }
    }

    public Selection(Selection orig) {
        // the shape can be shared because all changes create new instances
        this.shape = orig.shape;
        this.shapeBeforeDrag = orig.shapeBeforeDrag;

        this.frozen = orig.frozen;
        this.hidden = orig.hidden;
        this.disposed = orig.disposed;
        assert !orig.disposed;

        // the view is not copied directly; it will be set later
        this.view = null;

        // the animation timer is not copied; it will be started by setView if needed
        this.dashPhase = 0;
        this.marchingAntsTimer = null;
    }

    /**
     * Starts the "marching ants" animation timer.
     */
    public void startMarching() {
        assert !disposed : "disposed selection";
        assert view != null : "no view in selection";
        assert !isMarching();
        assert shape != null;

        if (frozen || hidden) {
            return;
        }

        marchingAntsTimer = new Timer(100, e -> {
            dashPhase += 1.0f / (float) view.getZoomScale();
            view.repaint();
        });
        marchingAntsTimer.start();
    }

    private void stopMarching() {
        if (marchingAntsTimer != null) {
            marchingAntsTimer.stop();
            marchingAntsTimer = null;
        }
    }

    public boolean isMarching() {
        return marchingAntsTimer != null;
    }

    /**
     * Paints the marching ants border onto the given Graphics2D,
     * which is assumed to be in image space.
     */
    public void paintMarchingAnts(Graphics2D g2) {
        assert Threads.calledOnEDT() : Threads.callInfo();
        assert !disposed : "disposed selection";

        if (shape == null || hidden) {
            return;
        }

        Stroke origStroke = g2.getStroke();

        // ensure that the border width doesn't depend on the zooming,
        // considering that the graphics coordinates are in image space
        double viewScale = view.getZoomScale();
        float lineWidth = (float) (DASH_WIDTH / viewScale);

        float[] dash;
        if (viewScale == 1.0) { // optimize for common case
            dash = MARCHING_ANTS_DASH;
        } else {
            float scaledDashLength = (float) (DASH_LENGTH / viewScale);
            dash = new float[]{scaledDashLength, scaledDashLength};
        }

        // draw white segments
        drawSegments(g2, WHITE, lineWidth, dash, dashPhase);

        // draw black segments offset by half a dash length
        float blackPhase = (float) (dashPhase + DASH_LENGTH / viewScale);
        drawSegments(g2, BLACK, lineWidth, dash, blackPhase);

        // restore original stroke
        g2.setStroke(origStroke);
    }

    private void drawSegments(Graphics2D g2, Color color, float lineWidth, float[] dash, float phase) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(lineWidth,
            CAP_BUTT, JOIN_ROUND, 0.0f, dash, phase));
        g2.draw(shape);
    }

    /**
     * Releases resources and marks the selection as unusable.
     */
    public void dispose() {
        assert AppMode.isUnitTesting() || Threads.calledOnEDT() : Threads.callInfo();
        assert !disposed;
        if (disposed) {
            return;
        }
        stopMarching();
        view.repaint();
        view = null;
        disposed = true;
    }

    /**
     * Sets the shape of the selection.
     */
    public void setShape(Shape newShape) {
        assert newShape != null;
        assert !disposed;

        boolean hadNoShape = (shape == null);
        shape = newShape;

        if (hadNoShape) {
            startMarching();
        }
    }

    public Shape getShape() {
        assert !disposed;
        return shape;
    }

    public boolean isRectangular() {
        assert !disposed;
        return shape instanceof Rectangle2D;
    }

    /**
     * Returns the bounds of the selection shape in image-space coordinates.
     */
    public Rectangle getShapeBounds() {
        assert !disposed;
        return shape.getBounds();
    }

    public Rectangle2D getShapeBounds2D() {
        assert !disposed;
        return shape.getBounds2D();
    }

    public void nudge(ArrowKey key) {
        transformAndAddHistory(key.toTransform());
    }

    private void transformAndAddHistory(AffineTransform at) {
        assert !disposed;
        Shape backupShape = transform(at);
        History.add(new SelectionShapeChangeEdit(
            "Nudge Selection", view.getComp(), backupShape));
    }

    /**
     * Applies an affine transform to the selection shape
     * and returns the shape before the transformation.
     */
    public Shape transform(AffineTransform at) {
        assert !disposed;
        Shape backupShape = shape;
        shape = at.createTransformedShape(shape);
        return backupShape;
    }

    public boolean isHidden() {
        return hidden;
    }

    /**
     * Sets whether the selection border is hidden.
     */
    public void setHidden(boolean hide, boolean calledFromMenu) {
        assert !disposed : "disposed selection";
        assert view != null;

        if (hidden == hide) {
            return; // no change
        }
        hidden = hide;

        if (hidden) {
            stopMarching();
        } else {
            startMarching();
        }

        view.repaint();

        if (!calledFromMenu) {
            SelectionActions.getShowHide().updateTextFrom(this);
        }
    }

    public boolean isFrozen() {
        return frozen;
    }

    /**
     * Sets whether the marching ants animation is frozen.
     */
    public void setFrozen(boolean frozen) {
        assert !disposed;
        if (this.frozen == frozen) {
            return; // no change
        }

        this.frozen = frozen;
        if (this.frozen) {
            stopMarching();
        } else {
            startMarching();
        }
    }

    public void setView(View view) {
        assert view != null;
        assert !isMarching();

        this.view = view;
        startMarching();
    }

    public View getView() {
        return view;
    }

    /**
     * Returns whether this selection is still active and usable.
     */
    public boolean isUsable() {
        return !disposed;
    }

    /**
     * Prepares for a drag/move operation by backing up the current shape.
     */
    public void prepareMovement() {
        assert shape != null;
        assert !disposed;

        shapeBeforeDrag = shape;
    }

    /**
     * Applies a transformation relative to the shape stored before dragging started.
     */
    private void transformWhileDragging(AffineTransform at) {
        assert !disposed;
        assert shapeBeforeDrag != null;

        shape = at.createTransformedShape(shapeBeforeDrag);
    }

    /**
     * Updates the selection shape during a drag operation by applying a translation delta.
     */
    public void moveWhileDragging(double relImX, double relImY) {
        assert !disposed;
        assert shapeBeforeDrag != null;

        if (shapeBeforeDrag instanceof Rectangle2D startRect) {
            // preserve the type information
            shape = new Rectangle2D.Double(
                startRect.getX() + relImX, startRect.getY() + relImY,
                startRect.getWidth(), startRect.getHeight());
        } else {
            shape = Shapes.translate(shapeBeforeDrag, relImX, relImY);
        }
    }

    /**
     * Finalizes the movement of the selection shape after
     * a drag operation and returns an edit for undo/redo.
     */
    public PixelitorEdit finalizeMovement(boolean keepOrigShape) {
        assert !disposed;
        assert shapeBeforeDrag != null;

        Composition comp = view.getComp();
        shape = comp.clipToCanvasBounds(shape);
        if (shape.getBounds().isEmpty()) { // moved off-canvas
            comp.deselect(false);
            return new DeselectEdit(comp, shapeBeforeDrag);
        }

        // the selection shape was changed successfully
        var edit = new SelectionShapeChangeEdit(
            MoveMode.MOVE_SELECTION_ONLY.getEditName(), comp, shapeBeforeDrag);
        if (!keepOrigShape) {
            shapeBeforeDrag = null;
        }
        return edit;
    }

    @Override
    public void imTransform(AffineTransform transform) {
        transformWhileDragging(transform);
    }

    @Override
    public void updateUI(View view) {
        view.repaint();
    }

    @Override
    public DebugNode createDebugNode(String name) {
        var node = new DebugNode(name, this);

        node.addBoolean("hidden", hidden);
        node.addBoolean("disposed", disposed);
        node.addBoolean("frozen", frozen);
        node.addBoolean("marching", isMarching());
        node.addBoolean("rectangular", isRectangular());

        node.addString("shape class", shape.getClass().getName());
        node.addAsString("bounds", getShapeBounds());
        node.addAsString("bounds 2D", getShapeBounds2D());

        return node;
    }

    @Override
    public String toString() {
        return "Selection{" +
            "composition=" + view.getComp().getName() +
            ", shape-class=" + (shape == null ? "null" : shape.getClass().getName()) +
            ", shapeBounds=" + (shape == null ? "null" : shape.getBounds()) +
            '}';
    }
}
