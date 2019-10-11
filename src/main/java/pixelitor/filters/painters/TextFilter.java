/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.FilterAction;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.layers.Drawable;
import pixelitor.utils.ImageUtils;

import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Adds a centered text to the current layer
 */
public class TextFilter extends FilterWithGUI {
    private TextSettings settings;
    private static TextFilter instance;

    private TextFilter() {
    }

    @SuppressWarnings("NonThreadSafeLazyInitialization")
    public static TextFilter getInstance() {
        assert EventQueue.isDispatchThread() : "not EDT thread";

        if (instance == null) {
            instance = new TextFilter();
        }

        return instance;
    }

    public static FilterAction createFilterAction() {
        return new FilterAction("Text", TextFilter::getInstance);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        String text = settings.getText();
        if (text.isEmpty()) {
            return src;
        }

        TranslatedTextPainter textPainter = new TranslatedTextPainter();
        settings.configurePainter(textPainter);

        if (settings.isWatermark()) {
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
        // don't do null check: it is an error if the settings
        // is null now, and *should* throw an exception
        settings.randomize();
    }

    public void setSettings(TextSettings settings) {
        this.settings = settings;
    }
}