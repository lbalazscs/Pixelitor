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

package pixelitor.tools.shapes;

import pixelitor.tools.gradient.paints.AngleGradientPaint;
import pixelitor.tools.gradient.paints.DiamondGradientPaint;
import pixelitor.tools.gradient.paints.SpiralGradientPaint;
import pixelitor.tools.util.ImDrag;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import static java.awt.AlphaComposite.DST_OUT;
import static java.awt.AlphaComposite.SRC_OVER;
import static java.awt.MultipleGradientPaint.ColorSpaceType.SRGB;
import static java.awt.MultipleGradientPaint.CycleMethod.NO_CYCLE;
import static pixelitor.colors.FgBgColors.getBGColor;
import static pixelitor.colors.FgBgColors.getFGColor;

/**
 * A Paint type based on two endpoints.
 */
public enum TwoPointPaintType {
    NONE("None") {
        @Override
        protected Paint createPaint(ImDrag imDrag) {
            throw new UnsupportedOperationException();
        }
    }, FOREGROUND("Foreground") {
        @Override
        protected Paint createPaint(ImDrag imDrag) {
            return getFGColor();
        }
    }, BACKGROUND("Background") {
        @Override
        protected Paint createPaint(ImDrag imDrag) {
            return getBGColor();
        }
    }, TRANSPARENT("Transparent") {
        @Override
        protected Paint createPaint(ImDrag imDrag) {
            return Color.WHITE; // does not matter
        }

        @Override
        public void prepare(Graphics2D g, ImDrag imDrag) {
            g.setComposite(AlphaComposite.getInstance(DST_OUT));
        }

        @Override
        public void finish(Graphics2D g) {
            g.setComposite(AlphaComposite.getInstance(SRC_OVER));
        }
    }, LINEAR_GRADIENT("Linear Gradient") {
        @Override
        protected Paint createPaint(ImDrag imDrag) {
            return new GradientPaint(
                (float) imDrag.getStartXFromCenter(),
                (float) imDrag.getStartYFromCenter(),
                getFGColor(),
                (float) imDrag.getEndX(),
                (float) imDrag.getEndY(),
                getBGColor());
        }
    }, RADIAL_GRADIENT("Radial Gradient") {
        private final float[] FRACTIONS = {0.0f, 1.0f};
        private final AffineTransform gradientTransform = new AffineTransform();

        @Override
        protected Paint createPaint(ImDrag imDrag) {
            Point2D center = imDrag.getCenterPoint();
            float distance = (float) imDrag.getDistance();

            return new RadialGradientPaint(center, distance / 2, center, FRACTIONS,
                new Color[]{getFGColor(), getBGColor()},
                NO_CYCLE, SRGB, gradientTransform);
        }
    }, ANGLE_GRADIENT("Angle Gradient") {
        @Override
        protected Paint createPaint(ImDrag imDrag) {
            return new AngleGradientPaint(imDrag.getCenterDrag(),
                getFGColor(), getBGColor(), NO_CYCLE);
        }
    }, SPIRAL_GRADIENT("Spiral Gradient") {
        @Override
        protected Paint createPaint(ImDrag imDrag) {
            return new SpiralGradientPaint(true, imDrag.getCenterDrag(),
                getFGColor(), getBGColor(), NO_CYCLE);
        }
    }, DIAMOND_GRADIENT("Diamond Gradient") {
        @Override
        protected Paint createPaint(ImDrag imDrag) {
            return new DiamondGradientPaint(imDrag.getCenterDrag(),
                getFGColor(), getBGColor(), NO_CYCLE);
        }
    };

    private final String guiName;

    TwoPointPaintType(String guiName) {
        this.guiName = guiName;
    }

    protected abstract Paint createPaint(ImDrag imDrag);

    /**
     * Called before the drawing/filling
     */
    public void prepare(Graphics2D g, ImDrag imDrag) {
        g.setPaint(createPaint(imDrag));
    }

    /**
     * Called after the drawing/filling
     */
    public void finish(Graphics2D g) {
        // by default do nothing
    }

    @Override
    public String toString() {
        return guiName;
    }
}
