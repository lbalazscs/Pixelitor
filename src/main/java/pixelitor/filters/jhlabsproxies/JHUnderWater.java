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

import com.jhlabs.image.SwimFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ReseedNoiseActionSetting;
import pixelitor.filters.gui.ShowOriginal;

import java.awt.image.BufferedImage;

/**
 * UnderWater filter based on JHLabs SwimFilter
 */
public class JHUnderWater extends FilterWithParametrizedGUI {
    public static final String NAME = "Underwater";

    private final RangeParam amount = new RangeParam("Amount", 0, 50, 100);
    private final RangeParam scale = new RangeParam("Scale", 1, 150, 300);
    private final RangeParam stretch = new RangeParam("Stretch", 1, 1, 50);
    private final RangeParam time = new RangeParam("Time", 100, 100, 1000);
    private final AngleParam angle = new AngleParam("Angle", 0);
    private final IntChoiceParam edgeAction = IntChoiceParam.getEdgeActionChoices(true);
    private final IntChoiceParam interpolation = IntChoiceParam.getInterpolationChoices();

    private SwimFilter filter;

    public JHUnderWater() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                amount.adjustRangeToImageSize(0.1),
                scale.adjustRangeToImageSize(0.3),
                stretch,
                angle,
                time,
                edgeAction,
                interpolation
        ).withAction(new ReseedNoiseActionSetting()));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if(filter == null) {
            filter = new SwimFilter(NAME);
        }

        filter.setAmount(amount.getValueAsFloat());
        filter.setScale(scale.getValueAsFloat());
        filter.setStretch(stretch.getValueAsFloat());
        filter.setTime(time.getValueAsPercentage());
        filter.setAngle((float) angle.getValueInRadians());
        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }
}