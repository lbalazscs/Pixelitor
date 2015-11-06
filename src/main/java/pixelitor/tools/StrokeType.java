/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

package pixelitor.tools;

import com.bric.awt.BristleStroke;
import com.bric.awt.CalligraphyStroke;
import com.bric.awt.CharcoalStroke;
import com.jhlabs.awt.CompositeStroke;
import com.jhlabs.awt.ShapeStroke;
import com.jhlabs.awt.WobbleStroke;
import com.jhlabs.awt.ZigzagStroke;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Stroke;

/**
 * The line type that can be selected in the Stroke Settings dialog in the Shapes Tool.
 * Some members are also used in some brushes.
 */
public enum StrokeType {
    BASIC("Basic") {
        @Override
        public Stroke getStroke(float width, int cap, int join, float[] dashFloats) {
            Stroke stroke = new BasicStroke(width, cap, join, 1.5f,
                    dashFloats,
                    0.0f);
            return stroke;
        }

        @Override
        public int getExtraWidth(int specifiedWidth) {
            return 0;
        }
    }, ZIGZAG("Zigzag") {
        private Stroke tmp;

        @Override
        public Stroke getStroke(float width, int cap, int join, float[] dashFloats) {
            tmp = BASIC.getStroke(width, cap, join, dashFloats);
            Stroke stroke = new ZigzagStroke(tmp, width, width);
            return stroke;
        }

        @Override
        public Stroke getInnerStroke() {
            return tmp;
        }

        @Override
        public int getExtraWidth(int specifiedWidth) {
            return specifiedWidth / 2;
        }
    }, WOBBLE("Wobble") {
        private float lastWidth = 0.0f;
        private WobbleStroke wobbleStroke;

        @Override
        public Stroke getStroke(float width, int cap, int join, float[] dashFloats) {
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

        @SuppressWarnings("RedundantMethodOverride")
        @Override
        public Stroke getInnerStroke() {
            return null; // TODO this should have one
        }

        @Override
        public int getExtraWidth(int specifiedWidth) {
            return (int) (specifiedWidth * 1.5);
        }
    }, CHARCOAL("Charcoal (can be slow!)") {
        @Override
        public Stroke getStroke(float width, int cap, int join, float[] dashFloats) {
            return new CharcoalStroke(width, 0.5f);
        }

        @Override
        public int getExtraWidth(int specifiedWidth) {
            return 0;
        }
    }, BRISTLE("Bristle (can be slow!)") {
        @Override
        public Stroke getStroke(float width, int cap, int join, float[] dashFloats) {
            return new BristleStroke(width, 0.5f);
        }

        @Override
        public int getExtraWidth(int specifiedWidth) {
            return 0;
        }
    }, OUTLINE("Outline") {
        @Override
        public Stroke getStroke(float width, int cap, int join, float[] dashFloats) {
            Stroke stroke = new CompositeStroke(
                    new BasicStroke(width, cap, join),
                    innerOutlineStroke);
            return stroke;
        }

        @Override
        public Stroke getInnerStroke() {
            return innerOutlineStroke;
        }

        @Override
        public int getExtraWidth(int specifiedWidth) {
            return 0;
        }
    }, CALLIGRAPHY("Calligraphy") {
        @Override
        public Stroke getStroke(float width, int cap, int join, float[] dashFloats) {
            return new CalligraphyStroke(width);
        }

        @Override
        public int getExtraWidth(int specifiedWidth) {
            return 0;
        }
    }, SHAPE("Shape") {
        private ShapeType shapeType;

        @Override
        public Stroke getStroke(float width, int cap, int join, float[] dashFloats) {
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
            return shapeType.getShape(new UserDrag(0, 0, size, size));
        }

        @Override
        public void setShapeType(ShapeType shapeType) {
            this.shapeType = shapeType;
        }

        @Override
        public int getExtraWidth(int specifiedWidth) {
            return 0;
        }
    };

    private static final float OUTLINE_WIDTH = 1.0f;
    private final static BasicStroke innerOutlineStroke = new BasicStroke(OUTLINE_WIDTH);

    private final String guiName;

    StrokeType(String guiName) {
        this.guiName = guiName;
    }

    /**
     * Some strokes have an inner stoke. They can return here a non-null value
     *
     * @return
     */
    public Stroke getInnerStroke() {
        return null;
    }

    public void setShapeType(ShapeType shapeType) {
        // do nothing by default, overridden in SHAPE
    }

    public abstract Stroke getStroke(float width, int cap, int join, float[] dashFloats);

    /**
     * The real thickness of some strokes (which is relevant for the undo) is bigger than
     * the the specified width. This method returns the extra width.
     */
    public abstract int getExtraWidth(int specifiedWidth);

//    /**
//     * A simple getter for the brushes
//     */
//    public Stroke getStroke(float width) {
//        return getStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, null);
//    }

    @Override
    public String toString() {
        return guiName;
    }
}

