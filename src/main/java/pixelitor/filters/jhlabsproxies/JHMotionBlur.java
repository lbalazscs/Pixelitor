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

import com.jhlabs.image.MotionBlur;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * "Motion Blur" filter based on the JHLabs MotionBlurOp/MotionBlurFilter classes
 */
public class JHMotionBlur extends ParametrizedFilter {
    public static final String NAME = "Motion Blur";

    @Serial
    private static final long serialVersionUID = 6811375510367120301L;

    private final AngleParam angle = new AngleParam("Direction", 0);
    private final RangeParam distance = new RangeParam("Distance", 0, 0, 200);
    private final EnumParam<MotionBlurQuality> quality = new EnumParam<>("Quality", MotionBlurQuality.class);
    private final BooleanParam hpSharpening = BooleanParam.forHPSharpening();

    public JHMotionBlur() {
        super(true);
        setParams(
            distance,
            angle,
            quality,
            hpSharpening
        );
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int distanceValue = distance.getValue();
        if (distanceValue == 0) {
            return src;
        }

        MotionBlur filter = quality.getSelected().createFilter(NAME, src);

        filter.setAngle((float) angle.getValueInIntuitiveRadians());
        filter.setDistance(distance.getValueAsFloat());

        dest = filter.filter(src, dest);

        if (hpSharpening.isChecked()) {
            dest = ImageUtils.getHighPassSharpenedImage(src, dest);
        }

        return dest;
    }

    @Override
    public boolean supportsGray() {
        return !hpSharpening.isChecked();
    }
}