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
import pixelitor.utils.CompActivationListener;
import pixelitor.utils.Messages;
import pixelitor.utils.Rnd;

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
 * Static methods related to the list of opened compositions
 */
public class OpenComps {
    private static final List<CompositionView> views = new ArrayList<>();
    private static CompositionView activeView;
    private static final List<CompActivationListener> activationListeners
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

    private OpenComps() {
    }

    public static boolean thereAreUnsavedChanges() {
        return views.stream()
                .anyMatch(CompositionView::isDirty);
    }

    public static List<CompositionView> getViews() {
        return views;
    }

    private static void setAnImageAsActiveIfNoneIs() {
        if (!views.isEmpty()) {
            boolean activeFound = false;
            for (CompositionView view : views) {
                if (view == activeView) {
                    activeFound = true;
                    break;
                }
            }

            if (!activeFound) {
                setActiveIC(views.get(0), true);
            }
        }
    }

    public static CompositionView getActiveView() {
        return activeView;
    }

    public static Composition getActiveCompOrNull() {
        if (activeView != null) {
            return activeView.getComp();
        }

        // there is no open image
        return null;
    }

    public static Path getActivePathOrNull() {
        if (activeView != null) {
            return activeView.getComp().getActivePath();
        }

        // there is no open image
        return null;
    }

    public static void setActivePath(Path path) {
        if (activeView == null) {
            throw new IllegalStateException();
        }
        activeView.getComp().setActivePath(path);
    }

    public static Optional<Composition> getActiveComp() {
        if (activeView != null) {
            return Optional.of(activeView.getComp());
        }

        // there is no open image
        return Optional.empty();
    }

    public static Optional<Composition> findCompositionByName(String name) {
        return views.stream()
                .map(CompositionView::getComp)
                .filter(c -> c.getName().equals(name))
                .findFirst();
    }

    public static Layer getActiveLayerOrNull() {
        if (activeView != null) {
            return activeView.getComp()
                    .getActiveLayer();
        }

        return null;
    }

    public static Optional<Layer> getActiveLayer() {
        return getActiveComp().map(Composition::getActiveLayer);
    }

    public static Drawable getActiveDrawableOrNull() {
        if (activeView != null) {
            Composition comp = activeView.getComp();
            return comp.getActiveDrawableOrNull();
        }

        return null;
    }

    public static Drawable getActiveDrawableOrThrow() {
        if (activeView != null) {
            Composition comp = activeView.getComp();
            return comp.getActiveDrawableOrThrow();
        }

        throw new IllegalStateException("no active image");
    }

    public static int getNumOpenImages() {
        return views.size();
    }

    public static BufferedImage getActiveCompositeImage() {
        if (activeView != null) {
            return activeView.getComp()
                    .getCompositeImage();
        }
        return null;
    }

    public static void imageClosed(CompositionView cv) {
        views.remove(cv);
        if (views.isEmpty()) {
            onAllImagesClosed();
        }
        setAnImageAsActiveIfNoneIs();
    }

    public static void setActiveIC(CompositionView cv, boolean activate) {
        if (activate) {
            if (cv == null) {
                throw new IllegalStateException("cannot activate null view");
            }
            ImageArea.activateIC(cv);
        }
        activeView = cv;
    }

    /**
     * Changes the cursor for all images
     */
    public static void setCursorForAll(Cursor cursor) {
        for (CompositionView cv : views) {
            cv.setCursor(cursor);
        }
    }

    public static void addActivationListener(CompActivationListener listener) {
        activationListeners.add(listener);
    }

    public static void removeActivationListener(CompActivationListener listener) {
        activationListeners.remove(listener);
    }

    private static void onAllImagesClosed() {
        setActiveIC(null, false);
        activationListeners.forEach(CompActivationListener::allCompsClosed);
        History.onAllImagesClosed();
        SelectionActions.setEnabled(false, null);

        PixelitorWindow.getInstance()
                .setTitle(Build.getPixelitorWindowFixTitle());
        FramesUI.resetCascadeIndex();
    }

    /**
     * Another image became active
     */
    public static void imageActivated(CompositionView cv) {
        if (cv == activeView) {
            return;
        }

        CompositionView oldCV = activeView;

        Composition comp = cv.getComp();
        setActiveIC(cv, false);
        SelectionActions.setEnabled(comp.hasSelection(), comp);
        cv.activateUI(true);

        for (CompActivationListener listener : activationListeners) {
            listener.compActivated(oldCV, cv);
        }

        Layer layer = comp.getActiveLayer();
        Layers.activeLayerChanged(layer);

        ZoomMenu.zoomChanged(cv.getZoomLevel());

        Canvas.activeCanvasImSizeChanged(comp.getCanvas());
        String title = comp.getName()
                + " - " + Build.getPixelitorWindowFixTitle();
        PixelitorWindow.getInstance().setTitle(title);
    }

    public static void repaintActive() {
        if (activeView != null) {
            activeView.repaint();
        }
    }

    public static void repaintAll() {
        for (CompositionView cv : views) {
            cv.repaint();
        }
    }

