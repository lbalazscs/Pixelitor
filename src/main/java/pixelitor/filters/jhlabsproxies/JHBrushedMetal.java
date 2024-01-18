/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.BrushedMetalFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.RangeParam;

import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.SplittableRandom;

import static java.awt.Color.GRAY;
import static pixelitor.filters.gui.TransparencyPolicy.NO_TRANSPARENCY;

/**
 * Brushed Metal filter based on the JHLabs BrushedMetalFilter
 */
public class JHBrushedMetal extends ParametrizedFilter {
    public static final String NAME = "Brushed Metal";

    @Serial
    private static final long serialVersionUID = 5028460952633659329L;

    private final ColorParam color = new ColorParam("Color", GRAY, NO_TRANSPARENCY);
    private final RangeParam radius = new RangeParam("Length", 0, 100, 500);
    private final RangeParam amount = new RangeParam("Amount (%)", 0, 50, 100);
    private final RangeParam shine = new RangeParam("Shine (%)", 0, 10, 100);

    public JHBrushedMetal() {
        super(false);

        setParams(
            color,
            radius.withAdjustedRange(0.5),
            amount,
            shine
        ).withReseedAction();
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        SplittableRandom rand = paramSet.getLastSeedSRandom();

        var filter = new BrushedMetalFilter(color.getColor().getRGB(),
            radius.getValue(),
            (float) amount.getPercentage(),
            true,
            (float) shine.getPercentage(),
            NAME);

        filter.setRandom(rand);

        return filter.filter(src, dest);
    }
}