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

import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.filters.gui.UserPreset;
import pixelitor.filters.lookup.LookupOps;
import pixelitor.layers.Filterable;

import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.Objects;

/**
 * Abstract base filter for channel-based adjustments across color spaces.
 *
 * @param <M> the channel adjustments model type
 */
public abstract class ChannelAdjustmentFilter<M extends ChannelAdjustmentsModel<?>> extends FilterWithGUI {
    @Serial
    private static final long serialVersionUID = 1L;

    private final M model;

    /**
     * Creates a filter backed by the given adjustments model.
     *
     * @param model the data model
     */
    protected ChannelAdjustmentFilter(M model) {
        this.model = Objects.requireNonNull(model, "model must not be null");
    }

    /**
     * Returns the backing model.
     */
    public M getModel() {
        return model;
    }

    @Override
    public final FilterGUI createGUI(Filterable layer, boolean resetSettings) {
        if (resetSettings) {
            model.resetAll();
        }
        return createChannelGUI(layer);
    }

    /**
     * Creates the filter configuration GUI.
     *
     * @param layer the layer being filtered
     * @return the GUI component
     */
    protected abstract FilterGUI createChannelGUI(Filterable layer);

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        return model.createOp().filter(src, dest);
    }

    @Override
    public void randomize() {
        model.randomize();
    }

    @Override
    public boolean supportsGray() {
        return LookupOps.supportsGray();
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        model.saveStateTo(preset);
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        model.loadStateFrom(preset);
    }
}
