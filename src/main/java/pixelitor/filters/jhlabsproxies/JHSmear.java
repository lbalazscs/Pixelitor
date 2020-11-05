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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.SmearFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.utils.ReseedSupport;

import java.awt.image.BufferedImage;
import java.util.Random;

import static pixelitor.gui.GUIText.OPACITY;

/**
 * Smear filter based on the JHLabs SmearFilter
 */
public class JHSmear extends ParametrizedFilter {
    public static final String NAME = "Smear";

    private final RangeParam distance = new RangeParam("Distance", 0, 15, 100);
    private final RangeParam density = new RangeParam("Density (%)", 0, 50, 100);
    private final AngleParam angle = new AngleParam("Angle (only for lines)", 0);
    private final RangeParam mix = new RangeParam(OPACITY, 0, 50, 100);

    private static final Item[] shapeChoices = {
        new Item("Lines", SmearFilter.LINES),
        new Item("Crosses", SmearFilter.CROSSES),
        new Item("Circles", SmearFilter.CIRCLES),
        new Item("Squares", SmearFilter.SQUARES),
        new Item("Diamonds", SmearFilter.DIAMONDS),
    };
    private final IntChoiceParam shape = new IntChoiceParam("Shape", shapeChoices);

    private SmearFilter filter;

    public JHSmear() {
        super(ShowOriginal.YES);

        setParams(
            distance.withAdjustedRange(0.1),
            shape.withAction(ReseedSupport.createAction()),
            angle,
            density,
            mix
        );

        // disable angle if the shape is not lines
        shape.setupDisableOtherIf(angle,
                selected -> selected.getValue() != SmearFilter.LINES);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int distanceValue = distance.getValue();
        if (distanceValue == 0) {
            return src;
        }

        Random rand = ReseedSupport.reInitialize();

        if (filter == null) {
            filter = new SmearFilter(NAME);
        }

        filter.setDistance(distanceValue);
        filter.setDensity(density.getPercentageValF());
        filter.setAngle((float) angle.getValueInRadians());
        filter.setMix(mix.getPercentageValF());
        filter.setShape(shape.getValue());
        filter.setRandomGenerator(rand);

        dest = filter.filter(src, dest);
        return dest;
    }
}