/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import org.mockito.ArgumentCaptor;
import pixelitor.TestHelper;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.gui.UserPreset;


import static org.mockito.Mockito.*;

@DisplayName("Levels tests")
@TestMethodOrder(MethodOrderer.Random.class)
class LevelsTest {
    private Levels levels;
    private ArgumentCaptor<RGBLookup> captor;
    private LevelsModel model;
    private FilterGUI filterGUI;
    private ChannelLevelsModel rgbPage;
    private ChannelLevelsModel rPage;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        levels = mock(Levels.class);
        captor = ArgumentCaptor.forClass(RGBLookup.class);
        model = new LevelsModel(levels);
        filterGUI = mock(FilterGUI.class);
        model.setLastGUI(filterGUI);
        rgbPage = model.getSubModels()[0];
        rPage = model.getSubModels()[1];
    }

    @Test
    void defaultSettingsProduceIdentity() {
        model.resetAllToDefault();

        var lookup = getCalculatedLookup();

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
        RGBLookup lookup = getCalculatedLookup();

        checkRGBMapping(lookup, 0, 0, 0, 0); // black remains black
        checkRGBMapping(lookup, 255, 255, 255, 255); // white remains white
        checkRGBMapping(lookup, 128, 46, 46, 46); // gray becomes darker
    }

    @Test
    @DisplayName("page Red, input dark = 100")
    void pageR_inputDark100() {
        rPage.getInputDark().setValue(100);
        RGBLookup lookup = getCalculatedLookup();

        checkRGBMapping(lookup, 0, 0, 0, 0); // black remains black
        checkRGBMapping(lookup, 255, 255, 255, 255); // white remains white
        checkRGBMapping(lookup, 128, 46, 128, 128); // only the red channel is affected
    }

    @Test
    @DisplayName("page RGB, input light = 150")
    void pageRGB_inputLight150() {
        rgbPage.getInputLight().setValue(150);
        RGBLookup lookup = getCalculatedLookup();

        checkRGBMapping(lookup, 0, 0, 0, 0); // black remains black
        checkRGBMapping(lookup, 255, 255, 255, 255); // white remains white
        checkRGBMapping(lookup, 128, 217, 217, 217); // gray becomes lighter (ps value: 218)
    }

    @Test
    @DisplayName("page Red, input light = 150")
    void pageR_inputLight150() {
        rPage.getInputLight().setValue(150);
        RGBLookup lookup = getCalculatedLookup();

        checkRGBMapping(lookup, 0, 0, 0, 0); // black remains black
        checkRGBMapping(lookup, 255, 255, 255, 255); // white remains white
        checkRGBMapping(lookup, 128, 217, 128, 128); // only the red channel is affected
    }

    @Test
    @DisplayName("page RGB, output dark = 100")
    void pageRGB_outputDark100() {
        rgbPage.getOutputDark().setValue(100);
        RGBLookup lookup = getCalculatedLookup();

        checkRGBMapping(lookup, 0, 100, 100, 100); // black becomes (100, 100, 100)
        checkRGBMapping(lookup, 255, 255, 255, 255); // white remains white
        checkRGBMapping(lookup, 128, 177, 177, 177); // gray becomes lighter (ps value: 178)
    }

    @Test
    @DisplayName("page Red, output dark = 100")
    void pageR_outputDark100() {
        rPage.getOutputDark().setValue(100);
        RGBLookup lookup = getCalculatedLookup();

        checkRGBMapping(lookup, 0, 100, 0, 0); // only the red channel is affected
        checkRGBMapping(lookup, 255, 255, 255, 255); // white remains white
        checkRGBMapping(lookup, 128, 177, 128, 128); // only the red channel is affected
    }

    @Test
    @DisplayName("page RGB, output light = 150")
    void pageRGB_outputLight150() {
        rgbPage.getOutputLight().setValue(150);
        RGBLookup lookup = getCalculatedLookup();

        checkRGBMapping(lookup, 0, 0, 0, 0); // black remains black
        checkRGBMapping(lookup, 255, 150, 150, 150); // white becomes (150, 150, 150)
        checkRGBMapping(lookup, 128, 75, 75, 75); // gray becomes darker
    }

    @Test
    @DisplayName("page Red, output light = 150")
    void pageR_outputLight150() {
        rPage.getOutputLight().setValue(150);
        RGBLookup lookup = getCalculatedLookup();

        checkRGBMapping(lookup, 0, 0, 0, 0); // black remains black
        checkRGBMapping(lookup, 255, 150, 255, 255); // only the red channel is affected
        checkRGBMapping(lookup, 128, 75, 128, 128); // // only the red channel is affected
    }

    private RGBLookup getCalculatedLookup() {
        verify(filterGUI, times(1)).settingsChanged(false);
        verify(levels, times(1)).setRGBLookup(captor.capture());
        return captor.getValue();
    }

    private static void checkRedMapping(RGBLookup lookup, int input, int expected) {
        Assertions.assertEquals(expected, lookup.mapRed(input));
    }

    private static void checkGreenMapping(RGBLookup lookup, int input, int expected) {
        Assertions.assertEquals(expected, lookup.mapGreen(input));
    }

    private static void checkBlueMapping(RGBLookup lookup, int input, int expected) {
        Assertions.assertEquals(expected, lookup.mapBlue(input));
    }

    private static void checkRGBMapping(RGBLookup lookup, int input,
                                        int expectedRed, int expectedGreen, int expectedBlue) {
        Assertions.assertEquals(expectedRed, lookup.mapRed(input));
        Assertions.assertEquals(expectedGreen, lookup.mapGreen(input));
        Assertions.assertEquals(expectedBlue, lookup.mapBlue(input));
    }

}