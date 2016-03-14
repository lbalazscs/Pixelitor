/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import pixelitor.Composition;
import pixelitor.gui.ImageComponent;
import pixelitor.history.History;
import pixelitor.history.SelectionChangeEdit;
import pixelitor.menus.view.ShowHideAction;

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

    // the shape that is currently drawn
    private Shape shape;

    private static final double DASH_WIDTH = 1.0;
    private static final float DASH_LENGTH = 4.0f;
    private static final float[] MARCHING_ANTS_DASH = {DASH_LENGTH, DASH_LENGTH};

    private boolean hidden = false;
    private boolean dead = false;
    private boolean frozen = false;

    public Selection(Shape shape, ImageComponent ic) {
        assert ic != null;

        this.shape = shape;
        this.ic = ic;

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
                dashPhase += 1 / ic.getViewScale();
                repaint();
            }
        });
        marchingAntsTimer.start();
    }

    public void stopMarching() {
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
        double viewScale = ic.getViewScale();
        float lineWidth = (float) (DASH_WIDTH / viewScale);

        g2.setPaint(WHITE);

        float[] dash;
        if (viewScale == 1.0) { // the most common case
            dash = MARCHING_ANTS_DASH;
        } else {
            float scaledDashLength = (float) (DASH_LENGTH / viewScale);
            dash = new float[]{scaledDashLength, scaledDashLength};
        }

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

    /**
     * Inverts the selection shape.
     */
    public void invert(Rectangle fullImage) {
        if (shape != null) {
            Area area = new Area(shape);
            Area fullArea = new Area(fullImage);
            fullArea.subtract(area);
            shape = fullArea;
        }
    }

    public void die() {
        stopMarching();
        repaint();
        ic = null;
        dead = true;
    }

    public void repaint() {
//        Rectangle selBounds = shape.getBounds();
//
//        if(lastShape != null) {
//            Rectangle r = lastShape.getBounds();
//            selBounds = selBounds.union(r);
//        }
//
//        component.repaint(selBounds.x, selBounds.y, selBounds.width + 1, selBounds.height + 1);

        // TODO the above optimization is not enough, the previous positions should be also considered for the
        // case when the selection is shrinking while dragging...

        ic.repaint();
    }

    public void setShape(Shape currentShape) {
        this.shape = currentShape;
    }

    /**
     * Intersects the selection shape with the composition bounds
     *
     * @return true if something is still selected
     */
    public boolean clipToCompSize(Composition comp) {
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
     * (but relative to the composition, not to the image)
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
        shape = type.createModifiedShape(oldArea, outlineArea);

        SelectionChangeEdit edit = new SelectionChangeEdit(ic.getComp(), backupShape, "Modify Selection");
        History.addEdit(edit);
    }

    public Shape transform(AffineTransform at) {
        Shape backupShape = shape;
        shape = at.createTransformedShape(shape);
        return backupShape;
    }

    public void nudge(AffineTransform at) {
        Shape backupShape = transform(at);
        History.addEdit(new SelectionChangeEdit(ic.getComp(), backupShape, "Nudge Selection"));
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
            ShowHideAction action = SelectionActions.getShowHideSelectionAction();
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

    @Override
    public String toString() {
        return "Selection{" +
                "composition=" + ic.getComp().getName() +
                ", shape-class=" + (shape == null ? "null" : shape.getClass().getName()) +
                ", shapeBounds=" + (shape == null ? "null" : shape.getBounds()) +
                '}';
    }
}
