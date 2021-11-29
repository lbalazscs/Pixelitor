/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
 * Renderer for both the crop composition guides and the normal guides.
 */
public class GuidesRenderer {
    private final GuideStyle style;

    public static final Lazy<GuidesRenderer> CROP_GUIDES_INSTANCE = Lazy.of(
        () -> new GuidesRenderer(AppPreferences.getCropGuideStyle()));
    public static final Lazy<GuidesRenderer> GUIDES_INSTANCE = Lazy.of(
        () -> new GuidesRenderer(AppPreferences.getGuideStyle()));

    public GuidesRenderer(GuideStyle style) {
        this.style = style;
    }

    public void draw(Graphics2D g2, List<? extends Shape> shapes) {
        g2.setStroke(style.getStrokeA());
        g2.setColor(style.getColorA());
        for (Shape shape : shapes) {
            g2.draw(shape);
        }

        if (style.getStrokeB() != null) {
            g2.setStroke(style.getStrokeB());
            g2.setColor(style.getColorB());
            for (Shape shape : shapes) {
                g2.draw(shape);
            }
        }
    }
}
