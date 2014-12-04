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
import pixelitor.filters.gui.ParametrizedAdjustPanel;
import pixelitor.utils.GUIUtils;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.OKCancelDialog;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

/**
 * Wizard for keyframe-based animations
 */
public class TweenWizard {
    private OKCancelDialog dialog = null;
    private TweenWizardState wizardState = TweenWizardState.SELECT_FILTER;
    private FilterWithParametrizedGUI filter;
    private ParamSetState initialState;
    private ParamSetState finalState;
    private int numFrames;
    private int millisBetweenFrames;

    /**
     * Show the wizard in a dialog
     */
    public void show(JFrame dialogParent) {
        ParametrizedAdjustPanel.setResetParams(false);
        dialog = new OKCancelDialog(
                wizardState.getPanel(TweenWizard.this),
                dialogParent,
                "Export Keyframe Animation",
                "Next", "Cancel") {

            @Override
            protected void dialogCanceled() {
                ParametrizedAdjustPanel.setResetParams(true);
                wizardState.onWizardCancelled(TweenWizard.this);
                super.dialogCanceled();
                dispose();
            }

            @Override
            protected void dialogAccepted() {
                // "next" was pressed
                wizardState.onMovingToTheNext(TweenWizard.this);
                TweenWizardState nextState = wizardState.getNext();
                if (nextState == null) {
                    dispose();
                    calculateAnimation();
                } else {
                    JPanel panel = nextState.getPanel(TweenWizard.this);
                    dialog.changeFormPanel(panel);
                    dialog.setHeaderMessage(nextState.getHelpMessage());
                    wizardState = nextState;
                }
            }
        };
        dialog.setHeaderMessage(wizardState.getHelpMessage());

        // it was packed already, but this is not correct because of the header message
        // and anyway we don't know the size of the filter dialogs in advance
        dialog.setSize(450, 300);

        GUIUtils.centerOnScreen(dialog);
        dialog.setVisible(true);
    }

    public void setFilter(FilterWithParametrizedGUI filter) {
        this.filter = filter;
    }

    public FilterWithParametrizedGUI getFilter() {
        return filter;
    }

    public void setInitialState(ParamSetState initialState) {
        this.initialState = initialState;
    }

    public void setFinalState(ParamSetState finalState) {
        this.finalState = finalState;
    }

    private void calculateAnimation() {
        System.out.println("Wizard::calculateAnimation: CALLED, thread = " + Thread.currentThread().getName());

        final ProgressMonitor progressMonitor = new ProgressMonitor(PixelitorWindow.getInstance(),
                "Progress", "Note", 1, 100);
        progressMonitor.setProgress(0);


        final RenderFramesTask task = new RenderFramesTask(filter, initialState, finalState, numFrames, millisBetweenFrames);
        task.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ("progress" == evt.getPropertyName()) {
                    int progress = (Integer) evt.getNewValue();

                    System.out.println("TweenWizard::propertyChange: progress = " + progress);

                    progressMonitor.setProgress(progress);
                    String message =
                            String.format("Completed %d%%.\n", progress);
                    progressMonitor.setNote(message);
                    if (progressMonitor.isCanceled() || task.isDone()) {
                        Toolkit.getDefaultToolkit().beep();
                        if (progressMonitor.isCanceled()) {
                            task.cancel(true);
                            System.out.println("TweenWizard::propertyChange: Task canceled");
                        } else {
                            System.out.println("TweenWizard::propertyChange: Task completed");
                        }
                    }

                }
            }
        });
        task.execute();
    }

    public void setNumFrames(int numFrames) {
        this.numFrames = numFrames;
    }

    public void setMillisBetweenFrames(int millisBetweenFrames) {
        this.millisBetweenFrames = millisBetweenFrames;
    }

    public void setNextButtonEnabled(boolean b) {
        dialog.setOKButtonEnabled(b);
    }
}

class RenderFramesTask extends SwingWorker<Void, Void> {
    private FilterWithParametrizedGUI filter;
    private ParamSetState initialState;
    private ParamSetState finalState;
    private int numFrames;
    private int millisBetweenFrames;

    public RenderFramesTask(FilterWithParametrizedGUI filter, ParamSetState initialState, ParamSetState finalState, int numFrames, int millisBetweenFrames) {
        this.filter = filter;
        this.initialState = initialState;
        this.finalState = finalState;
        this.numFrames = numFrames;
        this.millisBetweenFrames = millisBetweenFrames;
    }

    @Override
    protected Void doInBackground() throws Exception {
        System.out.println(String.format("RenderFramesTask::doInBackground: called on '%s'", Thread.currentThread().getName()));

        double[] time = new double[numFrames];
        double[] progress = new double[numFrames];

        File file = new File("output.gif");
        AnimationWriter animationWriter = new AnimGIFWriter(file, millisBetweenFrames);

        for (int i = 0; i < numFrames; i++) {
            int percentProgress = (int) ((100.0 * i) / numFrames);
            setProgress(percentProgress);

            time[i] = ((double) i) / numFrames;
            progress[i] = time[i]; // linear
            System.out.println(String.format("RenderFramesTask::doInBackground: " +
                    "time[%d] = %.2f, progress[%d] = %.2f, thread = '%s'", i, time[i], i, progress[i], Thread.currentThread().getName()));
            ParamSetState intermediateState = initialState.interpolate(finalState, time[i]);
            filter.getParamSet().setState(intermediateState);
            //filter.execute(ChangeReason.OP_PREVIEW);

            System.out.println("RenderFramesTask::doInBackground: before");
            Utils.executeFilterWithBusyCursor(filter, ChangeReason.OP_PREVIEW, PixelitorWindow.getInstance());
            System.out.println("RenderFramesTask::doInBackground: after");

            ImageComponent ic = ImageComponents.getActiveImageComponent();
//            ic.paintImmediately(ic.getBounds());
            ic.repaint();

            BufferedImage image = ImageComponents.getActiveCompositeImage();
            image = ImageUtils.copyImage(image); // TODO is this necessary?
            animationWriter.addFrame(image);
//            Utils.debugImage(image, "Step " + i);
        }
        setProgress(100);
        ImageComponents.getActiveComp().getActiveImageLayer().cancelPreviewing();

        animationWriter.finish();
        System.out.println("RenderFramesTask::doInBackground: file = " + file.getAbsolutePath() + (file.exists() ? " - exists" : " - does not exist!"));

        return null;
    }

    @Override
    protected void done() {
        System.out.println("RenderFramesTask::done: CALLED");
    }
}
