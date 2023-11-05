/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.lookup.FastLookupOp;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.LookupTable;
import java.awt.image.ShortLookupTable;
import java.io.Serial;

/**
 * Extract a channel from the image
 */
public class ExtractChannel extends ParametrizedFilter {
    public static final String NAME = "Extract Channel";

    @Serial
    private static final long serialVersionUID = -3743488918392302889L;

    private static final int RED_CHANNEL = 1;
    private static final int REMOVE_RED_CHANNEL = 2;

    private static final int GREEN_CHANNEL = 3;
    private static final int REMOVE_GREEN_CHANNEL = 4;

    private static final int BLUE_CHANNEL = 5;
    private static final int REMOVE_BLUE_CHANNEL = 6;

    private final IntChoiceParam channelParam = new IntChoiceParam("Channel", new Item[]{
        new Item("Red", RED_CHANNEL),
        new Item("Remove Red", REMOVE_RED_CHANNEL),
        new Item("Green", GREEN_CHANNEL),
        new Item("Remove Green", REMOVE_GREEN_CHANNEL),
        new Item("Blue", BLUE_CHANNEL),
        new Item("Remove Blue", REMOVE_BLUE_CHANNEL),
    });
    private final BooleanParam bwParam = new BooleanParam("Black and White", false);

    public ExtractChannel() {
        super(true);

        setParams(
            channelParam,
            bwParam);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int channel = channelParam.getValue();

        if (bwParam.isChecked()) {
            dest = bwExtractChannel(src, dest, channel);
        } else {
            dest = colorExtractChannel(src, dest, channel);
        }

        return dest;
    }

    private static BufferedImage bwExtractChannel(BufferedImage src, BufferedImage dest, int channel) {
        RGBPixelOp rgbOp = switch (channel) {
            case RED_CHANNEL -> (a, r, g, b) -> {
                g = r;
                b = r;
                return a << 24 | r << 16 | g << 8 | b;
            };
            case REMOVE_RED_CHANNEL -> (a, r, g, b) -> {
                int val = (g + b) / 2;
                return a << 24 | val << 16 | val << 8 | val;
            };
            case GREEN_CHANNEL -> (a, r, g, b) -> {
                r = g;
                b = g;
                return a << 24 | r << 16 | g << 8 | b;
            };
            case REMOVE_GREEN_CHANNEL -> (a, r, g, b) -> {
                int val = (r + b) / 2;
                return a << 24 | val << 16 | val << 8 | val;
            };
            case BLUE_CHANNEL -> (a, r, g, b) -> {
                r = b;
                g = b;
                return a << 24 | r << 16 | g << 8 | b;
            };
            case REMOVE_BLUE_CHANNEL -> (a, r, g, b) -> {
                int val = (r + g) / 2;
                return a << 24 | val << 16 | val << 8 | val;
            };
            default -> throw new IllegalStateException("should not het here");
        };
        return rgbOp.filter(src, dest);
    }

    private static BufferedImage colorExtractChannel(BufferedImage src, BufferedImage dest, int channel) {
        LookupTable lookupTable = switch (channel) {
            case RED_CHANNEL -> createOnlyRedLUT();
            case REMOVE_RED_CHANNEL -> createRemoveRedLUT();
            case GREEN_CHANNEL -> createOnlyGreenLUT();
            case REMOVE_GREEN_CHANNEL -> createRemoveGreenLUT();
            case BLUE_CHANNEL -> createOnlyBlueLUT();
            case REMOVE_BLUE_CHANNEL -> createRemoveBlueLUT();
            default -> throw new IllegalStateException("should not het here");
        };

        BufferedImageOp filterOp = new FastLookupOp((ShortLookupTable) lookupTable);
        filterOp.filter(src, dest);
        return dest;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }

    private static LookupTable createRemoveRedLUT() {
        short[][] maps = new short[3][256];
        maps[0] = createZeroMap();
        maps[1] = createIdentityMap();
        maps[2] = createIdentityMap();
        return new ShortLookupTable(0, maps);
    }

    private static LookupTable createOnlyRedLUT() {
        short[][] maps = new short[3][256];
        maps[0] = createIdentityMap();
        maps[1] = createZeroMap();
        maps[2] = createZeroMap();
        return new ShortLookupTable(0, maps);
    }

    private static LookupTable createRemoveGreenLUT() {
        short[][] maps = new short[3][256];
        maps[0] = createIdentityMap();
        maps[1] = createZeroMap();
        maps[2] = createIdentityMap();
        return new ShortLookupTable(0, maps);
    }

    private static LookupTable createOnlyGreenLUT() {
        short[][] maps = new short[3][256];
        maps[0] = createZeroMap();
        maps[1] = createIdentityMap();
        maps[2] = createZeroMap();
        return new ShortLookupTable(0, maps);
    }

    private static LookupTable createRemoveBlueLUT() {
        short[][] maps = new short[3][256];
        maps[0] = createIdentityMap();
        maps[1] = createIdentityMap();
        maps[2] = createZeroMap();
        return new ShortLookupTable(0, maps);
    }

    private static LookupTable createOnlyBlueLUT() {
        short[][] maps = new short[3][256];
        maps[0] = createZeroMap();
        maps[1] = createZeroMap();
        maps[2] = createIdentityMap();
        return new ShortLookupTable(0, maps);
    }

    private static short[] createIdentityMap() {
        short[] map = new short[256];
        for (int i = 0; i < 256; i++) {
            map[i] = (short) i;
        }
        return map;
    }

    private static short[] createZeroMap() {
        short[] map = new short[256];
        for (int i = 0; i < 256; i++) {
            map[i] = 0;
        }
        return map;
    }
}