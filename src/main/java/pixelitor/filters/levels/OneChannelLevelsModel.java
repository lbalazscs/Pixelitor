/*
 * Copyright 2017 Laszlo Balazs-Csiki
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

package pixelitor.filters.levels;

import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.filters.gui.RangeParam;

import java.awt.Color;

public class OneChannelLevelsModel implements ParamAdjustmentListener {
    private static final int BLACK_DEFAULT = 0;
    private static final int WHITE_DEFAULT = 255;

    private GrayScaleLookup adjustment = GrayScaleLookup.getDefault();

    private final RangeParam inputBlack;
    private final RangeParam inputWhite;
    private final RangeParam outputBlack;
    private final RangeParam outputWhite;
    private final LevelsAdjustmentType type;
    private final LevelsModel bigModel;

    public OneChannelLevelsModel(LevelsAdjustmentType type, LevelsModel bigModel) {
        this.type = type;
        this.bigModel = bigModel;

        inputBlack = new RangeParam("Input Dark", 0, BLACK_DEFAULT, 254);
        inputWhite = new RangeParam("Input Light", 1, WHITE_DEFAULT, 255);
        outputBlack = new RangeParam("Output Dark", 0, BLACK_DEFAULT, 254);
        outputWhite = new RangeParam("Output Light", 1, WHITE_DEFAULT, 255);

        inputBlack.setAdjustmentListener(this);
        inputWhite.setAdjustmentListener(this);
        outputBlack.setAdjustmentListener(this);
        outputWhite.setAdjustmentListener(this);
    }

    public Color getBackColor() {
        return type.getBackColor();
    }

    public Color getWhiteColor() {
        return type.getWhiteColor();
    }

    public RangeParam getInputBlack() {
        return inputBlack;
    }

    public RangeParam getInputWhite() {
        return inputWhite;
    }

    public RangeParam getOutputBlack() {
        return outputBlack;
    }

    public RangeParam getOutputWhite() {
        return outputWhite;
    }

    public String getName() {
        return type.getName();
    }

    public GrayScaleLookup getAdjustment() {
        return adjustment;
    }

    @Override
    public void paramAdjusted() {
        updateAdjustment();

        bigModel.adjustmentChanged();
    }

    private void updateAdjustment() {
        adjustment = new GrayScaleLookup(
                inputBlack.getValue(),
                inputWhite.getValue(),
                outputBlack.getValue(),
                outputWhite.getValue());
    }

    public void resetToDefaults() {
        inputBlack.reset(false);
        inputWhite.reset(false);
        outputBlack.reset(false);
        outputWhite.reset(false);

        updateAdjustment();
    }
}
