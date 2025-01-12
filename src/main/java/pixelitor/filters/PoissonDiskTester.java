/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.colors.Colors;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ColorListParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.PoissonDiskSampling;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.SplittableRandom;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

/**
 * Filter for testing Poisson Disk Sampling
 */
public class PoissonDiskTester extends ParametrizedFilter {
    public static final String NAME = "Poisson Disk Sampling";

    @Serial
    private static final long serialVersionUID = -6684473485597318552L;

    private final RangeParam distance = new RangeParam("Distance", 10, 20, 300);
    private final RangeParam k = new RangeParam("k", 1, 30, 100);
    private final BooleanParam improved = new BooleanParam("Improved");
    private final BooleanParam debugGrid = new BooleanParam("Debug Grid", false, IGNORE_RANDOMIZE);

    private final ColorListParam colors = new ColorListParam("Colors",
        1, 1, Colors.CW_RED, Colors.CW_GREEN, Colors.CW_BLUE,
        Colors.CW_ORANGE, Colors.CW_TEAL, Colors.CW_VIOLET, Colors.CW_YELLOW);

    public PoissonDiskTester() {
        super(false);

        setParams(
            distance,
            k,
            improved,
            debugGrid,
            colors
        ).withReseedAction();
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        SplittableRandom rand = paramSet.getLastSeedSRandom();

        Graphics2D g2 = dest.createGraphics();
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        int width = dest.getWidth();
        int height = dest.getHeight();
        Colors.fillWith(Color.WHITE, g2, width, height);

        double dist = distance.getValueAsDouble();
        var sampling = new PoissonDiskSampling(width, height,
            dist, k.getValue(), improved.isChecked(), rand);

        Color[] dotColors = colors.getColors();
        g2.setColor(Color.RED);
        sampling.renderPoints(g2, dist / 2, dotColors);

        if (debugGrid.isChecked()) {
            g2.setColor(Color.BLACK);
            sampling.renderGrid(g2);
        }

        return dest;
    }
}