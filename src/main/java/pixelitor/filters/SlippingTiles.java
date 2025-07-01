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

import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ChoiceParam;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.RangeParam;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Slipping Tiles filter, it will slice the image from right and left side and slip them away from the canvas vertically.
 */
public class SlippingTiles extends ParametrizedFilter {
    public static final String NAME = "Slipping Tiles";

    public enum Distributor {
        EVEN("Even") {
            @Override
            float getNthSegment(float n, float N, float of) {
                return of / N;
            }
        }, EXPONENTIAL("Exponential") {
            private static final double LN_10 = Math.log(10);

            @Override
            float getNthSegment(float n, float N, float of) {
                if (N == 1) {
                    return of;
                }
                return (float) (Math.exp(LN_10 * n / (N - 1)) * /*normalising*/ (of * (Math.exp(LN_10 / (N - 1)) - 1) / (Math.exp(LN_10 * N / (N - 1)) - 1)));
            }
        }, TICK_TOCK("Tick Tock") {
            @Override
            float getNthSegment(float n, float N, float of) {
                return of / N + (((int) n) % 2 == 0 ? 1 : -1) * of / 3 / N + /*normalising*/ ((N % 2 == 1 && n == 0) ? -of / 3 / N : 0);
            }
        };

        private final String displayName;

        Distributor(String displayName) {
            this.displayName = displayName;
        }

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
        abstract float getNthSegment(float n, float N, float of);

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum SlipDirection {
        DEFAULT("Default", 3),
        COUNTER_CLOCK_WISE("Counterclockwise", 2),
        CLOCK_WISE("Clockwise", 1),
        OPPOSITE("Opposite", 0);

        private final int val;
        private final String displayName;

        SlipDirection(String displayName, int val) {
            this.displayName = displayName;
            this.val = val;
        }

        @Override
        public String toString() {
            return displayName;
        }

        boolean isFirstSideFalling() {
            return (val & 0b10) > 0;
        }

        boolean isSecondSideFalling() {
            return (val & 0b1) > 0;
        }
    }

    private final RangeParam centerTileSizeParam = new RangeParam("Center Tile Width", 1, 50, 99);
    private final BooleanParam isCenterTileSizeAutomaticallyCalculatedParam = new BooleanParam("Auto-Adjust Width");
    private final RangeParam numberOfTilesParam = new RangeParam("Number of Tiles", 1, 4, 20);
    private final EnumParam<Distributor> distributionParam = new EnumParam<>("Distribution", Distributor.class);
    private final RangeParam slipDisplacementParam = new RangeParam("Slip Length", 1, 50, 99);
    private final RangeParam centerTilePositionParam = new RangeParam("Center Tile Position", 1, 50, 99);
    private final ChoiceParam<String> isVerticalParam = new ChoiceParam<>("Cut Direction", new String[]{"Vertical", "Horizontal"});
    private final EnumParam<SlipDirection> slipDirectionParam = new EnumParam<>("Slip Direction", SlipDirection.class);

