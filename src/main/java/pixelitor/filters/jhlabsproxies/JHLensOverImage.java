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

import com.jhlabs.image.SphereFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.GUIText;

import java.awt.image.BufferedImage;

/**
 * "Lens over image" filter based on the JHLabs SphereFilter
 */
public class JHLensOverImage extends ParametrizedFilter {
    public static final String NAME = "Lens Over Image";

    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final GroupedRangeParam radius = new GroupedRangeParam(GUIText.RADIUS, 0, 200, 999);

    // less than 100% doesn't create anything usable
    private final RangeParam refractionIndex = new RangeParam("Refraction Index (%)", 100, 150, 300);

    private final IntChoiceParam interpolation = IntChoiceParam.forInterpolation();

    private SphereFilter filter;

    public JHLensOverImage() {
        super(true);

        showAffectedArea();

        setParams(
            center,
            radius.withAdjustedRange(1.0),
            refractionIndex,
//                edgeAction,  // edge action doesn't create anything usable in this case
            interpolation
        );
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        float refraction = (float) refractionIndex.getPercentage();
        int hRadius = radius.getValue(0);
        int vRadius = radius.getValue(1);

        if (refraction == 1.0f || hRadius == 0 || vRadius == 0) {
            return src;
        }

        if (filter == null) {
            filter = new SphereFilter(NAME);
        }

        filter.setCentre(center.getRelativePoint());

        filter.setA(hRadius);
        filter.setB(vRadius);

        filter.setRefractionIndex(refraction);

//        filter.setEdgeAction(edgeAction.getCurrentInt());
        filter.setInterpolation(interpolation.getValue());

        dest = filter.filter(src, dest);
        setAffectedAreaShapes(filter.getAffectedAreaShapes());
        return dest;
    }
}