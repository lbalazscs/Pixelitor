/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.TransformFilter;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.impl.Sphere3DFilter;

import java.awt.image.BufferedImage;

/**
 * Sphere3D based on Sphere3DFilter
 */
public class Sphere3D extends ParametrizedFilter {
    public static final String NAME = "Sphere3D";

    private final RangeParam alpha = new RangeParam("alpha", 0, 50, 100);
    private final RangeParam beta = new RangeParam("beta", 0, 50, 100);
    private final RangeParam gamma = new RangeParam("gamma", 0, 50, 100);
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final IntChoiceParam interpolation = IntChoiceParam.forInterpolation();

    private Sphere3DFilter filter;

    public Sphere3D() {
        super(true);

        setParams(
            center,
            alpha,
            beta,
            gamma,
            interpolation
        );
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new Sphere3DFilter();
        }

        filter.setAlpha(alpha.getPercentageValF());
        filter.setBeta(beta.getPercentageValF());
        filter.setGamma(gamma.getPercentageValF());

        filter.setRelCenter(center.getRelativeX(), center.getRelativeY());
        filter.setInterpolation(interpolation.getValue());
        filter.setEdgeAction(TransformFilter.TRANSPARENT);

        return filter.filter(src, dest);
    }
}
