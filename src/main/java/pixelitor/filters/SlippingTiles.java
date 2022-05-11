/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.RangeParam;

import java.awt.image.BufferedImage;

/**
 *
 */
public class SlippingTiles extends ParametrizedFilter {
    public static final String NAME = "Slipping Tiles";

    public enum Distributor {
        EVEN() {
            @Override
            int getNthSegment(float n, float N, float of) {
                return (int) (of / N);
            }
        },
        EXPONENTIAL() {
            private static final double LN_10 = Math.log(10);

            @Override
            int getNthSegment(float n, float N, float of) {
                return (int) (Math.exp(LN_10 * n / (N - 1)) * /*normalising*/ (of * (Math.exp(LN_10 / (N - 1)) - 1) / (Math.exp(LN_10 * N / (N - 1)) - 1)));
            }
        },
        TICK_TOCK() {
            @Override
            int getNthSegment(float n, float N, float of) {
                return (int) (of / N + (((int) n) % 2 == 0 ? 1 : -1) * of / 3 / N + /*normalising*/ ((N % 2 == 1 && n == 0) ? -of / 3 / N : 0));
            }
        };

        /**
         * Consider that the given width `of` is divided into `N` segments.
         * This function calculates the width of `n`th segment.
         * <p>
         * One property to note is sum<n=0;n=N-1>[getNthWidth(n, N, of)] == of.
         *
         * @param n The index of tile width require; a number between 0 and N-1 inclusive.
         * @param N The total number of tile there should be.
         * @return The width of nth segment among the N divisions of the width 'of`
         */
        abstract int getNthSegment(float n, float N, float of);

    }

    private final RangeParam centerWidthParam = new RangeParam("Width", 1, 50, 99);
    private final RangeParam numberOfTilesParam = new RangeParam("Number Of Tiles", 2, 4, 20);
    private final EnumParam<Distributor> distributionParam = new EnumParam<>("Distribution", Distributor.class);
    private final RangeParam heightVarianceParam = new RangeParam("Height", 1, 50, 99);
    private final BooleanParam isMirrorParam = new BooleanParam("isMirror", false);

    public SlippingTiles() {
        super(false);

        setParams(
                centerWidthParam,
                numberOfTilesParam,
                distributionParam,
                heightVarianceParam,
                isMirrorParam
        );

        centerWidthParam.setToolTip("//TODO");
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int width = src.getWidth();
        int height = src.getHeight();
        int centerTileWidth = (int) (centerWidthParam.getValueAsDouble() * src.getWidth() / 100);
        int numberOfTiles = numberOfTilesParam.getValue();
        var distributor = distributionParam.getSelected();
        int finalHeight = (int) (heightVarianceParam.getValueAsDouble() * src.getHeight() / 100);
        var isMirror = isMirrorParam.isChecked();
        int remainingSpaceOnOneSide = (width - centerTileWidth) / 2;
        var graphics = dest.createGraphics();

        for (int
             i = 0,
             widthCovered = 0,
             heightCovered = finalHeight
             ; i < numberOfTiles; i++) {

            int nthWidth = distributor.getNthSegment(i, numberOfTiles, remainingSpaceOnOneSide);
            int nthHeight = distributor.getNthSegment(i, numberOfTiles, finalHeight);

            graphics.clipRect(widthCovered, 0, widthCovered + nthWidth, height);
            graphics.drawImage(src, 0, heightCovered, null);
            graphics.drawImage(src, 0, heightCovered - height, null);
            graphics.setClip(null);

            widthCovered += nthWidth;
            heightCovered -= nthHeight;
        }

        graphics.clipRect(remainingSpaceOnOneSide, 0, remainingSpaceOnOneSide + centerTileWidth, height);
        graphics.drawImage(src, 0, 0, null);
        graphics.setClip(null);

        for (int
             i = numberOfTiles - 1,
             widthCovered = 0,
             heightCovered = 0
             ; i >= 0; i--) {

            int nthWidth = distributor.getNthSegment(i, numberOfTiles, remainingSpaceOnOneSide);
            int nthHeight = distributor.getNthSegment(i, numberOfTiles, finalHeight);

            heightCovered += isMirror ? nthHeight : -nthHeight;

            graphics.clipRect(remainingSpaceOnOneSide + centerTileWidth + widthCovered, 0, widthCovered + nthWidth, height);
            graphics.drawImage(src, 0, heightCovered, null);
            graphics.drawImage(src, 0, heightCovered - (isMirror ? height : -height), null);
            graphics.setClip(null);

            widthCovered += nthWidth;
        }

        graphics.dispose();

        return dest;
    }
}