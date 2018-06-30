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
import pixelitor.tools.DraggablePoint;
import pixelitor.utils.Shapes;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class TransformBox {
    private static final int ROT_HANDLE_DISTANCE = 20;

//    /**
//     * This transforms the original box into its current shape
//     * in component space.
//     */
//    private AffineTransform coTransform = new AffineTransform();

    private final TransformHandle nw;
    private final TransformHandle ne;
    private final TransformHandle se;
    private final TransformHandle sw;
    private final RotationHandle rot;
    private final Consumer<AffineTransform> transformListener;
    private final List<DraggablePoint> handles;
    private final View ic;

    private Point2D origNW;
    private Point2D origNE;
    private Point2D origSE;
    private Point2D origSW;

    // the starting position of the box, corresponding to
    // the default size of the transformed object
    private final Rectangle origRect;

    private DraggablePoint activeHandle;

    // the final transformation will be calculated
    // as first a translation, then a scale, and then a rotation
    private double tx;
    private double ty;
    private double scaleX;
    private double scaleY;
    private double angle = 0.0;
    private double sin = 0.0;
    private double cos = 1.0;
    private GeneralPath boxShape;
    //    private AffineTransform rotate = new AffineTransform();
//    private AffineTransform translationScale = new AffineTransform();

    public TransformBox(Rectangle start, View ic,
                        Consumer<AffineTransform> transformListener) {
        this.origRect = start;
        this.ic = ic;

        int eastX = start.x + start.width;
        int southY = start.y + start.height;

        nw = new TransformHandle("NW", this,
                new Point2D.Double(start.x, start.y),
                ic);
        ne = new TransformHandle("NE", this,
                new Point2D.Double(eastX, start.y),
                ic);
        se = new TransformHandle("SE", this,
                new Point2D.Double(eastX, southY),
                ic);
        sw = new TransformHandle("SW", this,
                new Point2D.Double(start.x, southY),
                ic);

        Point2D center = Shapes.calcCenter(ne, sw);

        rot = new RotationHandle("rot", this,
                new Point2D.Double(center.getX(), ne.getY() - ROT_HANDLE_DISTANCE), ic);
        this.transformListener = transformListener;
        nw.setOpposite(se, true);
        ne.setOpposite(sw, true);

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
        transformListener.accept(getCoTransform());
        ic.repaint();
    }
//
//    public void setTranslationScale(AffineTransform translationScale) {
//        this.translationScale = translationScale;
//        transformListener.accept(getCoTransform());
//        ic.repaint();
//    }

    public AffineTransform getCoTransform() {
        AffineTransform at = new AffineTransform();
//        at.concatenate(rotate);
//        at.concatenate(translationScale);
        return at;
    }

    public void prepareRotation() {
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

        updateRotLocation();
        updateBoxShape();
    }

    public void updateRotLocation() {
        Point2D northCenter = Shapes.calcCenter(nw, ne);
        double rotX = northCenter.getX() + ROT_HANDLE_DISTANCE * sin;
        double rotY = northCenter.getY() - ROT_HANDLE_DISTANCE * cos;
        rot.setLocation(new Point2D.Double(rotX, rotY));
    }

//    public void applyCurrentTransform() {
//        System.out.println("TransformBox::applyCurrentTransform: CALLED");
//        assert draggedRotatedRect != null;
//
//        rotatedRect = draggedRotatedRect;
//        draggedRotatedRect = null;
//        coTransformSoFar.preConcatenate(currentCoTransform);
//        currentCoTransform = null;
//    }

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
            ic.repaint();
        } else {
            activeHandle = null;
        }
    }

    public void mouseDragged(MouseEvent e) {
        if (activeHandle != null) {
            int x = e.getX();
            int y = e.getY();
            activeHandle.mouseDragged(x, y);
            ic.repaint();
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (activeHandle != null) {
            int x = e.getX();
            int y = e.getY();
            activeHandle.mouseReleased(x, y);
            ic.repaint();
        }
    }

    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        DraggablePoint handle = handleWasHit(x, y);
        if (handle != null) {
            handle.setActive(true);
            activeHandle = handle;
            ic.repaint();
        } else {
            if (activeHandle != null) {
                activeHandle.setActive(false);
                ic.repaint();
                activeHandle = null;
            }
        }
    }

    public TransformHandle getNW() {
        return nw;
    }

    public TransformHandle getNE() {
        return ne;
    }

    public TransformHandle getSE() {
        return se;
    }

    public TransformHandle getSW() {
        return sw;
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
}
