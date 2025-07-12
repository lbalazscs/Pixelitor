/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.utils.Threads;

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
        assert Threads.calledOnEDT() : Threads.callInfo();

        this.animation = animation;
        this.drawable = drawable;
        progressMonitor = GUIUtils.createPercentageProgressMonitor("Rendering Animation Frames");
        addPropertyChangeListener(this::handleProgressUpdate);

        // call it while on the EDT
        drawable.startPreviewing();
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
            // allow the current frame to complete
            cancel(false);
        }
    }

    @Override
    protected Void doInBackground() {
        assert calledOutsideEDT() : "on EDT";

        AnimationWriter animationWriter = animation.createWriter();
        boolean hasError = false;

        try {
            ParametrizedFilter filter = animation.getFilter();
            int baseFrameCount = animation.getNumFrames();
            int totalFrames = calcTotalFrameCount(baseFrameCount);

            for (int frameIndex = 0; frameIndex < totalFrames; frameIndex++) {
                if (isCancelled()) {
                    break;
                }
                setProgress((int) ((100.0 * frameIndex) / totalFrames));

                double interpolationTime = calcInterpolationTime(frameIndex, baseFrameCount);
                BufferedImage image = renderSingleFrame(filter, interpolationTime);
                animationWriter.addFrame(image);
            }
        } catch (Exception e) {
            hasError = true;
            Messages.showExceptionOnEDT(e);
        } finally {
            setProgress(100);

            // treat errors as cancellation for cleanup purposes
            boolean cancelled = isCancelled() || hasError;
            SwingUtilities.invokeLater(() -> cleanupOnEDT(animationWriter, cancelled));
        }

        return null;
    }

    private int calcTotalFrameCount(int baseFrameCount) {
        // for a ping-pong animation, we render forward then reverse,
        // omitting the duplicated start and end frames for seamless looping
        if (animation.isPingPong() && baseFrameCount > 2) {
            return 2 * baseFrameCount - 2;
        }
        return baseFrameCount;
    }

    private static double calcInterpolationTime(int frameIndex, int baseFrameCount) {
        if (baseFrameCount <= 1) {
            return 0.0;
        }

        // time moves from 0.0 to 1.0 inclusive over baseFrameCount frames
        double timeStep = 1.0 / (baseFrameCount - 1);

        if (frameIndex < baseFrameCount) {
            // forward animation phase
            return frameIndex * timeStep;
        } else {
            // reverse animation phase (pong)

            // Here the same frames are calculated again.
            // They could be cached in an array of soft references
            // or in the case of the file sequence output,
            // the output files could be copied.
            // However, "ping-pong" animations are probably uncommon.
            int reverseIndex = 2 * (baseFrameCount - 1) - frameIndex;
            return reverseIndex * timeStep;
        }
    }

    private BufferedImage renderSingleFrame(ParametrizedFilter filter, double time) {
        long executionsBefore = Filter.executionCount;

        // filters must run on the EDT
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

    private void cleanupOnEDT(AnimationWriter animationWriter, boolean canceled) {
        drawable.stopPreviewing();
        if (canceled) {
            animationWriter.cancel();
        } else {
            animationWriter.finish();
        }
    }
}
