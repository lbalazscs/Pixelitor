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
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Wizard for keyframe-based animations
 */
public class TweenWizard {
    private OKCancelDialog dialog = null;
    private TweenWizardState wizardState = TweenWizardState.SELECT_FILTER;
    private FilterWithParametrizedGUI filter;

    ParamSetState initialState;
    ParamSetState finalState;
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
                if(nextState == null) {
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
        System.out.println("Wizard::calculateAnimation: CALLED");

        double[] time = new double[numFrames];
        double[] progress = new double[numFrames];

        File file = new File("output.gif");
        AnimationWriter animationWriter = new AnimGIFWriter(file, millisBetweenFrames);

        for (int i = 0; i < time.length; i++) {
            time[i] = ((double)i) / numFrames;
            progress[i] = time[i]; // linear
            System.out.println(String.format("KFWizard::calculateAnimation: " +
                    "time[%d] = %.2f, progress[%d] = %.2f, thread = '%s'", i, time[i], i, progress[i], Thread.currentThread().getName()));
            ParamSetState intermediateState = initialState.interpolate(finalState, time[i]);
            filter.getParamSet().setState(intermediateState);
            //filter.execute(ChangeReason.OP_PREVIEW);

            Utils.executeFilterWithBusyCursor(filter, ChangeReason.OP_PREVIEW, PixelitorWindow.getInstance());

            BufferedImage image = ImageComponents.getActiveCompositeImage();
            image = ImageUtils.copyImage(image); // TODO is this necessary?
            animationWriter.addFrame(image);
//            Utils.debugImage(image, "Step " + i);
            ImageComponents.getActiveComp().getActiveImageLayer().cancelPreviewing();
        }

        animationWriter.finish();
        System.out.println("KFWizard::calculateAnimation: file = " + file.getAbsolutePath() + (file.exists() ? " - exists" : " - does not exist!"));
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
