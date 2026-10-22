/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.channels.ChannelAdjustment;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.UserPreset;
import pixelitor.filters.lookup.ChannelLookup;
import pixelitor.filters.util.Channel;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.utils.Rnd;

import javax.swing.*;

import static java.awt.Color.GRAY;

/**
 * The model for the settings of a single channel in the Levels filter.
 */
public class ChannelLevelsModel implements ChannelAdjustment {
    private static final int MAX_VALUE = 255;
    private static final int MIN_VALUE = 0;
    private static final int DARK_DEFAULT = MIN_VALUE;
    private static final int LIGHT_DEFAULT = MAX_VALUE;

    private final RangeParam inputDark;
    private final RangeParam inputLight;
    private final RangeParam outputDark;
    private final RangeParam outputLight;
    private final Channel channel;
    private final RangeParam[] params;

    public ChannelLevelsModel(Channel channel, Runnable adjustmentListener) {
        this.channel = channel;

        inputDark = new RangeParam("Input Dark", MIN_VALUE, DARK_DEFAULT, MAX_VALUE);
        inputLight = new RangeParam("Input Light", MIN_VALUE, LIGHT_DEFAULT, MAX_VALUE);
        outputDark = new RangeParam("Output Dark", MIN_VALUE, DARK_DEFAULT, MAX_VALUE);
        outputLight = new RangeParam("Output Light", MIN_VALUE, LIGHT_DEFAULT, MAX_VALUE);
        params = new RangeParam[]{inputDark, inputLight, outputDark, outputLight};

        // ensure input dark and light levels don't cross
        inputLight.ensureNotLowerThan(inputDark);

        if (adjustmentListener != null) {
            for (RangeParam param : params) {
                param.setAdjustmentListener(adjustmentListener::run);
            }
        }
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

    @Override
    public ChannelLookup toLookup() {
        return ChannelLookup.ofLevels(
            inputDark.getValue(),
            inputLight.getValue(),
            outputDark.getValue(),
            outputLight.getValue());
    }

    @Override
    public void reset() {
        for (RangeParam param : params) {
            param.reset(false);
        }
    }

    @Override
    public void randomize() {
        // randomize the input levels of this channel for a simple, common adjustment
        int dark = Rnd.nextInt(128);
        int light = Rnd.nextInt(128) + 128;

        inputDark.setValueNoTrigger(dark);
        inputLight.setValueNoTrigger(light);
    }

    @Override
    public void saveTo(UserPreset preset) {
        for (RangeParam param : params) {
            String key = uniqueKey(param);
            preset.put(key, param.copyState().toPresetString());
        }
    }

    @Override
    public void loadFrom(UserPreset preset) {
        for (RangeParam param : params) {
            String key = uniqueKey(param);
            String value = preset.get(key);
            if (value != null) {
                param.loadStateFrom(value);
            }
        }
    }

    private String uniqueKey(RangeParam param) {
        return channel.getPresetKey() + " " + param.getName();
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    /**
     * Creates the UI panel that can configure this model.
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
