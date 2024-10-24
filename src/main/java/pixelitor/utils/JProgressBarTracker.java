/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import java.awt.Container;
import java.awt.Cursor;

import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.threadInfo;

/**
 * A {@link ThresholdProgressTracker} that tracks the progress
 * by using an arbitrary JProgressBar. Not to be confused
 * with the {@link StatusBarProgressTracker},
 * which uses a specific progress bar in the status bar.
 * <p>
 * The progress bar is only shown after the threshold is exceeded
 * and is automatically hidden when the operation completes.
 */
public class JProgressBarTracker extends ThresholdProgressTracker {
    private final ProgressPanel progressPanel;

    private final Container topContainer;
    private Cursor originalCursor;

    public JProgressBarTracker(ProgressPanel progressPanel) {
        super(null, 100);
        this.progressPanel = progressPanel;
        progressPanel.setProgress(0);

        // Find the top-level container for cursor management.
        // It can be a window, but if progressPanel is not
        // added yet to a window, the broadest available
        // GUI area will do.
        topContainer = GUIUtils.getTopContainer(progressPanel);
    }

    @Override
    protected void onProgressStart() {
        assert calledOnEDT() : threadInfo();

        originalCursor = topContainer.getCursor();
        topContainer.setCursor(Cursors.BUSY);

        progressPanel.showProgressBar();
    }

    @Override
    protected void onProgressUpdate(int percentComplete) {
        assert calledOnEDT() : threadInfo();

        progressPanel.setProgress(percentComplete);
        progressPanel.paintImmediately();
    }

    @Override
    protected void onProgressComplete() {
        progressPanel.setProgress(100);
        progressPanel.hideProgressBar();

        restoreOriginalCursor();
    }

    private void restoreOriginalCursor() {
        if (originalCursor != null) {
            topContainer.setCursor(originalCursor);
        } else {
            topContainer.setCursor(Cursors.DEFAULT);
        }
    }
}
