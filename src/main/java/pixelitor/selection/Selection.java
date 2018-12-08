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

package pixelitor.selection;

import pixelitor.Build;
import pixelitor.Composition;
import pixelitor.gui.ImageComponent;
import pixelitor.history.History;
import pixelitor.history.SelectionChangeEdit;
import pixelitor.menus.view.ShowHideAction;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_ROUND;
import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;

/**
 * Represents a selection on an image.
 */
public class Selection {
    private float dashPhase;
    private ImageComponent ic;
    private Timer marchingAntsTimer;

    // The shape that is currently drawn.
    // The coordinates are in image space, relative to the canvas.
    private Shape shape;

    private static final double DASH_WIDTH = 1.0;
    private static final float DASH_LENGTH = 4.0f;
    private static final float[] MARCHING_ANTS_DASH = {DASH_LENGTH, DASH_LENGTH};

    private boolean hidden = false;
    private boolean dead = false;
    private boolean frozen = false;

    public Selection(Shape shape, ImageComponent ic) {
        // TODO should not allow selections with null shape
        assert ic != null;

        this.shape = shape;
        this.ic = ic;

        // hack to prevent unit tests from starting the marching
        if (Build.isTesting()) {
            frozen = true;
        }

        startMarching();
    }

    // copy constructor
    public Selection(Selection orig, boolean shareIC) {
        if (shareIC) {
            this.ic = orig.ic;
        }

        // the shapes can be shared
        this.shape = orig.shape;

        // the Timer is not copied! - setIC starts it
    }

    public void startMarching() {
        if (frozen) {
            return;
        }

        assert !dead : "dead selection";
        assert ic != null : "no ic in selection";

        marchingAntsTimer = new Timer(100, null);
        marchingAntsTimer.addActionListener(evt -> {
            if(!hidden) {
                dashPhase += 1.0f / (float) ic.getViewScale();
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
        double viewScale = ic.getViewScale();
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
                JOIN_ROUND, 0.0f, dash,
                phase);
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
        stopMarching();
        repaint();
        ic = null;
        dead = true;
    }

    private void repaint() {
//        if(shape != null && !hidden) {
//             Rectangle selBounds = shape.getBounds();
//             ic.updateRegion(selBounds.x, selBounds.y, selBounds.x + selBounds.width + 1, selBounds.y + selBounds.height + 1, 1);

//             the above optimization is not enough, the previous positions should be also considered for the
//             case when the selection is shrinking while dragging.
//             But it does not seem to solve the pixel grid problem anyway
//        }

        ic.repaint();
    }

    public void setShape(Shape currentShape) {
        this.shape = currentShape;
    }

    /**
     * Restricts the selection shape to be within the canvas bounds.
     * This must be always called for new or changed selections.
     *
     * @return true if the selection shape is not empty
     */
    public boolean clipToCanvasSize(Composition comp) {
        assert comp == ic.getComp();
        if (shape != null) {
            shape = comp.clipShapeToCanvasSize(shape);

            repaint();

            return !shape.getBounds().isEmpty();
        }
        return false;
    }

    public Shape getShape() {
        return shape;
    }

    /**
     * Returns the shape bounds of the selection
     * Like everything else in this class, this is in image coordinates
     * (but relative to the canvas, not to the image)
     */
    public Rectangle getShapeBounds() {
        return shape.getBounds();
    }

    public void modify(SelectionModifyType type, float amount) {
        BasicStroke outlineStroke = new BasicStroke(amount);
        Shape outlineShape = outlineStroke.createStrokedShape(shape);

        Area oldArea = new Area(shape);
        Area outlineArea = new Area(outlineShape);

        Shape backupShape = shape;
        shape = type.modify(oldArea, outlineArea);

        Composition comp = ic.getComp();
        boolean notEmpty = clipToCanvasSize(comp);
        if (notEmpty) {
            SelectionChangeEdit edit = new SelectionChangeEdit(
                    "Modify Selection", comp, backupShape);
            History.addEdit(edit);
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
        History.addEdit(new SelectionChangeEdit(
                "Nudge Selection", ic.getComp(), backupShape));
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hide, boolean fromMenu) {
        assert !dead : "dead selection";

        boolean change = this.hidden != hide;
        if (!change) {
            return;
        }

        this.hidden = hide;

        if(hide) {
            stopMarching();
        } else {
            if(marchingAntsTimer == null) {
                startMarching();
            }
        }

        repaint();

        // if not called from the menu, update the menu name
        if(!fromMenu) {
            ShowHideAction action = SelectionActions.getShowHide();
            if(hide) {
                action.setShowName();
            } else {
                action.setHideName();
            }
        }
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean b) {
        this.frozen = b;
        if (b) {
            stopMarching();
        } else if (!hidden) {
            startMarching();
        }
    }

    // called when the composition is duplicated
    public void setIC(ImageComponent ic) {
        assert ic != null;
        this.ic = ic;
        startMarching();
    }

    public boolean isAlive() {
        return !dead;
    }

    public DebugNode createDebugNode(String name) {
        DebugNode node = new DebugNode(name, this);

        node.addBoolean("hidden", hidden);
        node.addBoolean("dead", dead);
        node.addBoolean("frozen", frozen);
        node.addBoolean("marching", isMarching());

        node.addString("Shape Class", shape.getClass().getName());
        node.addString("Bounds", getShapeBounds().toString());

        return node;
    }

    @Override
    public String toString() {
        return "Selection{" +
                "composition=" + ic.getComp().getName() +
                ", shape-class=" + (shape == null ? "null" : shape.getClass().getName()) +
                ", shapeBounds=" + (shape == null ? "null" : shape.getBounds()) +
                '}';
    }
}
