/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.colors.FgBgColors;
import pixelitor.gui.*;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.OpenViewEnabledAction;
import pixelitor.history.History;
import pixelitor.io.IO;
import pixelitor.layers.*;
import pixelitor.menus.file.RecentFilesMenu;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.selection.Selection;
import pixelitor.selection.SelectionActions;
import pixelitor.tools.Tools;
import pixelitor.tools.pen.Path;
import pixelitor.utils.Messages;
import pixelitor.utils.Rnd;
import pixelitor.utils.ViewActivationListener;
import pixelitor.utils.VisibleForTesting;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.Cursor;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.swing.JOptionPane.*;
import static pixelitor.gui.ImageArea.Mode.FRAMES;
import static pixelitor.utils.Texts.i18n;

/**
 * Static methods related to the list of open views.
 */
public class Views {
    private static final List<View> views = new ArrayList<>();
    private static View activeView;
    private static final List<ViewActivationListener> activationListeners
        = new ArrayList<>();

    public static final Action CLOSE_ALL_ACTION = new OpenViewEnabledAction(
        i18n("close_all"), comp -> warnAndCloseAll());

    public static final Action CLOSE_ACTIVE_ACTION = new OpenViewEnabledAction(
        i18n("close"), comp -> warnAndClose(comp.getView()));

    public static final Action CLOSE_UNMODIFIED_ACTION = new OpenViewEnabledAction(
        "Close Unmodified", comp -> warnAndCloseUnmodified());

    private Views() {
    }

    public static List<View> getAll() {
        return views;
    }

    public static View getActive() {
        return activeView;
    }

    public static int getNumViews() {
        return views.size();
    }

    public static void viewClosed(View view) {
        Composition comp = view.getComp();
        History.compClosed(comp);
        comp.dispose();

        views.remove(view);
        if (views.isEmpty()) {
            onAllViewsClosed();
        }
        activateAViewIfNoneIs();
    }

    private static void onAllViewsClosed() {
        setActiveView(null, false);
        activationListeners.forEach(ViewActivationListener::allViewsClosed);
        History.onAllViewsClosed();
        SelectionActions.update(null);
        PixelitorWindow.get().updateTitle(null);
        FramesUI.resetCascadeIndex();
    }

    private static void activateAViewIfNoneIs() {
        if (!views.isEmpty()) {
            boolean activeFound = views.stream()
                .anyMatch(view -> view == activeView);

            if (!activeFound) {
                activate(views.get(0));
            }
        }
    }

    public static void activate(View view) {
        setActiveView(view, true);
    }

    public static void setActiveView(View view, boolean activate) {
        if (view == activeView) {
            return;
        }

        if (activate) {
            if (view == null) {
                throw new IllegalStateException("Can't activate null view");
            }
            if (!view.isMock()) {
                ImageArea.activateView(view);
            }
        }
        activeView = view;
    }

    /**
     * Changes the cursor for all views
     */
    public static void setCursorForAll(Cursor cursor) {
        for (View view : views) {
            view.setCursor(cursor);
        }
    }

    public static void addActivationListener(ViewActivationListener listener) {
        activationListeners.add(listener);
    }

    public static void removeActivationListener(ViewActivationListener listener) {
        activationListeners.remove(listener);
    }

