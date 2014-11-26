/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
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
package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.MotionBlur;
import com.jhlabs.image.MotionBlurFilter;
import com.jhlabs.image.MotionBlurOp;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

/**
 * Motion Blur based on the JHLabs MotionBlur
 */
public class JHMotionBlur extends FilterWithParametrizedGUI {
    private final AngleParam angle = new AngleParam("Direction", 0);
    private final RangeParam distance = new RangeParam("Distance", 0, 200, 0);
    private final RangeParam rotation = new RangeParam("Spin Blur Amount (Degrees)", -45, 45, 0);
    private final RangeParam zoom = new RangeParam("Zoom Blur Amount (%)", 0, 200, 0);
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final BooleanParam hpSharpening = BooleanParam.createParamForHPSharpening();

    private final Mode mode;

    enum MBMethod {
        FAST {
            @Override
            public MotionBlur getImplementation() {
                MotionBlurOp op = new MotionBlurOp();
                return op;
            }
        }, GOOD {
            @Override
            public MotionBlur getImplementation() {
                MotionBlurFilter filter = new MotionBlurFilter();
                filter.setPremultiplyAlpha(false);
                filter.setWrapEdges(false);
                return filter;
            }
        };

        public abstract MotionBlur getImplementation();
    }

    private static final IntChoiceParam.Value[] methodChoices = {
            new IntChoiceParam.Value("Faster", MBMethod.FAST.ordinal()),
            new IntChoiceParam.Value("High Quality", MBMethod.GOOD.ordinal()),
    };
    private final IntChoiceParam method = new IntChoiceParam("Quality", methodChoices, true);

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
        }

        // the ParamSet cannot be created here, because the referenced fields belong to the filter...
    }

    public JHMotionBlur(Mode mode) {
        super(mode.toString(), true, false);
        this.mode = mode;

        if (mode == Mode.MOTION_BLUR) {
            setParamSet(new ParamSet(
                    distance,
                    angle,
                    method,
                    hpSharpening
            ));

        } else if (mode == Mode.SPIN_ZOOM_BLUR) {
            setParamSet(new ParamSet(
                    rotation,
                    zoom,
                    center,
                    method,
                    hpSharpening
            ));
        } else {
            throw new IllegalStateException("should not get here");
        }
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int distanceValue = distance.getValue();
        float zoomValue = zoom.getValueAsPercentage();
        float rotationValue = rotation.getValueInRadians();
        if (mode == Mode.MOTION_BLUR) {
            if (distanceValue == 0) {
                return src;
            }
        } else if (mode == Mode.SPIN_ZOOM_BLUR) {
            if (zoomValue == 0.0f && rotationValue == 0.0f) {
                return src;
            }
        }

        int intValue = method.getValue();
        MBMethod chosenMethod = MBMethod.values()[intValue];

        MotionBlur filter = chosenMethod.getImplementation();

        filter.setCentreX(center.getRelativeX());
        filter.setCentreY(center.getRelativeY());
        filter.setAngle((float) angle.getValueInIntuitiveRadians());
        filter.setDistance(distanceValue);
        filter.setRotation(rotationValue);
        filter.setZoom(zoomValue);

        dest = filter.filter(src, dest);

        if (hpSharpening.getValue()) {
            dest = ImageUtils.getHighPassSharpenedImage(src, dest);
        }

        return dest;
    }
}