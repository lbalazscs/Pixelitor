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

package pixelitor.filters.animation;

import pixelitor.Composition;
import pixelitor.filters.Filter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.ParamSetState;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.Drawable;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;

import static java.lang.String.format;
import static pixelitor.ChangeReason.TWEEN_PREVIEW;

/**
 * A SwingWorker for rendering the frames of a tween animation
 */
class RenderTweenFramesTask extends SwingWorker<Void, Void> {
    private final TweenAnimation animation;
    private final Drawable dr;

    public RenderTweenFramesTask(TweenAnimation tweenAnimation, Drawable dr) {
        this.animation = tweenAnimation;
        this.dr = dr;
    }

    void onPropertyChange(PropertyChangeEvent evt,
                          ProgressMonitor progressMonitor) {
        if ("progress".equals(evt.getPropertyName())) {
            int progress = (Integer) evt.getNewValue();

            onProgress(progress, progressMonitor);
        }
    }

    private void onProgress(int progress,
                            ProgressMonitor progressMonitor) {
        progressMonitor.setProgress(progress);
        progressMonitor.setNote(format("Completed %d%%.\n", progress));
        if (progressMonitor.isCanceled()) {
            // Probably nothing bad happens if the current frame rendering is
            // interrupted, but to be on the safe side, let the current frame
            // finish by passing false to cancel
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
        int numFrames = animation.getNumFrames();
        ParametrizedFilter filter = animation.getFilter();

        AnimationWriter animationWriter = animation.createAnimationWriter();
        boolean canceled = false;

        PixelitorWindow busyCursorParent = PixelitorWindow.getInstance();

        dr.tweenCalculatingStarted();

        int numTotalFrames = numFrames;
        boolean pingPong = animation.isPingPong() && numFrames > 2;
        if (pingPong) {
            numTotalFrames = 2 * numFrames - 2;
        }

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
                // TODO we are calculating the same frames again
                // they could be cached somewhere
                // perhaps in an array of soft references with the
                // calculated frames or in the case of file sequence
                // output one could simply make copies of the files.
                int effectiveFrame = 2 * (numFrames - 1) - frameNr;
                time = ((double) effectiveFrame) / numFrames;
            }

            try {
                // first render the frame...
                BufferedImage image = renderFrame(filter, time, busyCursorParent);

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
        SwingUtilities.invokeLater(() -> {
            dr.tweenCalculatingEnded();
            if (finalCanceled) {
                animationWriter.cancel();
            } else {
                animationWriter.finish();
            }
        });
    }

    private BufferedImage renderFrame(ParametrizedFilter filter,
                                      double time,
                                      PixelitorWindow busyCursorParent) {

        long runCountBefore = Filter.runCount;

        ParamSetState intermediateState = animation.tween(time);
        filter.getParamSet().setState(intermediateState);

        // all sorts of problems can happen
        // if filters run outside of EDT
        Runnable filterRunTask = () -> filter.run(dr, TWEEN_PREVIEW, busyCursorParent);
        GUIUtils.invokeAndWait(filterRunTask);

        long runCountAfter = Filter.runCount;
        assert runCountAfter == runCountBefore + 1;

        Composition comp = dr.getComp();
        comp.repaint();

        return comp.getCompositeImage();
    }

    @Override
    protected void done() {
    }
}
