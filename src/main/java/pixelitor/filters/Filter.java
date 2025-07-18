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

package pixelitor.filters;

import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.filters.gui.PresetOwner;
import pixelitor.filters.gui.UserPreset;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.Debuggable;

import java.awt.image.BufferedImage;
import java.io.Serial;
import java.io.Serializable;

import static pixelitor.utils.ImageUtils.isGrayscale;

/**
 * Base class for all user-selectable filters and color adjustments.
 * A filter transforms an image into another image.
 */
public abstract class Filter implements Serializable, PresetOwner, Debuggable {
    @Serial
    private static final long serialVersionUID = 1L;

    private transient String name;

    // tracking counter to detect unnecessary filter executions
    public static long executionCount = 0;

    protected Filter() {
    }

    /**
     * Applies the core image transformation logic.
     */
    protected abstract BufferedImage transform(BufferedImage src, BufferedImage dest);

    /**
     * Executes the filter, handling grayscale conversion if necessary.
     */
    public BufferedImage transformImage(BufferedImage src) {
        boolean grayConversion = false;

        // handle grayscale images (in layer masks) if
        // the filter doesn't support them directly
        if (isGrayscale(src) && !supportsGray()) {
            // converting grayscale to RGB
            grayConversion = true;
            src = ImageUtils.toSysCompatibleImage(src);
        }

        // create a destination image if the filter requires it
        BufferedImage dest = createDefaultDestImg() ?
            ImageUtils.createImageWithSameCM(src) : null;

        // apply the filter transformation
        dest = transform(src, dest);

        if (grayConversion) { // convert the result back
            dest = ImageUtils.convertToGrayscaleImage(dest);
        }

        executionCount++;

        assert dest != null : getName() + " returned null image";

        return dest;
    }

    /**
     * Returns true if a destination image should be created before the transformation.
     */
    protected boolean createDefaultDestImg() {
        // override to return false if the filter creates the destination image itself
        return true;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        if (name != null) {
            return name;
        }
        // Fall back to class name if no explicit name was set.
        // This happens when filters are created directly rather than through menus.
        return getClass().getSimpleName();
    }

    /**
     * Returns true if this filter can process grayscale (TYPE_BYTE_GRAY) images.
     */
    public boolean supportsGray() {
        // override to return false if the filter only works with RGB images
        return true;
    }

    /**
     * Returns true if this filter can be used as a smart filter.
     * One condition is that the filter must have a no-arg constructor.
     * Another condition is that is must support user presets.
     * Some filters are excluded for other reasons (for example an
     * experimental filter that should not be serialized).
     */
    public boolean canBeSmart() {
        return true;
    }

    /**
     * Returns a string representation of the filter's parameters (for debugging).
     */
    public String paramsAsString() {
        return "";
    }

    /**
     * Serialization support using the serialization proxy pattern.
     */
    @Serial
    protected Object writeReplace() {
        return new SerializationProxy(this);
    }

    @Override
    public boolean supportsUserPresets() {
        return false;
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        // must be overridden if the filter supports presets
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        // must be overridden if the filter supports presets
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPresetDirName() {
        // must be overridden if the filter supports presets
        throw new UnsupportedOperationException();
    }

    public Filter copy() {
        if (canBeSmart()) {
            // the serialization proxy can also create deep copies
            return (Filter) new SerializationProxy(this).readResolve();
        }

        if (this instanceof FilterWithGUI) {
            // These filters have state but no preset support.
            // Not currently a problem since they can't be smart filters.
            throw new UnsupportedOperationException();
        }

        // stateless filters can be shared
        return this;
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = new DebugNode(key, this);
        node.addString("name", getName());
        return node;
    }

    /**
     * Serialization proxy pattern as described in the "Effective Java" book.
     * It decouples a filter's serialized form from its internal fields.
     */
    private static class SerializationProxy implements Serializable {
        @Serial
        private static final long serialVersionUID = -1398273003180176188L;

        private final Class<? extends Filter> filterClass;
        private final String filterName;

        // the serialized state of the filter in preset format
        private String filterState;

        public SerializationProxy(Filter filter) {
            filterClass = filter.getClass();
            filterName = filter.getName();

            if (filter.supportsUserPresets()) {
                filterState = filter.createUserPreset("").writeToString();
            }
        }

        /**
         * Reconstructs and returns a Filter instance from this proxy.
         */
        @Serial
        protected Object readResolve() {
            Filter filter = null;
            try {
                // serializable filters must have a no-argument constructor
                filter = filterClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                String msg = "Could not instantiate " + filterClass.getName();
                Messages.showException(new RuntimeException(msg, e));
            }

            // restore the filter's name and state
            filter.setName(filterName);
            if (filter.supportsUserPresets() && filterState != null) {
                UserPreset preset = new UserPreset("", null);
                preset.loadFromString(filterState);
                filter.loadUserPreset(preset);
            }
            return filter;
        }
    }
}
