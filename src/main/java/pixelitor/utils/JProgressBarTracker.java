/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import javax.swing.*;
import java.awt.Cursor;
import java.awt.Window;

import static pixelitor.utils.Threads.callInfo;
import static pixelitor.utils.Threads.calledOnEDT;

/**
 * A {@link ThresholdProgressTracker} that tracks the progress
 * by using a given JProgressBar. Not to be confused
 * with the {@link StatusBarProgressTracker},
 * which uses a specific progress bar in the status bar.
 * <p>
 * The progress bar is only shown after the threshold is exceeded
 * and is automatically hidden when the operation completes.
 */
public class JProgressBarTracker extends ThresholdProgressTracker {
    private final ProgressPanel progressPanel;

    private final Window ownerWindow;
    private Cursor originalCursor;

    public JProgressBarTracker(ProgressPanel progressPanel) {
        super(null, 100);
        this.progressPanel = progressPanel;
        progressPanel.setProgress(0);

        ownerWindow = SwingUtilities.getWindowAncestor(progressPanel);
        if (ownerWindow == null) {
            throw new IllegalStateException(); // the panel must be added to a window first
        }
    }

    @Override
    protected void onProgressStart() {
        assert calledOnEDT() : callInfo();

        originalCursor = ownerWindow.getCursor();
        ownerWindow.setCursor(Cursors.BUSY);

        progressPanel.showProgressBar();
    }

    @Override
    protected void onProgressUpdate(int percentComplete) {
        assert calledOnEDT() : callInfo();

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
            ownerWindow.setCursor(originalCursor);
        } else {
            ownerWindow.setCursor(Cursors.DEFAULT);
        }
    }
}
