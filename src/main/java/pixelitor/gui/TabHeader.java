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
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A custom tab header component used in {@link TabsUI},
 * with a {@link CloseTabButton}.
 */
public class TabHeader extends JPanel {
    private final JLabel titleLabel;

    TabHeader(String title, TabViewContainer tab) {
        super(new BorderLayout(5, 0));
        setOpaque(false);
        titleLabel = new JLabel(title);

        add(titleLabel, BorderLayout.CENTER);
        add(new CloseTabButton(tab), BorderLayout.EAST);

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
