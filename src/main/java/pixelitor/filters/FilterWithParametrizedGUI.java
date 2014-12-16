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
package pixelitor.filters;

import pixelitor.filters.gui.AdjustPanel;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.ParametrizedAdjustPanel;
import pixelitor.utils.ImageUtils;

import java.awt.Shape;
import java.awt.image.BufferedImage;

/**
 * A filter that keeps its settings in a ParamSet object
 * The advantage is that subclasses don't need to create their own adjustment GUIs,
 * they only specify their ParamSet, and the GUI is built automatically
 */
public abstract class FilterWithParametrizedGUI extends FilterWithGUI {
    private ParamSet paramSet;

    private BooleanParam showOriginalParam = null;
    private BooleanParam showAffectedAreaParam = null;
    private ShowOriginalHelper showOriginalHelper;
    private boolean hasShowOriginal;
    private boolean hasAffectedAreaShapeParam;
    private Shape[] affectedAreaShapes;

    protected FilterWithParametrizedGUI(String name, boolean addShowOriginal, boolean hasAffectedAreaShape) {
        super(name);
        hasShowOriginal = addShowOriginal;
        this.hasAffectedAreaShapeParam = hasAffectedAreaShape;
        if (addShowOriginal) {
            showOriginalParam = BooleanParam.createParamForShowOriginal();
            showOriginalParam.setIgnoreFinalAnimationSettingMode(true);
            showOriginalHelper = new ShowOriginalHelper(getName());
        }
        if(hasAffectedAreaShapeParam) {
            showAffectedAreaParam = new BooleanParam("Show Affected Area", false);
        }
    }

    @Override
    public void randomizeSettings() {
        paramSet.randomize();
    }

    @Override
    public AdjustPanel getAdjustPanel() {
        return new ParametrizedAdjustPanel(this);
    }

    // from here show original functionality
    @Override
    public void endDialogSession() {
        if (hasShowOriginal) {
            BufferedImage img = showOriginalHelper.getLastTransformed();
            if (img != null) {
                // cannot be always flushed because it might be the active layer image as well
                // TODO keep track of it
                // img.flush();
                showOriginalHelper.setLastTransformed(null);
            }
            showOriginalHelper.setPreviousShowOriginal(false);

            showOriginalHelper.releaseCachedImage();
        }
    }

    @Override
    protected BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (hasShowOriginal) {
            boolean showOriginal = showOriginalParam.getValue();
            showOriginalHelper.setShowOriginal(showOriginal);
            if (showOriginal) {
                return src;
            }

            if (showOriginalHelper.showCached()) {
                dest = showOriginalHelper.getLastTransformed();
                assert dest != null;
                return dest;
            }
        }
        dest = doTransform(src, dest);

        if (hasShowOriginal) {
            showOriginalHelper.setLastTransformed(dest);
        }

        if(hasAffectedAreaShapeParam && showAffectedAreaParam.getValue()) {
            ImageUtils.paintAffectedAreaShapes(dest, affectedAreaShapes);
        }

        return dest;
    }

    public abstract BufferedImage doTransform(BufferedImage src, BufferedImage dest);

    public void setParamSet(ParamSet paramSet) {
        this.paramSet = paramSet;
        // switch the affected area functionality here on-off

//        paramSet.addCommonActions(showAffectedAreaParam, showOriginalParam);
        paramSet.addCommonActions(showOriginalParam);
    }

    public ParamSet getParamSet() {
        return paramSet;
    }

    public void setAffectedAreaShapes(Shape[] affectedAreaShapes) {
        this.affectedAreaShapes = affectedAreaShapes;
    }
}
