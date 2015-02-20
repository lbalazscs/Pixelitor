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

import pixelitor.tools.brushes.Brush;
import pixelitor.tools.brushes.BrushShapeProvider;
import pixelitor.tools.brushes.CalligraphyBrush;
import pixelitor.tools.brushes.IdealBrush;
import pixelitor.tools.brushes.ImageBrushType;
import pixelitor.tools.brushes.ImageDabsBrush;
import pixelitor.tools.brushes.OutlineCircleBrush;
import pixelitor.tools.brushes.OutlineSquareBrush;
import pixelitor.tools.brushes.ShapeDabsBrush;
import pixelitor.tools.brushes.WobbleBrush;

import java.util.function.Supplier;

/**
 * The brush types the user can use
 */
enum BrushType implements Supplier<Brush> {
    IDEAL("Hard") {
        @Override
        public Brush get() {
            return new IdealBrush();
        }
    }, SOFT("Soft") {
        @Override
        public Brush get() {
            return new ImageDabsBrush(ImageBrushType.SOFT, 0.25, false);
        }
    }, WOBBLE("Wobble") {
        @Override
        public Brush get() {
            return new WobbleBrush();
        }
    }, CALLIGRAPHY("Calligraphy") {
        @Override
        public Brush get() {
            return new CalligraphyBrush();
        }
    }, REALISTIC("Realistic") {
        @Override
        public Brush get() {
            return new ImageDabsBrush(ImageBrushType.REAL, 0.05, false);
        }
    }, HAIR("Hair") {
        @Override
        public Brush get() {
            return new ImageDabsBrush(ImageBrushType.HAIR, 0.02, false);
        }
    }, HEART("Heart") {
        @Override
        public Brush get() {
            return new ShapeDabsBrush(BrushShapeProvider.HEART, 2.3);
        }
//    }, ARROW("Image-Based Arrow") {
//        @Override
//        public Brush get() {
//            return new ImageDabsBrush(ImageBrushType.ARROW, 2.5, true);
//        }
//    }, GREEK("Image-Based Greek") {
//        @Override
//        public Brush get() {
//            return new ImageDabsBrush(ImageBrushType.GREEK, 2.0, true);
//        }
    }, OUTLINE_CIRCLE("Circles") {
        @Override
        public Brush get() {
            return new OutlineCircleBrush();
        }
    }, OUTLINE_SQUARE("Squares") {
        @Override
        public Brush get() {
            return new OutlineSquareBrush();
        }
    };

    private final String guiName;

    BrushType(String guiName) {
        this.guiName = guiName;
    }


    @Override
    public String toString() {
        return guiName;
    }
}