    public static void repaintVisible() {
        if (ImageArea.currentModeIs(FRAMES)) {
            repaintAll();
        } else {
            activeView.repaint();
        }
    }

    public static void fitActiveTo(AutoZoom autoZoom) {
        if (activeView != null) {
            activeView.zoomToFit(autoZoom);
        }
    }

    public static boolean isActive(CompositionView cv) {
        return cv == activeView;
    }

    public static void reloadActiveFromFileAsync() {
        // save a reference to the active view, because this will take
        // a while and another view might become active in the meantime
        CompositionView cv = activeView;

        Composition comp = cv.getComp();
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
                .thenAcceptAsync(cv::replaceJustReloadedComp,
                        EventQueue::invokeLater)
                .whenComplete((v, e) -> IOThread.processingFinishedFor(file))
                .exceptionally(Messages::showExceptionOnEDT);
    }

    public static void duplicateActive() {
        assert activeView != null;
        Composition newComp = Composition.createCopy(activeView.getComp(), false);

        addAsNewImage(newComp);
    }

    public static void onActiveIC(Consumer<CompositionView> action) {
        if (activeView != null) {
            action.accept(activeView);
        }
    }

    public static void forAllImages(Consumer<CompositionView> action) {
        for (CompositionView cv : views) {
            action.accept(cv);
        }
    }

    public static void onActiveComp(Consumer<Composition> action) {
        if (activeView != null) {
            Composition comp = activeView.getComp();
            action.accept(comp);
        }
    }

    public static void onActiveSelection(Consumer<Selection> action) {
        if (activeView != null) {
            activeView.getComp()
                    .onSelection(action);
        }
    }

    public static void onActiveLayer(Consumer<Layer> action) {
        if (activeView != null) {
            Layer activeLayer = activeView.getComp()
                    .getActiveLayer();
            action.accept(activeLayer);
        }
    }

    public static void onActiveImageLayer(Consumer<ImageLayer> action) {
        if (activeView != null) {
            ImageLayer activeLayer = (ImageLayer) activeView.getComp()
                    .getActiveLayer();
            action.accept(activeLayer);
        }
    }

    public static void onActiveTextLayer(Consumer<TextLayer> action) {
        if (activeView != null) {
            TextLayer activeLayer = (TextLayer) activeView.getComp()
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
            assert comp.getView() == null : "already has view";

            CompositionView cv = new CompositionView(comp);
            comp.addAllLayersToGUI();
            cv.setCursor(Tools.getCurrent().getStartingCursor());
            views.add(cv);
            MaskViewMode.NORMAL.activate(cv, comp.getActiveLayer(), "image added");
            ImageArea.addNewIC(cv);
            setActiveIC(cv, false);
        } catch (Exception e) {
            Messages.showException(e);
        }
    }

    public static void activateRandomView() {
        CompositionView cv = Rnd.chooseFrom(views);
        if (cv != activeView) {
            setActiveIC(cv, true);
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
        if (activeView == null) {
            throw new AssertionError("no active image");
        }
        ZoomLevel actual = activeView.getZoomLevel();
        if (actual != expected) {
            throw new AssertionError("expected = " + expected +
                    ", found = " + actual);
        }
    }

    private static String getOpenImageNamesAsString() {
        return views.stream()
                .map(CompositionView::getName)
                .collect(joining(", ", "[", "]"));
    }

    private static void warnAndCloseActive() {
        warnAndClose(activeView);
    }

    public static void warnAndClose(CompositionView cv) {
        try {
            Composition comp = cv.getComp();
            if (comp.isDirty()) {
                int answer = Dialogs.showCloseWarningDialog(comp.getName());

                if (answer == YES_OPTION) { // save
                    boolean fileSaved = OpenSave.save(comp, false);
                    if (fileSaved) {
                        cv.close();
                    }
                } else if (answer == NO_OPTION) { // don't save
                    cv.close();
                } else if (answer == CANCEL_OPTION) { // cancel
                    // do nothing
                } else { // dialog closed by pressing X
                    // do nothing
                }
            } else {
                cv.close();
            }
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }

    private static void warnAndCloseAll() {
        warnAndCloseAllIf(cv -> true);
    }

    public static void warnAndCloseAllBut(CompositionView selected) {
        warnAndCloseAllIf(cv -> cv != selected);
    }

    private static void closeUnmodified() {
        warnAndCloseAllIf(cv -> !cv.getComp().isDirty());
    }

    private static void warnAndCloseAllIf(Predicate<CompositionView> condition) {
        // make a copy because items will be removed from the original while iterating
        Iterable<CompositionView> tmpCopy = new ArrayList<>(views);
        for (CompositionView cv : tmpCopy) {
            if (condition.test(cv)) {
                warnAndClose(cv);
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
        } else { // Tabs: check only the current view
            CompositionView cv = getActiveView();
            if (cv.showPixelGridIfEnabled()) {
                cv.repaint();
            } else {
                showPixelGridHelp();
            }
        }
    }

    private static boolean isAnyPixelGridVisibleIfEnabled() {
        for (CompositionView cv : views) {
            if (cv.showPixelGridIfEnabled()) {
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
