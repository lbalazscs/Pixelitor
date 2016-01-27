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

import com.jhlabs.image.CircleFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;

import java.awt.image.BufferedImage;

/**
 * Wrap Around Arc based on the JHLabs CircleFilter
 */
public class JHWrapAroundArc extends FilterWithParametrizedGUI {
    public static final String NAME = "Wrap Around Arc";

    private CircleFilter filter;

    private final RangeParam radius = new RangeParam("Radius", 0, 50, 500);
    private final RangeParam thickness = new RangeParam("Thickness", 0, 150, 500);
    private final AngleParam rotateResult = new AngleParam("Rotate Result", 0);
    private final RangeParam spread = new RangeParam("Divide Angle", 1, 2, 24);

    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final IntChoiceParam edgeAction = IntChoiceParam.getEdgeActionChoices();
    private final IntChoiceParam interpolation = IntChoiceParam.getInterpolationChoices();

    public JHWrapAroundArc() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                radius.adjustRangeToImageSize(1.0),
                thickness.adjustRangeToImageSize(0.5),
                spread,
                rotateResult,
                center,
                edgeAction,
                interpolation
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new CircleFilter(NAME);
        }

        filter.setCentreX(center.getRelativeX());
        filter.setCentreY(center.getRelativeY());
        filter.setRadius(radius.getValueAsFloat());
        filter.setHeight(thickness.getValueAsFloat());
        filter.setAngle((float) rotateResult.getValueInIntuitiveRadians());

        double spreadValue = spread.getValueAsDouble();
        float spreadRadians = (float) (2 * Math.PI / spreadValue);
        filter.setSpreadAngle(spreadRadians);

        filter.setInterpolation(interpolation.getValue());
        filter.setEdgeAction(edgeAction.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }
}