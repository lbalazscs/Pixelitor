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

import pixelitor.PixelitorWindow;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.ParamSetState;
import pixelitor.filters.gui.ParametrizedAdjustPanel;
import pixelitor.utils.GUIUtils;
import pixelitor.utils.OKCancelDialog;

import javax.swing.*;
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
    private Interpolation interpolation;
    private TweenOutputType outputType;
    private File output; // file or directory

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
                if (nextState == null) { // dialog finished
                    dispose();
                    calculateAnimation();
                    ParametrizedAdjustPanel.setResetParams(true);
                } else {
                    JComponent panel = nextState.getPanel(TweenWizard.this);
                    dialog.changeForm(panel);
                    dialog.setHeaderMessage(nextState.getHeaderText());
                    wizardState = nextState;
                }
            }
        };
        dialog.setHeaderMessage(wizardState.getHeaderText());

        // it was packed already, but this is not correct because of the header message
        // and anyway we don't know the size of the filter dialogs in advance
        dialog.setSize(450, 380);

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
        final ProgressMonitor progressMonitor = new ProgressMonitor(PixelitorWindow.getInstance(),
                "Rendering Frames", "", 1, 100);
        progressMonitor.setProgress(0);


        final RenderFramesTask task = new RenderFramesTask(filter, initialState, finalState, numFrames, millisBetweenFrames, interpolation, outputType, output);
        task.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ("progress".equals(evt.getPropertyName())) {
                    int progress = (Integer) evt.getNewValue();

                    progressMonitor.setProgress(progress);
                    String message =
                            String.format("Completed %d%%.\n", progress);
                    progressMonitor.setNote(message);
                    if (progressMonitor.isCanceled()) {
                        // Probably nothing bad happens if the current frame rendering is
                        // interrupted, but to be on the safe side, let the current frame
                        // finish by passing false to cancel
                        task.cancel(false);
                    }
//                    if( task.isDone()) {
//                    }
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

    public void setInterpolation(Interpolation interpolation) {
        this.interpolation = interpolation;
    }

    public void setOutputType(TweenOutputType outputType) {
        this.outputType = outputType;
    }

    public void setOutput(File output) {
        this.output = output;
    }
}

