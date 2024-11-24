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

package pixelitor.gui;

import pixelitor.layers.LayersContainer;
import pixelitor.menus.view.*;

import static pixelitor.utils.AppPreferences.mainPrefs;

/**
 * Manages the visibility of various UI panels.
 */
public class WorkSpace {
    // preference keys for storing visibility states
    private static final String KEY_HISTOGRAMS_SHOWN = "histograms_shown";
    private static final String KEY_LAYERS_SHOWN = "layers_shown";
    private static final String KEY_TOOLS_SHOWN = "tools_shown";
    private static final String KEY_STATUS_BAR_SHOWN = "status_bar_shown";

    // default visibility states
    private static final boolean DEFAULT_HISTOGRAMS_VISIBLE = false;
    private static final boolean DEFAULT_TOOLS_VISIBLE = true;
    private static final boolean DEFAULT_LAYERS_VISIBLE = true;
    private static final boolean DEFAULT_STATUS_BAR_VISIBLE = true;

    // current visibility states
    private boolean histogramsVisible;
    private boolean toolsVisible;
    private boolean layersVisible;
    private boolean statusBarVisible;

    // actions for toggling visibility
    private final ShowHideHistogramsAction histogramsAction;
    private final ShowHideToolsAction toolsAction;
    private final ShowHideLayersAction layersAction;
    private final ShowHideStatusBarAction statusBarAction;
    private final ShowHideAllAction allAction;

    public WorkSpace() {
        // load visibility preferences
        histogramsVisible = mainPrefs.getBoolean(KEY_HISTOGRAMS_SHOWN, DEFAULT_HISTOGRAMS_VISIBLE);
        toolsVisible = mainPrefs.getBoolean(KEY_TOOLS_SHOWN, DEFAULT_TOOLS_VISIBLE);
        layersVisible = mainPrefs.getBoolean(KEY_LAYERS_SHOWN, DEFAULT_LAYERS_VISIBLE);
        statusBarVisible = mainPrefs.getBoolean(KEY_STATUS_BAR_SHOWN, DEFAULT_STATUS_BAR_VISIBLE);

        // initialize toogle actions
        histogramsAction = new ShowHideHistogramsAction(this);
        toolsAction = new ShowHideToolsAction(this);
        layersAction = new ShowHideLayersAction(this);
        statusBarAction = new ShowHideStatusBarAction(this);
        allAction = new ShowHideAllAction(this);
    }

    public void restoreDefaults(PixelitorWindow pw) {
        resetHistogramsVisibility();
        resetToolsVisibility(pw);
        resetLayersVisibility();
        resetStatusBarVisibility();

        pw.getContentPane().revalidate();
    }

    private void resetHistogramsVisibility() {
        if (HistogramsPanel.isShown() != DEFAULT_HISTOGRAMS_VISIBLE) {
            setHistogramsVisible(DEFAULT_HISTOGRAMS_VISIBLE, false);
            histogramsAction.updateText(DEFAULT_HISTOGRAMS_VISIBLE);
        }
    }

    private void resetToolsVisibility(PixelitorWindow pw) {
        if (pw.areToolsShown() != DEFAULT_TOOLS_VISIBLE) {
            setToolsVisible(DEFAULT_TOOLS_VISIBLE, false);
            toolsAction.updateText(DEFAULT_TOOLS_VISIBLE);
        }
    }

    private void resetLayersVisibility() {
        if (LayersContainer.areLayersShown() != DEFAULT_LAYERS_VISIBLE) {
            setLayersVisible(DEFAULT_LAYERS_VISIBLE, false);
            layersAction.updateText(DEFAULT_LAYERS_VISIBLE);
        }
    }

    private void resetStatusBarVisibility() {
        if (StatusBar.isShown() != DEFAULT_STATUS_BAR_VISIBLE) {
            setStatusBarVisible(DEFAULT_STATUS_BAR_VISIBLE, false);
            statusBarAction.updateText(DEFAULT_STATUS_BAR_VISIBLE);
        }
    }

    public boolean areHistogramsVisible() {
        return histogramsVisible;
    }

    public boolean areLayersVisible() {
        return layersVisible;
    }

    public boolean isStatusBarVisible() {
        return statusBarVisible;
    }

    public boolean areToolsVisible() {
        return toolsVisible;
    }

    public void savePreferences() {
        mainPrefs.putBoolean(KEY_HISTOGRAMS_SHOWN, histogramsVisible);
        mainPrefs.putBoolean(KEY_LAYERS_SHOWN, layersVisible);
        mainPrefs.putBoolean(KEY_TOOLS_SHOWN, toolsVisible);
        mainPrefs.putBoolean(KEY_STATUS_BAR_SHOWN, statusBarVisible);
    }

    public void setLayersVisible(boolean v, boolean revalidate) {
        layersVisible = v;
        PixelitorWindow.get().setLayersVisible(v, revalidate);
    }

    public void setHistogramsVisible(boolean v, boolean revalidate) {
        histogramsVisible = v;
        PixelitorWindow.get().setHistogramsVisible(v, revalidate);
    }

    public void setToolsVisible(boolean v, boolean revalidate) {
        toolsVisible = v;
        PixelitorWindow.get().setToolsVisible(v, revalidate);
    }

    public void setStatusBarVisible(boolean v, boolean revalidate) {
        statusBarVisible = v;
        PixelitorWindow.get().setStatusBarVisible(v, revalidate);
    }

    public ShowHideHistogramsAction getHistogramsAction() {
        return histogramsAction;
    }

    public ShowHideToolsAction getToolsAction() {
        return toolsAction;
    }

    public ShowHideLayersAction getLayersAction() {
        return layersAction;
    }

    public ShowHideStatusBarAction getStatusBarAction() {
        return statusBarAction;
    }

    public ShowHideAllAction getAllAction() {
        return allAction;
    }
}
