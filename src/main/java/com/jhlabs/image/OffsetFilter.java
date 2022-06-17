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

import java.awt.image.BufferedImage;

public class OffsetFilter extends TransformFilter {
    private int width, height;
    private int xOffset, yOffset;
    private boolean wrap;

    private double relativeX;
    private double relativeY;
    private boolean useRelative = false;

    public OffsetFilter(String filterName) {
        this(0, 0, true, filterName);
    }

    public OffsetFilter(int xOffset, int yOffset, boolean wrap, String filterName) {
        super(filterName);

        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.wrap = wrap;
        setEdgeAction(TRANSPARENT);
    }

    public void setRelativeX(double relativeX) {
        this.relativeX = relativeX;
    }

    public void setRelativeY(double relativeY) {
        this.relativeY = relativeY;
    }

    public double getRelativeX() {
        return relativeX;
    }

    public double getRelativeY() {
        return relativeY;
    }

    public boolean isUseRelative() {
        return useRelative;
    }

    /**
     * When useRelative is set, the relative settings overwrite the absolute settings
     *
     * @param useRelative
     */
    public void setUseRelative(boolean useRelative) {
        this.useRelative = useRelative;
    }

    public void setXOffset(int xOffset) {
        this.xOffset = xOffset;
    }

    public int getXOffset() {
        return xOffset;
    }

    public void setYOffset(int yOffset) {
        this.yOffset = yOffset;
    }

    public int getYOffset() {
        return yOffset;
    }

    public void setWrap(boolean wrap) {
        this.wrap = wrap;
    }

    public boolean getWrap() {
        return wrap;
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        if (wrap) {
            out[0] = (x + width - xOffset) % width;
            out[1] = (y + height - yOffset) % height;
        } else {
            out[0] = x - xOffset;
            out[1] = y - yOffset;
        }
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        width = src.getWidth();
        height = src.getHeight();

        if (useRelative) {
            xOffset = (int) (width * relativeX);
            yOffset = (int) (height * relativeY);
        }

        if (wrap) {
            while (xOffset < 0) {
                xOffset += width;
            }
            while (yOffset < 0) {
                yOffset += height;
            }
            xOffset %= width;
            yOffset %= height;
        }
        return super.filter(src, dst);
    }

    @Override
    public String toString() {
        return "Distort/Offset...";
    }
}
