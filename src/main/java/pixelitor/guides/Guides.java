/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.CanvasMargins;
import pixelitor.Composition;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.gui.View;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.history.History;
import pixelitor.utils.VisibleForTesting;

import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a set of guides.
 * Objects of this class should be mutated only while building,
 * for any changes a new instance should be built.
 */
public class Guides implements Serializable {
    // for compatibility with Pixelitor 4.2.0
    private static final long serialVersionUID = -1168950961227421664L;

    private final Composition comp;
    // all guides are stored as percentages so that resizing
    // does not affect their perceived position
    private final List<Double> horizontals = new ArrayList<>();
    private final List<Double> verticals = new ArrayList<>();

    private List<Line2D> lines;

    private String name;

    @VisibleForTesting
    Guides(Composition comp) {
        this.comp = comp;
    }

    public Guides copyForEnlargedCanvas(int north, int east, int south, int west) {
        Guides copy = new Guides(comp);
        copy.setName(String.format("enlarged : north = %d, east = %d, south = %d, west = %d%n",
                north, east, south, west));
        int oldWidth = comp.getCanvasImWidth();
        if (west != 0 || east != 0) {
            int newWidth = oldWidth + east + west;
            for (Double h : verticals) {
                double oldAbs = h * oldWidth;
                double adjustedRel = (oldAbs + west) / newWidth;
                copy.verticals.add(adjustedRel);
            }
        } else {
            copy.verticals.addAll(verticals);
        }

        int oldHeight = comp.getCanvasImHeight();
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

        copy.regenerateLines();
        return copy;
    }

    public Guides copyForCrop(Rectangle2D cropRect) {
        Canvas canvas = comp.getCanvas();
        int northMargin = (int) cropRect.getY();
        int westMargin = (int) cropRect.getX();
        int southMargin = (int) (canvas.getImHeight()
                - cropRect.getHeight() - cropRect.getY());
        int eastMargin = (int) (canvas.getImWidth()
                - cropRect.getWidth() - cropRect.getX());

        // a crop is a negative enlargement
        return copyForEnlargedCanvas(
                -northMargin, -eastMargin,
                -southMargin, -westMargin);
    }

    public static void showAddGridDialog(Composition comp) {
        Builder builder = new Builder(comp, true);
        AddGridGuidesPanel panel = new AddGridGuidesPanel(builder);
        new DialogBuilder()
                .title("Add Grid Guides")
                .content(panel)
                .okAction(() -> panel.createGuides(false))
                .cancelAction(builder::resetOldGuides)
                .show();
    }

    public static void showAddSingleGuideDialog(Composition comp,
                                                boolean horizontal) {
        Builder builder = new Builder(comp, false);
        AddSingleGuidePanel panel = new AddSingleGuidePanel(builder, horizontal);
        String dialogTitle = horizontal ? "Add Horizontal Guide" : "Add Vertical Guide";
        new DialogBuilder()
                .title(dialogTitle)
                .content(panel)
                .okAction(() -> panel.createGuides(false))
                .cancelAction(builder::resetOldGuides)
                .show();
    }

    public void addHorRelative(double percent) {
        horizontals.add(percent);
    }

    public void addHorAbsolute(int pixels) {
        double percent = pixels / (double) comp.getCanvasImWidth();
        horizontals.add(percent);
    }

    public void addVerRelative(double percent) {
        verticals.add(percent);
    }

    public void addVerAbsolute(int pixels) {
        double percent = pixels / (double) comp.getCanvasImHeight();
        verticals.add(percent);
    }

    public void addRelativeGrid(int numHorDivisions, int numVerDivisions) {
        // horizontal lines
        double divisionHeight = 1.0 / (double) numHorDivisions;
        for (int i = 1; i < numHorDivisions; i++) {
            double lineY = i * divisionHeight;
            horizontals.add(lineY);
        }

        // vertical lines
        double divisionWidth = 1.0 / (double) numVerDivisions;
        for (int i = 1; i < numVerDivisions; i++) {
            double lineX = i * divisionWidth;
            verticals.add(lineX);
        }
    }

    public void addAbsoluteGrid(int horNumber, // number of horizontal lines
                                int horDist,   // vertical abs. distance between them
                                int verNumber,
                                int verDist) {
        // horizontal lines
        int distFromTop = 0;
        int canvasHeight = comp.getCanvasImHeight();
        for (int i = 0; i < horNumber; i++) {
            distFromTop += horDist;
            double lineY = distFromTop / (double) canvasHeight;
            horizontals.add(lineY);
        }

        // vertical lines
        int distFromLeft = 0;
        int canvasWidth = comp.getCanvasImWidth();
        for (int i = 0; i < verNumber; i++) {
            distFromLeft += verDist;
            double lineX = distFromLeft / (double) canvasWidth;
            verticals.add(lineX);
        }
    }

    @VisibleForTesting
    public List<Double> getHorizontals() {
        return horizontals;
    }

    @VisibleForTesting
    public List<Double> getVerticals() {
        return verticals;
    }

    private void regenerateLines() {
        int width = comp.getCanvasImWidth();
        int height = comp.getCanvasImHeight();
        CanvasMargins margins = comp.getView().getCanvasMargins();
        View view = comp.getView();

        lines = new ArrayList<>();
        for (Double h : horizontals) {
            double y = h * height;

            // the generated lines have to be in component space
            double coStartX = view.imageXToComponentSpace(0) - margins.getLeft();
            double coStartY = view.imageYToComponentSpace(y);
            double coEndX = view.imageXToComponentSpace(width) + margins.getRight();
            double coEndY = coStartY;

            lines.add(new Line2D.Double(coStartX, coStartY, coEndX, coEndY));
        }
        for (Double v : verticals) {
            double x = v * width;

            double coStartX = view.imageXToComponentSpace(x);
            double coStartY = view.imageYToComponentSpace(0) - margins.getTop();
            double coEndX = coStartX;
            double coEndY = view.imageYToComponentSpace(height) + margins.getBottom();

            lines.add(new Line2D.Double(coStartX, coStartY, coEndX, coEndY));
        }
    }

    public void draw(Graphics2D g) {
        GuidesRenderer renderer = GuidesRenderer.GUIDES_INSTANCE.get();
        renderer.draw(g, lines);
    }

    public void coCoordsChanged() {
        regenerateLines();
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

    /**
     * Code that is common for building both single-line and grid guides.
     * Designed primarily for dialog sessions with previews.
     */
    public static class Builder {
        private final BooleanParam clearExisting;
        private final Composition comp;
        private final Guides oldGuides;

        public Builder(Composition comp, boolean clearByDefault) {
            this.comp = comp;
            oldGuides = comp.getGuides();
            clearExisting = new BooleanParam("Clear Existing Guides", clearByDefault);
        }

        public void build(boolean preview, Consumer<Guides> setup) {
            Guides guides = new Guides(comp);
            boolean useExisting = !clearExisting.isChecked();
            if (useExisting && oldGuides != null) {
                guides.copyValuesFrom(oldGuides);
            }

            setup.accept(guides);

            guides.regenerateLines();
            comp.setGuides(guides);
            comp.repaint();
            if (!preview) {
                History.addEdit(new GuidesChangeEdit(comp, oldGuides, guides));
            }
        }

        public void resetOldGuides() {
            comp.setGuides(oldGuides);
            comp.repaint();
        }

        public BooleanParam getClearExisting() {
            return clearExisting;
        }

        public void setAdjustmentListener(ParamAdjustmentListener updatePreview) {
            clearExisting.setAdjustmentListener(updatePreview);
        }

        public Canvas getCanvas() {
            return comp.getCanvas();
        }
    }
}
