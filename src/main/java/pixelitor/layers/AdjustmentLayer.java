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

package pixelitor.layers;

import pixelitor.Composition;
import pixelitor.FilterContext;
import pixelitor.filters.Filter;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.io.TranslatedImage;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.util.concurrent.CompletableFuture;

/**
 * An adjustment layer is a filter that acts on the result of the layers bellow it.
 */
public class AdjustmentLayer extends Layer implements Filterable {
    @Serial
    private static final long serialVersionUID = 2L;

    private Filter filter;

    // A copy created at the beginning of editing,
    // to support Cancel and Show Original.
    private transient Filter lastFilter;
    private transient boolean showOriginal = false;

    public AdjustmentLayer(Composition comp, String name, Filter filter) {
        super(comp, name);
        this.filter = filter;
        isAdjustment = true;
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        isAdjustment = true;
        lastFilter = null;
        showOriginal = false;
    }

    @Override
    protected Layer createTypeSpecificDuplicate(String duplicateName) {
        // TODO the filter should be copied so that it can be adjusted independently
        return new AdjustmentLayer(comp, duplicateName, filter);
    }

    @Override
    public CompletableFuture<Void> resize(Dimension newSize) {
        // do nothing
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void crop(Rectangle2D cropRect, boolean deleteCropped, boolean allowGrowing) {
        // do nothing
    }

    @Override
    public BufferedImage applyOnImage(BufferedImage src) {
        return filter.transformImage(src);
    }

    @Override
    public void paintLayerOnGraphics(Graphics2D g, boolean firstVisibleLayer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BufferedImage asImage(boolean applyMask, boolean applyOpacity) {
        return null;
    }

    @Override
    public boolean hasIconThumbnail() {
        return false;
    }

    @Override
    public void updateIconImage() {
        // do nothing
    }

    @Override
    public void edit() {
        if (filter instanceof FilterWithGUI fwg) {
            runFilter(filter, FilterContext.PREVIEWING);
        }
    }

    @Override
    public boolean isRasterizable() {
        return false;
    }

    @Override
    public boolean canExportImage() {
        return false;
    }

    @Override
    public TranslatedImage getTranslatedImage() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void startPreviewing() {
        lastFilter = filter.copy();
    }

    @Override
    public void stopPreviewing() {
        // do nothing
    }

    @Override
    public void setShowOriginal(boolean b) {
        showOriginal = b;

        Filter tmp = filter;
        filter = lastFilter;
        lastFilter = tmp;

        comp.update();
    }

    @Override
    public void previewingFilterSettingsChanged(Filter filter, boolean first, Component busyCursorParent) {
        comp.update();
    }

    @Override
    public void onFilterDialogAccepted(String filterName) {
        if (showOriginal) {
            filter = lastFilter;
            comp.update();
        }
        lastFilter = null;
        showOriginal = false;
    }

    @Override
    public void onFilterDialogCanceled() {
        if (!showOriginal) {
            filter = lastFilter;
            comp.update();
        }
        lastFilter = null;
        showOriginal = false;
    }

    @Override
    public void filterWithoutDialogFinished(BufferedImage filteredImage, FilterContext context, String filterName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void runFilter(Filter filter, FilterContext context) {
        startFilter(filter, false);
    }

    @Override
    public String getTypeString() {
        return "Adjustment Layer";
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
               + "{" + "filter=" + (filter == null ? "null filter" : filter.getName())
               + ", super=" + super.toString() + '}';
    }
}
