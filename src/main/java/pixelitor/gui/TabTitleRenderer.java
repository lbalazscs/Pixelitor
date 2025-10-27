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

package pixelitor.gui;

import javax.swing.*;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static javax.swing.BorderFactory.createEmptyBorder;

/**
 * A custom tab title renderer used in {@link TabsUI},
 * with a {@link CloseTabButton}
 */
class TabTitleRenderer extends JPanel {
    private final JLabel titleLabel;

    TabTitleRenderer(String title, TabViewContainer tab) {
        super(new GridBagLayout());
        setOpaque(false);
        titleLabel = new JLabel(title);
        titleLabel.setBorder(createEmptyBorder(0, 0, 0, 5));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        add(titleLabel, gbc);

        gbc.gridx++;
        gbc.weightx = 0;
        add(new CloseTabButton(tab), gbc);

        forwardMouseEventsToTab(tab);
    }

    private void forwardMouseEventsToTab(TabViewContainer tab) {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                tab.mousePressedOnTab(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                tab.mouseReleasedOnTab(e);
            }
        });
    }

    public void setTitle(String newTitle) {
        if (!titleLabel.getText().equals(newTitle)) {
            titleLabel.setText(newTitle);
        }
    }
}
