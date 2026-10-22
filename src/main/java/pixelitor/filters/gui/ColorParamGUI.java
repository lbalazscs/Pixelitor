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

import javax.swing.*;
import java.awt.FlowLayout;

import static java.awt.FlowLayout.LEFT;

/**
 * GUI component for interacting with a {@link ColorParam}.
 */
public class ColorParamGUI extends JPanel implements ParamGUI {
    private final ColorParam model;
    private final ColorSwatch colorSwatch;
    private ResetButton resetButton;

    public ColorParamGUI(ColorParam model, FilterButtonModel sideButtonModel, boolean addResetButton) {
        super(new FlowLayout(LEFT));

        this.model = model;

        colorSwatch = Colors.createFilterSwatch(this, model.getColor(), model.getName(),
            model::getColor, model.isTransparencyAllowed(),
            c -> model.setColor(c, true));
        add(colorSwatch);

        if (sideButtonModel != null) {
            add(sideButtonModel.createGUI());
        }

        if (addResetButton) {
            resetButton = new ResetButton(model);
            add(resetButton);
        }
    }

    @Override
    public void updateGUI() {
        colorSwatch.setForeground(model.getColor());
        if (resetButton != null) {
            resetButton.updateState();
        }
    }

    @Override
    public void setToolTip(String tip) {
        colorSwatch.setToolTipText(tip);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled); // so that isEnabled() works
        colorSwatch.setEnabled(enabled);
        if (resetButton != null) {
            resetButton.setEnabled(enabled);
        }
    }

    @Override
    public int getNumLayoutColumns() {
        return 2;
    }
}
