/*
 * Copyright 2013-2013 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor;

import pixelitor.filters.comp.CompositionUtils;
import pixelitor.filters.gui.ResizePanel;
import pixelitor.history.History;
import pixelitor.layers.Layer;
import pixelitor.menus.SelectionActions;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.menus.view.ZoomMenu;
import pixelitor.selection.Selection;
import pixelitor.tools.Tools;
import pixelitor.utils.Dialogs;
import pixelitor.utils.ImageSwitchListener;

import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Static methods for maintaining the list of open image components
 */
public class ImageComponents {
    private static final List<ImageComponent> imageComponents = new ArrayList<>();
    private static ImageComponent activeImageComponent;
    private static final Collection<ImageSwitchListener> imageSwitchListeners = new ArrayList<>();

    private ImageComponents() {
    }

    public static void addImageComponent(ImageComponent imageComponent) {
        imageComponents.add(imageComponent);
    }

    public static boolean thereAreUnsavedChanges() {
        for (ImageComponent p : imageComponents) {
            if (p.getComp().isDirty()) {
                return true;
            }
        }
        return false;
    }

    public static List<ImageComponent> getImageComponents() {
        return imageComponents;
    }

    private static void setNewImageAsActiveIfNecessary() {
        if (!imageComponents.isEmpty()) {
            boolean activeFound = false;

            for (ImageComponent component : imageComponents) {
                if (component == activeImageComponent) {
                    activeFound = true;
                    break;
                }
            }
            if (!activeFound) {
                setActiveImageComponent(imageComponents.get(0), true);
            }
        }
    }

    public static ImageComponent getActiveImageComponent() {
        return activeImageComponent;
    }

    public static Composition getActiveComp() {
        if (activeImageComponent != null) {
            return activeImageComponent.getComp();
        }

        return null;
    }

    public static Layer getActiveLayer() {
        return getActiveComp().getActiveLayer();
    }

    public static int getNrOfOpenImages() {
        return imageComponents.size();
    }

    public static BufferedImage getActiveCompositeImage() {
        Composition comp = getActiveComp();
        if (comp != null) {
            return comp.getCompositeImage();
        }

        return null;
    }

    /**
     * Crops tha active image based on the crop tool
     * @param selected
     */
    public static void toolCropActiveImage(boolean allowGrowing) {
        try {
            Composition comp = getActiveComp();
            if (comp == null) {
                return;
            }
            Rectangle cropRectangle = Tools.CROP.getCropRectangle(comp.getIC());
            CompositionUtils.cropImage(comp, cropRectangle, false, allowGrowing);
        } catch (Exception ex) {
            Dialogs.showExceptionDialog(ex);
        }
    }

    /**
     * Crops tha active image based on the selection bounds
     */
    public static void selectionCropActiveImage() {
        try {
            Composition comp = getActiveComp();
            if (comp == null) {
                return;
            }
            Selection selection = comp.getSelection();
            if (selection != null) {
                Rectangle selectionBounds = selection.getShapeBounds();
                CompositionUtils.cropImage(comp, selectionBounds, true, true);
            }
        } catch (Exception ex) {
            Dialogs.showExceptionDialog(ex);
        }
    }


    public static void resizeActiveImage() {
        try {
            Composition comp = getActiveComp();
            ResizePanel.showInDialog(comp);
        } catch (Exception ex) {
            Dialogs.showExceptionDialog(ex);
        }
    }

    public static void imageClosed(ImageComponent imageComponent) {
        imageComponents.remove(imageComponent);
//        numFramesOpen--;
        if (imageComponents.isEmpty()) {
            allImagesAreClosed();
        }
        setNewImageAsActiveIfNecessary();
    }

    public static void setActiveImageComponent(ImageComponent newActiveComponent, boolean activate) {
        activeImageComponent = newActiveComponent;
        if (activate) {
            if (newActiveComponent == null) {
                throw new IllegalStateException("cannot activate null imageComponent");
            }
            InternalImageFrame internalFrame = activeImageComponent.getInternalFrame();
            PixelitorWindow.getInstance().activateInternalImageFrame(internalFrame);
            newActiveComponent.onActivation();
        }
    }

    /**
     * When a new tool is activated, the cursor has to be changed for each image
     */
    public static void setToolCursor(Cursor cursor) {
        for (ImageComponent ic : imageComponents) {
            ic.setCursor(cursor);
        }
    }

    public static void addImageSwitchListener(ImageSwitchListener listener) {
        imageSwitchListeners.add(listener);
    }

    public static void allImagesAreClosed() {
        setActiveImageComponent(null, false);
        for (ImageSwitchListener listener : imageSwitchListeners) {
            listener.noOpenImageAnymore();
        }
        History.allImagesAreClosed();
        SelectionActions.setEnabled(false, null);

        PixelitorWindow.getInstance().setTitle(Build.getPixelitorWindowFixTitle());
    }

    /**
     * Another image became active
     */
    public static void activeImageHasChanged(ImageComponent ic) {
        ImageComponent oldIC = activeImageComponent;
        setActiveImageComponent(ic, false);
        for (ImageSwitchListener listener : imageSwitchListeners) {
            listener.activeImageHasChanged(oldIC, ic);
        }

        Composition newActiveComp = ic.getComp();
        Layer layer = newActiveComp.getActiveLayer();
        AppLogic.activeLayerChanged(layer);

        SelectionActions.setEnabled(newActiveComp.hasSelection(), newActiveComp);
        ZoomMenu.zoomChanged(ic.getZoomLevel());

        AppLogic.activeCompositionDimensionsChanged(newActiveComp);
        PixelitorWindow.getInstance().setTitle(ic.getComp().getName() + " - " + Build.getPixelitorWindowFixTitle());
    }

    public static void newImageOpened() {
//        numFramesOpen++;
        for (ImageSwitchListener listener : imageSwitchListeners) {
            listener.newImageOpened();
        }
    }

    public static void repaintActive() {
        if (activeImageComponent != null) {
            activeImageComponent.repaint();
        }
    }

    public static void fitActiveToScreen() {
        if (activeImageComponent != null) {
            activeImageComponent.setupFitScreenZoomSize();
        }
    }

    public static void fitActiveToActualPixels() {
        if (activeImageComponent != null) {
            activeImageComponent.setZoom(ZoomLevel.Z100, false);
        }
    }

    /**
     * Called by keyboard shortcuts via the menu
     */
    public static void increaseZoomForActiveIC() {
        ZoomLevel currentZoom = activeImageComponent.getZoomLevel();
        ZoomLevel newZoomLevel = currentZoom.zoomIn();
        activeImageComponent.setZoom(newZoomLevel, false);
    }

    /**
     * Called by keyboard shortcuts via the menu
     */
    public static void decreaseZoomForActiveIC() {
        ZoomLevel currentZoom = activeImageComponent.getZoomLevel();
        ZoomLevel newZoomLevel = currentZoom.zoomOut();
        activeImageComponent.setZoom(newZoomLevel, false);
    }
}
