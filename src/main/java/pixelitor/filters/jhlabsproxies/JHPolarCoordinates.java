/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.PolarFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.GUIText;

import java.awt.image.BufferedImage;

import static pixelitor.gui.GUIText.ZOOM;
import static pixelitor.utils.Texts.i18n;

/**
 * Polar Coordinates filter based on the JHLabs PolarFilter
 */
public class JHPolarCoordinates extends ParametrizedFilter {
    public static final String NAME = i18n("polar_coordinates");

    private final ImagePositionParam center = new ImagePositionParam("Center");

    private static final Item[] gridTypeChoices = {
        new Item("Rectangular to Polar ", PolarFilter.RECT_TO_POLAR),
        new Item("Polar to Rectangular", PolarFilter.POLAR_TO_RECT),
        new Item("Invert in Circle", PolarFilter.INVERT_IN_CIRCLE),
    };
    private final IntChoiceParam type = new IntChoiceParam(GUIText.TYPE, gridTypeChoices);
    private final IntChoiceParam edgeAction = IntChoiceParam.forEdgeAction();
    private final IntChoiceParam interpolation = IntChoiceParam.forInterpolation();
    private final RangeParam zoom = new RangeParam(ZOOM + " (%)", 1, 100, 501);
    private final AngleParam angle = new AngleParam("Angle", 0);

    private PolarFilter filter;

    public JHPolarCoordinates() {
        super(true);

        type.setPresetKey("Type");
        zoom.setPresetKey("Zoom (%)");

        setParams(center, type, zoom, angle, edgeAction, interpolation);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new PolarFilter(NAME);
        }

        filter.setType(type.getValue());
        filter.setEdgeAction(edgeAction.getValue());
        filter.setRelativeCentreX((float) center.getRelativeX());
        filter.setRelativeCentreY((float) center.getRelativeY());
        filter.setInterpolation(interpolation.getValue());
        filter.setZoom((float) zoom.getPercentage());
        filter.setAngle(angle.getValueInIntuitiveRadians());

        return filter.filter(src, dest);
    }
}