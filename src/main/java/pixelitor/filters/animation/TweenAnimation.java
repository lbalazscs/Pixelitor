/*
 * Copyright 2014 Laszlo Balazs-Csiki
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

import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.ParamSetState;

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

    public File getOutput() {
        return output;
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
}
