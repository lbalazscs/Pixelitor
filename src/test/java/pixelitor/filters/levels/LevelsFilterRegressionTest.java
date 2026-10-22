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

package pixelitor.filters.levels;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pixelitor.TestHelper;
import pixelitor.colors.ColorSpaces;
import pixelitor.filters.Filter;
import pixelitor.filters.gui.UserPreset;
import pixelitor.filters.util.Channel;
import pixelitor.filters.util.ColorSpace;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Characterization regression tests for {@link Levels}.
 * Pins down current preset serialization, round-tripping, sRGB and Oklab pixel output.
 */
@DisplayName("Levels filter regression tests")
class LevelsFilterRegressionTest {

    private static final String[] PARAM_NAMES = {
        "Input Dark", "Input Light", "Output Dark", "Output Light"
    };

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @Test
    @DisplayName("preset format: sRGB saves only sRGB keys with expected values")
    void presetFormatSrgb() {
        UserPreset preset = createSrgbLevelsPreset(
            10, 240, 20, 230,
            15, 245, 25, 235,
            5, 250, 0, 255,
            0, 255, 0, 255
        );
        Levels filter = new Levels();
        filter.loadUserPreset(preset);

        UserPreset saved = new UserPreset("saved");
        filter.saveStateTo(saved);

        UserPreset reloaded = new UserPreset("reloaded");
        reloaded.loadFromString(saved.writeToString());

        assertThat(reloaded.get(ColorSpace.PRESET_KEY)).isEqualTo("SRGB");

        // verify sRGB channel values
        assertLevelsParams(reloaded, Channel.RGB, 10, 240, 20, 230);
        assertLevelsParams(reloaded, Channel.RED, 15, 245, 25, 235);
        assertLevelsParams(reloaded, Channel.GREEN, 5, 250, 0, 255);
        assertLevelsParams(reloaded, Channel.BLUE, 0, 255, 0, 255);

        // verify no Oklab keys exist
        for (Channel ch : ColorSpace.OKLAB.getChannels()) {
            for (String param : PARAM_NAMES) {
                assertThat(reloaded.get(ch.getPresetKey() + " " + param))
                    .as("Oklab key should not exist in sRGB preset")
                    .isNull();
            }
        }

        // verify exact set of keys
        Set<String> keys = extractKeys(saved.writeToString());
        Set<String> expectedKeys = expectedKeysFor(ColorSpace.SRGB);
        assertThat(keys).isEqualTo(expectedKeys);
    }

    @Test
    @DisplayName("preset format: Oklab saves only Oklab keys with expected values")
    void presetFormatOklab() {
        UserPreset preset = createOklabLevelsPreset(
            20, 220, 10, 240,
            30, 210, 40, 200,
            50, 190, 60, 180
        );
        Levels filter = new Levels();
        filter.loadUserPreset(preset);

        UserPreset saved = new UserPreset("saved");
        filter.saveStateTo(saved);

        UserPreset reloaded = new UserPreset("reloaded");
        reloaded.loadFromString(saved.writeToString());

        assertThat(reloaded.get(ColorSpace.PRESET_KEY)).isEqualTo("OKLAB");

        // verify Oklab channel values
        assertLevelsParams(reloaded, Channel.OK_L, 20, 220, 10, 240);
        assertLevelsParams(reloaded, Channel.OK_A, 30, 210, 40, 200);
        assertLevelsParams(reloaded, Channel.OK_B, 50, 190, 60, 180);

        // verify no sRGB keys exist
        for (Channel ch : ColorSpace.SRGB.getChannels()) {
            for (String param : PARAM_NAMES) {
                assertThat(reloaded.get(ch.getPresetKey() + " " + param))
                    .as("sRGB key should not exist in Oklab preset")
                    .isNull();
            }
        }

        // verify exact set of keys
        Set<String> keys = extractKeys(saved.writeToString());
        Set<String> expectedKeys = expectedKeysFor(ColorSpace.OKLAB);
        assertThat(keys).isEqualTo(expectedKeys);
    }

    @Test
    @DisplayName("round trip: sRGB preset reload and filter copy produce identical results")
    void roundTripSrgb() {
        UserPreset preset = createSrgbLevelsPreset(
            25, 225, 35, 215,
            10, 240, 0, 255,
            0, 255, 10, 240,
            5, 250, 15, 235
        );
        Levels filter1 = new Levels();
        filter1.loadUserPreset(preset);

        UserPreset saved1 = new UserPreset("saved1");
        filter1.saveStateTo(saved1);

        Levels filter2 = new Levels();
        filter2.loadUserPreset(saved1);

        UserPreset saved2 = new UserPreset("saved2");
        filter2.saveStateTo(saved2);

        assertThat(saved2.writeToString()).isEqualTo(saved1.writeToString());

        // verify Filter.copy() output matches original
        Filter copy = filter1.copy();
        BufferedImage img = createTestImage();
        BufferedImage outOriginal = filter1.transform(img, null);
        BufferedImage outCopy = ((Levels) copy).transform(img, null);
        assertImagesEqual(outCopy, outOriginal);
    }

