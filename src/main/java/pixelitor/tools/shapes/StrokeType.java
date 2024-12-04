/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.shapes;

import com.bric.awt.BristleStroke;
import com.bric.awt.CalligraphyStroke;
import com.bric.awt.CharcoalStroke;
import com.jhlabs.awt.CompositeStroke;
import com.jhlabs.awt.ShapeStroke;
import com.jhlabs.awt.WobbleStroke;
import com.jhlabs.awt.ZigzagStroke;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.StrokeParam;
import pixelitor.tools.util.Drag;
import pixelitor.utils.RailwayTrackStroke;
import pixelitor.utils.TaperingStroke;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Stroke;

/**
 * Different stroke types available for drawing shapes.
 */
public enum StrokeType {
    BASIC("Basic", false) {
        @Override
        public Stroke createStroke(float width, int cap, int join, float[] dashPattern) {
            return new BasicStroke(width, cap, join, 1.5f, dashPattern, 0.0f);
        }
    }, ZIGZAG("Zigzag", false) {
        @Override
        public Stroke createStroke(float width, int cap, int join, float[] dashPattern) {
            Stroke tmp = BASIC.createStroke(width, cap, join, dashPattern);
            return new ZigzagStroke(tmp, width, width);
        }

        @Override
        public double getExtraThickness(double specifiedWidth) {
            return specifiedWidth / 2;
        }
    }, WOBBLE("Wobble", true) {
        private static final float SIZE_DIVIDING_FACTOR = 4.0f;
        private float lastWidth = 0.0f;
        private WobbleStroke wobbleStroke;

        @Override
        public Stroke createStroke(float width, int cap, int join, float[] dashPattern) {
            // Create a stroke object only when necessary; the main benefit
            // is that the seed doesn't change when the mouse is released.
            if (wobbleStroke == null || width != lastWidth) {
                wobbleStroke = new WobbleStroke(0.5f,
                    width / SIZE_DIVIDING_FACTOR, 10);
                lastWidth = width;
            }
            return wobbleStroke;
        }

        @Override
        public double getExtraThickness(double specifiedWidth) {
            return specifiedWidth * 1.5;
        }
    }, CHARCOAL("Charcoal (can be slow!)", true) {
        @Override
        public Stroke createStroke(float width, int cap, int join, float[] dashPattern) {
            return new CharcoalStroke(width, 0.5f);
        }
    }, BRISTLE("Bristle (can be slow!)", true) {
        @Override
        public Stroke createStroke(float width, int cap, int join, float[] dashPattern) {
            return new BristleStroke(width, 0.5f);
        }
    }, OUTLINE("Outline", false) {
        @Override
        public Stroke createStroke(float width, int cap, int join, float[] dashPattern) {
            return new CompositeStroke(
                new BasicStroke(width, cap, join),
                innerOutlineStroke);
        }

        @Override
        public Stroke getInnerStroke() {
            return innerOutlineStroke;
        }
    }, CALLIGRAPHY("Calligraphy", false) {
        @Override
        public Stroke createStroke(float width, int cap, int join, float[] dashPattern) {
            return new CalligraphyStroke(width);
        }
    }, SHAPE("Shape", false) {
        @Override
        public Stroke createStroke(float width, int cap, int join, float[] dashPattern) {
            // needs a full StrokeParam
            throw new UnsupportedOperationException();
        }

        @Override
        public Stroke createStroke(StrokeParam param, float width) {
            float[] dashPattern = param.getDashPattern(width);
            float advance = width * 1.2f;
            if (dashPattern != null) {
                advance *= 2.0f; // simulate dashes
            }
            ShapeType shapeType = param.getShapeType();
            Shape shape = shapeType.createShape(new Drag(0, 0, width, width), null);
            return new ShapeStroke(shape, advance);
        }
    }, TAPERING("Tapering", false) {
        @Override
        public Stroke createStroke(float width, int cap, int join, float[] dashPattern) {
            return new TaperingStroke(width);
        }
    }, TAPERING_REV("Reversed Tapering", false) {
        @Override
        public Stroke createStroke(float width, int cap, int join, float[] dashPattern) {
            return new TaperingStroke(width, true);
        }
    }, RAILWAY("Railway Tracks", false) {
        @Override
        public Stroke createStroke(float width, int cap, int join, float[] dashPattern) {
            return new RailwayTrackStroke(width);
        }
    };

    private static final String NAME = "Line Type";
    private static final float OUTLINE_WIDTH = 1.0f;
    private static final BasicStroke innerOutlineStroke = new BasicStroke(OUTLINE_WIDTH);

    private final String displayName;
    private final boolean slow;

    StrokeType(String displayName, boolean slow) {
        this.displayName = displayName;
        this.slow = slow;
    }

    public Stroke getInnerStroke() {
        // overridden for OUTLINE
        throw new UnsupportedOperationException();
    }

    public abstract Stroke createStroke(float width, int cap, int join, float[] dashPattern);

    public Stroke createStroke(StrokeParam param, float width) {
        return createStroke(width, param.getCap(), param.getJoin(), param.getDashPattern(width));
    }

    public boolean isSlow() {
        return slow;
    }

    /**
     * Returns the additional thickness added to the specified
     * width to determine the actual stroke thickness.
     */
    public double getExtraThickness(double specifiedWidth) {
        return 0; // return 0 by default, can be overridden
    }

    public static EnumParam<StrokeType> asParam() {
        return new EnumParam<>(NAME, StrokeType.class);
    }

    @Override
    public String toString() {
        return displayName;
    }
}

