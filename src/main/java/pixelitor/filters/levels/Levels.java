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

import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.filters.gui.UserPreset;
import pixelitor.filters.levels.gui.LevelsGUI;
import pixelitor.filters.lookup.FastLookupOp;
import pixelitor.layers.Drawable;
import pixelitor.utils.Rnd;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ShortLookupTable;
import java.util.Objects;

import static pixelitor.utils.Texts.i18n;

/**
 * The Levels filter
 */
public class Levels extends FilterWithGUI {
    public static final String NAME = i18n("levels");
    private RGBLookup rgbLookup;
    private final LevelsModel levelsModel;

    public Levels() {
        levelsModel = new LevelsModel(this);
    }

    @Override
    public FilterGUI createGUI(Drawable dr, boolean reset) {
        LevelsGUI gui = new LevelsGUI(this, dr, levelsModel);
        if (reset) {
            levelsModel.resetAllToDefault();
        } else {
            levelsModel.settingsChanged();
        }
        return gui;
    }

    public void setRGBLookup(RGBLookup rgbLookup) {
        this.rgbLookup = Objects.requireNonNull(rgbLookup);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (rgbLookup == null) {
            throw new IllegalStateException("rgbLookup not initialized");
        }

        ShortLookupTable lut = (ShortLookupTable) rgbLookup.getLookupOp();
        BufferedImageOp filterOp = new FastLookupOp(lut);
        dest = filterOp.filter(src, dest);

        return dest;
    }

    @Override
    public void randomizeSettings() {
        int inputDark = Rnd.nextInt(255);
        int inputLight = Rnd.nextInt(255);
        int outputDark = Rnd.nextInt(255);
        int outputLight = Rnd.nextInt(255);
        var g = new GrayScaleLookup(inputDark, inputLight, outputDark, outputLight);
        rgbLookup = new RGBLookup(g, g, g, g);
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