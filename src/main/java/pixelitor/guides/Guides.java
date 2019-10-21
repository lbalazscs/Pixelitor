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
import pixelitor.filters.comp.Flip;
import pixelitor.filters.comp.Rotate;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.gui.View;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.history.History;
import pixelitor.utils.VisibleForTesting;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static pixelitor.filters.comp.Flip.Direction.HORIZONTAL;
import static pixelitor.filters.comp.Flip.Direction.VERTICAL;
import static pixelitor.filters.comp.Rotate.SpecialAngle.ANGLE_180;
import static pixelitor.filters.comp.Rotate.SpecialAngle.ANGLE_270;
import static pixelitor.filters.comp.Rotate.SpecialAngle.ANGLE_90;

/**
 * Represents a set of guides.
 * Objects of this class should be mutated only while building,
 * for any changes a new instance should be built.
 */
public class Guides implements Serializable {
    // for compatibility with Pixelitor 4.2.0
    private static final long serialVersionUID = -1168950961227421664L;

    // all guide positions are stored as ratios relative to the canvas
    // so that resizing does not affect their perceived position
    private final List<Double> horizontals = new ArrayList<>();
    private final List<Double> verticals = new ArrayList<>();

    private List<Line2D> lines;

    // used only for debugging
    private String name;

    @VisibleForTesting
    Guides() {
    }

    public Guides copyForNewComp(View view) {
        Guides copy = new Guides();
        copy.name = name + " copy";

        copy.horizontals.addAll(horizontals);
        copy.verticals.addAll(verticals);

        copy.regenerateLines(view);

        return copy;
    }

    public Guides copyForFlip(Flip.Direction direction, View view) {
        Guides copy = new Guides();
        copy.name = name + " copy";

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

    public Guides copyForRotate(Rotate.SpecialAngle angle, View view) {
        Guides copy = new Guides();
        copy.name = name + " copy";

        if (angle == ANGLE_90) {
            for (Double horizontal : horizontals) {
                copy.verticals.add(1 - horizontal);
            }
            copy.horizontals.addAll(verticals);
        } else if (angle == ANGLE_180) {
            for (Double horizontal : horizontals) {
                copy.horizontals.add(1 - horizontal);
            }
            for (Double vertical : verticals) {
                copy.verticals.add(1 - vertical);
            }
        } else if (angle == ANGLE_270) {
            copy.verticals.addAll(horizontals);
            for (Double vertical : verticals) {
                copy.horizontals.add(1 - vertical);
            }
        } else {
            throw new IllegalStateException();
        }

        copy.regenerateLines(view);

        return copy;
    }

    public Guides copyForEnlargedCanvas(int north, int east, int south, int west, View view) {
        Guides copy = new Guides();
        copy.setName(String.format("enlarged : north = %d, east = %d, south = %d, west = %d%n",
                north, east, south, west));
        Canvas canvas = view.getCanvas();
        int oldWidth = canvas.getImWidth();
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

        int oldHeight = canvas.getImHeight();
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

        copy.regenerateLines(view);
        return copy;
    }

    public Guides copyForCrop(Rectangle cropRect, View view) {
        Canvas canvas = view.getCanvas();
        int northMargin = cropRect.y;
        int westMargin = cropRect.x;
        int southMargin = canvas.getImHeight() - cropRect.height - cropRect.y;
        int eastMargin = canvas.getImWidth() - cropRect.width - cropRect.x;

        // a crop is a negative enlargement
        return copyForEnlargedCanvas(
                -northMargin, -eastMargin,
                -southMargin, -westMargin,
                view);
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
        double percent = pixels / (double) canvas.getImWidth();
        horizontals.add(percent);
    }

    public void addVerRelative(double percent) {
        verticals.add(percent);
    }

    public void addVerAbsolute(int pixels, Canvas canvas) {
        double percent = pixels / (double) canvas.getImHeight();
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
                                int verDist,
                                View view) {
        Canvas canvas = view.getCanvas();
        // horizontal lines
        int distFromTop = 0;
        int canvasHeight = canvas.getImHeight();
        for (int i = 0; i < horNumber; i++) {
            distFromTop += horDist;
            double lineY = distFromTop / (double) canvasHeight;
            horizontals.add(lineY);
        }

        // vertical lines
        int distFromLeft = 0;
        int canvasWidth = canvas.getImWidth();
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

    private void regenerateLines(View view) {
        Canvas canvas = view.getCanvas();
        int width = canvas.getImWidth();
        int height = canvas.getImHeight();
        CanvasMargins margins = view.getCanvasMargins();

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

    // the view parameter
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
            Composition comp = view.getComp();
            comp.setGuides(guides);
            comp.repaint();
            if (!preview) {
                History.addEdit(new GuidesChangeEdit(comp, oldGuides, guides));
            }
        }

        public void resetOldGuides() {
            Composition comp = view.getComp();
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
