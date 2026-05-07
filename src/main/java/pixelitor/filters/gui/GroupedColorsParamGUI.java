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

/**
 * The GUI for a {@link GroupedColorsParam}.
 */
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
            createAndAddSwatch(i);
        }

        if (model.isLinkable()) {
            addLinkedCheckBox();
        }

        resetButton = new ResetButton(model);
        add(resetButton);
    }

    private void createAndAddSwatch(int index) {
        add(new JLabel(model.getName(index) + ":"));
        swatches[index] = new ColorSwatch(model.getColor(index), BUTTON_SIZE);

        GUIUtils.addClickAction(swatches[index], () -> showColorDialog(index));
        Colors.setupFilterColorPopupMenu(this, swatches[index],
            () -> model.getColor(index),
            newColor -> updateColor(index, newColor));

        add(swatches[index]);
    }

    /**
     * Shows the color chooser dialog for a specific color swatch.
     */
    private void showColorDialog(int index) {
        Colors.selectColorWithDialog(this, model.getName(),
            model.getColor(index), model.isTransparencyAllowed(),
            color -> updateColor(index, color));
    }

    /**
     * Updates a color in the model and GUI after a user change.
     */
    private void updateColor(int index, Color color) {
        swatches[index].setForeground(color);
        swatches[index].paintImmediately(0, 0, BUTTON_SIZE, BUTTON_SIZE);

        model.setColor(index, color, true);
    }

    @Override
    public void updateGUI() {
        int numColors = model.getNumColors();
        for (int i = 0; i < numColors; i++) {
            swatches[i].setForeground(model.getColor(i));
        }
        resetButton.updateState();
    }

    private void addLinkedCheckBox() {
        add(new JLabel(i18n("linked") + ":"));
        add(GUIUtils.createLinkCheckBox(model));
    }

    @Override
    public void setToolTip(String tip) {
        setToolTipText(tip);
    }

    @Override
    public int getNumLayoutColumns() {
        return 1;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        for (ColorSwatch swatch : swatches) {
            swatch.setEnabled(enabled);
        }
        resetButton.setEnabled(enabled);
    }
}
