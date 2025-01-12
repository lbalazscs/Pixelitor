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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.SparkleFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.RangeParam;

import java.awt.image.BufferedImage;
import java.io.Serial;

import static java.awt.Color.WHITE;
import static pixelitor.filters.gui.TransparencyPolicy.USER_ONLY_TRANSPARENCY;

/**
 * Sparkle filter based on the JHLabs SparkleFilter
 */
public class JHSparkle extends ParametrizedFilter {
    public static final String NAME = "Sparkle";

    @Serial
    private static final long serialVersionUID = -2547285385455761188L;

    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final BooleanParam lightOnly = new BooleanParam("Light Only");
    private final ColorParam color = new ColorParam("Color", WHITE, USER_ONLY_TRANSPARENCY);
    private final RangeParam numRays = new RangeParam("Number of Rays", 1, 200, 501);
    private final RangeParam radius = new RangeParam("High Intensity Radius", 1, 50, 500);
    private final RangeParam shine = new RangeParam("Shine", 0, 50, 100);
    private final RangeParam randomness = new RangeParam("Randomness", 0, 24, 48);

    private SparkleFilter filter;

    public JHSparkle() {
        super(true);

        var reseed = paramSet.createReseedAction("", "Reseed Randomness");
        randomness.setupEnableOtherIfNotZero(reseed);
        setParams(
            center,
            lightOnly,
            color,
            numRays,
            radius.withAdjustedRange(1.0),
            shine,
            randomness.withAction(reseed)
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new SparkleFilter(NAME);
        }

        filter.setLightOnly(lightOnly.isChecked());
        filter.setRelativeCenterX((float) center.getRelativeX());
        filter.setRelativeCenterY((float) center.getRelativeY());
        filter.setRadius(radius.getValue());
        filter.setNumRays(numRays.getValue());
        filter.setAmount(shine.getValue());
        filter.setRandomness(randomness.getValue());
        filter.setColor(color.getColor().getRGB());
        filter.setRandom(paramSet.getLastSeedRandom());

        return filter.filter(src, dest);
    }
}