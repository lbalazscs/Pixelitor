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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * Represents the mouse drag on the image made
 * by the user while using a tool.
 */
public class UserDrag {
    private final Tool tool;
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
    private boolean startFromCenter;

    public UserDrag(Tool tool) {
        this.tool = tool;
    }

    public void setStart(MouseEvent e, ImageComponent ic) {
        assert ic != null;

        coStartX = e.getX();
        coStartY = e.getY();
        this.ic = ic;

        imStartX = ic.componentXToImageSpace(coStartX);
        imStartY = ic.componentYToImageSpace(coStartY);
    }

    public void setEnd(MouseEvent e, ImageComponent ic) {
        assert ic != null;
        assert this.ic == ic : "ic changed for " + tool.getName()
                + ", was " + this.ic.getName()
                + ", is " + ic.getName();

        coEndX = e.getX();
        coEndY = e.getY();

        if (constrainPoints) {
            int dx = Math.abs(coEndX - coStartX);
            int dy = Math.abs(coEndY - coStartY);

            if (dx > dy) {
                coEndY = coStartY;
            } else {
                coEndX = coStartX;
            }
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

    private static final Stroke stroke3 = new BasicStroke(3);
    private static final Stroke stroke1 = new BasicStroke(1);

    public void drawGradientToolHelper(Graphics2D g) {
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        Line2D line = new Line2D.Double(coStartX, coStartY, coEndX, coEndY);

        g.setColor(Color.BLACK);
        g.setStroke(stroke3);
        g.draw(line);

        g.setColor(Color.WHITE);
        g.setStroke(stroke1);
        g.draw(line);
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
