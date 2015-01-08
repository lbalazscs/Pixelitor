/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.ImageDisplay;
import pixelitor.tools.UserDrag;
import pixelitor.utils.Dialogs;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Area;

/**
 * Represents a selection on an image.
 */
public class Selection {
    private float dashPhase;
    private ImageDisplay ic;
    private Timer marchingAntsTimer;

    // the shape that is currently drawn
    private Shape currentSelectionShape;

    // the last shape (possibly null)
    // when the mouse is released, the two selections are
    // combined according to the SelectionInteraction
    private Shape lastSelectionShape;

    private SelectionType selectionType;
    private SelectionInteraction selectionInteraction;

    public static final double DASH_WIDTH = 1.0;
    public static final float DASH_LENGTH = 4.0f;
    private static final float[] MARCHING_ANTS_DASH = {DASH_LENGTH, DASH_LENGTH};

    enum State {
        NO_SHAPE_YET { // we had a mouse press, but no drag or mouse up
            @Override
            Rectangle getShapeBounds(Shape currentSelectionShape) {
                throw new IllegalStateException("no shape yet");
            }

            @Override
            Shape getShape(Shape currentSelectionShape) {
                // can be null, for a simple click without a previous selection
                return currentSelectionShape;
            }
        }, HAS_SHAPE { // the normal state with marching ants
            @Override
            Rectangle getShapeBounds(Shape currentSelectionShape) {
                return currentSelectionShape.getBounds(); // bounds are cached in Area;
            }

            @Override
            Shape getShape(Shape currentSelectionShape) {
                if (currentSelectionShape == null) {
                    throw new IllegalStateException("null shape, while in HAS_SHAPE");
                }
                return currentSelectionShape;
            }
        }, DIED { // the selection is no longer there
            @Override
            Rectangle getShapeBounds(Shape currentSelectionShape) {
                throw new IllegalStateException("died");
            }

            @Override
            Shape getShape(Shape currentSelectionShape) {
                throw new IllegalStateException("getShape() called, while in DIED");
            }
        };

        abstract Rectangle getShapeBounds(Shape currentSelectionShape);

        abstract Shape getShape(Shape currentSelectionShape);
    }

    private State state;

    /**
     * Called when a new selection is created with the marquee selection tool
     */
    public Selection(ImageDisplay c, SelectionType selectionType, SelectionInteraction selectionInteraction) {
        this.ic = c;
        this.selectionType = selectionType;
        this.selectionInteraction = selectionInteraction;

        state = State.NO_SHAPE_YET;
    }

//    /**
//     * Called when a new selection is created with the lasso selection tool
//     */
//    public Selection(Component c, LassoSelectionType lassoSelectionType, SelectionInteraction selectionInteraction) {
//        this.component = c;
//
//        this.selectionInteraction = selectionInteraction;
//
//        state = State.NO_SHAPE_YET;
//    }


    /**
     * Called when a selection is created from a shape:
     * - when a deselect is undone (and when a  new selection is undone and then redone)
     * - selections from the Shapes Tool
     */
    public Selection(Shape shape, ImageDisplay c) {
        assert c != null;

        this.currentSelectionShape = shape;
        this.ic = c;

        // selectionType is not set, but this shouldn't be a problem

        startMarching();
        state = State.HAS_SHAPE;
    }

    /**
     * As the mouse is dragged or released, the current selection shape is continuously updated
     */
    public void updateSelection(UserDrag userDrag) {
        currentSelectionShape = selectionType.updateShape(userDrag, currentSelectionShape);

        if (state == State.NO_SHAPE_YET) {
            startMarching();
            state = State.HAS_SHAPE;
        }
    }

    /**
     * We get here if there is already a selection when the user starts a new selection shape
     */
    public void startNewShape(SelectionType selectionType, SelectionInteraction selectionInteraction) {
        if (state != State.HAS_SHAPE) {
            throw new IllegalStateException("state = " + state);
        }

        this.selectionType = selectionType;
        this.selectionInteraction = selectionInteraction;

        if (selectionInteraction != SelectionInteraction.REPLACE) {
            lastSelectionShape = currentSelectionShape;
        }
    }

    /**
     * The mouse has been released and the currently drawn shape must be combined
     * with the already existing shape according to the selection interaction type
     *
     * @return true if something is still selected
     */
    public boolean combineShapes() {
        boolean somethingSelected = true;
        if (lastSelectionShape != null) {
            currentSelectionShape = selectionInteraction.combine(lastSelectionShape, currentSelectionShape);

            Rectangle newBounds = currentSelectionShape.getBounds();
            if (newBounds.isEmpty()) {
                currentSelectionShape = null;
                updateComponent();
                if (!Build.CURRENT.isRobotTest()) {
                    Dialogs.showInfoDialog("Nothing selected", "As a result of the "
                            + selectionInteraction.toString().toLowerCase() + " operation, nothing is selected now.");
                }
                somethingSelected = false;
            }

            lastSelectionShape = null;
        } else {
            // there is the current selection only but it also can be empty if it has width or height = 0
            if (currentSelectionShape.getBounds().isEmpty()) {
                somethingSelected = false;
            }
        }
        return somethingSelected;
    }

