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

package pixelitor.filters;

import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Value;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

import static java.awt.AlphaComposite.CLEAR;
import static java.awt.AlphaComposite.DST;
import static java.awt.AlphaComposite.DST_ATOP;
import static java.awt.AlphaComposite.DST_IN;
import static java.awt.AlphaComposite.DST_OUT;
import static java.awt.AlphaComposite.DST_OVER;
import static java.awt.AlphaComposite.SRC;
import static java.awt.AlphaComposite.SRC_ATOP;
import static java.awt.AlphaComposite.SRC_IN;
import static java.awt.AlphaComposite.SRC_OUT;
import static java.awt.AlphaComposite.SRC_OVER;
import static java.awt.AlphaComposite.XOR;
import static java.awt.AlphaComposite.getInstance;
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

    private final IntChoiceParam rule = new IntChoiceParam("Rule", new Value[]{
            new Value("SRC_OVER", SRC_OVER),
            new Value("SRC", SRC),
            new Value("DST_OVER", DST_OVER),
            new Value("DST", DST),
            new Value("SRC_IN", SRC_IN),
            new Value("DST_IN", DST_IN),
            new Value("SRC_ATOP", SRC_ATOP),
            new Value("DST_ATOP", DST_ATOP),
            new Value("SRC_OUT", SRC_OUT),
            new Value("DST_OUT", DST_OUT),
            new Value("CLEAR", CLEAR),
            new Value("XOR", XOR),
    });

    private final IntChoiceParam mode = new IntChoiceParam("Mode", new Value[]{
            new Value("Shape", MODE_SHAPE),
            new Value("Image", MODE_IMAGE),
    });

    private final RangeParam alpha = new RangeParam("Constant Alpha (%)", 0, 100, 100);

    public PorterDuff() {
        super(ShowOriginal.YES);

        setParams(rule, mode, alpha);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        Graphics2D g = dest.createGraphics();
        g.drawImage(src, 0, 0, null);

        g.setComposite(getInstance(rule.getValue(), alpha.getPercentageValF()));

        int width = src.getWidth();
        int height = src.getHeight();

        if(mode.getValue() == MODE_SHAPE) {
            // if it is filled directly with a shape, then it will
            // have any effect only within the shape
            fillWithRedEllipse(g, width, height);
        } else if(mode.getValue() == MODE_IMAGE) {
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

