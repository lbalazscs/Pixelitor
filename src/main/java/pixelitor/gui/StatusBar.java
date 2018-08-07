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

import pixelitor.menus.view.ZoomControl;
import pixelitor.utils.ProgressHandler;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;

import static javax.swing.BorderFactory.createEtchedBorder;

/**
 * The status bar of the app.
 */
public class StatusBar extends JPanel {
    private final JLabel statusBarLabel;
    private final JPanel leftPanel;

    public static final StatusBar INSTANCE = new StatusBar();
    private static int numProgressBars = 0;

    private StatusBar() {
        super(new BorderLayout(0, 0));

        leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        statusBarLabel = new JLabel("Pixelitor started");
        leftPanel.add(statusBarLabel);

        add(leftPanel, BorderLayout.CENTER);
        add(ZoomControl.INSTANCE, BorderLayout.EAST);

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
        assert msg != null;
        assert EventQueue.isDispatchThread() : "not EDT thread";

        statusBarLabel.setText("");

        numProgressBars++;
        return new StatusBarProgressHandler(leftPanel, msg, max);
    }

    public boolean isShown() {
        return getParent() != null;
    }

    static class StatusBarProgressHandler implements ProgressHandler {
        private final JLabel msgLabel;
        private final JPanel leftPanel;
        private final JProgressBar progressBar;

        public StatusBarProgressHandler(JPanel leftPanel, String msg, int max) {
            this.leftPanel = leftPanel;
            progressBar = new JProgressBar(0, max);
            msgLabel = new JLabel(msg);

            // call these instead of revalidate/repaint
            // because we want to stay on the EDT
            leftPanel.validate();
            leftPanel.add(msgLabel);
            leftPanel.add(progressBar);

            leftPanel.paintImmediately(0, 0, leftPanel.getWidth(), leftPanel.getHeight());
        }

        @Override
        public void updateProgress(int value) {
            assert EventQueue.isDispatchThread() : "not EDT thread";

            progressBar.setValue(value);
            leftPanel.paintImmediately(progressBar.getBounds());
        }

        @Override
        public void stopProgress() {
            assert EventQueue.isDispatchThread() : "not EDT thread";

            leftPanel.remove(progressBar);
            leftPanel.remove(msgLabel);

            leftPanel.revalidate();
            leftPanel.repaint();
            StatusBar.numProgressBars--;
        }
    } 
}
