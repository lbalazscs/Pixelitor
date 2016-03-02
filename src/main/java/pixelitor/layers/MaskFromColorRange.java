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

package pixelitor.layers;

import com.jhlabs.image.PointFilter;
import net.jafama.FastMath;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;
import static pixelitor.filters.gui.ColorParam.OpacitySetting.NO_OPACITY;

/**
 * MaskFromColor filter based on Impl
 */
public class MaskFromColorRange extends FilterWithParametrizedGUI {
    public static final String NAME = "Mask from Color Range";

    private final RangeParam tolerance = new RangeParam("Tolerance", 0, 10, 100);
    private final RangeParam fuzziness = new RangeParam("Fuzziness", 0, 10, 100);
    private final ColorParam color = new ColorParam("Color", Color.GREEN, NO_OPACITY);
    private final BooleanParam invert = new BooleanParam("Invert", false);

    private Impl filter;

    public MaskFromColorRange() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(color, tolerance, fuzziness, invert));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new Impl(NAME);
        }

        filter.setColor(color.getColor());
        filter.setTolerance(tolerance.getValue(), fuzziness.getValueAsPercentage());
        filter.setInvert(invert.isChecked());

        dest = filter.filter(src, dest);
        return dest;
    }

    public static BufferedImage getMaskFrom(BufferedImage input, double tolerance) {
        int width = input.getWidth();
        int height = input.getHeight();
        BufferedImage maskImage = new BufferedImage(width, height, TYPE_BYTE_GRAY);
        return maskImage;
    }

    private static class Impl extends PointFilter {
        public static final int WHITE_PIXEL = 0xFF_FF_FF_FF;
        public static final int BLACK_PIXEL = 0xFF_00_00_00;
        private double toleranceMin;
        private double toleranceMax;
        private int refR;
        private int refG;
        private int refB;
        private boolean invert;

        protected Impl(String filterName) {
            super(filterName);
        }

        public void setColor(Color c) {
            refR = c.getRed();
            refG = c.getGreen();
            refB = c.getBlue();
        }

        public void setTolerance(double tolerance, double fuzziness) {
            this.toleranceMin = tolerance * (1.0 - fuzziness);
            this.toleranceMax = tolerance * (1.0 + fuzziness);
        }

        public void setInvert(boolean invert) {
            this.invert = invert;
        }

        @Override
        public int filterRGB(int x, int y, int rgb) {
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;

            int deltaR = r - refR;
            int deltaG = g - refG;
            int deltaB = b - refB;

            double dist = FastMath.sqrt(deltaR * deltaR + deltaG * deltaG + deltaB * deltaB);

            if (dist > toleranceMax) {
                if (invert) {
                    return WHITE_PIXEL;
                } else {
                    return BLACK_PIXEL;
                }
            } else if (dist < toleranceMin) {
                if (invert) {
                    return BLACK_PIXEL;
                } else {
                    return WHITE_PIXEL;
                }
            } else {
                // linear interpolation
                int v = (int) ((toleranceMax - dist) * 255 / (toleranceMax - toleranceMin));
                if (invert) {
                    v = 255 - v;
                }
                return 0xFF_00_00_00 | (v << 16) | (v << 8) | v;
            }
        }
    }
}
