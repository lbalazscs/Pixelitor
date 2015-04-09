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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.SparkleFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;

import java.awt.image.BufferedImage;

import static java.awt.Color.WHITE;
import static pixelitor.filters.gui.ColorParam.OpacitySetting.USER_ONLY_OPACITY;

/**
 * Sparkle based on the JHLabs SparkleFilter
 */
public class JHSparkle extends FilterWithParametrizedGUI {
    private final BooleanParam lightOnly = new BooleanParam("Light Only", false);
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final RangeParam nrOfRays = new RangeParam("Number of Rays", 1, 500, 200);
    private final RangeParam radius = new RangeParam("High Intensity Radius", 1, 500, 50);
    private final RangeParam shine = new RangeParam("Shine", 0, 100, 50);
    private final RangeParam randomness = new RangeParam("Randomness", 0, 50, 25);

    private final ColorParam color = new ColorParam("Color", WHITE, USER_ONLY_OPACITY);

    private SparkleFilter filter;

    public JHSparkle() {
        super("Sparkle", true, false);
        setParamSet(new ParamSet(
                center,
                lightOnly,
                color,
                nrOfRays,
                radius.adjustRangeToImageSize(1.0),
                shine,
                randomness
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new SparkleFilter();
        }

        filter.setLightOnly(lightOnly.isChecked());
        filter.setRelativeCentreX(center.getRelativeX());
        filter.setRelativeCentreY(center.getRelativeY());
        filter.setRadius(radius.getValue());
        filter.setRays(nrOfRays.getValue());
        filter.setAmount(shine.getValue());
        filter.setRandomness(randomness.getValue());
        filter.setColor(color.getColor().getRGB());

        dest = filter.filter(src, dest);
        return dest;
    }
}