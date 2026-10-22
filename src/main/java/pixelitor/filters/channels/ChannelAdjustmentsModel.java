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

package pixelitor.filters.channels;

import pixelitor.filters.gui.UserPreset;
import pixelitor.filters.lookup.LookupOps;
import pixelitor.filters.util.Channel;
import pixelitor.filters.util.ColorSpace;

import java.awt.image.BufferedImageOp;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Central, view-free data model for channel-based adjustments across color spaces.
 *
 * @param <T> the channel adjustment type
 */
public class ChannelAdjustmentsModel<T extends ChannelAdjustment> {
    /**
     * Minimal view interface for updating the GUI shell.
     */
    @FunctionalInterface
    public interface View {
        /**
         * Updates the GUI to reflect the current model state.
         */
        void updateGUI();
    }

    private final Map<Channel, T> adjustments = new EnumMap<>(Channel.class);
    private ColorSpace colorSpace = ColorSpace.SRGB;
    private Channel activeChannel = ColorSpace.SRGB.getPrimaryChannel();
    private Runnable adjustmentListener;
    private View view;

    /**
     * Creates a new model using the provided factory to instantiate adjustments.
     *
     * @param factory creates an adjustment for a channel with the given change listener
     */
    public ChannelAdjustmentsModel(BiFunction<Channel, Runnable, T> factory) {
        Objects.requireNonNull(factory, "factory must not be null");
        for (Channel channel : Channel.values()) {
            adjustments.put(channel, factory.apply(channel, this::fireAdjustmentChanged));
        }
    }

    public ColorSpace getColorSpace() {
        return colorSpace;
    }

    public Channel getActiveChannel() {
        return activeChannel;
    }

    public T getAdjustment(Channel channel) {
        return adjustments.get(channel);
    }

    public T getActiveAdjustment() {
        return adjustments.get(activeChannel);
    }

    public Collection<T> getAllAdjustments() {
        return adjustments.values();
    }

    public void setAdjustmentListener(Runnable adjustmentListener) {
        this.adjustmentListener = adjustmentListener;
    }

    public void setGUI(View view) {
        this.view = view;
    }

    /**
     * Sets the active color space and selects its primary channel without notifying.
     *
     * @return true if the color space changed
     */
    public boolean setColorSpace(ColorSpace newColorSpace) {
        if (newColorSpace == null || this.colorSpace == newColorSpace) {
            return false;
        }
        this.colorSpace = newColorSpace;
        this.activeChannel = newColorSpace.getPrimaryChannel();
        return true;
    }

    /**
     * Sets the active channel without notifying.
     * Ignores null and channels that do not belong to the current color space.
     *
     * @return true if the active channel changed
     */
    public boolean setActiveChannel(Channel newActiveChannel) {
        if (newActiveChannel == null
            || newActiveChannel.getColorSpace() != colorSpace
            || activeChannel == newActiveChannel) {
            return false;
        }
        activeChannel = newActiveChannel;
        return true;
    }

    /**
     * Resets the active adjustment to its identity state without notifying.
     */
    public void resetActiveAdjustment() {
        getActiveAdjustment().reset();
    }

    /**
     * Notifies the adjustment listener if present.
     */
    public void fireAdjustmentChanged() {
        if (adjustmentListener != null) {
            adjustmentListener.run();
        }
    }

    private void notifyWholeStateChanged() {
        if (view != null) {
            view.updateGUI();
        }
        fireAdjustmentChanged();
    }

    /**
     * Resets every adjustment of every color space and selects sRGB and its primary channel.
     */
    public void resetAll() {
        for (T adjustment : adjustments.values()) {
            adjustment.reset();
        }
        colorSpace = ColorSpace.SRGB;
        activeChannel = colorSpace.getPrimaryChannel();
        notifyWholeStateChanged();
    }

    /**
     * Resets every adjustment of every color space, preserves the active color space
     * and channel selection, and randomizes the active adjustment.
     */
    public void randomize() {
        for (T adjustment : adjustments.values()) {
            adjustment.reset();
        }
        getActiveAdjustment().randomize();
        notifyWholeStateChanged();
    }

    /**
     * Saves the active color space and its channel adjustments to the preset.
     */
    public void saveStateTo(UserPreset preset) {
        Objects.requireNonNull(preset, "preset must not be null");
        preset.put(ColorSpace.PRESET_KEY, colorSpace.name());
        for (Channel channel : colorSpace.getChannels()) {
            adjustments.get(channel).saveTo(preset);
        }
    }

    /**
     * Restores state from the preset. Resets all adjustments, restores the color space
     * (defaulting to sRGB if absent), selects its primary channel, and loads its adjustments.
     */
    public void loadStateFrom(UserPreset preset) {
        Objects.requireNonNull(preset, "preset must not be null");
        for (T adjustment : adjustments.values()) {
            adjustment.reset();
        }
        colorSpace = preset.getEnum(ColorSpace.PRESET_KEY, ColorSpace.class);
        activeChannel = colorSpace.getPrimaryChannel();
        for (Channel channel : colorSpace.getChannels()) {
            adjustments.get(channel).loadFrom(preset);
        }
        notifyWholeStateChanged();
    }

    /**
     * Creates a {@link BufferedImageOp} applying the active color space's adjustments.
     */
    public BufferedImageOp createOp() {
        return LookupOps.create(colorSpace, ch -> adjustments.get(ch).toLookup());
    }
}
