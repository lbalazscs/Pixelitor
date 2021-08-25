/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.FilterContext;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.UserPreset;
import pixelitor.filters.util.FilterAction;
import pixelitor.filters.util.FilterUtils;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.Drawable;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMask;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.test.RandomGUITest;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.io.Serializable;

import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;
import static pixelitor.FilterContext.FILTER_WITHOUT_DIALOG;

/**
 * The superclass of all Pixelitor filters and color adjustments.
 * A filter always works on a {@link Drawable}. If there is a selection,
 * the filter modifies only the selection bounds, otherwise the whole
 * {@link Drawable} is modified, not just the part included in the canvas.
 */
public abstract class Filter implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private transient FilterAction filterAction;

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

    /**
     * The normal starting point, used when called from the menu.
     * Overwritten for filters with GUI.
     * Filters that work normally without a dialog can still have a
     * dialog when invoked from places like "Random Filter".
     *
     * @return true if the filter was not cancelled
     */
    public boolean startOn(Drawable dr, boolean reset) {
        startOn(dr, FILTER_WITHOUT_DIALOG);
        return true;
    }

    /**
     * This method never shows a dialog.
     */
    public void startOn(Drawable dr, FilterContext context) {
        startOn(dr, context, PixelitorWindow.get());
    }

    public void startOn(Drawable dr, FilterContext context, Component busyCursorParent) {
        long startTime = System.nanoTime();

        Runnable task = () -> runFilter(dr, context);
        GUIUtils.runWithBusyCursor(busyCursorParent, task);

        long totalTime = (System.nanoTime() - startTime) / 1_000_000;
        Messages.showPerformanceMessage(getName(), totalTime);

        FilterUtils.setLastFilter(this);
    }

    private void runFilter(Drawable dr, FilterContext context) {
        try {
            if (dr == null) {
                throw new IllegalStateException("not image layer or mask");
            }

            BufferedImage src = dr.getFilterSourceImage();
            BufferedImage dest = transformImage(src);

            assert dest != null;

            if (context.isPreview()) {
                dr.changePreviewImage(dest, getName(), context);
            } else {
                dr.filterWithoutDialogFinished(dest, context, getName());
            }
        } catch (OutOfMemoryError e) {
            Dialogs.showOutOfMemoryDialog(e);
        } catch (Throwable e) {
            Layer layer = (Layer) dr;
            if (layer instanceof LayerMask mask) {
                layer = mask.getOwner();
            }
            String errorDetails = String.format(
                "Error while running the filter '%s'%n" +
                    "composition = '%s'%n" +
                    "layer = '%s' (%s)%n" +
                    "hasMask = '%s'%n" +
                    "mask editing = '%b'%n" +
                    "params = %s",
                getName(), layer.getComp().getName(),
                layer.getName(), layer.getClass().getSimpleName(),
                layer.hasMask(), layer.isMaskEditing(), paramsAsString());

            var ise = new IllegalStateException(errorDetails, e);
            if (RandomGUITest.isRunning()) {
                throw ise; // we can debug the exact filter parameters only in RandomGUITest
            }
            Messages.showException(ise);
        }
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

    public void setFilterAction(FilterAction filterAction) {
        this.filterAction = filterAction;
    }

    public String getName() {
        if (filterAction != null) {
            return filterAction.getName();
        }
        // We cannot assume that a FilterAction always exists because the
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

    public String paramsAsString() {
        return "";
    }

    @Serial
    protected Object writeReplace() {
        return new SerializationProxy(this);
    }

    /**
     * See the "Effective Java" book for the serialization proxy pattern.
     */
    private static class SerializationProxy implements Serializable {
        private final Class<? extends Filter> filterClass;

        // The serialized state of the filter in preset form
        private String filterState;

        public SerializationProxy(Filter filter) {
            filterClass = filter.getClass();

            if (filter instanceof ParametrizedFilter pf) {
                UserPreset preset = pf.getParamSet().toUserPreset(filter.getName(), "ser");
                filterState = preset.saveToString();
            }

        }

        @Serial
        protected Object readResolve() {
            Filter filter = null;
            try {
                filter = filterClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                Messages.showException(e);
            }
            if (filter instanceof ParametrizedFilter pf && filterState != null) {
                ParamSet paramSet = pf.getParamSet();
                UserPreset preset = paramSet.toUserPreset(filter.getName(), "ser");
                preset.loadFromString(filterState);
                paramSet.loadPreset(preset);
            }
            return filter;
        }
    }
}
