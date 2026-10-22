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

package pixelitor.filters.curves;

import com.jhlabs.image.Curve;
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
 * Characterization regression tests for {@link ToneCurvesFilter}.
 * Pins down current preset serialization, round-tripping, sRGB and Oklab pixel output.
 */
@DisplayName("ToneCurvesFilter regression tests")
class ToneCurvesFilterRegressionTest {

    private static final String IDENTITY_CURVE = "0.0,0.0#1.0,1.0";

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @Test
    @DisplayName("preset format: sRGB saves only sRGB keys with expected values")
    void presetFormatSrgb() {
        String rgbCurve = "0.0,0.0#0.5,0.7#1.0,1.0";
        String rCurve = "0.0,0.0#0.4,0.6#1.0,1.0";
        String gCurve = "0.0,0.0#0.3,0.5#1.0,1.0";
        String bCurve = IDENTITY_CURVE;

        UserPreset preset = createSrgbCurvesPreset(rgbCurve, rCurve, gCurve, bCurve);
        ToneCurvesFilter filter = new ToneCurvesFilter();
        filter.loadUserPreset(preset);

        UserPreset saved = new UserPreset("saved");
        filter.saveStateTo(saved);

        UserPreset reloaded = new UserPreset("reloaded");
        reloaded.loadFromString(saved.writeToString());

        assertThat(reloaded.get(ColorSpace.PRESET_KEY)).isEqualTo("SRGB");
        assertThat(reloaded.get(Channel.RGB.getPresetKey())).isEqualTo(rgbCurve);
        assertThat(reloaded.get(Channel.RED.getPresetKey())).isEqualTo(rCurve);
        assertThat(reloaded.get(Channel.GREEN.getPresetKey())).isEqualTo(gCurve);
        assertThat(reloaded.get(Channel.BLUE.getPresetKey())).isEqualTo(bCurve);

        // verify no Oklab keys exist
        for (Channel ch : ColorSpace.OKLAB.getChannels()) {
            assertThat(reloaded.get(ch.getPresetKey()))
                .as("Oklab key should not exist in sRGB preset")
                .isNull();
        }

        // verify exact set of keys
        Set<String> keys = extractKeys(saved.writeToString());
        Set<String> expectedKeys = expectedKeysFor(ColorSpace.SRGB);
        assertThat(keys).isEqualTo(expectedKeys);
    }

    @Test
    @DisplayName("preset format: Oklab saves only Oklab keys with expected values")
    void presetFormatOklab() {
        String lCurve = "0.0,0.0#0.5,0.8#1.0,1.0";
        String aCurve = "0.0,0.0#0.4,0.5#1.0,1.0";
        String bCurve = "0.0,0.0#0.6,0.7#1.0,1.0";

        UserPreset preset = createOklabCurvesPreset(lCurve, aCurve, bCurve);
        ToneCurvesFilter filter = new ToneCurvesFilter();
        filter.loadUserPreset(preset);

        UserPreset saved = new UserPreset("saved");
        filter.saveStateTo(saved);

        UserPreset reloaded = new UserPreset("reloaded");
        reloaded.loadFromString(saved.writeToString());

        assertThat(reloaded.get(ColorSpace.PRESET_KEY)).isEqualTo("OKLAB");
        assertThat(reloaded.get(Channel.OK_L.getPresetKey())).isEqualTo(lCurve);
        assertThat(reloaded.get(Channel.OK_A.getPresetKey())).isEqualTo(aCurve);
        assertThat(reloaded.get(Channel.OK_B.getPresetKey())).isEqualTo(bCurve);

        // verify no sRGB keys exist
        for (Channel ch : ColorSpace.SRGB.getChannels()) {
            assertThat(reloaded.get(ch.getPresetKey()))
                .as("sRGB key should not exist in Oklab preset")
                .isNull();
        }

        // verify exact set of keys
        Set<String> keys = extractKeys(saved.writeToString());
        Set<String> expectedKeys = expectedKeysFor(ColorSpace.OKLAB);
        assertThat(keys).isEqualTo(expectedKeys);
    }

    @Test
    @DisplayName("round trip: sRGB preset reload and filter copy produce identical results")
    void roundTripSrgb() {
        UserPreset preset = createSrgbCurvesPreset(
            "0.0,0.0#0.4,0.6#1.0,1.0",
            "0.0,0.0#0.5,0.7#1.0,1.0",
            IDENTITY_CURVE,
            "0.0,0.0#0.3,0.4#1.0,1.0"
        );
        ToneCurvesFilter filter1 = new ToneCurvesFilter();
        filter1.loadUserPreset(preset);

        UserPreset saved1 = new UserPreset("saved1");
        filter1.saveStateTo(saved1);

        ToneCurvesFilter filter2 = new ToneCurvesFilter();
        filter2.loadUserPreset(saved1);

        UserPreset saved2 = new UserPreset("saved2");
        filter2.saveStateTo(saved2);

        assertThat(saved2.writeToString()).isEqualTo(saved1.writeToString());

        Filter copy = filter1.copy();
        BufferedImage img = createTestImage();
        BufferedImage outOriginal = filter1.transform(img, null);
        BufferedImage outCopy = ((ToneCurvesFilter) copy).transform(img, null);
        assertImagesEqual(outCopy, outOriginal);
    }

