/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.PointFilter;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Value;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.filters.lookup.LuminanceLookup;

import java.awt.image.BufferedImage;

/**
 * Makes pixels transparent proportionally to a channel value
 */
public class ChannelToTransparency extends ParametrizedFilter {
    public static final String NAME = "Channel to Transparency";

    private static final int LUMINOSITY = 1;
    private static final int RED = 2;
    private static final int GREEN = 3;
    private static final int BLUE = 4;

    private final IntChoiceParam channel = new IntChoiceParam("Channel", new Value[]{
            new Value("Luminosity", LUMINOSITY),
            new Value("Red", RED),
            new Value("Green", GREEN),
            new Value("Blue", BLUE)
    });
    private final BooleanParam invertParam = new BooleanParam("Invert", false);
    private final BooleanParam keepParam = new BooleanParam("Keep Existing Transparency", true);

    public ChannelToTransparency() {
        super(ShowOriginal.YES);

        setParams(
                channel,
                invertParam,
                keepParam
        );
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        ChannelToTransparencyFilter filter;
        boolean invert = invertParam.isChecked();
        boolean keep = keepParam.isChecked();

        switch (channel.getValue()) {
            case LUMINOSITY:
                filter = new ChannelToTransparencyFilter(NAME, invert, keep) {
                    @Override
                    int getChannelValue(int rgb) {
                        return (int) LuminanceLookup.from(rgb);
                    }
                };
                break;
            case RED:
                filter = new ChannelToTransparencyFilter(NAME, invert, keep) {
                    @Override
                    int getChannelValue(int rgb) {
                        int r = (rgb >>> 16) & 0xFF;
                        return r;
                    }
                };
                break;
            case GREEN:
                filter = new ChannelToTransparencyFilter(NAME, invert, keep) {
                    @Override
                    int getChannelValue(int rgb) {
                        int g = (rgb >>> 8) & 0xFF;
                        return g;
                    }
                };
                break;
            case BLUE:
                filter = new ChannelToTransparencyFilter(NAME, invert, keep) {
                    @Override
                    int getChannelValue(int rgb) {
                        int b = rgb & 0xFF;
                        return b;
                    }
                };
                break;
            default:
                throw new IllegalStateException("unexpected value " + channel.getValue());
        }

        return filter.filter(src, dest);
    }

    @Override
    public void randomizeSettings() {
        // no settings
    }

    abstract static class ChannelToTransparencyFilter extends PointFilter {
        private final boolean invert;
        private final boolean keep;

        protected ChannelToTransparencyFilter(String filterName, boolean invert, boolean keep) {
            super(filterName);
            this.invert = invert;
            this.keep = keep;
        }

        @Override
        public int filterRGB(int x, int y, int argb) {
            int v = getChannelValue(argb);
            int alpha;
            if (invert) {
                alpha = v;
            } else {
                alpha = 255 - v;
            }

            if (keep) {
                int origAlpha = (argb >>> 24) & 0xFF;
                if (origAlpha < alpha) {
                    alpha = origAlpha;
                }
            }

            argb = argb & 0x00FFFFFF; // clear alpha
            return alpha << 24 | argb;
        }

        abstract int getChannelValue(int rgb);
    }
}