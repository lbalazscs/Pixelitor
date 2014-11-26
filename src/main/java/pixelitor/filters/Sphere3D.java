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

import com.jhlabs.image.TransformFilter;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.impl.Sphere3DFilter;

import java.awt.image.BufferedImage;

/**
 * Sphere3D based on Sphere3DFilter
 */
public class Sphere3D extends FilterWithParametrizedGUI {
    private RangeParam alphaParam = new RangeParam("alpha", 0, 100, 50);
    private RangeParam betaParam = new RangeParam("beta", 0, 100, 50);
    private RangeParam gammaParam = new RangeParam("gamma", 0, 100, 50);
    private ImagePositionParam centerParam = new ImagePositionParam("Center");
    private IntChoiceParam interpolationParam = IntChoiceParam.getInterpolationChoices();

    private Sphere3DFilter filter;

    public Sphere3D() {
        super("Sphere3D", true, false);
        setParamSet(new ParamSet(
                centerParam,
                alphaParam,
                betaParam,
                gammaParam,
                interpolationParam
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if(filter == null) {
            filter = new Sphere3DFilter();
        }

        filter.setAlpha(alphaParam.getValueAsPercentage());
        filter.setBeta(betaParam.getValueAsPercentage());
        filter.setGamma(gammaParam.getValueAsPercentage());

        filter.setCenterX(centerParam.getRelativeX());
        filter.setCenterY(centerParam.getRelativeY());
        filter.setInterpolation(interpolationParam.getValue());
        filter.setEdgeAction(TransformFilter.TRANSPARENT);

        dest = filter.filter(src, dest);
        return dest;
    }
}
