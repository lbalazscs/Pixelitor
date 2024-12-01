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

import javax.imageio.ImageWriter;
import javax.imageio.event.IIOWriteProgressListener;

/**
 * Tracks the writing of a file with the help of a {@link ProgressTracker}.
 */
public class TrackingWriteProgressListener implements IIOWriteProgressListener {
    private final ProgressTracker tracker;
    private int trackedPercentage = 0;

    public TrackingWriteProgressListener(ProgressTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public void imageStarted(ImageWriter source, int imageIndex) {
    }

    @Override
    public void imageProgress(ImageWriter source, float percentageDone) {
        int currentPercentage = Math.round(percentageDone);
        if (currentPercentage > trackedPercentage) {
            tracker.unitsDone(currentPercentage - trackedPercentage);
            trackedPercentage = currentPercentage;
        }
    }

    @Override
    public void imageComplete(ImageWriter source) {
        tracker.finished();
    }

    @Override
    public void thumbnailStarted(ImageWriter source, int imageIndex, int thumbnailIndex) {
    }

    @Override
    public void thumbnailProgress(ImageWriter source, float percentageDone) {
    }

    @Override
    public void thumbnailComplete(ImageWriter source) {
    }

    @Override
    public void writeAborted(ImageWriter source) {
    }
}
