/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters.animation;

import pixelitor.ChangeReason;
import pixelitor.ImageComponent;
import pixelitor.ImageComponents;
import pixelitor.PixelitorWindow;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.ParamSetState;
import pixelitor.layers.ImageLayer;
import pixelitor.utils.Dialogs;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

class RenderFramesTask extends SwingWorker<Void, Void> {
    private TweenAnimation animation;

    public RenderFramesTask(TweenAnimation tweenAnimation) {
        this.animation = tweenAnimation;
    }

    @Override
    protected Void doInBackground() throws Exception {
        try {
            renderFrames();
        } catch (final Exception e) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Dialogs.showExceptionDialog(e);
                }
            });
        }

        return null;
    }

    private void renderFrames() {
        int numFrames = animation.getNumFrames();
        FilterWithParametrizedGUI filter = animation.getFilter();

        double[] time = new double[numFrames];

        AnimationWriter animationWriter = animation.createAnimationWriter();
        boolean canceled = false;

        ImageLayer activeImageLayer = ImageComponents.getActiveComp().getActiveImageLayer();
        activeImageLayer.tweenCalculatingStarted();

        for (int i = 0; i < numFrames; i++) {
            if (isCancelled()) {
                canceled = true;
                break;
            }
            int percentProgress = (int) ((100.0 * i) / numFrames);
            setProgress(percentProgress);

            time[i] = ((double) i) / numFrames;
            ParamSetState intermediateState = animation.tween(time[i]);
            filter.getParamSet().setState(intermediateState);

//            System.out.println("RenderFramesTask::renderFrames: CALLED, rendering frame " + i);

            Utils.executeFilterWithBusyCursor(filter, ChangeReason.OP_PREVIEW, PixelitorWindow.getInstance());

            ImageComponent ic = ImageComponents.getActiveImageComponent();
            ic.repaint();

            BufferedImage image = ImageComponents.getActiveCompositeImage();
            image = ImageUtils.copyImage(image); // TODO is this necessary?

            try {
                animationWriter.addFrame(image);
            } catch (IOException e) {
                canceled = true;
                Dialogs.showExceptionDialog(e);
                break;
            }
        }
        setProgress(100);
//        activeImageLayer.cancelPreviewing();
        activeImageLayer.tweenCalculatingEnded();

        if (canceled) {
            animationWriter.cancel();
        } else {
            animationWriter.finish();
        }
    }

    @Override
    protected void done() {
    }
}
