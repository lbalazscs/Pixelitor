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
import pixelitor.filters.gui.UserPreset;
import pixelitor.filters.lookup.GrayScaleLookup;
import pixelitor.filters.lookup.RGBLookup;
import pixelitor.filters.util.Channel;
import pixelitor.filters.util.ColorSpace;
import pixelitor.utils.Rnd;

import static pixelitor.filters.util.Channel.BLUE;
import static pixelitor.filters.util.Channel.GREEN;
import static pixelitor.filters.util.Channel.OK_A;
import static pixelitor.filters.util.Channel.OK_B;
import static pixelitor.filters.util.Channel.OK_L;
import static pixelitor.filters.util.Channel.RED;
import static pixelitor.filters.util.Channel.RGB;

public class LevelsModel {
    private final ChannelLevelsModel rgbModel;
    private final ChannelLevelsModel rModel;
    private final ChannelLevelsModel gModel;
    private final ChannelLevelsModel bModel;
    private final ChannelLevelsModel okLModel;
    private final ChannelLevelsModel okAModel;
    private final ChannelLevelsModel okBModel;

    private final Levels filter;
    private FilterGUI lastGUI;

    private ColorSpace colorSpace = ColorSpace.SRGB;

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

        okLModel = new ChannelLevelsModel(OK_L, this);
        okAModel = new ChannelLevelsModel(OK_A, this);
        okBModel = new ChannelLevelsModel(OK_B, this);

        subModels = new ChannelLevelsModel[]{
            rgbModel, rModel, gModel, bModel,
            okLModel, okAModel, okBModel
        };
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
        switch (colorSpace) {
            case SRGB -> updateSrgbFilterLookup();
            case OKLAB -> updateOklabFilterLookup();
        }
    }

    private void updateSrgbFilterLookup() {
        GrayScaleLookup rgb = rgbModel.getLookup();
        GrayScaleLookup r = rModel.getLookup();
        GrayScaleLookup g = gModel.getLookup();
        GrayScaleLookup b = bModel.getLookup();
        filter.setFilterOp(new RGBLookup(rgb, r, g, b).asFastLookupOp());
    }

    private void updateOklabFilterLookup() {
        GrayScaleLookup l = okLModel.getLookup();
        GrayScaleLookup a = okAModel.getLookup();
        GrayScaleLookup b = okBModel.getLookup();
        filter.setFilterOp(new OklabLevelsFilter(l, a, b));
    }

    public void resetAllAndRun() {
        resetAll();
        settingsChanged();
    }

    public void resetAll() {
        this.colorSpace = ColorSpace.SRGB;

        for (ChannelLevelsModel model : subModels) {
            model.resetToDefaults();
        }
    }

    public void resetChannelToDefault(Channel channel) {
        getModelForChannel(channel).resetToDefaults();
        settingsChanged();
    }

    private ChannelLevelsModel getModelForChannel(Channel channel) {
        return switch (channel) {
            case RGB -> rgbModel;
            case RED -> rModel;
            case GREEN -> gModel;
            case BLUE -> bModel;
            case OK_L -> okLModel;
            case OK_A -> okAModel;
            case OK_B -> okBModel;
        };
    }

    public ChannelLevelsModel[] getSubModels() {
        return subModels;
    }

    public void saveToUserPreset(UserPreset preset) {
        preset.put("Color Space", colorSpace.toString());
        for (ChannelLevelsModel model : subModels) {
            model.saveToUserPreset(preset);
        }
    }

    public void loadUserPreset(UserPreset preset) {
        // will default to sRGB for old presets that don't have this key
        ColorSpace targetSpace = preset.getEnum("Color Space", ColorSpace.class);
        setColorSpace(targetSpace, false);

        for (ChannelLevelsModel model : subModels) {
            model.loadUserPreset(preset);
        }
        settingsChanged();
    }

    public void randomizeAndRun() {
        resetAll();

        // Get the primary channel for the current color space
        Channel primaryChannel = colorSpace.getPrimaryChannel();
        ChannelLevelsModel primaryModel = getModelForChannel(primaryChannel);

        // randomize the input levels of the main RGB channel for a simple, common adjustment
        int inputDark = Rnd.nextInt(128);
        int inputLight = Rnd.nextInt(128) + 128;

        primaryModel.getInputDark().setValueNoTrigger(inputDark);
        primaryModel.getInputLight().setValueNoTrigger(inputLight);

        // manually trigger an update of the model and filter
        primaryModel.paramAdjusted();
    }

    public void setColorSpace(ColorSpace colorSpace, boolean trigger) {
        if (this.colorSpace == colorSpace) {
            return;
        }
        this.colorSpace = colorSpace;
        if (trigger) {
            settingsChanged();
        }
    }
}
