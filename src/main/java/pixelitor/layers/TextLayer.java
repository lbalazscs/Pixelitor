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

package pixelitor.layers;

import pixelitor.Composition;
import pixelitor.filters.painters.TranslatedTextPainter;
import pixelitor.history.TranslateEdit;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;

import static java.awt.Color.WHITE;

/**
 * A text layer
 */
public class TextLayer extends ShapeLayer {
    private static final long serialVersionUID = 2L;

    private final String text;
    private final int startX = 20;
    private final int startY = 70;

    private final Font font = new Font("Comic Sans MS", Font.BOLD, 42);

    private final TranslatedTextPainter painter;

    public TextLayer(Composition comp, String name, String text) {
        super(comp, name);
        this.text = text;

        Paint paint = WHITE;

        painter = new TranslatedTextPainter(text, font, paint);
    }

    @Override
    public Shape getShape(Graphics2D g) {
        FontRenderContext frc = g.getFontRenderContext();
        TextLayout textLayout = new TextLayout(text, font, frc);
        return textLayout.getOutline(null);
    }

    @Override
    public Layer duplicate() {
        return new TextLayer(comp, getDuplicateLayerName(), text);
    }

    @Override
    public void moveWhileDragging(int x, int y) {
        super.moveWhileDragging(x, y);
        painter.setTranslationX(getTranslationX());
        painter.setTranslationY(getTranslationY());
    }

    @Override
    public void paintLayerOnGraphics(Graphics2D g, boolean firstVisibleLayer) {
        setupDrawingComposite(g, firstVisibleLayer);
        painter.paint(g, null, comp.getCanvasWidth(), comp.getCanvasHeight());

//      g.setPaint(paint);
//      g.setFont(font);
//      g.drawString(text, startX + getTranslationX(), startY + getTranslationY());
    }

    @Override
    TranslateEdit createTranslateEdit(int oldTranslationX, int oldTranslationY) {
        return new TranslateEdit(this, null, oldTranslationX, oldTranslationY);
    }
}
