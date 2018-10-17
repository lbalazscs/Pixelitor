/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
package pixelitor.tools.shapes;

/**
 * The "Action" in the shapes tool
 */
public enum ShapesAction {
    FILL("Fill", true, false, false, true, false) {
    }, STROKE("Stroke", false, true, true, true, false) {
    }, FILL_AND_STROKE("Fill and Stroke", true, true, true, true, false) {
    }, EFFECTS_ONLY("Effects Only", false, false, false, true, false) {
    }, SELECTION("Selection", false, false, false, false, true) {
    }, SELECTION_FROM_STROKE("Selection from Stroke", false, false, true, false, true) {
    };

    private final String guiName;
    private final boolean hasFillPaint;
    private final boolean hasStrokePaint;
    private final boolean hasStrokeSettings;
    private final boolean canHaveEffects;
    private final boolean createSelection;

    ShapesAction(String guiName, boolean hasFillPaint,
                 boolean hasStrokePaint,
                 boolean hasStrokeSettings,
                 boolean canHaveEffects,
                 boolean createSelection) {

        this.hasFillPaint = hasFillPaint;
        this.hasStrokePaint = hasStrokePaint;
        this.hasStrokeSettings = hasStrokeSettings;
        this.canHaveEffects = canHaveEffects;
        this.createSelection = createSelection;
        this.guiName = guiName;

        // check whether the arguments are compatible with each other
        if (createSelection) {
            if (hasStrokePaint || hasFillPaint || canHaveEffects) {
                throw new IllegalArgumentException();
            }
        } else if (canHaveEffects) {
            // it is ok
        } else {
            if (!hasStrokePaint && !hasFillPaint) {
                throw new IllegalArgumentException();
            }
        }
    }

    public boolean hasStrokeSettings() {
        return hasStrokeSettings;
    }

    public boolean hasFillPaint() {
        return hasFillPaint;
    }

    public boolean hasStrokePaint() {
        return hasStrokePaint;
    }

    public boolean canHaveEffects() {
        return canHaveEffects;
    }

    public boolean createSelection() {
        return createSelection;
    }

    @Override
    public String toString() {
        return guiName;
    }
}
