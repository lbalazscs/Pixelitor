/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.painters;

import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.filters.gui.UserPreset;
import pixelitor.layers.Drawable;
import pixelitor.layers.TextLayer;
import pixelitor.utils.ImageUtils;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * A filter which adds a text to the current image layer.
 * It has the same GUI as a {@link TextLayer}.
 */
public class TextFilter extends FilterWithGUI {
    private TextSettings settings;

    public TextFilter() {
        settings = new TextSettings();
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        String text = settings.getText();
        if (text.isEmpty()) {
            return src;
        }

        var textPainter = new TransformedTextPainter();
        settings.configurePainter(textPainter);

        if (settings.hasWatermark()) {
            dest = settings.watermarkImage(src, textPainter);
        } else {
            int width = dest.getWidth();
            int height = dest.getHeight();

            textPainter.setFillPaint(settings.getColor());

            dest = ImageUtils.copyImage(src);
            Graphics2D g = dest.createGraphics();
            textPainter.paint(g, this, width, height);
            g.dispose();
        }

        return dest;
    }

    @Override
    public FilterGUI createGUI(Drawable dr) {
        return new TextSettingsPanel(this, dr);
    }

    @Override
    public void randomizeSettings() {
        settings.randomize();
    }

    public TextSettings getSettings() {
        return settings;
    }

    public void setSettings(TextSettings settings) {
        this.settings = settings;
    }

    @Override
    public boolean canHaveUserPresets() {
        return true;
    }

    @Override
    public String getPresetDirName() {
        return TextLayer.TEXT_PRESETS_DIR_NAME;
    }

    @Override
    public UserPreset createUserPreset(String presetName) {
        return settings.createUserPreset(presetName);
    }

    @Override
    public void loadStateFrom(UserPreset preset) {
        settings.loadStateFrom(preset);
    }

    @Override
    public boolean supportsGray() {
        return !settings.hasWatermark();
    }
}