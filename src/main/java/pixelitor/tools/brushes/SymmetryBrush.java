/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.tools.util.PPoint;
import pixelitor.utils.debug.DebugNode;

import java.awt.Graphics2D;

/**
 * Delegates the work to other brushes according to
 * the symmetry and brush type settings
 */
public class SymmetryBrush implements Brush {
    private static final int MAX_BRUSHES = 4;

    private final Brush[] brushes = new Brush[MAX_BRUSHES];
    private int numBrushes;
    private final Tool tool;
    private BrushType brushType;
    private Symmetry symmetry;
    private final AffectedArea affectedArea;

    public SymmetryBrush(Tool tool, BrushType brushType,
                         Symmetry symmetry, double radius) {
        this.tool = tool;
        this.brushType = brushType;
        this.symmetry = symmetry;
        affectedArea = new AffectedArea();
        numBrushes = symmetry.getNumBrushes();
        assert numBrushes <= MAX_BRUSHES;
        brushTypeChanged(brushType, radius);
    }

    public AffectedArea getAffectedArea() {
        return affectedArea;
    }

    @Override
    public void setTarget(Composition comp, Graphics2D g) {
        for (int i = 0; i < numBrushes; i++) {
            brushes[i].setTarget(comp, g);
        }
    }

    @Override
    public void setRadius(double radius) {
        for (int i = 0; i < numBrushes; i++) {
            brushes[i].setRadius(radius);
        }
    }

    @Override
    public void setPrevious(PPoint previous) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public double getEffectiveRadius() {
        return brushes[0].getEffectiveRadius();
    }

    @Override
    public PPoint getPrevious() {
        return brushes[0].getPrevious();
    }

    @Override
    public boolean isDrawing() {
        return brushes[0].isDrawing();
    }

    @Override
    public void initDrawing(PPoint p) {
        for (int i = 0; i < numBrushes; i++) {
            PPoint transformed;
            if (i == 0) {
                transformed = p;
            } else {
                transformed = symmetry.transform(p, i);
            }
            brushes[i].initDrawing(transformed);
        }
    }

    @Override
    public void startAt(PPoint p) {
        symmetry.startAt(this, p);
    }

    @Override
    public void continueTo(PPoint p) {
        symmetry.continueTo(this, p);
    }

    @Override
    public void lineConnectTo(PPoint p) {
        symmetry.lineConnectTo(this, p);
    }

    @Override
    public void finishBrushStroke() {
        symmetry.finish(this);
    }

    public void brushTypeChanged(BrushType brushType, double radius) {
        this.brushType = brushType;
        for (int i = 0; i < numBrushes; i++) {
            if(brushes[i] != null) {
                brushes[i].dispose();
            }
            brushes[i] = brushType.createBrush(tool, radius);
        }
        assert allBrushesAreDifferentInstances();
    }

    public void symmetryChanged(Symmetry symmetry, double radius) {
        this.symmetry = symmetry;

        int newNumBrushes = symmetry.getNumBrushes();
        assert newNumBrushes <= MAX_BRUSHES;

        if (newNumBrushes > numBrushes) {
            // we need to create more brushes of the same type
            PPoint previous0 = brushes[0].getPrevious();
            for (int i = numBrushes; i < newNumBrushes; i++) {
                brushes[i] = brushType.createBrush(tool, radius);

                // if the first brush has a previous point at the time of the
                // symmetry change (it was already used), then propagate
                // the previous coordinates to the newly created brushes
                if (previous0 != null) {
                    PPoint generatedPrevious = symmetry.transform(previous0, i);
                    brushes[i].setPrevious(generatedPrevious);
                }
            }
        } else if (newNumBrushes < numBrushes) {
            for (int i = newNumBrushes; i < numBrushes; i++) {
                brushes[i].dispose();
                brushes[i] = null;
            }
        }
        numBrushes = newNumBrushes;
        assert allBrushesAreDifferentInstances();
    }

    // used in assertions
    private boolean allBrushesAreDifferentInstances() {
        for (int i = 0; i < numBrushes; i++) {
            for (int j = 0; j < numBrushes; j++) {
                if (i != j && brushes[i] == brushes[j]) {
                    return false;
                }
            }
        }
        return true;
    }

    public void startAt(int brushNo, PPoint p) {
        // the tracking of the affected area is done at this level
        if(brushNo == 0) {
            affectedArea.initAt(p);
        } else {
            affectedArea.updateWith(p);
        }

        // do the actual painting
        brushes[brushNo].startAt(p);
    }

    public void continueTo(int brushNo, PPoint p) {
        affectedArea.updateWith(p);
        brushes[brushNo].continueTo(p);
    }

    public void lineConnectTo(int brushNo, PPoint p) {
        affectedArea.updateWith(p);
        brushes[brushNo].lineConnectTo(p);
    }

    public void finish(int brushNo) {
        brushes[brushNo].finishBrushStroke();
    }

    @Override
    public DebugNode getDebugNode() {
        var node = new DebugNode("symmetry brush", this);

        for (int i = 0; i < numBrushes; i++) {
            node.add(brushes[i].getDebugNode());
        }

        node.addString("type", brushType.toString());
        node.addString("symmetry", symmetry.toString());

        node.add(affectedArea.getDebugNode());

        return node;
    }

    @Override
    public double getPreferredSpacing() {
        return brushes[0].getPreferredSpacing();
    }
}
