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

package pixelitor.tools.shapes;

import pixelitor.tools.gradient.paints.AngleGradientPaint;
import pixelitor.tools.gradient.paints.DiamondGradientPaint;
import pixelitor.tools.gradient.paints.SpiralGradientPaint;
import pixelitor.tools.util.Drag;

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
    NONE("None", false) {
        @Override
        protected Paint createPaint(Drag drag, Color fgColor, Color bgColor) {
            throw new UnsupportedOperationException();
        }
    }, FOREGROUND("Foreground", false) {
        @Override
        protected Paint createPaint(Drag drag, Color fgColor, Color bgColor) {
            return fgColor;
        }
    }, BACKGROUND("Background", false) {
        @Override
        protected Paint createPaint(Drag drag, Color fgColor, Color bgColor) {
            return bgColor;
        }
    }, ERASE("Erase", false) {
        @Override
        protected Paint createPaint(Drag drag, Color fgColor, Color bgColor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void prepare(Graphics2D g, Drag drag, Color fgColor, Color bgColor) {
            g.setComposite(AlphaComposite.getInstance(DST_OUT));
        }

        @Override
        public void finish(Graphics2D g) {
            g.setComposite(AlphaComposite.getInstance(SRC_OVER));
        }
    }, LINEAR_GRADIENT("Linear Gradient", true) {
        @Override
        protected Paint createPaint(Drag drag, Color fgColor, Color bgColor) {
            return new GradientPaint(
                (float) drag.getStartXFromCenter(),
                (float) drag.getStartYFromCenter(),
                fgColor,
                (float) drag.getEndX(),
                (float) drag.getEndY(),
                bgColor);
        }
    }, RADIAL_GRADIENT("Radial Gradient", true) {
        private final float[] FRACTIONS = {0.0f, 1.0f};
        private final AffineTransform gradientTransform = new AffineTransform();

        @Override
        protected Paint createPaint(Drag drag, Color fgColor, Color bgColor) {
            Point2D center = drag.getCenterPoint();
            float distance = (float) drag.calcImDist();

            return new RadialGradientPaint(center, distance / 2, center, FRACTIONS,
                new Color[]{fgColor, bgColor},
                NO_CYCLE, SRGB, gradientTransform);
        }
    }, ANGLE_GRADIENT("Angle Gradient", false) {
        @Override
        protected Paint createPaint(Drag drag, Color fgColor, Color bgColor) {
            return new AngleGradientPaint(drag.getCenterDrag(),
                fgColor, bgColor, NO_CYCLE);
        }
    }, SPIRAL_GRADIENT("Spiral Gradient", false) {
        @Override
        protected Paint createPaint(Drag drag, Color fgColor, Color bgColor) {
            return new SpiralGradientPaint(true, drag.getCenterDrag(),
                fgColor, bgColor, NO_CYCLE);
        }
    }, DIAMOND_GRADIENT("Diamond Gradient", false) {
        @Override
        protected Paint createPaint(Drag drag, Color fgColor, Color bgColor) {
            return new DiamondGradientPaint(drag.getCenterDrag(),
                fgColor, bgColor, NO_CYCLE);
        }
    };

    private final String guiName;

    // true if when blending it with a custom blending mode,
    // the src has no transparency, and the blending modes don't work
    private final boolean hasBlendingIssue;

    TwoPointPaintType(String guiName, boolean hasBlendingIssue) {
        this.guiName = guiName;
        this.hasBlendingIssue = hasBlendingIssue;
    }

    protected final Paint createPaint(Drag drag) {
        return createPaint(drag, getFGColor(), getBGColor());
    }

    protected abstract Paint createPaint(Drag drag, Color fgColor, Color bgColor);

    /**
     * Called before the drawing/filling
     */
    public void prepare(Graphics2D g, Drag drag, Color fgColor, Color bgColor) {
        g.setPaint(createPaint(drag, fgColor, bgColor));
    }

    /**
     * Called after the drawing/filling
     */
    public void finish(Graphics2D g) {
        // by default do nothing
    }

    public boolean hasBlendingIssue() {
        return hasBlendingIssue;
    }

    @Override
    public String toString() {
        return guiName;
    }
}
