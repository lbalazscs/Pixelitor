/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.ImageMath;
import pixelitor.Canvas;
import pixelitor.tools.brushes.SymmetryBrush;
import pixelitor.tools.util.PPoint;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * The symmetry modes for brush tools, defining the transformations
 * applied to points by a {@link SymmetryBrush}.
 */
public enum Symmetry {
    NONE("None"),
    HORIZONTAL_MIRROR("Horizontal", Symmetry::mirrorHorizontally),
    VERTICAL_MIRROR("Vertical", Symmetry::mirrorVertically),
    HOR_AND_VER_MIRROR("Horizontal + Vertical",
        Symmetry::mirrorVertically, Symmetry::mirrorHorizontally, Symmetry::mirrorBoth),
    DIAGONAL_SLASH("Diagonal /", Symmetry::mirrorDiagonalSlash),
    DIAGONAL_BACKSLASH("Diagonal \\", Symmetry::mirrorDiagonalBackslash),
    ROTATION_2("Central Symmetry", Symmetry::mirrorBoth),
    ROTATION_3("Central 3", Symmetry::rotate120, Symmetry::rotate240);

    // parameters of the currently active canvas
    private static double canvasWidth;
    private static double canvasHeight;
    private static double canvasCenterX;
    private static double canvasCenterY;

    // rotation constants for Central 3
    private static final double COS_120 = -0.5;
    private static final double SIN_120 = ImageMath.COS_30; // sin 120° = cos 30°
    private static final double COS_240 = COS_120;
    private static final double SIN_240 = -SIN_120;

    public static final String PRESET_KEY = "Mirror";

    private final String displayName;
    private final List<UnaryOperator<PPoint>> extraTransforms; // for brushes 1..n-1

    Symmetry(String displayName) {
        this.displayName = displayName;
        this.extraTransforms = List.of();
    }

    @SafeVarargs
    Symmetry(String displayName, UnaryOperator<PPoint>... extraTransforms) {
        this.displayName = displayName;
        this.extraTransforms = List.of(extraTransforms);
    }

    /**
     * Updates the canvas dimensions used for symmetry calculations.
     */
    public static void activeCanvasSizeChanged(Canvas canvas) {
        canvasWidth = canvas.getWidth();
        canvasHeight = canvas.getHeight();
        canvasCenterX = canvasWidth / 2.0;
        canvasCenterY = canvasHeight / 2.0;
    }

    /**
     * Transforms the given master (first) point for the given brush index.
     * Index 0 returns the point itself.
     */
    public PPoint transform(PPoint p, int brushIndex) {
        return brushIndex == 0 ? p : extraTransforms.get(brushIndex - 1).apply(p);
    }

    /**
     * Returns the number of brushes required for this symmetry mode.
     */
    public int getNumBrushes() {
        return 1 + extraTransforms.size();
    }

    private static PPoint mirrorVertically(PPoint p) {
        return PPoint.fromIm(canvasWidth - p.getImX(), p.getImY(), p.getView());
    }

    private static PPoint mirrorHorizontally(PPoint p) {
        return PPoint.fromIm(p.getImX(), canvasHeight - p.getImY(), p.getView());
    }

    private static PPoint mirrorBoth(PPoint p) {
        return PPoint.fromIm(canvasWidth - p.getImX(), canvasHeight - p.getImY(), p.getView());
    }

    private static PPoint mirrorDiagonalSlash(PPoint p) {
        double imX = p.getImX();
        double imY = p.getImY();
        double den = canvasWidth * canvasWidth + canvasHeight * canvasHeight;
        double mirrorImX = ((canvasWidth * canvasWidth - canvasHeight * canvasHeight) * imX - 2 * canvasWidth * canvasHeight * imY + 2 * canvasHeight * canvasHeight * canvasWidth) / den;
        double mirrorImY = (-2 * canvasWidth * canvasHeight * imX + (canvasHeight * canvasHeight - canvasWidth * canvasWidth) * imY + 2 * canvasWidth * canvasWidth * canvasHeight) / den;

        return PPoint.fromIm(mirrorImX, mirrorImY, p.getView());
    }

    private static PPoint mirrorDiagonalBackslash(PPoint p) {
        double imX = p.getImX();
        double imY = p.getImY();
        double den = canvasWidth * canvasWidth + canvasHeight * canvasHeight;
        double mirrorImX = ((canvasWidth * canvasWidth - canvasHeight * canvasHeight) * imX + 2 * canvasWidth * canvasHeight * imY) / den;
        double mirrorImY = (2 * canvasWidth * canvasHeight * imX + (canvasHeight * canvasHeight - canvasWidth * canvasWidth) * imY) / den;

        return PPoint.fromIm(mirrorImX, mirrorImY, p.getView());
    }

    private static PPoint rotate120(PPoint p) {
        return getRotatedPoint(p, COS_120, SIN_120);
    }

    private static PPoint rotate240(PPoint p) {
        return getRotatedPoint(p, COS_240, SIN_240);
    }

    /**
     * Calculates a rotated point around the canvas center.
     */
    private static PPoint getRotatedPoint(PPoint p, double cosTheta, double sinTheta) {
        // coordinates relative to the center
        double relX = p.getImX() - canvasCenterX;
        double relY = canvasCenterY - p.getImY(); // calculate using an upward-pointing Y-axis

        // rotate relative coordinates
        double rotX = relX * cosTheta - relY * sinTheta;
        double rotY = relX * sinTheta + relY * cosTheta;

        // translate back to the original coordinate system
        double finalX = canvasCenterX + rotX;
        double finalY = canvasCenterY - rotY;
        return PPoint.fromIm(finalX, finalY, p.getView());
    }

    @Override
    public String toString() {
        return displayName;
    }
}
