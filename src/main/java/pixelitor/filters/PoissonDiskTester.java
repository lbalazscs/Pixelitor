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

import pixelitor.colors.Colors;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.PoissonDiskSampling;
import pixelitor.utils.ReseedSupport;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.SplittableRandom;

import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

/**
 * Filter for testing Poisson Disk Sampling
 */
public class PoissonDiskTester extends ParametrizedFilter {
    public static final String NAME = "Poisson Disk Sampling";

    private final RangeParam distance = new RangeParam("Distance", 10, 20, 300);
    private final RangeParam k = new RangeParam("k", 1, 30, 100);
    private final BooleanParam improved = new BooleanParam("Improved", false);
    private final BooleanParam debugGrid = new BooleanParam("Debug Grid", false, IGNORE_RANDOMIZE);

    public PoissonDiskTester() {
        super(false);

        setParams(
            distance,
            k,
            improved,
            debugGrid
        ).withAction(ReseedSupport.createAction());
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        SplittableRandom rand = ReseedSupport.getLastSeedSRandom();

        Graphics2D g2 = dest.createGraphics();
        int width = dest.getWidth();
        int height = dest.getHeight();
        Colors.fillWith(Color.WHITE, g2, width, height);

        var sampling = new PoissonDiskSampling(width, height,
            distance.getValueAsDouble(), k.getValue(), improved.isChecked(), rand);

        g2.setColor(Color.RED);
        sampling.showSamples(g2, 5.0);

        if (debugGrid.isChecked()) {
            g2.setColor(Color.BLACK);
            sampling.showGrid(g2);
        }

        return dest;
    }
}