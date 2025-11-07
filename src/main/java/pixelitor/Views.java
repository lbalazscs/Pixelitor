/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.utils.ViewEnabledAction;
import pixelitor.history.History;
import pixelitor.io.FileIO;
import pixelitor.layers.*;
import pixelitor.menus.file.RecentFilesMenu;
import pixelitor.selection.Selection;
import pixelitor.selection.SelectionActions;
import pixelitor.tools.Tools;
import pixelitor.tools.pen.Path;
import pixelitor.utils.Messages;
import pixelitor.utils.Rnd;
import pixelitor.utils.ViewActivationListener;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;
import static javax.swing.JOptionPane.CANCEL_OPTION;
import static javax.swing.JOptionPane.CLOSED_OPTION;
import static javax.swing.JOptionPane.NO_OPTION;
import static javax.swing.JOptionPane.WARNING_MESSAGE;
import static javax.swing.JOptionPane.YES_OPTION;
import static pixelitor.gui.ImageArea.Mode.FRAMES;
import static pixelitor.utils.Texts.i18n;

/**
 * Static methods for managing the collection of open views.
 */
public class Views {
    private static final List<View> views = new ArrayList<>();
    private static int pastedCount = 1;
    private static View activeView;
    private static final List<ViewActivationListener> activationListeners
        = new ArrayList<>();

    public static final Action CLOSE_ALL_ACTION = new ViewEnabledAction(
        i18n("close_all"), comp -> warnAndCloseAll());

    public static final Action CLOSE_ACTIVE_ACTION = new ViewEnabledAction(
        i18n("close"), comp -> warnAndClose(comp.getView()));

