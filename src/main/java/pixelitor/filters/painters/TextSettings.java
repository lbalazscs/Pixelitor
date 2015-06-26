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

import org.jdesktop.swingx.painter.AbstractLayoutPainter;
import org.jdesktop.swingx.painter.effects.AreaEffect;

import java.awt.Color;
import java.awt.Font;
import java.util.Random;

import static java.awt.Color.BLACK;
import static java.awt.Font.BOLD;
import static java.awt.Font.SANS_SERIF;

/**
 * Text settings for the text filter and text layers
 */
public class TextSettings {
    private String text = "Pixelitor";
    private Font font = new Font(SANS_SERIF, BOLD, 50);
    private AreaEffect[] areaEffects;
    private Color color = BLACK;
    private AbstractLayoutPainter.VerticalAlignment verticalAlignment;
    private AbstractLayoutPainter.HorizontalAlignment horizontalAlignment;
    private boolean watermark;

    public TextSettings(String text, Font font, Color color, AreaEffect[] areaEffects, AbstractLayoutPainter.HorizontalAlignment horizontalAlignment, AbstractLayoutPainter.VerticalAlignment verticalAlignment, boolean watermark) {
        this.areaEffects = areaEffects;
        this.color = color;
        this.font = font;
        this.horizontalAlignment = horizontalAlignment;
        this.text = text;
        this.verticalAlignment = verticalAlignment;
        this.watermark = watermark;
    }

    public AreaEffect[] getAreaEffects() {
        return areaEffects;
    }

    public Color getColor() {
        return color;
    }

    public Font getFont() {
        return font;
    }

    public AbstractLayoutPainter.HorizontalAlignment getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public String getText() {
        return text;
    }

    public AbstractLayoutPainter.VerticalAlignment getVerticalAlignment() {
        return verticalAlignment;
    }

    public boolean isWatermark() {
        return watermark;
    }

    public void randomizeText() {
        Random random = new Random();
        text = Long.toHexString(random.nextLong());
    }
}
