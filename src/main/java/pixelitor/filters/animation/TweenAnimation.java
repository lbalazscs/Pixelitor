/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.CompositeState;
import pixelitor.gui.utils.Dialogs;

import java.awt.Component;
import java.awt.EventQueue;
import java.io.File;

import static java.nio.file.Files.isWritable;

public class TweenAnimation {
    private ParametrizedFilter filter;
    private CompositeState initialState;
    private CompositeState finalState;
    private int numFrames;
    private int millisBetweenFrames;
    private Interpolation interpolation;
    private TweenOutputType outputType;
    private File output; // file or directory
    private boolean pingPong;

    public ParametrizedFilter getFilter() {
        return filter;
    }

    public void setFilter(ParametrizedFilter filter) {
        this.filter = filter;
    }

    public void copyFinalStateFromCurrent() {
        finalState = filter.getParamSet().copyState();
    }

    public void copyInitialStateFromCurrent() {
        initialState = filter.getParamSet().copyState();
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

    public CompositeState tween(double time) {
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
        assert EventQueue.isDispatchThread() : "not EDT thread";

        if (outputType.needsDirectory()) {
            if (output.list().length == 0) {
                return true;
            } else {
                return showFolderNotEmptyDialog(dialogParent);
            }
        } else { // file
            if (output.exists()) {
                boolean overwrite = showFileExistsDialog(dialogParent);
                if (overwrite) {
                    if (isWritable(output.toPath())) {
                        return true;
                    } else {
                        Dialogs.showFileNotWritableDialog(output);
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }
    }

    private boolean showFolderNotEmptyDialog(Component dialogParent) {
        return Dialogs.showYesNoWarningDialog(dialogParent, "Folder not empty",
                String.format("<html>The folder <b>%s</b> is not empty. " +
                                "<br>Some files might be overwritten. Are sure you want to continue?",
                        output.getAbsolutePath()));
    }

    private boolean showFileExistsDialog(Component dialogParent) {
        return Dialogs.showYesNoWarningDialog(dialogParent, "File exists",
                output.getAbsolutePath() + " exists already. Overwrite?");
    }

    public void setPingPong(boolean pingPong) {
        this.pingPong = pingPong;
    }

    public boolean isPingPong() {
        return pingPong;
    }
}
