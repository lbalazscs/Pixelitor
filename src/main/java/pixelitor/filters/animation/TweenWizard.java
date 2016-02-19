/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import pixelitor.automate.Wizard;
import pixelitor.automate.WizardPage;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.ParametrizedAdjustPanel;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.ValidatedForm;
import pixelitor.layers.ImageLayer;

import javax.swing.*;
import java.awt.Component;

import static pixelitor.filters.animation.TweenWizardPage.OUTPUT_SETTINGS;
import static pixelitor.filters.animation.TweenWizardPage.SELECT_FILTER;

/**
 * Wizard for tweening animations
 */
public class TweenWizard extends Wizard {
    private final TweenAnimation animation = new TweenAnimation();

    public TweenWizard(ImageLayer layer) {
        super(SELECT_FILTER, "Export Tweening Animation", "Render", 450, 380, layer);
    }

    @Override
    protected void finalCleanup() {
        ParametrizedAdjustPanel.setResetParams(true);
        FilterWithParametrizedGUI filter = animation.getFilter();
        if (filter != null) { // a filter was already selected
            filter.getParamSet().setFinalAnimationSettingMode(false);
        }
    }

    private void calculateAnimation() {
        ProgressMonitor progressMonitor = new ProgressMonitor(PixelitorWindow.getInstance(),
                "Rendering Frames", "", 1, 100);
        progressMonitor.setProgress(0);

        RenderFramesTask task = new RenderFramesTask(animation, layer);
        task.addPropertyChangeListener(evt -> {
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
        });
        task.execute();
    }

    public TweenAnimation getAnimation() {
        return animation;
    }

    @Override
    protected boolean mayMoveForwardIfNextPressed(WizardPage currentPage, Component dialogParent) {
        if(currentPage == OUTPUT_SETTINGS) {
            ValidatedForm settings = (ValidatedForm) currentPage.getPanel(this, layer);
            if (!settings.isDataValid()) {
                Dialogs.showErrorDialog(dialogParent, "Error", settings.getErrorMessage());
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean mayProceedAfterMovingForward(WizardPage wizardPage, Component dialogParent) {
        if(wizardPage == OUTPUT_SETTINGS) {
            if (!animation.checkOverwrite(dialogParent)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void executeFinalAction() {
        calculateAnimation();
    }
}

