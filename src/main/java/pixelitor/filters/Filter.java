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
 * The superclass of all Pixelitor filters and color adjustments.
 * A filter transforms an image into another image.
 */
public abstract class Filter implements Serializable, PresetOwner, Debuggable {
    @Serial
    private static final long serialVersionUID = 1L;

    private transient String name;

    // used for making sure that there are no
    // unnecessary filter executions triggered
    public static long runCount = 0;

    protected Filter() {
    }

    /**
     * The main functionality of a filter.
     */
    protected abstract BufferedImage transform(BufferedImage src, BufferedImage dest);

    /**
     * Whether a default destination image should be created before
     * running the filter. If this returns false,
     * null will be passed and the filter will take care of that
     */
    protected boolean createDefaultDestImg() {
        return true;
    }

    public BufferedImage transformImage(BufferedImage src) {
        boolean convertFromGray = false;
        if (src.getType() == TYPE_BYTE_GRAY) { // editing a mask
            if (!supportsGray()) {
                convertFromGray = true;
                src = ImageUtils.toSysCompatibleImage(src);
            }
        }

        BufferedImage dest = null;
        if (createDefaultDestImg()) {
            dest = ImageUtils.createImageWithSameCM(src);
        }

        dest = transform(src, dest);

        if (convertFromGray) { // convert the result back
            dest = ImageUtils.convertToGrayScaleImage(dest);
        }

        runCount++;

        assert dest != null : getName() + " returned null dest";

        return dest;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        if (name != null) {
            return name;
        }
        // We cannot assume that a name always exists because the
        // filter can be created directly when it is not necessary
        // to put it in a menu. 
        return getClass().getSimpleName();
    }

    /**
     * Whether this filter supports editing TYPE_BYTE_GRAY
     * images used in layer masks
     */
    public boolean supportsGray() {
        return true;
    }

    public boolean canBeSmart() {
        return true;
    }

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
            // the serialization proxy can also create duplicates
            return (Filter) new SerializationProxy(this).readResolve();
        }

        // can be shared if there are no settings
        // TODO a few filters do have settings, but no preset support.
        return this;
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = new DebugNode(key, this);

        node.addString("name", getName());

        return node;
    }

    /**
     * See the "Effective Java" book for the serialization proxy pattern.
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
            Filter filter = null;
            try {
                filter = filterClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                System.out.println("SerializationProxy::readResolve: could not instantiate " + filterClass.getName());
                Messages.showException(e);
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
