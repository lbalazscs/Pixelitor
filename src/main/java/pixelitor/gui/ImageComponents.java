/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Build;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.Layers;
import pixelitor.gui.utils.Dialogs;
import pixelitor.history.History;
import pixelitor.io.IOThread;
import pixelitor.io.OpenSave;
import pixelitor.layers.Drawable;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.MaskViewMode;
import pixelitor.layers.TextLayer;
import pixelitor.menus.MenuAction;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.menus.view.ZoomMenu;
import pixelitor.selection.Selection;
import pixelitor.selection.SelectionActions;
import pixelitor.tools.Tools;
import pixelitor.tools.pen.Path;
import pixelitor.utils.ActiveImageChangeListener;
import pixelitor.utils.Messages;
import pixelitor.utils.RandomUtils;

import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static javax.swing.JOptionPane.CANCEL_OPTION;
import static javax.swing.JOptionPane.NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;
import static pixelitor.gui.ImageArea.Mode.FRAMES;

/**
 * Static methods for maintaining the list of open ImageComponent objects
 */
public class ImageComponents {
    private static final List<ImageComponent> icList = new ArrayList<>();
    private static ImageComponent activeIC;
    private static final List<ActiveImageChangeListener> activeICChangeListeners
            = new ArrayList<>();

    public static final MenuAction CLOSE_ALL_ACTION = new MenuAction("Close All") {
        @Override
        public void onClick() {
            warnAndCloseAll();
        }
    };

    public static final MenuAction CLOSE_ACTIVE_ACTION = new MenuAction("Close") {
        @Override
        public void onClick() {
            warnAndCloseActive();
        }
    };

    public static final MenuAction CLOSE_UNMODIFIED_ACTION = new MenuAction("Close Unmodified") {
        @Override
        public void onClick() {
            closeUnmodified();
        }
    };

    private ImageComponents() {
    }

    public static boolean thereAreUnsavedChanges() {
        return icList.stream()
                .anyMatch(ImageComponent::isDirty);
    }

    public static List<ImageComponent> getICList() {
        return icList;
    }

    private static void setAnImageAsActiveIfNoneIs() {
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

    public static Composition getActiveCompOrNull() {
        if (activeIC != null) {
            return activeIC.getComp();
        }

        // there is no open image
        return null;
    }

    public static Path getActivePathOrNull() {
        if (activeIC != null) {
            return activeIC.getComp().getActivePath();
        }

        // there is no open image
        return null;
    }

    public static void setActivePath(Path path) {
        if (activeIC == null) {
            throw new IllegalStateException();
        }
        activeIC.getComp().setActivePath(path);
    }

    public static Optional<Composition> getActiveComp() {
        if (activeIC != null) {
            return Optional.of(activeIC.getComp());
        }

        // there is no open image
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
            return activeIC.getComp()
                    .getActiveLayer();
        }

        return null;
    }

    public static Optional<Layer> getActiveLayer() {
        return getActiveComp().map(Composition::getActiveLayer);
    }

    public static Drawable getActiveDrawableOrNull() {
        if (activeIC != null) {
            Composition comp = activeIC.getComp();
            return comp.getActiveDrawableOrNull();
        }

        return null;
    }

    public static Drawable getActiveDrawableOrThrow() {
        if (activeIC != null) {
            Composition comp = activeIC.getComp();
            return comp.getActiveDrawableOrThrow();
        }

        throw new IllegalStateException("no active image");
    }

    public static int getNumOpenImages() {
        return icList.size();
    }

    public static BufferedImage getActiveCompositeImage() {
        if (activeIC != null) {
            return activeIC.getComp()
                    .getCompositeImage();
        }
        return null;
    }

    public static void imageClosed(ImageComponent ic) {
        icList.remove(ic);
        if (icList.isEmpty()) {
            onAllImagesClosed();
        }
        setAnImageAsActiveIfNoneIs();
    }

    public static void setActiveIC(ImageComponent ic, boolean activate) {
        if (activate) {
            if (ic == null) {
                throw new IllegalStateException("cannot activate null ic");
            }
            ImageArea.activateIC(ic);
        }
        activeIC = ic;
//        System.out.println("ImageComponents::setActiveIC: new active ic is "
//                + Ansi.yellow(activeIC == null ? "null" : activeIC.getName())
//                + ", set on " + Thread.currentThread().getName());
    }

    /**
     * Changes the cursor for all images
     */
    public static void setCursorForAll(Cursor cursor) {
        for (ImageComponent ic : icList) {
            ic.setCursor(cursor);
        }
    }

