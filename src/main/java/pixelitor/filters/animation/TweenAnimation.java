/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.gui.ParamSet;
import pixelitor.gui.utils.Dialogs;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.Files.isWritable;
import static pixelitor.utils.Threads.callInfo;
import static pixelitor.utils.Threads.calledOnEDT;

/**
 * A tweening animation that interpolates filter parameters between two states.
 */
public class TweenAnimation {
    public static final int MAX_FRAMES = 100_000;

    private ParametrizedFilter filter;
    private FilterState initialState;
    private FilterState finalState;

    private int numFrames;
    private int millisBetweenFrames;
    private TimeInterpolation interpolation;
    private boolean pingPong;

    private TweenOutputType outputType;
    private File outputLocation; // file or directory

    public TweenAnimation() {
    }

    public ParametrizedFilter getFilter() {
        return filter;
    }

    public void setFilter(ParametrizedFilter filter) {
        if (this.filter != filter) {
            this.filter = filter;
            // setting a different filter discards the captured states
            initialState = null;
            finalState = null;
        }
    }

    public void captureInitialState() {
        initialState = filter.getParamSet().copyState(true);
    }

    /**
     * Restores the captured initial state into the filter's {@link ParamSet}
     * without triggering a filter execution, leaving non-animatable parameters untouched.
     */
    public void restoreInitialState() {
        if (initialState == null) {
            throw new IllegalStateException("Initial state has not been captured yet");
        }
        filter.getParamSet().setState(initialState, true);
    }

    public void captureFinalState() {
        finalState = filter.getParamSet().copyState(true);
    }

    public FilterState interpolateState(double time) {
        double progress = interpolation.timeToProgress(time);
        return initialState.interpolate(finalState, progress);
    }

    /**
     * Calculates the total number of frames rendered for the given base frame count
     * and ping-pong setting, avoiding integer overflow.
     */
    public static int totalFrameCount(int baseFrames, boolean pingPong) {
        if (pingPong && baseFrames > 2) {
            long total = 2L * baseFrames - 2;
            return (int) Math.min(total, Integer.MAX_VALUE);
        }
        return baseFrames;
    }

    /**
     * Calculates the normalized interpolation time (0.0 to 1.0) for a given frame index.
     */
    public double calcInterpolationTime(int frameIndex) {
        return calcInterpolationTime(frameIndex, numFrames);
    }

    /**
     * Calculates the normalized interpolation time (0.0 to 1.0) for a given frame index
     * and base frame count.
     */
    public static double calcInterpolationTime(int frameIndex, int baseFrameCount) {
        if (baseFrameCount <= 1) {
            return 0.0;
        }

        // time moves from 0.0 to 1.0 inclusive over baseFrameCount frames
        double timeStep = 1.0 / (baseFrameCount - 1);

        if (frameIndex < baseFrameCount) {
            // forward animation phase
            return frameIndex * timeStep;
        } else {
            // reverse animation phase (pong)
            int reverseIndex = 2 * (baseFrameCount - 1) - frameIndex;
            return reverseIndex * timeStep;
        }
    }

    public AnimationWriter createWriter() {
        return outputType.createWriter(outputLocation, millisBetweenFrames);
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

    public void setOutputLocation(File outputLocation) {
        this.outputLocation = outputLocation;
    }

    public void setOutputType(TweenOutputType outputType) {
        this.outputType = outputType;
    }

    public void setPingPong(boolean pingPong) {
        this.pingPong = pingPong;
    }

    public boolean isPingPong() {
        return pingPong;
    }

    /**
     * Warns the user about overwriting existing files and returns whether to proceed.
     */
    public boolean confirmOverwrite(Component dialogParent) {
        assert calledOnEDT() : callInfo();

        if (outputType.needsDirectory()) {
            return confirmOverwriteForDirectory(dialogParent);
        } else { // file
            return confirmOverwriteForFile(dialogParent);
        }
    }

    private boolean confirmOverwriteForDirectory(Component dialogParent) {
        assert outputLocation.isDirectory() : outputLocation.getAbsolutePath();

        Path dirPath = outputLocation.toPath();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            if (!stream.iterator().hasNext()) {
                return true; // empty directory: OK
            } else {
                return showFolderNotEmptyDialog(dialogParent);
            }
        } catch (IOException e) {
            Dialogs.showError(dialogParent, "Folder Read Error",
                "Could not read the contents of the folder:\n" + e.getMessage());
            return false;
        }
    }

    private boolean confirmOverwriteForFile(Component dialogParent) {
        if (outputLocation.exists()) {
            assert outputLocation.isFile() : outputLocation.getAbsolutePath();

            boolean overwrite = showFileExistsDialog(dialogParent);
            if (overwrite) {
                if (isWritable(outputLocation.toPath())) {
                    return true;
                } else {
                    Dialogs.showFileNotWritableError(outputLocation);
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
        return Dialogs.showYesNoWarning(dialogParent, "Folder Not Empty",
            String.format("<html>The folder <b>%s</b> is not empty. " +
                    "<br>Some files might be overwritten. Are you sure you want to continue?",
                outputLocation.getAbsolutePath()));
    }

    private boolean showFileExistsDialog(Component dialogParent) {
        return Dialogs.showYesNoWarning(dialogParent, "File Exists",
            outputLocation.getAbsolutePath() + " exists already. Overwrite?");
    }
}
