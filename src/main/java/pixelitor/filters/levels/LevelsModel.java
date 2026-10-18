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
import pixelitor.filters.lookup.ChannelLookup;
import pixelitor.filters.lookup.RGBLookup;
import pixelitor.filters.util.Channel;
import pixelitor.filters.util.ColorSpace;
import pixelitor.utils.Rnd;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import static pixelitor.filters.util.Channel.*;

/**
 * The data model for the {@link Levels} filter, managing the channel
 * settings for each supported color space.
 */
public class LevelsModel {
    private final Map<Channel, ChannelLevelsModel> channelModels;

    private LevelsGUI levelsGUI;

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
        return createFilterOp().filter(src, dest);
    }

    /**
     * Creates the filter operation based on the channel settings of the active color space.
     */
    BufferedImageOp createFilterOp() {
        return switch (colorSpace) {
            case SRGB -> createSrgbOp();
            case OKLAB -> createOklabOp();
        };
    }

    /**
     * Notifies the GUI to refresh the preview.
     */
    public void settingsChanged() {
        if (levelsGUI != null) {
            levelsGUI.startPreview(false);
        }
    }

    private BufferedImageOp createSrgbOp() {
        ChannelLookup rgb = getLookupForChannel(RGB);
        ChannelLookup r = getLookupForChannel(RED);
        ChannelLookup g = getLookupForChannel(GREEN);
        ChannelLookup b = getLookupForChannel(BLUE);
        return new RGBLookup(rgb, r, g, b).asFastLookupOp();
    }

    private BufferedImageOp createOklabOp() {
        ChannelLookup l = getLookupForChannel(OK_L);
        ChannelLookup a = getLookupForChannel(OK_A);
        ChannelLookup b = getLookupForChannel(OK_B);
        return new OklabLevelsFilter(l, a, b);
    }

    public void resetAllAndRun() {
        resetAll();
        settingsChanged();
    }

    public void resetAll() {
        // goes through setColorSpace so that the GUI follows the reset
        setColorSpace(ColorSpace.SRGB, false);

        resetAllChannels();
    }

    private void resetAllChannels() {
        for (ChannelLevelsModel model : getChannelModels()) {
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

    private ChannelLookup getLookupForChannel(Channel channel) {
        return channelModels.get(channel).createLookup();
    }

    public Collection<ChannelLevelsModel> getChannelModels() {
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
    }

    public void randomizeAndRun() {
        resetAllChannels(); // keep the active color space

        Channel primaryChannel = colorSpace.getPrimaryChannel();
        ChannelLevelsModel primaryModel = getModelForChannel(primaryChannel);

        // randomize the input levels of the primary channel for a simple, common adjustment
        int inputDark = Rnd.nextInt(128);
        int inputLight = Rnd.nextInt(128) + 128;

        primaryModel.getInputDark().setValueNoTrigger(inputDark);
        primaryModel.getInputLight().setValueNoTrigger(inputLight);

        // trigger a preview update
        settingsChanged();
    }

    public void setColorSpace(ColorSpace colorSpace, boolean triggerFilter) {
        if (this.colorSpace == colorSpace) {
            return;
        }
        this.colorSpace = colorSpace;

        if (levelsGUI != null) {
            // synchronize the UI dropdown to reflect the
            // data model (necessary when loading a preset)
            levelsGUI.setSelectedColorSpace(colorSpace);
        }

        if (triggerFilter) {
            settingsChanged();
        }
    }

    public ColorSpace getColorSpace() {
        return colorSpace;
    }
}
