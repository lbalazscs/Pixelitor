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
package pixelitor.filters.impl;

import net.jafama.FastMath;
import pixelitor.filters.EmptyPolar;

/**
 * A template for transform filters with polar coordinates
 */
public class EmptyPolarFilter extends CenteredTransformFilter {
    private float zoom;
    private double rotateResult;

    public EmptyPolarFilter() {
        super(EmptyPolar.NAME);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        float dx = x - cx;
        float dy = y - cy;
        double r = Math.sqrt(dx * dx + dy * dy);
        double angle = FastMath.atan2(dy, dx);
        angle += rotateResult;

        double zoomedR = r / zoom;
        float u = (float) (zoomedR * FastMath.cos(angle));
        float v = (float) (zoomedR * FastMath.sin(angle));

        out[0] = (u + cx);
        out[1] = (v + cy);
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    public void setRotateResult(double rotateResult) {
        this.rotateResult = rotateResult;
    }
}
