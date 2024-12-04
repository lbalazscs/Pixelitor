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

package pixelitor.filters;

import pixelitor.filters.gui.PresetOwner;
import pixelitor.filters.gui.UserPreset;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.Debuggable;

import java.awt.image.BufferedImage;
import java.io.Serial;
import java.io.Serializable;

import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;

/**
 * Base class for all filters and color adjustments in Pixelitor.
 * A filter transforms an image into another image.
 */
public abstract class Filter implements Serializable, PresetOwner, Debuggable {
    @Serial
    private static final long serialVersionUID = 1L;

    private transient String name;

    // used for making sure that there are no
    // unnecessary filter executions triggered
    public static long executionCount = 0;

    protected Filter() {
    }

    /**
     * The core image transformation logic.
     */
    protected abstract BufferedImage transform(BufferedImage src, BufferedImage dest);

    /**
     * Executes the filter transformation while handling
     * conversion for grayscale images if needed.
     */
    public BufferedImage transformImage(BufferedImage src) {
        boolean grayConversion = false;
        if (src.getType() == TYPE_BYTE_GRAY && !supportsGray()) {
            // converting the image to RGB, because the filter
            // doesn't support the grayscale image of a layer mask
            grayConversion = true;
            src = ImageUtils.toSysCompatibleImage(src);
        }

        BufferedImage dest = createDefaultDestImg() ?
            ImageUtils.createImageWithSameCM(src) : null;

        dest = transform(src, dest);

        if (grayConversion) { // convert the result back
            dest = ImageUtils.convertToGrayScaleImage(dest);
        }

        executionCount++;

        assert dest != null : getName() + " returned null image";

        return dest;
    }

    /**
     * Determines if a default destination image should be created
     * before running the filter.
     * Override this method to return false if the filter creates
     * the destination image itself.
     */
    protected boolean createDefaultDestImg() {
        return true;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        if (name != null) {
            return name;
        }
        // We cannot assume that a name always exists because the
        // filter can be created directly without being put in a menu.
        return getClass().getSimpleName();
    }

    /**
     * Whether this filter can process grayscale
     * images (TYPE_BYTE_GRAY) used in layer masks.
     */
    public boolean supportsGray() {
        return true;
    }

    /**
     * Whether this filter can be used as a smart filter.
     * One condition is that the filter must have a no-arg constructor.
     * Another condition is that is must support user presets.
     */
    public boolean canBeSmart() {
        return true;
    }

    /**
     * Returns a string representation of the filter's current parameters.
     */
    public String paramsAsString() {
        return "";
    }

    @Serial
    protected Object writeReplace() {
        return new SerializationProxy(this);
    }

    @Override
    public boolean canHaveUserPresets() {
        return false;
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        // overridden if the filter supports presets
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        // overridden if the filter supports presets
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPresetDirName() {
        // overridden if the filter supports presets
        throw new UnsupportedOperationException();
    }

    public Filter copy() {
        if (canHaveUserPresets()) {
            // the serialization proxy can also create deep copies
            return (Filter) new SerializationProxy(this).readResolve();
        }

        // Stateless filters can be shared.
        // TODO a few filters do have settings, but no preset support.
        //   Currently not a problem because they can't be smart filters.
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
     */
    private static class SerializationProxy implements Serializable {
        @Serial
        private static final long serialVersionUID = -1398273003180176188L;

        private final Class<? extends Filter> filterClass;
        private final String filterName;

        // The serialized state of the filter in preset format
        private String filterState;

        public SerializationProxy(Filter filter) {
            filterClass = filter.getClass();
            filterName = filter.getName();

            if (filter.canHaveUserPresets()) {
                filterState = filter.createUserPreset("").saveToString();
            }
        }

        @Serial
        protected Object readResolve() {
            // When deserializing, the filter constructor is called,
            // and then the state is restored from the preset.
            // Serializable filters must have a no-argument constructor.
            Filter filter = null;
            try {
                filter = filterClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                String msg = "Could not instantiate " + filterClass.getName();
                Messages.showException(new RuntimeException(msg, e));
            }
            filter.setName(filterName);
            if (filter.canHaveUserPresets() && filterState != null) {
                UserPreset preset = new UserPreset("", null);
                preset.loadFromString(filterState);
                filter.loadUserPreset(preset);
            }
            return filter;
        }
    }
}
