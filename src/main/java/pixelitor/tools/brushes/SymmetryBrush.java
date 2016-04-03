/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.tools.BrushType;
import pixelitor.tools.Symmetry;
import pixelitor.tools.Tool;
import pixelitor.utils.debug.DebugNode;

import java.awt.Graphics2D;

/**
 * Delegates the work to other brushes according to the symmetry and brush type settings
 */
public class SymmetryBrush implements Brush {
    private static final int MAX_BRUSHES = 4;

    private final Brush[] brushes = new Brush[MAX_BRUSHES];
    private int numInstantiatedBrushes;
    private final Tool tool;
    private BrushType brushType;
    private Symmetry symmetry;
    private final BrushAffectedArea affectedArea;

    public SymmetryBrush(Tool tool, BrushType brushType, Symmetry symmetry, int radius) {
        this.tool = tool;
        this.brushType = brushType;
        this.symmetry = symmetry;
        this.affectedArea = new BrushAffectedArea();
        numInstantiatedBrushes = symmetry.getNumBrushes();
        assert numInstantiatedBrushes <= MAX_BRUSHES;
        brushTypeChanged(brushType, radius);
    }

    public BrushAffectedArea getAffectedArea() {
        return affectedArea;
    }

    @Override
    public void setTarget(Composition comp, Graphics2D g) {
        for(int i = 0; i < numInstantiatedBrushes; i++) {
            brushes[i].setTarget(comp, g);
        }
    }

    @Override
    public void setRadius(int radius) {
        for(int i = 0; i < numInstantiatedBrushes; i++) {
            assert brushes[i] != null : "i = " + i + ", numInstantiatedBrushes = " + numInstantiatedBrushes;
            brushes[i].setRadius(radius);
        }
    }

    @Override
    public void onDragStart(double x, double y) {
        symmetry.onDragStart(this, x, y);
    }

    @Override
    public void onNewMousePoint(double x, double y) {
        symmetry.onNewMousePoint(this, x, y);
    }

    public void brushTypeChanged(BrushType brushType, int radius) {
        this.brushType = brushType;
        for(int i = 0; i < numInstantiatedBrushes; i++) {
            if(brushes[i] != null) {
                brushes[i].dispose();
            }
            brushes[i] = brushType.createBrush(tool, radius);
        }
        assert checkThatAllBrushesAreDifferentInstances();
    }

    public void symmetryChanged(Symmetry symmetry, int radius) {
        this.symmetry = symmetry;
        if(symmetry.getNumBrushes() > numInstantiatedBrushes) {
            // we need to create more brushes of the same type
            int newNumBrushes = symmetry.getNumBrushes();
            assert newNumBrushes <= MAX_BRUSHES;
            for(int i = numInstantiatedBrushes; i < newNumBrushes; i++) {
                brushes[i] = brushType.createBrush(tool, radius);
            }
            numInstantiatedBrushes = newNumBrushes;
        }
        assert checkThatAllBrushesAreDifferentInstances();
    }

    private boolean checkThatAllBrushesAreDifferentInstances() {
        for (int i = 0; i < numInstantiatedBrushes; i++) {
            for (int j = 0; j < numInstantiatedBrushes; j++) {
                if(i != j) {
                    if(brushes[i] == brushes[j]) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void onDragStart(int brushNo, double x, double y) {
        affectedArea.updateAffectedCoordinates(x, y);
        brushes[brushNo].onDragStart(x, y);
    }

    public void onNewMousePoint(int brushNo, double endX, double endY) {
        affectedArea.updateAffectedCoordinates(endX, endY);
        brushes[brushNo].onNewMousePoint(endX, endY);
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = new DebugNode("Symmetry Brush", this);

        for (int i = 0; i < numInstantiatedBrushes; i++) {
            node.add(brushes[i].getDebugNode());
        }

        node.addStringChild("Brush Type", brushType.toString());
        node.addStringChild("Symmetry", symmetry.toString());

        node.add(affectedArea.getDebugNode());

        return node;
    }
}
