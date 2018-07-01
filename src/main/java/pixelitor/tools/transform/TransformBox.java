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

package pixelitor.tools.transform;

import pixelitor.gui.View;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.utils.Shapes;
import pixelitor.utils.VisibleForTesting;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * A widget that calculates an {@link AffineTransform}
 * based on the interactive movement of handles
 */
public class TransformBox {
    private static final int ROT_HANDLE_DISTANCE = 20;

    private final TransformHandle nw;
    private final TransformHandle ne;
    private final TransformHandle se;
    private final TransformHandle sw;
    private final RotationHandle rot;
    private final Consumer<AffineTransform> transformListener;
    private final List<DraggablePoint> handles;
    private final View view;

    private Point2D origNW;
    private Point2D origNE;
    private Point2D origSE;
    private Point2D origSW;

    // the starting position of the box, corresponding to
    // the default size of the transformed object
    private final Rectangle origCoRect;
    private final Rectangle2D origImRect;

    private DraggablePoint activeHandle;

    private double angle = 0.0;
    private double sin = 0.0;
    private double cos = 1.0;

    // the box shape in component coordinates
    private GeneralPath boxShape;

    private boolean globalDrag = false;
    private int globalDragStartX;
    private int globalDragStartY;

    public TransformBox(Rectangle start, View view,
                        Consumer<AffineTransform> transformListener) {
        this.origCoRect = start;
        origImRect = view.componentToImageSpace(origCoRect);
        this.view = view;

        int eastX = start.x + start.width;
        int southY = start.y + start.height;

        nw = new TransformHandle("NW", this,
                new Point2D.Double(start.x, start.y),
                view, Color.WHITE);
        ne = new TransformHandle("NE", this,
                new Point2D.Double(eastX, start.y),
                view, Color.WHITE);
        se = new TransformHandle("SE", this,
                new Point2D.Double(eastX, southY),
                view, Color.WHITE);
        sw = new TransformHandle("SW", this,
                new Point2D.Double(start.x, southY),
                view, Color.WHITE);

        Point2D center = Shapes.calcCenter(ne, sw);

        rot = new RotationHandle("rot", this,
                new Point2D.Double(center.getX(), ne.getY() - ROT_HANDLE_DISTANCE), view);
        this.transformListener = transformListener;

        nw.setVerNeighbor(sw, true);
        nw.setHorNeighbor(ne, true);

        se.setHorNeighbor(sw, true);
        se.setVerNeighbor(ne, true);

        updateBoxShape();

        handles = Arrays.asList(nw, ne, se, sw, rot);
    }

    public void rotate(AffineTransform rotate) {
//        this.rotate = rotate;
        rotateHandlePositions(rotate);
        view.repaint();
    }

    public AffineTransform getImTransform() {
        AffineTransform at = new AffineTransform();

        if (angle != 0) {
            // rotate with origin at NW
            at.rotate(angle, nw.imX, nw.imY);
        }

        // scale with origin at NW
        at.translate(nw.imX, nw.imY);
        double scaleX = calcImWidth() / origImRect.getWidth();
        double scaleY = calcImHeight() / origImRect.getHeight();
        at.scale(scaleX, scaleY);
//        at.translate(-nw.imX, -nw.imY);
//
//        // first step: translate
//        double tx = nw.imX - origImRect.getX();
//        double ty = nw.imY - origImRect.getY();
//        at.translate(tx, ty);

        // the two commented out translates above merged into one
        at.translate(-origImRect.getX(), -origImRect.getY());

        return at;
    }

    // calculate the width of the rotated rectangle in image space
    private double calcImWidth() {
        if (cos > sin) {
            return (ne.imX - nw.imX) / cos;
        } else {
            // precision is better if the calculation
            // is based on y coordinates
            return (ne.imY - nw.imY) / sin;
        }
    }

    // calculate the height of the rotated rectangle in image space
    private double calcImHeight() {
        if (cos > sin) {
            return (sw.imY - nw.imY) / cos;
        } else {
            // precision is better if the calculation
            // is based on x coordinates
            return (nw.imX - sw.imX) / sin;
        }
    }

    public void copyHandleLocations() {
        origNW = nw.getLocationCopy();
        origNE = ne.getLocationCopy();
        origSE = se.getLocationCopy();
        origSW = sw.getLocationCopy();
    }

    public void rotateHandlePositions(AffineTransform at) {
        at.transform(origNW, nw);
        at.transform(origNE, ne);
        at.transform(origSE, se);
        at.transform(origSW, sw);

        handlePositionsChanged();
    }

