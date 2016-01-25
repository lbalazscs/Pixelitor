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

package pixelitor.filters;

import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ReseedNoiseActionSetting;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.filters.impl.PolarTilesFilter;

import java.awt.image.BufferedImage;

/**
 * Polar Glass Tiles filter
 */
public class PolarTiles extends FilterWithParametrizedGUI {
    public static final String NAME = "Polar Glass Tiles";

    private final ImagePositionParam center = new ImagePositionParam("Center");

    private final RangeParam numAngDivisions = new RangeParam("Number of Angular Divisions", 0, 7, 100);
    private final RangeParam numRadDivisions = new RangeParam("Number of Radial Divisions", 0, 7, 50);
    private final RangeParam rotateEffect = new RangeParam("Rotate Effect", 0, 0, 100);

    private final RangeParam randomness = new RangeParam("Randomness", 0, 0, 100);
    private final RangeParam curvature = new RangeParam("Curvature", 0, 4, 20);

    private final RangeParam zoom = new RangeParam("Zoom (%)", 1, 100, 500);
    private final AngleParam rotateImage = new AngleParam("Rotate Image", 0);

    private final IntChoiceParam edgeAction = IntChoiceParam.getEdgeActionChoices(true);
    private final IntChoiceParam interpolation = IntChoiceParam.getInterpolationChoices();

    private PolarTilesFilter filter;

    public PolarTiles() {
        super(ShowOriginal.YES);
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
                interpolation
        ).withAction(new ReseedNoiseActionSetting("Reseed Randomness")));
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