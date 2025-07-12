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

package pixelitor.layers;

import pixelitor.FilterContext;
import pixelitor.filters.Filter;
import pixelitor.gui.utils.Dialogs;
import pixelitor.tools.util.PPoint;
import pixelitor.tools.util.PRectangle;
import pixelitor.utils.Messages;
import pixelitor.utils.test.RandomGUITest;

import java.awt.Component;
import java.awt.Composite;
import java.awt.image.BufferedImage;

/**
 * Interface for layers with pixel-based content, such as image
 * layers or masks. They can be used with brush tools and filters.
 */
public interface Drawable extends Filterable {
    BufferedImage getImage();

    /**
     * Sets the image ignoring the selection
     */
    void setImage(BufferedImage newImage);

    void changeImageForUndoRedo(BufferedImage img, boolean ignoreSelection);

    TmpLayer createTmpLayer(Composite c, boolean softSelection);

    void mergeTmpDrawingLayerDown();

    BufferedImage getCanvasSizedSubImage();

    BufferedImage getFilterSourceImage();

    /**
     * Returns the subimage determined by the selection bounds,
     * or the image if there is no selection.
     */
    BufferedImage getSelectedSubImage(boolean copyIfNoSelection);

    /**
     * Returns the image from which the "image position" thumbnail is calculated.
     * The canvas size is not considered, only the selection.
     */
    BufferedImage getImageForFilterDialogs();

    void debugImages();

    void updateIconImage();

    /**
     * Returns the X translation of the underlying BufferedImage
     * relative to the canvas (always zero or negative).
     */
    int getTx();

    /**
     * Returns the Y translation of the underlying BufferedImage
     * relative to the canvas (always zero or negative).
     */
    int getTy();

    boolean isMaskEditing();

    String getName();

    void changePreviewImage(BufferedImage newPreview, String filterName, FilterContext context);

    @Override
    default void startPreview(Filter filter, boolean initialPreview, Component busyCursorTarget) {
        startFilter(filter, FilterContext.PREVIEWING, busyCursorTarget);
    }

    @Override
    default void runFilter(Filter filter, FilterContext context) {
        try {
            BufferedImage src = getFilterSourceImage();
            assert src != null;

            BufferedImage dest = filter.transformImage(src);
            assert dest != null;

            if (context.isPreview()) {
                changePreviewImage(dest, filter.getName(), context);
            } else {
                filterWithoutDialogFinished(dest, context, filter.getName());
            }
        } catch (OutOfMemoryError e) {
            Dialogs.showOutOfMemoryDialog(e);
        } catch (Throwable e) {
            String errorDetails = String.format(
                "Error while running the filter '%s'%n" +
                    "composition = '%s'%n" +
                    "layer = '%s' (%s)%n" +
                    "params = %s",
                filter.getName(),
                getComp().getDebugName(),
                getName(), getClass().getSimpleName(),
                filter.paramsAsString());

            var ise = new IllegalStateException(errorDetails, e);
            if (RandomGUITest.isRunning()) {
                throw ise; // we can debug the exact filter parameters only in RandomGUITest
            }
            Messages.showException(ise);
        }
    }

    void update();

    void repaintRegion(PPoint start, PPoint end, double thickness);

    void repaintRegion(PRectangle area);
}
