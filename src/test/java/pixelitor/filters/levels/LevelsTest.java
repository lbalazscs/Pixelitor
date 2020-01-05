/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import pixelitor.filters.gui.PreviewExecutor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class LevelsTest {
    private Levels levels;
    private ArgumentCaptor<RGBLookup> captor;
    private LevelsModel model;
    private PreviewExecutor executor;
    private OneChannelLevelsModel rgbPage;
    private OneChannelLevelsModel rPage;

    @Before
    public void setUp() {
        levels = mock(Levels.class);
        captor = ArgumentCaptor.forClass(RGBLookup.class);
        model = new LevelsModel(levels);
        executor = mock(PreviewExecutor.class);
        model.setExecutor(executor);
        rgbPage = model.getSubModels()[0];
        rPage = model.getSubModels()[1];
    }

    @Test
    public void testDefaultSettingsProduceIdentity() {
        model.resetToDefaultSettings();

        var lookup = getCalculatedLookup();

        for (int i = 0; i < 256; i++) {
            checkRedMapping(lookup, i, i);
            checkGreenMapping(lookup, i, i);
            checkBlueMapping(lookup, i, i);
        }
    }

    @Test
    public void pageRGB_inputBlack100() {
        rgbPage.getInputDark().setValue(100);
        RGBLookup lookup = getCalculatedLookup();

        checkRGBMapping(lookup, 0, 0, 0, 0); // black remains black
        checkRGBMapping(lookup, 255, 255, 255, 255); // white remains white
        checkRGBMapping(lookup, 128, 46, 46, 46); // gray becomes darker
    }

    @Test
    public void pageR_inputBlack100() {
        rPage.getInputDark().setValue(100);
        RGBLookup lookup = getCalculatedLookup();

        checkRGBMapping(lookup, 0, 0, 0, 0); // black remains black
        checkRGBMapping(lookup, 255, 255, 255, 255); // white remains white
        checkRGBMapping(lookup, 128, 46, 128, 128); // only the red channel is affected
    }

    @Test
    public void pageRGB_inputWhite150() {
        rgbPage.getInputLight().setValue(150);
        RGBLookup lookup = getCalculatedLookup();

        checkRGBMapping(lookup, 0, 0, 0, 0); // black remains black
        checkRGBMapping(lookup, 255, 255, 255, 255); // white remains white
        checkRGBMapping(lookup, 128, 217, 217, 217); // gray becomes lighter (ps value: 218)
    }

    @Test
    public void pageR_inputWhite150() {
        rPage.getInputLight().setValue(150);
        RGBLookup lookup = getCalculatedLookup();

        checkRGBMapping(lookup, 0, 0, 0, 0); // black remains black
        checkRGBMapping(lookup, 255, 255, 255, 255); // white remains white
        checkRGBMapping(lookup, 128, 217, 128, 128); // only the red channel is affected
    }

    @Test
    public void pageRGB_outputBlack100() {
        rgbPage.getOutputDark().setValue(100);
        RGBLookup lookup = getCalculatedLookup();

        checkRGBMapping(lookup, 0, 100, 100, 100); // black becomes (100, 100, 100)
        checkRGBMapping(lookup, 255, 255, 255, 255); // white remains white
        checkRGBMapping(lookup, 128, 177, 177, 177); // gray becomes lighter (ps value: 178)
    }

    @Test
    public void pageR_outputBlack100() {
        rPage.getOutputDark().setValue(100);
        RGBLookup lookup = getCalculatedLookup();

        checkRGBMapping(lookup, 0, 100, 0, 0); // only the red channel is affected
        checkRGBMapping(lookup, 255, 255, 255, 255); // white remains white
        checkRGBMapping(lookup, 128, 177, 128, 128); // only the red channel is affected
    }

    @Test
    public void pageRGB_outputWhite150() {
        rgbPage.getOutputLight().setValue(150);
        RGBLookup lookup = getCalculatedLookup();

        checkRGBMapping(lookup, 0, 0, 0, 0); // black remains black
        checkRGBMapping(lookup, 255, 150, 150, 150); // white becomes (150, 150, 150)
        checkRGBMapping(lookup, 128, 75, 75, 75); // gray becomes darker
    }

    @Test
    public void pageR_outputWhite150() {
        rPage.getOutputLight().setValue(150);
        RGBLookup lookup = getCalculatedLookup();

        checkRGBMapping(lookup, 0, 0, 0, 0); // black remains black
        checkRGBMapping(lookup, 255, 150, 255, 255); // only the red channel is affected
        checkRGBMapping(lookup, 128, 75, 128, 128); // // only the red channel is affected
    }

    private RGBLookup getCalculatedLookup() {
        verify(executor, times(1)).runFilterPreview();
        verify(levels, times(1)).setRGBLookup(captor.capture());
        return captor.getValue();
    }

    private static void checkRedMapping(RGBLookup lookup, int input, int expected) {
        assertEquals(expected, lookup.mapRed(input));
    }

    private static void checkGreenMapping(RGBLookup lookup, int input, int expected) {
        assertEquals(expected, lookup.mapGreen(input));
    }

    private static void checkBlueMapping(RGBLookup lookup, int input, int expected) {
        assertEquals(expected, lookup.mapBlue(input));
    }

    private static void checkRGBMapping(RGBLookup lookup, int input,
                                        int expectedRed, int expectedGreen, int expectedBlue) {
        assertEquals(expectedRed, lookup.mapRed(input));
        assertEquals(expectedGreen, lookup.mapGreen(input));
        assertEquals(expectedBlue, lookup.mapBlue(input));
    }
}