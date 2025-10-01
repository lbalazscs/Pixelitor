/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.io.ORAImageInfo;
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
 * An adjustment layer contains a filter that acts on the result of the layers below it.
 */
public class AdjustmentLayer extends Layer implements Filterable {
    @Serial
    private static final long serialVersionUID = 2L;

    protected Filter filter;

    // a backup of the filter created at the beginning of an editing session
    // to support "Cancel" and "Show Original" in the filter dialog
    private transient Filter filterBackup;

    // toggles between the current filter and the backup for previewing
    private transient boolean showOriginal = false;

    // a smart filter is tentative when it's not yet part of the composition
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
        filterBackup = null;
        showOriginal = false;
        tentative = false;
    }

    @Override
    protected AdjustmentLayer createTypeSpecificCopy(CopyType copyType, Composition newComp) {
        String copyName = copyType.createLayerCopyName(name);
        // the filter is copied to ensure the new layer has its own filter instance
        return new AdjustmentLayer(newComp, copyName, filter.copy());
    }

    @Override
    public CompletableFuture<Void> resize(Dimension newSize) {
        // do nothing, as an adjustment layer has no content of its own
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void crop(Rectangle2D cropRect, boolean deleteCropped, boolean allowGrowing) {
        // do nothing, as an adjustment layer has no content of its own
    }

    @Override
    public BufferedImage transformImage(BufferedImage src) {
        return filter.transformImage(src);
    }

    @Override
    public void paint(Graphics2D g, boolean firstVisibleLayer) {
        // adjustment layers don't paint directly; they transform the composite image
        throw new UnsupportedOperationException();
    }

    @Override
    public BufferedImage toImage(boolean applyMask, boolean applyOpacity) {
        // an adjustment layer has no image content of its own
        return null;
    }

    @Override
    public boolean hasRasterIcon() {
        return false;
    }

    @Override
    public void updateIconImage() {
        // do nothing, the icon is static
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
    public boolean canExportORAImage() {
        return false;
    }

    @Override
    public ORAImageInfo getORAImageInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void startPreviewing() {
        filterBackup = filter.copy();
    }

    @Override
    public void stopPreviewing() {
        // cleanup is handled in onFilterDialogAccepted/Canceled
    }

    @Override
    public void setShowOriginal(boolean show) {
        assert showOriginal != show : "should toggle";
        showOriginal = show;

        // Swaps the currently active filter object with the stored one.
        // Toggling the checkbox off swaps them back.
        Filter tmp = filter;
        filter = filterBackup;
        filterBackup = tmp;

        holder.update(); // repaint with the currently active filter
    }

    @Override
    public void startPreview(Filter filter, boolean initialPreview, Component busyCursorTarget) {
        // the initial preview of an adjustment layer doesn't change anything,
        // it only shows the initial settings, so no update is needed
        if (!initialPreview) {
            holder.update();
        }
    }

    public void setTentative(boolean tentative) {
        this.tentative = tentative;
    }

    @Override
    public void onFilterDialogAccepted(String filterName) {
        if (showOriginal) {
            // We do not assume that clicking "Show Original" means
            // that the user wants to revert to the backup.
            // We switch the references, because filterBackup holds
            // the modified state, and this is what we want to keep.
            filter = filterBackup;
            holder.update();
        } else {
            if (!tentative && filterSettingsChanged()) {
                String editName = getName() + " Changed";
                History.add(new FilterChangedEdit(editName, this, filterBackup, null));
            }
        }

        // clean up the transient state from the editing session
        filterBackup = null;
        showOriginal = false;
    }

    @Override
    public void onFilterDialogCanceled() {
        // if showOriginal is true, filter already holds
        // the original state, so no change is needed
        if (!showOriginal) {
            // restore the filter state from before the dialog was opened
            filter = filterBackup;

            // when the filter was copied, then it wasn't adjusted to the image size
            adaptToContext();

            holder.update();
        }

        // clean up the transient state from the editing session
        filterBackup = null;
        showOriginal = false;
    }

    /**
     * Checks if the filter settings have changed during the editing session.
     */
    protected boolean filterSettingsChanged() {
        // equals is overridden for parametrized filters to
        // compare the param values.
        return !filter.equals(filterBackup);
    }

    /**
     * Adapts the filter's parameters to the current image
     * if it's a parametrized filter.
     */
    public void adaptToContext() {
        if (filter instanceof ParametrizedFilter pf) {
            pf.getParamSet().adaptToContext(this, false);
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
    public void setFilterFromHistory(Filter filter) {
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
