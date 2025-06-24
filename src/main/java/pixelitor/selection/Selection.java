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
 * Represents a selection area on an image with animated "marching ants" border.
 */
public class Selection implements Transformable {
    private static final double DASH_WIDTH = 1.0;
    private static final float DASH_LENGTH = 4.0f;
    private static final float[] MARCHING_ANTS_DASH = {DASH_LENGTH, DASH_LENGTH};
    private float dashPhase;
    private Timer marchingAntsTimer;

    // The shape of the selection.
    // The coordinates are in image space, relative to the canvas.
    private Shape shape;

    // backup of the shape before a selection drag started
    private Shape shapeBeforeDrag;

    private View view; // the view displaying this selection

    // if true, then the "marching ants" are not marching
    private boolean frozen = false;

    // if true, then the "marching ants" are not painted at all
    private boolean hidden = false;

    // if true, then this object should not be used anymore
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

        startMarching();
    }

    public Selection(Selection orig) {
        // the shapes can be shared because all changes create new instances
        this.shape = orig.shape;

        this.shapeBeforeDrag = orig.shapeBeforeDrag;
        this.frozen = orig.frozen;
        this.hidden = orig.hidden;

        this.view = null; // not copied - will be copied later

        this.dashPhase = 0;
        this.marchingAntsTimer = null;
        // The animation timer is not copied - will be started by setView if needed.

        this.disposed = orig.disposed;
        assert !orig.disposed;
    }

    public void startMarching() {
        assert !disposed : "disposed selection";
        assert view != null : "no view in selection";

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
     * Paints the marching ants border onto the given Graphics2D.
     * Assumes g2 is in image space.
     */
    public void paintMarchingAnts(Graphics2D g2) {
        assert Threads.calledOnEDT() : Threads.threadInfo();
        assert !disposed : "disposed selection";

        if (shape == null || hidden) {
            return;
        }

        Stroke origStroke = g2.getStroke();

        // Ensure that the border width doesn't depend on the zooming,
        // considering that the graphics coordinates are in image space.
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
        assert AppMode.isUnitTesting() || Threads.calledOnEDT() : Threads.threadInfo();
        if (disposed) {
            return;
        }
        stopMarching();
        view.repaint();
        view = null;
        disposed = true;
    }

    public void setShape(Shape newShape) {
        assert newShape != null;
        assert !disposed;

        shape = newShape;
    }

    /**
     * Ensures the selection shape stays within canvas bounds.
     * This must be always called for new or changed selections.
     *
     * @return true if the selection shape is valid
     */
    private boolean clipToCanvasBounds(Composition comp) {
        assert comp == view.getComp();
        if (shape != null) {
            shape = comp.clipToCanvasBounds(shape);
            view.repaint();
            return !shape.getBounds().isEmpty();
        }
        return false;
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
     * Returns the shape bounds of the selection
     * Like everything else in this class, this is in image coordinates
     * (but relative to the canvas, not to the image)
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
     * Applies an affine transform to the selection shape.
     * Returns the shape before transformation.
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

    public void setHidden(boolean hide, boolean calledFromMenu) {
        assert !disposed : "disposed selection";
        assert view != null;

        if (hidden == hide) {
            return; // no change
        }
        hidden = hide;

        if (hide) {
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

    public void setFrozen(boolean b) {
        assert !disposed;
        if (this.frozen == b) {
            return; // no change
        }

        frozen = b;
        if (b) {
            stopMarching();
        } else if (!hidden) {
            startMarching();
        }
    }

    // called when the composition is duplicated
    public void setView(View view) {
        assert view != null;
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
     * Prepares for a drag/move operation.
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
     * Finalizes the movement of the selection shape after a drag operation.
     * Returns an edit that can undo/redo the whole movement.
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
