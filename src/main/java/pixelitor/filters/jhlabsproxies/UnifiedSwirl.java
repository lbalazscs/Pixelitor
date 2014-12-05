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

import com.jhlabs.image.PinchFilter;
import com.jhlabs.image.SwirlMethod;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;

import java.awt.image.BufferedImage;

/**
 * Swirl-Bulge-Pinch filter can use SwirlFilter or PinchFilter
 */
public class UnifiedSwirl extends FilterWithParametrizedGUI {
//    private static final int AFFECT_EVERYWHERE = 1;
//    private static final int AFFECT_INSIDE_RADIUS = 2;
//
//    private final IntChoiceParam algorithmChooser = new IntChoiceParam("Affect",
//            new IntChoiceParam.Value[] {
//                    new IntChoiceParam.Value("Around Radius", AFFECT_EVERYWHERE),
//                    new IntChoiceParam.Value("Inside Radius", AFFECT_INSIDE_RADIUS),
//            });

    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final RangeParam radius = new RangeParam("Radius", 1, 999, 500);
    private final RangeParam swirlAmount = new RangeParam("Swirl Amount", -360, 360, 90);
    private final RangeParam pinchBulgeAmount = new RangeParam("Pinch-Bulge Amount", -100, 100, 0);
    private RangeParam zoomParam = new RangeParam("Zoom (%)", 1, 500, 100);
    private AngleParam rotateResultParam = new AngleParam("Rotate Result", 0);

    private final IntChoiceParam edgeAction = IntChoiceParam.getEdgeActionChoices();
    private final IntChoiceParam interpolation = IntChoiceParam.getInterpolationChoices();

    private SwirlMethod filter;
    private PinchFilter pinchFilter;
//    private SwirlFilter swirlFilter;

    public UnifiedSwirl() {
        super("Swirl, Pinch, Bulge", true, true);
        setParamSet(new ParamSet(
//                algorithmChooser,
                swirlAmount,
                pinchBulgeAmount,
                radius.adjustRangeToImageSize(1.0),
                center,
                zoomParam,
                rotateResultParam,
                edgeAction,
                interpolation
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
//        int algorithm = algorithmChooser.getValue();
//        if(algorithm == AFFECT_EVERYWHERE) {
//            if(swirlFilter == null) {
//                swirlFilter = new SwirlFilter();
//            }
//            filter = swirlFilter;
//        } else if(algorithm == AFFECT_INSIDE_RADIUS) {
            if(pinchFilter == null) {
                pinchFilter = new PinchFilter();
            }
            filter = pinchFilter;
//        }

        filter.setPinchBulgeAmount(pinchBulgeAmount.getValueAsPercentage());
        filter.setSwirlAmount(swirlAmount.getValueInRadians());
        filter.setRadius(radius.getValue());
        filter.setCenterX(center.getRelativeX());
        filter.setCenterY(center.getRelativeY());

        filter.setZoom(zoomParam.getValueAsPercentage());
        filter.setRotateResultAngle((float) rotateResultParam.getValueInIntuitiveRadians());

        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        dest = filter.filter(src, dest);
        setAffectedAreaShapes(filter.getAffectedAreaShapes());

        return dest;
    }
}