    @Test
    @DisplayName("round trip: Oklab preset reload and filter copy produce identical results")
    void roundTripOklab() {
        UserPreset preset = createOklabLevelsPreset(
            30, 210, 15, 230,
            40, 200, 20, 220,
            50, 190, 25, 210
        );
        Levels filter1 = new Levels();
        filter1.loadUserPreset(preset);

        UserPreset saved1 = new UserPreset("saved1");
        filter1.saveStateTo(saved1);

        Levels filter2 = new Levels();
        filter2.loadUserPreset(saved1);

        UserPreset saved2 = new UserPreset("saved2");
        filter2.saveStateTo(saved2);

        assertThat(saved2.writeToString()).isEqualTo(saved1.writeToString());

        Filter copy = filter1.copy();
        BufferedImage img = createTestImage();
        BufferedImage outOriginal = filter1.transform(img, null);
        BufferedImage outCopy = ((Levels) copy).transform(img, null);
        assertImagesEqual(outCopy, outOriginal);
    }

    @Test
    @DisplayName("pixel output sRGB: master channel only")
    void pixelOutputSrgbMasterOnly() {
        int inDark = 30;
        int inLight = 220;
        int outDark = 10;
        int outLight = 240;

        UserPreset preset = createSrgbLevelsPreset(
            inDark, inLight, outDark, outLight,
            0, 255, 0, 255,
            0, 255, 0, 255,
            0, 255, 0, 255
        );
        Levels filter = new Levels();
        filter.loadUserPreset(preset);

        BufferedImage img = createTestImage();
        BufferedImage out = filter.transform(img, null);

        int[] masterLut = computeReferenceLut(inDark, inLight, outDark, outLight);
        assertImageMatchesSrgbReference(out, img, masterLut, null, null, null);
    }

    @Test
    @DisplayName("pixel output sRGB: single channel (Red) only")
    void pixelOutputSrgbSingleChannelOnly() {
        int inDark = 40;
        int inLight = 200;
        int outDark = 20;
        int outLight = 230;

        UserPreset preset = createSrgbLevelsPreset(
            0, 255, 0, 255,
            inDark, inLight, outDark, outLight,
            0, 255, 0, 255,
            0, 255, 0, 255
        );
        Levels filter = new Levels();
        filter.loadUserPreset(preset);

        BufferedImage img = createTestImage();
        BufferedImage out = filter.transform(img, null);

        int[] rLut = computeReferenceLut(inDark, inLight, outDark, outLight);
        assertImageMatchesSrgbReference(out, img, null, rLut, null, null);
    }

    @Test
    @DisplayName("pixel output sRGB: master and channels combined")
    void pixelOutputSrgbMasterAndChannel() {
        int mInDark = 20;
        int mInLight = 230;
        int mOutDark = 10;
        int mOutLight = 245;

        int rInDark = 35;
        int rInLight = 210;
        int rOutDark = 15;
        int rOutLight = 235;

        int gInDark = 45;
        int gInLight = 205;
        int gOutDark = 25;
        int gOutLight = 225;

        UserPreset preset = createSrgbLevelsPreset(
            mInDark, mInLight, mOutDark, mOutLight,
            rInDark, rInLight, rOutDark, rOutLight,
            gInDark, gInLight, gOutDark, gOutLight,
            0, 255, 0, 255
        );
        Levels filter = new Levels();
        filter.loadUserPreset(preset);

        BufferedImage img = createTestImage();
        BufferedImage out = filter.transform(img, null);

        int[] masterLut = computeReferenceLut(mInDark, mInLight, mOutDark, mOutLight);
        int[] rLut = computeReferenceLut(rInDark, rInLight, rOutDark, rOutLight);
        int[] gLut = computeReferenceLut(gInDark, gInLight, gOutDark, gOutLight);

        assertImageMatchesSrgbReference(out, img, masterLut, rLut, gLut, null);
    }

