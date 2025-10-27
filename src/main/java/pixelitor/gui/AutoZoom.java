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

package pixelitor.gui;

import pixelitor.gui.utils.ViewEnabledAction;
import pixelitor.utils.Utils;

import javax.swing.*;

import static java.lang.String.format;
import static pixelitor.utils.Keys.ACTUAL_PIXELS_KEY;
import static pixelitor.utils.Keys.FIT_SPACE_KEY;

/**
 * Automatic zoom modes for fitting content within the available space.
 */
public enum AutoZoom {
    FIT_SPACE("Fit Space") {
        @Override
        public double calcRatio(double horRatio, double verRatio) {
            return Math.max(horRatio, verRatio);
        }
    }, FIT_WIDTH("Fit Width") {
        @Override
        public double calcRatio(double horRatio, double verRatio) {
            return horRatio;
        }
    }, FIT_HEIGHT("Fit Height") {
        @Override
        public double calcRatio(double horRatio, double verRatio) {
            return verRatio;
        }
    }, ACTUAL_PIXELS("Actual Pixels") {
        @Override
        public double calcRatio(double horRatio, double verRatio) {
            throw new IllegalStateException("should not be called");
        }
    };

    public static final String FIT_SPACE_TOOLTIP = format(
        "Display the image at the largest zoom that can fit in the available space (%s)",
        Utils.keystrokeToText(FIT_SPACE_KEY));
    public static final String ACTUAL_PIXELS_TOOLTIP = format(
        "Set the zoom level to 100%% (%s)",
        Utils.keystrokeToText(ACTUAL_PIXELS_KEY));

    // these actions aren't stored inside the enum constants
    // to prevent static initialization problems
    public static final Action ACTUAL_PIXELS_ACTION = ACTUAL_PIXELS.toAction(ACTUAL_PIXELS_TOOLTIP);
    public static final Action FIT_HEIGHT_ACTION = FIT_HEIGHT.toAction("Fit the height of the image to the available vertical space");
    public static final Action FIT_WIDTH_ACTION = FIT_WIDTH.toAction("Fit the width of the image to the available horizontal space");
    public static final Action FIT_SPACE_ACTION = FIT_SPACE.toAction(FIT_SPACE_TOOLTIP);

    private final String displayName;

    AutoZoom(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Determines which ratio (horizontal or vertical) to use
     * for the zoom calculation, based on the auto-zoom mode.
     */
    public abstract double calcRatio(double horRatio, double verRatio);

    private Action toAction(String toolTip) {
        var action = new ViewEnabledAction(displayName,
            comp -> comp.getView().setZoom(this));
        action.setToolTip(toolTip);
        return action;
    }
}
