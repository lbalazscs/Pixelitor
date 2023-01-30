/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.gui.FilterState;
import pixelitor.gui.utils.Dialogs;

import java.awt.Component;
import java.io.File;

import static java.nio.file.Files.isWritable;
import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.threadInfo;

public class TweenAnimation {
    private ParametrizedFilter filter;
    private FilterState initialState;
    private FilterState finalState;
    private int numFrames;
    private int millisBetweenFrames;
    private TimeInterpolation interpolation;
    private TweenOutputType outputType;
    private File output; // file or directory
    private boolean pingPong;

    public ParametrizedFilter getFilter() {
        return filter;
    }

    public void setFilter(ParametrizedFilter filter) {
        this.filter = filter;
    }

    public void rememberFinalState() {
        finalState = filter.getParamSet().copyState(true);
    }

    public void rememberInitialState() {
        initialState = filter.getParamSet().copyState(true);
    }

    public void setInterpolation(TimeInterpolation interpolation) {
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

    public AnimationWriter createWriter() {
        return outputType.createAnimationWriter(output, millisBetweenFrames);
    }

    public FilterState tween(double time) {
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
        assert calledOnEDT() : threadInfo();

        if (outputType.needsDirectory()) {
            return checkOverwriteForDirectory(dialogParent);
        } else { // file
            return checkOverwriteForFile(dialogParent);
        }
    }

    private boolean checkOverwriteForDirectory(Component dialogParent) {
        if (output.list().length == 0) {
            return true; // empty directory: OK
        } else {
            return showFolderNotEmptyDialog(dialogParent);
        }
    }

    private boolean checkOverwriteForFile(Component dialogParent) {
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
            return true; // the output file doesn't exist: OK
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
