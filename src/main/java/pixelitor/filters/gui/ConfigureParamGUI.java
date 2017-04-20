/*
 * Copyright 2017 Laszlo Balazs-Csiki
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

import javax.swing.*;
import java.awt.BorderLayout;
import java.util.function.Function;

/**
 * A button with the "Configure..." text
 */
public class ConfigureParamGUI extends JPanel implements ParamGUI {
    private final JButton configureButton;
    private final DefaultButton defaultButton;

    public ConfigureParamGUI(Function<JDialog, JDialog> dialogFactory, DefaultButton defaultButton) {
        super(new BorderLayout());

        this.defaultButton = defaultButton;
        configureButton = new JButton("Configure...");
        add(configureButton, BorderLayout.CENTER);
        add(defaultButton, BorderLayout.EAST);
        configureButton.addActionListener(e -> createAndShowDialog(dialogFactory));
    }

    private void createAndShowDialog(Function<JDialog, JDialog> dialogFactory) {
        JDialog owner = (JDialog) SwingUtilities.getWindowAncestor(configureButton);
        JDialog dialog = dialogFactory.apply(owner);
        dialog.setVisible(true);
    }

    @Override
    public void updateGUI() {

    }

    @Override
    public void setToolTip(String tip) {

    }

    @Override
    public void setEnabled(boolean enabled) {
        configureButton.setEnabled(enabled);
        defaultButton.setEnabled(enabled);
    }

    @Override
    public boolean isEnabled() {
        return configureButton.isEnabled();
    }
}
