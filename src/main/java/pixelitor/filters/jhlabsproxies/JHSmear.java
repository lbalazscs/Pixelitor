/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.utils.ReseedSupport;
import pixelitor.utils.Utils;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Smear based on the JHLabs SmearFilter
 */
public class JHSmear extends FilterWithParametrizedGUI {
    public static final String NAME = "Smear";

    private final RangeParam distance = new RangeParam("Distance", 0, 15, 100);
    private final RangeParam density = new RangeParam("Density (%)", 0, 50, 100);
    private final AngleParam angle = new AngleParam("Angle (only for lines)", 0);
    private final RangeParam mix = new RangeParam("Opacity (%)", 0, 50, 100);

    private static final IntChoiceParam.Value[] shapeChoices = {
            new IntChoiceParam.Value("Lines", SmearFilter.LINES),
            new IntChoiceParam.Value("Crosses", SmearFilter.CROSSES),
            new IntChoiceParam.Value("Circles", SmearFilter.CIRCLES),
            new IntChoiceParam.Value("Squares", SmearFilter.SQUARES),
    };
    private final IntChoiceParam shape = new IntChoiceParam("Shape", shapeChoices);

    private SmearFilter filter;

    public JHSmear() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                distance.adjustRangeToImageSize(0.1),
                shape.withAction(ReseedSupport.createAction()),
                density,
                angle,
                mix
        ));

        // disable angle if the shape is not lines
        Utils.setupDisableOtherIf(shape, angle,
                selected -> selected.getIntValue() != SmearFilter.LINES);

    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int distanceValue = distance.getValue();
        if (distanceValue == 0) {
            return src;
        }

        ReseedSupport.reInitialize();
        Random rand = ReseedSupport.getRand();

        if (filter == null) {
            filter = new SmearFilter(NAME);
        }

        filter.setDistance(distanceValue);
        filter.setDensity(density.getValueAsPercentage());
        filter.setAngle((float) angle.getValueInRadians());
        filter.setMix(mix.getValueAsPercentage());
        filter.setShape(shape.getValue());
        filter.setRandomGenerator(rand);

        dest = filter.filter(src, dest);
        return dest;
    }
}