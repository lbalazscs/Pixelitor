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
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;

class RenderFramesTask extends SwingWorker<Void, Void> {
    private FilterWithParametrizedGUI filter;
    private ParamSetState initialState;
    private ParamSetState finalState;
    private int numFrames;
    private int millisBetweenFrames;
    private Interpolation interpolation;

    public RenderFramesTask(FilterWithParametrizedGUI filter, ParamSetState initialState, ParamSetState finalState, int numFrames, int millisBetweenFrames, Interpolation interpolation) {
        this.filter = filter;
        this.initialState = initialState;
        this.finalState = finalState;
        this.numFrames = numFrames;
        this.millisBetweenFrames = millisBetweenFrames;
        this.interpolation = interpolation;

        System.out.println("RenderFramesTask::RenderFramesTask: interpolation = " + interpolation);
    }

    @Override
    protected Void doInBackground() throws Exception {
        double[] time = new double[numFrames];
        double[] progress = new double[numFrames];

        File file = new File("output.gif");
        AnimationWriter animationWriter = new AnimGIFWriter(file, millisBetweenFrames);
        boolean canceled = false;

        for (int i = 0; i < numFrames; i++) {
            if (isCancelled()) {
                canceled = true;
                break;
            }
            int percentProgress = (int) ((100.0 * i) / numFrames);
            setProgress(percentProgress);

            time[i] = ((double) i) / numFrames;
            progress[i] = interpolation.time2progress(time[i]);
//            System.out.println(String.format("RenderFramesTask::doInBackground: " +
//                    "time[%d] = %.2f, progress[%d] = %.2f, thread = '%s'", i, time[i], i, progress[i], Thread.currentThread().getName()));
            ParamSetState intermediateState = initialState.interpolate(finalState, progress[i]);
            filter.getParamSet().setState(intermediateState);

            Utils.executeFilterWithBusyCursor(filter, ChangeReason.OP_PREVIEW, PixelitorWindow.getInstance());

            ImageComponent ic = ImageComponents.getActiveImageComponent();
            ic.repaint();

            BufferedImage image = ImageComponents.getActiveCompositeImage();
            image = ImageUtils.copyImage(image); // TODO is this necessary?
            animationWriter.addFrame(image);
        }
        setProgress(100);
        ImageComponents.getActiveComp().getActiveImageLayer().cancelPreviewing();

        if (canceled) {
            animationWriter.cancel();
        } else {
            animationWriter.finish();
        }

        return null;
    }

    @Override
    protected void done() {
    }
}
