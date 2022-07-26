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

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.Serial;

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
    private BufferedImage cachedImage;
    private final SmartObject smartObject;
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
        if (next != null) {
            copy.setNext(next.copy(this, newSmartObject));
        }
        return copy;
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

        if (cachedImage != null) {
            return cachedImage;
        }

        cachedImage = applyLayer(null, prevImage, false);
        return cachedImage;
    }

    public SmartFilter getNext() {
        return next;
    }

    public void setNext(SmartFilter next) {
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
            // invalidate only starting from the next one
            if (next != null) {
                next.invalidateChain();
            }
            smartObject.recalculateImage(false);
            comp.update();
        }
    }

    public void settingsChanged() {
        invalidateChain();
        smartObject.recalculateImage(false);
    }

    @Override
    public void activate(boolean addToHistory) {
        smartObject.activate(addToHistory);
    }

    public void setImageSource(ImageSource imageSource) {
        this.imageSource = imageSource;
    }

    @Override
    public void previewingFilterSettingsChanged(Filter filter, boolean first, Component busyCursorParent) {
        if (!first) {
            settingsChanged();
            comp.update();
        }
    }

    public void updateOptions(SmartObject layer) {
        if (filter instanceof ParametrizedFilter pf) {
            pf.getParamSet().updateOptions(layer, false);
        }
    }

    @Override
    public String toString() {
        return "SmartFilter(name=" + getName()
               + ", visibility = " + isVisible() + ")";
    }
}
