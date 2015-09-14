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

package pixelitor.filters.levels.gui;

import pixelitor.filters.levels.OneChannelLevelsModel;
import pixelitor.utils.CardPanelWithCombo;
import pixelitor.utils.SliderSpinner;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;

import static java.awt.Color.GRAY;

public class OneChannelLevelsPanel extends CardPanelWithCombo.Card {
    //    private String channelName;
    private final Collection<SliderSpinner> sliders = new ArrayList<>();
    private final Box box = Box.createVerticalBox();



    private final SliderSpinner inputBlackSlider;
    private final SliderSpinner inputWhiteSlider;
    private final SliderSpinner outputBlackSlider;
    private final SliderSpinner outputWhiteSlider;

    public OneChannelLevelsPanel(OneChannelLevelsModel model) {
        super(model.getName());
        add(box);

        inputBlackSlider = new SliderSpinner(model.getInputBlackParam(), GRAY, model.getBackColor());
        inputWhiteSlider = new SliderSpinner(model.getInputWhiteParam(), model.getWhiteColor(), GRAY);
        outputBlackSlider = new SliderSpinner(model.getOutputBlackParam(), GRAY, model.getWhiteColor());
        outputWhiteSlider = new SliderSpinner(model.getOutputWhiteParam(), model.getBackColor(), GRAY);

        addSliderSpinner(inputBlackSlider);
        addSliderSpinner(inputWhiteSlider);
        addSliderSpinner(outputBlackSlider);
        addSliderSpinner(outputWhiteSlider);
    }

    private void addSliderSpinner(SliderSpinner sp) {
        box.add(sp);
        sliders.add(sp);
    }


}