    public void handlePositionsChanged() {
        updateRotLocation();
        updateBoxShape();
        transformListener.accept(getImTransform());
    }

    public void updateRotLocation() {
        Point2D northCenter = Shapes.calcCenter(nw, ne);

        double rotDistX = ROT_HANDLE_DISTANCE * sin;
        double rotDistY = ROT_HANDLE_DISTANCE * cos;
        if (calcImHeight() < 0) {
            rotDistX *= -1;
            rotDistY *= -1;
        }

        double rotX = northCenter.getX() + rotDistX;
        double rotY = northCenter.getY() - rotDistY;
        rot.setLocation(new Point2D.Double(rotX, rotY));
    }

    public void paint(Graphics2D g) {
        // paint the lines
        Shapes.drawVisible(g, boxShape);
        Line2D line = new Line2D.Double(Shapes.calcCenter(nw, ne), rot);
        Shapes.drawVisible(g, line);

        // paint the handles
        for (DraggablePoint handle : handles) {
            handle.paintHandle(g);
        }
    }

    public void updateBoxShape() {
        boxShape = new GeneralPath();
        boxShape.moveTo(nw.getX(), nw.getY());
        boxShape.lineTo(ne.getX(), ne.getY());
        boxShape.lineTo(se.getX(), se.getY());
        boxShape.lineTo(sw.getX(), sw.getY());
        boxShape.lineTo(nw.getX(), nw.getY());
        boxShape.closePath();
    }

    DraggablePoint handleWasHit(int x, int y) {
        for (DraggablePoint handle : handles) {
            if (handle.handleContains(x, y)) {
                return handle;
            }
        }
        return null;
    }

    public void mousePressed(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        DraggablePoint handle = handleWasHit(x, y);
        if (handle != null) {
            handle.setActive(true);
            handle.mousePressed(x, y);
            activeHandle = handle;
            view.repaint();
        } else {
            activeHandle = null;
            if (boxShape.contains(x, y)) {
                globalDrag = true;
                globalDragStartX = x;
                globalDragStartY = y;
                copyHandleLocations();
            }
        }
    }

    public void mouseDragged(MouseEvent e) {
        if (activeHandle != null) {
            activeHandle.mouseDragged(e.getX(), e.getY());
            view.repaint();
        } else if (globalDrag) {
            dragAll(e.getX(), e.getY());
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (activeHandle != null) {
            int x = e.getX();
            int y = e.getY();
            activeHandle.mouseReleased(x, y);
            if (!activeHandle.handleContains(x, y)) {
                // we can get here if the handle has a
                // constrained position
                activeHandle.setActive(false);
                activeHandle = null;
            }
            view.repaint();
        } else if (globalDrag) {
            dragAll(e.getX(), e.getY());
            globalDrag = false;
        }
    }

    private void dragAll(int x, int y) {
        int dx = x - globalDragStartX;
        int dy = y - globalDragStartY;

        nw.setLocation(origNW.getX() + dx, origNW.getY() + dy);
        ne.setLocation(origNE.getX() + dx, origNE.getY() + dy);
        se.setLocation(origSE.getX() + dx, origSE.getY() + dy);
        sw.setLocation(origSW.getX() + dx, origSW.getY() + dy);

        handlePositionsChanged();
        view.repaint();
    }

    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        DraggablePoint handle = handleWasHit(x, y);
        if (handle != null) {
            handle.setActive(true);
            activeHandle = handle;
            view.repaint();
        } else {
            if (activeHandle != null) {
                activeHandle.setActive(false);
                activeHandle = null;
                view.repaint();
            }
        }
    }

    public Point2D getCenter() {
        return Shapes.calcCenter(nw, se);
    }

    public void viewSizeChanged(View view) {
        for (DraggablePoint handle : handles) {
            handle.restoreCoordsFromImSpace(view);
        }
        updateBoxShape();
    }

    public void setAngle(double angle) {
        this.angle = angle;
        cos = Math.cos(angle);
        sin = Math.sin(angle);
    }

    public double getSin() {
        return sin;
    }

    public double getCos() {
        return cos;
    }

    @VisibleForTesting
    public TransformHandle getNW() {
        return nw;
    }

    @VisibleForTesting
    public TransformHandle getNE() {
        return ne;
    }

    @VisibleForTesting
    public TransformHandle getSE() {
        return se;
    }

    @VisibleForTesting
    public TransformHandle getSW() {
        return sw;
    }
}
