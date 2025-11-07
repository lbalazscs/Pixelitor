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

import pixelitor.gui.utils.DialogBuilder;

import javax.swing.*;
import java.awt.BorderLayout;
import java.util.function.Consumer;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.EAST;

/**
 * A panel that shows a "Configure..." button which opens a dialog when clicked.
 * It is the GUI of the params with dialog, such as
 * {@link CompositeParam}, {@link EffectsParam}, {@link StrokeParam}.
 */
public class DialogLauncherGUI extends JPanel implements ParamGUI {
    private static final String CONFIGURE_BUTTON_TEXT = "Configure...";
    
    private final JButton configureButton;
    private final ResetButton resetButton;

    public DialogLauncherGUI(Consumer<DialogBuilder> dialogConfigurator,
                             ResetButton resetButton) {
        super(new BorderLayout());
        this.resetButton = resetButton;

        configureButton = new JButton(CONFIGURE_BUTTON_TEXT);
        configureButton.addActionListener(e -> createAndShowDialog(dialogConfigurator));

        add(configureButton, CENTER);
        add(resetButton, EAST);
    }

    private void createAndShowDialog(Consumer<DialogBuilder> dialogConfigurator) {
        DialogBuilder builder = new DialogBuilder()
            .owner(SwingUtilities.getWindowAncestor(configureButton))
            .parentComponent(configureButton);
        dialogConfigurator.accept(builder);
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
        super.setEnabled(enabled);
    }
}
