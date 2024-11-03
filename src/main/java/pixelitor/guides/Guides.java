/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.compactions.Outsets;
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

/**
 * Represents a set of guide lines.
 * Objects of this class should be mutated only while building,
 * for any changes a new instance should be built.
 */
public class Guides implements Serializable, Debuggable {
    @Serial
    private static final long serialVersionUID = -1168950961227421664L;

    // guide positions are stored as ratios (0.0 to 1.0) relative to the
    // canvas so that resizing does not affect their image-space position
    private final List<Double> horizontals = new ArrayList<>();
    private final List<Double> verticals = new ArrayList<>();

    // cached rendered lines in component space
    private transient List<Line2D> lines;

    // used only for debugging
    private String name;

    public Guides copyIdentical(View view) {
        Guides copy = createEmptyCopy();

        copy.horizontals.addAll(horizontals);
        copy.verticals.addAll(verticals);

        copy.regenerateLines(view);
        return copy;
    }

    public Guides copyFlipping(Flip.Direction direction, View view) {
        Guides copy = switch (direction) {
            case HORIZONTAL -> copyFlippingHorizontally();
            case VERTICAL -> copyFlippingVertically();
        };

        copy.regenerateLines(view);
        return copy;
    }

    private Guides copyFlippingHorizontally() {
        Guides copy = createEmptyCopy();
        copy.horizontals.addAll(horizontals);
        for (Double vertical : verticals) {
            copy.verticals.add(1 - vertical);
        }
        return copy;
    }

    private Guides copyFlippingVertically() {
        Guides copy = createEmptyCopy();
        copy.verticals.addAll(verticals);
        for (Double horizontal : horizontals) {
            copy.horizontals.add(1 - horizontal);
        }
        return copy;
    }

    public Guides copyRotating(QuadrantAngle angle, View view) {
        Guides copy = switch (angle) {
            case ANGLE_90 -> copyRotating90();
            case ANGLE_180 -> copyRotating180();
            case ANGLE_270 -> copyRotating270();
        };

        copy.regenerateLines(view);
        return copy;
    }

    private Guides copyRotating90() {
        Guides copy = createEmptyCopy();
        for (Double horizontal : horizontals) {
            copy.verticals.add(1 - horizontal);
        }
        copy.horizontals.addAll(verticals);
        return copy;
    }

    private Guides copyRotating180() {
        Guides copy = createEmptyCopy();
        for (Double horizontal : horizontals) {
            copy.horizontals.add(1 - horizontal);
        }
        for (Double vertical : verticals) {
            copy.verticals.add(1 - vertical);
        }
        return copy;
    }

    private Guides copyRotating270() {
        Guides copy = createEmptyCopy();
        copy.verticals.addAll(horizontals);
        for (Double vertical : verticals) {
            copy.horizontals.add(1 - vertical);
        }
        return copy;
    }

    private Guides createEmptyCopy() {
        Guides copy = new Guides();
        copy.setName(name + " copy");
        return copy;
    }

    public Guides copyEnlarging(Outsets enlargement, View view, Canvas canvas) {
        Guides copy = new Guides();
        copy.setName("copy with enlargement: " + enlargement.toString());

        copyVerticalsEnlarging(copy, enlargement.right, enlargement.left, canvas);
        copyHorizontalsEnlarging(copy, enlargement.top, enlargement.bottom, canvas);

        copy.regenerateLines(view);
        return copy;
    }

    private void copyVerticalsEnlarging(Guides copy, int right, int left, Canvas canvas) {
        int origWidth = canvas.getWidth();
        if (left != 0 || right != 0) {
            int newWidth = origWidth + right + left;
            for (Double h : verticals) {
                double origAbs = h * origWidth;
                double adjustedRatio = (origAbs + left) / newWidth;
                copy.verticals.add(adjustedRatio);
            }
        } else {
            copy.verticals.addAll(verticals);
        }
    }

