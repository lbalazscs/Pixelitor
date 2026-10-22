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

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OklabLookupFilter tests")
class OklabLookupFilterTest {

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @Test
    @DisplayName("identity LUTs preserve RGB values within +-1 (or +-2 due to 8-bit quantization)")
    void identityLutsPreservePixels() {
        ChannelLookup id = ChannelLookup.getIdentity();
        OklabLookupFilter filter = new OklabLookupFilter(id, id, id);

        // black is exactly unchanged
        int black = 0xFF_00_00_00;
        assertThat(filter.processPixel(0, 0, black)).isEqualTo(black);

        // white is within +-1
        int white = 0xFF_FF_FF_FF;
        int whiteOut = filter.processPixel(0, 0, white);
        assertThat(Math.abs(((whiteOut >> 16) & 0xFF) - 255)).isLessThanOrEqualTo(1);
        assertThat(Math.abs(((whiteOut >> 8) & 0xFF) - 255)).isLessThanOrEqualTo(1);
        assertThat(Math.abs((whiteOut & 0xFF) - 255)).isLessThanOrEqualTo(1);

        // across all grayscale levels, error is at most +-2 due to 8-bit table quantization
        for (int v = 0; v < 256; v++) {
            int inRgb = 0xFF_00_00_00 | (v << 16) | (v << 8) | v;
            int outRgb = filter.processPixel(0, 0, inRgb);

            int rDiff = Math.abs(((outRgb >> 16) & 0xFF) - v);
            int gDiff = Math.abs(((outRgb >> 8) & 0xFF) - v);
            int bDiff = Math.abs((outRgb & 0xFF) - v);

            assertThat(rDiff).as("red diff for v=%d", v).isLessThanOrEqualTo(2);
            assertThat(gDiff).as("green diff for v=%d", v).isLessThanOrEqualTo(2);
            assertThat(bDiff).as("blue diff for v=%d", v).isLessThanOrEqualTo(2);
        }
    }

    @Test
    @DisplayName("alpha-0 pixels are unchanged")
    void alphaZeroPixelsAreUnchanged() {
        // use LUTs that heavily modify non-zero pixels
        ChannelLookup l = ChannelLookup.ofLevels(50, 200, 0, 255);
        ChannelLookup a = ChannelLookup.ofLevels(50, 200, 0, 255);
        ChannelLookup b = ChannelLookup.ofLevels(50, 200, 0, 255);
        OklabLookupFilter filter = new OklabLookupFilter(l, a, b);

        int[] transparentPixels = {
            0x00_00_00_00,
            0x00_FF_00_00,
            0x00_00_FF_00,
            0x00_00_00_FF,
            0x00_12_34_56,
            0x00_AA_BB_CC
        };

        for (int rgb : transparentPixels) {
            assertThat(filter.processPixel(0, 0, rgb))
                .as("pixel 0x%08X", rgb)
                .isEqualTo(rgb);
        }
    }

    @Test
    @DisplayName("alpha is preserved")
    void alphaIsPreserved() {
        ChannelLookup l = ChannelLookup.ofLevels(40, 210, 10, 240);
        ChannelLookup a = ChannelLookup.ofLevels(30, 220, 20, 230);
        ChannelLookup b = ChannelLookup.ofLevels(20, 230, 30, 220);
        OklabLookupFilter filter = new OklabLookupFilter(l, a, b);

        for (int alpha = 1; alpha <= 255; alpha++) {
            int inRgb = (alpha << 24) | 0x00_80_60_40;
            int outRgb = filter.processPixel(0, 0, inRgb);
            assertThat(outRgb >>> 24)
                .as("alpha preservation for alpha=%d", alpha)
                .isEqualTo(alpha);
        }
    }

    @Test
    @DisplayName("non-identity L LUT changes lightness in expected direction")
    void nonIdentityLLutChangesLightness() {
        ChannelLookup id = ChannelLookup.getIdentity();

        // darkening: output range [0, 80]
        ChannelLookup darkL = ChannelLookup.ofLevels(0, 255, 0, 80);
        OklabLookupFilter darkFilter = new OklabLookupFilter(darkL, id, id);

        // brightening: output range [180, 255]
        ChannelLookup brightL = ChannelLookup.ofLevels(0, 255, 180, 255);
        OklabLookupFilter brightFilter = new OklabLookupFilter(brightL, id, id);

        int midGray = 0xFF_80_80_80;
        int darkened = darkFilter.processPixel(0, 0, midGray);
        int brightened = brightFilter.processPixel(0, 0, midGray);

        int origR = (midGray >> 16) & 0xFF;
        int darkR = (darkened >> 16) & 0xFF;
        int brightR = (brightened >> 16) & 0xFF;

        assertThat(darkR).as("darkened red").isLessThan(origR);
        assertThat(brightR).as("brightened red").isGreaterThan(origR);

        int origG = (midGray >> 8) & 0xFF;
        int darkG = (darkened >> 8) & 0xFF;
        int brightG = (brightened >> 8) & 0xFF;

        assertThat(darkG).as("darkened green").isLessThan(origG);
        assertThat(brightG).as("brightened green").isGreaterThan(origG);

        int origB = midGray & 0xFF;
        int darkB = darkened & 0xFF;
        int brightB = brightened & 0xFF;

        assertThat(darkB).as("darkened blue").isLessThan(origB);
        assertThat(brightB).as("brightened blue").isGreaterThan(origB);
    }

    @Test
    @DisplayName("filter transforms BufferedImage correctly")
    void filterBufferedImage() {
        ChannelLookup id = ChannelLookup.getIdentity();
        OklabLookupFilter filter = new OklabLookupFilter(id, id, id);

        BufferedImage src = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        src.setRGB(0, 0, 0xFF_00_00_00);
        src.setRGB(1, 0, 0xFF_FF_FF_FF);
        src.setRGB(0, 1, 0x00_12_34_56); // transparent
        src.setRGB(1, 1, 0x80_80_80_80);

        BufferedImage dest = filter.filter(src, null);
        assertThat(dest).isNotNull();
        assertThat(dest.getWidth()).isEqualTo(2);
        assertThat(dest.getHeight()).isEqualTo(2);

        // transparent pixel unchanged
        assertThat(dest.getRGB(0, 1)).isEqualTo(0x00_12_34_56);
        // black unchanged
        assertThat(dest.getRGB(0, 0)).isEqualTo(0xFF_00_00_00);
        // alpha preserved on translucent pixel
        assertThat(dest.getRGB(1, 1) >>> 24).isEqualTo(0x80);
    }
}
