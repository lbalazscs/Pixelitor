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

import pixelitor.filters.gui.AngleParam;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.image.BufferedImage;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;

public class JHStripedHalftone extends JHMaskedHalftone {
    public static final String NAME = "Striped Halftone";

    private final AngleParam angle = new AngleParam("Angle", 0);

    public JHStripedHalftone() {
        setParams(
                angle,
                stripesDistance,
                repetitionType,
                shiftStripes,
                softness,
                monochrome
        );
    }

    @Override
    protected BufferedImage createMaskImage(BufferedImage src) {
        float[] fractions = {0.0f, 1.0f};
        Color[] colors = {BLACK, WHITE};

        BufferedImage stripes = ImageUtils.createImageWithSameCM(src);
        Graphics2D g = stripes.createGraphics();
        float x1 = src.getWidth() / 2.0f;
        float y1 = src.getHeight() / 2.0f;
        float dist = stripesDistance.getValueAsFloat() / distanceCorrection;
        double angleVal = angle.getValueInRadians() + Math.PI / 2;
        double dx = dist * Math.cos(angleVal);
        float x2 = (float) (x1 + dx);
        double dy = dist * Math.sin(angleVal);
        float y2 = (float) (y1 + dy);
        float shiftPercent = shiftStripes.getPercentageValF() * distanceCorrection;
        double shiftX = shiftPercent * dx;
        double shiftY = shiftPercent * dy;
        x1 += shiftX;
        y1 += shiftY;
        x2 += shiftX;
        y2 += shiftY;

        var paint = new LinearGradientPaint(x1, y1, x2, y2,
                fractions, colors, cycleMethod);

        g.setPaint(paint);
        g.fillRect(0, 0, src.getWidth(), src.getHeight());
        g.dispose();
        return stripes;
    }
}
