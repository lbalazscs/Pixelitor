/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.levels.gui;

import pixelitor.filters.levels.OneChannelLevelsModel;
import pixelitor.gui.utils.CardPanelWithCombo;
import pixelitor.gui.utils.SliderSpinner;

import javax.swing.*;

import static java.awt.Color.GRAY;

/**
 * The panel corresponding to one channel in the Levels GUI
 */
public class OneChannelLevelsPanel extends CardPanelWithCombo.Card {
    public OneChannelLevelsPanel(OneChannelLevelsModel model) {
        super(model.getName());
        Box box = Box.createVerticalBox();
        add(box);

        SliderSpinner inputDarkSlider = new SliderSpinner(model.getInputDark(), GRAY, model.getDarkColor());
        SliderSpinner inputLightSlider = new SliderSpinner(model.getInputLight(), model.getLightColor(), GRAY);
        SliderSpinner outputDarkSlider = new SliderSpinner(model.getOutputDark(), GRAY, model.getLightColor());
        SliderSpinner outputLightSlider = new SliderSpinner(model.getOutputLight(), model.getDarkColor(), GRAY);

        box.add(inputDarkSlider);
        box.add(inputLightSlider);
        box.add(outputDarkSlider);
        box.add(outputLightSlider);
    }
}
