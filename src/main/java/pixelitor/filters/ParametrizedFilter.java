/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters;

import pixelitor.filters.gui.*;
import pixelitor.layers.Drawable;
import pixelitor.utils.ImageUtils;

import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * A filter that keeps its settings in a ParamSet object
 * The advantage is that subclasses don't need to create their own adjustment GUIs,
 * they only specify their ParamSet, and the GUI is built automatically
 */
public abstract class ParametrizedFilter extends FilterWithGUI {
    @Serial
    private static final long serialVersionUID = 3796358314893014182L;

    protected transient ParamSet paramSet;

    private transient BooleanParam showAffectedAreaParam = null;
    private final transient boolean addShowOriginal;
    private transient boolean hasAffectedAreaShapeParam;

    // not fully implemented - the idea is to show interactively
    // the area affected by a filter
    private transient Shape[] affectedAreaShapes;

    protected ParametrizedFilter(boolean addShowOriginal) {
        this.addShowOriginal = addShowOriginal;
    }

    protected void showAffectedArea() {
        hasAffectedAreaShapeParam = true;
        showAffectedAreaParam = new BooleanParam("Show Affected Area", false);
    }

    @Override
    public void randomizeSettings() {
        paramSet.randomize();
    }

    @Override
    public FilterGUI createGUI(Drawable dr, boolean reset) {
        return new ParametrizedFilterGUI(this, dr, addShowOriginal, reset);
    }

    @Override
    protected BufferedImage transform(BufferedImage src, BufferedImage dest) {
        dest = doTransform(src, dest);

        if (hasAffectedAreaShapeParam && showAffectedAreaParam.isChecked()) {
            ImageUtils.paintAffectedAreaShapes(dest, affectedAreaShapes);
        }

        return dest;
    }

    public abstract BufferedImage doTransform(BufferedImage src, BufferedImage dest);

    private void setParamSet(ParamSet paramSet) {
        this.paramSet = paramSet;
        // switch the affected area functionality here on-off
//        paramSet.addCommonActions(showAffectedAreaParam);

        paramSet.addCommonActions();
    }

    public ParamSet setParams(FilterParam param) {
        ParamSet ps = new ParamSet(param);
        setParamSet(ps);
        return ps;
    }

    public ParamSet setParams(FilterParam... params) {
        ParamSet ps = new ParamSet(params);
        setParamSet(ps);
        return ps;
    }

    public ParamSet getParamSet() {
        return paramSet;
    }

    public void setAffectedAreaShapes(Shape[] affectedAreaShapes) {
        this.affectedAreaShapes = affectedAreaShapes;
    }

    /**
     * Some filters can't be animated well, they can return true
     * here in order to be excluded from the list of animation filters
     */
    public boolean excludedFromAnimation() {
        return false;
    }

    public void addParams(FilterParam... params) {
        paramSet.addParams(params);
    }

    public void addParamsToFront(FilterParam... params) {
        paramSet.addParamsToFront(params);
    }

    public void insertParamAtIndex(FilterParam param, int index) {
        paramSet.insertParamAtIndex(param, index);
    }

    @Override
    public boolean hasBuiltinPresets() {
        return paramSet.hasBuiltinPresets();
    }

    @Override
    public Preset[] getBuiltinPresets() {
        return paramSet.getBuiltinPresets();
    }

    @Override
    public boolean canHaveUserPresets() {
        return paramSet.canHaveUserPresets();
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        paramSet.saveStateTo(preset);
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        paramSet.loadUserPreset(preset);
    }

    @Override
    public void loadFilterState(FilterState filterState, boolean reset) {
        paramSet.applyState(filterState, reset);
    }

    @Override
    public String paramsAsString() {
        UserPreset preset = new UserPreset(getName(), "Debug");
        paramSet.saveStateTo(preset);
        return preset.toString();
    }

    public void set(String paramName, String value) {
        paramSet.set(paramName, value);
    }
}
