/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jdesktop.swingx.painter;

import java.awt.Font;
import java.awt.Paint;
import java.awt.Rectangle;

/**
 *
 */
public class TranslatedTextPainter extends TextPainter {
    private int translationX = 0;
    private int translationY = 0;

    public TranslatedTextPainter(String text, Font font, Paint paint) {
        super(text, font, paint);
    }

    @Override
    protected Rectangle calculateLayout(int contentWidth, int contentHeight, int width, int height) {
        Rectangle rectangle = super.calculateLayout(contentWidth, contentHeight, width, height);
        rectangle.translate(translationX, translationY);
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

    public void setTranslationX(int translationX) {
        this.translationX = translationX;
    }

    public void setTranslationY(int translationY) {
        this.translationY = translationY;
    }
}
