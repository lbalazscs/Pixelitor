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

import com.jhlabs.image.SmearFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.Texts;

import java.awt.image.BufferedImage;
import java.io.Serial;

import static pixelitor.gui.GUIText.OPACITY;

/**
 * Smear filter based on the JHLabs SmearFilter
 */
public class JHSmear extends ParametrizedFilter {
    public static final String NAME = Texts.i18n("smear");

    @Serial
    private static final long serialVersionUID = -1276539723041828316L;

    private final RangeParam distance = new RangeParam("Distance", 0, 15, 100);
    private final RangeParam density = new RangeParam("Density (%)", 0, 50, 100);
    private final AngleParam angle = new AngleParam("Angle (only for lines)", 0);
    private final RangeParam mix = new RangeParam(OPACITY, 0, 50, 100);

    private final IntChoiceParam shape = new IntChoiceParam("Shape", new Item[]{
        new Item("Lines", SmearFilter.LINES),
        new Item("Crosses", SmearFilter.CROSSES),
        new Item("Circles", SmearFilter.CIRCLES),
        new Item("Squares", SmearFilter.SQUARES),
        new Item("Diamonds", SmearFilter.DIAMONDS),
    });

    private SmearFilter filter;

    public JHSmear() {
        super(true);

        mix.setPresetKey("Opacity (%)");

        initParams(
            distance.withAdjustedRange(0.1),
            shape.withAction(paramSet.createReseedAction()),
            angle,
            density,
            mix
        );

        // disable the angle if the shape isn't "lines"
        shape.setupDisableOtherIf(angle,
            selected -> selected.valueIsNot(SmearFilter.LINES));
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int distanceValue = distance.getValue();
        if (distanceValue == 0) {
            return src;
        }

        if (filter == null) {
            filter = new SmearFilter(NAME);
        }

        filter.setDistance(distanceValue);
        filter.setDensity((float) density.getPercentage());
        filter.setAngle((float) angle.getValueInRadians());
        filter.setMix((float) mix.getPercentage());
        filter.setShape(shape.getValue());
        filter.setRandom(paramSet.getLastSeedRandom());

        return filter.filter(src, dest);
    }
}