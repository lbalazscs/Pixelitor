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

import com.jhlabs.image.PinchFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;

import java.awt.image.BufferedImage;

/**
 * Swirl-Pinch-Bulge filter
 */
public class SwirlPinchBulge extends FilterWithParametrizedGUI {
    public static final String NAME = "Swirl, Pinch, Bulge";

    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final RangeParam radius = new RangeParam("Radius", 1, 500, 999);
    private final RangeParam swirlAmount = new RangeParam("Swirl Amount", -360, 90, 360);
    private final RangeParam pinchBulgeAmount = new RangeParam("Pinch-Bulge Amount", -100, 0, 100);
    private final RangeParam zoom = new RangeParam("Zoom (%)", 1, 100, 500);
    private final AngleParam rotateResult = new AngleParam("Rotate Result", 0);

    private final IntChoiceParam edgeAction = IntChoiceParam.getEdgeActionChoices();
    private final IntChoiceParam interpolation = IntChoiceParam.getInterpolationChoices();

    private PinchFilter filter;

    public SwirlPinchBulge() {
        super(ShowOriginal.YES);
        showAffectedArea();

        setParamSet(new ParamSet(
                swirlAmount,
                pinchBulgeAmount,
                radius.adjustRangeToImageSize(1.0),
                center,
                zoom,
                rotateResult,
                edgeAction,
                interpolation
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new PinchFilter();
        }

        filter.setPinchBulgeAmount(pinchBulgeAmount.getValueAsPercentage());
        filter.setSwirlAmount(swirlAmount.getValueInRadians());
        filter.setRadius(radius.getValueAsFloat());
        filter.setCenterX(center.getRelativeX());
        filter.setCenterY(center.getRelativeY());

        filter.setZoom(zoom.getValueAsPercentage());
        filter.setRotateResultAngle((float) rotateResult.getValueInIntuitiveRadians());

        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        dest = filter.filter(src, dest);
        setAffectedAreaShapes(filter.getAffectedAreaShapes());

        return dest;
    }
}