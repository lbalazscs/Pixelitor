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

package pixelitor.guides;

import pixelitor.utils.AppPreferences;
import pixelitor.utils.Lazy;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.List;

/**
 * Renderer for guide lines (crop, guides).
 */
public class GuidesRenderer {
    private final GuideStyle guideStyle;

    public static final Lazy<GuidesRenderer> CROP_GUIDES_INSTANCE = Lazy.of(
            () -> new GuidesRenderer(AppPreferences.getCropGuideStyle()));
    public static final Lazy<GuidesRenderer> GUIDES_INSTANCE = Lazy.of(
            () -> new GuidesRenderer(AppPreferences.getGuideStyle()));

    public GuidesRenderer(GuideStyle guideStyle) {
        this.guideStyle = guideStyle;
    }

    public void draw(Graphics2D g2, List<? extends Shape> shapes) {
        g2.setStroke(guideStyle.getStrokeA());
        g2.setColor(guideStyle.getColorA());
        for (Shape shape : shapes) {
            g2.draw(shape);
        }

        if (guideStyle.getStrokeB() != null) {
            g2.setStroke(guideStyle.getStrokeB());
            g2.setColor(guideStyle.getColorB());
            for (Shape shape : shapes) {
                g2.draw(shape);
            }
        }
    }
}
