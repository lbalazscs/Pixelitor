/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GUIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.util.function.Consumer;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.EAST;

/**
 * A button with the "Configure..." text.
 * It is the GUI of the params with dialog, such as
 * {@link DialogParam}, {@link EffectsParam}, {@link StrokeParam}.
 */
public class ConfigureParamGUI extends JPanel implements ParamGUI {
    private final JButton configureButton;
    private final ResetButton resetButton;

    public ConfigureParamGUI(Consumer<DialogBuilder> dialogConfig,
                             ResetButton resetButton) {
        super(new BorderLayout());

        this.resetButton = resetButton;
        configureButton = new JButton("Configure...");
        add(configureButton, CENTER);
        add(resetButton, EAST);
        configureButton.addActionListener(e -> createAndShowDialog(dialogConfig));
    }

    private void createAndShowDialog(Consumer<DialogBuilder> dialogConfig) {
        DialogBuilder builder = new DialogBuilder()
            .owner(GUIUtils.getDialogAncestor(configureButton))
            .parentComponent(configureButton);
        dialogConfig.accept(builder);
        builder.show();
    }

    @Override
    public int getNumLayoutColumns() {
        return 2;
    }

    @Override
    public void updateGUI() {

    }

    @Override
    public void setToolTip(String tip) {
        configureButton.setToolTipText(tip);
    }

    @Override
    public void setEnabled(boolean enabled) {
        configureButton.setEnabled(enabled);
        resetButton.setEnabled(enabled);
    }

    @Override
    public boolean isEnabled() {
        return configureButton.isEnabled();
    }
}
