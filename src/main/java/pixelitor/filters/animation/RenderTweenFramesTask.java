/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.Filter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.FilterState;
import pixelitor.gui.PixelitorWindow;
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
 * A SwingWorker for rendering the frames of a tween animation
 */
class RenderTweenFramesTask extends SwingWorker<Void, Void> {
    private final TweenAnimation animation;
    private final Drawable dr;
    private final ProgressMonitor progressMonitor;

    public RenderTweenFramesTask(TweenAnimation animation, Drawable dr) {
        this.animation = animation;
        this.dr = dr;
        progressMonitor = GUIUtils.createPercentageProgressMonitor("Rendering Frames");
        addPropertyChangeListener(this::onPropertyChange);
    }

    private void onPropertyChange(PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())) {
            int progress = (Integer) evt.getNewValue();

            onProgress(progress);
        }
    }

    private void onProgress(int progress) {
        progressMonitor.setProgress(progress);
        progressMonitor.setNote(format("Completed %d%%.%n", progress));
        if (progressMonitor.isCanceled()) {
            // Probably nothing bad happens if the current frame rendering is
            // interrupted, but to be on the safe side, let the current frame finish
            cancel(false);
        }
    }

    @Override
    protected Void doInBackground() {
        try {
            renderFrames();
        } catch (Exception e) {
            Messages.showExceptionOnEDT(e);
        }

        return null;
    }

    private void renderFrames() {
        assert calledOutsideEDT() : "on EDT";

        int numFrames = animation.getNumFrames();
        ParametrizedFilter filter = animation.getFilter();

        AnimationWriter animationWriter = animation.createAnimationWriter();

        dr.tweenCalculatingStarted();

        int numTotalFrames = numFrames;
        boolean pingPong = animation.isPingPong() && numFrames > 2;
        if (pingPong) {
            numTotalFrames = 2 * numFrames - 2;
        }

        boolean canceled = false;
        for (int frameNr = 0; frameNr < numTotalFrames; frameNr++) {
            if (isCancelled()) {
                canceled = true;
                break;
            }
            int percentProgress = (int) ((100.0 * frameNr) / numTotalFrames);
            setProgress(percentProgress);

            double time;
            if (frameNr < numFrames) { // ping: normal animation forwards
                time = ((double) frameNr) / numFrames;
            } else { // pong: animating backwards
                // Here the same frames are calculated again.
                // They could be cached in an array of soft references
                // or in the case of the file sequence output,
                // the output files could be copied.
                // However, "ping-pong" animations are probably uncommon.
                int effectiveFrame = 2 * (numFrames - 1) - frameNr;
                time = ((double) effectiveFrame) / numFrames;
            }

            try {
                // first render the frame...
                BufferedImage image = renderFrame(filter, time);

                // ...then write the file
                // TODO ideally while writing out the frame,
                // the rendering of the next frame should be
                // started on another thread
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

    private BufferedImage renderFrame(ParametrizedFilter filter, double time) {
        long runCountBefore = Filter.runCount;

        // all sorts of problems can happen
        // if filters run outside of EDT
        var busyCursorParent = PixelitorWindow.get();
        Runnable filterRunTask = () -> {
            FilterState intermediateState = animation.tween(time);
            filter.getParamSet().setState(intermediateState, true);
            filter.startOn(dr, TWEEN_PREVIEW, busyCursorParent);
        };
        GUIUtils.invokeAndWait(filterRunTask);

        long runCountAfter = Filter.runCount;
        assert runCountAfter == runCountBefore + 1;

        var comp = dr.getComp();
        comp.repaint();

        return comp.getCompositeImage();
    }

    private void finishOnEDT(AnimationWriter animationWriter, boolean canceled) {
        dr.tweenCalculatingEnded();
        if (canceled) {
            animationWriter.cancel();
        } else {
            animationWriter.finish();
        }
    }
}
