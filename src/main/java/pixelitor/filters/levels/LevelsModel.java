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

import pixelitor.filters.gui.UserPreset;
import pixelitor.filters.lookup.GrayScaleLookup;
import pixelitor.filters.lookup.RGBLookup;
import pixelitor.filters.util.Channel;
import pixelitor.filters.util.ColorSpace;
import pixelitor.utils.Rnd;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import static pixelitor.filters.util.Channel.BLUE;
import static pixelitor.filters.util.Channel.GREEN;
import static pixelitor.filters.util.Channel.OK_A;
import static pixelitor.filters.util.Channel.OK_B;
import static pixelitor.filters.util.Channel.OK_L;
import static pixelitor.filters.util.Channel.RED;
import static pixelitor.filters.util.Channel.RGB;

public class LevelsModel {
    private final Map<Channel, ChannelLevelsModel> channelModels;

    private LevelsGUI levelsGUI;
    private BufferedImageOp filterOp;

    private ColorSpace colorSpace = ColorSpace.SRGB;

    public LevelsModel() {
        channelModels = new EnumMap<>(Channel.class);

        for (Channel channel : Channel.values()) {
            channelModels.put(channel, new ChannelLevelsModel(channel, this));
        }
    }

    public void setLevelsGUI(LevelsGUI levelsGUI) {
        this.levelsGUI = levelsGUI;
    }

    /**
     * Applies the configured filter operation to the source image.
     *
     * @param src  the source image
     * @param dest the destination image, or null
     * @return the filtered image
     */
    public BufferedImage apply(BufferedImage src, BufferedImage dest) {
        if (filterOp == null) {
            // this can happen if this method is called before
            // the GUI is created or a preset is loaded
            updateFilterLookup();
        }
        return filterOp.filter(src, dest);
    }

    /**
     * Returns the current filter operation. Used for testing purposes.
     */
    BufferedImageOp getFilterOp() {
        return filterOp;
    }

    /**
     * Updates the filter and notifies the GUI to refresh the preview.
     */
    public void settingsChanged() {
        updateFilterLookup();

        if (levelsGUI == null) {
            return; // it's null when loading a smart filter or running non-interactively
        }
        levelsGUI.startPreview(false);
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
        GrayScaleLookup rgb = getLookupForChannel(RGB);
        GrayScaleLookup r = getLookupForChannel(RED);
        GrayScaleLookup g = getLookupForChannel(GREEN);
        GrayScaleLookup b = getLookupForChannel(BLUE);
        this.filterOp = new RGBLookup(rgb, r, g, b).asFastLookupOp();
    }

    private void updateOklabFilterLookup() {
        GrayScaleLookup l = getLookupForChannel(OK_L);
        GrayScaleLookup a = getLookupForChannel(OK_A);
        GrayScaleLookup b = getLookupForChannel(OK_B);
        this.filterOp = new OklabLevelsFilter(l, a, b);
    }

    public void resetAllAndRun() {
        resetAll();
        settingsChanged();
    }

    public void resetAll() {
        this.colorSpace = ColorSpace.SRGB;

        for (ChannelLevelsModel model : getSubModels()) {
            model.resetToDefaults();
        }
    }

    public void resetChannelToDefault(Channel channel) {
        getModelForChannel(channel).resetToDefaults();
        settingsChanged();
    }

    public ChannelLevelsModel getModelForChannel(Channel channel) {
        return channelModels.get(channel);
    }

    private GrayScaleLookup getLookupForChannel(Channel channel) {
        return channelModels.get(channel).getLookup();
    }

    public Collection<ChannelLevelsModel> getSubModels() {
        return channelModels.values();
    }

    public void saveToUserPreset(UserPreset preset) {
        preset.put(ColorSpace.PRESET_KEY, colorSpace.name());
        for (var entry : channelModels.entrySet()) {
            Channel channel = entry.getKey();
            if (channel.getColorSpace() == colorSpace) {
                ChannelLevelsModel channelModel = entry.getValue();
                channelModel.saveToUserPreset(preset);
            }
        }
    }

    public void loadUserPreset(UserPreset preset) {
        ColorSpace targetSpace = preset.getEnum(ColorSpace.PRESET_KEY, ColorSpace.class);
        setColorSpace(targetSpace, false);

        for (var entry : channelModels.entrySet()) {
            Channel channel = entry.getKey();
            if (channel.getColorSpace() == colorSpace) {
                ChannelLevelsModel channelModel = entry.getValue();
                channelModel.loadUserPreset(preset);
            }
        }

        settingsChanged();
    }

    public void randomizeAndRun() {
        resetAll();

        Channel primaryChannel = colorSpace.getPrimaryChannel();
        ChannelLevelsModel primaryModel = getModelForChannel(primaryChannel);

        // randomize the input levels of the primary channel for a simple, common adjustment
        int inputDark = Rnd.nextInt(128);
        int inputLight = Rnd.nextInt(128) + 128;

        primaryModel.getInputDark().setValueNoTrigger(inputDark);
        primaryModel.getInputLight().setValueNoTrigger(inputLight);

        // trigger an update of the model and filter
        primaryModel.paramAdjusted();
    }

    public void setColorSpace(ColorSpace colorSpace, boolean triggerFilter) {
        if (this.colorSpace == colorSpace) {
            return;
        }
        this.colorSpace = colorSpace;

        if (levelsGUI != null) {
            // synchronize the UI dropdown to reflect the 
            // data model (necessary when loading a preset)
            levelsGUI.setColorSpaceUI(colorSpace);
        }

        if (triggerFilter) {
            settingsChanged();
        }
    }
}
