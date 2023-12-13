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

package pixelitor.filters.gui;

import com.bric.swing.ColorSwatch;
import pixelitor.colors.Colors;
import pixelitor.gui.utils.GUIUtils;

import javax.swing.*;
import java.awt.Color;
import java.awt.FlowLayout;

import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.filters.gui.ColorParamGUI.BUTTON_SIZE;
import static pixelitor.utils.Texts.i18n;

public class GroupedColorsParamGUI extends JPanel implements ParamGUI {
    private final GroupedColorsParam model;
    private final ResetButton resetButton;
    private final ColorSwatch[] swatches;

    public GroupedColorsParamGUI(GroupedColorsParam model) {
        super(new FlowLayout(FlowLayout.LEFT));
        setBorder(createTitledBorder(model.getName()));

        this.model = model;

        int numColors = model.getNumColors();
        swatches = new ColorSwatch[numColors];

        for (int i = 0; i < numColors; i++) {
            int index = i; // final copy for the lambdas
            add(new JLabel(model.getName(i) + ":"));

            swatches[i] = new ColorSwatch(model.getColor(i), BUTTON_SIZE);

            GUIUtils.addClickAction(swatches[i], () -> showColorDialog(index));

            Colors.setupFilterColorsPopupMenu(this, swatches[i],
                () -> model.getColor(index),
                newColor -> updateColor(index, newColor));

            add(swatches[i]);
        }

        if (model.isLinkable()) {
            addLinkCheckBox();
        }

        resetButton = new ResetButton(model);
        add(resetButton);
    }

    private void showColorDialog(int index) {
        Colors.selectColorWithDialog(this, model.getName(),
            model.getColor(index), model.allowTransparency(),
            color -> updateColor(index, color));
    }

    private void updateColor(int index, Color color) {
        swatches[index].setForeground(color);
        swatches[index].paintImmediately(0, 0, BUTTON_SIZE, BUTTON_SIZE);

        model.setColor(color, index, true);
    }

    @Override
    public void updateGUI() {
        int numColors = model.getNumColors();
        for (int i = 0; i < numColors; i++) {
            swatches[i].setForeground(model.getColor(i));
        }
        resetButton.updateIcon();
    }

    private void addLinkCheckBox() {
        add(new JLabel(i18n("linked") + ":"));
        add(GUIUtils.createLinkCheckBox(model));
    }

    @Override
    public void setToolTip(String tip) {

    }

    @Override
    public int getNumLayoutColumns() {
        return 1;
    }
}
