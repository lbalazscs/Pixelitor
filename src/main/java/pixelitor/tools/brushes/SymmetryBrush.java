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

package pixelitor.tools.brushes;

import pixelitor.Composition;
import pixelitor.tools.AbstractBrushTool;
import pixelitor.tools.Symmetry;

import java.awt.Graphics2D;
import java.util.function.Supplier;

/**
 * Delegates the work to other brushes according to the symmetry and brush type settings
 */
public class SymmetryBrush implements Brush {
    public static final int MAX_BRUSHES = 4;

    private final Brush[] brushes = new Brush[MAX_BRUSHES];
    private int radius = AbstractBrushTool.DEFAULT_BRUSH_RADIUS;
    //    private Composition comp;
    private int numInstantiatedBrushes;
    private Supplier<Brush> brushSupplier;
    private Symmetry symmetry;
    private BrushAffectedArea affectedArea;

    public SymmetryBrush(Supplier<Brush> brushSupplier, Symmetry symmetry, BrushAffectedArea affectedArea) {
        this.brushSupplier = brushSupplier;
        this.symmetry = symmetry;
        this.affectedArea = affectedArea;
        numInstantiatedBrushes = symmetry.getNumBrushes();
        assert numInstantiatedBrushes <= MAX_BRUSHES;
        brushTypeChanged(brushSupplier);
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

    @Override
    public void onDragStart(int x, int y) {
        symmetry.onDragStart(this, x, y);
    }

    @Override
    public void onNewMousePoint(int x, int y) {
        symmetry.onNewMousePoint(this, x, y);
    }

    public void brushTypeChanged(Supplier<Brush> brushSupplier) {
        this.brushSupplier = brushSupplier;
        for(int i = 0; i < numInstantiatedBrushes; i++) {
            brushes[i] = brushSupplier.get();
        }
    }

    public void symmetryChanged(Symmetry symmetry) {
        this.symmetry = symmetry;
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
        affectedArea.updateAffectedCoordinates(x, y);
        brushes[brushNo].onDragStart(x, y);
    }

    public void onNewMousePoint(int brushNo, int endX, int endY) {
        affectedArea.updateAffectedCoordinates(endX, endY);

        if(radius <= 0) {
            throw new IllegalStateException("radius is " + radius);
        }

        brushes[brushNo].onNewMousePoint(endX, endY);
//        comp.updateRegion(startX, startY, endX, endY, 2 * radius);
    }
}
