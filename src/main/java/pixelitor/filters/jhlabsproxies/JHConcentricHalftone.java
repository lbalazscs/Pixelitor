/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.jhlabsproxies;

import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.gui.GUIText;
import pixelitor.tools.gradient.paints.SpiralGradientPaint;
import pixelitor.tools.util.ImDrag;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.image.BufferedImage;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;

public class JHConcentricHalftone extends JHMaskedHalftone {
    public static final String NAME = "Concentric Halftone";
    private static final int TYPE_CONCENTRIC = 1;
    private static final int TYPE_SPIRAL_CW = 2;
    private static final int TYPE_SPIRAL_CCW = 3;

    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final IntChoiceParam gradientType = new IntChoiceParam(GUIText.TYPE,
        new Item[]{
            new Item("Concentric", TYPE_CONCENTRIC),
            new Item("Spiral CW", TYPE_SPIRAL_CW),
            new Item("Spiral CCW", TYPE_SPIRAL_CCW),
        });

    public JHConcentricHalftone() {
        setParams(
                center,
                gradientType,
                repetitionType,
                stripesDistance,
                softness,
                invert,
                monochrome
        );
    }

    @Override
    protected BufferedImage createMaskImage(BufferedImage src) {
        BufferedImage stripes = ImageUtils.createImageWithSameCM(src);
        Graphics2D g = stripes.createGraphics();
        float cx = src.getWidth() * center.getRelativeX();
        float cy = src.getHeight() * center.getRelativeY();
        float radius = stripesDistance.getValueAsFloat() / distanceCorrection;
        int type = gradientType.getValue();
        Paint paint;
        if (type == TYPE_CONCENTRIC) {
            float[] fractions = {0.0f, 1.0f};
            Color[] colors = {BLACK, WHITE};
            paint = new RadialGradientPaint(cx, cy,
                    radius, fractions, colors, cycleMethod);
        } else {
            ImDrag imDrag = new ImDrag(cx, cy, cx + 2 * radius, cy);
            Color startColor = BLACK;
            Color endColor = WHITE;
            if (type == TYPE_SPIRAL_CW) {
                paint = new SpiralGradientPaint(true,
                        imDrag, startColor, endColor, cycleMethod);
            } else if (type == TYPE_SPIRAL_CCW) {
                paint = new SpiralGradientPaint(false,
                        imDrag, startColor, endColor, cycleMethod);
            } else {
                throw new IllegalStateException("type = " + type);
            }
        }

        g.setPaint(paint);
        g.fillRect(0, 0, src.getWidth(), src.getHeight());
        g.dispose();
        return stripes;
    }
}
