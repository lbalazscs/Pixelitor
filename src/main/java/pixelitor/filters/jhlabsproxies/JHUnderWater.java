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

import java.awt.image.BufferedImage;

/**
 * UnderWater filter based on JHLabs SwimFilter
 */
public class JHUnderWater extends FilterWithParametrizedGUI {
    private RangeParam amountParam = new RangeParam("Amount", 0, 100, 50);
    private RangeParam scaleParam = new RangeParam("Scale", 1, 300, 50);
    private RangeParam stretchParam = new RangeParam("Stretch", 1, 50, 1);
    private RangeParam turbulenceParam = new RangeParam("Turbulence", 0, 100, 0);
    private RangeParam timeParam = new RangeParam("Time", 100, 1000, 100);
    private AngleParam angleParam = new AngleParam("Angle", 0);
    private IntChoiceParam edgeActionParam =  IntChoiceParam.getEdgeActionChoices(true);
    private IntChoiceParam interpolationParam = IntChoiceParam.getInterpolationChoices();

    private SwimFilter filter;

    public JHUnderWater() {
        super("Underwater", true, false);
        setParamSet(new ParamSet(
                amountParam,
                scaleParam,
                stretchParam,
                angleParam,
                turbulenceParam,
                timeParam,
                edgeActionParam,
                interpolationParam
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if(filter == null) {
            filter = new SwimFilter();
        }

        filter.setAmount(amountParam.getValue());
        filter.setScale(scaleParam.getValue());
        filter.setStretch(stretchParam.getValue());
        filter.setTurbulence(turbulenceParam.getValueAsPercentage());
        filter.setTime(timeParam.getValueAsPercentage());
        filter.setAngle((float) angleParam.getValueInRadians());
        filter.setEdgeAction(edgeActionParam.getValue());
        filter.setInterpolation(interpolationParam.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }
}