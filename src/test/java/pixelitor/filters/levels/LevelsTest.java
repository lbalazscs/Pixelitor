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
import pixelitor.filters.lookup.FastLookupOp;
import pixelitor.filters.lookup.RGBLookup;
import pixelitor.filters.util.Channel;
import pixelitor.filters.util.ColorSpace;

import java.awt.image.BufferedImageOp;
import java.awt.image.ShortLookupTable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("Levels tests")
@TestMethodOrder(MethodOrderer.Random.class)
class LevelsTest {
    private LevelsModel model;
    private LevelsGUI filterGUI;
    private ChannelLevelsModel rgbPage;
    private ChannelLevelsModel rPage;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        model = new LevelsModel();
        filterGUI = mock(LevelsGUI.class);
        model.setLevelsGUI(filterGUI);

        rgbPage = model.getModelForChannel(Channel.RGB);
        rPage = model.getModelForChannel(Channel.RED);
    }

    @Test
    void defaultSettingsProduceIdentity() {
        model.resetAllAndRun();

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
        checkRGBMapping(lookup, 128, 217, 217, 217); // gray becomes lighter (ps value: 218)
    }

    @Test
    @DisplayName("page Red, input light = 150")
    void pageR_inputLight150() {
        rPage.getInputLight().setValue(150);
        RGBLookup lookup = getCalculatedSrgbLookup();

        checkRGBMapping(lookup, 0, 0, 0, 0); // black remains black
        checkRGBMapping(lookup, 255, 255, 255, 255); // white remains white
        checkRGBMapping(lookup, 128, 217, 128, 128); // only the red channel is affected
    }

    @Test
    @DisplayName("page RGB, output dark = 100")
    void pageRGB_outputDark100() {
        rgbPage.getOutputDark().setValue(100);
        RGBLookup lookup = getCalculatedSrgbLookup();

        checkRGBMapping(lookup, 0, 100, 100, 100); // black becomes (100, 100, 100)
        checkRGBMapping(lookup, 255, 255, 255, 255); // white remains white
        checkRGBMapping(lookup, 128, 177, 177, 177); // gray becomes lighter (ps value: 178)
    }

    @Test
    @DisplayName("page Red, output dark = 100")
    void pageR_outputDark100() {
        rPage.getOutputDark().setValue(100);
        RGBLookup lookup = getCalculatedSrgbLookup();

        checkRGBMapping(lookup, 0, 100, 0, 0); // only the red channel is affected
        checkRGBMapping(lookup, 255, 255, 255, 255); // white remains white
        checkRGBMapping(lookup, 128, 177, 128, 128); // only the red channel is affected
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
        checkRGBMapping(lookup, 128, 75, 128, 128); // // only the red channel is affected
    }

    @Test
    @DisplayName("Oklab, page L, input dark = 100")
    void pageOklabL_inputDark100() {
        model.setColorSpace(ColorSpace.OKLAB, true);
        // the Oklab L model is the 5th one (index 4)
        ChannelLevelsModel okLPage = model.getModelForChannel(Channel.OK_L);
        okLPage.getInputDark().setValue(100);

        BufferedImageOp op = model.getFilterOp();
        assertThat(op)
            .isNotNull()
            .isInstanceOf(OklabLevelsFilter.class);
        OklabLevelsFilter oklabFilter = (OklabLevelsFilter) op;

        // black (L=0) should be unaffected by an input range adjustment that starts at 100
        int black = 0xFF000000;
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
    }

    private RGBLookup getCalculatedSrgbLookup() {
        // Verify that the model notified the GUI to update the preview.
        // This is triggered by the setValue() calls in the test methods.
        verify(filterGUI, atLeastOnce()).startPreview(false);

        // get the BufferedImageOp that the model configured
        BufferedImageOp op = model.getFilterOp();
        assertThat(op)
            .isNotNull()
            .isInstanceOf(FastLookupOp.class);

        // extract the lookup table data and reconstruct an RGBLookup for assertions
        FastLookupOp fastOp = (FastLookupOp) op;
        ShortLookupTable table = fastOp.getLookupTable();
        short[][] data = table.getTable();

        return new RGBLookup(data[0], data[1], data[2]);
    }

    private static void checkRedMapping(RGBLookup lookup, int input, int expected) {
        assertThat(lookup.mapRed(input)).isEqualTo(expected);
    }

    private static void checkGreenMapping(RGBLookup lookup, int input, int expected) {
        assertThat(lookup.mapGreen(input)).isEqualTo(expected);
    }

    private static void checkBlueMapping(RGBLookup lookup, int input, int expected) {
        assertThat(lookup.mapBlue(input)).isEqualTo(expected);
    }

    private static void checkRGBMapping(RGBLookup lookup, int input,
                                        int expectedRed, int expectedGreen, int expectedBlue) {
        assertThat(lookup.mapRed(input)).isEqualTo(expectedRed);
        assertThat(lookup.mapGreen(input)).isEqualTo(expectedGreen);
        assertThat(lookup.mapBlue(input)).isEqualTo(expectedBlue);
    }
}
