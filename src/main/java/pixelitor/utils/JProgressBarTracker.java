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

package pixelitor.utils;

import pixelitor.gui.utils.GUIUtils;

import javax.swing.*;
import java.awt.Container;

import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.threadInfo;

/**
 * A {@link ProgressTracker} that tracks the progress by using an
 * arbitrary {@link JProgressBar}.
 * <p>
 * Not to be confused with the {@link StatusBarProgressTracker}
 * which uses a specific progress bar in the status bar.
 */
public class JProgressBarTracker extends ThresholdProgressTracker {
    private final ProgressPanel progressPanel;

    private final Container topContainer;

    public JProgressBarTracker(ProgressPanel progressPanel) {
        super(100, null);
        this.progressPanel = progressPanel;
        progressPanel.setProgress(0);

        // can be a window, but if progressPanel is not
        // added yet to a window, the broadest available
        // GUI area will do
        topContainer = GUIUtils.getTopContainer(progressPanel);
    }

    @Override
    void startProgressTracking() {
        assert calledOnEDT() : threadInfo();

        topContainer.setCursor(Cursors.BUSY);

        progressPanel.showProgressBar();
    }

    @Override
    void updateProgressTracking(int percent) {
        assert calledOnEDT() : threadInfo();

        progressPanel.setProgress(percent);
        progressPanel.paintImmediately();
    }

    @Override
    void finishProgressTracking() {
        progressPanel.setProgress(100);
        progressPanel.hideProgressBar();

        topContainer.setCursor(Cursors.DEFAULT);
    }
}
