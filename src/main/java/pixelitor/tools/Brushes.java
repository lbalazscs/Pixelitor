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

package pixelitor.tools;

import pixelitor.Composition;
import pixelitor.tools.brushes.Brush;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.function.Supplier;

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

    private final Brush[] brushes = new Brush[MAX_BRUSHES];
    private int radius = AbstractBrushTool.DEFAULT_BRUSH_RADIUS;
    //    private Composition comp;
    private int numInstantiatedBrushes;
    private Supplier<Brush> brushSupplier;

    Brushes(Supplier<Brush> brushSupplier, Symmetry symmetry) {
        this.brushSupplier = brushSupplier;
        numInstantiatedBrushes = symmetry.getNumBrushes();
        assert numInstantiatedBrushes <= MAX_BRUSHES;
        brushTypeChanged(brushSupplier);
    }

    public void brushTypeChanged(Supplier<Brush> brushSupplier) {
        this.brushSupplier = brushSupplier;
        for(int i = 0; i < numInstantiatedBrushes; i++) {
            brushes[i] = brushSupplier.get();
        }
    }

    public void symmetryChanged(Symmetry symmetry) {
        if(symmetry.getNumBrushes() > numInstantiatedBrushes) {
            // we need to create more brushes of the same type
            int newNumBrushes = symmetry.getNumBrushes();
            assert newNumBrushes <= MAX_BRUSHES;
            for(int i = numInstantiatedBrushes; i < newNumBrushes; i++) {
                brushes[i] = brushSupplier.get();
            }
            numInstantiatedBrushes = newNumBrushes;
        }
    }

    public void onDragStart(int brushNo, int x, int y) {
        updateAffectedCoordinates(x, y);

        brushes[brushNo].onDragStart(x, y);

//        comp.updateRegion(x - radius, y - radius, x + radius + 1, y + radius + 1, 0);
    }

    public void onNewMousePoint(int brushNo, int endX, int endY) {
        updateAffectedCoordinates(endX, endY);

        if(radius <= 0) {
            throw new IllegalStateException("radius is " + radius);
        }

        brushes[brushNo].onNewMousePoint(endX, endY);
//        comp.updateRegion(startX, startY, endX, endY, 2 * radius);
    }

    public void updateAffectedCoordinates(int x, int y) {
        if(x > maxX) {
            maxX = x;
        } else if(x < minX) {
            minX = x;
        }

        if(y > maxY) {
            maxY = y;
        } else if(y < minY) {
            minY = y;
        }
    }

    public void initAffectedCoordinates(int x, int y) {
        minX = x;
        minY = y;
        maxX = x;
        maxY = y;
    }

    /**
     * Calculates the rectangle affected by a brush stroke for the undo mechanism
     */
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


    public void setTarget(Composition comp, Graphics2D g) {
        for(int i = 0; i < numInstantiatedBrushes; i++) {
            brushes[i].setTarget(comp, g);
        }
    }

    public void setRadius(int radius) {
        this.radius = radius;
        for(int i = 0; i < numInstantiatedBrushes; i++) {
            assert brushes[i] != null : "i = " + i + ", numInstantiatedBrushes = " + numInstantiatedBrushes;
            brushes[i].setRadius(radius);
        }
    }
}