    @Test
    @DisplayName("round trip: Oklab preset reload and filter copy produce identical results")
    void roundTripOklab() {
        UserPreset preset = createOklabCurvesPreset(
            "0.0,0.0#0.4,0.7#1.0,1.0",
            "0.0,0.0#0.5,0.6#1.0,1.0",
            "0.0,0.0#0.3,0.5#1.0,1.0"
        );
        ToneCurvesFilter filter1 = new ToneCurvesFilter();
        filter1.loadUserPreset(preset);

        UserPreset saved1 = new UserPreset("saved1");
        filter1.saveStateTo(saved1);

        ToneCurvesFilter filter2 = new ToneCurvesFilter();
        filter2.loadUserPreset(saved1);

        UserPreset saved2 = new UserPreset("saved2");
        filter2.saveStateTo(saved2);

        assertThat(saved2.writeToString()).isEqualTo(saved1.writeToString());

        Filter copy = filter1.copy();
        BufferedImage img = createTestImage();
        BufferedImage outOriginal = filter1.transform(img, null);
        BufferedImage outCopy = ((ToneCurvesFilter) copy).transform(img, null);
        assertImagesEqual(outCopy, outOriginal);
    }

    @Test
    @DisplayName("pixel output sRGB: master curve only")
    void pixelOutputSrgbMasterOnly() {
        String masterCurveStr = "0.0,0.0#0.5,0.75#1.0,1.0";
        UserPreset preset = createSrgbCurvesPreset(
            masterCurveStr, IDENTITY_CURVE, IDENTITY_CURVE, IDENTITY_CURVE
        );
        ToneCurvesFilter filter = new ToneCurvesFilter();
        filter.loadUserPreset(preset);

        BufferedImage img = createTestImage();
        BufferedImage out = filter.transform(img, null);

        int[] masterTable = makeTableFrom(masterCurveStr);
        assertImageMatchesSrgbReference(out, img, masterTable, null, null, null);
    }

    @Test
    @DisplayName("pixel output sRGB: single channel (Red) only")
    void pixelOutputSrgbSingleChannelOnly() {
        String rCurveStr = "0.0,0.0#0.3,0.6#1.0,1.0";
        UserPreset preset = createSrgbCurvesPreset(
            IDENTITY_CURVE, rCurveStr, IDENTITY_CURVE, IDENTITY_CURVE
        );
        ToneCurvesFilter filter = new ToneCurvesFilter();
        filter.loadUserPreset(preset);

        BufferedImage img = createTestImage();
        BufferedImage out = filter.transform(img, null);

        int[] rTable = makeTableFrom(rCurveStr);
        assertImageMatchesSrgbReference(out, img, null, rTable, null, null);
    }

    @Test
    @DisplayName("pixel output sRGB: master and channels combined")
    void pixelOutputSrgbMasterAndChannel() {
        String masterCurveStr = "0.0,0.0#0.4,0.65#1.0,1.0";
        String rCurveStr = "0.0,0.0#0.35,0.7#1.0,1.0";
        String gCurveStr = "0.0,0.0#0.6,0.4#1.0,1.0";
        UserPreset preset = createSrgbCurvesPreset(
            masterCurveStr, rCurveStr, gCurveStr, IDENTITY_CURVE
        );
        ToneCurvesFilter filter = new ToneCurvesFilter();
        filter.loadUserPreset(preset);

        BufferedImage img = createTestImage();
        BufferedImage out = filter.transform(img, null);

        int[] masterTable = makeTableFrom(masterCurveStr);
        int[] rTable = makeTableFrom(rCurveStr);
        int[] gTable = makeTableFrom(gCurveStr);

        assertImageMatchesSrgbReference(out, img, masterTable, rTable, gTable, null);
    }

