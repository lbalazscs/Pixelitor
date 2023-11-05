/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.guides;

import pixelitor.Canvas;
import pixelitor.compactions.Flip;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.gui.View;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.history.History;
import pixelitor.utils.QuadrantAngle;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.Debuggable;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static java.lang.String.format;
import static pixelitor.compactions.Flip.Direction.HORIZONTAL;
import static pixelitor.compactions.Flip.Direction.VERTICAL;

/**
 * Represents a set of guides.
 * Objects of this class should be mutated only while building,
 * for any changes a new instance should be built.
 */
public class Guides implements Serializable, Debuggable {
    @Serial
    private static final long serialVersionUID = -1168950961227421664L;

    // all guide positions are stored as ratios relative to the canvas
    // so that resizing does not affect their image-space position
    private final List<Double> horizontals = new ArrayList<>();
    private final List<Double> verticals = new ArrayList<>();

    // the rendered lines in component space
    private transient List<Line2D> lines;

    // used only for debugging
    private String name;

    public Guides copyForNewComp(View view) {
        Guides copy = createCopy();

        copy.horizontals.addAll(horizontals);
        copy.verticals.addAll(verticals);

        copy.regenerateLines(view);

        return copy;
    }

    public Guides copyForFlip(Flip.Direction direction, View view) {
        Guides copy = createCopy();

        if (direction == HORIZONTAL) {
            copy.horizontals.addAll(horizontals);
            for (Double vertical : verticals) {
                copy.verticals.add(1 - vertical);
            }
        } else if (direction == VERTICAL) {
            copy.verticals.addAll(verticals);
            for (Double horizontal : horizontals) {
                copy.horizontals.add(1 - horizontal);
            }
        } else {
            throw new IllegalStateException();
        }

        copy.regenerateLines(view);

        return copy;
    }

    public Guides copyForRotate(QuadrantAngle angle, View view) {
        Guides copy = createCopy();

        switch (angle) {
            case ANGLE_90 -> copyLinesRotating90(copy);
            case ANGLE_180 -> copyLinesRotating180(copy);
            case ANGLE_270 -> copyLinesRotating270(copy);
        }

        copy.regenerateLines(view);

        return copy;
    }

    private void copyLinesRotating90(Guides copy) {
        for (Double horizontal : horizontals) {
            copy.verticals.add(1 - horizontal);
        }
        copy.horizontals.addAll(verticals);
    }

    private void copyLinesRotating180(Guides copy) {
        for (Double horizontal : horizontals) {
            copy.horizontals.add(1 - horizontal);
        }
        for (Double vertical : verticals) {
            copy.verticals.add(1 - vertical);
        }
    }

    private void copyLinesRotating270(Guides copy) {
        copy.verticals.addAll(horizontals);
        for (Double vertical : verticals) {
            copy.horizontals.add(1 - vertical);
        }
    }

    private Guides createCopy() {
        Guides copy = new Guides();
        copy.setName(name + " copy");
        return copy;
    }

    public Guides copyForEnlargedCanvas(int north, int east, int south, int west, View view, Canvas oldCanvas) {
        Guides copy = new Guides();
        copy.setName(format("enlarged : north = %d, east = %d, south = %d, west = %d%n",
            north, east, south, west));

        copyVerticals(copy, east, west, oldCanvas);
        copyHorizontals(copy, north, south, oldCanvas);

        copy.regenerateLines(view);
        return copy;
    }

    private void copyVerticals(Guides copy, int east, int west, Canvas oldCanvas) {
        int oldWidth = oldCanvas.getWidth();
        if (west != 0 || east != 0) {
            int newWidth = oldWidth + east + west;
            for (Double h : verticals) {
                double oldAbs = h * oldWidth;
                double adjustedRatio = (oldAbs + west) / newWidth;
                copy.verticals.add(adjustedRatio);
            }
        } else {
            copy.verticals.addAll(verticals);
        }
    }

    private void copyHorizontals(Guides copy, int north, int south, Canvas oldCanvas) {
        int oldHeight = oldCanvas.getHeight();
        if (north != 0 || south != 0) {
            int newHeight = oldHeight + north + south;
            for (Double v : horizontals) {
                double oldAbs = v * oldHeight;
                double adjustedRel = (oldAbs + north) / newHeight;
                copy.horizontals.add(adjustedRel);
            }
        } else {
            copy.horizontals.addAll(horizontals);
        }
    }

    public Guides copyForCrop(Rectangle cropRect, View view) {
        Canvas oldCanvas = view.getCanvas();
        int northMargin = cropRect.y;
        int westMargin = cropRect.x;
        int southMargin = oldCanvas.getHeight() - cropRect.height - cropRect.y;
        int eastMargin = oldCanvas.getWidth() - cropRect.width - cropRect.x;

        // a crop is a negative enlargement
        return copyForEnlargedCanvas(
            -northMargin, -eastMargin, -southMargin, -westMargin, view, oldCanvas);
    }

