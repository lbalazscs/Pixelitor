/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

package pixelitor.filters.gui;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.swing.*;
import java.awt.Color;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static pixelitor.filters.gui.GUIParam.Trigger.DO;
import static pixelitor.filters.gui.GUIParam.Trigger.DONT;

@RunWith(Parameterized.class)
public class GUIParamTest {
    private GUIParam param;

    public GUIParamTest(GUIParam param) {
        this.param = param;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> instancesToTest() {
        return Arrays.asList(new Object[][]{
                {new RangeParam("Param Name", 0, 10, 0)},
                {new RangeWithColorsParam(Color.CYAN, Color.RED, "Param Name", -100, 100, 0)},
                {new GroupedRangeParam("Param Name", 0, 100, 0)},
                {new ImagePositionParam("Param Name")},
                {new GradientParam("Param Name", Color.BLACK, Color.WHITE)},
                {new TextParam("Param Name", "default text")},
                {new ColorParam("Param Name", Color.BLACK, true, true)},
                {new BooleanParam("Param Name", true)},
                {new AngleParam("Param Name", 0)},
                {new ElevationAngleParam("Param Name", 0)},
                {new ActionParam("Param Name", e -> {
                }, "tooltip text")},
                {new ReseedNoiseActionParam("Param Name", e -> {
                })},
                {new IntChoiceParam("Param Name", new IntChoiceParam.Value[]{
                        new IntChoiceParam.Value("Better", 0),
                        new IntChoiceParam.Value("Faster", 1),
                })},
        });
    }

    @Test
    public void testGetName() {
        assertEquals("Param Name", param.getName());
    }

    @Test
    public void testCreateGUI() {
        JComponent gui = param.createGUI();
        assertNotNull(gui);
    }

    @Test
    public void testSetAdjustmentListener() {
        param.setAdjustmentListener(() -> {
        });
    }

    @Test
    public void testGetNrOfGridBagCols() {
        int cols = param.getNrOfGridBagCols();
        assertTrue(cols > 0 && cols < 3);
    }

    @Test
    public void testRandomize() {
        param.randomize();
    }

    @Test
    public void testSetTrigger() {
        param.setTrigger(DONT);
        param.setTrigger(DO);
    }

    @Test
    public void testConsiderImageSize() {
        param.considerImageSize(new Rectangle(0, 0, 1000, 600));
    }

    @Test
    public void testCopyAndSetState() {
        try {
            ParamState paramState = param.copyState();
            assertNotNull(paramState);
            param.setState(paramState);
        } catch (UnsupportedOperationException e) {
            // It is OK to throw this exception
        }
    }

    @Test
    public void testCanBeAnimated() {
        boolean b = param.canBeAnimated();
    }

    @Test
    public void testSetEnabledLogically() {
        param.setEnabledLogically(true);
        param.setEnabledLogically(false);
    }

    @Test
    public void testSetFinalAnimationSettingMode() {
        param.setFinalAnimationSettingMode(true);
        param.setFinalAnimationSettingMode(false);
    }
}