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

package pixelitor.filters.impl;

import com.jhlabs.image.ImageMath;
import net.jafama.FastMath;
import pixelitor.filters.LittlePlanet;
import pixelitor.utils.Geometry;

import java.awt.geom.Point2D;

/**
 * The implementation of the {@link LittlePlanet} filter.
 * This is actually a rectangular -> polar filter with some extra features.
 */
public class LittlePlanetFilter extends CenteredTransformFilter {
    private final double rotationAngle;
    private final double zoom;
    private final float innerZoom;
    private final boolean inverted;

    /**
     * Constructs a new LittlePlanetFilter.
     *
     * @param filterName    the name of the filter.
     * @param edgeAction    the edge handling strategy (TRANSPARENT, REPEAT_EDGE, WRAP_AROUND, REFLECT).
     * @param interpolation the interpolation method (NEAREST_NEIGHBOR, BILINEAR, BICUBIC).
     * @param center        the effect's center (in pixels).
     * @param rotationAngle the angle (in radians) to rotate the resulting "planet".
     * @param zoom          the global zoom level (percentage).
     * @param innerZoom     the inner zoom percentage, used to bias the radial transformation.
     * @param inverted      if true, the polar mapping is inverted.
     */
    public LittlePlanetFilter(String filterName, int edgeAction, int interpolation, Point2D center,
                              double rotationAngle, double zoom, double innerZoom, boolean inverted) {
        super(filterName, edgeAction, interpolation, center);

        this.rotationAngle = rotationAngle;
        this.zoom = zoom;

        // converts percentage to bias float
        this.innerZoom = (float) (1 - innerZoom / 2.0);
        this.inverted = inverted;
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        double dx = x - cx;
        double dy = cy - y;
        double r = Math.sqrt(dx * dx + dy * dy);

        double maxRadius = height * zoom / 2;
        double angle = Geometry.atan2ToIntuitive(FastMath.atan2(dy, dx)) + rotationAngle;

        if (angle > 2 * Math.PI) {
            angle -= 2 * Math.PI;
        }
        float srcX = (float) (angle * width / (2 * Math.PI));
        float radiusRatio = (float) (r / maxRadius);
        float biasedRadiusRatio = ImageMath.bias(radiusRatio, innerZoom);
        float srcY = biasedRadiusRatio * height;
        if (!inverted) {
            srcY = height - srcY;
        }

        out[0] = srcX;
        out[1] = srcY;
    }
}
