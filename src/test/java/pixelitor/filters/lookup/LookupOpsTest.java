/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.lookup;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pixelitor.TestHelper;
import pixelitor.filters.util.Channel;
import pixelitor.filters.util.ColorSpace;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.util.Random;
import java.util.function.Function;

import static java.awt.image.BufferedImage.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LookupOps tests")
class LookupOpsTest {

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @Test
    @DisplayName("both backends give identical results on opaque TYPE_INT_RGB images")
    void identicalResultsOnOpaqueImage() {
        Random rnd = new Random(12345L);
        Function<Channel, ChannelLookup> lookups = createRandomLookups(rnd);

        BufferedImageOp fastOp = LookupOps.create(ColorSpace.SRGB, lookups, true);
        BufferedImageOp transferOp = LookupOps.create(ColorSpace.SRGB, lookups, false);

        BufferedImage src = createRandomImage(16, 16, TYPE_INT_RGB, rnd);

        BufferedImage destFast = fastOp.filter(src, null);
        BufferedImage destTransfer = transferOp.filter(src, null);

        assertImagesEqual(destFast, destTransfer);
    }

    @Test
    @DisplayName("both backends give identical results on non-premultiplied TYPE_INT_ARGB images")
    void identicalResultsOnArgbImage() {
        Random rnd = new Random(67890L);
        Function<Channel, ChannelLookup> lookups = createRandomLookups(rnd);

        BufferedImageOp fastOp = LookupOps.create(ColorSpace.SRGB, lookups, true);
        BufferedImageOp transferOp = LookupOps.create(ColorSpace.SRGB, lookups, false);

        BufferedImage src = createRandomImage(16, 16, TYPE_INT_ARGB, rnd);

        BufferedImage destFast = fastOp.filter(src, null);
        BufferedImage destTransfer = transferOp.filter(src, null);

        assertImagesEqual(destFast, destTransfer);
    }

    @Test
    @DisplayName("TYPE_INT_ARGB_PRE: alpha-255 pixels match between backends")
    void argbPreAlpha255PixelsMatch() {
        // FastLookupOp unpremultiplies semi-transparent pixels before applying the LUT
        // and repremultiplies afterwards, whereas TransferFilter (PointFilter) applies
        // LUTs directly to the premultiplied components without unpremultiplying.
        // Therefore, semi-transparent pixels differ, but pixels with alpha=255
        // (where premultiplied == unpremultiplied) must produce identical results.
        Random rnd = new Random(54321L);
        Function<Channel, ChannelLookup> lookups = createRandomLookups(rnd);

        BufferedImageOp fastOp = LookupOps.create(ColorSpace.SRGB, lookups, true);
        BufferedImageOp transferOp = LookupOps.create(ColorSpace.SRGB, lookups, false);

        BufferedImage src = createRandomImage(16, 16, TYPE_INT_ARGB_PRE, rnd);

        BufferedImage destFast = fastOp.filter(src, null);
        BufferedImage destTransfer = transferOp.filter(src, null);

        int comparedCount = 0;
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int srcPixel = src.getRGB(x, y);
                int alpha = srcPixel >>> 24;
                if (alpha == 255) {
                    assertThat(destFast.getRGB(x, y))
                        .as("pixel at (%d, %d) with alpha=255", x, y)
                        .isEqualTo(destTransfer.getRGB(x, y));
                    comparedCount++;
                }
            }
        }
        assertThat(comparedCount).as("should have tested at least one opaque pixel").isGreaterThan(0);
    }

    @Test
    @DisplayName("TransferFilter backend runs on TYPE_BYTE_GRAY image without exception")
    void transferFilterRunsOnGrayImage() {
        Random rnd = new Random(99999L);
        Function<Channel, ChannelLookup> lookups = createRandomLookups(rnd);

        BufferedImageOp transferOp = LookupOps.create(ColorSpace.SRGB, lookups, false);

        int w = 16;
        int h = 16;
        BufferedImage graySrc = new BufferedImage(w, h, TYPE_BYTE_GRAY);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int gray = rnd.nextInt(256);
                graySrc.getRaster().setSample(x, y, 0, gray);
            }
        }

        BufferedImage dest = transferOp.filter(graySrc, null);

        assertThat(dest).isNotNull();
        assertThat(dest.getWidth()).isEqualTo(w);
        assertThat(dest.getHeight()).isEqualTo(h);
        assertThat(dest.getType()).isEqualTo(TYPE_BYTE_GRAY);
    }

    @Test
    @DisplayName("supportsGray reflects the sRGB backend flag")
    void supportsGrayConsistency() {
        assertThat(LookupOps.supportsGray()).isEqualTo(!LookupOps.isSrgbFastLookup());
    }

    @Test
    @DisplayName("Oklab color space returns OklabLookupFilter regardless of backend flag")
    void oklabReturnsOklabLookupFilter() {
        Function<Channel, ChannelLookup> lookups = _ -> ChannelLookup.getIdentity();
        BufferedImageOp opTrue = LookupOps.create(ColorSpace.OKLAB, lookups, true);
        BufferedImageOp opFalse = LookupOps.create(ColorSpace.OKLAB, lookups, false);

        assertThat(opTrue).isInstanceOf(OklabLookupFilter.class);
        assertThat(opFalse).isInstanceOf(OklabLookupFilter.class);
    }

    private static Function<Channel, ChannelLookup> createRandomLookups(Random rnd) {
        ChannelLookup rgb = randomLookup(rnd);
        ChannelLookup r = randomLookup(rnd);
        ChannelLookup g = randomLookup(rnd);
        ChannelLookup b = randomLookup(rnd);
        return ch -> switch (ch) {
            case RGB -> rgb;
            case RED -> r;
            case GREEN -> g;
            case BLUE -> b;
            default -> ChannelLookup.getIdentity();
        };
    }

    private static ChannelLookup randomLookup(Random rnd) {
        int[] table = new int[256];
        for (int i = 0; i < 256; i++) {
            table[i] = rnd.nextInt(256);
        }
        return ChannelLookup.ofTable(table);
    }

    private static BufferedImage createRandomImage(int width, int height, int type, Random rnd) {
        BufferedImage img = new BufferedImage(width, height, type);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int a = switch (type) {
                    case TYPE_INT_RGB -> 255;
                    case TYPE_INT_ARGB, TYPE_INT_ARGB_PRE -> (x % 3 == 0) ? 255 : ((x % 3 == 1) ? 128 : 0);
                    default -> 255;
                };
                int r = rnd.nextInt(256);
                int g = rnd.nextInt(256);
                int b = rnd.nextInt(256);
                img.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return img;
    }

    private static void assertImagesEqual(BufferedImage actual, BufferedImage expected) {
        assertThat(actual.getWidth()).isEqualTo(expected.getWidth());
        assertThat(actual.getHeight()).isEqualTo(expected.getHeight());
        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                assertThat(actual.getRGB(x, y))
                    .as("pixel at (%d, %d)", x, y)
                    .isEqualTo(expected.getRGB(x, y));
            }
        }
    }
}
