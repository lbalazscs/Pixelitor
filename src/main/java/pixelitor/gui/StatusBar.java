/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.utils.GUIUtils;
import pixelitor.menus.view.ZoomControl;
import pixelitor.utils.ProgressHandler;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.EAST;
import static java.awt.FlowLayout.LEFT;
import static javax.swing.BorderFactory.createEtchedBorder;
import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.threadInfo;

/**
 * The status bar of the app.
 */
public class StatusBar extends JPanel {
    private final JLabel statusBarLabel;
    private final JPanel leftPanel;

    private static final StatusBar INSTANCE = new StatusBar();
    private static int numProgressBars = 0;

    private StatusBar() {
        super(new BorderLayout(0, 0));

        leftPanel = new JPanel(new FlowLayout(LEFT, 5, 0));
        statusBarLabel = new JLabel("Pixelitor started");
        leftPanel.add(statusBarLabel);

        add(leftPanel, CENTER);
        add(ZoomControl.get(), EAST);

        setBorder(createEtchedBorder());
    }

    public void setMessage(String msg) {
        assert msg != null;
        if (numProgressBars > 0) {
            // ignore any messages
        } else {
            statusBarLabel.setText(msg);
        }
    }

    public ProgressHandler startProgress(String msg, int max) {
        assert calledOnEDT() : threadInfo();
        assert msg != null;

        statusBarLabel.setText("");

        numProgressBars++;
        return new StatusBarProgressHandler(leftPanel, msg, max);
    }

    public static StatusBar get() {
        return INSTANCE;
    }

    public static boolean isShown() {
        return INSTANCE.getParent() != null;
    }

    static class StatusBarProgressHandler implements ProgressHandler {
        private final JLabel msgLabel;
        private final JPanel container;
        private final JProgressBar progressBar;
        private final boolean determinate;

        public StatusBarProgressHandler(JPanel container, String msg, int max) {
            assert calledOnEDT() : threadInfo();
            this.container = container;

            determinate = max > 0;

            if (determinate) {
                progressBar = new JProgressBar(0, max);
            } else {
                progressBar = new JProgressBar(0, 100);
                progressBar.setIndeterminate(true);
            }
            msgLabel = new JLabel(msg);

            container.add(msgLabel);
            container.add(progressBar);

            // call these instead of revalidate()/repaint()
            // because the EDT will be blocked
            container.validate(); // otherwise the panel width/height are 0
            GUIUtils.paintImmediately(container);
        }

        @Override
        public void updateProgress(int value) {
            assert calledOnEDT() : threadInfo();
            assert determinate;

            progressBar.setValue(value);
            container.paintImmediately(progressBar.getBounds());
        }

        @Override
        public void stopProgress() {
            assert calledOnEDT() : threadInfo();

            if (!determinate) {
                // probably this is not necessary to stop
                // the indeterminate animation, but can't be bad
                progressBar.setValue(100);
                progressBar.setIndeterminate(false);
            }

            container.remove(progressBar);
            container.remove(msgLabel);

            container.revalidate();
            container.repaint();
            numProgressBars--;
        }
    }
}
