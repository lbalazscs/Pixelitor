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

import java.awt.Color;
import java.awt.geom.Point2D;

/**
 * A corner handle of a {@link TransformBox}
 */
public class TransformHandle extends DraggablePoint {
    private final TransformBox box;
    private TransformHandle verNeighbor;
    private TransformHandle horNeighbor;

    private double sin;
    private double cos;
    private double verOrigX;
    private double verOrigY;
    private double horOrigX;
    private double horOrigY;

    public TransformHandle(String name, TransformBox box, Point2D pos, View view, Color c) {
        super(name, pos.getX(), pos.getY(), view, c, Color.RED);
        this.box = box;
    }

    @Override
    public void setLocation(double x, double y) {
        super.setLocation(x, y);

        // the image space coordinates need to be updated continuously
        // because the transform calculations are based on them
        calcImCoords();
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

        // calculate the deltas in the original coordinate system
        double odx = dx * cos + dy * sin;
        double ody = -dx * sin + dy * cos;

        // the vertical neighbor is moved only by odx
        verNeighbor.setLocation(verOrigX + odx * cos, verOrigY + odx * sin);

        // the horizontal neighbor is moved only by ody
        horNeighbor.setLocation(horOrigX - ody * sin, horOrigY + ody * cos);

        box.handlePositionsChanged();
    }

    @Override
    public void mouseReleased(int x, int y) {
        super.mouseReleased(x, y);

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
