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

import com.jhlabs.image.PointFilter;
import pixelitor.colors.Colors;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.lookup.LuminanceLookup;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * Makes pixels transparent proportionally to a channel value
 */
public class ChannelToTransparency extends ParametrizedFilter {
    public static final String NAME = "Channel to Transparency";

    @Serial
    private static final long serialVersionUID = -8354668803636534983L;

    private static final int LUMINOSITY = 1;
    private static final int RED = 2;
    private static final int GREEN = 3;
    private static final int BLUE = 4;

    private final IntChoiceParam channel = new IntChoiceParam("Channel", new Item[]{
//        new Item(GUIText.BRIGHTNESS, LUMINOSITY),
//        new Item(i18n("red"), RED),
//        new Item(i18n("green"), GREEN),
//        new Item(i18n("blue"), BLUE)
        new Item("Brightness", LUMINOSITY),
        new Item("Red", RED),
        new Item("Green", GREEN),
        new Item("Blue", BLUE)
    });
    private final BooleanParam invertParam = new BooleanParam("Invert");
    private final BooleanParam keepParam = new BooleanParam("Keep Existing Transparency", true);

    public ChannelToTransparency() {
        super(true);

        initParams(
            channel,
            invertParam,
            keepParam
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        boolean invert = invertParam.isChecked();
        boolean keep = keepParam.isChecked();

        ChannelToTransparencyFilter filter = switch (channel.getValue()) {
            case LUMINOSITY -> new ChannelToTransparencyFilter(invert, keep) {
                @Override
                int getChannelValue(int rgb) {
                    return (int) LuminanceLookup.from(rgb);
                }
            };
            case RED -> new ChannelToTransparencyFilter(invert, keep) {
                @Override
                int getChannelValue(int rgb) {
                    return (rgb >>> 16) & 0xFF;
                }
            };
            case GREEN -> new ChannelToTransparencyFilter(invert, keep) {
                @Override
                int getChannelValue(int rgb) {
                    return (rgb >>> 8) & 0xFF;
                }
            };
            case BLUE -> new ChannelToTransparencyFilter(invert, keep) {
                @Override
                int getChannelValue(int rgb) {
                    return rgb & 0xFF;
                }
            };
            default -> throw new IllegalStateException("unexpected value " + channel.getValue());
        };

        return filter.filter(src, dest);
    }

    @Override
    public void randomize() {
        // no settings
    }

    abstract static class ChannelToTransparencyFilter extends PointFilter {
        private final boolean invert;
        private final boolean keep;

        protected ChannelToTransparencyFilter(boolean invert, boolean keep) {
            super(NAME);
            this.invert = invert;
            this.keep = keep;
        }

        @Override
        public int processPixel(int x, int y, int argb) {
            int v = getChannelValue(argb);
            int newAlpha = invert ? v : 255 - v;

            if (keep) {
                int origAlpha = (argb >>> 24) & 0xFF;
                if (origAlpha < newAlpha) {
                    newAlpha = origAlpha;
                }
            }

            return Colors.setAlpha(argb, newAlpha);
        }

        abstract int getChannelValue(int rgb);
    }
}