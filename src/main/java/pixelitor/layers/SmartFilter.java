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
import pixelitor.CopyType;
import pixelitor.filters.Filter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.filters.util.FilterAction;
import pixelitor.filters.util.FilterSearchPanel;
import pixelitor.gui.utils.PAction;
import pixelitor.history.FilterChangedEdit;
import pixelitor.history.History;
import pixelitor.utils.Icons;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Smart filters allow non-destructive editing, but unlike
 * regular adjustment layers, they don't have to run every time
 * some other layer is edited, because their output is cached
 * inside the smart object. Additionally, smart filters also
 * cache their own output, so that if the filter settings are changed,
 * only the filters downstream from that filter will be rerun.
 */
public class SmartFilter extends AdjustmentLayer implements ImageSource {
    @Serial
    private static final long serialVersionUID = 1L;

    private ImageSource imageSource;
    private transient BufferedImage cachedImage;
    private SmartObject smartObject;

    // the next smart filter in the chain of smart filters
    private SmartFilter next;

    // static field for the copy-paste of smart filters
    public static SmartFilter copiedSmartFilter;

    public SmartFilter(Filter filter, ImageSource imageSource, SmartObject smartObject) {
        super(smartObject.getComp(), filter.getName(), filter);
        setImageSource(imageSource);
        setSmartObject(smartObject);
        holder = smartObject;
    }

    public SmartFilter(SmartFilter orig, Composition newComp, String name) {
        super(newComp, name, orig.getFilter().copy());

        this.imageSource = orig.imageSource;
        this.smartObject = orig.smartObject;

        holder = smartObject;
        cachedImage = orig.cachedImage; // safe to share
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // defaults for transient fields
        cachedImage = null;

        in.defaultReadObject();

        // migrate
        holder = smartObject;
    }

    @Override
    protected SmartFilter createTypeSpecificCopy(CopyType copyType, Composition newComp) {
        return new SmartFilter(this, newComp, copyType.createLayerCopyName(name));
    }

    @Override
    public BufferedImage getImage() {
        // If not visible, then ignore the cached image.
        // The previous image must be cached anyway.
        BufferedImage prevImage = imageSource.getImage();
        if (!isVisible()) {
            return prevImage;
        }

        return adjustImageWithMasksAndBlending(prevImage, false);
    }

    @Override
    protected BufferedImage adjustImageWithMasksAndBlending(BufferedImage imgSoFar,
                                                            boolean isFirstVisibleLayer) {
        if (isFirstVisibleLayer) {
            return imgSoFar; // there's nothing we can do
        }
        BufferedImage transformed = applyOnImage(imgSoFar);

        if (usesMask()) {
            // copy, because otherwise different masks
            // are applied to the same cached image
            transformed = ImageUtils.copyImage(transformed);

            mask.applyTo(transformed);
        }
        if (!usesMask() && isNormalAndOpaque()) {
            return transformed;
        } else {
            // unlike an adjustment layer, this makes sure that imgSoFar
            // (which could be cached in the image source is not modified
            BufferedImage copy = ImageUtils.copyImage(imgSoFar);

            Graphics2D g = copy.createGraphics();
            setupDrawingComposite(g, isFirstVisibleLayer);
            g.drawImage(transformed, 0, 0, null);
            g.dispose();
            return copy;
        }
    }

    @Override
    public BufferedImage applyOnImage(BufferedImage src) {
        if (cachedImage != null) {
            return cachedImage;
        }
        assert src != null;
        cachedImage = filter.transformImage(src);

        // TODO this check should not be necessary
        if (cachedImage == src) {
            cachedImage = ImageUtils.copyImage(cachedImage);
        }

        return cachedImage;
    }

    public ImageSource getImageSource() {
        return imageSource;
    }

    public void setImageSource(ImageSource imageSource) {
        this.imageSource = Objects.requireNonNull(imageSource);
    }

    public SmartObject getSmartObject() {
        return smartObject;
    }

    public void setSmartObject(SmartObject smartObject) {
        this.smartObject = Objects.requireNonNull(smartObject);
        this.holder = smartObject;
    }

    @Override
    public void setHolder(LayerHolder holder) {
        // setSmartObject should be used to set the holder
        throw new IllegalStateException();
    }

    @Override
    public LayerHolder getHolderForNewLayers() {
        // don't try to add regular layers inside a smart object
        // just because a smart filter is selected.
        return smartObject.getHolderForNewLayers();
    }

    public SmartFilter getNext() {
        return next;
    }

    public void setNext(SmartFilter next) {
        assert next != this;
        this.next = next;
    }

    /**
     * Recursively invalidates all smart filters after the edited one.
     */
    public void invalidateChain() {
        invalidateCache();
        if (next != null) {
            next.invalidateChain();
        }
    }

    public Stream<SmartFilter> getChain() {
        return Stream.iterate(this, Objects::nonNull, SmartFilter::getNext);
    }

    public String debugChain() {
        return getChain()
            .limit(5) // prevent infinite recursion
            .map(SmartFilter::toString)
            .collect(Collectors.joining(", ", "[", "]"));
    }

    private void invalidateCache() {
//        System.out.println("SmartFilter::invalidateCache: CALLED for " + getName());
        if (cachedImage != null) {
            cachedImage.flush();
            cachedImage = null;
        }
    }

    public boolean hasCachedImage() {
        return cachedImage != null;
    }

