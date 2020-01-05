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

import com.jhlabs.image.MarbleFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;

import java.awt.image.BufferedImage;

import static pixelitor.filters.gui.IntChoiceParam.EDGE_REPEAT_PIXELS;
import static pixelitor.filters.gui.ReseedActions.reseedNoise;

/**
 * Turbulent Distortion filter based on the JHLabs MarbleFilter
 */
public class JHTurbulentDistortion extends ParametrizedFilter {
    public static final String NAME = "Turbulent Distortion";

    private final RangeParam scale = new RangeParam("Size", 2, 20, 100);
    private final RangeParam amount = new RangeParam("Amount", 0, 10, 100);
    private final RangeParam turbulence = new RangeParam("Turbulence", 0, 50, 100);
    private final RangeParam time = new RangeParam("Time", 0, 0, 100);

    private final IntChoiceParam edgeAction = IntChoiceParam.forEdgeAction();
    private final IntChoiceParam interpolation = IntChoiceParam.forInterpolation();

    private MarbleFilter filter;

    public JHTurbulentDistortion() {
        super(ShowOriginal.YES);

        setParams(
                scale.withAdjustedRange(0.1),
                amount.withAdjustedRange(0.07),
                turbulence,
                time,
                edgeAction.withDefaultChoice(EDGE_REPEAT_PIXELS),
                interpolation
        ).withAction(reseedNoise());
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if(amount.isZero() || turbulence.isZero()) {
            return src;
        }

        if (filter == null) {
            filter = new MarbleFilter(NAME);
        }

        filter.setTurbulence(turbulence.getPercentageValF());
        filter.setScale(scale.getValueAsFloat());
        filter.setAmount(amount.getValueAsFloat());
        filter.setTime(time.getPercentageValF() * 5);
        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }
}