    public static void addActiveImageChangeListener(ActiveImageChangeListener listener) {
        activeICChangeListeners.add(listener);
    }

    public static void removeActiveImageChangeListener(ActiveImageChangeListener listener) {
        activeICChangeListeners.remove(listener);
    }

    private static void onAllImagesClosed() {
        setActiveIC(null, false);
        activeICChangeListeners.forEach(ActiveImageChangeListener::noOpenImageAnymore);
        History.onAllImagesClosed();
        SelectionActions.setEnabled(false, null);

        PixelitorWindow.getInstance()
                .setTitle(Build.getPixelitorWindowFixTitle());
        FramesUI.resetCascadeIndex();
    }

    /**
     * Another image became active
     */
    public static void imageActivated(ImageComponent ic) {
        if (ic == activeIC) {
            return;
        }

        ImageComponent oldIC = activeIC;

        Composition comp = ic.getComp();
        setActiveIC(ic, false);
        SelectionActions.setEnabled(comp.hasSelection(), comp);
        ic.activateUI(true);

        for (ActiveImageChangeListener listener : activeICChangeListeners) {
            listener.activeImageChanged(oldIC, ic);
        }

        Layer layer = comp.getActiveLayer();
        Layers.activeLayerChanged(layer);

        ZoomMenu.zoomChanged(ic.getZoomLevel());

        Canvas.activeCanvasImSizeChanged(comp.getCanvas());
        String title = comp.getName()
                + " - " + Build.getPixelitorWindowFixTitle();
        PixelitorWindow.getInstance().setTitle(title);
    }

    public static void repaintActive() {
        if (activeIC != null) {
            activeIC.repaint();
        }
    }

    public static void repaintAll() {
        for (ImageComponent ic : icList) {
            ic.repaint();
        }
    }

    public static void repaintVisible() {
        if (ImageArea.currentModeIs(FRAMES)) {
            repaintAll();
        } else {
            activeIC.repaint();
        }
    }

    public static void fitActiveTo(AutoZoom autoZoom) {
        if (activeIC != null) {
            activeIC.zoomToFit(autoZoom);
        }
    }

    public static boolean isActive(ImageComponent ic) {
        return ic == activeIC;
    }

    public static void reloadActiveFromFileAsync() {
        // save a reference to the active image, because this will take
        // a while and another image might become active in the meantime
        ImageComponent ic = activeIC;

        Composition comp = ic.getComp();
        File file = comp.getFile();
        if (file == null) {
            String msg = format(
                    "The image '%s' cannot be reloaded because it was not yet saved.",
                    comp.getName());
            Messages.showError("No file", msg);
            return;
        }

        if (!file.exists()) {
            String msg = format(
                    "The image '%s' cannot be reloaded because the file\n" +
                            "%s\n" +
                            "does not exist anymore.",
                    comp.getName(), file.getAbsolutePath());
            Messages.showError("No file found", msg);
            return;
        }

        // prevents starting a new reload on the EDT while an asynchronous
        // reload is already scheduled or running on the IO thread
        if (IOThread.isProcessing(file)) {
            return;
        }

        OpenSave.loadCompFromFileAsync(file)
                .thenAcceptAsync(ic::replaceJustReloadedComp,
                        EventQueue::invokeLater)
                .whenComplete((v, e) -> IOThread.processingFinishedFor(file))
                .exceptionally(Messages::showExceptionOnEDT);
    }

    public static void duplicateActive() {
        assert activeIC != null;
        Composition newComp = Composition.createCopy(activeIC.getComp(), false);

        addAsNewImage(newComp);
    }

    public static void onActiveIC(Consumer<ImageComponent> action) {
        if (activeIC != null) {
            action.accept(activeIC);
        }
    }

