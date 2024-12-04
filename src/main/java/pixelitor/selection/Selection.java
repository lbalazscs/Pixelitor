/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.utils.Shapes;
import pixelitor.utils.Threads;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.Debuggable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_ROUND;
import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;

/**
 * Represents a selection area on an image with animated "marching ants" border.
 */
public class Selection implements Debuggable, Transformable {
    private static final double DASH_WIDTH = 1.0;
    private static final float DASH_LENGTH = 4.0f;
    private static final float[] MARCHING_ANTS_DASH = {DASH_LENGTH, DASH_LENGTH};
    private float dashPhase;
    private Timer marchingAntsTimer;

    // The shape of the selection.
    // The coordinates are in image space, relative to the canvas.
    private Shape shape;

    // the original shape before a shape movement
    private Shape origShape;

    private View view;

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

        // hack to prevent unit tests from starting the marching
        if (AppMode.isUnitTesting()) {
            frozen = true;
        }

        startMarching();
    }

    // copy constructor
    public Selection(Selection orig, boolean shareView) {
        if (shareView) {
            view = orig.view;
        }

        // the shapes can be shared
        shape = orig.shape;

        // The animation timer is not copied - will be started by setView if needed.
    }

    public void startMarching() {
        assert !disposed : "disposed selection";
        assert view != null : "no view in selection";

        if (frozen || hidden) {
            return;
        }

        marchingAntsTimer = new Timer(100, e -> {
            dashPhase += 1.0f / (float) view.getZoomScale();
            repaint();
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

    public void paintMarchingAnts(Graphics2D g2) {
        assert Threads.calledOnEDT() : Threads.threadInfo();
        assert !disposed : "dead selection";

        if (shape == null || hidden) {
            return;
        }

        Stroke origStroke = g2.getStroke();

        // As the selection coordinates are in image space, this is
        // called with a Graphics2D transformed into image space.
        // The line width has to be scaled to compensate.
        double viewScale = view.getZoomScale();
        float lineWidth = (float) (DASH_WIDTH / viewScale);

        float[] dash;
        if (viewScale == 1.0) { // the most common case
            dash = MARCHING_ANTS_DASH;
        } else {
            float scaledDashLength = (float) (DASH_LENGTH / viewScale);
            dash = new float[]{scaledDashLength, scaledDashLength};
        }

        // Draw white segments
        drawSegments(g2, WHITE, lineWidth, dash, dashPhase);

        // Draw black segments
        float blackPhase = (float) (dashPhase + DASH_LENGTH / viewScale);
        drawSegments(g2, BLACK, lineWidth, dash, blackPhase);

        // Restore original stroke
        g2.setStroke(origStroke);
    }

    private void drawSegments(Graphics2D g2, Color color, float lineWidth, float[] dash, float phase) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(lineWidth,
            CAP_BUTT, JOIN_ROUND, 0.0f, dash, phase));
        g2.draw(shape);
    }

    public void dispose() {
        assert AppMode.isUnitTesting() || Threads.calledOnEDT() : Threads.threadInfo();
        if (disposed) {
            return;
        }
        stopMarching();
        repaint();
        view = null;
        disposed = true;
    }

    private void repaint() {
//        Rectangle selBounds = shape.getBounds();
//        view.updateRegion(selBounds.x, selBounds.y, selBounds.x + selBounds.width + 1, selBounds.y + selBounds.height + 1, 1);

//        The above optimization isn't good; the previous positions should be also considered for the
//        case when the selection is shrinking while dragging.
//        But it doesn't seem to solve the pixel grid problem anyway.

        view.repaint();
    }

    public void setShape(Shape currentShape) {
        assert currentShape != null;
        shape = currentShape;
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
            repaint();
            return !shape.getBounds().isEmpty();
        }
        return false;
    }

    public Shape getShape() {
        return shape;
    }

    public boolean isRectangular() {
        return shape instanceof Rectangle2D;
    }

    /**
     * Returns the shape bounds of the selection
     * Like everything else in this class, this is in image coordinates
     * (but relative to the canvas, not to the image)
     */
    public Rectangle getShapeBounds() {
        return shape.getBounds();
    }

    public Rectangle2D getShapeBounds2D() {
        return shape.getBounds2D();
    }

    /**
     * Modifies the selection shape using the given modification type and amount.
     */
    public void modify(SelectionModifyType type, float amount) {
        Shape prevShape = shape;

        // Create the modified shape
        Stroke borderStroke = new BasicStroke(amount);
        Shape borderShape = borderStroke.createStrokedShape(shape);
        Area currentArea = new Area(shape);
        Area borderArea = new Area(borderShape);
        shape = type.modify(currentArea, borderArea);

        // Handle the modification result
        var comp = view.getComp();
        boolean valid = clipToCanvasBounds(comp);
        if (valid) {
            var edit = new SelectionShapeChangeEdit(
                "Modify Selection", comp, prevShape);
            History.add(edit);
        } else {
            comp.deselect(true);
        }
    }

    public Shape transform(AffineTransform at) {
        Shape backupShape = shape;
        shape = at.createTransformedShape(shape);
        return backupShape;
    }

    public void nudge(AffineTransform at) {
        Shape backupShape = transform(at);
        History.add(new SelectionShapeChangeEdit(
            "Nudge Selection", view.getComp(), backupShape));
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hide, boolean fromMenu) {
        assert !disposed : "dead selection";

        boolean change = hidden != hide;
        if (!change) {
            return;
        }

        hidden = hide;

        if (hide) {
            stopMarching();
        } else {
            startMarching();
        }

        repaint();

        // if not called from the menu, update the menu name
        if (!fromMenu) {
            SelectionActions.getShowHide().updateTextFrom(this);
        }
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean b) {
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

    public void startMovement() {
        assert shape != null;
        origShape = shape;
    }

    public void transformWhileDragging(AffineTransform at) {
        shape = at.createTransformedShape(origShape);
    }

    public void moveWhileDragging(double relImX, double relImY) {
        if (origShape instanceof Rectangle2D startRect) {
            // preserve the type information
            shape = new Rectangle2D.Double(
                startRect.getX() + relImX, startRect.getY() + relImY,
                startRect.getWidth(), startRect.getHeight());
        } else {
            shape = Shapes.translate(origShape, relImX, relImY);
        }
    }

    public PixelitorEdit endMovement(boolean keepStartShape) {
        var comp = view.getComp();

        shape = comp.clipToCanvasBounds(shape);
        if (shape.getBounds().isEmpty()) { // moved outside the canvas
            comp.deselect(false);
            return new DeselectEdit(comp, origShape);
        }

        var edit = new SelectionShapeChangeEdit(
            MoveMode.MOVE_SELECTION_ONLY.getEditName(), comp, origShape);
        if (!keepStartShape) {
            origShape = null;
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
