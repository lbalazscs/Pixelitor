/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

/**
 * The implementation of the {@link LittlePlanet} filter.
 * This is actually a rectangular -> polar filter with some extra features.
 */
public class LittlePlanetFilter extends CenteredTransformFilter {
    private double rotationAngle;
    private double zoom;
    private float innerZoom;
    private boolean inverted = false;

    public LittlePlanetFilter() {
        super(LittlePlanet.NAME);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        double dx = x - cx;
        double dy = cy - y;
        double r = Math.sqrt(dx * dx + dy * dy);

        double maxRadius = srcHeight * zoom / 2;
        double angle = Geometry.atan2ToIntuitive(FastMath.atan2(dy, dx)) + rotationAngle;

        if (angle > 2 * Math.PI) {
            angle -= 2 * Math.PI;
        }
        float srcX = (float) (angle * srcWidth / (2 * Math.PI));
        float radiusRatio = (float) (r / maxRadius);
        float biasedRadiusRatio = ImageMath.bias(radiusRatio, innerZoom);
        float srcY = biasedRadiusRatio * srcHeight;
        if (!inverted) {
            srcY = srcHeight - srcY;
        }

        out[0] = srcX;
        out[1] = srcY;
    }

    public void setRotationAngle(double turn) {
        rotationAngle = turn;
    }

    public void setZoom(double zoom) {
        this.zoom = zoom;
    }

    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }

    public void setInnerZoom(double innerZoom) {
        this.innerZoom = (float) (1 - innerZoom / 2.0);
    }
}
