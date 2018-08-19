/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.brushes;

import pixelitor.filters.gui.FilterParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.SliderSpinner;

import javax.swing.*;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;

public class BrushSettingsPanel extends JPanel {

    private final GridBagHelper gbh;

    public BrushSettingsPanel() {
        super(new GridBagLayout());
        gbh = new GridBagHelper(this);
    }

    public void addLabelWithControl(String text, JComponent c) {
        gbh.addLabelWithControl(text, c);
    }

    public void addLabelWithControlNoStretch(String text, JComponent c) {
        gbh.addLabelWithControlNoStretch(text, c);
    }

    public void addSlider(RangeParam param) {
        SliderSpinner slider = SliderSpinner.simpleWithDefaultButton(param);
        gbh.addLabelWithControl(param.getName() + ":", slider);
    }

    public void addParam(FilterParam param) {
        gbh.addLabelWithControlNoStretch(param.getName() + ":", param.createGUI());
    }

    public void addOnlyButton(String text, ActionListener action) {
        JButton button = new JButton(text);
        gbh.addOnlyControl(button);
        button.addActionListener(action);
    }
}