    private void startMarching() {
        assert ic != null;

        marchingAntsTimer = new Timer(100, null);
        marchingAntsTimer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                dashPhase += 1 / ic.getViewScale();

                updateComponent();
            }
        });
        marchingAntsTimer.start();
    }

    public void paintMarchingAnts(Graphics2D g2) {
        if (currentSelectionShape == null) {
            return;
        }

        // while a new selection is drawn with marching ants, the
        // old selection is drawn frozen (phase is always 0)
        paintAnts(g2, lastSelectionShape, 0);
        paintAnts(g2, currentSelectionShape, dashPhase);
    }

    private void paintAnts(Graphics2D g2, Shape shape, float phase) {
        if (shape == null) {
            return;
        }

        double viewScale = ic.getViewScale();
        float lineWidth = (float) (DASH_WIDTH / viewScale);

        g2.setPaint(Color.WHITE);

        float[] dash;
        if (viewScale == 1.0) { // the most common case
            dash = MARCHING_ANTS_DASH;
        } else {
            float scaledDashLength = (float) (DASH_LENGTH / viewScale);
            dash = new float[]{scaledDashLength, scaledDashLength};
        }

        Stroke stroke = new BasicStroke(lineWidth, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_ROUND, 0.0f, dash,
                phase);
        g2.setStroke(stroke);
        g2.draw(shape);

        g2.setPaint(Color.BLACK);
        Stroke stroke2 = new BasicStroke(lineWidth, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_ROUND, 0.0f, dash,
                (float) (phase + DASH_LENGTH / viewScale));
        g2.setStroke(stroke2);
        g2.draw(shape);
    }

    /**
     * Inverts the selection shape.
     */
    public void invert(Rectangle fullImage) {
        if (currentSelectionShape != null) {
            Area area = new Area(currentSelectionShape);
            Area fullArea = new Area(fullImage);
            fullArea.subtract(area);
            currentSelectionShape = fullArea;
        }
    }

    public void deselectAndDispose() {
        switch (state) {
            case NO_SHAPE_YET:
                state = State.DIED;
                break;
            case HAS_SHAPE:
                marchingAntsTimer.stop();
                updateComponent();
                ic = null;
                state = State.DIED;
                break;
            case DIED:
                throw new IllegalStateException("died twice");
        }
    }

    private void updateComponent() {
//        Rectangle selBounds = currentSelectionShape.getBounds();
//
//        if(lastSelectionShape != null) {
//            Rectangle r = lastSelectionShape.getBounds();
//            selBounds = selBounds.union(r);
//        }
//
//        component.repaint(selBounds.x, selBounds.y, selBounds.width + 1, selBounds.height + 1);

        // TODO the above optimization is not enough, the previous positions should be also considered for the
        // case when the selection is shrinking while dragging...

        ic.repaint();
    }

    /**
     * The selection tool does not need this method, but sometimes the shape
     * of the selection must be set directly
     */
    public void setShape(Shape selectionShape) {
        currentSelectionShape = selectionShape;
    }

    public SelectionInteraction getSelectionInteraction() {
        return selectionInteraction;
    }

    /**
     * Intersects the selection shape with the composition bounds
     *
     * @return true if something is still selected
     */
    public boolean clipToCompSize(Composition comp) {
        if (currentSelectionShape != null) {
            Canvas canvas = comp.getCanvas();
            Area compBounds = new Area(new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight()));
            Area tmp = new Area(currentSelectionShape);
            tmp.intersect(compBounds);
            currentSelectionShape = tmp;
            updateComponent();

            return !currentSelectionShape.getBounds().isEmpty();
        }
        return false;
    }

    public void addNewPolygonalLassoPoint(UserDrag userDrag) {
        Polygon polygon = (Polygon) currentSelectionShape;
        int[] xPoints = polygon.xpoints;
        int[] yPoints = polygon.ypoints;
        int nPoints = polygon.npoints;

        int newNPoints = nPoints + 1;
        int[] newXPoints = new int[newNPoints];
        int[] newYPoints = new int[newNPoints];

//        for (int i = 0; i < nPoints; i++) {
//            newXPoints[i] = xPoints[i];
//            newYPoints[i] = yPoints[i];
//        }

        System.arraycopy(xPoints, 0, newXPoints, 0, nPoints);
        System.arraycopy(yPoints, 0, newXPoints, 0, nPoints);

        newXPoints[newNPoints - 1] = userDrag.getEndX();
        newYPoints[newNPoints - 1] = userDrag.getEndY();

        currentSelectionShape = new Polygon(newXPoints, newYPoints, newNPoints);
    }

    public Shape getShape() {
        return state.getShape(currentSelectionShape);
    }

    /**
     * Returns the shape bounds of the selection
     * Like everything else in this class, this is in image coordinates
     */
    public Rectangle getShapeBounds() {
        return state.getShapeBounds(currentSelectionShape);
    }

    @Override
    public String toString() {
        return "Selection{" +
                "composition=" + ic.getComp().getName() +
                ", currentSelectionShape-class=" + (currentSelectionShape == null ? "null" : currentSelectionShape.getClass().getName()) +
                ", currentSelectionShapeBounds=" + (currentSelectionShape == null ? "null" : currentSelectionShape.getBounds()) +
                ", lastSelectionShape-class=" + (lastSelectionShape == null ? "null" : lastSelectionShape.getClass().getName()) +
                ", selectionType=" + selectionType +
                ", selectionInteraction=" + selectionInteraction +
                ", state=" + state +
                '}';
    }
}
