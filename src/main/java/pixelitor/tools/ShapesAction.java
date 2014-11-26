/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.tools;

/**
 *
 */
public enum ShapesAction {
    FILL(true, false, false, false, true, true, false) {
        @Override
        public String toString() {
            return "Fill";
        }
    }, STROKE(false, true, true, true, false, true, false) {
        @Override
        public String toString() {
            return "Stroke";
        }
    }, FILL_AND_STROKE(true, true, true, true, true, true, false) {
        @Override
        public String toString() {
            return "Fill and Stroke";
        }
    }, EFFECTS_ONLY(false, false, false, false, false, true, false) {
        @Override
        public String toString() {
            return "Effects Only";
        }
    }, SELECTION(false, false, false, false, false, false, true) {
        @Override
        public String toString() {
            return "Selection";
        }
    }, SELECTION_FROM_STROKE(false, false, true, false, false, false, true) {
        @Override
        public String toString() {
            return "Stroked Selection";
        }
    };

    private final boolean enableStrokeSettings;
    private final boolean enableFillPaintSelection;
    private final boolean enableStrokePaintSelection;

    private final boolean stroke;
    private final boolean fill;
    private final boolean drawEffects;
    private final boolean createSelection;

    ShapesAction(boolean enableFillPaintSelection, boolean enableStrokePaintSelection, boolean enableStrokeSettings, boolean stroke, boolean fill, boolean drawEffects, boolean createSelection) {
        this.enableFillPaintSelection = enableFillPaintSelection;
        this.enableStrokePaintSelection = enableStrokePaintSelection;
        this.enableStrokeSettings = enableStrokeSettings;
        this.stroke = stroke;
        this.fill = fill;
        this.drawEffects = drawEffects;
        this.createSelection = createSelection;

        if (createSelection) {
            if (stroke || fill || drawEffects) {
                throw new IllegalArgumentException();
            }
        } else if (drawEffects) {
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

}
