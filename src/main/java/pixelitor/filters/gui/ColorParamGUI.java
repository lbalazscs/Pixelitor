/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import static java.awt.FlowLayout.LEFT;

/**
 * The GUI for a {@link ColorParam}
 */
public class ColorParamGUI extends JPanel implements ParamGUI {
    private final ColorParam model;
//    private JButton button;

    private final ColorSwatch colorSwatch;

    private static final int BUTTON_SIZE = 30;
    private DefaultButton defaultButton;

    public ColorParamGUI(ColorParam model, FilterButtonModel action, boolean addDefaultButton) {
        this.model = model;
        setLayout(new FlowLayout(LEFT));

        Color color = model.getColor();
        colorSwatch = new ColorSwatch(color, BUTTON_SIZE);
        add(colorSwatch);

        GUIUtils.addColorDialogListener(colorSwatch, this::showColorDialog);

        Colors.setupFilterColorsPopupMenu(this, colorSwatch,
            model::getColor, this::updateColor);

        if (action != null) {
            add(action.createGUI());
        }

        if (addDefaultButton) {
            defaultButton = new DefaultButton(model);
            add(defaultButton);
        }
    }

    private void showColorDialog() {
//        Color color = JColorChooser.showDialog(this, "Select Color", model.getColor());

        Colors.selectColorWithDialog(this, model.getName(),
            model.getColor(), model.allowTransparency(), this::updateColor);
    }

    private void updateColor(Color color) {
        colorSwatch.setForeground(color);
        colorSwatch.paintImmediately(0, 0, BUTTON_SIZE, BUTTON_SIZE);

        model.setColor(color, true);
    }

    @Override
    public void updateGUI() {
//        button.setBackground(model.getColor());

        colorSwatch.setForeground(model.getColor());
        if (defaultButton != null) {
            defaultButton.updateIcon();
        }
    }

    @Override
    public void setToolTip(String tip) {
        colorSwatch.setToolTipText(tip);

//        button.setToolTipText(tip);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled); // so that isEnabled() works
        colorSwatch.setEnabled(enabled);
    }

    @Override
    public int getNumLayoutColumns() {
        return 2;
    }
}
