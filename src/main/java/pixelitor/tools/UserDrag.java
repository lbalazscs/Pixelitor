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

package pixelitor.tools;

import pixelitor.gui.ImageComponent;
import pixelitor.utils.Shapes;
import pixelitor.utils.Utils;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;

/**
 * Represents the mouse drag on the image made
 * by the user while using a {@link DragTool}.
 * Only the start and end points are relevant.
 */
public class UserDrag {
    private final DragTool tool;
    private boolean finished;

    // The coordinates in the component (mouse) space.
    private int coStartX;
    private int coEndX;
    private int coStartY;
    private int coEndY;

    // The coordinates in image space
    private double imStartX;
    private double imStartY;
    private double imEndX;
    private double imEndY;

    private double oldImEndX;
    private double oldImEndY;

    private ImageComponent ic;

    private boolean constrainPoints = false;
    private boolean startFromCenter = false;

    public UserDrag(DragTool tool) {
        this.tool = tool;
    }

    public void setStart(PMouseEvent e) {
        assert e.getIC() != null;
        this.ic = e.getIC();

        coStartX = e.getCoX();
        coStartY = e.getCoY();

        imStartX = e.getImX();
        imStartY = e.getImY();
    }

    public void setEnd(PMouseEvent e) {
        assert ic != null;
        assert this.ic == e.getIC() : "ic changed for " + tool.getName()
                + ", was " + this.ic.getName()
                + ", is " + ic.getName();

        coEndX = e.getCoX();
        coEndY = e.getCoY();

        if (constrainPoints) {
            Point2D constrainedEnd = Utils.constrainEndPoint(coStartX, coStartY, coEndX, coEndY);
            coEndX = (int) constrainedEnd.getX();
            coEndY = (int) constrainedEnd.getY();
        }

        imEndX = ic.componentXToImageSpace(coEndX);
        imEndY = ic.componentYToImageSpace(coEndY);
    }

    // returns the start x coordinate in image space
    public double getImStartX() {
        return imStartX;
    }

    // returns the start y coordinate in image space
    public double getImStartY() {
        return imStartY;
    }

    // returns the end x coordinate in image space
    public double getImEndX() {
        return imEndX;
    }

    // returns the end y coordinate in image space
    public double getImEndY() {
        return imEndY;
    }

    // returns the start x coordinate in component space
    public int getCoStartX() {
        return coStartX;
    }

    // returns the start y coordinate in component space
    public int getCoStartY() {
        return coStartY;
    }

    // returns the end x coordinate in component space
    public int getCoEndX() {
        return coEndX;
    }

    // returns the end y coordinate in component space
    public int getCoEndY() {
        return coEndY;
    }

    public boolean isClick() {
        return ((coStartX == coEndX) && (coStartY == coEndY));
    }

    public ImDrag toImDrag() {
        ImDrag d = new ImDrag(imStartX, imStartY, imEndX, imEndY);
        d.setStartFromCenter(startFromCenter);
        return d;
    }

    public void setConstrainPoints(boolean constrainPoints) {
        this.constrainPoints = constrainPoints;
    }

    public void drawGradientArrow(Graphics2D g) {
        Shapes.drawGradientArrow(g, coStartX, coStartY, coEndX, coEndY);
    }

    public void saveEndValues() {
        oldImEndX = imEndX;
        oldImEndY = imEndY;
        // it is not necessary to save
        // the component end coordinates for space-down move
    }

    public void adjustStartForSpaceDownMove() {
        double dx = imEndX - oldImEndX;
        double dy = imEndY - oldImEndY;

        imStartX += dx;
        imStartY += dy;

        // it is not necessary to update
        // the component coordinates for space-down move
    }

    public void setStartFromCenter(boolean startFromCenter) {
        this.startFromCenter = startFromCenter;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    //    public Rectangle getAffectedStrokedRect(int thickness) {
//        Rectangle r = createPositiveRect();
//        if (thickness == 0) {
//            return r;
//        }
//
//        int halfThickness = thickness / 2 + 1;
//        int sizeEnlargement = thickness + 2;
//
//        r.setBounds(r.x - halfThickness, r.y - halfThickness, r.width + sizeEnlargement, r.height + sizeEnlargement);
//        return r;
//    }
}
