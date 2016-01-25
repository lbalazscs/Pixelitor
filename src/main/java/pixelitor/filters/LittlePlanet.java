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
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.filters.impl.LittlePlanetFilter;

import java.awt.image.BufferedImage;

/**
 * Little Planet
 */
public class LittlePlanet extends FilterWithParametrizedGUI {
    public static final String NAME = "Little Planet";

    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final AngleParam rotateResult = new AngleParam("Rotate Result", -Math.PI / 2);
    private final RangeParam zoom = new RangeParam("Zoom (%)", 1, 100, 300);
    private final RangeParam innerZoom = new RangeParam("Inner Zoom (%)", 30, 100, 180);
    private final BooleanParam invert = new BooleanParam("Invert", false);

    private final IntChoiceParam edgeAction = IntChoiceParam.getEdgeActionChoices(true);
    private final IntChoiceParam interpolation = IntChoiceParam.getInterpolationChoices();


    private LittlePlanetFilter filter;

    public LittlePlanet() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                rotateResult,
                zoom,
                innerZoom,
                center,
                invert,
                edgeAction,
                interpolation
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if(filter == null) {
            filter = new LittlePlanetFilter();
        }

        filter.setZoom(zoom.getValueAsPercentage());
        filter.setInnerZoom(innerZoom.getValueAsPercentage());
        filter.setRotateResult(rotateResult.getValueInIntuitiveRadians());
        filter.setInverted(invert.isChecked());

        filter.setCenterX(center.getRelativeX());
        filter.setCenterY(center.getRelativeY());

        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }
}