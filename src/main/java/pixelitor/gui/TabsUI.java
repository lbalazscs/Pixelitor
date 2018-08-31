/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.utils.Keys;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BorderFactory.createEtchedBorder;

/**
 * A user interface ({@link ImageAreaUI} implementation)
 * where the edited images are in tabs
 */
public class TabsUI extends JTabbedPane implements ImageAreaUI {

    private boolean userInitiated = true;

    public TabsUI() {
        addChangeListener(e -> tabsChanged());

        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(Keys.CTRL_TAB, "navigateNext");
        inputMap.put(Keys.CTRL_SHIFT_TAB, "navigatePrevious");
    }

    private void tabsChanged() {
        if (!userInitiated) {
            return;
        }
        int selectedIndex = getSelectedIndex();
        if (selectedIndex != -1) { // it is -1 if all tabs have been closed
            ImageTab tab = (ImageTab) getComponentAt(selectedIndex);
            tab.onActivation();
        }
    }

    @Override
    public void activateIC(ImageComponent ic) {
        ImageTab tab = (ImageTab) ic.getImageWindow();
        setSelectedIndex(indexOfComponent(tab));
    }

    @Override
    public void addNewIC(ImageComponent ic) {
        ImageTab tab = new ImageTab(ic, this);
        ic.setImageWindow(tab);

        int myIndex = getTabCount();

        try {
            userInitiated = false;
            addTab(ic.getName(), tab);
        } finally {
            userInitiated = true;
        }

        setTabComponentAt(myIndex, new TabTitleRenderer(ic.getName(), this, tab));
        setSelectedIndex(myIndex);
        tab.onActivation();
    }

    private static void warnAndCloseTab(ImageTab tab) {
        if (!RandomGUITest.isRunning()) {
            // this will call closeTab
            ImageComponents.warnAndClose(tab.getIC());
        }
    }

    public void closeTab(ImageTab tab) {
        remove(indexOfComponent(tab));
        ImageComponent ic = tab.getIC();
        ImageComponents.imageClosed(ic);
    }

    public void selectTab(ImageTab tab) {
        // expect that this call is not needed
        // since new tabs are already selected
        assert getSelectedIndex() == indexOfComponent(tab);
    }

    static class TabTitleRenderer extends JPanel {
        private final JLabel titleLabel;

        public TabTitleRenderer(String title, TabsUI pane, ImageTab tab) {
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
            add(new CloseTabButton(pane, tab), gbc);
        }

        public void setTitle(String newTitle) {
            if (!titleLabel.getText().equals(newTitle)) {
                titleLabel.setText(newTitle);
            }
        }
    }

    static class CloseTabButton extends JButton {
        private static final MouseListener buttonMouseListener = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                CloseTabButton button = (CloseTabButton) e.getComponent();
                button.setBorderPainted(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                CloseTabButton button = (CloseTabButton) e.getComponent();
                button.setBorderPainted(false);
            }
        };
        public static final int MARGIN = 5;
        public static final int SIZE = 17;

        public CloseTabButton(TabsUI pane, ImageTab tab) {
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
                g2.setColor(Color.BLACK);
            }
            g2.drawLine(MARGIN, MARGIN, SIZE - MARGIN - 1, SIZE - MARGIN - 1);
            g2.drawLine(SIZE - MARGIN - 1, MARGIN, MARGIN, SIZE - MARGIN - 1);
            g2.dispose();
        }
    }
}
