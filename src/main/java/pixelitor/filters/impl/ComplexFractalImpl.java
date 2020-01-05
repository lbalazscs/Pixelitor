/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.PointFilter;

import java.awt.image.BufferedImage;

public abstract class ComplexFractalImpl extends PointFilter {
    // the bounds in the complex space
    private final double cxMin;
    private final double cxMax;
    private final double cyMin;
    private final double cyMax;
    private final double cxRange;
    private final double cyRange;

    // the actual start in the complex space,
    // taking the zooming into account
    protected double cxStart;
    protected double cyStart;

    // multipliers for translating image
    // coordinates into complex coordinates
    protected double xMultiplier;
    protected double yMultiplier;

    private double zoomCenterX = 0.5f;
    private double zoomCenterY = 0.5f;

    private int maxIterations = 570;
    private double zoom = 1.0f;

    protected int[] colors;

    protected ComplexFractalImpl(String filterName, double cxMin, double cxMax, double cyMin, double cyMax) {
        super(filterName);

        this.cxMin = cxMin;
        this.cxMax = cxMax;
        this.cyMin = cyMin;
        this.cyMax = cyMax;

        cxRange = cxMax - cxMin;
        cyRange = cyMax - cyMin;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        double zoomedRangeCX = cxRange / zoom;
        double zoomedRangeCY = cyRange / zoom;

        xMultiplier = zoomedRangeCX / src.getWidth();
        yMultiplier = zoomedRangeCY / src.getHeight();

        // the zoom center in the "C-space"
        double zoomCenterCX = cxMin + zoomCenterX * cxRange;
        double zoomCenterCY = cyMin + zoomCenterY * cyRange;

        double cxZoomedMin = zoomCenterCX - zoomedRangeCX / 2;
        double cxZoomedMax = zoomCenterCX + zoomedRangeCX / 2;
        double cyZoomedMin = zoomCenterCY - zoomedRangeCY / 2;
        double cyZoomedMax = zoomCenterCY + zoomedRangeCY / 2;

        // if the zoomed range would go outside of
        // the default c range, then adjust it back
        if(cxZoomedMax > cxMax) {
            cxStart = cxZoomedMin - (cxZoomedMax - cxMax);
        } else if(cxZoomedMin < cxMin) {
            cxStart = cxMin;
        } else {
            cxStart =  cxZoomedMin;
        }

        if(cyZoomedMax > cyMax) {
            cyStart = cyZoomedMin - (cyZoomedMax - cyMax);
        } else if(cyZoomedMin < cyMin) {
            cyStart = cyMin;
        } else {
            cyStart =  cyZoomedMin;
        }

        return super.filter(src, dst);
    }

    protected int calcIteratedColor(double zx, double zy, double cx, double cy) {
        int it = iterate(zx, zy, cx, cy, maxIterations);
        return colors[it];
    }

    private static int iterate(double x, double y, double cx, double cy, int maxIt) {
        int it = maxIt;
        double x2 = 0;
        double y2 = 0;
        double xy;
        while (x2 + y2 <= 4 && it > 0) {
            it--;
            xy = x * y;
            x2 = x * x;
            y2 = y * y;
            x = x2 - y2 + cx;
            y = xy + xy + cy;
        }
        return it;
    }

    private static int iterateUnOpt(double x, double y, double cx, double cy, int maxIt) {
        int it = maxIt;
        while (x * x + y * y < 4 && it > 0) {
            it--;
            double xTmp = x * x - y * y + cx;
            double yTmp = 2.0 * x * y + cy;
            if (x == xTmp && y == yTmp) {
                it = 0;
                break;
            }
            y = yTmp;
            x = xTmp;
        }
        return it;
    }

    public void setZoom(double zoom) {
        this.zoom = zoom;
    }

    public void setColors(int[] colors) {
        this.colors = colors;
    }

    public void setZoomCenter(double zoomCenterX, double zoomCenterY) {
        this.zoomCenterX = zoomCenterX;
        this.zoomCenterY = zoomCenterY;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }
}
