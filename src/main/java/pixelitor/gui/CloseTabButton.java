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

package pixelitor.gui;

import pixelitor.gui.utils.Themes;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static javax.swing.BorderFactory.createEtchedBorder;

/**
 * The close button of the tabs in {@link TabsUI}
 */
class CloseTabButton extends JButton {
    private static final MouseListener buttonMouseListener = new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
            ((CloseTabButton) e.getComponent()).setBorderPainted(true);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            ((CloseTabButton) e.getComponent()).setBorderPainted(false);
        }
    };
    private static final int MARGIN = 5;
    private static final int SIZE = 17;

    CloseTabButton(TabViewContainer tab) {
        setPreferredSize(new Dimension(SIZE, SIZE));
        setToolTipText("Close this tab");
        setUI(new BasicButtonUI());
        setContentAreaFilled(false);
        setFocusable(false);
        setBorder(createEtchedBorder());
        setBorderPainted(false);
        addMouseListener(buttonMouseListener);
        setRolloverEnabled(true);
        addActionListener(e -> TabsUI.warnAndCloseTab(tab));
    }

    @Override
    public void updateUI() {
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(2));
        if (getModel().isRollover()) {
            g2.setColor(Color.RED);
        } else {
            if (Themes.getCurrent().isDark()) {
                g2.setColor(Themes.LIGHT_ICON_COLOR);
            } else {
                g2.setColor(Color.BLACK);
            }
        }
        g2.drawLine(MARGIN, MARGIN, SIZE - MARGIN - 1, SIZE - MARGIN - 1);
        g2.drawLine(SIZE - MARGIN - 1, MARGIN, MARGIN, SIZE - MARGIN - 1);
        g2.dispose();
    }
}
