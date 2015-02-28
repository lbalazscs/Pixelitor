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

/**
 * The "Action" in the shapes tool
 */
public enum ShapesAction {
    FILL(true, false, false, false, true, true, false, "Fill") {
    }, STROKE(false, true, true, true, false, true, false, "Stroke") {
    }, FILL_AND_STROKE(true, true, true, true, true, true, false, "Fill and Stroke") {
    }, EFFECTS_ONLY(false, false, false, false, false, true, false, "Effects Only") {
    }, SELECTION(false, false, false, false, false, false, true, "Selection") {
    }, SELECTION_FROM_STROKE(false, false, true, false, false, false, true, "Stroked Selection") {
    };

    private final boolean enableStrokeSettings;
    private final boolean enableFillPaintSelection;
    private final boolean enableStrokePaintSelection;

    private final boolean stroke;
    private final boolean fill;
    private final boolean drawEffects;
    private final boolean createSelection;

    private final String guiName;

    ShapesAction(boolean enableFillPaintSelection, boolean enableStrokePaintSelection, boolean enableStrokeSettings, boolean stroke, boolean fill, boolean drawEffects, boolean createSelection, String guiName) {
        this.enableFillPaintSelection = enableFillPaintSelection;
        this.enableStrokePaintSelection = enableStrokePaintSelection;
        this.enableStrokeSettings = enableStrokeSettings;
        this.stroke = stroke;
        this.fill = fill;
        this.drawEffects = drawEffects;
        this.createSelection = createSelection;
        this.guiName = guiName;

        // check whether the arguments are compatible with each other
        if (createSelection) {
            if (stroke || fill || drawEffects) {
                throw new IllegalArgumentException();
            }
        } else if (drawEffects) {
            // it is ok
        } else {
            if (!stroke && !fill) {
                throw new IllegalArgumentException();
            }
        }
    }

    public boolean enableStrokeSettings() {
        return enableStrokeSettings;
    }

    public boolean enableFillPaintSelection() {
        return enableFillPaintSelection;
    }

    public boolean enableStrokePaintSelection() {
        return enableStrokePaintSelection;
    }

    public boolean hasStroke() {
        return stroke;
    }

    public boolean hasFill() {
        return fill;
    }

    public boolean drawEffects() {
        return drawEffects;
    }

    public boolean createSelection() {
        return createSelection;
    }

    @Override
    public String toString() {
        return guiName;
    }
}
