/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
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

import com.jhlabs.image.CircleFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;

import java.awt.image.BufferedImage;

/**
 * Wrap Around Arc based on the JHLabs CircleFilter
 */
public class JHWrapAroundArc extends FilterWithParametrizedGUI {
    private CircleFilter filter;

    private final RangeParam radius = new RangeParam("Radius", 0, 500, 50);
    private final RangeParam thickness = new RangeParam("Thickness", 0, 500, 150);
    private final AngleParam rotateResult = new AngleParam("Rotate Result", 0);
    private final RangeParam spread = new RangeParam("Divide Angle", 1, 24, 2);

    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final IntChoiceParam edgeAction = IntChoiceParam.getEdgeActionChoices();
    private final IntChoiceParam interpolation = IntChoiceParam.getInterpolationChoices();

    public JHWrapAroundArc() {
        super("Wrap Around Arc", true, false);
        setParamSet(new ParamSet(
                radius.adjustRangeAccordingToImage(1.0),
                thickness.adjustRangeAccordingToImage(0.5),
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
            filter = new CircleFilter();
        }

        filter.setCentreX(center.getRelativeX());
        filter.setCentreY(center.getRelativeY());
        filter.setRadius(radius.getValue());
        filter.setHeight(thickness.getValue());
        filter.setAngle((float) rotateResult.getValueInIntuitiveRadians());

        int spreadValue = spread.getValue();
        float spreadRadians = (float) (2 * Math.PI / spreadValue);
        filter.setSpreadAngle(spreadRadians);

        filter.setInterpolation(interpolation.getValue());
        filter.setEdgeAction(edgeAction.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }
}