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
package pixelitor.filters;

import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.impl.LittlePlanetFilter;

import java.awt.image.BufferedImage;

/**
 * Little Planet
 */
public class LittlePlanet extends FilterWithParametrizedGUI {
    private ImagePositionParam centerParam = new ImagePositionParam("Center");
    private AngleParam rotateResultParam = new AngleParam("Rotate Result", - Math.PI / 2);
    private RangeParam zoomParam = new RangeParam("Zoom (%)", 1, 300, 100);
    private RangeParam innerZoomParam = new RangeParam("Inner Zoom (%)", 30, 180, 100);
    private BooleanParam invertParam = new BooleanParam("Invert", false);

    private IntChoiceParam edgeActionParam =  IntChoiceParam.getEdgeActionChoices(true);
    private IntChoiceParam interpolationParam = IntChoiceParam.getInterpolationChoices();


    private LittlePlanetFilter filter;

    public LittlePlanet() {
        super("Little Planet", true, false);
        setParamSet(new ParamSet(
                rotateResultParam,
                zoomParam,
                innerZoomParam,
                centerParam,
                invertParam,
                edgeActionParam,
                interpolationParam
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if(filter == null) {
            filter = new LittlePlanetFilter();
        }

        filter.setZoom(zoomParam.getValueAsPercentage());
        filter.setInnerZoom(innerZoomParam.getValueAsPercentage());
        filter.setRotateResult(rotateResultParam.getValueInIntuitiveRadians());
        filter.setInverted(invertParam.getValue());

        filter.setCenterX(centerParam.getRelativeX());
        filter.setCenterY(centerParam.getRelativeY());

        filter.setEdgeAction(edgeActionParam.getValue());
        filter.setInterpolation(interpolationParam.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }
}