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

package pixelitor.filters.levels;

import org.jdesktop.swingx.VerticalLayout;
import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.UserPreset;
import pixelitor.filters.lookup.GrayScaleLookup;
import pixelitor.filters.util.Channel;
import pixelitor.gui.utils.SliderSpinner;

import javax.swing.*;

import static java.awt.Color.GRAY;

/**
 * The model for the settings of a single channel in the Levels filter.
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

        // ensure input dark and light levels don't cross
        inputLight.ensureHigherValueThan(inputDark);

        for (RangeParam param : params) {
            param.setAdjustmentListener(this);
        }
    }

    /**
     * Returns the parameter for the input shadow level.
     */
    public RangeParam getInputDark() {
        return inputDark;
    }

    /**
     * Returns the parameter for the input highlight level.
     */
    public RangeParam getInputLight() {
        return inputLight;
    }

    /**
     * Returns the parameter for the output shadow level.
     */
    public RangeParam getOutputDark() {
        return outputDark;
    }

    /**
     * Returns the parameter for the output highlight level.
     */
    public RangeParam getOutputLight() {
        return outputLight;
    }

    /**
     * Returns the lookup table for this channel's level adjustments.
     */
    public GrayScaleLookup getLookup() {
        return lookup;
    }

    @Override
    public void paramAdjusted() {
        updateLookup();
        mainModel.settingsChanged();
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

    /**
     * Returns the channel represented by this model.
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * Returns the UI panel that can configure this model.
     */
    JPanel createPanel() {
        var inputDarkSlider = new SliderSpinner(getInputDark(), GRAY, channel.getDarkColor());
        var inputLightSlider = new SliderSpinner(getInputLight(), channel.getLightColor(), GRAY);
        var outputDarkSlider = new SliderSpinner(getOutputDark(), GRAY, channel.getLightColor());
        var outputLightSlider = new SliderSpinner(getOutputLight(), channel.getDarkColor(), GRAY);

        JPanel panel = new JPanel(new VerticalLayout());
        panel.add(inputDarkSlider);
        panel.add(inputLightSlider);
        panel.add(outputDarkSlider);
        panel.add(outputLightSlider);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        return panel;
    }
}
