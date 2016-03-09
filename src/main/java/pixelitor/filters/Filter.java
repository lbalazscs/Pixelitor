/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.layers.ImageLayer;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.Utils;
import pixelitor.utils.test.RandomGUITest;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.Serializable;

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
     * Should a default destination buffer be created before running the op or null can be passed and the
     * op will take care of that
     */
    @SuppressWarnings("WeakerAccess")
    protected boolean createDefaultDestBuffer() {
        return true;
    }

    /**
     * This code is executed with busy cursor
     */
    public void runit(ImageLayer layer, ChangeReason changeReason) {
        BufferedImage dest;

        try {
            if (layer == null) {
                if (changeReason == ChangeReason.REPEAT_LAST) {
                    // TODO bug: the repeat last menu is not
                    // always deactivated for text layers
                    return;
                } else {
                    // all other cases should work
                    throw new IllegalStateException("not image layer or mask");
                }
            }

            BufferedImage src = layer.getFilterSourceImage();
            dest = executeForOneLayer(src);

            assert dest != null;

            if (changeReason.isPreview()) {
                layer.changePreviewImage(dest, getName(), changeReason);
            } else {
                layer.filterWithoutDialogFinished(dest, changeReason, getName());
            }
        } catch (Exception e) {
            String msg = String.format(
                    "Error while executing the filter '%s'\n" +
                            "composition = '%s'\n" +
                            "layer = '%s' (%s)\n" +
                            "hasMask = '%s'\n" +
                            "mask editing = '%b'",
                    getName(), layer.getComp().getName(),
                    layer.getName(), layer.getClass().getSimpleName(),
                    layer.hasMask(), layer.isMaskEditing());
            throw new IllegalStateException(msg, e);
        }
    }


    public void execute(ImageLayer layer) {
         execute(layer, ChangeReason.OP_WITHOUT_DIALOG);
    }

    public void execute(ImageLayer layer, ChangeReason changeReason) {
        // only filters without a GUI call this

        executeFilterWithBusyCursor(layer, changeReason, PixelitorWindow.getInstance());
    }

    public BufferedImage executeForOneLayer(BufferedImage src) {
        boolean convertFromGray = false;
        if (src.getType() == BufferedImage.TYPE_BYTE_GRAY) { // editing a mask
            if (!supportsGray()) {
//                System.out.println("Filter::executeForOneLayer: converting to RGB for " + getName());
                convertFromGray = true;
                src = ImageUtils.toSysCompatibleImage(src);
            }
        }

        BufferedImage dest = null;
        if (createDefaultDestBuffer()) {
            dest = ImageUtils.createImageWithSameColorModel(src);
        }

        dest = transform(src, dest);

        if (convertFromGray) { // convert the result back
            dest = ImageUtils.convertToGrayScaleImage(dest);
        }

        runCount++;

        assert dest != null : getName() + " returned null dest";

        return dest;
    }

    /**
     * Executes the given filter with busy cursor
     */
    public void executeFilterWithBusyCursor(ImageLayer layer, ChangeReason changeReason, Component busyCursorParent) {
        String filterName = getName();

        try {
            long startTime = System.nanoTime();

            Runnable task = () -> runit(layer, changeReason);
            Utils.executeWithBusyCursor(busyCursorParent, task);

            long totalTime = (System.nanoTime() - startTime) / 1_000_000;
            String performanceMessage;
            if (totalTime < 1000) {
                performanceMessage = filterName + " took " + totalTime + " ms";
            } else {
                float seconds = totalTime / 1000.0f;
                performanceMessage = String.format("%s took %.1f s", filterName, seconds);
            }
            Messages.showStatusMessage(performanceMessage);
        } catch (OutOfMemoryError e) {
            Dialogs.showOutOfMemoryDialog(e);
        } catch (Throwable e) { // make sure AssertionErrors are caught
            if (RandomGUITest.isRunning()) {
                throw e; // we can debug the exact filter parameters only in RandomGUITest
            }
            Messages.showException(e);
        }

        FilterUtils.setLastExecutedFilter(this);
        RepeatLast.INSTANCE.setActionName("Repeat " + filterName);
    }

    public abstract void randomizeSettings();

    public void setFilterAction(FilterAction filterAction) {
        this.filterAction = filterAction;
    }

    public String getName() {
        if(filterAction != null) {
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
