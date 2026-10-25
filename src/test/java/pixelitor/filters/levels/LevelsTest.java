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

import org.junit.jupiter.api.*;
import pixelitor.TestHelper;
import pixelitor.filters.curves.ToneCurve;
import pixelitor.filters.gui.UserPreset;
import pixelitor.filters.lookup.*;
import pixelitor.filters.util.Channel;
import pixelitor.filters.util.ColorSpace;

import java.awt.image.BufferedImageOp;
import java.awt.image.ShortLookupTable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Levels tests")
@TestMethodOrder(MethodOrderer.Random.class)
class LevelsTest {
    private LevelsModel model;
    private final AtomicInteger previewCount = new AtomicInteger();
    private ChannelLevelsModel rgbPage;
    private ChannelLevelsModel rPage;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        model = new LevelsModel();
        previewCount.set(0);
        model.setAdjustmentListener(previewCount::incrementAndGet);

        rgbPage = model.getAdjustment(Channel.RGB);
        rPage = model.getAdjustment(Channel.RED);
    }

    @Test
    void filterOpIsInitializedImmediately() {
        var newModel = new LevelsModel();
        BufferedImageOp op = newModel.createOp();
        assertThat(op).isNotNull();
        if (LookupOps.isSrgbFastLookup()) {
            assertThat(op).isInstanceOf(FastLookupOp.class);
        } else {
            assertThat(op).isInstanceOf(RgbTransferFilter.class);
        }
    }

    @Test
    void resetAllLeavesFilterOpValid() {
        model.setColorSpace(ColorSpace.OKLAB);
        model.resetAll();
        BufferedImageOp op = model.createOp();
        assertThat(op).isNotNull();
        if (LookupOps.isSrgbFastLookup()) {
            assertThat(op).isInstanceOf(FastLookupOp.class);
        } else {
            assertThat(op).isInstanceOf(RgbTransferFilter.class);
        }
    }

    @Test
    void defaultSettingsProduceIdentity() {
        model.resetAll();

        var lookup = getCalculatedSrgbLookup();

        for (int i = 0; i < 256; i++) {
            checkRedMapping(lookup, i, i);
            checkGreenMapping(lookup, i, i);
            checkBlueMapping(lookup, i, i);
        }
    }

    @Test
    @DisplayName("page RGB, input dark = 100")
    void pageRGB_inputDark100() {
        rgbPage.getInputDark().setValue(100);
        RGBLookup lookup = getCalculatedSrgbLookup();

        checkRGBMapping(lookup, 0, 0, 0, 0); // black remains black
        checkRGBMapping(lookup, 255, 255, 255, 255); // white remains white
        checkRGBMapping(lookup, 128, 46, 46, 46); // gray becomes darker
    }

    @Test
    @DisplayName("page Red, input dark = 100")
    void pageR_inputDark100() {
        rPage.getInputDark().setValue(100);
        RGBLookup lookup = getCalculatedSrgbLookup();

        checkRGBMapping(lookup, 0, 0, 0, 0); // black remains black
        checkRGBMapping(lookup, 255, 255, 255, 255); // white remains white
        checkRGBMapping(lookup, 128, 46, 128, 128); // only the red channel is affected
    }

    @Test
    @DisplayName("page RGB, input light = 150")
    void pageRGB_inputLight150() {
        rgbPage.getInputLight().setValue(150);
        RGBLookup lookup = getCalculatedSrgbLookup();

        checkRGBMapping(lookup, 0, 0, 0, 0); // black remains black
        checkRGBMapping(lookup, 255, 255, 255, 255); // white remains white
        checkRGBMapping(lookup, 128, 218, 218, 218); // gray becomes lighter
    }

    @Test
    @DisplayName("page Red, input light = 150")
    void pageR_inputLight150() {
        rPage.getInputLight().setValue(150);
        RGBLookup lookup = getCalculatedSrgbLookup();

        checkRGBMapping(lookup, 0, 0, 0, 0); // black remains black
        checkRGBMapping(lookup, 255, 255, 255, 255); // white remains white
        checkRGBMapping(lookup, 128, 218, 128, 128); // only the red channel is affected
    }

    @Test
    @DisplayName("page RGB, output dark = 100")
    void pageRGB_outputDark100() {
        rgbPage.getOutputDark().setValue(100);
        RGBLookup lookup = getCalculatedSrgbLookup();

        checkRGBMapping(lookup, 0, 100, 100, 100); // black becomes (100, 100, 100)
        checkRGBMapping(lookup, 255, 255, 255, 255); // white remains white
        checkRGBMapping(lookup, 128, 178, 178, 178); // gray becomes lighter
    }

    @Test
    @DisplayName("page Red, output dark = 100")
    void pageR_outputDark100() {
        rPage.getOutputDark().setValue(100);
        RGBLookup lookup = getCalculatedSrgbLookup();

        checkRGBMapping(lookup, 0, 100, 0, 0); // only the red channel is affected
        checkRGBMapping(lookup, 255, 255, 255, 255); // white remains white
        checkRGBMapping(lookup, 128, 178, 128, 128); // only the red channel is affected
    }

    @Test
    @DisplayName("page RGB, output light = 150")
    void pageRGB_outputLight150() {
        rgbPage.getOutputLight().setValue(150);
        RGBLookup lookup = getCalculatedSrgbLookup();

        checkRGBMapping(lookup, 0, 0, 0, 0); // black remains black
        checkRGBMapping(lookup, 255, 150, 150, 150); // white becomes (150, 150, 150)
        checkRGBMapping(lookup, 128, 75, 75, 75); // gray becomes darker
    }

    @Test
    @DisplayName("page Red, output light = 150")
    void pageR_outputLight150() {
        rPage.getOutputLight().setValue(150);
        RGBLookup lookup = getCalculatedSrgbLookup();

        checkRGBMapping(lookup, 0, 0, 0, 0); // black remains black
        checkRGBMapping(lookup, 255, 150, 255, 255); // only the red channel is affected
        checkRGBMapping(lookup, 128, 75, 128, 128); // only the red channel is affected
    }

    @Test
    @DisplayName("Oklab, page L, input dark = 100")
    void pageOklabL_inputDark100() {
        model.setColorSpace(ColorSpace.OKLAB);
        model.fireAdjustmentChanged();
        ChannelLevelsModel okLPage = model.getAdjustment(Channel.OK_L);
        okLPage.getInputDark().setValue(100);

        BufferedImageOp op = model.createOp();
        assertThat(op)
            .isNotNull()
            .isInstanceOf(OklabLookupFilter.class);
        OklabLookupFilter oklabFilter = (OklabLookupFilter) op;

        // black (L=0) should be unaffected by an input range adjustment that starts at 100
        int black = 0xFF_00_00_00;
        assertThat(oklabFilter.processPixel(0, 0, black)).isEqualTo(black);

        // White (L=1.0) should also be unaffected.
        // Due to floating point inaccuracies in color space conversion, the result might not be
        // exactly 0xFFFFFFFF. We check that it's very close to white.
        int white = 0xFFFFFFFF;
        int whiteResult = oklabFilter.processPixel(0, 0, white);
        int r = (whiteResult >> 16) & 0xFF;
        int g = (whiteResult >> 8) & 0xFF;
        int b = whiteResult & 0xFF;
        assertThat(r).as("Red component should be > 250").isGreaterThan(250);
        assertThat(g).as("Green component should be > 250").isGreaterThan(250);
        assertThat(b).as("Blue component should be > 250").isGreaterThan(250);

        // a mid-gray should become darker because its L value is stretched down
        int gray = 0xFF808080; // 128, 128, 128
        int result = oklabFilter.processPixel(0, 0, gray);
        int resultRed = (result >> 16) & 0xFF;
        assertThat(resultRed)
            .as("Gray should become darker")
            .isLessThan(128);

        assertThat(previewCount.get()).isPositive();
    }

    @Test
    @DisplayName("resetAll selects sRGB and its primary channel and resets all adjustments")
    void resetAll_selectsSrgbAndResetsAll() {
        model.setColorSpace(ColorSpace.OKLAB);
        model.setActiveChannel(Channel.OK_A);
        model.getAdjustment(Channel.RGB).getInputDark().setValue(50);
        model.getAdjustment(Channel.OK_A).getInputLight().setValue(200);

        previewCount.set(0);
        model.resetAll();

        assertThat(model.getColorSpace()).isEqualTo(ColorSpace.SRGB);
        assertThat(model.getActiveChannel()).isEqualTo(Channel.RGB);
        assertThat(previewCount.get()).isEqualTo(1);

        for (ChannelLevelsModel adj : model.getAllAdjustments()) {
            assertThat(adj.getInputDark().getValue()).isEqualTo(0);
            assertThat(adj.getInputLight().getValue()).isEqualTo(255);
            assertThat(adj.getOutputDark().getValue()).isEqualTo(0);
            assertThat(adj.getOutputLight().getValue()).isEqualTo(255);
        }
    }

    @Test
    @DisplayName("randomize preserves color space and active channel, randomizing only the active channel")
    void randomize_preservesColorSpaceAndRandomizesOnlyActiveChannel() {
        model.setColorSpace(ColorSpace.OKLAB);
        model.setActiveChannel(Channel.OK_A);
        model.getAdjustment(Channel.OK_B).getInputDark().setValue(50);

        previewCount.set(0);
        model.randomize();

        assertThat(model.getColorSpace()).isEqualTo(ColorSpace.OKLAB);
        assertThat(model.getActiveChannel()).isEqualTo(Channel.OK_A);
        assertThat(previewCount.get()).isEqualTo(1);

        ChannelLevelsModel active = model.getActiveAdjustment();
        assertThat(active.getInputDark().getValue()).isBetween(0, 127);
        assertThat(active.getInputLight().getValue()).isBetween(128, 255);
        assertThat(active.getOutputDark().getValue()).isEqualTo(0);
        assertThat(active.getOutputLight().getValue()).isEqualTo(255);

        for (ChannelLevelsModel adj : model.getAllAdjustments()) {
            if (adj != active) {
                assertThat(adj.getInputDark().getValue()).isEqualTo(0);
                assertThat(adj.getInputLight().getValue()).isEqualTo(255);
                assertThat(adj.getOutputDark().getValue()).isEqualTo(0);
                assertThat(adj.getOutputLight().getValue()).isEqualTo(255);
            }
        }
    }

    @Test
    @DisplayName("loadStateFrom with missing keys uses defaults and does not throw")
    void loadStateFrom_missingKeysUsesDefaults() {
        model.setColorSpace(ColorSpace.OKLAB);
        model.getAdjustment(Channel.OK_L).getInputDark().setValue(60);
        model.getAdjustment(Channel.RGB).getInputDark().setValue(70);

        UserPreset partialPreset = new UserPreset("partial");
        partialPreset.put(ColorSpace.PRESET_KEY, ColorSpace.OKLAB.name());
        partialPreset.put("L Input Dark", "35");

        model.loadStateFrom(partialPreset);

        assertThat(model.getColorSpace()).isEqualTo(ColorSpace.OKLAB);
        assertThat(model.getActiveChannel()).isEqualTo(Channel.OK_L);
        assertThat(model.getAdjustment(Channel.OK_L).getInputDark().getValue()).isEqualTo(35);
        assertThat(model.getAdjustment(Channel.OK_L).getInputLight().getValue()).isEqualTo(255);
        assertThat(model.getAdjustment(Channel.OK_L).getOutputDark().getValue()).isEqualTo(0);
        assertThat(model.getAdjustment(Channel.OK_L).getOutputLight().getValue()).isEqualTo(255);

        assertThat(model.getAdjustment(Channel.OK_A).getInputDark().getValue()).isEqualTo(0);
        assertThat(model.getAdjustment(Channel.OK_B).getInputDark().getValue()).isEqualTo(0);
        assertThat(model.getAdjustment(Channel.RGB).getInputDark().getValue()).isEqualTo(0);

        // completely empty preset defaults to SRGB with all defaults
        UserPreset emptyPreset = new UserPreset("empty");
        model.loadStateFrom(emptyPreset);

        assertThat(model.getColorSpace()).isEqualTo(ColorSpace.SRGB);
        assertThat(model.getActiveChannel()).isEqualTo(Channel.RGB);
        for (ChannelLevelsModel adj : model.getAllAdjustments()) {
            assertThat(adj.getInputDark().getValue()).isEqualTo(0);
            assertThat(adj.getInputLight().getValue()).isEqualTo(255);
            assertThat(adj.getOutputDark().getValue()).isEqualTo(0);
            assertThat(adj.getOutputLight().getValue()).isEqualTo(255);
        }
    }

    @Test
    @DisplayName("loadStateFrom notifies exactly once when listener attached")
    void loadStateFrom_notifiesExactlyOnce() {
        UserPreset preset = new UserPreset("test");
        preset.put(ColorSpace.PRESET_KEY, ColorSpace.OKLAB.name());

        previewCount.set(0);
        model.loadStateFrom(preset);

        assertThat(previewCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("saveStateTo saves only the active color space and does not notify")
    void saveStateTo_savesOnlyActiveColorSpaceAndDoesNotNotify() {
        model.setColorSpace(ColorSpace.SRGB);
        model.getAdjustment(Channel.RGB).getInputDark().setValue(15);
        model.getAdjustment(Channel.OK_L).getInputDark().setValue(45);

        previewCount.set(0);
        UserPreset srgbPreset = new UserPreset("srgb");
        model.saveStateTo(srgbPreset);

        assertThat(previewCount.get()).isZero();
        assertThat(srgbPreset.get(ColorSpace.PRESET_KEY)).isEqualTo("SRGB");
        assertThat(srgbPreset.get("rgb Input Dark")).isEqualTo("15");
        assertThat(srgbPreset.get("L Input Dark")).isNull();

        model.setColorSpace(ColorSpace.OKLAB);
        previewCount.set(0);
        UserPreset oklabPreset = new UserPreset("oklab");
        model.saveStateTo(oklabPreset);

        assertThat(previewCount.get()).isZero();
        assertThat(oklabPreset.get(ColorSpace.PRESET_KEY)).isEqualTo("OKLAB");
        assertThat(oklabPreset.get("L Input Dark")).isEqualTo("45");
        assertThat(oklabPreset.get("rgb Input Dark")).isNull();
    }

    @Nested
    @DisplayName("ChannelAdjustment sub-model tests")
    class ChannelAdjustmentTests {
        @Test
        @DisplayName("ChannelLevelsModel.loadFrom with missing keys keeps defaults")
        void channelLevelsModel_loadFrom_missingKeysKeepsDefaults() {
            var levels = new ChannelLevelsModel(Channel.RED, null);

            // empty preset has no keys for this channel: defaults must remain untouched
            var emptyPreset = new UserPreset("empty");
            levels.loadFrom(emptyPreset);

            assertThat(levels.getInputDark().getValue()).isEqualTo(0);
            assertThat(levels.getInputLight().getValue()).isEqualTo(255);
            assertThat(levels.getOutputDark().getValue()).isEqualTo(0);
            assertThat(levels.getOutputLight().getValue()).isEqualTo(255);

            // preset with only one key set: missing keys must still keep their defaults
            var partialPreset = new UserPreset("partial");
            partialPreset.put("red Input Dark", "50");
            levels.loadFrom(partialPreset);

            assertThat(levels.getInputDark().getValue()).isEqualTo(50);
            assertThat(levels.getInputLight().getValue()).isEqualTo(255);
            assertThat(levels.getOutputDark().getValue()).isEqualTo(0);
            assertThat(levels.getOutputLight().getValue()).isEqualTo(255);
        }

        @Test
        @DisplayName("ChannelLevelsModel saveTo / loadFrom round trip")
        void channelLevelsModel_saveToLoadFrom_roundTrip() {
            var source = new ChannelLevelsModel(Channel.GREEN, null);
            source.getInputDark().setValue(25);
            source.getInputLight().setValue(220);
            source.getOutputDark().setValue(15);
            source.getOutputLight().setValue(240);

            var preset = new UserPreset("test");
            source.saveTo(preset);

            var target = new ChannelLevelsModel(Channel.GREEN, null);
            target.loadFrom(preset);

            assertThat(target.getInputDark().getValue()).isEqualTo(25);
            assertThat(target.getInputLight().getValue()).isEqualTo(220);
            assertThat(target.getOutputDark().getValue()).isEqualTo(15);
            assertThat(target.getOutputLight().getValue()).isEqualTo(240);
            assertThat(target.toLookup().getTable()).isEqualTo(source.toLookup().getTable());
        }

        @Test
        @DisplayName("ChannelLevelsModel randomize sets input dark in [0,127] and input light in [128,255]")
        void channelLevelsModel_randomize() {
            var levels = new ChannelLevelsModel(Channel.RGB, null);
            levels.reset();

            levels.randomize();

            assertThat(levels.getInputDark().getValue()).isBetween(0, 127);
            assertThat(levels.getInputLight().getValue()).isBetween(128, 255);
            assertThat(levels.getOutputDark().getValue()).isEqualTo(0);
            assertThat(levels.getOutputLight().getValue()).isEqualTo(255);
        }

        @Test
        @DisplayName("ToneCurve saveTo / loadFrom round trip")
        void toneCurve_saveToLoadFrom_roundTrip() {
            var source = new ToneCurve(Channel.BLUE);
            source.addKnot(0.25f, 0.4f, false);
            source.addKnot(0.75f, 0.6f, false);

            var preset = new UserPreset("test");
            source.saveTo(preset);

            var target = new ToneCurve(Channel.BLUE);
            target.loadFrom(preset);

            assertThat(target.curveData.x).isEqualTo(source.curveData.x);
            assertThat(target.curveData.y).isEqualTo(source.curveData.y);
            assertThat(target.toLookup().getTable()).isEqualTo(source.toLookup().getTable());
        }

        @Test
        @DisplayName("ToneCurve randomize adds exactly two knots to a reset curve")
        void toneCurve_randomize_addsExactlyTwoKnotsToResetCurve() {
            var curve = new ToneCurve(Channel.OK_L);
            curve.reset();
            assertThat(curve.curveData.x).hasSize(2);

            curve.randomize();

            // reset curve had 2 knots (0,0) and (1,1); randomize must add exactly 2 knots
            assertThat(curve.curveData.x).hasSize(4);
            assertThat(curve.curveData.y).hasSize(4);
            assertThat(curve.curveData.x[0]).isEqualTo(0.0f);
            assertThat(curve.curveData.y[0]).isEqualTo(0.0f);
            assertThat(curve.curveData.x[3]).isEqualTo(1.0f);
            assertThat(curve.curveData.y[3]).isEqualTo(1.0f);
            assertThat(curve.curveData.x[1]).isBetween(0.1f, 0.4f);
            assertThat(curve.curveData.x[2]).isBetween(0.6f, 0.9f);
        }
    }

    private RGBLookup getCalculatedSrgbLookup() {
        // Verify that the model notified to update the preview.
        // This is triggered by the setValue() calls in the test methods.
        assertThat(previewCount.get()).isPositive();

        // get the BufferedImageOp that the model configured
        BufferedImageOp op = model.createOp();
        assertThat(op).isNotNull();

        if (LookupOps.isSrgbFastLookup()) {
            assertThat(op).isInstanceOf(FastLookupOp.class);
            FastLookupOp fastOp = (FastLookupOp) op;
            ShortLookupTable table = fastOp.getLookupTable();
            short[][] data = table.getTable();
            return new RGBLookup(data[0], data[1], data[2]);
        } else {
            assertThat(op).isInstanceOf(RgbTransferFilter.class);
            return ((RgbTransferFilter) op).getLookup();
        }
    }

    private static void checkRedMapping(RGBLookup lookup, int input, int expected) {
        assertThat(mapRed(lookup, input)).isEqualTo(expected);
    }

    private static void checkGreenMapping(RGBLookup lookup, int input, int expected) {
        assertThat(mapGreen(lookup, input)).isEqualTo(expected);
    }

    private static void checkBlueMapping(RGBLookup lookup, int input, int expected) {
        assertThat(mapBlue(lookup, input)).isEqualTo(expected);
    }

    private static void checkRGBMapping(RGBLookup lookup, int input,
                                        int expectedRed, int expectedGreen, int expectedBlue) {
        assertThat(mapRed(lookup, input)).as("red").isEqualTo(expectedRed);
        assertThat(mapGreen(lookup, input)).as("green").isEqualTo(expectedGreen);
        assertThat(mapBlue(lookup, input)).as("blue").isEqualTo(expectedBlue);
    }

    private static int mapRed(RGBLookup lookup, int input) {
        return lookup.getRedLut()[input];
    }

    private static int mapGreen(RGBLookup lookup, int input) {
        return lookup.getGreenLut()[input];
    }

    private static int mapBlue(RGBLookup lookup, int input) {
        return lookup.getBlueLut()[input];
    }
}