    public static void forAllImages(Consumer<ImageComponent> action) {
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
            activeIC.getComp()
                    .onSelection(action);
        }
    }

    public static void onActiveLayer(Consumer<Layer> action) {
        if (activeIC != null) {
            Layer activeLayer = activeIC.getComp()
                    .getActiveLayer();
            action.accept(activeLayer);
        }
    }

    public static void onActiveImageLayer(Consumer<ImageLayer> action) {
        if (activeIC != null) {
            ImageLayer activeLayer = (ImageLayer) activeIC.getComp()
                    .getActiveLayer();
            action.accept(activeLayer);
        }
    }

    public static void onActiveTextLayer(Consumer<TextLayer> action) {
        if (activeIC != null) {
            TextLayer activeLayer = (TextLayer) activeIC.getComp()
                    .getActiveLayer();
            action.accept(activeLayer);
        }
    }

    public static void onActiveDrawable(Consumer<Drawable> action) {
        Drawable dr = getActiveDrawableOrNull();
        if (dr != null) {
            action.accept(dr);
        }
    }

    public static void addAsNewImage(Composition comp) {
        try {
            assert comp.getIC() == null : "already has ic";

            ImageComponent ic = new ImageComponent(comp);
            comp.addAllLayersToGUI();
            ic.setCursor(Tools.getCurrent().getStartingCursor());
            icList.add(ic);
            MaskViewMode.NORMAL.activate(ic, comp.getActiveLayer(), "image added");
            ImageArea.addNewIC(ic);
            setActiveIC(ic, false);
        } catch (Exception e) {
            Messages.showException(e);
        }
    }

    public static void activateRandomIC() {
        ImageComponent ic = RandomUtils.chooseFrom(icList);
        if (ic != activeIC) {
            setActiveIC(ic, true);
        }
    }

    public static void assertNumOpenImagesIs(int expected) {
        int numOpenImages = getNumOpenImages();
        if (numOpenImages == expected) {
            return;
        }

        throw new AssertionError(format(
                "Expected %d images, found %d (%s)",
                expected, numOpenImages, getOpenImageNamesAsString()));
    }

    public static void assertNumOpenImagesIsAtLeast(int expected) {
        int numOpenImages = getNumOpenImages();
        if (numOpenImages >= expected) {
            return;
        }
        throw new AssertionError(format(
                "Expected at least %d images, found %d (%s)",
                expected, numOpenImages, getOpenImageNamesAsString()));
    }

    public static void assertZoomOfActiveIs(ZoomLevel expected) {
        if (activeIC == null) {
            throw new AssertionError("no active image");
        }
        ZoomLevel actual = activeIC.getZoomLevel();
        if (actual != expected) {
            throw new AssertionError("expected = " + expected +
                    ", found = " + actual);
        }
    }

    private static String getOpenImageNamesAsString() {
        return icList.stream()
                .map(ImageComponent::getName)
                .collect(joining(", ", "[", "]"));
    }

    private static void warnAndCloseActive() {
        warnAndClose(activeIC);
    }

    public static void warnAndClose(ImageComponent ic) {
        try {
            Composition comp = ic.getComp();
            if (comp.isDirty()) {
                int answer = Dialogs.showCloseWarningDialog(comp.getName());

                if (answer == YES_OPTION) { // save
                    boolean fileSaved = OpenSave.save(comp, false);
                    if (fileSaved) {
                        ic.close();
                    }
                } else if (answer == NO_OPTION) { // don't save
                    ic.close();
                } else if (answer == CANCEL_OPTION) { // cancel
                    // do nothing
                } else { // dialog closed by pressing X
                    // do nothing
                }
            } else {
                ic.close();
            }
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }

    private static void warnAndCloseAll() {
        warnAndCloseAllIf(ic -> true);
    }

    public static void warnAndCloseAllBut(ImageComponent selected) {
        warnAndCloseAllIf(ic -> ic != selected);
    }

    private static void closeUnmodified() {
        warnAndCloseAllIf(ic -> !ic.getComp().isDirty());
    }

    private static void warnAndCloseAllIf(Predicate<ImageComponent> condition) {
        // make a copy because items will be removed from the original while iterating
        Iterable<ImageComponent> tmpCopy = new ArrayList<>(icList);
        for (ImageComponent ic : tmpCopy) {
            if (condition.test(ic)) {
                warnAndClose(ic);
            }
        }
    }

    public static void pixelGridEnabled() {
        if (ImageArea.currentModeIs(FRAMES)) {
            if (isAnyPixelGridVisibleIfEnabled()) {
                repaintAll();
            } else {
                showPixelGridHelp();
            }
        } else { // Tabs: check only the current ic
            ImageComponent ic = getActiveIC();
            if (ic.showPixelGridIfEnabled()) {
                ic.repaint();
            } else {
                showPixelGridHelp();
            }
        }
    }

    private static boolean isAnyPixelGridVisibleIfEnabled() {
        for (ImageComponent ic : icList) {
            if (ic.showPixelGridIfEnabled()) {
                return true;
            }
        }
        return false;
    }

    private static void showPixelGridHelp() {
        Messages.showInfo("Pixel Grid",
                "The pixel grid consists of lines between the pixels,\n" +
                        "and is shown only if the zoom is at least 1600%\n" +
                        "and there is no selection.");
    }
}
