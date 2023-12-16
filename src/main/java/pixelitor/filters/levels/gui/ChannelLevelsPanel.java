/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.levels.ChannelLevelsModel;
import pixelitor.gui.utils.SliderSpinner;

import javax.swing.*;

import static java.awt.Color.GRAY;

/**
 * The panel corresponding to one channel in the Levels GUI
 */
public class ChannelLevelsPanel extends JPanel {
    private final String channelName;

    public ChannelLevelsPanel(ChannelLevelsModel model) {
        channelName = model.getChannelName();
        Box box = Box.createVerticalBox();
        add(box);

        var inputDarkSlider = new SliderSpinner(model.getInputDark(), GRAY, model.getDarkColor());
        var inputLightSlider = new SliderSpinner(model.getInputLight(), model.getLightColor(), GRAY);
        var outputDarkSlider = new SliderSpinner(model.getOutputDark(), GRAY, model.getLightColor());
        var outputLightSlider = new SliderSpinner(model.getOutputLight(), model.getDarkColor(), GRAY);

        box.add(inputDarkSlider);
        box.add(inputLightSlider);
        box.add(outputDarkSlider);
        box.add(outputLightSlider);
    }

    public String getChannelName() {
        return channelName;
    }
}
