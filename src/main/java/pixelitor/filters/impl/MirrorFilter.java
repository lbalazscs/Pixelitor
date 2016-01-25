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

import pixelitor.filters.Mirror;

/**
 * Mirror filter implementation
 */
public class MirrorFilter extends CenteredTransformFilter {

    public static final int LEFT_OVER_RIGHT = 0;
    public static final int RIGHT_OVER_LEFT = 1;
    public static final int BOTTOM_OVER_TOP = 2;
    public static final int TOP_OVER_BOTTOM = 3;

    private int type;

    public MirrorFilter() {
        super(Mirror.NAME);
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        switch (type) {
            case LEFT_OVER_RIGHT:
                if (x < cx) {
                    out[0] = x;
                    out[1] = y;
                } else {
                    out[0] = cx + cx - x;
                    out[1] = y;
                }
                break;
            case RIGHT_OVER_LEFT:
                if (x > cx) {
                    out[0] = x;
                    out[1] = y;
                } else {
                    out[0] = cx + cx - x;
                    out[1] = y;
                }
                break;
            case TOP_OVER_BOTTOM:
                if (y < cy) {
                    out[0] = x;
                    out[1] = y;
                } else {
                    out[0] = x;
                    out[1] = cy + cy - y;
                }
                break;
            case BOTTOM_OVER_TOP:
                if (y > cy) {
                    out[0] = x;
                    out[1] = y;
                } else {
                    out[0] = x;
                    out[1] = cy + cy - y;
                }
                break;
        }

    }
}