    @Test
    @DisplayName("pixel output Oklab: matches reference transformation and preserves alpha")
    void pixelOutputOklab() {
        int lInDark = 30;
        int lInLight = 220;
        int lOutDark = 10;
        int lOutLight = 240;

        int aInDark = 40;
        int aInLight = 210;
        int aOutDark = 20;
        int aOutLight = 230;

        int bInDark = 50;
        int bInLight = 200;
        int bOutDark = 30;
        int bOutLight = 220;

        UserPreset preset = createOklabLevelsPreset(
            lInDark, lInLight, lOutDark, lOutLight,
            aInDark, aInLight, aOutDark, aOutLight,
            bInDark, bInLight, bOutDark, bOutLight
        );
        Levels filter = new Levels();
        filter.loadUserPreset(preset);

        BufferedImage img = createTestImage();
        BufferedImage out = filter.transform(img, null);

        int[] lLut = computeReferenceLut(lInDark, lInLight, lOutDark, lOutLight);
        int[] aLut = computeReferenceLut(aInDark, aInLight, aOutDark, aOutLight);
        int[] bLut = computeReferenceLut(bInDark, bInLight, bOutDark, bOutLight);

        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int srcRgb = img.getRGB(x, y);
                int outRgb = out.getRGB(x, y);

                int a = srcRgb >>> 24;
                assertThat(outRgb >>> 24).as("alpha preserved at (%d, %d)", x, y).isEqualTo(a);

                if (a == 0) {
                    assertThat(outRgb).as("alpha 0 pixel unchanged at (%d, %d)", x, y).isEqualTo(srcRgb);
                } else {
                    int expectedRgb = referenceOklabPixel(srcRgb, lLut, aLut, bLut);
                    assertThat(outRgb).as("Oklab pixel at (%d, %d)", x, y).isEqualTo(expectedRgb);
                }
            }
        }
    }

    @Test
    @DisplayName("identity: sRGB default leaves image exactly unchanged")
    void identitySrgb() {
        Levels filter = new Levels();
        BufferedImage img = createIdentityTestImage();
        BufferedImage out = filter.transform(img, null);
        assertImagesEqual(out, img);
    }

    @Test
    @DisplayName("identity: Oklab default leaves image unchanged within +-1 per channel")
    void identityOklab() {
        UserPreset preset = createOklabLevelsPreset(
            0, 255, 0, 255,
            0, 255, 0, 255,
            0, 255, 0, 255
        );
        Levels filter = new Levels();
        filter.loadUserPreset(preset);

        BufferedImage img = createIdentityTestImage();
        BufferedImage out = filter.transform(img, null);

        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int srcRgb = img.getRGB(x, y);
                int outRgb = out.getRGB(x, y);

                int aSrc = srcRgb >>> 24;
                int aOut = outRgb >>> 24;
                assertThat(aOut).as("alpha at (%d, %d)", x, y).isEqualTo(aSrc);

                if (aSrc > 0) {
                    int rSrc = (srcRgb >> 16) & 0xFF;
                    int gSrc = (srcRgb >> 8) & 0xFF;
                    int bSrc = srcRgb & 0xFF;

                    int rOut = (outRgb >> 16) & 0xFF;
                    int gOut = (outRgb >> 8) & 0xFF;
                    int bOut = outRgb & 0xFF;

                    assertThat(Math.abs(rOut - rSrc)).as("red within +-1 at (%d, %d)", x, y).isBetween(0, 2);
                    assertThat(Math.abs(gOut - gSrc)).as("green within +-1 at (%d, %d)", x, y).isBetween(0, 2);
                    assertThat(Math.abs(bOut - bSrc)).as("blue within +-1 at (%d, %d)", x, y).isBetween(0, 2);
                }
            }
        }
    }

    @Test
    @DisplayName("extrapolation outside [inputDark, inputLight] and threshold behavior")
    void extrapolationAndThresholdBehavior() {
        // current extrapolation behavior: the linear mapping is extended beyond
        // the input range and clamped to [0, 255]. This will later be controlled by a flag.
        UserPreset preset = createSrgbLevelsPreset(
            50, 150, 100, 200,
            0, 255, 0, 255,
            0, 255, 0, 255,
            0, 255, 0, 255
        );
        Levels filter = new Levels();
        filter.loadUserPreset(preset);

        BufferedImage img = new BufferedImage(3, 1, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, 0xFF_00_00_00); // 0
        img.setRGB(1, 0, 0xFF_64_64_64); // 100
        img.setRGB(2, 0, 0xFF_FF_FF_FF); // 255

        BufferedImage out = filter.transform(img, null);

        // 0 -> 50, 100 -> 150, 255 -> 255
        assertThat(out.getRGB(0, 0)).isEqualTo(0xFF_32_32_32);
        assertThat(out.getRGB(1, 0)).isEqualTo(0xFF_96_96_96);
        assertThat(out.getRGB(2, 0)).isEqualTo(0xFF_FF_FF_FF);

        // threshold case: inputDark == inputLight == 100
        // values < 100 map to outputDark, values >= 100 map to outputLight
        UserPreset threshPreset = createSrgbLevelsPreset(
            100, 100, 20, 220,
            0, 255, 0, 255,
            0, 255, 0, 255,
            0, 255, 0, 255
        );
        Levels threshFilter = new Levels();
        threshFilter.loadUserPreset(threshPreset);

        BufferedImage threshImg = new BufferedImage(4, 1, BufferedImage.TYPE_INT_ARGB);
        threshImg.setRGB(0, 0, 0xFF_00_00_00); // 0 < 100
        threshImg.setRGB(1, 0, 0xFF_63_63_63); // 99 < 100
        threshImg.setRGB(2, 0, 0xFF_64_64_64); // 100 >= 100
        threshImg.setRGB(3, 0, 0xFF_FF_FF_FF); // 255 >= 100

        BufferedImage threshOut = threshFilter.transform(threshImg, null);

        assertThat(threshOut.getRGB(0, 0)).isEqualTo(0xFF_14_14_14); // 20
        assertThat(threshOut.getRGB(1, 0)).isEqualTo(0xFF_14_14_14); // 20
        assertThat(threshOut.getRGB(2, 0)).isEqualTo(0xFF_DC_DC_DC); // 220
        assertThat(threshOut.getRGB(3, 0)).isEqualTo(0xFF_DC_DC_DC); // 220
    }

    private static UserPreset createSrgbLevelsPreset(
        int rgbInDark, int rgbInLight, int rgbOutDark, int rgbOutLight,
        int rInDark, int rInLight, int rOutDark, int rOutLight,
        int gInDark, int gInLight, int gOutDark, int gOutLight,
        int bInDark, int bInLight, int bOutDark, int bOutLight
    ) {
        UserPreset preset = new UserPreset("test");
        preset.put(ColorSpace.PRESET_KEY, ColorSpace.SRGB.name());
        putLevelsParams(preset, Channel.RGB, rgbInDark, rgbInLight, rgbOutDark, rgbOutLight);
        putLevelsParams(preset, Channel.RED, rInDark, rInLight, rOutDark, rOutLight);
        putLevelsParams(preset, Channel.GREEN, gInDark, gInLight, gOutDark, gOutLight);
        putLevelsParams(preset, Channel.BLUE, bInDark, bInLight, bOutDark, bOutLight);
        return preset;
    }

    private static UserPreset createOklabLevelsPreset(
        int lInDark, int lInLight, int lOutDark, int lOutLight,
        int aInDark, int aInLight, int aOutDark, int aOutLight,
        int bInDark, int bInLight, int bOutDark, int bOutLight
    ) {
        UserPreset preset = new UserPreset("test");
        preset.put(ColorSpace.PRESET_KEY, ColorSpace.OKLAB.name());
        putLevelsParams(preset, Channel.OK_L, lInDark, lInLight, lOutDark, lOutLight);
        putLevelsParams(preset, Channel.OK_A, aInDark, aInLight, aOutDark, aOutLight);
        putLevelsParams(preset, Channel.OK_B, bInDark, bInLight, bOutDark, bOutLight);
        return preset;
    }

    private static void putLevelsParams(UserPreset preset, Channel ch,
                                        int inDark, int inLight, int outDark, int outLight) {
        String prefix = ch.getPresetKey() + " ";
        preset.put(prefix + "Input Dark", String.valueOf(inDark));
        preset.put(prefix + "Input Light", String.valueOf(inLight));
        preset.put(prefix + "Output Dark", String.valueOf(outDark));
        preset.put(prefix + "Output Light", String.valueOf(outLight));
    }

    private static void assertLevelsParams(UserPreset preset, Channel ch,
                                           int inDark, int inLight, int outDark, int outLight) {
        String prefix = ch.getPresetKey() + " ";
        assertThat(preset.get(prefix + "Input Dark")).isEqualTo(String.valueOf(inDark));
        assertThat(preset.get(prefix + "Input Light")).isEqualTo(String.valueOf(inLight));
        assertThat(preset.get(prefix + "Output Dark")).isEqualTo(String.valueOf(outDark));
        assertThat(preset.get(prefix + "Output Light")).isEqualTo(String.valueOf(outLight));
    }

    private static int[] computeReferenceLut(int inDark, int inLight, int outDark, int outLight) {
        int[] lut = new int[256];
        int inputRange = inLight - inDark;
        if (inputRange == 0) {
            int darkVal = Math.clamp(outDark, 0, 255);
            int lightVal = Math.clamp(outLight, 0, 255);
            for (int i = 0; i < 256; i++) {
                lut[i] = (i < inDark) ? darkVal : lightVal;
            }
        } else {
            double multiplier = (double) (outLight - outDark) / inputRange;
            double offset = outDark - multiplier * inDark;
            for (int i = 0; i < 256; i++) {
                lut[i] = Math.clamp((int) Math.round(multiplier * i + offset), 0, 255);
            }
        }
        return lut;
    }

    private static void assertImageMatchesSrgbReference(
        BufferedImage actual, BufferedImage src,
        int[] masterLut, int[] rLut, int[] gLut, int[] bLut
    ) {
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int srcRgb = src.getRGB(x, y);
                int actualRgb = actual.getRGB(x, y);

                int a = srcRgb >>> 24;
                assertThat(actualRgb >>> 24).as("alpha at (%d, %d)", x, y).isEqualTo(a);

                if (a == 0) {
                    continue; // do not assert RGB of alpha-0 pixels
                }

                int r = (srcRgb >> 16) & 0xFF;
                int g = (srcRgb >> 8) & 0xFF;
                int b = srcRgb & 0xFF;

                int expR = masterLut != null ? masterLut[r] : r;
                int expG = masterLut != null ? masterLut[g] : g;
                int expB = masterLut != null ? masterLut[b] : b;

                if (rLut != null) {
                    expR = rLut[expR];
                }
                if (gLut != null) {
                    expG = gLut[expG];
                }
                if (bLut != null) {
                    expB = bLut[expB];
                }

                int expectedRgb = (a << 24) | (expR << 16) | (expG << 8) | expB;
                assertThat(actualRgb).as("pixel at (%d, %d)", x, y).isEqualTo(expectedRgb);
            }
        }
    }

    private static int referenceOklabPixel(int rgb, int[] lLut, int[] aLut, int[] bLut) {
        int alpha = rgb & 0xFF_00_00_00;
        if (alpha == 0) {
            return rgb;
        }

        float[] oklab = ColorSpaces.srgbToOklab(rgb);
        float l = oklab[0];
        float a = oklab[1];
        float b = oklab[2];

        int lIndex = Math.clamp((int) (l * 255.0f + 0.5f), 0, 255);
        float newL = lLut[lIndex] / 255.0f;

        float normA = (a - (-0.5f)) / 1.0f;
        int aIndex = Math.clamp((int) (normA * 255.0f + 0.5f), 0, 255);
        float newA = (aLut[aIndex] / 255.0f) * 1.0f + (-0.5f);

        float normB = (b - (-0.5f)) / 1.0f;
        int bIndex = Math.clamp((int) (normB * 255.0f + 0.5f), 0, 255);
        float newB = (bLut[bIndex] / 255.0f) * 1.0f + (-0.5f);

        return alpha | (ColorSpaces.oklabToSrgb(newL, newA, newB) & 0x00_FF_FF_FF);
    }

    private static BufferedImage createTestImage() {
        int width = 16;
        int height = 16;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = y * width + x;
                int r = i;
                int g = 255 - i;
                int b = (i * 7) % 256;
                // pixel 0 is transparent; others alternate between 255 and 128
                int a = (i == 0) ? 0 : ((i % 2 == 0) ? 255 : 128);
                img.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return img;
    }

    private static BufferedImage createIdentityTestImage() {
        int width = 16;
        int height = 16;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int v = y * width + x; // 0 to 255
                int a = (v == 0) ? 0 : ((v % 2 == 0) ? 255 : 128);
                img.setRGB(x, y, (a << 24) | (v << 16) | (v << 8) | v);
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

    private static Set<String> extractKeys(String presetString) {
        Set<String> keys = new HashSet<>();
        for (String line : presetString.lines().toList()) {
            int idx = line.indexOf('=');
            if (idx > 0) {
                keys.add(line.substring(0, idx).trim());
            }
        }
        return keys;
    }

    private static Set<String> expectedKeysFor(ColorSpace colorSpace) {
        Set<String> keys = new HashSet<>();
        keys.add(ColorSpace.PRESET_KEY);
        for (Channel ch : colorSpace.getChannels()) {
            for (String param : PARAM_NAMES) {
                keys.add(ch.getPresetKey() + " " + param);
            }
        }
        return keys;
    }
}
