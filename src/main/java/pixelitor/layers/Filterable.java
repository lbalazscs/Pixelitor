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

package pixelitor.layers;

import pixelitor.Composition;
import pixelitor.FilterContext;
import pixelitor.Views;
import pixelitor.filters.Filter;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.filters.util.Filters;
import pixelitor.gui.MouseZoomMethod;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.View;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.menus.view.ZoomMenu;
import pixelitor.tools.Tools;
import pixelitor.utils.Cursors;
import pixelitor.utils.Messages;
import pixelitor.utils.Threads;

import javax.swing.*;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.image.BufferedImage;

import static pixelitor.FilterContext.FILTER_WITHOUT_DIALOG;
import static pixelitor.gui.utils.Screens.Align.FRAME_RIGHT;

/**
 * Interface for layers that can interact with a filtering dialog
 * session, enabling filter adjustments and preview.
 */
public interface Filterable {
    Composition getComp();

    /**
     * Initializes a filter previewing session.
     */
    void startPreviewing();

    void stopPreviewing();

    void setShowOriginal(boolean b);

    // if "firstPreview" is true, then the settings haven't yet changed,
    // this is called only to trigger the first preview run of the filter
    void startPreview(Filter filter, boolean firstPreview, Component busyCursorTarget);

    void onFilterDialogAccepted(String filterName);

    void onFilterDialogCanceled();

    void filterWithoutDialogFinished(BufferedImage filteredImage, FilterContext context, String filterName);

    default void startFilter(Filter filter, FilterContext context) {
        startFilter(filter, context, PixelitorWindow.get());
    }

    default void startFilter(Filter filter, FilterContext context, Component busyCursorTarget) {
        long startTime = System.nanoTime();

        Runnable task = () -> runFilter(filter, context);
        GUIUtils.runWithBusyCursor(task, busyCursorTarget);

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
        assert Threads.calledOnEDT();

        if (filter instanceof FilterWithGUI fwg) {
            PixelitorWindow.get().setCursor(Cursors.BUSY);
            View view = Views.getActive();

            // Save the view cursor set by the current tool
            // so that it can be restored later.
            Cursor toolViewCursor = view.getCursor();
            view.setCursor(Cursors.BUSY);

            startPreviewing();

            Tools.forceFinish();

            FilterGUI gui = fwg.createGUI(this, reset);

            MouseZoomMethod.CURRENT.installOnOther(gui, getComp().getView());
            ZoomMenu.setupZoomKeys(gui);

            DialogBuilder dialogBuilder = new DialogBuilder()
                .title(filter.getName())
                .menuBar(fwg.getMenuBar())
                .name("filterDialog")
                .content(gui)
                .withScrollbars()
                .enableCopyShortcuts()
                .onVisibleAction(() -> gui.startPreview(true))
                .okAction(() -> onFilterDialogAccepted(filter.getName()))
                .cancelAction(this::onFilterDialogCanceled);
            JDialog dialog = dialogBuilder.build();

            PixelitorWindow.get().setCursor(Cursors.DEFAULT);
            view.setCursor(toolViewCursor);

            GUIUtils.showDialog(dialog, FRAME_RIGHT);
            return dialogBuilder.wasAccepted();
        }
        startFilter(filter, FILTER_WITHOUT_DIALOG);
        return true;
    }
}
