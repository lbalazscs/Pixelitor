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

import pixelitor.ImageSource;
import pixelitor.filters.Filter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.gui.BlendingModePanel;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.PAction;
import pixelitor.history.History;
import pixelitor.history.LayerBlendingEdit;
import pixelitor.history.LayerOpacityEdit;
import pixelitor.utils.Icons;
import pixelitor.utils.ImageUtils;

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

    public SmartFilter(Filter filter, ImageSource imageSource, SmartObject smartObject) {
        super(smartObject.getComp(), filter.getName(), filter);
        this.imageSource = imageSource;
        this.smartObject = smartObject;
    }

    /**
     * Creates a deep copy of the chain of smart filters.
     * This method must be invoked on the first smart filter,
     * and the rest is added recursively.
     */
    public SmartFilter copy(ImageSource imageSource, SmartObject newSmartObject) {
        SmartFilter copy = (SmartFilter) duplicate(false, true);
        copy.setImageSource(imageSource);
        copy.setSmartObject(newSmartObject);
        if (next != null) {
            copy.setNext(next.copy(this, newSmartObject));
        }
        return copy;
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // defaults for transient fields
        cachedImage = null;

        in.defaultReadObject();
    }

    @Override
    protected Layer createTypeSpecificDuplicate(String duplicateName) {
        SmartFilter duplicate = new SmartFilter(filter.copy(), imageSource, smartObject);
        duplicate.setName(duplicateName, false);
        return duplicate;
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
        this.imageSource = imageSource;
    }

    public void setSmartObject(SmartObject smartObject) {
        this.smartObject = smartObject;
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
    public void setOpacity(float newOpacity, boolean addToHistory, boolean update) {
        assert newOpacity <= 1.0f : "newOpacity = " + newOpacity;
        assert newOpacity >= 0.0f : "newOpacity = " + newOpacity;

        if (opacity == newOpacity) {
            return;
        }
        float prevOpacity = opacity;
        opacity = newOpacity;

        if (update) {
            layerLevelSettingsChanged();
        }

        if (addToHistory) {
            History.add(new LayerOpacityEdit(this, prevOpacity));
        }
    }

    @Override
    public void setBlendingMode(BlendingMode newMode, boolean addToHistory, boolean update) {
        if (blendingMode == newMode) {
            return;
        }

        BlendingMode prevMode = blendingMode;
        blendingMode = newMode;

        if (update) {
            layerLevelSettingsChanged();
        }

        if (addToHistory) {
            History.add(new LayerBlendingEdit(this, prevMode));
        }
    }

    public void filterSettingsChanged() {
        invalidateChain();
        smartObject.recalculateImage(false);
    }

    private void layerLevelSettingsChanged() {
        // invalidate only starting from the next one
        if (next != null) {
            next.invalidateChain();
        }
        smartObject.recalculateImage(true);
        comp.update();
    }

    @Override
    public void onFilterDialogAccepted(String filterName) {
        super.onFilterDialogAccepted(filterName);
        smartObject.updateIconImage();
    }

    @Override
    public void activate(boolean addToHistory) {
        smartObject.activate(addToHistory);
    }

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
            comp.update();
        }
    }

    public void maskUpdated() {
        if (next != null) {
            next.invalidateChain();
        }
        smartObject.recalculateImage(false);
    }

    @Override
    public JPopupMenu createLayerIconPopupMenu() {
        // just create the popup menu from scratch, since the
        // superclasses don't add anything to it
        JPopupMenu popup = new JPopupMenu();

        if (filter instanceof FilterWithGUI) {
            popup.add(new PAction("Edit " + getName() + "...") {
                @Override
                protected void onClick() {
                    edit();
                }
            });
        }
        popup.add(new PAction("Delete " + getName()) {
            @Override
            protected void onClick() {
                smartObject.deleteSmartFilter(SmartFilter.this);
            }
        });
        popup.add(new PAction("Copy " + getName()) {
            @Override
            protected void onClick() {
                Filter.copiedSmartFilter = getFilter().copy();
            }
        });

        popup.add(new PAction("Blending Options...") {
            @Override
            protected void onClick() {
                showBlendingOptions();
            }
        });

        if (!hasMask()) {
            popup.add(new PAction("Add Layer Mask") {
                @Override
                protected void onClick() {
                    addMask(false);
                }
            });
        }

        if (smartObject.getNumSmartFilters() > 1) {
            popup.addSeparator();
            popup.add(new PAction("Move Up", Icons.getNorthArrowIcon()) {
                @Override
                protected void onClick() {
                    smartObject.moveUp(SmartFilter.this);
                }
            });
            popup.add(new PAction("Move Down", Icons.getSouthArrowIcon()) {
                @Override
                protected void onClick() {
                    smartObject.moveDown(SmartFilter.this);
                }
            });
        }

        return popup;
    }

    public void showBlendingOptions() {
        BlendingModePanel panel = new BlendingModePanel(true);

        panel.setOpacity(getOpacity());
        panel.setBlendingMode(getBlendingMode());

        panel.addOpacityListener(newOpacity ->
            setOpacity(newOpacity, true, true));
        panel.addBlendingModeListener(newBlendingMode ->
            setBlendingMode(newBlendingMode, true, true));

        new DialogBuilder()
            .title("Blending Options for " + getName())
            .content(panel)
            .parentComponent((LayerGUI) ui)
            .noCancelButton()
            .okText("Close")
            .show();
    }

    public void updateOptions(SmartObject layer) {
        if (filter instanceof ParametrizedFilter pf) {
            pf.getParamSet().updateOptions(layer, false);
        }
    }

    public boolean checkConsistency() {
        if (!smartObject.containsSmartFilter(this)) {
            throw new AssertionError("smart object '%s' doesn't contain '%s'"
                .formatted(smartObject.getName(), getName()));
        }

        if (next != null) {
            if (next.getImageSource() != this) {
                throw new AssertionError("image source of " + next.getName() + " is not " + getName());
            }
            ;
            //noinspection TailRecursion
            return next.checkConsistency();
        }
        return true;
    }

    @Override
    public String toString() {
        return "SmartFilter(name=" + getName()
               + ", visibility = " + isVisible()
               + ", next = " + (next != null ? next.getName() : "null")
               + ", cached = " + (cachedImage != null)
               + ")";
    }
}
