/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.ChangeReason;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.Dialogs;
import pixelitor.layers.Drawable;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.LayerMask;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.Utils;
import pixelitor.utils.test.RandomGUITest;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.Serializable;

import static pixelitor.ChangeReason.FILTER_WITHOUT_DIALOG;
import static pixelitor.ChangeReason.REPEAT_LAST;

/**
 * The superclass of all Pixelitor filters and color adjustments
 */
public abstract class Filter implements Serializable {
    private static final long serialVersionUID = 1L;

    private transient FilterAction filterAction;

    // used for making sure that there are no unnecessary filters triggered
    public static long runCount = 0;

    protected Filter() {
    }

    /**
     * The main functionality of a filter.
     */
    protected abstract BufferedImage transform(BufferedImage src, BufferedImage dest);

    /**
     * Should a default destination image be created before
     * running the filter. If this returns false,
     * null will be passed and the filter will take care of that
     */
    @SuppressWarnings("WeakerAccess")
    protected boolean createDefaultDestImg() {
        return true;
    }

    /**
     * The normal starting point, used when called in the menu
     * Overwritten for filters with GUI
     */
    public void startOn(Drawable dr) {
        startOn(dr, FILTER_WITHOUT_DIALOG);
    }

    public void startOn(Drawable dr, ChangeReason cr) {
        run(dr, cr, PixelitorWindow.getInstance());
    }

    public void run(Drawable dr, ChangeReason cr, Component busyCursorParent) {
        String filterName = getName();

        long startTime = System.nanoTime();

        Runnable task = () -> transformAndHandleExceptions(dr, cr);
        Utils.runWithBusyCursor(busyCursorParent, task);

        long totalTime = (System.nanoTime() - startTime) / 1_000_000;
        Messages.showPerformanceMessage(filterName, totalTime);

        FilterUtils.setLastFilter(this);
        RepeatLast.INSTANCE.setActionName("Repeat " + filterName);
    }

    private void transformAndHandleExceptions(Drawable dr, ChangeReason cr) {
        BufferedImage dest;

        try {
            if (dr == null) {
                if (cr == REPEAT_LAST) {
                    // TODO bug: the repeat last menu is not
                    // always deactivated for text layers
                    return;
                } else {
                    // all other cases should work
                    throw new IllegalStateException("not image layer or mask");
                }
            }

            BufferedImage src = dr.getFilterSourceImage();
            dest = transformImage(src);

            assert dest != null;

            if (cr.isPreview()) {
                dr.changePreviewImage(dest, getName(), cr);
            } else {
                dr.filterWithoutDialogFinished(dest, cr, getName());
            }
        } catch (OutOfMemoryError e) {
            Dialogs.showOutOfMemoryDialog(e);
        } catch (Throwable e) {
            ImageLayer layer = (ImageLayer) dr;
            if (layer instanceof LayerMask) {
                layer = (ImageLayer) layer.getParent();
            }
            String msg = String.format(
                    "Error while running the filter '%s'\n" +
                            "composition = '%s'\n" +
                            "layer = '%s' (%s)\n" +
                            "hasMask = '%s'\n" +
                            "mask editing = '%b'",
                    getName(), layer.getComp()
                            .getName(),
                    layer.getName(), layer.getClass()
                            .getSimpleName(),
                    layer.hasMask(), layer.isMaskEditing());


            IllegalStateException ise = new IllegalStateException(msg, e);
            if (RandomGUITest.isRunning()) {
                throw ise; // we can debug the exact filter parameters only in RandomGUITest
            }
            Messages.showException(ise);
        }
    }

    public BufferedImage transformImage(BufferedImage src) {
        boolean convertFromGray = false;
        if (src.getType() == BufferedImage.TYPE_BYTE_GRAY) { // editing a mask
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
        // to put it in a menu. Therefore return the class name.
        return getClass().getSimpleName();
    }

    public String getListName() {
        return filterAction.getListName();
    }

    /**
     * Whether this filter supports editing TYPE_BYTE_GRAY
     * images used in layer masks
     */
    public boolean supportsGray() {
        return true;
    }
}
