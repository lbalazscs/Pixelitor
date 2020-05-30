/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.menus.view.ShowHideHistogramsAction;
import pixelitor.menus.view.ShowHideLayersAction;
import pixelitor.menus.view.ShowHideStatusBarAction;
import pixelitor.menus.view.ShowHideToolsAction;

import static pixelitor.utils.AppPreferences.mainNode;

/**
 * Static utility methods for managing the visibility of
 * various UI areas
 */
public class WorkSpace {
    private static final String HISTOGRAMS_SHOWN_KEY = "histograms_shown";
    private static final String LAYERS_SHOWN_KEY = "layers_shown";
    private static final String TOOLS_SHOWN_KEY = "tools_shown";
    private static final String STATUS_BAR_SHOWN_KEY = "status_bar_shown";

    private static final boolean DEFAULT_HISTOGRAMS_VISIBILITY = false;
    private static final boolean DEFAULT_TOOLS_VISIBILITY = true;
    private static final boolean DEFAULT_LAYERS_VISIBILITY = true;
    private static final boolean DEFAULT_STATUS_BAR_VISIBILITY = true;

    private static boolean histogramsVisibility;
    private static boolean toolsVisibility;
    private static boolean layersVisibility;
    private static boolean statusBarVisibility;

    private static boolean loaded = false;

    private WorkSpace() {
    }

    private static void load() {
        if (loaded) {
            return;
        }

        histogramsVisibility = mainNode.getBoolean(HISTOGRAMS_SHOWN_KEY, DEFAULT_HISTOGRAMS_VISIBILITY);
        toolsVisibility = mainNode.getBoolean(TOOLS_SHOWN_KEY, DEFAULT_TOOLS_VISIBILITY);
        layersVisibility = mainNode.getBoolean(LAYERS_SHOWN_KEY, DEFAULT_LAYERS_VISIBILITY);
        statusBarVisibility = mainNode.getBoolean(STATUS_BAR_SHOWN_KEY, DEFAULT_STATUS_BAR_VISIBILITY);

        loaded = true;
    }

    public static void resetDefaults(PixelitorWindow pw) {
        resetDefaultHistogramsVisibility();
        resetDefaultToolsVisibility(pw);
        resetDefaultLayersVisibility();
        resetDefaultStatusBarVisibility();

        pw.getContentPane().revalidate();
    }

    private static void resetDefaultHistogramsVisibility() {
        if (HistogramsPanel.isShown() != DEFAULT_HISTOGRAMS_VISIBILITY) {
            setHistogramsVisibility(DEFAULT_HISTOGRAMS_VISIBILITY, false);
            ShowHideHistogramsAction.INSTANCE.updateText(DEFAULT_HISTOGRAMS_VISIBILITY);
        }
    }

    private static void resetDefaultToolsVisibility(PixelitorWindow pw) {
        if (pw.areToolsShown() != DEFAULT_TOOLS_VISIBILITY) {
            setToolsVisibility(DEFAULT_TOOLS_VISIBILITY, false);
            ShowHideToolsAction.INSTANCE.updateText(DEFAULT_TOOLS_VISIBILITY);
        }
    }

    private static void resetDefaultLayersVisibility() {
        if (LayersContainer.areLayersShown() != DEFAULT_LAYERS_VISIBILITY) {
            setLayersVisibility(DEFAULT_LAYERS_VISIBILITY, false);
            ShowHideLayersAction.INSTANCE.updateText(DEFAULT_LAYERS_VISIBILITY);
        }
    }

    private static void resetDefaultStatusBarVisibility() {
        if (StatusBar.isShown() != DEFAULT_STATUS_BAR_VISIBILITY) {
            setStatusBarVisibility(DEFAULT_STATUS_BAR_VISIBILITY, false);
            ShowHideStatusBarAction.INSTANCE.updateText(DEFAULT_STATUS_BAR_VISIBILITY);
        }
    }

    public static boolean getHistogramsVisibility() {
        load();
        return histogramsVisibility;
    }

    public static boolean getLayersVisibility() {
        load();
        return layersVisibility;
    }

    public static boolean getStatusBarVisibility() {
        load();
        return statusBarVisibility;
    }

    public static boolean getToolsVisibility() {
        load();
        return toolsVisibility;
    }

    public static void saveVisibility() {
        mainNode.putBoolean(HISTOGRAMS_SHOWN_KEY, histogramsVisibility);
        mainNode.putBoolean(LAYERS_SHOWN_KEY, layersVisibility);
        mainNode.putBoolean(TOOLS_SHOWN_KEY, toolsVisibility);
        mainNode.putBoolean(STATUS_BAR_SHOWN_KEY, statusBarVisibility);
    }

    public static void setLayersVisibility(boolean v, boolean revalidate) {
        layersVisibility = v;
        PixelitorWindow.get().setLayersVisibility(v, revalidate);
    }

    public static void setHistogramsVisibility(boolean v, boolean revalidate) {
        histogramsVisibility = v;
        PixelitorWindow.get().setHistogramsVisibility(v, revalidate);
    }

    public static void setToolsVisibility(boolean v, boolean revalidate) {
        toolsVisibility = v;
        PixelitorWindow.get().setToolsVisibility(v, revalidate);
    }

    public static void setStatusBarVisibility(boolean v, boolean revalidate) {
        statusBarVisibility = v;
        PixelitorWindow.get().setStatusBarVisibility(v, revalidate);
    }
}
