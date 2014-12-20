/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.SwimFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ReseedNoiseActionParam;

import java.awt.image.BufferedImage;

/**
 * UnderWater filter based on JHLabs SwimFilter
 */
public class JHUnderWater extends FilterWithParametrizedGUI {
    private RangeParam amount = new RangeParam("Amount", 0, 100, 50);
    private RangeParam scale = new RangeParam("Scale", 1, 300, 150);
    private RangeParam stretch = new RangeParam("Stretch", 1, 50, 1);
//    private RangeParam turbulenceParam = new RangeParam("Turbulence", 0, 100, 0);
private RangeParam time = new RangeParam("Time", 100, 1000, 100);
    private AngleParam angle = new AngleParam("Angle", 0);
    private IntChoiceParam edgeAction = IntChoiceParam.getEdgeActionChoices(true);
    private IntChoiceParam interpolation = IntChoiceParam.getInterpolationChoices();

    private SwimFilter filter;

    public JHUnderWater() {
        super("Underwater", true, false);
        setParamSet(new ParamSet(
                amount.adjustRangeToImageSize(0.1),
                scale.adjustRangeToImageSize(0.3),
                stretch,
                angle,
//                turbulenceParam,
                time,
                edgeAction,
                interpolation,
                new ReseedNoiseActionParam()
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if(filter == null) {
            filter = new SwimFilter();
        }

        filter.setAmount(amount.getValueAsFloat());
        filter.setScale(scale.getValueAsFloat());
        filter.setStretch(stretch.getValueAsFloat());
//        filter.setTurbulence(turbulenceParam.getValueAsPercentage());
        filter.setTime(time.getValueAsPercentage());
        filter.setAngle((float) angle.getValueInRadians());
        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }
}