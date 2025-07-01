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
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.filters.gui.UserPreset;
import pixelitor.filters.lookup.RGBLookup;
import pixelitor.layers.Filterable;
import pixelitor.utils.Rnd;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.Serial;
import java.util.Objects;

import static pixelitor.utils.Texts.i18n;

/**
 * The Levels filter, adjusting the tonal range of an image.
 */
public class Levels extends FilterWithGUI {
    public static final String NAME = i18n("levels");

    @Serial
    private static final long serialVersionUID = 1780232770405846617L;

    private RGBLookup rgbLookup;
    private final LevelsModel levelsModel;

    public Levels() {
        levelsModel = new LevelsModel(this);
    }

    @Override
    public FilterGUI createGUI(Filterable layer, boolean reset) {
        LevelsGUI gui = new LevelsGUI(this, layer, levelsModel);
        if (reset) {
            levelsModel.resetAll();
        }
        // ensure the lookup is initialized for the preview
        levelsModel.updateFilterLookup();
        return gui;
    }

    public void setRGBLookup(RGBLookup rgbLookup) {
        this.rgbLookup = Objects.requireNonNull(rgbLookup);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (rgbLookup == null) {
            // this can happen if transform is called before the GUI is created or a preset is loaded
            levelsModel.updateFilterLookup();
        }

        BufferedImageOp filterOp = rgbLookup.asFastLookupOp();
        dest = filterOp.filter(src, dest);

        return dest;
    }

    @Override
    public void randomize() {
        levelsModel.resetAll();

        // randomize the input levels of the main RGB channel for a simple, common adjustment
        int inputDark = Rnd.nextInt(128);
        int inputLight = Rnd.nextInt(128) + 128;

        ChannelLevelsModel rgbModel = levelsModel.getRgbModel();
        rgbModel.getInputDark().setValueNoTrigger(inputDark);
        rgbModel.getInputLight().setValueNoTrigger(inputLight);

        // manually trigger an update of the model and filter
        rgbModel.paramAdjusted();
    }

    @Override
    public boolean supportsGray() {
        return false;
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        levelsModel.saveToUserPreset(preset);
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        levelsModel.loadUserPreset(preset);
    }
}