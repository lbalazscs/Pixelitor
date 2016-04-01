/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import pixelitor.AppLogic;
import pixelitor.Build;
import pixelitor.Composition;
import pixelitor.filters.comp.Crop;
import pixelitor.history.AddToHistory;
import pixelitor.history.History;
import pixelitor.history.PixelitorEdit;
import pixelitor.io.OpenSaveManager;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.MaskViewMode;
import pixelitor.layers.TextLayer;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.menus.view.ZoomMenu;
import pixelitor.selection.Selection;
import pixelitor.selection.SelectionActions;
import pixelitor.tools.Tools;
import pixelitor.utils.ImageSwitchListener;
import pixelitor.utils.Messages;

import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Static methods for maintaining the list of open ImageComponent objects
 */
public class ImageComponents {
    private static final List<ImageComponent> icList = new ArrayList<>();
    private static ImageComponent activeIC;
    private static final Collection<ImageSwitchListener> imageSwitchListeners = new ArrayList<>();

    private ImageComponents() {
    }

    public static void addIC(ImageComponent ic) {
        icList.add(ic);
    }

    public static boolean thereAreUnsavedChanges() {
        for (ImageComponent ic : icList) {
            if (ic.getComp().isDirty()) {
                return true;
            }
        }
        return false;
    }

    public static List<ImageComponent> getICList() {
        return icList;
    }

    private static void setNewImageAsActiveIfNecessary() {
        if (!icList.isEmpty()) {
            boolean activeFound = false;

            for (ImageComponent ic : icList) {
                if (ic == activeIC) {
                    activeFound = true;
                    break;
                }
            }
            if (!activeFound) {
                setActiveIC(icList.get(0), true);
            }
        }
    }

    public static ImageComponent getActiveIC() {
        return activeIC;
    }

    public static boolean hasActiveImage() {
        return activeIC != null && !icList.isEmpty();
    }

    public static Composition getActiveCompOrNull() {
        if (activeIC != null) {
            return activeIC.getComp();
        }

        return null;
    }

    public static Optional<Composition> getActiveComp() {
        if (activeIC != null) {
            return Optional.of(activeIC.getComp());
        }

        return Optional.empty();
    }

    public static Optional<Composition> findCompositionByName(String name) {
        return icList.stream()
                .map(ImageComponent::getComp)
                .filter(c -> c.getName().equals(name))
                .findFirst();
    }

    public static Layer getActiveLayerOrNull() {
        if (activeIC != null) {
            return activeIC.getComp().getActiveLayer();
        }

        return null;
    }

    public static Optional<Layer> getActiveLayer() {
        return getActiveComp().map(Composition::getActiveLayer);
    }

    public static ImageLayer getActiveImageLayerOrMaskOrNull() {
        if (activeIC != null) {
            Composition comp = activeIC.getComp();
            return comp.getActiveMaskOrImageLayerOrNull();
        }

        return null;
    }

    public static Optional<ImageLayer> getActiveImageLayerOrMask() {
        return getActiveComp().flatMap(Composition::getActiveMaskOrImageLayerOpt);
    }

    public static int getNrOfOpenImages() {
        return icList.size();
    }

    public static BufferedImage getActiveCompositeImageOrNull() {
        if (activeIC != null) {
            return activeIC.getComp().getCompositeImage();
        }
        return null;
    }

