/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.*;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.impl.PolarTilesFilter;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * Polar Glass Tiles filter
 */
public class PolarTiles extends ParametrizedFilter {
    public static final String NAME = "Polar Glass Tiles";

    @Serial
    private static final long serialVersionUID = -2428230904945914960L;

    private final IntChoiceParam modeParam = new IntChoiceParam("Type", new Item[]{
        new Item("Concentric", PolarTilesFilter.MODE_CONCENTRIC),
        new Item("Spiral", PolarTilesFilter.MODE_SPIRAL),
        new Item("Vortex", PolarTilesFilter.MODE_VORTEX),
    });
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final RangeParam numAngDivisions = new RangeParam("Angular Divisions", 0, 7, 100);
    private final RangeParam numRadDivisions = new RangeParam("Radial Divisions", 0, 7, 50);
    private final RangeParam rotateEffect = new RangeParam("Rotate Effect", 0, 0, 100);
    private final RangeParam randomness = new RangeParam("Randomness", 0, 0, 100);
    private final RangeParam curvature = new RangeParam("Curvature", 0, 7, 20);

    private final RangeParam zoom = new RangeParam("Zoom Image (%)", 1, 100, 500);
    private final AngleParam rotateImage = new AngleParam("Rotate Image", 0);

    private final IntChoiceParam edgeAction = IntChoiceParam.forEdgeAction(true);
    private final IntChoiceParam interpolation = IntChoiceParam.forInterpolation();

    private PolarTilesFilter filter;

    public PolarTiles() {
        super(true);

        var reseedRandomness = paramSet.createReseedNoiseAction("", "Reseed Randomness");
        randomness.setupEnableOtherIfNotZero(reseedRandomness);
        initParams(
            modeParam,
            center,
            numAngDivisions,
            numRadDivisions,
            curvature.withAdjustedRange(0.02),
            rotateEffect,
            randomness.withAction(reseedRandomness),
            new CompositeParam("Background", zoom, rotateImage),
            edgeAction,
            interpolation
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new PolarTilesFilter();
        }

        filter.setMode(modeParam.getValue());
        filter.setCenter(center.getAbsolutePoint(src));
        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());
        filter.setRotateResult((float) rotateImage.getValueInIntuitiveRadians());
        filter.setZoom(zoom.getPercentage());
        filter.setRotateEffect(rotateEffect.getPercentage());
        filter.setNumADivisions(numAngDivisions.getValue());
        filter.setNumRDivisions(numRadDivisions.getValue());
        filter.setCurvature(curvature.getValueAsDouble());
        filter.setRandomness(randomness.getPercentage());

        return filter.filter(src, dest);
    }
}