    @Override
    public void setVisible(boolean newVisibility, boolean addToHistory, boolean update) {
        super.setVisible(newVisibility, addToHistory, false);
        if (update) {
            layerLevelSettingsChanged();
        }
    }

    @Override
    public void setMaskEnabled(boolean maskEnabled, boolean addToHistory) {
        super.setMaskEnabled(maskEnabled, addToHistory);
        layerLevelSettingsChanged();
    }

    @Override
    public void deleteMask(boolean addToHistory) {
        super.deleteMask(addToHistory);
        layerLevelSettingsChanged();
    }

    @Override
    public void addConfiguredMask(LayerMask mask) {
        super.addConfiguredMask(mask);
        layerLevelSettingsChanged();
    }

    @Override
    public void setOpacity(float newOpacity, boolean addToHistory, boolean update) {
        if (opacity == newOpacity) {
            return;
        }
        super.setOpacity(newOpacity, addToHistory, false);
        if (update) {
            layerLevelSettingsChanged();
        }
    }

    @Override
    public void setBlendingMode(BlendingMode newMode, boolean addToHistory, boolean update) {
        if (blendingMode == newMode) {
            return;
        }
        super.setBlendingMode(newMode, addToHistory, false);
        if (update) {
            layerLevelSettingsChanged();
        }
    }

    public void filterSettingsChanged() {
        invalidateChain();
        smartObject.invalidateImageCache();
    }

    // "layer level" = not related to the filter settings
    private void layerLevelSettingsChanged() {
        // invalidate only starting from the next one
        if (next != null) {
            next.invalidateChain();
        }
        smartObject.invalidateImageCache();
        holder.update();
        smartObject.updateIconImage();
    }

    @Override
    public void onFilterDialogAccepted(String filterName) {
        super.onFilterDialogAccepted(filterName);
        smartObject.updateIconImage();
    }

    @Override
    public void onFilterDialogCanceled() {
        super.onFilterDialogCanceled();
        filterSettingsChanged();
    }

    @Override
    public void setFilter(Filter filter) {
        this.filter = filter;

        filterSettingsChanged();
        holder.update();
    }

//    @Override
//    public boolean isActiveRoot() {
//        return smartObject.isActiveRoot();
//    }

    @Override
    public void setMaskEditing(boolean newValue) {
        super.setMaskEditing(newValue);
        if (newValue) {
            smartObject.setFilterMaskEditing(this);
        }
    }

    @Override
    public void previewingFilterSettingsChanged(Filter filter, boolean first, Component busyCursorParent) {
        if (!first) {
            filterSettingsChanged();
            holder.update();
        }
    }

    public void maskChanged() {
        if (next != null) {
            next.invalidateChain();
        }
        smartObject.invalidateImageCache();
    }

    @Override
    public JPopupMenu createLayerIconPopupMenu() {
        // just create the popup menu from scratch, since the
        // superclasses don't add anything to it
        JPopupMenu popup = new JPopupMenu();

        if (filter instanceof FilterWithGUI) {
            popup.add(new PAction("Edit " + getName() + "...", this::edit));
        }
        popup.add(new PAction("Delete " + getName(), () ->
            smartObject.deleteSmartFilter(this, true, true)));
        popup.add(new PAction("Copy " + getName(), () ->
            copiedSmartFilter = (SmartFilter) copy(CopyType.UNDO, true, comp)));

        if (!hasMask()) {
            popup.add(new PAction("Add Layer Mask", () -> addMask(false)));
        }

        popup.add(new PAction("Change Filter Type...", this::replaceFilter));

        if (smartObject.getNumSmartFilters() > 1) {
            popup.addSeparator();
            popup.add(new PAction("Move Up", Icons.getNorthArrowIcon(), () ->
                smartObject.moveUp(this)));
            popup.add(new PAction("Move Down", Icons.getSouthArrowIcon(), () ->
                smartObject.moveDown(this)));
        }

        return popup;
    }

    private void replaceFilter() {
        FilterAction action = FilterSearchPanel.showInDialog("Replace " + filter.getName());
        if (action != null) {
            Filter oldFilter = filter;
            String oldName = getName();

            filter = action.createNewInstanceFilter();
            setName(filter.getName(), false);

            History.add(new FilterChangedEdit(this, oldFilter, oldName));

            filterSettingsChanged();
            holder.update();
            edit();
        }
    }

    public void updateOptions(SmartObject layer) {
        if (filter instanceof ParametrizedFilter pf) {
            pf.getParamSet().updateOptions(this, false);
        }
    }

    public BufferedImage getCachedImage() {
        return cachedImage;
    }

    @Override
    public boolean checkInvariants() {
        if (!super.checkInvariants()) {
            return false;
        }

        if (!smartObject.containsSmartFilter(this)) {
            throw new AssertionError("smart object '%s' doesn't contain '%s'"
                .formatted(smartObject.getName(), getName()));
        }

        if (next != null) {
            if (next.getImageSource() != this) {
                throw new AssertionError("image source of " + next.getName() + " is not " + getName());
            }
        }

        return true;
    }

    @Override
    public String getTypeString() {
        return "Smart Filter";
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.addString("imageSource class", imageSource.getClass().getSimpleName());
        node.add(imageSource.createDebugNode("imageSource"));
        node.addString("next", next == null ? "null" : next.toString());
        node.addBoolean("cached", (cachedImage != null));

        return node;
    }
}
