/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.CopyType;
import pixelitor.FilterContext;
import pixelitor.filters.Filter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.history.FilterChangedEdit;
import pixelitor.history.History;
import pixelitor.io.TranslatedImage;
import pixelitor.utils.debug.DebugNode;

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

    protected Filter filter;

    // A copy created at the beginning of editing,
    // to support Cancel and Show Original.
    private transient Filter lastFilter;
    private transient boolean showOriginal = false;

    private transient boolean tentative = false;

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
        tentative = false;
    }

    @Override
    protected AdjustmentLayer createTypeSpecificCopy(CopyType copyType, Composition newComp) {
        String copyName = copyType.createLayerCopyName(name);
        return new AdjustmentLayer(comp, copyName, filter.copy());
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
    public BufferedImage transformImage(BufferedImage src) {
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
    public boolean hasRasterThumbnail() {
        return false;
    }

    @Override
    public void updateIconImage() {
        // do nothing
    }

    @Override
    public boolean edit() {
        if (filter instanceof FilterWithGUI) {
            return startFilter(filter, false);
        }
        return true;
    }

    @Override
    public boolean isRasterizable() {
        return false;
    }

    @Override
    public boolean exportsORAImage() {
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

        holder.update();
    }

    @Override
    public void startPreview(Filter filter, boolean firstPreview, Component busyCursorTarget) {
        if (!firstPreview) {
            holder.update();
        }
    }

    public void setTentative(boolean tentative) {
        this.tentative = tentative;
    }

    @Override
    public void onFilterDialogAccepted(String filterName) {
        if (showOriginal) {
            filter = lastFilter;
            holder.update();
        } else {
            if (!tentative) {
                History.add(new FilterChangedEdit(this, lastFilter, null));
            }
        }

        lastFilter = null;
        showOriginal = false;
    }

    @Override
    public void onFilterDialogCanceled() {
        if (!showOriginal) {
            filter = lastFilter;

            // when the filter was copied, then it wasn't adjusted to the image size
            updateOptions();

            holder.update();
        }
        lastFilter = null;
        showOriginal = false;
    }

    protected boolean filterSettingsChanged() {
        // equals is overridden for parametrized filters to
        // compare the param values.
        return !filter.equals(lastFilter);
    }

    public void updateOptions() {
        if (filter instanceof ParametrizedFilter pf) {
            pf.getParamSet().updateOptions(this, false);
        }
    }

    @Override
    public void filterWithoutDialogFinished(BufferedImage filteredImage, FilterContext context, String filterName) {
        throw new IllegalStateException();
    }

    @Override
    public void runFilter(Filter filter, FilterContext context) {
        startFilter(filter, false);
    }

    public Filter getFilter() {
        return filter;
    }

    // used only for undo/redo
    public void setFilter(Filter filter) {
        this.filter = filter;

        holder.update();
    }

    @Override
    public String getTypeString() {
        return "Adjustment Layer";
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.add(filter.createDebugNode("filter"));

        return node;
    }
}
