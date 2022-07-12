/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.gui.UserPreset;

import java.awt.Color;

/**
 * The model (the GUI-independent part) of the settings
 * for one channel in the Levels filter
 */
public class ChannelLevelsModel implements ParamAdjustmentListener {
    private static final int MAX_VALUE = 255;
    private static final int MIN_VALUE = 0;
    private static final int DARK_DEFAULT = MIN_VALUE;
    private static final int LIGHT_DEFAULT = MAX_VALUE;

    private GrayScaleLookup lookup = GrayScaleLookup.getIdentity();

    private final RangeParam inputDark;
    private final RangeParam inputLight;
    private final RangeParam outputDark;
    private final RangeParam outputLight;
    private final Channel channel;
    private final LevelsModel mainModel;
    private final RangeParam[] params;

    public ChannelLevelsModel(Channel channel, LevelsModel mainModel) {
        this.channel = channel;
        this.mainModel = mainModel;

        inputDark = new RangeParam("Input Dark", MIN_VALUE, DARK_DEFAULT, MAX_VALUE);
        inputLight = new RangeParam("Input Light", MIN_VALUE, LIGHT_DEFAULT, MAX_VALUE);
        outputDark = new RangeParam("Output Dark", MIN_VALUE, DARK_DEFAULT, MAX_VALUE);
        outputLight = new RangeParam("Output Light", MIN_VALUE, LIGHT_DEFAULT, MAX_VALUE);
        params = new RangeParam[]{inputDark, inputLight, outputDark, outputLight};

        ensureInputValueOrdering();

        for (RangeParam param : params) {
            param.setAdjustmentListener(this);
        }
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
        return channel.getDarkColor();
    }

    public Color getLightColor() {
        return channel.getLightColor();
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

    public String getChannelName() {
        return channel.getName();
    }

    public GrayScaleLookup getLookup() {
        return lookup;
    }

    @Override
    public void paramAdjusted() {
        updateLookup();

        mainModel.settingsChanged(false);
    }

    private void updateLookup() {
        lookup = new GrayScaleLookup(
            inputDark.getValue(),
            inputLight.getValue(),
            outputDark.getValue(),
            outputLight.getValue());
    }

    public void resetToDefaults() {
        for (RangeParam param : params) {
            param.reset(false);
        }

        updateLookup();
    }

    public void saveToUserPreset(UserPreset preset) {
        for (RangeParam param : params) {
            String key = uniqueKey(param);
            preset.put(key, param.copyState().toSaveString());
        }
    }

    public void loadUserPreset(UserPreset preset) {
        for (RangeParam param : params) {
            String key = uniqueKey(param);
            param.loadStateFrom(preset.get(key));
        }
        updateLookup();
    }

    private String uniqueKey(RangeParam param) {
        return channel.getPresetKey() + " " + param.getName();
    }

    public Channel getChannel() {
        return channel;
    }
}
