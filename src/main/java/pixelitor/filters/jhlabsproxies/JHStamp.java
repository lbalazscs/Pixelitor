/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.StampFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;

import java.awt.image.BufferedImage;
import java.io.Serial;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static pixelitor.filters.gui.RandomizeMode.IGNORE_RANDOMIZE;
import static pixelitor.filters.gui.TransparencyMode.MANUAL_ALPHA_ONLY;

/**
 * Stamp filter based on the JHLabs {@link StampFilter}.
 */
public class JHStamp extends ParametrizedFilter {
    public static final String NAME = "Stamp";

    @Serial
    private static final long serialVersionUID = -3253680298480520434L;

    private final RangeParam lightDarkBalance = new RangeParam("Light/Dark Balance (%)", 0, 50, 100);
    private final RangeParam smoothness = new RangeParam("Smoothness", 0, 25, 50);
    private final RangeParam soften = new RangeParam("Soften", 0, 3, 100);
    private final ColorParam darkColor = new ColorParam("Dark Color", BLACK, MANUAL_ALPHA_ONLY);
    private final ColorParam lightColor = new ColorParam("Light Color", WHITE, MANUAL_ALPHA_ONLY);

    private final IntChoiceParam blurMethod = new IntChoiceParam("Blur Method",
        new Item[]{
            new Item("Fast", StampFilter.BOX3_BLUR),
            new Item("Gaussian (slow for large images!)", StampFilter.GAUSSIAN_BLUR)
        }, IGNORE_RANDOMIZE);

    public JHStamp() {
        super(true);

        // for compatibility with 4.3.1 and earlier
        lightColor.setPresetKey("Bright Color");

        initParams(
            lightDarkBalance,
            smoothness.withAdjustedRange(0.05),
            soften,
            lightColor,
            darkColor,
            blurMethod
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        StampFilter filter = new StampFilter(NAME,
            smoothness.getValueAsFloat(),     // blur radius
            lightDarkBalance.getPercentage(), // threshold
            soften.getPercentage(),           // softness
            lightColor.getColor().getRGB(),
            darkColor.getColor().getRGB(),
            blurMethod.getValue());

        return filter.filter(src, dest);
    }
}
