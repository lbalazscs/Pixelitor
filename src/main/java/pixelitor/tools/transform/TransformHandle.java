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

import java.awt.Color;
import java.awt.geom.Point2D;

public class TransformHandle extends DraggablePoint {
    private final TransformBox box;
    private TransformHandle opposite;
    private TransformHandle verNeighbor;
    private TransformHandle horNeighbor;

    private double sin;
    private double cos;
    private double verOrigX;
    private double verOrigY;
    private double horOrigX;
    private double horOrigY;

    public TransformHandle(String name, TransformBox box, Point2D pos, View ic) {
        super(name, pos.getX(), pos.getY(), ic, Color.WHITE, Color.RED);
        this.box = box;
    }

    @Override
    public void mousePressed(int x, int y) {
        super.mousePressed(x, y); // sets dragStartX, dragStartY
        sin = box.getSin();
        cos = box.getCos();

        verOrigX = verNeighbor.getX();
        verOrigY = verNeighbor.getY();
        horOrigX = horNeighbor.getX();
        horOrigY = horNeighbor.getY();
    }

    @Override
    public void mouseDragged(double x, double y) {
        double dx = x - dragStartX;
        double dy = y - dragStartY;
        double newX = origX + dx;
        double newY = origY + dy;
        setLocation(newX, newY);

        // in order to constrain the neighbors, we need the
        // deltas in the original coordinates
        double odx = dx * cos + dy * sin;
        double ody = -dx * sin + dy * cos;

        // the vertical neighbor is moved by odx
        verNeighbor.setLocation(verOrigX + odx * cos, verOrigY + odx * sin);
        // the horizontal neighbor is moved by ody
        horNeighbor.setLocation(horOrigX - ody * sin, horOrigY + ody * cos);

        box.updateRotLocation();
        box.updateBoxShape();
    }

    @Override
    public void mouseReleased(int x, int y) {
        super.mouseReleased(x, y);
    }

    public void setOpposite(TransformHandle opposite, boolean propagate) {
        this.opposite = opposite;
        if (propagate) {
            opposite.setOpposite(this, false);
        }
    }

    public void setVerNeighbor(TransformHandle verNeighbor, boolean propagate) {
        this.verNeighbor = verNeighbor;
        if (propagate) {
            verNeighbor.setVerNeighbor(this, false);
        }
    }

    public void setHorNeighbor(TransformHandle horNeighbor, boolean propagate) {
        this.horNeighbor = horNeighbor;
        if (propagate) {
            horNeighbor.setHorNeighbor(this, false);
        }
    }
}