    public static final Action CLOSE_ALL_UNMODIFIED_ACTION = new ViewEnabledAction(
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
            allViewsClosed();
        }
        ensureActiveViewExists();
    }

    private static void allViewsClosed() {
        setActiveView(null, false);
        activationListeners.forEach(ViewActivationListener::allViewsClosed);
        History.onAllViewsClosed();
        SelectionActions.update(null);
        PixelitorWindow.get().updateTitle(null);
        FramesUI.resetCascadeCount();
    }

    // ensures that an active view is set if there are any open views remaining
    private static void ensureActiveViewExists() {
        if (!views.isEmpty() && !views.contains(activeView)) {
            activate(views.getFirst());
        }
    }

    public static void activate(View view) {
        setActiveView(view, true);
    }

    /**
     * Sets the active view, optionally triggering full UI activation.
     */
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

        assert view == null || view.checkInvariants();
        activeView = view;
    }

    /**
     * Changes the mouse cursor for all open views.
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

        Composition comp = view.getComp();
        setActiveView(view, false);
        SelectionActions.update(comp);
        view.getViewContainer().select();
        view.showLayersUI();

        for (ViewActivationListener listener : activationListeners) {
            listener.viewActivated(oldView, view);
        }

        // assume that the new view has a proper mask view mode set up
        LayerEvents.fireActiveCompChanged(comp, false);

        boolean maskEditing = view.getMaskViewMode().isEditingMask();
        Tools.maskEditingChanged(maskEditing);
        FgBgColors.maskEditingChanged(maskEditing);

        Canvas.activeCanvasSizeChanged(comp.getCanvas());
        PixelitorWindow.get().updateTitle(comp);

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

    /**
     * Repaints all currently visible views.
     */
    public static void repaintVisible() {
        if (ImageArea.isActiveMode(FRAMES)) {
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

    public static void onActive(Consumer<View> action) {
        if (activeView != null) {
            action.accept(activeView);
        }
    }

    public static void forEach(Consumer<View> action) {
        for (View view : views) {
            action.accept(view);
        }
    }

    public static View activateRandomView() {
        if (views.isEmpty()) {
            return null;
        }
        View view = Rnd.chooseFrom(views);
        if (view != activeView) {
            activate(view);
            return view;
        }
        return null;
    }

    public static List<String> getOpenCompNames() {
        return views.stream()
            .map(View::getName)
            .toList();
    }

    /**
     * Closes the given view, prompting to save unsaved changes if necessary.
     */
    public static void warnAndClose(View view) {
        if (RandomGUITest.isRunning()) {
            return;
        }

        try {
            Composition comp = view.getComp();
            if (comp.hasUnsavedChanges()) {
                int answer = Dialogs.showCloseWarningDialog(comp.getName());

                switch (answer) {
                    case YES_OPTION:  // "Save"
                        boolean saved = FileIO.save(comp, false);
                        if (saved) {
                            view.close();
                        }
                        break;
                    case NO_OPTION:  // "Don't Save"
                        view.close();
                        break;
                    case CANCEL_OPTION:
                    case CLOSED_OPTION:  // dialog closed by pressing X
                        // do nothing
                        return;
                    default:
                        throw new IllegalStateException("answer = " + answer);
                }
            } else {
                // no unsaved changes, close directly
                view.close();
            }
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }

    private static void warnAndCloseAll() {
        warnAndCloseAllIf(view -> true);
    }

    public static void warnAndCloseAllBut(View ignored) {
        warnAndCloseAllIf(view -> view != ignored);
    }

    private static void warnAndCloseUnmodified() {
        warnAndCloseAllIf(view -> !view.getComp().isDirty());
    }

    // close all views matching a predicate, prompting for unsaved changes
    private static void warnAndCloseAllIf(Predicate<View> condition) {
        // make a copy because items will be removed from the original while iterating
        List<View> viewsToProcess = new ArrayList<>(views);
        for (View view : viewsToProcess) {
            if (condition.test(view)) {
                warnAndClose(view);
            }
        }
    }

    public static boolean doesAnyViewAllowPixelGrid() {
        for (View view : views) {
            if (view.allowPixelGrid()) {
                return true;
            }
        }
        return false;
    }

    public static Composition getActiveComp() {
        if (activeView != null) {
            return activeView.getComp();
        }

        // there is no open view
        return null;
    }

    public static void onActiveComp(Consumer<Composition> action) {
        if (activeView != null) {
            try {
                action.accept(activeView.getComp());
            } catch (Exception e) {
                Messages.showException(e);
            }
        }
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
            .filter(Composition::hasUnsavedChanges)
            .collect(toList());
    }

    /**
     * Adds a newly loaded composition to the application, sets it as active, and updates related UI.
     */
    public static Composition addJustLoadedComp(Composition comp) {
        assert comp != null;

        addNew(comp);

        File file = comp.getFile();
        RecentFilesMenu.INSTANCE.addRecentFile(file);
        Messages.showFileOpenedMessage(comp);

        // TODO this is a workaround hack because adj layer filters running
        //  outside the EDT during the loading process mess up the tabs
        EventQueue.invokeLater(() -> PixelitorWindow.get().repaint());

        return comp;
    }

    public static void addNewPasted(BufferedImage pastedImage) {
        String name = "Pasted Image " + pastedCount++;
        addNew(Composition.fromImage(pastedImage, null, name));
    }

    /**
     * Adds the given composition to the UI, creating and configuring a new view for it.
     */
    public static void addNew(Composition comp) {
        try {
            assert comp.getView() == null : "already has a view";
            registerView(new View(comp));
        } catch (Exception e) {
            Messages.showException(e);
        }
    }

    // registers a new view with various app components
    private static void registerView(View view) {
        Composition comp = view.getComp();
        comp.addLayersToUI();
        view.setCursor(Tools.getActive().getStartingCursor());
        views.add(view);
        view.setMaskViewMode(MaskViewMode.NORMAL, comp.getActiveLayer());
        ImageArea.addView(view);
        setActiveView(view, false);
    }

    /**
     * Returns the number of layers in the active layer holder.
     */
    public static int getNumLayersInActiveHolder() {
        Composition comp = getActiveComp();
        if (comp == null) {
            throw new AssertionError("no open images");
        }

//        return comp.getActiveHolder().getNumLayers();

        // in the case of smart filters, this one checks
        // the holder of the smart object
        return getActiveLayer().getHolderForNewLayers().getNumLayers();
    }

    /**
     * Returns the active layer of the active composition.
     */
    public static Layer getActiveLayer() {
        if (activeView != null) {
            return activeView.getComp().getActiveLayer();
        }

        return null;
    }

    public static Layer findFirstLayerWhere(Predicate<Layer> predicate, boolean includeMasks) {
        if (activeView != null) {
            return activeView.getComp().findFirstLayerWhere(predicate, includeMasks);
        }

        return null;
    }

    public static void onActiveLayer(Consumer<Layer> action) {
        if (activeView != null) {
            action.accept(activeView.getComp().getActiveLayer());
        }
    }

    public static Drawable getActiveDrawable() {
        if (activeView != null) {
            return activeView.getComp().getActiveDrawable();
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

    /**
     * Returns the selection of the active composition.
     */
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

    public static void setActivePath(Path path) {
        if (activeView != null) {
            activeView.getComp().setActivePath(path);
        }
    }

    /**
     * Warns the user if a file is already open and prompts for confirmation to open it again.
     */
    public static boolean warnIfAlreadyOpen(File file) {
        View view = findViewByFile(file);
        if (view == null) {
            return true; // the file is not open; proceed with opening
        }

        activate(view);

        String title = "File Already Open";
        String msg = "<html>The file <b>" + file.getAbsolutePath()
            + "</b> is already opened.";
        String[] options = {"Open Again", GUIText.CANCEL};
        boolean again = Dialogs.showOKCancelDialog(view.getDialogParent(),
            msg, title, options, 1, WARNING_MESSAGE);
        return again;
    }

    // finds an open view associated with the given file path
    private static View findViewByFile(File targetFile) {
        for (View view : views) {
            File file = view.getComp().getFile();
            if (file != null && file.getPath().equals(targetFile.getPath())) {
                return view;
            }
        }
        return null;
    }

    public static void appWindowActivated() {
        // check if any views need to be automatically reloaded due to external modifications
        CompletableFuture<Composition> chainedChecks = CompletableFuture.completedFuture(null);
        for (View view : views) {
            // make sure that the next reload is not started
            // before the previous one is finished
            chainedChecks = chainedChecks.thenCompose(comp -> view.checkForExternalModifications());
        }
    }

    // called from test initialization to ensure that views
    // from previous test runs don't interfere with the current test
    public static void clear() {
        views.clear();
        activeView = null;
        pastedCount = 1;
    }
}
