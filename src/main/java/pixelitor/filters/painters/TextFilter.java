/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import org.jdesktop.swingx.painter.TextPainter;
import pixelitor.filters.FilterAction;
import pixelitor.filters.gui.AdjustPanel;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.layers.ImageLayer;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
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

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (settings.getText().isEmpty()) {
            return src;
        }

        TextPainter textPainter = new TextPainter();
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
    public AdjustPanel createAdjustPanel(ImageLayer layer) {
        return new TextAdjustmentsPanel(this, layer);
    }

    @Override
    public void randomizeSettings() {
        if(settings != null) {
            settings.randomizeText();
        }
    }

    public void setSettings(TextSettings settings) {
        this.settings = settings;
    }

    @SuppressWarnings("NonThreadSafeLazyInitialization")
    public static TextFilter getInstance() {
        assert SwingUtilities.isEventDispatchThread();

        if (instance == null) {
            System.out.println("TextFilter::getInstance: CREATING");
            instance = new TextFilter();
        }

        return instance;
    }

    public static FilterAction createFilterAction() {
        FilterAction fa = new FilterAction("Text", TextFilter::getInstance);
        return fa;
    }
}