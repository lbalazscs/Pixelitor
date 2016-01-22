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

import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.ParamSetState;
import pixelitor.gui.utils.Dialogs;

import java.awt.Component;
import java.awt.EventQueue;
import java.io.File;

public class TweenAnimation {
    private FilterWithParametrizedGUI filter;
    private ParamSetState initialState;
    private ParamSetState finalState;
    private int numFrames;
    private int millisBetweenFrames;
    private Interpolation interpolation;
    private TweenOutputType outputType;
    private File output; // file or directory
    private boolean pingPong;

    public FilterWithParametrizedGUI getFilter() {
        return filter;
    }

    public void setFilter(FilterWithParametrizedGUI filter) {
        this.filter = filter;
    }

    public void copyFinalStateFromCurrent() {
        this.finalState = filter.getParamSet().copyState();
    }

    public void copyInitialStateFromCurrent() {
        this.initialState = filter.getParamSet().copyState();
    }

    public Interpolation getInterpolation() {
        return interpolation;
    }

    public void setInterpolation(Interpolation interpolation) {
        this.interpolation = interpolation;
    }

    public void setMillisBetweenFrames(int millisBetweenFrames) {
        this.millisBetweenFrames = millisBetweenFrames;
    }

    public int getNumFrames() {
        return numFrames;
    }

    public void setNumFrames(int numFrames) {
        this.numFrames = numFrames;
    }

    public void setOutput(File output) {
        this.output = output;
    }

    public void setOutputType(TweenOutputType outputType) {
        this.outputType = outputType;
    }

    public AnimationWriter createAnimationWriter() {
        return outputType.createAnimationWriter(
                output, millisBetweenFrames);
    }

    public ParamSetState tween(double time) {
        double progress = interpolation.time2progress(time);
        return initialState.interpolate(finalState, progress);
    }

    /**
     * A final warning if something might get overwritten because
     * the selected file exists or the selected directory is not empty
     *
     * @return true if the rendering can proceed
     */
    public boolean checkOverwrite(Component dialogParent) {
        assert EventQueue.isDispatchThread();
        if (outputType.needsDirectory()) {
            if (output.list().length == 0) {
                return true;
            } else {
                return Dialogs.showYesNoWarningDialog(dialogParent, "Folder not empty",
                        "<html>The folder " + output.getAbsolutePath() + " is not empty. " +
                                "<br>Some files might get replaced. Are sure you want to continue?");
            }
        } else { // file
            if (output.exists()) {
                return Dialogs.showYesNoWarningDialog(dialogParent, "File exists",
                        output.getAbsolutePath() + " exists already. Overwrite?");
            } else {
                return true;
            }
        }
    }

    public void setPingPong(boolean pingPong) {
        this.pingPong = pingPong;
    }

    public boolean isPingPong() {
        return pingPong;
    }
}
