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

package pixelitor.filters;

import com.jhlabs.image.PointFilter;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.filters.lookup.LuminanceLookup;

import java.awt.image.BufferedImage;

/**
 * Makes pixels transparent proportionally to a channel value
 */
public class ChannelToTransparency extends FilterWithParametrizedGUI {
    public static final String NAME = "Channel to Transparency";

    private static final int LUMINOSITY = 1;
    private static final int RED = 2;
    private static final int GREEN = 3;
    private static final int BLUE = 4;

    private final IntChoiceParam channelParam = new IntChoiceParam("Channel",
            new IntChoiceParam.Value[]{
                    new IntChoiceParam.Value("Luminosity", LUMINOSITY),
                    new IntChoiceParam.Value("Red", RED),
                    new IntChoiceParam.Value("Green", GREEN),
                    new IntChoiceParam.Value("Blue", BLUE)
            });
    private final BooleanParam invertParam = new BooleanParam("Invert", false);

    public ChannelToTransparency() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                channelParam,
                invertParam
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        ChannelToTransparencyFilter filter;
        boolean invert = invertParam.isChecked();

        switch (channelParam.getValue()) {
            case LUMINOSITY:
                filter = new ChannelToTransparencyFilter(NAME, invert) {
                    @Override
                    int getChannelValue(int rgb) {
                        return LuminanceLookup.getLuminosity(rgb);
                    }
                };
                break;
            case RED:
                filter = new ChannelToTransparencyFilter(NAME, invert) {
                    @Override
                    int getChannelValue(int rgb) {
                        int r = (rgb >>> 16) & 0xFF;
                        return r;
                    }
                };
                break;
            case GREEN:
                filter = new ChannelToTransparencyFilter(NAME, invert) {
                    @Override
                    int getChannelValue(int rgb) {
                        int g = (rgb >>> 8) & 0xFF;
                        return g;
                    }
                };
                break;
            case BLUE:
                filter = new ChannelToTransparencyFilter(NAME, invert) {
                    @Override
                    int getChannelValue(int rgb) {
                        int b = (rgb) & 0xFF;
                        return b;
                    }
                };
                break;
            default:
                throw new IllegalStateException("unexpected value " + channelParam.getValue());
        }

        return filter.filter(src, dest);
    }

    @Override
    public void randomizeSettings() {
        // no settings
    }

    static abstract class ChannelToTransparencyFilter extends PointFilter {
        private final boolean invert;

        protected ChannelToTransparencyFilter(String filterName, boolean invert) {
            super(filterName);
            this.invert = invert;
        }

        @Override
        public int filterRGB(int x, int y, int rgb) {
            int v = getChannelValue(rgb);
            int alpha;
            if (invert) {
                alpha = v;
            } else {
                alpha = 255 - v;
            }

            rgb = rgb & 0x00FFFFFF; // clear alpha
            return alpha << 24 | rgb;
        }

        abstract int getChannelValue(int rgb);
    }
}