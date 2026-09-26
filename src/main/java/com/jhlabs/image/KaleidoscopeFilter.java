/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jhlabs.image;

import net.jafama.FastMath;

import java.awt.geom.Point2D;

import static com.jhlabs.image.ImageMath.INV_TAU;

/**
 * A filter that produces the effect of looking into a kaleidoscope:
 * a narrow angular wedge of the source image is repeatedly mirrored and
 * rotated around a center point to build a symmetric, tiled pattern.
 * Optionally supports radial tiling (concentric mirrored rings) and a
 * lens effect (radial bulge or pinch) that is applied before the folding,
 * so the center of the pattern looks like a glass lens or crystal ball.
 */
public class KaleidoscopeFilter extends TransformFilter {
    private final float angle;
    private final float rotation;

    private final double invSectorAngle; // sides / 2π

    // the center in pixel coordinates
    private final double cx;
    private final double cy;

    private final float zoom;
    private final double twist;

    private final int rings;
    private final double bandWidth;

    // lens effect
    private final double lensStrength;
    private final double lensRadius;
    private final double invLensRadius;
    private final boolean lensActive;

    /**
     * Constructs a KaleidoscopeFilter.
     *
     * @param filterName    the name of the filter.
     * @param edgeAction    the edge handling strategy (TRANSPARENT, REPEAT_EDGE, WRAP_AROUND, REFLECT).
     * @param interpolation the interpolation method (NEAREST_NEIGHBOR, BILINEAR, BICUBIC).
     * @param angle         the angle of the kaleidoscope.
     * @param rotation      the secondary angle of the kaleidoscope (rotates the result).
     * @param sides         the number of sides of the kaleidoscope.
     * @param center        the center of the effect in pixels.
     * @param zoom          the zoom factor applied to the kaleidoscope effect.
     * @param refRadius     the reference radius (in pixels). Also used as the radius of the lens.
     * @param twist         the spiral twist factor between -1 and 1 (0: no spiral effect).
     * @param rings         the number of concentric mirrored rings (0 to 10, 0: no radial tiling).
     * @param lensStrength  the lens strength (pinch-bulge effect) between -1 and 1 (0: no lens effect).
     */
    public KaleidoscopeFilter(String filterName, int edgeAction, int interpolation,
                              float angle, float rotation, int sides,
                              Point2D center, float zoom,
                              double refRadius, double twist,
                              int rings, double lensStrength) {
        super(filterName, edgeAction, interpolation);

        this.angle = angle;
        this.rotation = rotation;
        this.invSectorAngle = sides * INV_TAU;
        this.cx = center.getX();
        this.cy = center.getY();
        this.zoom = zoom;
        this.twist = Math.TAU * twist / refRadius;
        this.rings = rings;
        this.bandWidth = (rings > 0 && refRadius > 0.0) ? refRadius / rings : 0.0;

        this.lensStrength = ImageMath.clamp(lensStrength, -1.0, 1.0);
        this.lensRadius = refRadius;
        this.invLensRadius = refRadius > 0.0 ? 1.0 / refRadius : 0.0;
        this.lensActive = this.lensStrength != 0.0 && refRadius > 0.0;
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        // polar coordinates
        double dx = x - cx;
        double dy = y - cy;
        double r = lensWarp(Math.sqrt(dx * dx + dy * dy)); // the lens only changes the radius, not the angle
        double rawTheta = FastMath.atan2(dy, dx) - r * twist - angle - rotation;

        // radial tiling (concentric mirrored rings)
        double rTiled = r;
        if (rings > 0 && bandWidth > 0.0) {
            double u = r / bandWidth;
            rTiled = ImageMath.triangle(u) * bandWidth;
        }

        // create kaleidoscope effect by repeating angular segments
        double foldedTheta = ImageMath.triangle(rawTheta * invSectorAngle);

        double finalTheta = foldedTheta + angle; // apply final rotation
        double zoomedR = rTiled / zoom; // apply final zooming

        // convert back to Cartesian coordinates
        out[0] = (float) (cx + zoomedR * FastMath.cos(finalTheta));
        out[1] = (float) (cy + zoomedR * FastMath.sin(finalTheta));
    }

    /**
     * Warps the radius to simulate a lens. Inside the lens radius R, with ρ = r / R:
     * <pre>
     *   r' = r * (1 - s * (1 - ρ)²)
     * </pre>
     * For positive s (bulge) the source radius is smaller than the output radius,
     * so the center is magnified by 1 / (1 - s). For negative s (pinch) it is shrunk.
     * The function is continuous and has slope 1 at ρ = 1, so it blends into the
     * unwarped area outside the lens without a visible seam.
     * It is monotonic for s in [-1, 1], so the warp never folds back on itself.
     */
    private double lensWarp(double r) {
        if (!lensActive || r >= lensRadius) {
            return r;
        }
        double d = 1.0 - r * invLensRadius;
        return r * (1.0 - lensStrength * d * d);
    }
}
