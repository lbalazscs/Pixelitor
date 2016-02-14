/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * The status bar of the app.
 */
public class StatusBar extends JPanel {
    private final JLabel statusBarLabel;
    private final JPanel leftPanel;
    private JProgressBar progressBar;
    private boolean inProgress = false;

    public static final StatusBar INSTANCE = new StatusBar();

    private StatusBar() {
        super(new BorderLayout(0, 0));

        leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        statusBarLabel = new JLabel("Pixelitor started");
        leftPanel.add(statusBarLabel);

        add(leftPanel, BorderLayout.CENTER);
        add(ZoomControl.INSTANCE, BorderLayout.EAST);

        setBorder(BorderFactory.createEtchedBorder());
    }

    public void setMessage(String msg) {
        if (inProgress) {
            // ignore any messages
        } else {
            statusBarLabel.setText(msg);
        }
    }

    public void startProgress(String msg, int max) {
        statusBarLabel.setText(msg);
        progressBar = new JProgressBar(0, max);
//        progressBar.setStringPainted(true);

        // call these instead of revalidate/repaint
        // because we want to stay on the EDT
        leftPanel.validate();
        progressBar.setSize(progressBar.getPreferredSize());
        progressBar.setLocation(statusBarLabel.getPreferredSize().width + 7, -1);
        leftPanel.add(progressBar);

        leftPanel.paintImmediately(0, 0, leftPanel.getWidth(), leftPanel.getHeight());

        inProgress = true;
    }

    public void updateProgress(int value) {
        assert inProgress;
        progressBar.setValue(value);
        leftPanel.paintImmediately(progressBar.getBounds());
    }

    public void stopProgress() {
        leftPanel.remove(progressBar);
        leftPanel.revalidate();
        repaint();
        progressBar = null;
        inProgress = false;
    }

    public boolean isShown() {
        return getParent() != null;
    }
}
