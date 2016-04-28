/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

package pixelitor.tools.shapestool;

import pixelitor.colors.FgBgColors;
import pixelitor.tools.UserDrag;
import pixelitor.tools.gradientpaints.AngleGradientPaint;
import pixelitor.tools.gradientpaints.DiamondGradientPaint;
import pixelitor.tools.gradientpaints.SpiralGradientPaint;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import static java.awt.AlphaComposite.DST_OUT;
import static java.awt.AlphaComposite.SRC_OVER;

/**
 * A Paint type based on two endpoints of a UserDrag.
 * Used as a fill type in the Shapes Tool.
 */
enum TwoPointBasedPaint {
    LINEAR_GRADIENT {
        @Override
        protected Paint getPaint(UserDrag ud) {
            Color fgColor = FgBgColors.getFG();
            Color bgColor = FgBgColors.getBG();

            return new GradientPaint(
                    (float)ud.getStartXFromCenter(),
                    (float)ud.getStartYFromCenter(),
                    fgColor,
                    (float)ud.getEndX(),
                    (float)ud.getEndY(),
                    bgColor);
        }

        @Override
        public String toString() {
            return "Linear Gradient";
        }
    }, RADIAL_GRADIENT {
        private final float[] FRACTIONS = {0.0f, 1.0f};
        private final AffineTransform gradientTransform = new AffineTransform();

        @Override
        protected Paint getPaint(UserDrag userDrag) {
            Color fgColor = FgBgColors.getFG();
            Color bgColor = FgBgColors.getBG();

            Point2D center = userDrag.getCenterPoint();
            float distance = (float) userDrag.getDistance();

            return new RadialGradientPaint(center, distance / 2, center, FRACTIONS, new Color[]{fgColor, bgColor},
                    MultipleGradientPaint.CycleMethod.NO_CYCLE, MultipleGradientPaint.ColorSpaceType.SRGB, gradientTransform);
        }

        @Override
        public String toString() {
            return "Radial Gradient";
        }
    }, ANGLE_GRADIENT {
        @Override
        protected Paint getPaint(UserDrag userDrag) {
            Color fgColor = FgBgColors.getFG();
            Color bgColor = FgBgColors.getBG();

            Point2D center = userDrag.getCenterPoint();
            UserDrag centerUserDrag = new UserDrag(center.getX(), center.getY(), userDrag.getEndX(), userDrag.getEndY());

            Paint paint = new AngleGradientPaint(centerUserDrag, fgColor, bgColor, MultipleGradientPaint.CycleMethod.NO_CYCLE);
            return paint;
        }

        @Override
        public String toString() {
            return "Angle Gradient";
        }
    }, SPIRAL_GRADIENT {
        @Override
        protected Paint getPaint(UserDrag userDrag) {
            Color fgColor = FgBgColors.getFG();
            Color bgColor = FgBgColors.getBG();

            Point2D center = userDrag.getCenterPoint();
            UserDrag centerUserDrag = new UserDrag(center.getX(), center.getY(), userDrag.getEndX(), userDrag.getEndY());

            Paint paint = new SpiralGradientPaint(true, centerUserDrag, fgColor, bgColor, MultipleGradientPaint.CycleMethod.NO_CYCLE);
            return paint;
        }

        @Override
        public String toString() {
            return "Spiral Gradient";
        }
    }, DIAMOND_GRADIENT {
        @Override
        protected Paint getPaint(UserDrag userDrag) {
            Color fgColor = FgBgColors.getFG();
            Color bgColor = FgBgColors.getBG();

            Point2D center = userDrag.getCenterPoint();
            UserDrag centerUserDrag = new UserDrag(center.getX(), center.getY(), userDrag.getEndX(), userDrag.getEndY());

            Paint paint = new DiamondGradientPaint(centerUserDrag, fgColor, bgColor, MultipleGradientPaint.CycleMethod.NO_CYCLE);
            return paint;
        }

        @Override
        public String toString() {
            return "Diamond Gradient";
        }
    }, FOREGROUND {
        @Override
        protected Paint getPaint(UserDrag userDrag) {
            Color fgColor = FgBgColors.getFG();
            return fgColor;
        }

        @Override
        public String toString() {
            return "Foreground";
        }
    }, BACKGROUND {
        @Override
        protected Paint getPaint(UserDrag userDrag) {
            Color bgColor = FgBgColors.getBG();
            return bgColor;
        }

        @Override
        public String toString() {
            return "Background";
        }
    }, TRANSPARENT {
        @Override
        protected Paint getPaint(UserDrag userDrag) {
            return Color.WHITE; // does not matter
        }

        @Override
        public void setupPaint(Graphics2D g, UserDrag userDrag) {
            g.setComposite(AlphaComposite.getInstance(DST_OUT));
        }

        @Override
        public void restorePaint(Graphics2D g) {
            g.setComposite(AlphaComposite.getInstance(SRC_OVER));
        }

        @Override
        public String toString() {
            return "Transparent";
        }
    };

    protected abstract Paint getPaint(UserDrag userDrag);

    public void setupPaint(Graphics2D g, UserDrag userDrag) {
        g.setPaint(getPaint(userDrag));
    }

    public void restorePaint(Graphics2D g) {
    }
}
