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

package pixelitor.filters;

import com.jhlabs.image.TransformFilter;
import pixelitor.filters.gui.*;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

public class DisplacementMap extends ParametrizedFilter {
    public static final String NAME = "Displacement Map";

    private final SelectImageParam imageParam = new SelectImageParam("Displacement Map");
    private final BooleanParam tileParam = new BooleanParam("Tile");
    private final RangeParam amount = new RangeParam(
        "Amount", 0, 10, 100);
    private final AngleParam angle = new AngleParam("Angle", 0);
    private final IntChoiceParam edgeAction = IntChoiceParam.forEdgeAction();
    private final IntChoiceParam interpolation = IntChoiceParam.forInterpolation();

    private Impl filter;

    public DisplacementMap() {
        super(true);

        setParams(
            imageParam,
            tileParam,
            amount.withAdjustedRange(0.2),
            angle,
            edgeAction,
            interpolation
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new Impl();
        }

        filter.setMap(imageParam.getImage());
        filter.setTile(tileParam.isChecked());
        filter.setAngle(angle.getValueInRadians());
        filter.setAmount(amount.getValueAsDouble());
        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        return filter.filter(src, dest);
    }

    @Override
    public boolean canBeSmart() {
        return false;
    }

    private static class Impl extends TransformFilter {
        private int[] mapPixels;
        private int mapWidth;
        private int mapHeight;

        private double amount;
        private boolean tile;
        private double sin;
        private double cos;

        public void setMap(BufferedImage map) {
            mapPixels = ImageUtils.getPixels(map);
            mapWidth = map.getWidth();
            mapHeight = map.getHeight();
        }

        public void setTile(boolean tile) {
            this.tile = tile;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }

        public void setAngle(double angle) {
            sin = Math.sin(angle + Math.PI);
            cos = Math.cos(angle + Math.PI);
        }

        protected Impl() {
            super(NAME);
        }

        @Override
        protected void transformInverse(int x, int y, float[] out) {
            int mapX = x;
            int mapY = y;
            if (tile) {
                mapX = x % mapWidth;
                mapY = y % mapHeight;
            } else {
                if (x >= mapWidth || y >= mapHeight) {
                    out[0] = x;
                    out[1] = y;
                    return;
                }
            }

            int mapRGB = mapPixels[mapY * mapWidth + mapX];
            int r = (mapRGB >>> 16) & 0xFF;
            int g = (mapRGB >>> 8) & 0xFF;
            int b = mapRGB & 0xFF;

            int lum = r + g + b;

            // Max luminosity = 255*3 = 765.
            // Light values move the pixels in the angle arrow's direction,
            // dark values move them in the opposite direction.
            double dist = amount * lum / 765.0 - amount / 2.0;
            double distX = dist * cos;
            double distY = dist * sin;

            out[0] = (float) (x + distX);
            out[1] = (float) (y + distY);
        }
    }
}
