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

package pixelitor.gui;

import pixelitor.gui.utils.OpenViewEnabledAction;
import pixelitor.menus.view.ZoomMenu;

import javax.swing.*;

/**
 * Automatic zoom modes for fitting content within the available space.
 */
public enum AutoZoom {
    FIT_SPACE("Fit Space", ZoomMenu.FIT_SPACE_TOOLTIP) {
        @Override
        public double calcRatio(double horRatio, double verRatio) {
            return Math.max(horRatio, verRatio);
        }
    }, FIT_WIDTH("Fit Width", "Fit the width of the image to the available horizontal space") {
        @Override
        public double calcRatio(double horRatio, double verRatio) {
            return horRatio;
        }
    }, FIT_HEIGHT("Fit Height", "Fit the height of the image to the available vertical space") {
        @Override
        public double calcRatio(double horRatio, double verRatio) {
            return verRatio;
        }
    }, ACTUAL_PIXELS("Actual Pixels", ZoomMenu.ACTUAL_PIXELS_TOOLTIP) {
        @Override
        public double calcRatio(double horRatio, double verRatio) {
            throw new IllegalStateException("should not be called");
        }
    };

    public static final Action ACTUAL_PIXELS_ACTION = ACTUAL_PIXELS.toAction();
    public static final Action FIT_HEIGHT_ACTION = FIT_HEIGHT.toAction();
    public static final Action FIT_WIDTH_ACTION = FIT_WIDTH.toAction();
    public static final Action FIT_SPACE_ACTION = FIT_SPACE.toAction();

    private final String displayName;
    private final String toolTip;

    AutoZoom(String displayName, String toolTip) {
        this.displayName = displayName;
        this.toolTip = toolTip;
    }

    /**
     * Selects the image-to-available-area ratio that
     * will be used for the auto zoom calculations
     */
    public abstract double calcRatio(double horRatio, double verRatio);

    private Action toAction() {
        var action = new OpenViewEnabledAction(displayName,
            comp -> comp.getView().setZoom(this));
        action.setToolTip(toolTip);
        return action;
    }
}
