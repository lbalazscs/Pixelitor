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

import pixelitor.gui.HistogramsPanel;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.StatusBar;
import pixelitor.gui.WorkSpace;
import pixelitor.layers.LayersContainer;

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
    public boolean getStartupVisibility() {
        return !allHidden;
    }

    @Override
    public boolean getCurrentVisibility() {
        return !allHidden;
    }

    @Override
    public void setVisibility(boolean show) {
        var pw = PixelitorWindow.get();
        boolean histogramsAreShown = HistogramsPanel.isShown();
        boolean layersAreShown = LayersContainer.areLayersShown();
        boolean statusBarIsShown = StatusBar.isShown();
        boolean toolsAreShown = pw.areToolsShown();

        // remember the current visibility, but only when hiding
        if (!show) {
            histogramsWereShown = histogramsAreShown;
            layersWereShown = layersAreShown;
            statusBarWasShown = statusBarIsShown;
            toolsWereShown = toolsAreShown;
        }

        // If "Hide All" is running (show=false), then nothing should be shown.
        // If "Restore Workspace" is running (show=true), then show something
        // either because it was hidden by this action or because it is already shown.
        boolean showHistograms = show && (histogramsWereShown || histogramsAreShown);
        boolean showLayers = show && (layersWereShown || layersAreShown);
        boolean showStatusBar = show && (statusBarWasShown || statusBarIsShown);
        boolean showTools = show && (toolsWereShown || toolsAreShown);

        if (histogramsAreShown != showHistograms) {
            workSpace.getHistogramsAction().updateText(show);
            pw.setHistogramsVisible(showHistograms, false);
        }

        if (layersAreShown != showLayers) {
            workSpace.getLayersAction().updateText(show);
            pw.setLayersVisible(showLayers, false);
        }

        if (statusBarIsShown != showStatusBar) {
            workSpace.getStatusBarAction().updateText(show);
            pw.setStatusBarVisible(showStatusBar, false);
        }

        if (toolsAreShown != showTools) {
            workSpace.getToolsAction().updateText(show);
            pw.setToolsVisible(showTools, false);
        }

        pw.getContentPane().revalidate();

        allHidden = !show;
    }
}