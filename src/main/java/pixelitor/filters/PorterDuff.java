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

package pixelitor.filters;

import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

import static java.awt.AlphaComposite.*;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * A development helper filter illustrating the Porter-Duff
 * rules in {@link AlphaComposite}
 * See https://en.wikipedia.org/wiki/Alpha_compositing
 */
public class PorterDuff extends ParametrizedFilter {
    public static final String NAME = "Porter-Duff";
    private static final int MODE_SHAPE = 1;
    private static final int MODE_IMAGE = 2;

    private final IntChoiceParam rule = new IntChoiceParam("Rule", new Item[]{
        new Item("SRC_OVER", SRC_OVER),
        new Item("SRC", SRC),
        new Item("DST_OVER", DST_OVER),
        new Item("DST", DST),
        new Item("SRC_IN", SRC_IN),
        new Item("DST_IN", DST_IN),
        new Item("SRC_ATOP", SRC_ATOP),
        new Item("DST_ATOP", DST_ATOP),
        new Item("SRC_OUT", SRC_OUT),
        new Item("DST_OUT", DST_OUT),
        new Item("CLEAR", CLEAR),
        new Item("XOR", XOR),
    });

    private final IntChoiceParam mode = new IntChoiceParam("Mode", new Item[]{
        new Item("Shape", MODE_SHAPE),
        new Item("Image", MODE_IMAGE),
    });

    private final RangeParam alpha = new RangeParam("Constant Alpha (%)", 0, 100, 100);

    public PorterDuff() {
        super(true);

        setParams(rule, mode, alpha);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        Graphics2D g = dest.createGraphics();
        g.drawImage(src, 0, 0, null);

        g.setComposite(getInstance(rule.getValue(), alpha.getPercentageValF()));

        int width = src.getWidth();
        int height = src.getHeight();

        if (mode.getValue() == MODE_SHAPE) {
            // if it is filled directly with a shape, then it will
            // have any effect only within the shape
            fillWithRedEllipse(g, width, height);
        } else if (mode.getValue() == MODE_IMAGE) {
            BufferedImage img = new BufferedImage(width, height, TYPE_INT_ARGB);
            Graphics2D imgG = img.createGraphics();
            fillWithRedEllipse(imgG, width, height);
            imgG.dispose();
            g.drawImage(img, 0, 0, null);
        } else {
            throw new IllegalStateException();
        }

        g.dispose();
        return dest;
    }

    private static void fillWithRedEllipse(Graphics2D g, int width, int height) {
        g.setColor(Color.RED);
        g.fill(new Ellipse2D.Double(0, 0, width, height));
    }
}

