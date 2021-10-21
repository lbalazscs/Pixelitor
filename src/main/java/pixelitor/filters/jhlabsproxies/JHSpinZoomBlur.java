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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.MotionBlur;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

/**
 * "Spin and Zoom Blur" filter based on the JHLabs MotionBlurOp/MotionBlurFilter classes
 */
public class JHSpinZoomBlur extends ParametrizedFilter {
    public static final String NAME = "Spin and Zoom Blur";

    private final RangeParam rotation = new RangeParam("Spin Blur Amount (Degrees)", -45, 0, 45);
    private final RangeParam zoom = new RangeParam("Zoom Blur Amount", 0, 0, 200);
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final BooleanParam hpSharpening = BooleanParam.forHPSharpening();
    private final EnumParam<MotionBlurQuality> quality = new EnumParam<>("Quality", MotionBlurQuality.class);

    public JHSpinZoomBlur() {
        super(true);

        setParams(
            rotation,
            zoom,
            center,
            quality,
            hpSharpening
        );
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        float zoomValue = zoom.getPercentageValF();
        float rotationValue = rotation.getValueInRadians();
        if (zoomValue == 0.0f && rotationValue == 0.0f) {
            return src;
        }

        MotionBlur filter = quality.getSelected().createFilter(NAME, src);

        filter.setCentreX(center.getRelativeX());
        filter.setCentreY(center.getRelativeY());
        filter.setRotation(rotationValue);
        filter.setZoom(zoomValue);

        dest = filter.filter(src, dest);

        if (hpSharpening.isChecked()) {
            dest = ImageUtils.getHighPassSharpenedImage(src, dest);
        }

        return dest;
    }
}