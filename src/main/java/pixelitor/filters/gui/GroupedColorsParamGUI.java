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
import java.awt.FlowLayout;

import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.utils.Texts.i18n;

/**
 * The GUI for a {@link GroupedColorsParam}.
 */
public class GroupedColorsParamGUI extends JPanel implements ParamGUI {
    private final GroupedColorsParam model;
    private final ResetButton resetButton;
    private final ColorSwatch[] swatches;
    private final JCheckBox linkedCB;

    public GroupedColorsParamGUI(GroupedColorsParam model) {
        super(new FlowLayout(FlowLayout.LEFT));
        setBorder(createTitledBorder(model.getName()));
        this.model = model;

        resetButton = new ResetButton(model);

        int numColors = model.getNumColors();
        swatches = new ColorSwatch[numColors];
        for (int i = 0; i < numColors; i++) {
            createAndAddSwatch(i);
        }

        if (model.isLinkable()) {
            add(new JLabel(i18n("linked") + ":"));
            linkedCB = GUIUtils.createLinkCheckBox(model);
            linkedCB.addItemListener(_ -> resetButton.updateState());
            add(linkedCB);
        } else {
            linkedCB = null;
        }

        add(resetButton);
    }

    private void createAndAddSwatch(int index) {
        add(new JLabel(model.getColorName(index) + ":"));
        swatches[index] = Colors.createFilterSwatch(this, model.getColor(index), model.getName(),
            () -> model.getColor(index), model.isTransparencyAllowed(),
            c -> model.setColor(index, c, true));
        add(swatches[index]);
    }

    @Override
    public void updateGUI() {
        int numColors = model.getNumColors();
        for (int i = 0; i < numColors; i++) {
            swatches[i].setForeground(model.getColor(i));
        }
        resetButton.updateState();
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
        if (linkedCB != null) {
            linkedCB.setEnabled(enabled);
        }
    }
}
