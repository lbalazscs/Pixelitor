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

package pixelitor.filters.animation;

import pixelitor.Composition;
import pixelitor.filters.Filter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.FilterState;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.Drawable;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;

import static java.lang.String.format;
import static pixelitor.FilterContext.TWEEN_PREVIEW;
import static pixelitor.utils.Threads.calledOutsideEDT;

/**
 * A SwingWorker that renders frames for a tweening animation
 * by interpolating between two filter states.
 */
class RenderTweenFramesTask extends SwingWorker<Void, Void> {
    private final TweenAnimation animation;
    private final Drawable drawable;
    private final ProgressMonitor progressMonitor;

    public RenderTweenFramesTask(TweenAnimation animation, Drawable drawable) {
        this.animation = animation;
        this.drawable = drawable;
        progressMonitor = GUIUtils.createPercentageProgressMonitor("Rendering Animation Frames");
        addPropertyChangeListener(this::handleProgressUpdate);
    }

    private void handleProgressUpdate(PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())) {
            updateProgressMonitor((Integer) evt.getNewValue());
        }
    }

    private void updateProgressMonitor(int progressPercent) {
        progressMonitor.setProgress(progressPercent);
        progressMonitor.setNote(format("Completed %d%%.%n", progressPercent));

        if (progressMonitor.isCanceled()) {
            // Allow current frame to complete
            cancel(false);
        }
    }

    @Override
    protected Void doInBackground() {
        try {
            renderAllFrames();
        } catch (Exception e) {
            Messages.showExceptionOnEDT(e);
        }

        return null;
    }

    private void renderAllFrames() {
        assert calledOutsideEDT() : "on EDT";

        AnimationWriter animationWriter = animation.createWriter();
        ParametrizedFilter filter = animation.getFilter();
        int baseFrameCount = animation.getNumFrames();

        drawable.startTweening();

        int totalFrames = calculateTotalFrameCount(baseFrameCount);

        boolean canceled = false;
        for (int frameIndex = 0; frameIndex < totalFrames; frameIndex++) {
            if (isCancelled()) {
                canceled = true;
                break;
            }
            int percentProgress = (int) ((100.0 * frameIndex) / totalFrames);
            setProgress(percentProgress);

            double interpolationTime = calcInterpolationTime(frameIndex, baseFrameCount);

            try {
                BufferedImage image = renderSingleFrame(filter, interpolationTime);
                animationWriter.addFrame(image);
            } catch (Exception e) {
                canceled = true;
                Messages.showException(e);
                break;
            }
        }

        setProgress(100);

        boolean finalCanceled = canceled;
        SwingUtilities.invokeLater(() -> finishOnEDT(animationWriter, finalCanceled));
    }

    private int calculateTotalFrameCount(int baseFrameCount) {
        int numTotalFrames = baseFrameCount;
        boolean pingPong = animation.isPingPong() && baseFrameCount > 2;
        if (pingPong) {
            // For ping-pong, we need frames for forward and reverse,
            // minus the two duplicated end frames
            numTotalFrames = 2 * baseFrameCount - 2;
        }
        return numTotalFrames;
    }

    /**
     * Calculates the interpolation time for the current frame, handling both
     * forward and reverse (pong) animation phases.
     *
     * @param frameIndex     Current frame being rendered
     * @param baseFrameCount Number of frames in the forward sequence
     * @return Interpolation time value between 0.0 and 1.0
     */
    private static double calcInterpolationTime(int frameIndex, int baseFrameCount) {
        if (frameIndex < baseFrameCount) {
            // Forward animation phase
            return ((double) frameIndex) / baseFrameCount;
        } else {
            // Reverse animation phase (pong)

            // Here the same frames are calculated again.
            // They could be cached in an array of soft references
            // or in the case of the file sequence output,
            // the output files could be copied.
            // However, "ping-pong" animations are probably uncommon.
            int reverseIndex = 2 * (baseFrameCount - 1) - frameIndex;
            return ((double) reverseIndex) / baseFrameCount;
        }
    }

    private BufferedImage renderSingleFrame(ParametrizedFilter filter, double time) {
        long executionsBefore = Filter.executionCount;

        // Filters must run on EDT
        GUIUtils.invokeAndWait(() -> {
            FilterState intermediateState = animation.tween(time);
            filter.getParamSet().setState(intermediateState, true);
            drawable.startFilter(filter, TWEEN_PREVIEW);
        });

        assert Filter.executionCount == executionsBefore + 1;

        Composition comp = drawable.getComp();
        comp.repaint();
        return comp.getCompositeImage();
    }

    private void finishOnEDT(AnimationWriter animationWriter, boolean canceled) {
        drawable.endTweening();
        if (canceled) {
            animationWriter.cancel();
        } else {
            animationWriter.finish();
        }
    }
}
