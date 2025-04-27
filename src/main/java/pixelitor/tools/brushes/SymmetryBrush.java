/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.layers.Drawable;
import pixelitor.tools.BrushType;
import pixelitor.tools.Symmetry;
import pixelitor.tools.Tool;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.debug.DebugNode;

import java.awt.Graphics2D;

/**
 * A brush implementing symmetry by delegating to multiple
 * internal brushes based on the current symmetry settings.
 */
public class SymmetryBrush implements Brush {
    // maximum number of simultaneous brushes
    private static final int MAX_BRUSHES = 4;

    private final Brush[] brushes = new Brush[MAX_BRUSHES];

    // current number of active brushes based on symmetry setting
    private int numBrushes;

    private final Tool tool;
    private BrushType brushType;
    private Symmetry symmetry;

    // the affected area is shared between all the internal brushes
    private final AffectedArea affectedArea;

    public SymmetryBrush(Tool tool, BrushType brushType,
                         Symmetry symmetry, double radius) {
        this.tool = tool;
        this.brushType = brushType;
        this.symmetry = symmetry;
        affectedArea = new AffectedArea();
        numBrushes = symmetry.getNumBrushes();
        assert numBrushes <= MAX_BRUSHES;

        // initialize the internal brushes
        brushTypeChanged(brushType, radius);
    }

    public AffectedArea getAffectedArea() {
        return affectedArea;
    }

    @Override
    public void setTarget(Drawable dr, Graphics2D g) {
        for (int i = 0; i < numBrushes; i++) {
            brushes[i].setTarget(dr, g);
        }
    }

    @Override
    public void setRadius(double radius) {
        for (int i = 0; i < numBrushes; i++) {
            brushes[i].setRadius(radius);
        }
    }

    @Override
    public PPoint getPrevious() {
        // the sub-brushes manage their own previous points
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPrevious(PPoint previous) {
        // the sub-brushes manage their own previous points
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasPrevious() {
        // it's the same for all brushes
        return brushes[0].hasPrevious();
    }

    @Override
    public double getMaxEffectiveRadius() {
        // all brushes have the same max effective radius
        return brushes[0].getMaxEffectiveRadius();
    }

    @Override
    public boolean isDrawing() {
        // the drawing state is consistent across brushes
        return brushes[0].isDrawing();
    }

    @Override
    public double getPreferredSpacing() {
        // all internal brushes have the same preferred spacing
        return brushes[0].getPreferredSpacing();
    }

    @Override
    public void initDrawing(PPoint p) {
        for (int i = 0; i < numBrushes; i++) {
            PPoint transformed = (i == 0) ? p : symmetry.transform(p, i);
            brushes[i].initDrawing(transformed);
        }
    }

    // Delegate user actions to the Symmetry enum, which handles applying the
    // action to the correct internal brushes with transformed coordinates.

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
        symmetry.finishBrushStroke(this);
    }

    public void brushTypeChanged(BrushType newBrushType, double radius) {
        this.brushType = newBrushType;
        for (int i = 0; i < numBrushes; i++) {
            if (brushes[i] != null) {
                brushes[i].dispose();
            }
            brushes[i] = newBrushType.createBrush(tool, radius);
        }
    }

    public void symmetryChanged(Symmetry symmetry, double radius) {
        this.symmetry = symmetry;

        int newNumBrushes = symmetry.getNumBrushes();
        assert newNumBrushes <= MAX_BRUSHES;

        if (newNumBrushes > numBrushes) {
            // create additional brushes if the new symmetry mode requires more
            PPoint previous0 = brushes[0].getPrevious();
            for (int i = numBrushes; i < newNumBrushes; i++) {
                brushes[i] = brushType.createBrush(tool, radius);

                // if the primary brush was already used, propagate its
                // last known position to the newly created brushes,
                // applying the new symmetry transformation
                if (previous0 != null) {
                    PPoint generatedPrevious = symmetry.transform(previous0, i);
                    brushes[i].setPrevious(generatedPrevious);
                }
            }
        } else if (newNumBrushes < numBrushes) {
            // dispose of surplus brushes if the new symmetry mode requires fewer
            for (int i = newNumBrushes; i < numBrushes; i++) {
                brushes[i].dispose();
                brushes[i] = null;
            }
        }
        // else numBrushes == newNumBrushes, no structural change needed

        numBrushes = newNumBrushes;
    }

    @Override
    public void dispose() {
        for (int i = 0; i < numBrushes; i++) {
            brushes[i].dispose();
            brushes[i] = null;
        }
        numBrushes = 0;
    }

    // Methods called by the Symmetry enum to perform actions on specific internal brushes.

    /**
     * Starts a brush stroke for a specific internal brush.
     */
    public void startAt(int brushNo, PPoint p) {
        // the tracking of the shared affected area is done at this level
        if (brushNo == 0) {
            affectedArea.initAt(p);
        } else {
            affectedArea.updateWith(p);
        }

        // do the actual drawing
        brushes[brushNo].startAt(p);
    }

    /**
     * Continues a brush stroke for a specific internal brush.
     */
    public void continueTo(int brushNo, PPoint p) {
        affectedArea.updateWith(p);
        brushes[brushNo].continueTo(p);
    }

    /**
     * Connects the last point with a line for a specific internal brush.
     */
    public void lineConnectTo(int brushNo, PPoint p) {
        affectedArea.updateWith(p);
        brushes[brushNo].lineConnectTo(p);
    }

    /**
     * Finishes the brush stroke for a specific internal brush.
     */
    public void finishBrushStroke(int brushNo) {
        brushes[brushNo].finishBrushStroke();
    }

    @Override
    public DebugNode createDebugNode(String key) {
        var node = new DebugNode("symmetry brush", this);

        node.addInt("numBrushes", numBrushes);
        node.addAsString("type", brushType);
        node.addAsString("symmetry", symmetry);

        for (int i = 0; i < numBrushes; i++) {
            node.add(brushes[i].createDebugNode("brush " + i));
        }
        node.add(affectedArea.createDebugNode("affected area"));
        return node;
    }
}
