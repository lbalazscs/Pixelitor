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

package pixelitor.menus.view;

import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.WorkSpace;

import javax.swing.*;

/**
 * The {@link Action} that toggles the visibility of the
 * histograms, layers, status bar, and tools at the same time.
 * The show action only re-shows the UI elements hidden by this action.
 */
public class ShowHideAllAction extends ShowHideAction {
    private boolean histogramsWereShown = false;
    private boolean layersWereShown = false;
    private boolean statusBarWasShown = false;
    private boolean toolsWereShown = false;

    private boolean allHidden = false;

    public ShowHideAllAction(WorkSpace workSpace) {
        super("restore_ws", "hide_all", workSpace);
    }

    @Override
    public boolean isVisible() {
        return !allHidden;
    }

    @Override
    public void setVisibility(boolean show) {
        var pw = PixelitorWindow.get();

        // when hiding, remember the current visibility
        if (!show) {
            histogramsWereShown = workSpace.areHistogramsVisible();
            layersWereShown = workSpace.areLayersVisible();
            statusBarWasShown = workSpace.isStatusBarVisible();
            toolsWereShown = workSpace.areToolsVisible();
        }

        // determine the target visibility for each panel
        boolean showHistograms = show ? histogramsWereShown : false;
        boolean showLayers = show ? layersWereShown : false;
        boolean showStatusBar = show ? statusBarWasShown : false;
        boolean showTools = show ? toolsWereShown : false;

        // apply changes only where needed
        if (workSpace.areHistogramsVisible() != showHistograms) {
            workSpace.getHistogramsAction().updateText(showHistograms);
            workSpace.setHistogramsVisible(showHistograms, false);
        }

        if (workSpace.areLayersVisible() != showLayers) {
            workSpace.getLayersAction().updateText(showLayers);
            workSpace.setLayersVisible(showLayers, false);
        }

        if (workSpace.isStatusBarVisible() != showStatusBar) {
            workSpace.getStatusBarAction().updateText(showStatusBar);
            workSpace.setStatusBarVisible(showStatusBar, false);
        }

        if (workSpace.areToolsVisible() != showTools) {
            workSpace.getToolsAction().updateText(showTools);
            workSpace.setToolsVisible(showTools, false);
        }

        // revalidate only once at the end
        pw.getContentPane().revalidate();

        allHidden = !show;
    }
}
