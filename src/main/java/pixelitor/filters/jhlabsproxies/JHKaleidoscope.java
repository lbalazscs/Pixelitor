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

import com.jhlabs.image.KaleidoscopeFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;

import java.awt.image.BufferedImage;

/**
 * A kaleidoscope based on the JHLabs KaleidoscopeFilter
 */
public class JHKaleidoscope extends FilterWithParametrizedGUI {
    public static final String NAME = "Kaleidoscope";

    private final AngleParam angle = new AngleParam("Angle", 0);
    private final AngleParam rotateResult = new AngleParam("Rotate Result", 0);
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final RangeParam sides = new RangeParam("Sides", 0, 3, 10);
//    private final RangeParam radius = new RangeParam("Radius", 0, 999, 0);

    private final RangeParam zoom = new RangeParam("Zoom (%)", 1, 100, 500);
    private final IntChoiceParam edgeAction = IntChoiceParam.getEdgeActionChoices(true);
    private final IntChoiceParam interpolation = IntChoiceParam.getInterpolationChoices();

    private KaleidoscopeFilter filter;

    public JHKaleidoscope() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                center,
                angle,
                sides,
//                radius,
                zoom,
                rotateResult,
                edgeAction,
                interpolation
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new KaleidoscopeFilter(NAME);
        }
        filter.setAngle((float) angle.getValueInRadians());
        filter.setAngle2((float) rotateResult.getValueInRadians());
        filter.setCentreX(center.getRelativeX());
        filter.setCentreY(center.getRelativeY());
//        filter.setRadius(radius.getValue());
        filter.setSides(sides.getValue());
        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());
        filter.setZoom(zoom.getValueAsPercentage());

        dest = filter.filter(src, dest);
        return dest;
    }
}
