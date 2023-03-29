/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Composition;
import pixelitor.FilterContext;
import pixelitor.filters.Filter;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.filters.util.Filters;
import pixelitor.gui.MouseZoomMethod;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.menus.view.ZoomMenu;
import pixelitor.tools.Tools;
import pixelitor.utils.Messages;

import java.awt.Component;
import java.awt.image.BufferedImage;

import static pixelitor.FilterContext.FILTER_WITHOUT_DIALOG;
import static pixelitor.gui.utils.Screens.Align.FRAME_RIGHT;

/**
 * A layer (or layer mask) that can interact with a filtering dialog session.
 */
public interface Filterable {
    Composition getComp();

    /**
     * Initializes a filter previewing session.
     */
    void startPreviewing();

    void stopPreviewing();

    void setShowOriginal(boolean b);

    // if "first" is true, then the settings haven't really changed,
    // this is called only to trigger the first running of the filter
    void previewingFilterSettingsChanged(Filter filter, boolean first, Component busyCursorParent);

    void onFilterDialogAccepted(String filterName);

    void onFilterDialogCanceled();

    void filterWithoutDialogFinished(BufferedImage filteredImage, FilterContext context, String filterName);

    default void startFilter(Filter filter, FilterContext context) {
        startFilter(filter, context, PixelitorWindow.get());
    }

    default void startFilter(Filter filter, FilterContext context, Component busyCursorParent) {
        long startTime = System.nanoTime();

        Runnable task = () -> runFilter(filter, context);
        GUIUtils.runWithBusyCursor(task, busyCursorParent);

        long totalTime = (System.nanoTime() - startTime) / 1_000_000;
        Messages.showPerformanceMessage(filter.getName(), totalTime);

        Filters.setLastFilter(filter);
    }

    void runFilter(Filter filter, FilterContext context);

    /**
     * This method is used when the filter is started from the menu.
     *
     * @return true if the filter was not cancelled
     */
    default boolean startFilter(Filter filter, boolean reset) {
        if (filter instanceof FilterWithGUI fwg) {
            startPreviewing();

            Tools.forceFinish();

            FilterGUI gui = fwg.createGUI(this, reset);

            MouseZoomMethod.CURRENT.installOnJComponent(gui, getComp().getView());
            ZoomMenu.setupZoomKeys(gui);

            return new DialogBuilder()
                .title(filter.getName())
                .menuBar(fwg.getMenuBar())
                .name("filterDialog")
                .content(gui)
                .align(FRAME_RIGHT)
                .withScrollbars()
                .enableCopyVisibleShortcut()
                .okAction(() -> onFilterDialogAccepted(filter.getName()))
                .cancelAction(this::onFilterDialogCanceled)
                .show()
                .wasAccepted();
        }
        startFilter(filter, FILTER_WITHOUT_DIALOG);
        return true;
    }
}
