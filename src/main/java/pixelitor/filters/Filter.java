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
import pixelitor.Composition;
import pixelitor.ImageComponent;
import pixelitor.ImageComponents;
import pixelitor.PixelitorWindow;
import pixelitor.layers.Layer;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.Utils;

import java.awt.image.BufferedImage;
import java.io.Serializable;

/**
 * The superclass of all Pixelitor filters and color adjustments
 */
public abstract class Filter implements Serializable {
    private static final long serialVersionUID = 1L;

    private transient FilterAction filterAction;

    protected Filter() {
    }

    // used for making sure that there are no unnecessary filters triggered
    public static long runCount = 0;

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
    public void runit(Composition comp, ChangeReason changeReason) {
        BufferedImage src = comp.getFilterSource();
        BufferedImage dest;

        try {
            dest = executeForOneLayer(src);
        } catch (Exception e) {
            Layer activeLayer = comp.getActiveLayer();
            String msg = String.format(
                    "Error while executing the filter '%s'\n" +
                            "composition = '%s'\n" +
                            "layer = '%s'\n" +
                            "mask editing = '%b'",
                    getName(), comp.getName(),
                    activeLayer.getName(), activeLayer.isMaskEditing());
            throw new IllegalStateException(msg, e);
        }

        assert dest != null;

        if (changeReason.isPreview()) {
            comp.changePreviewImage(dest, getName(), changeReason);
        } else {
            comp.filterWithoutDialogFinished(dest, changeReason, getName());
        }
    }

    public void execute() {
        ImageComponent ic = ImageComponents.getActiveIC();
        if (ic != null) {
            if (!ic.activeIsImageLayer()) {
                Messages.showNotImageLayerError();
                return;
            }
            execute(ChangeReason.OP_WITHOUT_DIALOG);
        }
    }

    public void execute(ChangeReason changeReason) {
        // only filters without a GUI call this

        Utils.executeFilterWithBusyCursor(this, changeReason, PixelitorWindow.getInstance());
    }

    public BufferedImage executeForOneLayer(BufferedImage src) {
        boolean convertFromGray = false;
        if (src.getType() == BufferedImage.TYPE_BYTE_GRAY) { // editing a mask
            if (!supportsGray()) {
                System.out.println("Filter::executeForOneLayer: converting to RGB for " + getName());
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
     * The main functionality of a filter.
     */
    protected abstract BufferedImage transform(BufferedImage src, BufferedImage dest);

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
