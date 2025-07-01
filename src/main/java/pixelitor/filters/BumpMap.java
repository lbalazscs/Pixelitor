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

import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.SelectImageParam;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

/**
 * The Bump Map filter
 */
public class BumpMap extends ParametrizedFilter {
    public static final String NAME = "Bump Map";

    private final SelectImageParam imageParam = new SelectImageParam("Bump Map");
    private final BooleanParam tileParam = new BooleanParam("Tile");
    private final AngleParam lightDirection = new AngleParam(
        "Light Direction", 0);
    private final RangeParam depth = new RangeParam(
        "Depth", 1, 7, 15);

    public BumpMap() {
        super(true);

        initParams(
            imageParam,
            tileParam,
            lightDirection,
            depth
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        BufferedImage bumpImage = imageParam.getImage();
        float adjustedDepth = (float) (Math.pow(2, depth.getValue()) / 100.0);
        return ImageUtils.bumpMap(src, bumpImage,
            (float) lightDirection.getValueInIntuitiveRadians(),
            adjustedDepth, NAME, tileParam.isChecked());
    }

    @Override
    public boolean supportsGray() {
        return false;
    }

    @Override
    public boolean canBeSmart() {
        return false;
    }
}
