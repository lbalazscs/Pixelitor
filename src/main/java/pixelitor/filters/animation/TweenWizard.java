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
import pixelitor.filters.gui.ParametrizedAdjustPanel;
import pixelitor.utils.Dialogs;
import pixelitor.utils.GUIUtils;
import pixelitor.utils.OKCancelDialog;
import pixelitor.utils.ValidatedForm;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Wizard for tweening animations
 */
public class TweenWizard {
    private OKCancelDialog dialog = null;
    private TweenWizardPage wizardPage = TweenWizardPage.SELECT_FILTER;
    private TweenAnimation animation = new TweenAnimation();

    /**
     * Show the wizard in a dialog
     */
    public void start(JFrame dialogParent) {
        try {
            showDialog(dialogParent);
        } finally {
            ParametrizedAdjustPanel.setResetParams(true);
            FilterWithParametrizedGUI filter = animation.getFilter();
            if(filter != null) {
                filter.getParamSet().setFinalAnimationSettingMode(false);
            }
        }
    }

    private void showDialog(final JFrame dialogParent) {
        dialog = new OKCancelDialog(
                wizardPage.getPanel(TweenWizard.this),
                dialogParent,
                "Export Tweening Animation",
                "Next", "Cancel") {

            @Override
            protected void dialogCanceled() {
                wizardPage.onWizardCancelled(TweenWizard.this);
                super.dialogCanceled();
                dispose();
            }

            @Override
            protected void dialogAccepted() { // "next" was pressed
                // check if it may move forward
                if (wizardPage == TweenWizardPage.OUTPUT_SETTINGS) {
                    ValidatedForm settings = (ValidatedForm) wizardPage.getPanel(TweenWizard.this);
                    if (!settings.isDataValid()) {
                        Dialogs.showErrorDialog(this, "Error", settings.getErrorMessage());
                        return;
                    }
                }

                // move forward
                wizardPage.onMovingToTheNext(TweenWizard.this);

                // final overwrite check - must be called after onMovingToTheNext
                if (wizardPage == TweenWizardPage.OUTPUT_SETTINGS) {
                    if (!animation.checkOverwrite(this)) {
                        return;
                    }
                }

                TweenWizardPage nextPage = wizardPage.getNext();
                if (nextPage == null) { // dialog finished
                    dispose();
                    calculateAnimation();
                } else {
                    JComponent panel = nextPage.getPanel(TweenWizard.this);
                    dialog.changeForm(panel);
                    dialog.setHeaderMessage(nextPage.getHeaderText(TweenWizard.this));
                    wizardPage = nextPage;

                    if (wizardPage.getNext() == null) { // this is the last page
                        setOKButtonText("Render");
                    }
                }
            }
        };
        dialog.setHeaderMessage(wizardPage.getHeaderText(TweenWizard.this));

        // it was packed already, but this is not correct because of the header message
        // and anyway we don't know the size of the filter dialogs in advance
        dialog.setSize(450, 380);

        GUIUtils.centerOnScreen(dialog);
        dialog.setVisible(true);
    }

    private void calculateAnimation() {
        final ProgressMonitor progressMonitor = new ProgressMonitor(PixelitorWindow.getInstance(),
                "Rendering Frames", "", 1, 100);
        progressMonitor.setProgress(0);

        final RenderFramesTask task = new RenderFramesTask(animation);
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

    public TweenAnimation getAnimation() {
        return animation;
    }
}