    public static void showAddGridDialog(View view) {
        Builder builder = new Builder(view, true);
        AddGridGuidesPanel panel = new AddGridGuidesPanel(builder);
        new DialogBuilder()
            .title("Add Grid Guides")
            .content(panel)
            .withScrollbars()
            .okAction(() -> panel.createGuides(false))
            .cancelAction(builder::resetOldGuides)
            .show();
    }

    public static void showAddSingleGuideDialog(View view,
                                                boolean horizontal) {
        Builder builder = new Builder(view, false);
        AddSingleGuidePanel panel = new AddSingleGuidePanel(builder, horizontal);
        String dialogTitle = horizontal ? "Add Horizontal Guide" : "Add Vertical Guide";
        new DialogBuilder()
            .title(dialogTitle)
            .content(panel)
            .withScrollbars()
            .okAction(() -> panel.createGuides(false))
            .cancelAction(builder::resetOldGuides)
            .show();
    }

    public void addHorRelative(double percent) {
        horizontals.add(percent);
    }

    public void addHorAbsolute(int pixels, Canvas canvas) {
        double percent = pixels / (double) canvas.getWidth();
        horizontals.add(percent);
    }

    public void addVerRelative(double percent) {
        verticals.add(percent);
    }

    public void addVerAbsolute(int pixels, Canvas canvas) {
        double percent = pixels / (double) canvas.getHeight();
        verticals.add(percent);
    }

    public void addRelativeGrid(int numHorDivisions, int numVerDivisions) {
        // horizontal lines
        double divisionHeight = 1.0 / numHorDivisions;
        for (int i = 1; i < numHorDivisions; i++) {
            double lineY = i * divisionHeight;
            horizontals.add(lineY);
        }

        // vertical lines
        double divisionWidth = 1.0 / numVerDivisions;
        for (int i = 1; i < numVerDivisions; i++) {
            double lineX = i * divisionWidth;
            verticals.add(lineX);
        }
    }

    public List<Double> getHorizontals() {
        return horizontals;
    }

    public List<Double> getVerticals() {
        return verticals;
    }

    private void regenerateLines(View view) {
        Canvas canvas = view.getCanvas();
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        int canvasMarginX = view.getCanvasStartX();
        int canvasMarginY = view.getCanvasStartY();

        lines = new ArrayList<>();

        for (Double h : horizontals) {
            double y = h * canvasHeight;

            // the generated lines have to be in component space
            double coY = view.imageYToComponentSpace(y);
            double coStartX = view.imageXToComponentSpace(0) - canvasMarginX;
            double coEndX = view.imageXToComponentSpace(canvasWidth) + canvasMarginX;

            lines.add(new Line2D.Double(coStartX, coY, coEndX, coY));
        }

        for (Double v : verticals) {
            double x = v * canvasWidth;

            double coX = view.imageXToComponentSpace(x);
            double coStartY = view.imageYToComponentSpace(0) - canvasMarginY;
            double coEndY = view.imageYToComponentSpace(canvasHeight) + canvasMarginY;

            lines.add(new Line2D.Double(coX, coStartY, coX, coEndY));
        }
    }

    public void draw(Graphics2D g) {
        GuidesRenderer renderer = GuidesRenderer.GUIDES_INSTANCE.get();
        renderer.draw(g, lines);
    }

    public void coCoordsChanged(View view) {
        regenerateLines(view);
    }

    private void copyValuesFrom(Guides other) {
        assert other != null;
        horizontals.addAll(other.horizontals);
        verticals.addAll(other.verticals);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public DebugNode createDebugNode(String key) {
        var node = new DebugNode(key, this);

        for (Double h : horizontals) {
            node.addDouble("horizontal", h);
        }
        for (Double v : verticals) {
            node.addDouble("vertical", v);
        }

        return node;
    }

    /**
     * Code that is common for building both single-line and grid guides.
     * Designed primarily for dialog sessions with previews.
     */
    public static class Builder {
        private final BooleanParam clearExisting;
        private final View view;
        private final Guides oldGuides;

        public Builder(View view, boolean clearByDefault) {
            this.view = view;
            oldGuides = view.getComp().getGuides();
            clearExisting = new BooleanParam("Clear Existing Guides", clearByDefault);
        }

        public void build(boolean preview, Consumer<Guides> setup) {
            Guides guides = new Guides();
            boolean useExisting = !clearExisting.isChecked();
            if (useExisting && oldGuides != null) {
                guides.copyValuesFrom(oldGuides);
            }

            setup.accept(guides);

            guides.regenerateLines(view);
            var comp = view.getComp();
            comp.setGuides(guides);
            comp.repaint();
            if (!preview) {
                History.add(new GuidesChangeEdit(comp, oldGuides, guides));
            }
        }

        public void resetOldGuides() {
            var comp = view.getComp();
            comp.setGuides(oldGuides);
            view.repaint();
        }

        public BooleanParam getClearExisting() {
            return clearExisting;
        }

        public void setAdjustmentListener(ParamAdjustmentListener updatePreview) {
            clearExisting.setAdjustmentListener(updatePreview);
        }

        public Canvas getCanvas() {
            return view.getCanvas();
        }
    }
}
