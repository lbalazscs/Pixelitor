/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ReseedNoiseFilterAction;
import pixelitor.filters.gui.ShowOriginal;

import java.awt.image.BufferedImage;

/**
 * Underwater filter based on JHLabs SwimFilter
 */
public class JHUnderWater extends ParametrizedFilter {
    public static final String NAME = "Underwater";

    private final RangeParam amount = new RangeParam("Amount", 0, 50, 100);
    private final RangeParam scale = new RangeParam("Scale", 1, 150, 300);
    private final RangeParam stretch = new RangeParam("Stretch", 0, 0, 50);
    private final RangeParam time = new RangeParam("Time", 0, 0, 1000);
    private final AngleParam angle = new AngleParam("Angle", 0);
    private final IntChoiceParam edgeAction = IntChoiceParam.forEdgeAction(true);
    private final IntChoiceParam interpolation = IntChoiceParam.forInterpolation();

    private SwimFilter filter;

    public JHUnderWater() {
        super(ShowOriginal.YES);

        ReseedNoiseFilterAction reseed = new ReseedNoiseFilterAction();
        setParams(
                amount.withAdjustedRange(0.1),
                scale.withAdjustedRange(0.3),
                stretch,
                angle,
                time,
                edgeAction,
                interpolation
        ).withAction(reseed);

        amount.setupEnableOtherIfNotZero(reseed);
        stretch.setupEnableOtherIfNotZero(angle);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (amount.isZero()) {
            return src;
        }
        
        if(filter == null) {
            filter = new SwimFilter(NAME);
        }

        filter.setAmount(amount.getValueAsFloat());
        filter.setScale(scale.getValueAsFloat());
        filter.setStretch(stretch.getValueAsFloat() + 1.0f);
        filter.setTime(time.getValueAsPercentage());
        filter.setAngle((float) angle.getValueInRadians());
        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }
}