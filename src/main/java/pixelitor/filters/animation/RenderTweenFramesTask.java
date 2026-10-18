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
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import static java.lang.String.format;
import static pixelitor.filters.FilterContext.TWEEN_PREVIEW;
import static pixelitor.utils.Threads.callInfo;
import static pixelitor.utils.Threads.calledOutsideEDT;

/**
 * A SwingWorker that renders frames for a tweening animation
 * by interpolating between two filter states.
 */
class RenderTweenFramesTask extends SwingWorker<Void, Void> {
    private final TweenAnimation animation;
    private final Drawable drawable;
    private final ProgressMonitor progressMonitor;

    private AnimationWriter animationWriter;

    public RenderTweenFramesTask(TweenAnimation animation, Drawable drawable) {
        assert Threads.calledOnEDT() : Threads.callInfo();

        this.animation = animation;
        this.drawable = drawable;
        progressMonitor = GUIUtils.createPercentageProgressMonitor("Rendering Animation Frames");
        addPropertyChangeListener(this::handleProgressUpdate);

        // must run on the EDT, so call it here
        drawable.startPreviewing();
    }

    private void handleProgressUpdate(PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())) {
            updateProgressMonitor((Integer) evt.getNewValue());
        }
    }

    private void updateProgressMonitor(int progressPercent) {
        progressMonitor.setProgress(progressPercent);
        progressMonitor.setNote(format("Completed %d%%", progressPercent));

        if (progressMonitor.isCanceled()) {
            // allow the current frame to complete
            cancel(false);
        }
    }

    @Override
    protected Void doInBackground() throws Exception {
        assert calledOutsideEDT() : callInfo();

        try {
            animationWriter = animation.createWriter();
            ParametrizedFilter filter = animation.getFilter();
            int totalFrames = TweenAnimation.totalFrameCount(
                animation.getNumFrames(), animation.isPingPong());

            for (int frameIndex = 0; frameIndex < totalFrames; frameIndex++) {
                if (isCancelled()) {
                    break;
                }
                setProgress((int) ((100.0 * frameIndex) / totalFrames));

                double normalizedTime = animation.calcInterpolationTime(frameIndex);
                BufferedImage image = renderSingleFrame(filter, normalizedTime);
                animationWriter.addFrame(image);
            }
        } finally {
            setProgress(100);
        }
        return null;
    }

    // invoked exactly once on the EDT
    @Override
    protected void done() {
        drawable.stopPreviewing();

        try {
            get(); // rethrows anything that escaped doInBackground
            if (animationWriter != null) {
                animationWriter.finish();
            }
        } catch (CancellationException e) {
            cancelWriter();
        } catch (ExecutionException e) {
            cancelWriter();
            Messages.showException(e.getCause());
        } catch (Exception e) { // InterruptedException or a failure in finish()
            Messages.showException(e);
        }
    }

    private void cancelWriter() {
        if (animationWriter != null) {
            try {
                animationWriter.cancel();
            } catch (Exception e) {
                Messages.showException(e);
            }
        }
    }

    private BufferedImage renderSingleFrame(ParametrizedFilter filter, double time) {
        long executionsBefore = Filter.executionCount;

        // filters must run on the EDT
        GUIUtils.invokeAndWait(() -> {
            FilterState intermediateState = animation.interpolateState(time);
            filter.getParamSet().setState(intermediateState, true);
            drawable.startFilter(filter, TWEEN_PREVIEW);
        });

        assert Filter.executionCount == executionsBefore + 1;

        Composition comp = drawable.getComp();
        comp.repaint();
        return comp.getCompositeImage();
    }
}
