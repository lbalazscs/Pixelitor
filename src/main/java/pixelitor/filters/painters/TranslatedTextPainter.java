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

import java.awt.Font;
import java.awt.Rectangle;

/**
 * A TextPainter that can have an extra translation (so that text
 * layers can be moved with the move tool)
 */
public class TranslatedTextPainter extends TextPainter {
    private static final long serialVersionUID = -2064757977654857961L;

    private int translationX = 0;
    private int translationY = 0;

    private transient Rectangle lastTextBounds;

    @Override
    protected Rectangle calculateLayout(int contentWidth, int contentHeight, int width, int height) {
        Rectangle rectangle = super.calculateLayout(contentWidth, contentHeight, width, height);
        rectangle.translate(translationX, translationY);
        lastTextBounds = rectangle;
        return rectangle;
    }

    @Override
    protected String calculateText(Object component) {
        return getText();
    }

    @Override
    protected Font calculateFont(Object component) {
        return getFont();
    }

    public void setTranslation(int translationX, int translationY) {
        this.translationX = translationX;
        this.translationY = translationY;
    }

    public int getTX() {
        return translationX;
    }

    public int getTY() {
        return translationY;
    }

    public Rectangle getTextBounds() {
        return lastTextBounds;
    }
}