    /**
     * Crops the active image based on the crop tool
     */
    public static void toolCropActiveImage(boolean allowGrowing) {
        try {
            onActiveComp(comp -> {
                Rectangle2D cropRect = Tools.CROP.getCropRect(comp.getIC());
                new Crop(cropRect, false, allowGrowing).process(comp);
            });
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }

    /**
     * Crops the active image based on the selection bounds
     */
    public static void selectionCropActiveImage() {
        try {
            Composition comp = getActiveCompOrNull();
            if (comp != null) {
                comp.onSelection(selection -> {
                    Rectangle selectionBounds = selection.getShapeBounds();
                    new Crop(selectionBounds, true, true).process(comp);
                });
            }
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }

    public static void imageClosed(ImageComponent ic) {
        icList.remove(ic);
        if (icList.isEmpty()) {
            onAllImagesClosed();
        }
        setNewImageAsActiveIfNecessary();
    }

    public static void setActiveIC(ImageComponent ic, boolean activate) {
        activeIC = ic;
        if (activate) {
            if (ic == null) {
                throw new IllegalStateException("cannot activate null imageComponent");
            }
            // activate is always false in unit tests
            InternalImageFrame internalFrame = ic.getInternalFrame();
            Desktop.INSTANCE.activateInternalImageFrame(internalFrame);
            ic.onActivation();
        }
    }

    /**
     * When a new tool is activated, the cursor has to be changed for each image
     */
    public static void setCursorForAll(Cursor cursor) {
        for (ImageComponent ic : icList) {
            ic.setCursor(cursor);
        }
    }

    public static void addImageSwitchListener(ImageSwitchListener listener) {
        imageSwitchListeners.add(listener);
    }

    public static void removeImageSwitchListener(ImageSwitchListener listener) {
        imageSwitchListeners.remove(listener);
    }

    private static void onAllImagesClosed() {
        setActiveIC(null, false);
        imageSwitchListeners.forEach(ImageSwitchListener::noOpenImageAnymore);
        History.onAllImagesClosed();
        SelectionActions.setEnabled(false, null);

        PixelitorWindow.getInstance().setTitle(Build.getPixelitorWindowFixTitle());
    }

    /**
     * Another image became active
     */
    public static void activeImageHasChanged(ImageComponent ic) {
        ImageComponent oldIC = activeIC;

        setActiveIC(ic, false);
        for (ImageSwitchListener listener : imageSwitchListeners) {
            listener.activeImageHasChanged(oldIC, ic);
        }

        Composition newActiveComp = ic.getComp();
        Layer layer = newActiveComp.getActiveLayer();
        AppLogic.activeLayerChanged(layer);

        SelectionActions.setEnabled(newActiveComp.hasSelection(), newActiveComp);
        ZoomMenu.zoomChanged(ic.getZoomLevel());

        AppLogic.activeCompSizeChanged(newActiveComp);
        PixelitorWindow.getInstance().setTitle(ic.getComp().getName() + " - " + Build.getPixelitorWindowFixTitle());
    }

    public static void newImageOpened(Composition comp) {
//        numFramesOpen++;
        imageSwitchListeners.forEach((imageSwitchListener) -> imageSwitchListener.newImageOpened(comp));
    }

    public static void repaintActive() {
        if (activeIC != null) {
            activeIC.repaint();
        }
    }

    public static void repaintAll() {
        //noinspection Convert2streamapi
        for (ImageComponent ic : icList) {
            ic.repaint();
        }
    }

    public static void fitActiveToScreen() {
        if (activeIC != null) {
            activeIC.zoomToFitScreen();
        }
    }

    public static void fitActiveToActualPixels() {
        if (activeIC != null) {
            activeIC.setZoomAtCenter(ZoomLevel.Z100);
        }
    }

    public static boolean isActive(ImageComponent ic) {
        return ic == activeIC;
    }

    public static boolean isActiveLayerImageLayer() {
        Layer activeLayer = activeIC.getComp().getActiveLayer();
        return activeLayer instanceof ImageLayer;
    }

    public static void reloadActiveFromFile() {
        Composition comp = activeIC.getComp();
        File file = comp.getFile();
        if (file == null) {
            String msg = String.format("The image '%s' cannot be reloaded because it was not yet saved.", comp.getName());
            Messages.showError("No file", msg);
            return;
        }
        if (!file.exists()) {
            String msg = String.format("The image '%s' cannot be reloaded because the file\n" +
                            "%s\n" +
                            "does not exist anymore.",
                    comp.getName(), file.getAbsolutePath());
            Messages.showError("No file found", msg);
            return;
        }

        Composition newComp = OpenSaveManager.createCompositionFromFile(file);

        PixelitorEdit edit = activeIC.replaceComp(newComp, AddToHistory.YES, MaskViewMode.NORMAL);

        // needs to be called before addEdit because of the consistency checks
        newImageOpened(newComp);

        assert edit != null;
        History.addEdit(edit);

        String msg = String.format("The image '%s' was reloaded from the file %s.",
                comp.getName(), file.getAbsolutePath());
        Messages.showStatusMessage(msg);
    }

    public static void duplicateActive() {
        assert activeIC != null;
        Composition newComp = Composition.createCopy(activeIC.getComp(), false);

        AppLogic.addComposition(newComp);
    }

    public static void onActiveIC(Consumer<ImageComponent> action) {
        if (activeIC != null) {
            action.accept(activeIC);
        }
    }

    public static void onActiveICAndComp(BiConsumer<ImageComponent, Composition> action) {
        if (activeIC != null) {
            Composition comp = activeIC.getComp();
            action.accept(activeIC, comp);
        }
    }

    public static <T> T fromActiveIC(Function<ImageComponent, T> function) {
        return function.apply(activeIC);
    }

    public static void onAllImages(Consumer<ImageComponent> action) {
        //noinspection Convert2streamapi
        for (ImageComponent ic : icList) {
            action.accept(ic);
        }
    }

    public static void onActiveComp(Consumer<Composition> action) {
        if (activeIC != null) {
            Composition comp = activeIC.getComp();
            action.accept(comp);
        }
    }

    public static void onActiveSelection(Consumer<Selection> action) {
        if (activeIC != null) {
            activeIC.getComp().onSelection(action);
        }
    }

    public static void onActiveLayer(Consumer<Layer> action) {
        if (activeIC != null) {
            Layer activeLayer = activeIC.getComp().getActiveLayer();
            action.accept(activeLayer);
        }
    }

    public static void onActiveImageLayer(Consumer<ImageLayer> action) {
        if (activeIC != null) {
            ImageLayer activeLayer = (ImageLayer) activeIC.getComp().getActiveLayer();
            action.accept(activeLayer);
        }
    }

    public static void onActiveTextLayer(Consumer<TextLayer> action) {
        if (activeIC != null) {
            TextLayer activeLayer = (TextLayer) activeIC.getComp().getActiveLayer();
            action.accept(activeLayer);
        }
    }
}
