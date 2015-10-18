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

package pixelitor;

import pixelitor.layers.GlobalLayerChangeListener;
import pixelitor.layers.GlobalLayerMaskChangeListener;
import pixelitor.layers.Layer;
import pixelitor.tools.Symmetry;
import pixelitor.tools.Tools;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Static methods to support global application logic
 */
public class AppLogic {
    /**
     * Global listeners which always act on the active layer of the active composition
     */
    private static final Collection<GlobalLayerChangeListener> layerChangeListeners = new ArrayList<>();
    private static final Collection<GlobalLayerMaskChangeListener> layerMaskChangeListeners = new ArrayList<>();

    private AppLogic() {
    }

    public static void activeCompSizeChanged(Composition comp) {
        Symmetry.setCompositionSize(comp.getCanvasWidth(), comp.getCanvasHeight());
    }

    public static void addLayerChangeListener(GlobalLayerChangeListener listener) {
        layerChangeListeners.add(listener);
    }

    public static void addLayerMaskChangeListener(GlobalLayerMaskChangeListener listener) {
        layerMaskChangeListeners.add(listener);
    }

    public static void maskChanged(Layer affectedLayer) {
        for (GlobalLayerMaskChangeListener listener : layerMaskChangeListeners) {
            listener.maskAddedOrDeleted(affectedLayer);
        }
    }

    // used for GUI updates
    public static void activeCompLayerCountChanged(Composition comp, int newLayerCount) {
        for (GlobalLayerChangeListener listener : layerChangeListeners) {
            listener.activeCompLayerCountChanged(comp, newLayerCount);
        }
    }

    public static void activeLayerChanged(Layer newActiveLayer) {
        assert newActiveLayer != null;
        for (GlobalLayerChangeListener listener : layerChangeListeners) {
            listener.activeLayerChanged(newActiveLayer);
        }

        // if the new active layer has no layer mask, then
        // disable all mask-editing modes
        if (!newActiveLayer.hasMask()) {
            Composition comp = newActiveLayer.getComp();
            if (comp.hasRealIC()) {
                comp.getIC().setShowLayerMask(false);
                FgBgColors.setLayerMaskEditing(false);
            }
        }
    }

    public static void layerOrderChanged(Composition comp) {
        for (GlobalLayerChangeListener listener : layerChangeListeners) {
            listener.layerOrderChanged(comp);
        }
    }

    public static void exitApp(PixelitorWindow pw) {
        if (ImageComponents.thereAreUnsavedChanges()) {
            int answer = JOptionPane.showConfirmDialog(null, "There are unsaved changes. Are you sure you want to exit?", "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                pw.setVisible(false);
                AppPreferences.savePrefsAndExit();
            }
        } else {
            pw.setVisible(false);
            AppPreferences.savePrefsAndExit();
        }
    }

    /**
     * Adds a composition to the app.
     */
    public static void addComposition(Composition comp) {
        try {
            ImageComponent ic = new ImageComponent(comp);
            ic.setCursor(Tools.getCurrentTool().getCursor());
            ImageComponents.setActiveIC(ic, false);
            comp.addLayersToGUI();

            Desktop.INSTANCE.addNewImageComponent(ic);
        } catch (Exception e) {
            Messages.showException(e);
        }
    }
}

