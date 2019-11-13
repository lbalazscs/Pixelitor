/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.layers.LayersContainer;

import javax.swing.*;

/**
 * An either "Show Hidden" or "Hide All" action,
 * depending on the current visibility
 */
public class ShowHideAllAction extends ShowHideAction {
    public static final Action INSTANCE = new ShowHideAllAction();

    private boolean histogramsWereShown = false;
    private boolean layersWereShown = false;
    private boolean statusBarWasShown = false;
    private boolean toolsWereShown = false;

    private boolean allHidden = false;

    private ShowHideAllAction() {
        super("Show Hidden", "Hide All");
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
        PixelitorWindow pw = PixelitorWindow.getInstance();
        boolean histogramsAreShown = HistogramsPanel.INSTANCE.isShown();
        boolean layersAreShown = LayersContainer.areLayersShown();
        boolean statusBarIsShown = StatusBar.INSTANCE.isShown();
        boolean toolsAreShown = pw.areToolsShown();

        // remember the current visibility, but only when hiding
        if(!show) {
            histogramsWereShown = histogramsAreShown;
            layersWereShown = layersAreShown;
            statusBarWasShown = statusBarIsShown;
            toolsWereShown = toolsAreShown;
        }

        // If "Hide All" is running (show=false), then nothing should be shown.
        // If "Show Hidden" is running (show=true), then show something
        // either because it was hidden by this action or because it is already shown.
        boolean showHistograms = show && (histogramsWereShown || histogramsAreShown);
        boolean showLayers = show && (layersWereShown || layersAreShown);
        boolean showStatusBar = show && (statusBarWasShown || statusBarIsShown);
        boolean showTools = show && (toolsWereShown || toolsAreShown);

        if (histogramsAreShown != showHistograms) {
            ShowHideHistogramsAction.INSTANCE.updateText(show);
            pw.setHistogramsVisibility(showHistograms, false);
        }

        if (layersAreShown != showLayers) {
            ShowHideLayersAction.INSTANCE.updateText(show);
            pw.setLayersVisibility(showLayers, false);
        }

        if (statusBarIsShown != showStatusBar) {
            ShowHideStatusBarAction.INSTANCE.updateText(show);
            pw.setStatusBarVisibility(showStatusBar, false);
        }

        if (toolsAreShown != showTools) {
            ShowHideToolsAction.INSTANCE.updateText(show);
            pw.setToolsVisibility(showTools, false);
        }

        pw.getContentPane().revalidate();

        allHidden = !show;
    }
}