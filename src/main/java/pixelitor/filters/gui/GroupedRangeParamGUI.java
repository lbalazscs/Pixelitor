/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.SliderSpinner;

import javax.swing.*;
import java.awt.GridBagLayout;

import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.gui.utils.SliderSpinner.LabelPosition.NONE;
import static pixelitor.utils.Texts.i18n;

/**
 * The GUI for a {@link GroupedRangeParam}.
 */
public class GroupedRangeParamGUI extends JPanel implements ParamGUI {
    private final int numChildren;
    private final GroupedRangeParam model;
    private final GridBagHelper gbh;
    private final SliderSpinner[] sliders;

    public GroupedRangeParamGUI(GroupedRangeParam model) {
        super(new GridBagLayout());
        gbh = new GridBagHelper(this);

        this.model = model;
        numChildren = model.getNumChildren();
        sliders = new SliderSpinner[numChildren];

        addSliderSpinners();

        if (model.isLinkable()) {
            addLinkCheckBox();
        }

        setBorder(createTitledBorder(model.getName()));
    }

    private void addSliderSpinners() {
        for (int i = 0; i < numChildren; i++) {
            RangeParam param = model.getRangeParam(i);
            // doesn't call param.createGUI() because we don't want another border
            sliders[i] = new SliderSpinner(param, NONE, true);
            sliders[i].setupTicks();

            String name = param.getName();
            sliders[i].setName(name);
            gbh.addLabelAndControl(name + ":", sliders[i], i);
        }
    }

    private void addLinkCheckBox() {
        JCheckBox linkedCB = GUIUtils.createLinkCheckBox(model);
        gbh.addLabelAndControl(i18n("linked") + ":", linkedCB, numChildren);
    }

    @Override
    public void updateGUI() {
        // can be empty
    }

    @Override
    public void setToolTip(String tip) {
        for (SliderSpinner slider : sliders) {
            slider.setToolTip(tip);
        }
    }

    @Override
    public int getNumLayoutColumns() {
        return 1;
    }
}
