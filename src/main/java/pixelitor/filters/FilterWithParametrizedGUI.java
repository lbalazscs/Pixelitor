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

package pixelitor.filters;

import pixelitor.filters.gui.AdjustPanel;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.FilterParam;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.ParametrizedAdjustPanel;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.layers.ImageLayer;
import pixelitor.utils.ImageUtils;

import java.awt.Shape;
import java.awt.image.BufferedImage;

/**
 * A filter that keeps its settings in a ParamSet object
 * The advantage is that subclasses don't need to create their own adjustment GUIs,
 * they only specify their ParamSet, and the GUI is built automatically
 */
public abstract class FilterWithParametrizedGUI extends FilterWithGUI {
    protected ParamSet paramSet;

    private BooleanParam showAffectedAreaParam = null;
    private final ShowOriginal addShowOriginal;
    private boolean hasAffectedAreaShapeParam;
    private Shape[] affectedAreaShapes;

    protected FilterWithParametrizedGUI(ShowOriginal addShowOriginal) {
        this.addShowOriginal = addShowOriginal;
    }

    protected void showAffectedArea() {
        this.hasAffectedAreaShapeParam = true;
        showAffectedAreaParam = new BooleanParam("Show Affected Area", false);
    }

    @Override
    public void randomizeSettings() {
        paramSet.randomize();
    }

    @Override
    public AdjustPanel createAdjustPanel(ImageLayer layer) {
        return new ParametrizedAdjustPanel(this, layer, addShowOriginal);
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

    public void setParamSet(ParamSet paramSet) {
        this.paramSet = paramSet;
        // switch the affected area functionality here on-off
//        paramSet.addCommonActions(showAffectedAreaParam);

        paramSet.addCommonActions();
    }

    public ParamSet getParamSet() {
        return paramSet;
    }

    public void setAffectedAreaShapes(Shape[] affectedAreaShapes) {
        this.affectedAreaShapes = affectedAreaShapes;
    }

    /**
     * Some filters cannot be animated well, they can return true
     * here in order to be excluded from the list of animation filters
     */
    public boolean excludeFromAnimation() {
        return false;
    }

    public void addParamsToFront(FilterParam... params) {
        paramSet.addParamsToFront(params);
    }
}
