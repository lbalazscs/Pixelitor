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

package pixelitor.tools.util;

import pixelitor.Build;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.tools.DragTool;
import pixelitor.utils.Shapes;
import pixelitor.utils.Utils;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

/**
 * Represents the mouse drag on the image made
 * by the user while using a {@link DragTool}.
 * Only the start and end points are relevant.
 */
public class UserDrag {
    private final DragTool tool;

    private boolean dragging;

    // The coordinates in the component (mouse) space.
    private int coStartX;
    private int coEndX;
    private int coStartY;
    private int coEndY;

    // The coordinates in image space
    private double imStartX;
    private double imStartY;
//    private double imEndX;
//    private double imEndY;

    private int prevCoEndX;
    private int prevCoEndY;

    private ImageComponent ic;

    private boolean constrainPoints = false;
    private boolean startFromCenter = false;

    public UserDrag(DragTool tool) {
        this.tool = tool;
    }

    public void setStart(PMouseEvent e) {
        assert e.getIC() != null;
        assert ImageComponents.isActive(e.getIC());

        this.ic = e.getIC();

        coStartX = e.getCoX();
        coStartY = e.getCoY();

        imStartX = e.getImX();
        imStartY = e.getImY();
    }

    public void setEnd(PMouseEvent e) {
        assert ic != null;

        if (this.ic != e.getIC()) { // TODO happens in random tests
            if (Build.CURRENT.isDevelopment()) {
                System.out.println("\nUserDrag::setEnd: " +
                        "another ic x = " + e.getCoX() + ", y = " + e
                        .getCoY() + ", tool name = " + tool.getName());
                boolean thisIsActive = ImageComponents.isActive(this.ic);
                boolean thatIsActive = ImageComponents.isActive(e.getIC());
                System.out.println("UserDrag::setEnd: "
                        + "thisIsActive = " + thisIsActive
                        + ", thatIsActive = " + thatIsActive);
            }
            return;
        }

        coEndX = e.getCoX();
        coEndY = e.getCoY();

        if (constrainPoints) {
            Point2D constrainedEnd = Utils.constrainEndPoint(coStartX, coStartY, coEndX, coEndY);
            coEndX = (int) constrainedEnd.getX();
            coEndY = (int) constrainedEnd.getY();
        }

        dragging = true;
//        imEndX = ic.componentXToImageSpace(coEndX);
//        imEndY = ic.componentYToImageSpace(coEndY);
    }

    // returns the start x coordinate in image space
    public double getImStartX() {
        return imStartX;
    }

    // returns the start y coordinate in image space
    public double getImStartY() {
        return imStartY;
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
        double imEndX = ic.componentXToImageSpace(coEndX);
        double imEndY = ic.componentYToImageSpace(coEndY);

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
        prevCoEndX = coEndX;
        prevCoEndY = coEndY;
    }

    public void adjustStartForSpaceDownDrag() {
        int dx = coEndX - prevCoEndX;
        int dy = coEndY - prevCoEndY;

        coStartX += dx;
        coStartY += dy;

        imStartX = ic.componentXToImageSpace(coStartX);
        imStartY = ic.componentYToImageSpace(coStartY);
    }

    public void setStartFromCenter(boolean startFromCenter) {
        this.startFromCenter = startFromCenter;
    }

    public boolean isDragging() {
        return dragging;
    }

    public void mouseReleased() {
        this.dragging = false;
    }

    public Rectangle toCoRect() {
        return new Rectangle(coStartX, coStartY, coEndX - coStartX, coEndY - coStartY);
    }

    public PRectangle toPosPRect() {
        return PRectangle.positiveFromCo(toCoRect(), ic);
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
