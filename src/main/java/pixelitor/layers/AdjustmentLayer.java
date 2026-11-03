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

package pixelitor.layers;

import pixelitor.Composition;
import pixelitor.CopyOptions;
import pixelitor.filters.Filter;
import pixelitor.filters.FilterContext;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.history.FilterChangedEdit;
import pixelitor.history.History;
import pixelitor.io.ORAImageInfo;
import pixelitor.utils.debug.DebugNode;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.util.concurrent.CompletableFuture;

/**
 * An adjustment layer contains a filter that acts on the composite of the layers below it.
 */
public class AdjustmentLayer extends Layer implements Filterable {
    @Serial
    private static final long serialVersionUID = 2L;

    protected Filter filter;

    // a backup of the filter created at the beginning of an editing session
    // to support "Cancel" and "Show Original" in the filter dialog
    private transient Filter filterBackup;

    //  whether the preview is currently showing the original
    //  (unmodified) filter state rather than the edited filter
    private transient boolean showOriginal = false;

    // true if this layer was just added and its configuration dialog
    // is still open (if canceled, the layer will be removed)
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
    protected AdjustmentLayer createTypeSpecificCopy(CopyOptions options, Composition newComp) {
        String copyName = options.createLayerCopyName(name);
        // the filter is copied to ensure the new layer has its own filter instance
        return new AdjustmentLayer(newComp, copyName, filter.copy());
    }

    @Override
    public CompletableFuture<Void> resize(Dimension newSize) {
        // do nothing, as an adjustment layer has no content of its own
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void crop(Rectangle cropRect, boolean deleteCropped, boolean allowGrowing) {
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
        // do nothing; the icon is static
    }

    @Override
    public boolean showEditUI() {
        if (filter instanceof FilterWithGUI) {
            return startFilterInteractively(filter, false);
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
        swapFilters();

        holder.update(); // repaint with the currently active filter
    }

    /**
     * Swaps the active filter with the stored backup.
     */
    private void swapFilters() {
        Filter tmp = filter;
        filter = filterBackup;
        filterBackup = tmp;
    }

    /**
     * If the original is currently shown, undoes the reference swap
     * done by setShowOriginal(true), so that 'filter' is the edited
     * filter and 'filterBackup' is the pre-session state.
     *
     * @return true if the original was being shown
     */
    private boolean undoShowOriginalSwap() {
        if (!showOriginal) {
            return false;
        }
        swapFilters();
        showOriginal = false;
        return true;
    }

    /**
     * Cleans up the transient state from the editing session.
     */
    private void endEditingSession() {
        filterBackup = null;
        showOriginal = false;
    }

    /**
     * Returns true if the preview currently shows the original
     * (pre-session) filter instead of the edited one.
     */
    protected boolean isShowingOriginal() {
        return showOriginal;
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
        // Accepting while "Show Original" is checked doesn't mean that the user
        // wants to discard the changes. Undoing the swap makes 'filter' the edited
        // filter and 'filterBackup' the pre-session state, as in the normal case.
        boolean wasShowingOriginal = undoShowOriginalSwap();

        if (!tentative && filterSettingsChanged()) {
            String editName = getName() + " Changed";
            History.add(new FilterChangedEdit(editName, this, filterBackup, null));
        }

        endEditingSession();

        if (wasShowingOriginal) {
            // the display was showing the original settings
            holder.update();
        }
    }

    @Override
    public void onFilterDialogCanceled() {
        // After this, 'filterBackup' is the pre-session state,
        // whether or not "Show Original" was checked.
        undoShowOriginalSwap();

        // restore the filter state from before the dialog was opened
        filter = filterBackup;

        // the backup is a copy, which wasn't adapted to the image size
        adaptToContext();

        endEditingSession();
        holder.update();
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
    public void filterWithoutDialogFinished(BufferedImage src, BufferedImage dest, FilterContext context, String filterName) {
        // filters without dialogs are allowed, but this
        // method is never called on adjustment layers
        throw new IllegalStateException();
    }

    @Override
    public void runFilter(Filter filter, FilterContext context) {
        startFilterInteractively(filter, false);
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
