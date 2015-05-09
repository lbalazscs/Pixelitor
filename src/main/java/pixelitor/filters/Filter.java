/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

import pixelitor.Build;
import pixelitor.ChangeReason;
import pixelitor.Composition;
import pixelitor.PixelitorWindow;
import pixelitor.layers.Layers;
import pixelitor.utils.Dialogs;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

/**
 * The superclass of all Pixelitor filters and color adjustments
 */
public abstract class Filter extends AbstractAction {
    protected boolean copySrcToDstBeforeRunning = false;
    protected String listNamePrefix = null;

    // used for making sure that there are no unnecessary filters triggered
    public static long runCount = 0;

    protected Filter(String name) {
        this(name, null);
    }

    protected Filter(String name, Icon icon) {
        assert name != null;

        putValue(Action.SMALL_ICON, icon);
        putValue(Action.NAME, name);

        FilterUtils.addFilter(this);
    }


    public String getMenuName() {
        return (String) getValue(Action.NAME);
    }

    public void setMenuName(String s) {
        putValue(Action.NAME, s);
    }

    public String getName() {
        return getMenuName();
    }

    /**
     * This name appears when all filters are listed in a combo box
     * For example "Fill with Transparent" is better than "Transparent" in this case
     */
    public String getListName() {
        if (listNamePrefix == null) {
            return getName();
        } else {
            return listNamePrefix + getName();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!Layers.activeIsImageLayer()) {
            Dialogs.showNotImageLayerDialog();
            return;
        }

        execute(ChangeReason.OP_WITHOUT_DIALOG);
    }

    @Override
    public String toString() {
        return getListName();
    }

    /**
     * Should the contents of the source BufferedImage be copied into the destination before running the op
     */
    @SuppressWarnings("WeakerAccess")
    protected boolean copyContents() {
        // TODO - not overwritten - should be removed?
        return copySrcToDstBeforeRunning;
    }

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

//        Utils.debugImage(src, "src");

        BufferedImage dest = executeForOneLayer(src);
        assert dest != null;

        if (changeReason.isPreview()) {
            comp.changePreviewImage(dest, getName());
        } else {
            comp.filterWithoutDialogFinished(dest, changeReason, getName());
        }
    }

    public void execute(ChangeReason changeReason) {
        // only filters without a GUI call this

        Utils.executeFilterWithBusyCursor(this, changeReason, PixelitorWindow.getInstance());
    }

    public BufferedImage executeForOneLayer(BufferedImage src) {
        BufferedImage dest = null;
        if (createDefaultDestBuffer()) {
            if (copyContents()) {
                dest = ImageUtils.copyImage(src);
            } else {
                dest = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
            }
        }

        dest = transform(src, dest);
        runCount++;
//        System.out.println(String.format("Filter::executeForOneLayer: transformed '%s'", getName()));

        if (dest == null) {
            if (Build.CURRENT == Build.DEVELOPMENT) {
                System.out.println(String.format("Filter::executeForOneLayer: '%s' returned null dest", getName()));
            }
        }
        assert dest != null;

        return dest;
    }

    protected abstract BufferedImage transform(BufferedImage src, BufferedImage dest);

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Filter filter = (Filter) o;

        if (!getListName().equals(filter.getListName())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return getListName().hashCode();
    }

    public abstract void randomizeSettings();
}
