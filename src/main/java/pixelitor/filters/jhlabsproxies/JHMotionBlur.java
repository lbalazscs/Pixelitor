/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import com.jhlabs.image.MotionBlurFilter;
import com.jhlabs.image.MotionBlurOp;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.*;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.util.FilterAction;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;
import static pixelitor.filters.jhlabsproxies.JHMotionBlur.Mode.MOTION_BLUR;
import static pixelitor.filters.jhlabsproxies.JHMotionBlur.Mode.SPIN_ZOOM_BLUR;
import static pixelitor.filters.jhlabsproxies.JHMotionBlur.MotionBlurQuality.BETTER;
import static pixelitor.filters.jhlabsproxies.JHMotionBlur.MotionBlurQuality.FASTER;

/**
 * "Motion Blur" and "Spin and Zoom Blur" filters based on the JHLabs
 * MotionBlurOp/MotionBlurFilter classes
 */
public class JHMotionBlur extends ParametrizedFilter {
    private final AngleParam angle = new AngleParam("Direction", 0);
    private final RangeParam distance = new RangeParam("Distance", 0, 0, 200);
    private final RangeParam rotation = new RangeParam("Spin Blur Amount (Degrees)", -45, 0, 45);
    private final RangeParam zoom = new RangeParam("Zoom Blur Amount", 0, 0, 200);
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final BooleanParam hpSharpening = BooleanParam.forHPSharpening();

    private final Mode mode;

    enum MotionBlurQuality {
        FASTER {
            @Override
            public MotionBlur createFilter(String filterName) {
                return new MotionBlurOp(filterName);
            }
        }, BETTER {
            @Override
            public MotionBlur createFilter(String filterName) {
                var filter = new MotionBlurFilter(filterName);
                filter.setPremultiplyAlpha(false);
                filter.setWrapEdges(false);
                return filter;
            }
        };

        public abstract MotionBlur createFilter(String filterName);
    }

    private static final Item[] methodChoices = {
        new Item("Faster", FASTER.ordinal()),
        new Item("High Quality (slow for large images)", BETTER.ordinal()),
    };

    private final IntChoiceParam method = new IntChoiceParam("Quality", methodChoices, IGNORE_RANDOMIZE);

    public enum Mode {
        MOTION_BLUR {
            @Override
            public String toString() {
                return "Motion Blur";
            }
        }, SPIN_ZOOM_BLUR {
            @Override
            public String toString() {
                return "Spin and Zoom Blur";
            }
        };

        public FilterAction createFilterAction() {
            return new FilterAction(toString(), () -> new JHMotionBlur(this));
        }

        // the ParamSet can't be created here, because the referenced fields belong to the filter...
    }

    public JHMotionBlur(Mode mode) {
        super(ShowOriginal.YES);

        this.mode = mode;

        if(mode == MOTION_BLUR) {
            setParams(
                    distance,
                    angle,
                    method,
                    hpSharpening
            );

        } else if(mode == SPIN_ZOOM_BLUR) {
            setParams(
                    rotation,
                    zoom,
                    center,
                    method,
                    hpSharpening
            );
        } else {
            throw new IllegalStateException("should not get here");
        }
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int distanceValue = distance.getValue();
        float zoomValue = zoom.getPercentageValF();
        float rotationValue = rotation.getValueInRadians();
        if(mode == MOTION_BLUR) {
            if (distanceValue == 0) {
                return src;
            }
        } else if(mode == SPIN_ZOOM_BLUR) {
            if (zoomValue == 0.0f && rotationValue == 0.0f) {
                return src;
            }
        }

        int intValue = method.getValue();
        MotionBlurQuality chosenMethod = MotionBlurQuality.values()[intValue];

        String filterName = mode.toString();
        MotionBlur filter = chosenMethod.createFilter(filterName);

        filter.setCentreX(center.getRelativeX());
        filter.setCentreY(center.getRelativeY());
        filter.setAngle((float) angle.getValueInIntuitiveRadians());
        filter.setDistance(distance.getValueAsFloat());
        filter.setRotation(rotationValue);
        filter.setZoom(zoomValue);

        dest = filter.filter(src, dest);

        if (hpSharpening.isChecked()) {
            dest = ImageUtils.getHighPassSharpenedImage(src, dest);
        }

        return dest;
    }
}