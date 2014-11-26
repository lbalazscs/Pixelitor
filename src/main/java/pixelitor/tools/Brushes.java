/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.tools;

import pixelitor.Composition;
import pixelitor.tools.brushes.Brush;

import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * This class maintains up to four brushes (due to the symmetry drawings)
 * Normally (symmetry = none) there is only one brush;
 */
public class Brushes {
    public static final int MAX_BRUSHES = 4;

    // affected area coordinates
    private int minX = 0;
    private int minY = 0;
    private int maxX = 0;
    private int maxY = 0;

    private Brush[] brushes = new Brush[MAX_BRUSHES];
    private Graphics2D g;
    private int radius = AbstractBrushTool.DEFAULT_BRUSH_RADIUS;
    private Composition comp;

    public Brushes(BrushType brushType) {
        brushTypeChanged(brushType);
    }

    public void brushTypeChanged(BrushType brushType) {
        for (int i = 0; i < MAX_BRUSHES; i++) {
            brushes[i] = brushType.createBrush();
        }
    }

    public void reset() {
        for (int i = 0; i < MAX_BRUSHES; i++) {
            brushes[i].reset();
        }
    }

    public void drawPoint(int brushNo, int x, int y) {
        updateAffectedCoordinates(x, y);

        brushes[brushNo].drawPoint(g, x, y, radius);

        comp.updateRegion(x - radius, y - radius, x + radius + 1, y + radius + 1, 0);
    }

    public void drawLine(int brushNo, int startX, int startY, int endX, int endY) {
        updateAffectedCoordinates(endX, endY);

        brushes[brushNo].drawLine(g, startX, startY, endX, endY, radius);
        comp.updateRegion(startX, startY, endX, endY, 2 * radius);
    }

    public void updateAffectedCoordinates(int x, int y) {
        if (x > maxX) {
            maxX = x;
        } else if (x < minX) {
            minX = x;
        }

        if (y > maxY) {
            maxY = y;
        } else if (y < minY) {
            minY = y;
        }
    }

    public void initAffectedCoordinates(int x, int y) {
        minX = x;
        minY = y;
        maxX = x;
        maxY = y;
    }

    public Rectangle getRectangleAffectedByBrush() {
        // To be on the safe side, save a little more than necessary - some brushes have randomness
        int radius2 = 2 * radius;
        int radius4 = 4 * radius;

        int saveX = minX - radius2;
        int saveY = minY - radius2;
        int saveWidth = maxX - minX + radius4;
        int saveHeight = maxY - minY + radius4;
        Rectangle rectangleAffectedByBrush = new Rectangle(saveX, saveY, saveWidth, saveHeight);
        return rectangleAffectedByBrush;
    }


    public void setDrawingGraphics(Graphics2D g) {
        this.g = g;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public void setComp(Composition comp) {
        this.comp = comp;
    }
}
