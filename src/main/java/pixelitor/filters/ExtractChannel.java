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

import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.filters.lookup.FastLookupOp;
import pixelitor.filters.lookup.LookupFactory;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.LookupTable;
import java.awt.image.ShortLookupTable;

/**
 * Extract a channel from the image
 */
public class ExtractChannel extends FilterWithParametrizedGUI {
    private static final int RED_CHANNEL = 1;
    private static final int REMOVE_RED_CHANNEL = 2;

    private static final int GREEN_CHANNEL = 3;
    private static final int REMOVE_GREEN_CHANNEL = 4;

    private static final int BLUE_CHANNEL = 5;
    private static final int REMOVE_BLUE_CHANNEL = 6;

    private final IntChoiceParam channelParam = new IntChoiceParam("Channel", new IntChoiceParam.Value[]{
            new IntChoiceParam.Value("Red", RED_CHANNEL),
            new IntChoiceParam.Value("Remove Red", REMOVE_RED_CHANNEL),
            new IntChoiceParam.Value("Green", GREEN_CHANNEL),
            new IntChoiceParam.Value("Remove Green", REMOVE_GREEN_CHANNEL),
            new IntChoiceParam.Value("Blue", BLUE_CHANNEL),
            new IntChoiceParam.Value("Remove Blue", REMOVE_BLUE_CHANNEL),
    });
    private final BooleanParam bwParam = new BooleanParam("Black and White", false);

    public ExtractChannel() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                channelParam,
                bwParam));
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
        RGBPixelOp rgbOp;
        switch (channel) {
            case RED_CHANNEL:
                rgbOp = (a, r, g, b) -> {
                    g = r;
                    b = r;
                    return (a << 24) | (r << 16) | (g << 8) | b;
                };
                break;
            case REMOVE_RED_CHANNEL:
                rgbOp = (a, r, g, b) -> {
                    int val = (g + b) / 2;
                    return (a << 24) | (val << 16) | (val << 8) | val;
                };
                break;
            case GREEN_CHANNEL:
                rgbOp = (a, r, g, b) -> {
                    r = g;
                    b = g;
                    return (a << 24) | (r << 16) | (g << 8) | b;
                };
                break;
            case REMOVE_GREEN_CHANNEL:
                rgbOp = (a, r, g, b) -> {
                    int val = (r + b) / 2;
                    return (a << 24) | (val << 16) | (val << 8) | val;
                };
                break;
            case BLUE_CHANNEL:
                rgbOp = (a, r, g, b) -> {
                    r = b;
                    g = b;
                    return (a << 24) | (r << 16) | (g << 8) | b;
                };
                break;
            case REMOVE_BLUE_CHANNEL:
                rgbOp = (a, r, g, b) -> {
                    int val = (r + g) / 2;
                    return (a << 24) | (val << 16) | (val << 8) | val;
                };
                break;
            default:
                throw new IllegalStateException("should not het here");
        }
        return FilterUtils.runRGBPixelOp(rgbOp, src, dest);
    }

    private static BufferedImage colorExtractChannel(BufferedImage src, BufferedImage dest, int channel) {
        LookupTable lookupTable;

        switch (channel) {
            case RED_CHANNEL:
                lookupTable = LookupFactory.createLookupForOnlyRed();
                break;
            case REMOVE_RED_CHANNEL:
                lookupTable = LookupFactory.createLookupForRemoveRed();
                break;
            case GREEN_CHANNEL:
                lookupTable = LookupFactory.createLookupForOnlyGreen();
                break;
            case REMOVE_GREEN_CHANNEL:
                lookupTable = LookupFactory.createLookupForRemoveGreen();
                break;
            case BLUE_CHANNEL:
                lookupTable = LookupFactory.createLookupForOnlyBlue();
                break;
            case REMOVE_BLUE_CHANNEL:
                lookupTable = LookupFactory.createLookupForRemoveBlue();
                break;
            default:
                throw new IllegalStateException("should not het here");
        }

        BufferedImageOp filterOp = new FastLookupOp((ShortLookupTable) lookupTable);
        filterOp.filter(src, dest);
        return dest;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}