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

import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.gui.FilterParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.UserPreset;
import pixelitor.filters.lookup.GrayScaleLookup;
import pixelitor.filters.lookup.RGBLookup;
import pixelitor.filters.util.Channel;

import java.util.ArrayList;
import java.util.List;

import static pixelitor.filters.util.Channel.BLUE;
import static pixelitor.filters.util.Channel.GREEN;
import static pixelitor.filters.util.Channel.RED;
import static pixelitor.filters.util.Channel.RGB;

public class LevelsModel {
    private final ChannelLevelsModel rgbModel;
    private final ChannelLevelsModel rModel;
    private final ChannelLevelsModel gModel;
    private final ChannelLevelsModel bModel;
    private final Levels filter;
    private FilterGUI lastGUI;

    /**
     * Contains the sub-models in the order they should appear in the GUI.
     */
    private final ChannelLevelsModel[] subModels;

    public LevelsModel(Levels filter) {
        this.filter = filter;
        rgbModel = new ChannelLevelsModel(RGB, this);
        rModel = new ChannelLevelsModel(RED, this);
        gModel = new ChannelLevelsModel(GREEN, this);
        bModel = new ChannelLevelsModel(BLUE, this);

        subModels = new ChannelLevelsModel[]{
            rgbModel, rModel, gModel, bModel};
    }

    public void setLastGUI(FilterGUI lastGUI) {
        this.lastGUI = lastGUI;
    }

    /**
     * Updates the filter and notifies the GUI to refresh the preview.
     */
    public void settingsChanged() {
        updateFilterLookup();

        if (lastGUI == null) {
            return; // lastGUI is null when loading a smart filter or running non-interactively
        }
        lastGUI.startPreview(false);
    }

    /**
     * Builds the combined lookup table from all channel models and applies it to the filter.
     */
    public void updateFilterLookup() {
        GrayScaleLookup rgb = rgbModel.getLookup();
        GrayScaleLookup r = rModel.getLookup();
        GrayScaleLookup g = gModel.getLookup();
        GrayScaleLookup b = bModel.getLookup();

        RGBLookup combinedLookup = new RGBLookup(rgb, r, g, b);
        filter.setRGBLookup(combinedLookup);
    }

    public void resetAllAndRun() {
        resetAll();
        settingsChanged();
    }

    public void resetAll() {
        for (ChannelLevelsModel model : subModels) {
            model.resetToDefaults();
        }
    }

    public void resetChannelToDefault(Channel channel) {
        getModelForChannel(channel).resetToDefaults();
        settingsChanged();
    }

    private ChannelLevelsModel getModelForChannel(Channel channel) {
        for (ChannelLevelsModel model : subModels) {
            if (model.getChannel() == channel) {
                return model;
            }
        }
        throw new IllegalArgumentException("channel: " + channel);
    }

    public ChannelLevelsModel getRgbModel() {
        return rgbModel;
    }

    public ChannelLevelsModel[] getSubModels() {
        return subModels;
    }

    public ParamSet getParamSet() {
        List<FilterParam> params = new ArrayList<>();
        for (ChannelLevelsModel subModel : subModels) {
            params.add(subModel.getInputDark());
            params.add(subModel.getInputLight());
            params.add(subModel.getOutputDark());
            params.add(subModel.getOutputLight());
        }

        ParamSet paramSet = new ParamSet();
        paramSet.addParams(params);
        return paramSet;
    }

    public void saveToUserPreset(UserPreset preset) {
        for (ChannelLevelsModel model : subModels) {
            model.saveToUserPreset(preset);
        }
    }

    public void loadUserPreset(UserPreset preset) {
        for (ChannelLevelsModel model : subModels) {
            model.loadUserPreset(preset);
        }
        settingsChanged();
    }
}
