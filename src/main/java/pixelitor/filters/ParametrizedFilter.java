/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

/**
 * A filter that keeps its settings in a ParamSet object
 * The advantage is that subclasses don't need to create their own adjustment GUIs,
 * they only specify their ParamSet, and the GUI is built automatically
 */
public abstract class ParametrizedFilter extends FilterWithGUI {
    protected ParamSet paramSet;

    private BooleanParam showAffectedAreaParam = null;
    private final ShowOriginal addShowOriginal;
    private boolean hasAffectedAreaShapeParam;

    // not fully implemented - the idea is to show interactively
    // the area affected by a filter
    private Shape[] affectedAreaShapes;

    protected ParametrizedFilter(ShowOriginal addShowOriginal) {
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
    public FilterGUI createGUI(Drawable dr) {
        return new ParametrizedFilterGUI(this, dr, addShowOriginal);
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
    public FilterState[] getBuiltinPresets() {
        return paramSet.getBuiltinPresets();
    }

    @Override
    public boolean canHaveUserPresets() {
        return paramSet.canHaveUserPresets();
    }

    public void saveAsPreset(DialogMenuBar menu) {

    }

    @Override
    public UserPreset createUserPreset(String presetName) {
        return paramSet.toUserPreset(getName(), presetName);
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        paramSet.loadPreset(preset);
    }

    @Override
    public String paramsAsString() {
        return paramSet.toUserPreset(getName(), "Debug").toString();
    }
}
