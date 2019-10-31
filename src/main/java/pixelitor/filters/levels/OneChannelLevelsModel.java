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

package pixelitor.filters.levels;

import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.filters.gui.RangeParam;

import java.awt.Color;

/**
 * The model (the GUI-independent part) of the settings
 * for one channel in the Levels filter
 */
public class OneChannelLevelsModel implements ParamAdjustmentListener {
    public static final int MAX_VALUE = 255;
    public static final int MIN_VALUE = 0;
    private static final int DARK_DEFAULT = MIN_VALUE;
    private static final int LIGHT_DEFAULT = MAX_VALUE;

    private GrayScaleLookup lookup = GrayScaleLookup.getIdentity();

    private final RangeParam inputDark;
    private final RangeParam inputLight;
    private final RangeParam outputDark;
    private final RangeParam outputLight;
    private final EditedChannelsType type;
    private final LevelsModel bigModel;

    public OneChannelLevelsModel(EditedChannelsType type, LevelsModel bigModel) {
        this.type = type;
        this.bigModel = bigModel;

        inputDark = new RangeParam("Input Dark", MIN_VALUE, DARK_DEFAULT, MAX_VALUE);
        inputLight = new RangeParam("Input Light", MIN_VALUE, LIGHT_DEFAULT, MAX_VALUE);
        outputDark = new RangeParam("Output Dark", MIN_VALUE, DARK_DEFAULT, MAX_VALUE);
        outputLight = new RangeParam("Output Light", MIN_VALUE, LIGHT_DEFAULT, MAX_VALUE);

        ensureInputValueOrdering();

        inputDark.setAdjustmentListener(this);
        inputLight.setAdjustmentListener(this);
        outputDark.setAdjustmentListener(this);
        outputLight.setAdjustmentListener(this);
    }

    private void ensureInputValueOrdering() {
        inputDark.addChangeListener(e -> {
            int darkValue = inputDark.getValue();
            int lightValue = inputLight.getValue();
            if (lightValue <= darkValue) {
                if (darkValue < MAX_VALUE) {
                    inputLight.setValueNoTrigger(darkValue + 1);
                } else {
                    inputLight.setValueNoTrigger(MAX_VALUE);
                }
            }
        });
        inputLight.addChangeListener(e -> {
            int darkValue = inputDark.getValue();
            int lightValue = inputLight.getValue();
            if (lightValue <= darkValue) {
                if (lightValue > MIN_VALUE) {
                    inputDark.setValueNoTrigger(lightValue - 1);
                } else {
                    inputDark.setValueNoTrigger(MIN_VALUE);
                }
            }
        });
    }

    public Color getDarkColor() {
        return type.getDarkColor();
    }

    public Color getLightColor() {
        return type.getLightColor();
    }

    public RangeParam getInputDark() {
        return inputDark;
    }

    public RangeParam getInputLight() {
        return inputLight;
    }

    public RangeParam getOutputDark() {
        return outputDark;
    }

    public RangeParam getOutputLight() {
        return outputLight;
    }

    public String getName() {
        return type.getName();
    }

    public GrayScaleLookup getLookup() {
        return lookup;
    }

    @Override
    public void paramAdjusted() {
        updateAdjustment();

        bigModel.adjustmentChanged();
    }

    private void updateAdjustment() {
        lookup = new GrayScaleLookup(
                inputDark.getValue(),
                inputLight.getValue(),
                outputDark.getValue(),
                outputLight.getValue());
    }

    public void resetToDefaults() {
        inputDark.reset(false);
        inputLight.reset(false);
        outputDark.reset(false);
        outputLight.reset(false);

        updateAdjustment();
    }
}
