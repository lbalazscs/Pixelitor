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

package pixelitor.tools.shapes;

import com.bric.awt.BristleStroke;
import com.bric.awt.CalligraphyStroke;
import com.bric.awt.CharcoalStroke;
import com.jhlabs.awt.CompositeStroke;
import com.jhlabs.awt.ShapeStroke;
import com.jhlabs.awt.WobbleStroke;
import com.jhlabs.awt.ZigzagStroke;
import pixelitor.filters.gui.EnumParam;
import pixelitor.tools.util.ImDrag;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Stroke;

/**
 * The line type that can be selected in the
 * Stroke Settings dialog of the Shapes Tool.
 * Some members are also used in some brushes.
 */
public enum StrokeType {
    BASIC("Basic", false) {
        @Override
        public Stroke createStroke(float width, int cap, int join, float[] dashFloats) {
            return new BasicStroke(width, cap, join, 1.5f,
                    dashFloats,
                    0.0f);
        }

        @Override
        public int getExtraThickness(int specifiedWidth) {
            return 0;
        }
    }, ZIGZAG("Zigzag", false) {
        private Stroke tmp;

        @Override
        public Stroke createStroke(float width, int cap, int join, float[] dashFloats) {
            tmp = BASIC.createStroke(width, cap, join, dashFloats);
            return new ZigzagStroke(tmp, width, width);
        }

        @Override
        public Stroke getInnerStroke() {
            return tmp;
        }

        @Override
        public int getExtraThickness(int specifiedWidth) {
            return specifiedWidth / 2;
        }
    }, WOBBLE("Wobble", true) {
        private float lastWidth = 0.0f;
        private WobbleStroke wobbleStroke;

        @Override
        public Stroke createStroke(float width, int cap, int join, float[] dashFloats) {
            if (wobbleStroke == null) {
                wobbleStroke = new WobbleStroke(0.5f, width, 10);
                lastWidth = width;
                return wobbleStroke;
            }

            if (width == lastWidth) {
                // avoids calling new WobbleStroke objects, the main benefit is that
                // the seed is not changed when the mouse is released
                return wobbleStroke;
            } else {
                wobbleStroke = new WobbleStroke(0.5f, width, 10);
                lastWidth = width;
                return wobbleStroke;
            }
        }

        @Override
        public int getExtraThickness(int specifiedWidth) {
            return (int) (specifiedWidth * 1.5);
        }
    }, CHARCOAL("Charcoal (can be slow!)", true) {
        @Override
        public Stroke createStroke(float width, int cap, int join, float[] dashFloats) {
            return new CharcoalStroke(width, 0.5f);
        }

        @Override
        public int getExtraThickness(int specifiedWidth) {
            return 0;
        }
    }, BRISTLE("Bristle (can be slow!)", true) {
        @Override
        public Stroke createStroke(float width, int cap, int join, float[] dashFloats) {
            return new BristleStroke(width, 0.5f);
        }

        @Override
        public int getExtraThickness(int specifiedWidth) {
            return 0;
        }
    }, OUTLINE("Outline", false) {
        @Override
        public Stroke createStroke(float width, int cap, int join, float[] dashFloats) {
            return new CompositeStroke(
                    new BasicStroke(width, cap, join),
                    innerOutlineStroke);
        }

        @Override
        public Stroke getInnerStroke() {
            return innerOutlineStroke;
        }

        @Override
        public int getExtraThickness(int specifiedWidth) {
            return 0;
        }
    }, CALLIGRAPHY("Calligraphy", false) {
        @Override
        public Stroke createStroke(float width, int cap, int join, float[] dashFloats) {
            return new CalligraphyStroke(width);
        }

        @Override
        public int getExtraThickness(int specifiedWidth) {
            return 0;
        }
    }, SHAPE("Shape", false) {
        private ShapeType shapeType;

        @Override
        public Stroke createStroke(float width, int cap, int join, float[] dashFloats) {
            float advance = width * 1.2f;
            if (dashFloats != null) {
                advance *= 2.0f; // simulate dashes
            }
            int size = (int) width;
            Shape shape = getShape(size);
            return new ShapeStroke(shape, advance);
        }

        public Shape getShape(int size) {
            if (shapeType == null) {
                shapeType = ShapeType.KIWI;
            }
            return shapeType.createShape(new ImDrag(0, 0, size, size));
        }

        @Override
        public void setShapeType(ShapeType shapeType) {
            this.shapeType = shapeType;
        }

        @Override
        public int getExtraThickness(int specifiedWidth) {
            return 0;
        }
    };

    public static final String NAME = "Line Type";
    private static final float OUTLINE_WIDTH = 1.0f;
    private static final BasicStroke innerOutlineStroke = new BasicStroke(OUTLINE_WIDTH);

    private final String guiName;
    private final boolean slow;

    StrokeType(String guiName, boolean slow) {
        this.guiName = guiName;
        this.slow = slow;
    }

    /**
     * Composite strokes have an inner stroke.
     * They can return here a non-null value
     */
    public Stroke getInnerStroke() {
        return null;
    }

    public void setShapeType(ShapeType shapeType) {
        // do nothing by default, overridden in SHAPE
    }

    public abstract Stroke createStroke(float width, int cap, int join, float[] dashFloats);

    public boolean isSlow() {
        return slow;
    }

    /**
     * Return the real thickness (for the undo), which can be bigger
     * than the specified width.
     */
    public abstract int getExtraThickness(int specifiedWidth);

    public static EnumParam<StrokeType> asParam() {
        return new EnumParam<>(NAME, StrokeType.class);
    }

    @Override
    public String toString() {
        return guiName;
    }
}

