/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.tools.AbstractBrushTool;
import pixelitor.tools.BrushType;
import pixelitor.tools.Symmetry;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.debug.DebugNode;

import java.awt.Graphics2D;
import java.util.function.BiConsumer;

/**
 * A brush implementing symmetry by delegating to multiple
 * internal brushes based on the current symmetry settings.
 */
public class SymmetryBrush implements Brush {
    // maximum number of simultaneous brushes
    public static final int MAX_BRUSHES = 4;

    // only the first numBrushes entries are non-null
    private final Brush[] brushes = new Brush[MAX_BRUSHES];

    // current number of active brushes based on symmetry setting
    private int numBrushes;

    private final AbstractBrushTool tool;
    private BrushType brushType;
    private Symmetry symmetry;

    // the affected area, tracked here for all internal brushes together
    private final AffectedArea affectedArea;

    public SymmetryBrush(AbstractBrushTool tool, BrushType brushType,
                         Symmetry symmetry, double radius, AffectedArea affectedArea) {
        this.tool = tool;
        this.brushType = brushType;
        this.symmetry = symmetry;
        this.affectedArea = affectedArea;
        numBrushes = symmetry.getNumBrushes();
        assert numBrushes <= MAX_BRUSHES;

        // initialize the internal brushes
        updateBrushType(brushType, radius);
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
    public PPoint getPrevPos() {
        // used by the lazy mouse brush, when the user's first action
        // after enabling lazy mouse is a Shift-click line connect
        return brushes[0].getPrevPos();
    }

    @Override
    public void setPrevPos(PPoint previous) {
        // the internal brushes manage their own previous points
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasPrevPos() {
        // it's the same for all brushes
        return brushes[0].hasPrevPos();
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
            brushes[i].initDrawing(symmetry.transform(p, i));
        }
    }

    @Override
    public void startStrokeAt(PPoint p) {
        forEachBrush(p, Brush::startStrokeAt);
    }

    @Override
    public void continueTo(PPoint p) {
        forEachBrush(p, Brush::continueTo);
    }

    @Override
    public void connectWithLineTo(PPoint p) {
        forEachBrush(p, Brush::connectWithLineTo);
    }

    @Override
    public void finishBrushStroke() {
        for (int i = 0; i < numBrushes; i++) {
            brushes[i].finishBrushStroke();
        }
    }

    private void forEachBrush(PPoint p, BiConsumer<Brush, PPoint> action) {
        for (int i = 0; i < numBrushes; i++) {
            PPoint q = symmetry.transform(p, i);
            affectedArea.add(q);
            action.accept(brushes[i], q);
        }
    }

    public void updateBrushType(BrushType newBrushType, double radius) {
        this.brushType = newBrushType;
        for (int i = 0; i < numBrushes; i++) {
            if (brushes[i] != null) {
                brushes[i].dispose();
            }
            brushes[i] = newBrushType.createBrush(tool, radius);
        }
    }

    public void updateSymmetry(Symmetry newSymmetry, double radius) {
        this.symmetry = newSymmetry;

        int newNumBrushes = newSymmetry.getNumBrushes();
        assert newNumBrushes <= MAX_BRUSHES;

        // dispose of surplus brushes if the new symmetry mode requires fewer
        for (int i = newNumBrushes; i < numBrushes; i++) {
            brushes[i].dispose();
            brushes[i] = null;
        }

        // create additional brushes if the new symmetry mode requires more
        for (int i = numBrushes; i < newNumBrushes; i++) {
            brushes[i] = brushType.createBrush(tool, radius);
        }
        numBrushes = newNumBrushes;

        // The mirrored positions depend on the symmetry, so they must be
        // re-derived for the retained brushes too, not only for the new ones.
        // If the primary brush was already used, propagate its last known
        // position, applying the new symmetry transformation.
        PPoint previous0 = brushes[0].getPrevPos();
        if (previous0 != null) {
            for (int i = 1; i < numBrushes; i++) {
                brushes[i].setPrevPos(newSymmetry.transform(previous0, i));
            }
        }
    }

    @Override
    public void dispose() {
        for (int i = 0; i < numBrushes; i++) {
            brushes[i].dispose();
            brushes[i] = null;
        }
        numBrushes = 0;
    }

    @Override
    public DebugNode createDebugNode(String key) {
        var node = new DebugNode("symmetry brush", this);

        node.addInt("num brushes", numBrushes);
        node.addAsString("type", brushType);
        node.addAsString("symmetry", symmetry);

        for (int i = 0; i < numBrushes; i++) {
            node.add(brushes[i].createDebugNode("brush " + i));
        }
        node.add(affectedArea.createDebugNode("affected area"));
        return node;
    }
}