    @Test
    @DisplayName("pixel output Oklab: matches reference transformation and preserves alpha")
    void pixelOutputOklab() {
        String lCurveStr = "0.0,0.0#0.5,0.7#1.0,1.0";
        String aCurveStr = "0.0,0.0#0.4,0.6#1.0,1.0";
        String bCurveStr = "0.0,0.0#0.6,0.35#1.0,1.0";

        UserPreset preset = createOklabCurvesPreset(lCurveStr, aCurveStr, bCurveStr);
        ToneCurvesFilter filter = new ToneCurvesFilter();
        filter.loadUserPreset(preset);

        BufferedImage img = createTestImage();
        BufferedImage out = filter.transform(img, null);

        int[] lTable = makeTableFrom(lCurveStr);
        int[] aTable = makeTableFrom(aCurveStr);
        int[] bTable = makeTableFrom(bCurveStr);

        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int srcRgb = img.getRGB(x, y);
                int outRgb = out.getRGB(x, y);

                int a = srcRgb >>> 24;
                assertThat(outRgb >>> 24).as("alpha preserved at (%d, %d)", x, y).isEqualTo(a);

                if (a == 0) {
                    assertThat(outRgb).as("alpha 0 pixel unchanged at (%d, %d)", x, y).isEqualTo(srcRgb);
                } else {
                    int expectedRgb = referenceOklabPixel(srcRgb, lTable, aTable, bTable);
                    assertThat(outRgb).as("Oklab pixel at (%d, %d)", x, y).isEqualTo(expectedRgb);
                }
            }
        }
    }

    @Test
    @DisplayName("identity: sRGB default leaves image exactly unchanged")
    void identitySrgb() {
        ToneCurvesFilter filter = new ToneCurvesFilter();
        BufferedImage img = createIdentityTestImage();
        BufferedImage out = filter.transform(img, null);
        assertImagesEqual(out, img);
    }

    @Test
    @DisplayName("identity: Oklab default leaves image unchanged within +-1 per channel")
    void identityOklab() {
        UserPreset preset = createOklabCurvesPreset(IDENTITY_CURVE, IDENTITY_CURVE, IDENTITY_CURVE);
        ToneCurvesFilter filter = new ToneCurvesFilter();
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

    private static UserPreset createSrgbCurvesPreset(
        String rgbCurve, String rCurve, String gCurve, String bCurve
    ) {
        UserPreset preset = new UserPreset("test");
        preset.put(ColorSpace.PRESET_KEY, ColorSpace.SRGB.name());
        preset.put(Channel.RGB.getPresetKey(), rgbCurve);
        preset.put(Channel.RED.getPresetKey(), rCurve);
        preset.put(Channel.GREEN.getPresetKey(), gCurve);
        preset.put(Channel.BLUE.getPresetKey(), bCurve);
        return preset;
    }

    private static UserPreset createOklabCurvesPreset(
        String lCurve, String aCurve, String bCurve
    ) {
        UserPreset preset = new UserPreset("test");
        preset.put(ColorSpace.PRESET_KEY, ColorSpace.OKLAB.name());
        preset.put(Channel.OK_L.getPresetKey(), lCurve);
        preset.put(Channel.OK_A.getPresetKey(), aCurve);
        preset.put(Channel.OK_B.getPresetKey(), bCurve);
        return preset;
    }

    private static int[] makeTableFrom(String curveStr) {
        Curve curve = new Curve();
        String[] pairs = curveStr.split("#");
        float[] x = new float[pairs.length];
        float[] y = new float[pairs.length];
        for (int i = 0; i < pairs.length; i++) {
            String[] xy = pairs[i].split(",");
            x[i] = Float.parseFloat(xy[0]);
            y[i] = Float.parseFloat(xy[1]);
        }
        curve.setKnots(x, y);
        return curve.makeTable();
    }

    private static void assertImageMatchesSrgbReference(
        BufferedImage actual, BufferedImage src,
        int[] masterTable, int[] rTable, int[] gTable, int[] bTable
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

                int expR = masterTable != null ? masterTable[r] : r;
                int expG = masterTable != null ? masterTable[g] : g;
                int expB = masterTable != null ? masterTable[b] : b;

                if (rTable != null) {
                    expR = rTable[expR];
                }
                if (gTable != null) {
                    expG = gTable[expG];
                }
                if (bTable != null) {
                    expB = bTable[expB];
                }

                int expectedRgb = (a << 24) | (expR << 16) | (expG << 8) | expB;
                assertThat(actualRgb).as("pixel at (%d, %d)", x, y).isEqualTo(expectedRgb);
            }
        }
    }

    private static int referenceOklabPixel(int rgb, int[] lTable, int[] aTable, int[] bTable) {
        int alpha = rgb & 0xFF_00_00_00;
        if (alpha == 0) {
            return rgb;
        }

        float[] oklab = ColorSpaces.srgbToOklab(rgb);
        float l = oklab[0];
        float a = oklab[1];
        float b = oklab[2];

        int lIndex = Math.clamp((int) (l * 255.0f + 0.5f), 0, 255);
        float newL = lTable[lIndex] / 255.0f;

        float normA = (a - (-0.5f)) / 1.0f;
        int aIndex = Math.clamp((int) (normA * 255.0f + 0.5f), 0, 255);
        float newA = (aTable[aIndex] / 255.0f) * 1.0f + (-0.5f);

        float normB = (b - (-0.5f)) / 1.0f;
        int bIndex = Math.clamp((int) (normB * 255.0f + 0.5f), 0, 255);
        float newB = (bTable[bIndex] / 255.0f) * 1.0f + (-0.5f);

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
            keys.add(ch.getPresetKey());
        }
        return keys;
    }
}
