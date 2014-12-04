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
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ReseedNoiseActionParam;
import pixelitor.filters.impl.PolarTilesFilter;

import java.awt.image.BufferedImage;

/**
 *
 */
public class PolarTiles extends FilterWithParametrizedGUI {

    private ImagePositionParam centerParam = new ImagePositionParam("Center");

    private RangeParam numADivisionsParam = new RangeParam("Number of Angular Divisions", 0, 100, 7);
    private RangeParam numRDivisionsParam = new RangeParam("Number of Radial Divisions", 0, 50, 7);
    private RangeParam rotateParam = new RangeParam("Rotate Effect", 0, 100, 0);

    private RangeParam randomnessParam = new RangeParam("Randomness", 0, 100, 0);
    private RangeParam curvatureParam = new RangeParam("Curvature", 0, 20, 4);

    private RangeParam zoomParam = new RangeParam("Zoom (%)", 1, 500, 100);
    private AngleParam rotateResultParam = new AngleParam("Rotate Result", 0);

    private IntChoiceParam edgeActionParam = IntChoiceParam.getEdgeActionChoices(true);
    private IntChoiceParam interpolationParam = IntChoiceParam.getInterpolationChoices();

    private PolarTilesFilter filter;

    public PolarTiles() {
        super("Polar Glass Tiles", true, false);
        setParamSet(new ParamSet(
                centerParam,
                numADivisionsParam,
                numRDivisionsParam,
                curvatureParam.adjustRangeToImageSize(0.02),
                rotateParam,
                zoomParam,
                randomnessParam,
                rotateResultParam,
                edgeActionParam,
                interpolationParam,
                new ReseedNoiseActionParam("Reseed Randomness")
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new PolarTilesFilter();
        }

        filter.setCenterX(centerParam.getRelativeX());
        filter.setCenterY(centerParam.getRelativeY());
        filter.setEdgeAction(edgeActionParam.getValue());
        filter.setInterpolation(interpolationParam.getValue());
        filter.setRotateResult((float) rotateResultParam.getValueInIntuitiveRadians());
        filter.setZoom(zoomParam.getValueAsPercentage());
        filter.setT(rotateParam.getValueAsPercentage());
        filter.setNumADivisions(numADivisionsParam.getValue());
        filter.setNumRDivisions(numRDivisionsParam.getValue());
        filter.setCurvature(curvatureParam.getValue());
        filter.setRandomness(randomnessParam.getValueAsPercentage());

        dest = filter.filter(src, dest);
        return dest;
    }
}