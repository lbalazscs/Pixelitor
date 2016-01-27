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

import com.jhlabs.image.MarbleFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ReseedNoiseActionSetting;
import pixelitor.filters.gui.ShowOriginal;

import java.awt.image.BufferedImage;

/**
 * Turbulent Distortion based on the JHLabs MarbleFilter
 */
public class JHTurbulentDistortion extends FilterWithParametrizedGUI {
    public static final String NAME = "Turbulent Distortion";

    private final RangeParam scale = new RangeParam("Size", 2, 20, 100);
    private final RangeParam amount = new RangeParam("Amount", 1, 10, 100);
    private final RangeParam turbulence = new RangeParam("Turbulence", 1, 50, 100);

    private final IntChoiceParam edgeAction = IntChoiceParam.getEdgeActionChoices();
    private final IntChoiceParam interpolation = IntChoiceParam.getInterpolationChoices();

    private MarbleFilter filter;

    public JHTurbulentDistortion() {
        super(ShowOriginal.YES);

        edgeAction.setDefaultChoice(IntChoiceParam.EDGE_REPEAT_PIXELS);
        setParamSet(new ParamSet(
                scale.adjustRangeToImageSize(0.1),
                amount.adjustRangeToImageSize(0.07),
                turbulence,
                edgeAction,
                interpolation
        ).withAction(new ReseedNoiseActionSetting()));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new MarbleFilter(NAME);
        }

        filter.setTurbulence(turbulence.getValueAsPercentage());
        filter.setScale(scale.getValueAsFloat());
        filter.setAmount(amount.getValueAsFloat());
        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }
}
