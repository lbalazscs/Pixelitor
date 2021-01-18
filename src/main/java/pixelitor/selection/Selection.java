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

package pixelitor.selection;

import pixelitor.AppContext;
import pixelitor.Composition;
import pixelitor.gui.View;
import pixelitor.history.DeselectEdit;
import pixelitor.history.History;
import pixelitor.history.PixelitorEdit;
import pixelitor.history.SelectionShapeChangeEdit;
import pixelitor.tools.move.MoveMode;
import pixelitor.utils.debug.DebugNode;

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
 * Represents a selection on an image.
 */
public class Selection {
    private float dashPhase;
    private View view;
    private Timer marchingAntsTimer;

    // The shape of the selection.
    // The coordinates are in image space, relative to the canvas.
    private Shape shape;

    private static final double DASH_WIDTH = 1.0;
    private static final float DASH_LENGTH = 4.0f;
    private static final float[] MARCHING_ANTS_DASH = {DASH_LENGTH, DASH_LENGTH};

    // if true, then the "marching ants" are not marching
    private boolean frozen = false;

    // if true, then the "marching ants" are not painted at all
    private boolean hidden = false;

    // if true, then this object should not be used anymore
    private boolean dead = false;

    // the original shape before a shape movement
    private Shape moveStartShape;

    public Selection(Shape shape, View view) {
        // the shape can be null, because this Selection
        // object can be created after a mouse press
        assert view != null;

        this.shape = shape;
        this.view = view;

        // hack to prevent unit tests from starting the marching
        if (AppContext.isUnitTesting()) {
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

        // the Timer is not copied! - setView starts it
    }

    public void startMarching() {
        if (frozen) {
            return;
        }

        assert !dead : "dead selection";
        assert view != null : "no view in selection";

        marchingAntsTimer = new Timer(100, null);
        marchingAntsTimer.addActionListener(evt -> {
            if (!hidden) {
                dashPhase += 1.0f / (float) view.getScaling();
                repaint();
            }
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
        assert !dead : "dead selection";

        if (shape == null || hidden) {
            return;
        }

        paintAnts(g2, shape, dashPhase);
    }

    private void paintAnts(Graphics2D g2, Shape shape, float phase) {
        // As the selection coordinates are in image space, this is
        // called with a Graphics2D transformed into image space.
        // The line width has to be scaled to compensate.
        double viewScale = view.getScaling();
        float lineWidth = (float) (DASH_WIDTH / viewScale);

        float[] dash;
        if (viewScale == 1.0) { // the most common case
            dash = MARCHING_ANTS_DASH;
        } else {
            float scaledDashLength = (float) (DASH_LENGTH / viewScale);
            dash = new float[]{scaledDashLength, scaledDashLength};
        }

        g2.setPaint(WHITE);
        Stroke stroke = new BasicStroke(lineWidth, CAP_BUTT,
            JOIN_ROUND, 0.0f, dash, phase);
        g2.setStroke(stroke);
        g2.draw(shape);

        g2.setPaint(BLACK);
        Stroke stroke2 = new BasicStroke(lineWidth, CAP_BUTT,
            JOIN_ROUND, 0.0f, dash,
            (float) (phase + DASH_LENGTH / viewScale));
        g2.setStroke(stroke2);
        g2.draw(shape);
    }

    public void die() {
        if (dead) {
            return;
        }
        stopMarching();
        repaint();
        view = null;
        dead = true;
    }

    private void repaint() {
//        Rectangle selBounds = shape.getBounds();
//        view.updateRegion(selBounds.x, selBounds.y, selBounds.x + selBounds.width + 1, selBounds.y + selBounds.height + 1, 1);
//        the above optimization is not good, the previous positions should be also considered for the
//        case when the selection is shrinking while dragging.
//        But it does not seem to solve the pixel grid problem anyway

        view.repaint();
    }

    public void setShape(Shape currentShape) {
        shape = currentShape;
    }

    /**
     * Restricts the selection shape to be within the canvas bounds.
     * This must be always called for new or changed selections.
     *
     * @return true if the selection shape is not empty
     */
    private boolean clipToCanvasSize(Composition comp) {
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

    public void modify(SelectionModifyType type, float amount) {
        Stroke outlineStroke = new BasicStroke(amount);
        Shape outlineShape = outlineStroke.createStrokedShape(shape);

        Area oldArea = new Area(shape);
        Area outlineArea = new Area(outlineShape);

        Shape backupShape = shape;
        shape = type.modify(oldArea, outlineArea);

        var comp = view.getComp();
        boolean notEmpty = clipToCanvasSize(comp);
        if (notEmpty) {
            var edit = new SelectionShapeChangeEdit(
                "Modify Selection", comp, backupShape);
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
        assert !dead : "dead selection";

        boolean change = hidden != hide;
        if (!change) {
            return;
        }

        hidden = hide;

        if (hide) {
            stopMarching();
        } else {
            if (marchingAntsTimer == null) {
                startMarching();
            }
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

    public boolean isAlive() {
        return !dead;
    }

    public void startMovement() {
        moveStartShape = shape;
    }

    public void moveWhileDragging(double relImX, double relImY) {
        var at = AffineTransform.getTranslateInstance(relImX, relImY);
        shape = at.createTransformedShape(moveStartShape);
    }

    public PixelitorEdit endMovement() {
        var comp = view.getComp();

        shape = comp.clipToCanvasBounds(shape);
        if (shape.getBounds().isEmpty()) { // moved outside the canvas
            var deselectEdit = new DeselectEdit(comp, moveStartShape);
            comp.deselect(false);
            return deselectEdit;
        }

        var edit = new SelectionShapeChangeEdit(
            MoveMode.MOVE_SELECTION_ONLY.getEditName(), comp, moveStartShape);
        moveStartShape = null;
        return edit;
    }

    public DebugNode createDebugNode(String name) {
        var node = new DebugNode(name, this);

        node.addBoolean("hidden", hidden);
        node.addBoolean("dead", dead);
        node.addBoolean("frozen", frozen);
        node.addBoolean("marching", isMarching());

        node.addString("shape class", shape.getClass().getName());
        node.addString("bounds", getShapeBounds().toString());
        node.addString("bounds 2D", getShapeBounds2D().toString());

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