    private void copyHorizontalsEnlarging(Guides copy, int top, int bottom, Canvas canvas) {
        int origHeight = canvas.getHeight();
        if (top != 0 || bottom != 0) {
            int newHeight = origHeight + top + bottom;
            for (Double v : horizontals) {
                double origAbs = v * origHeight;
                double adjustedRel = (origAbs + top) / newHeight;
                copy.horizontals.add(adjustedRel);
            }
        } else {
            copy.horizontals.addAll(horizontals);
        }
    }

    public Guides copyCropping(Rectangle cropRect, View view) {
        Canvas canvas = view.getCanvas();
        int topMargin = cropRect.y;
        int leftMargin = cropRect.x;
        int bottomMargin = canvas.getHeight() - cropRect.height - cropRect.y;
        int rightMargin = canvas.getWidth() - cropRect.width - cropRect.x;
        Outsets margin = new Outsets(topMargin, leftMargin, bottomMargin, rightMargin);

        // a crop is a negative enlargement
        margin.negate();
        return copyEnlarging(margin, view, canvas);
    }

    public static void showAddGridDialog(View view) {
        Builder builder = new Builder(view, true);
        AddGridGuidesPanel panel = new AddGridGuidesPanel(builder);
        new DialogBuilder()
            .title("Add Grid Guides")
            .content(panel)
            .withScrollbars()
            .okAction(() -> panel.createGuides(false))
            .cancelAction(builder::resetOrigGuides)
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
            .cancelAction(builder::resetOrigGuides)
            .show();
    }

    public void addHorRelative(double rel) {
        horizontals.add(rel);
    }

    public void addHorAbsolute(int pixels, Canvas canvas) {
        horizontals.add(pixels / (double) canvas.getWidth());
    }

    public void addVerRelative(double rel) {
        verticals.add(rel);
    }

    public void addVerAbsolute(int pixels, Canvas canvas) {
        verticals.add(pixels / (double) canvas.getHeight());
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
        int marginX = view.getCanvasStartX();
        int marginY = view.getCanvasStartY();

        lines = new ArrayList<>();

        // horizontal guides
        for (Double h : horizontals) {
            double y = h * canvasHeight;

            // the generated lines have to be in component space
            double coY = view.imageYToComponentSpace(y);
            double coStartX = view.imageXToComponentSpace(0) - marginX;
            double coEndX = view.imageXToComponentSpace(canvasWidth) + marginX;

            lines.add(new Line2D.Double(coStartX, coY, coEndX, coY));
        }

        // vertical guides
        for (Double v : verticals) {
            double x = v * canvasWidth;

            double coX = view.imageXToComponentSpace(x);
            double coStartY = view.imageYToComponentSpace(0) - marginY;
            double coEndY = view.imageYToComponentSpace(canvasHeight) + marginY;

            lines.add(new Line2D.Double(coX, coStartY, coX, coEndY));
        }
    }

    public void draw(Graphics2D g) {
        GuidesRenderer.GUIDES_INSTANCE.get().draw(g, lines);
    }

    public void coCoordsChanged(View view) {
        regenerateLines(view);
    }

    private void copyValuesFrom(Guides other) {
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
        private final Guides origGuides;

        public Builder(View view, boolean clearByDefault) {
            this.view = view;
            origGuides = view.getComp().getGuides();
            clearExisting = new BooleanParam("Clear Existing Guides", clearByDefault);
        }

        public void build(boolean preview, Consumer<Guides> setup) {
            Guides guides = new Guides();
            boolean useExisting = !clearExisting.isChecked();
            if (useExisting && origGuides != null) {
                guides.copyValuesFrom(origGuides);
            }

            setup.accept(guides);

            guides.regenerateLines(view);
            var comp = view.getComp();
            comp.setGuides(guides);
            comp.repaint();
            if (!preview) {
                History.add(new GuidesChangeEdit(comp, origGuides, guides));
            }
        }

        public void resetOrigGuides() {
            var comp = view.getComp();
            comp.setGuides(origGuides);
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
