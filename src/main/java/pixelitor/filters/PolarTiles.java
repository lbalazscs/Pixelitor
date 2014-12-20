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
 * Polar Glass Tiles filter
 */
public class PolarTiles extends FilterWithParametrizedGUI {

    private ImagePositionParam center = new ImagePositionParam("Center");

    private RangeParam numAngDivisions = new RangeParam("Number of Angular Divisions", 0, 100, 7);
    private RangeParam numRadDivisions = new RangeParam("Number of Radial Divisions", 0, 50, 7);
    private RangeParam rotateEffect = new RangeParam("Rotate Effect", 0, 100, 0);

    private RangeParam randomness = new RangeParam("Randomness", 0, 100, 0);
    private RangeParam curvature = new RangeParam("Curvature", 0, 20, 4);

    private RangeParam zoom = new RangeParam("Zoom (%)", 1, 500, 100);
    private AngleParam rotateImage = new AngleParam("Rotate Image", 0);

    private IntChoiceParam edgeAction = IntChoiceParam.getEdgeActionChoices(true);
    private IntChoiceParam interpolation = IntChoiceParam.getInterpolationChoices();

    private PolarTilesFilter filter;

    public PolarTiles() {
        super("Polar Glass Tiles", true, false);
        setParamSet(new ParamSet(
                center,
                numAngDivisions,
                numRadDivisions,
                curvature.adjustRangeToImageSize(0.02),
                rotateEffect,
                zoom,
                randomness,
                rotateImage,
                edgeAction,
                interpolation,
                new ReseedNoiseActionParam("Reseed Randomness")
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new PolarTilesFilter();
        }

        filter.setCenterX(center.getRelativeX());
        filter.setCenterY(center.getRelativeY());
        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());
        filter.setRotateResult((float) rotateImage.getValueInIntuitiveRadians());
        filter.setZoom(zoom.getValueAsPercentage());
        filter.setT(rotateEffect.getValueAsPercentage());
        filter.setNumADivisions(numAngDivisions.getValue());
        filter.setNumRDivisions(numRadDivisions.getValue());
        filter.setCurvature(curvature.getValueAsDouble());
        filter.setRandomness(randomness.getValueAsPercentage());

        dest = filter.filter(src, dest);
        return dest;
    }
}