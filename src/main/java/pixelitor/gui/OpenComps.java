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
import java.util.function.Function;
import java.util.function.Predicate;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.swing.JOptionPane.CANCEL_OPTION;
import static javax.swing.JOptionPane.CLOSED_OPTION;
import static javax.swing.JOptionPane.NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;
import static pixelitor.gui.ImageArea.Mode.FRAMES;

/**
 * Static methods related to the list of opened compositions
 */
public class OpenComps {
    private static final List<View> views = new ArrayList<>();
    private static View activeView;
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

    public static List<Composition> getUnsavedComps() {
        return views.stream()
                .map(View::getComp)
                .filter(Composition::isDirty)
                .collect(toList());
    }

    public static List<View> getViews() {
        return views;
    }

    public static View getActiveView() {
        return activeView;
    }

    public static boolean isActive(View view) {
        return view == activeView;
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

    public static Selection getActiveSelection() {
        if (activeView != null) {
            return activeView.getComp().getSelection();
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
            .map(View::getComp)
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

    public static void imageClosed(View view) {
        views.remove(view);
        if (views.isEmpty()) {
            onAllImagesClosed();
        }
        activateAViewIfNoneIs();
    }

    private static void activateAViewIfNoneIs() {
        if (!views.isEmpty()) {
            boolean activeFound = false;
            for (View view : views) {
                if (view == activeView) {
                    activeFound = true;
                    break;
                }
            }

            if (!activeFound) {
                setActiveView(views.get(0), true);
            }
        }
    }

    public static void setActiveView(View view, boolean activate) {
        if (activate) {
            if (view == null) {
                throw new IllegalStateException("cannot activate null view");
            }
            ImageArea.activateView(view);
        }
        activeView = view;
    }

    /**
     * Changes the cursor for all images
     */
    public static void setCursorForAll(Cursor cursor) {
        for (View view : views) {
            view.setCursor(cursor);
        }
    }

    public static void addActivationListener(CompActivationListener listener) {
        activationListeners.add(listener);
    }

    public static void removeActivationListener(CompActivationListener listener) {
        activationListeners.remove(listener);
    }

    private static void onAllImagesClosed() {
        setActiveView(null, false);
        activationListeners.forEach(CompActivationListener::allCompsClosed);
        History.onAllImagesClosed();
        SelectionActions.setEnabled(false, null);

        PixelitorWindow.getInstance().updateTitle(null);
        FramesUI.resetCascadeIndex();
    }

    /**
     * Another view became active
     */
    public static void viewActivated(View view) {
        if (view == activeView) {
            return;
        }

        View oldView = activeView;

        Composition comp = view.getComp();
        setActiveView(view, false);
        SelectionActions.setEnabled(comp.hasSelection(), comp);
        view.activateUI(true);

        for (CompActivationListener listener : activationListeners) {
            listener.compActivated(oldView, view);
        }

        Layer layer = comp.getActiveLayer();
        Layers.activeLayerChanged(layer);

        ZoomMenu.zoomChanged(view.getZoomLevel());

        Canvas.activeCanvasImSizeChanged(comp.getCanvas());
        PixelitorWindow.getInstance().updateTitle(comp);
    }

    public static void repaintActive() {
        if (activeView != null) {
            activeView.repaint();
        }
    }

    public static void repaintAll() {
        for (View view : views) {
            view.repaint();
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

    public static void reloadActiveFromFileAsync() {
        // save a reference to the active view, because this will take
        // a while and another view might become active in the meantime
        View view = activeView;

        Composition comp = view.getComp();
        File file = comp.getFile();
        if (file == null) {
            String msg = format(
                "The image '%s' cannot be reloaded because it was not yet saved.",
                comp.getName());
            Messages.showError("No file", msg);
            return;
        }

        String path = file.getAbsolutePath();
        if (!file.exists()) {
            String msg = format(
                "<html>The image '%s' cannot be reloaded because the file<br>" +
                    "<b>%s</b><br>" +
                    "does not exist anymore.",
                    comp.getName(), path);
            Messages.showError("File not found", msg);
            return;
        }

        // prevents starting a new reload on the EDT while an asynchronous
        // reload is already scheduled or running on the IO thread
        if (IOThread.isProcessing(path)) {
            return;
        }
        IOThread.markReadProcessing(path);

        OpenSave.loadCompFromFileAsync(file)
            .thenAcceptAsync(view::replaceJustReloadedComp,
                EventQueue::invokeLater)
                .whenComplete((v, e) -> IOThread.readingFinishedFor(path))
            .exceptionally(Messages::showExceptionOnEDT);
    }

    public static void duplicateActive() {
        assert activeView != null;

        Composition activeComp = activeView.getComp();
        Composition newComp = activeComp.createCopy(false, true);

        addAsNewImage(newComp);
    }

    public static void onActiveView(Consumer<View> action) {
        if (activeView != null) {
            action.accept(activeView);
        }
    }

    public static void forEachView(Consumer<View> action) {
        for (View view : views) {
            action.accept(view);
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
            activeView.getComp().onSelection(action);
        }
    }

    public static void onActiveLayer(Consumer<Layer> action) {
        if (activeView != null) {
            Layer activeLayer = activeView.getComp().getActiveLayer();
            action.accept(activeLayer);
        }
    }

    public static void onActiveImageLayer(Consumer<ImageLayer> action) {
        if (activeView != null) {
            ImageLayer activeLayer = (ImageLayer) activeView.getComp().getActiveLayer();
            action.accept(activeLayer);
        }
    }

    public static void onActiveTextLayer(Consumer<TextLayer> action) {
        if (activeView != null) {
            TextLayer activeLayer = (TextLayer) activeView.getComp().getActiveLayer();
            action.accept(activeLayer);
        }
    }

    public static <T> T fromActiveTextLayer(Function<TextLayer, T> function) {
        if (activeView != null) {
            TextLayer activeLayer = (TextLayer) activeView.getComp().getActiveLayer();
            return function.apply(activeLayer);
        }
        return null;
    }

    public static void onActiveDrawable(Consumer<Drawable> action) {
        Drawable dr = getActiveDrawableOrNull();
        if (dr != null) {
            action.accept(dr);
        }
    }

    public static void addAsNewImage(Composition comp) {
        try {
            assert comp.getView() == null : "already has a view";

            View view = new View(comp);
            comp.addAllLayersToGUI();
            view.setCursor(Tools.getCurrent().getStartingCursor());
            views.add(view);
            MaskViewMode.NORMAL.activate(view, comp.getActiveLayer(), "image added");
            ImageArea.addNewView(view);
            setActiveView(view, false);
        } catch (Exception e) {
            Messages.showException(e);
        }
    }

    public static void activateRandomView() {
        View view = Rnd.chooseFrom(views);
        if (view != activeView) {
            setActiveView(view, true);
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
            .map(View::getName)
            .collect(joining(", ", "[", "]"));
    }

    private static void warnAndCloseActive() {
        warnAndClose(activeView);
    }

    public static void warnAndClose(View view) {
        try {
            Composition comp = view.getComp();
            if (comp.isDirty()) {
                int answer = Dialogs.showCloseWarningDialog(comp.getName());

                if (answer == YES_OPTION) { // "Save"
                    boolean fileSaved = OpenSave.save(comp, false);
                    if (fileSaved) {
                        view.close();
                    }
                } else if (answer == NO_OPTION) { // "Don't Save"
                    view.close();
                } else if (answer == CANCEL_OPTION) {
                    // do nothing
                } else if (answer == CLOSED_OPTION) { // dialog closed by pressing X
                    // do nothing
                } else {
                    throw new IllegalStateException("answer = " + answer);
                }
            } else {
                view.close();
            }
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }

    private static void warnAndCloseAll() {
        warnAndCloseAllIf(view -> true);
    }

    public static void warnAndCloseAllBut(View selected) {
        warnAndCloseAllIf(view -> view != selected);
    }

    private static void closeUnmodified() {
        warnAndCloseAllIf(view -> !view.getComp().isDirty());
    }

    private static void warnAndCloseAllIf(Predicate<View> condition) {
        // make a copy because items will be removed from the original while iterating
        Iterable<View> tmpCopy = new ArrayList<>(views);
        for (View view : tmpCopy) {
            if (condition.test(view)) {
                warnAndClose(view);
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
            View view = getActiveView();
            if (view != null && view.showPixelGridIfEnabled()) {
                view.repaint();
            } else {
                showPixelGridHelp();
            }
        }
    }

    private static boolean isAnyPixelGridVisibleIfEnabled() {
        for (View view : views) {
            if (view.showPixelGridIfEnabled()) {
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