    public static void viewActivated(View view) {
        if (view == activeView) {
            return;
        }

        View oldView = activeView;
        if (oldView != null) {
            oldView.getComp().deactivated();
        }

        var comp = view.getComp();
        setActiveView(view, false);
        SelectionActions.update(comp);
        view.getViewContainer().select();
        view.showLayersUI();

        for (ViewActivationListener listener : activationListeners) {
            listener.viewActivated(oldView, view);
        }

        Layers.activeCompChanged(comp, true);

        boolean maskEditing = view.getMaskViewMode().editMask();
        Tools.setupMaskEditing(maskEditing);
        FgBgColors.setLayerMaskEditing(maskEditing);

        Canvas.activeCanvasSizeChanged(comp.getCanvas());
        PixelitorWindow.get().updateTitle(comp);

//        // Invoke only later, when the view can correctly
//        // translate between image and component spaces.
//        // Important when loading serialized compositions with active shape layers.
//        EventQueue.invokeLater(() -> Tools.editingTargetChanged(comp.getActiveLayer()));
        // Invoking later can lead to situations where the comp has no view,
        // and it's not necessary anymore?
        Tools.editingTargetChanged(comp.getActiveLayer());
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

    public static void fitActive(AutoZoom autoZoom) {
        if (activeView != null) {
            activeView.setZoom(autoZoom);
        }
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

    public static View activateRandomView() {
        View view = Rnd.chooseFrom(views);
        if (view != activeView) {
            activate(view);
            return view;
        }
        return null;
    }

    public static void assertNumViewsIs(int expected) {
        int numViews = getNumViews();
        if (numViews == expected) {
            return;
        }

        throw new AssertionError(format(
            "Expected %d views, found %d (%s)",
            expected, numViews, getOpenCompNamesAsString()));
    }

    public static void assertNumViewsIsAtLeast(int minimum) {
        int numViews = getNumViews();
        if (numViews >= minimum) {
            return;
        }
        throw new AssertionError(format(
            "Expected at least %d views, found %d (%s)",
            minimum, numViews, getOpenCompNamesAsString()));
    }

    public static void assertZoomOfActiveIs(ZoomLevel expected) {
        if (activeView == null) {
            throw new AssertionError("no active view");
        }
        ZoomLevel actual = activeView.getZoomLevel();
        if (actual != expected) {
            throw new AssertionError("expected = " + expected +
                ", found = " + actual);
        }
    }

    private static String getOpenCompNamesAsString() {
        return views.stream()
            .map(View::getName)
            .collect(joining(", ", "[", "]"));
    }

    public static void warnAndClose(View view) {
        if (RandomGUITest.isRunning()) {
            return;
        }

        try {
            var comp = view.getComp();
            if (comp.isUnsaved()) {
                int answer = Dialogs.showCloseWarningDialog(comp.getName());

                if (answer == YES_OPTION) { // "Save"
                    boolean fileSaved = IO.save(comp, false);
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

    private static void warnAndCloseUnmodified() {
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

    public static boolean isAnyPixelGridAllowed() {
        for (View view : views) {
            if (view.allowPixelGrid()) {
                return true;
            }
        }
        return false;
    }

    public static boolean activeCompIs(Composition comp) {
        if (activeView != null) {
            return activeView.getComp() == comp;
        }
        // there is no open view
        return comp == null;
    }

    public static Composition getActiveComp() {
        if (activeView != null) {
            return activeView.getComp();
        }

        // there is no open view
        return null;
    }

    public static Optional<Composition> getActiveCompOpt() {
        return Optional.ofNullable(getActiveComp());
    }

    public static void onActiveComp(Consumer<Composition> action) {
        if (activeView != null) {
            var comp = activeView.getComp();
            action.accept(comp);
        }
    }

    public static <T> T fromActiveComp(Function<Composition, T> function) {
        if (activeView != null) {
            return function.apply(activeView.getComp());
        }

        // there is no open view
        return null;
    }

    public static BufferedImage getActiveCompositeImage() {
        return fromActiveComp(Composition::getCompositeImage);
    }

    public static Optional<Composition> findCompByName(String name) {
        return views.stream()
            .map(View::getComp)
            .filter(c -> c.getName().equals(name))
            .findFirst();
    }

    public static List<Composition> getUnsavedComps() {
        return views.stream()
            .map(View::getComp)
            .filter(Composition::isUnsaved)
            .collect(toList());
    }

    public static Composition addJustLoadedComp(Composition comp) {
        assert comp != null;

        addAsNewComp(comp);

        File file = comp.getFile();
        RecentFilesMenu.INSTANCE.addFile(file);
        Messages.showFileOpenedMessage(comp);

        return comp;
    }

    public static void addAsNewComp(BufferedImage image, File file, String name) {
        var comp = Composition.fromImage(image, file, name);
        addAsNewComp(comp);
    }

    public static void addAsNewComp(Composition comp) {
        try {
            assert comp.getView() == null : "already has a view";

            View view = new View(comp);
            comp.addAllLayersToUI();
            view.setCursor(Tools.getCurrent().getStartingCursor());
            views.add(view);
            MaskViewMode.NORMAL.activate(view, comp.getActiveLayer());
            ImageArea.addNewView(view);
            setActiveView(view, false);

// commented out, because ImageArea.addNewView(view); should always call this anyway
//            Tools.editingTargetChanged(comp.getActiveLayer());
        } catch (Exception e) {
            Messages.showException(e);
        }
    }

    @VisibleForTesting
    public static void assertNumLayersIs(int expected) {
        int found = getNumLayersInActiveComp();
        if (found != expected) {
            throw new AssertionError("expected " + expected + ", found = " + found);
        }
    }

    @VisibleForTesting
    public static int getNumLayersInActiveComp() {
        var comp = getActiveComp();
        if (comp == null) {
            throw new AssertionError("no open images");
        }

        return comp.getNumLayers();
    }

    public static Layer getActiveLayer() {
        if (activeView != null) {
            return activeView.getComp().getActiveLayer();
        }

        return null;
    }

    public static Layer getEditingTarget() {
        if (activeView != null) {
            return activeView.getComp().getEditingTarget();
        }

        return null;
    }

    public static void onActiveLayer(Consumer<Layer> action) {
        if (activeView != null) {
            Layer activeLayer = activeView.getComp().getActiveLayer();
            action.accept(activeLayer);
        }
    }

    public static void onEditingTarget(Consumer<Layer> action) {
        if (activeView != null) {
            Layer editingTarget = activeView.getComp().getEditingTarget();
            action.accept(editingTarget);
        }
    }

    public static Drawable getActiveDrawable() {
        if (activeView != null) {
            var comp = activeView.getComp();
            return comp.getActiveDrawable();
        }

        return null;
    }

    public static Drawable getActiveDrawableOrThrow() {
        if (activeView != null) {
            return activeView.getComp().getActiveDrawableOrThrow();
        }

        throw new IllegalStateException("no active view");
    }

    public static Filterable getActiveFilterable() {
        if (activeView != null) {
            return activeView.getComp().getActiveFilterable();
        }

        throw new IllegalStateException("no active view");
    }

    public static Selection getActiveSelection() {
        if (activeView != null) {
            return activeView.getComp().getSelection();
        }

        // there is no open view
        return null;
    }

    public static Path getActivePath() {
        if (activeView != null) {
            return activeView.getComp().getActivePath();
        }

        // there is no open view
        return null;
    }

    public static boolean activePathIs(Path path) {
        if (activeView != null) {
            Path activePath = activeView.getComp().getActivePath();
            return activePath == path;
        }

        // there is no open view
        return path == null;
    }

    public static void setActivePath(Path path) {
        if (activeView != null) {
            activeView.getComp().setActivePath(path);
        }
    }

    /**
     * Return true if the opening of the file should proceed
     */
    public static boolean warnIfAlreadyOpen(File file) {
        View view = viewOfFile(file);
        if (view == null) {
            return true;
        }
        activate(view);
        String title = "File already opened";
        String msg = "<html>The file <b>" + file.getAbsolutePath()
                     + "</b> is already opened.";
        String[] options = {"Open Again", GUIText.CANCEL};
        boolean again = Dialogs.showOKCancelDialog(view.getDialogParent(),
            msg, title, options, 1, WARNING_MESSAGE);
        return again;
    }

    private static View viewOfFile(File newFile) {
        for (View view : views) {
            File file = view.getComp().getFile();
            if (file != null && file.getPath().equals(newFile.getPath())) {
                return view;
            }
        }
        return null;
    }

    public static void thumbSizeChanged(int newThumbSize) {
        // since the layer GUIs are cached, all views have
        // to be notified to update their buttons
        for (View view : views) {
            view.thumbSizeChanged(newThumbSize);
        }
    }

    public static void appActivated() {
        // Check if any views need to be automatically reloaded
        CompletableFuture<Composition> cf = CompletableFuture.completedFuture(null);
        for (View view : views) {
            // make sure that the next reload is not started
            // before the previous one is finished
            cf = cf.thenCompose(comp -> view.checkForAutoReload());
        }
    }
}
