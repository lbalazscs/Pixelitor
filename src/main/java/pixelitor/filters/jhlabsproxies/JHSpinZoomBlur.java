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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.MotionBlur;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * "Spin and Zoom Blur" filter based on the JHLabs MotionBlurOp/MotionBlurFilter classes
 */
public class JHSpinZoomBlur extends ParametrizedFilter {
    public static final String NAME = "Spin and Zoom Blur";

    @Serial
    private static final long serialVersionUID = -1904077888275935586L;

    private final RangeParam rotation = new RangeParam("Spin Blur Amount (Degrees)", -45, 0, 45);
    private final RangeParam zoom = new RangeParam("Zoom Blur Amount", 0, 0, 200);
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final BooleanParam hpSharpening = BooleanParam.forHPSharpening();
    private final EnumParam<MotionBlurQuality> quality = new EnumParam<>("Quality", MotionBlurQuality.class);

    public JHSpinZoomBlur() {
        super(true);

        initParams(
            rotation,
            zoom,
            center,
            quality,
            hpSharpening
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        float zoomValue = (float) zoom.getPercentage();
        float rotationValue = rotation.getValueInRadians();
        if (zoomValue == 0.0f && rotationValue == 0.0f) {
            return src;
        }

        MotionBlur filter = quality.getSelected().createFilter(NAME, src);

        filter.setCenter(center.getRelativePoint());
        filter.setRotation(rotationValue);
        filter.setZoom(zoomValue);

        dest = filter.filter(src, dest);

        if (hpSharpening.isChecked()) {
            dest = ImageUtils.toHighPassSharpenedImage(src, dest);
        }

        return dest;
    }

    @Override
    public boolean supportsGray() {
        return !hpSharpening.isChecked();
    }
}