    public SlippingTiles() {
        super(true);

        initParams(
            centerTileSizeParam,
            isCenterTileSizeAutomaticallyCalculatedParam,
            numberOfTilesParam,
            distributionParam,
            slipDisplacementParam,
            centerTilePositionParam,
            isVerticalParam,
            slipDirectionParam
        );

        isCenterTileSizeAutomaticallyCalculatedParam.setupDisableOtherIfChecked(centerTileSizeParam);

        centerTileSizeParam.setToolTip("The width of the center tile (in %).");
        isCenterTileSizeAutomaticallyCalculatedParam.setToolTip("If checked, the width of center tile is calculated to fit along with the size of cuts.");
        numberOfTilesParam.setToolTip("The number of tiles to cut on either side.");
        distributionParam.setToolTip("The method of cutting the tiles.");
        slipDisplacementParam.setToolTip("The distance the tiles must slip.");
        centerTilePositionParam.setToolTip("Move around the center tile.");
        isVerticalParam.setToolTip("Alters the direction of the cut.");
        slipDirectionParam.setToolTip("Alters the direction the tiles should slip.");
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        var isVertical = isVerticalParam.getParamValue().equals("Vertical");

        // Size of image perpendicular to the cut
        int sizePerpCut = isVertical ? src.getWidth() : src.getHeight();
        // Size of image along the cut
        int sizeAlonCut = isVertical ? src.getHeight() : src.getWidth();

        int numberOfTiles = numberOfTilesParam.getValue();
        var distributor = distributionParam.getSelected();
        int centerTileSize = isCenterTileSizeAutomaticallyCalculatedParam.isChecked() ?
            (int) (distributor.getNthSegment(2 * numberOfTiles + 1, 2 * numberOfTiles + 1, sizePerpCut)) :
            ((int) (centerTileSizeParam.getPercentage() * sizePerpCut));
        int finalSlipDisplacement = (int) (slipDisplacementParam.getPercentage() * sizeAlonCut);
        var slipDirection = slipDirectionParam.getSelected();
        var graphics = dest.createGraphics();

        var shiftFactor = centerTilePositionParam.getPercentage();
        int remainingSpaceOnOneSide = (int) ((sizePerpCut - centerTileSize) * shiftFactor);
        int remainingSpaceOnOtherSide = (int) ((sizePerpCut - centerTileSize) * (1 - shiftFactor));

        float distCoveredPerpCut = 0, distCoveredAlonCut = finalSlipDisplacement;
        for (int i = 0; i < numberOfTiles; i++) {
            float nthSizePerpCut = distributor.getNthSegment(i, numberOfTiles, remainingSpaceOnOneSide);
            float nthSizeAlonCut = -distributor.getNthSegment(i, numberOfTiles, finalSlipDisplacement);
            if (slipDirection.isFirstSideFalling()) {
                nthSizeAlonCut *= -1;
            }

            if (isVertical) {
                paintWalls(graphics, src, (int) distCoveredPerpCut, (int) (distCoveredPerpCut + nthSizePerpCut), sizeAlonCut, (int) distCoveredAlonCut, (int) (distCoveredAlonCut - sizeAlonCut));
            } else {
                paintRoofAndCeiling(graphics, src, (int) distCoveredPerpCut, (int) (distCoveredPerpCut + nthSizePerpCut), sizeAlonCut, (int) distCoveredAlonCut, (int) (distCoveredAlonCut - sizeAlonCut));
            }

            distCoveredPerpCut += nthSizePerpCut;
            distCoveredAlonCut -= nthSizeAlonCut;
        }

        if (isVertical) {
            graphics.clipRect(remainingSpaceOnOneSide, 0, remainingSpaceOnOneSide + centerTileSize, sizeAlonCut);
        } else {
            graphics.clipRect(0, remainingSpaceOnOneSide, sizeAlonCut, remainingSpaceOnOneSide + centerTileSize);
        }
        graphics.drawImage(src, 0, 0, null);
        graphics.setClip(null);

        distCoveredPerpCut = distCoveredAlonCut = 0;
        for (int i = numberOfTiles - 1; i >= 0; i--) {

            float nthSizePerpCut = distributor.getNthSegment(i, numberOfTiles, remainingSpaceOnOtherSide);
            float nthSizeAlonCut = -distributor.getNthSegment(i, numberOfTiles, finalSlipDisplacement);
            if (slipDirection.isSecondSideFalling()) {
                nthSizeAlonCut *= -1;
            }

            distCoveredAlonCut += nthSizeAlonCut;

            if (isVertical) {
                paintWalls(graphics, src, (int) (remainingSpaceOnOneSide + centerTileSize + distCoveredPerpCut), (int) (distCoveredPerpCut + nthSizePerpCut), sizeAlonCut, (int) distCoveredAlonCut, (int) (distCoveredAlonCut - (slipDirection.isSecondSideFalling() ? sizeAlonCut : -sizeAlonCut)));
            } else {
                paintRoofAndCeiling(graphics, src, (int) (remainingSpaceOnOneSide + centerTileSize + distCoveredPerpCut), (int) (distCoveredPerpCut + nthSizePerpCut), sizeAlonCut, (int) distCoveredAlonCut, (int) (distCoveredAlonCut - (slipDirection.isSecondSideFalling() ? sizeAlonCut : -sizeAlonCut)));
            }

            distCoveredPerpCut += nthSizePerpCut;
        }

        graphics.dispose();

        return dest;
    }

    private static void paintWalls(Graphics2D graphics, BufferedImage src, int clipOffset, int clipSizePerpCut, int clipSizeAlonCut, int imageDisplacement1, int imageDisplacement2) {
        graphics.clipRect(clipOffset, 0, clipSizePerpCut, clipSizeAlonCut);
        graphics.drawImage(src, 0, imageDisplacement1, null);
        graphics.drawImage(src, 0, imageDisplacement2, null);
        graphics.setClip(null);
    }

    private static void paintRoofAndCeiling(Graphics2D graphics, BufferedImage src, int clipOffset, int clipSizePerpCut, int clipSizeAlonCut, int imageDisplacement1, int imageDisplacement2) {
        graphics.clipRect(0, clipOffset, clipSizeAlonCut, clipSizePerpCut);
        graphics.drawImage(src, imageDisplacement1, 0, null);
        graphics.drawImage(src, imageDisplacement2, 0, null);
        graphics.setClip(null);
    }
}