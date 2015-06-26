/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
import pixelitor.filters.gui.AdjustPanel;
import pixelitor.filters.gui.FilterWithGUI;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Adds a centered text to the current layer
 */
public class TextFilter extends FilterWithGUI {
    private TextSettings settings;
    public static final TextFilter INSTANCE = new TextFilter();

    private TextFilter() {
        super("Text");
        copySrcToDstBeforeRunning = true;
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

            Graphics2D g = dest.createGraphics();
            textPainter.paint(g, this, width, height);
            g.dispose();
        }

        return dest;
    }

    @Override
    public AdjustPanel createAdjustPanel() {
        return new TextAdjustmentsPanel(this);
    }

    @Override
    public void randomizeSettings() {
        settings.randomizeText();
    }

    public void setSettings(TextSettings settings) {
        this.settings = settings;
    }
}