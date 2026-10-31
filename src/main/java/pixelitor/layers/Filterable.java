/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.layers;

import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.filters.Filter;
import pixelitor.filters.FilterContext;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.filters.util.Filters;
import pixelitor.gui.MouseZoomMethod;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.View;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.menus.view.ZoomMenu;
import pixelitor.tools.Tools;
import pixelitor.utils.Cursors;
import pixelitor.utils.Messages;
import pixelitor.utils.Threads;

import javax.swing.*;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.image.BufferedImage;

import static pixelitor.filters.FilterContext.FILTER_WITHOUT_DIALOG;
import static pixelitor.gui.utils.Screens.Align.FRAME_RIGHT;

/**
 * A layer that a {@link Filter} can be applied to, either directly or
 * through an interactive filter dialog with a live preview.
 * <p>
 * There are two families of implementations, and they differ in what
 * "applying a filter" means:
 * <ul>
 *   <li>{@link Drawable}s ({@link ImageLayer} and {@link LayerMask}) hold
 *   pixels and are filtered <em>destructively</em>: the filter transforms the
 *   layer's image (limited to the selection, if there is one), and the result
 *   replaces the original pixels together with an undoable history edit.
 *   During a dialog session the result is shown from a separate preview image,
 *   so the real pixels are replaced only if the dialog is accepted.</li>
 *   <li>{@link AdjustmentLayer} and its subclass {@link SmartFilter} own a
 *   filter instance and apply it <em>non-destructively</em> when the
 *   composition is rendered. They have no preview image: the dialog edits the
 *   settings of the owned filter in place, and accepting or canceling the
 *   dialog keeps or restores those settings.</li>
 * </ul>
 * <p>
 * <b>Direct use.</b> {@link #startFilter(Filter, FilterContext)} applies a
 * filter with its current settings and without any dialog. This is what
 * filters without a GUI and the "Repeat Last" action
 * ({@link pixelitor.filters.RepeatLast}) rely on, and it is meaningful
 * only for drawables. The {@link FilterContext} tells whether the result is
 * only a preview, and whether an undo edit is needed.
 * <p>
 * <b>Interactive use.</b> {@link #startFilterInteractively(Filter, boolean)}
 * starts a filter the way a user would: filters that have a GUI run a
 * <em>dialog session</em>, in which the methods of this interface are
 * called in the following order:
 * <ol>
 *   <li>{@link #startPreviewing()}, before the GUI is created.</li>
 *   <li>The filter creates its {@link FilterGUI} with
 *   {@link FilterWithGUI#createGUI(Filterable, boolean)}. The GUI keeps a
 *   reference to this object (which is why GUIs must not be cached), and is
 *   shown in a modal dialog built with a {@link DialogBuilder}.</li>
 *   <li>When the dialog becomes visible, the GUI requests the first preview
 *   with {@link #startPreview(Filter, boolean, Component)}, and it requests
 *   a new one after each change of the filter settings.</li>
 *   <li>Optionally, {@link #setShowOriginal(boolean)} is called when the
 *   "Show Original" checkbox of the dialog is toggled.</li>
 *   <li>The session ends with exactly one of
 *   {@link #onFilterDialogAccepted(String)} (the user pressed OK) or
 *   {@link #onFilterDialogCanceled()} (Cancel, Esc, closing the window, or a
 *   failure while setting up the dialog). Only after that is the GUI
 *   disposed.</li>
 * </ol>
 * <p>
 * The interactive protocol runs on the event dispatch thread, and sessions
 * never overlap on the same layer, since the dialog is modal.
 *
 * @see Drawable
 * @see AdjustmentLayer
 * @see FilterGUI
 * @see FilterWithGUI
 * @see pixelitor.filters.util.FilterAction
 */
public interface Filterable {
    /**
     * Returns the composition that contains this layer.
     * <p>
     * The composition supplies the context in which a filter operates,
     * for example the current selection and the canvas size.
     */
    Composition getComp();

    /**
     * Begins a filter dialog session by preparing this layer to show
     * preview results (or live-edited filter settings) while the dialog
     * is open.
     * <p>
     * It is called once, on the EDT, <em>before</em> the filter's
     * {@link FilterGUI} is created. Creating the GUI can change the settings of
     * the filter (when the settings are reset), so a layer that needs to
     * remember the state from before the editing must capture it here.
     * The session must later be ended by exactly one call to either
     * {@link #onFilterDialogAccepted(String)} or
     * {@link #onFilterDialogCanceled()}.
     * <p>
     * Implementations behave as follows:
     * <ul>
     *   <li>A {@link Drawable} switches to its preview state and prepares the
     *   image that will show the preview. If there is a selection, this is a
     *   copy of the real image, because only the selected region is replaced
     *   in place, which would otherwise also modify the real image. Without a
     *   selection, the preview image simply replaces the image reference.</li>
     *   <li>An {@link AdjustmentLayer} stores a backup copy of its filter, so
     *   that the dialog can be canceled and "Show Original" can work.</li>
     * </ul>
     * <p>
     * If this method throws, the session is considered not to have started and
     * no cleanup callback is called, so it must not leave partial state behind.
     * Sessions can't be nested: implementations may assert that no session is
     * already active.
     */
    void startPreviewing();

    /**
     * Ends the preview mode, discards everything that was needed only for it,
     * and restores the normal display of the layer, without committing the
     * previewed result.
     * <p>
     * It must only be called during a session. {@link ImageLayer} (and
     * therefore {@link LayerMask}) implements the canceling of a dialog session by
     * delegating to this method, while the accept path does its own cleanup.
     * Implementations are free to leave it empty if their cleanup is done in
     * the dialog callbacks, as {@link AdjustmentLayer} does.
     */
    void stopPreviewing();

    /**
     * Switches the display between the original state and the filter preview
     * during a dialog session. It is called by the "Show Original" checkbox of
     * the filter dialog, and it has no meaning outside a session.
     * <p>
     * Implementations behave as follows:
     * <ul>
     *   <li>A {@link Drawable} toggles between showing its unmodified image and
     *   the preview image. Nothing happens if it is already in the requested
     *   state.</li>
     *   <li>An {@link AdjustmentLayer} swaps its active filter with the backup
     *   created in {@link #startPreviewing()}, so the two instances exchange
     *   roles. It expects only real toggles, and asserts that the requested
     *   state differs from the current one.</li>
     * </ul>
     * <p>
     * Accepting or canceling the dialog while the original is displayed must
     * still give the correct result. For drawables, accepting commits the
     * preview, and canceling discards it. For adjustment layers, accepting keeps the
     * edited settings, and canceling restores the original ones.
     * <p>
     * The checkbox is not the only way out of the "original" display: when a
     * parameter changes, the GUI deselects the checkbox <em>without</em>
     * calling this method (see
     * {@link pixelitor.filters.gui.ParametrizedFilterGUI#paramAdjusted()})
     * and requests a new preview. Therefore the following call to
     * {@link #startPreview(Filter, boolean, Component)} with
     * {@code initialPreview = false} must also bring the layer back from the
     * "original" display to the preview. Drawables do this implicitly when the
     * new preview image arrives.
     *
     * @param b true to show the original, false to show the preview again
     */
    void setShowOriginal(boolean b);

    /**
     * Computes and displays a preview of the given filter with its current
     * settings. The {@link FilterGUI} calls it once when the dialog becomes
     * visible, and again after every change of the filter settings.
     * <p>
     * Implementations behave as follows:
     * <ul>
     *   <li>A {@link Drawable} runs the filter on its filter source image
     *   with the {@link FilterContext#PREVIEWING} context (via
     *   {@link #startFilter(Filter, FilterContext, Component)}) and shows the
     *   result as the preview image.</li>
     *   <li>An {@link AdjustmentLayer} ignores the {@code filter} argument,
     *   because the filter being edited is the one it already owns. It only
     *   has to repaint, and not even that for the initial preview.</li>
     *   <li>A {@link SmartFilter} invalidates its cached output unless this is
     *   the initial preview, and recomputes it if necessary.</li>
     * </ul>
     *
     * @param filter           the filter to preview, with the settings
     *                         currently shown in the dialog
     * @param initialPreview   true if the settings haven't changed yet and the
     *                         call only triggers the first preview run when the
     *                         dialog is shown, false if it was triggered by a
     *                         settings change. Implementations can use it to
     *                         avoid unnecessary work.
     * @param busyCursorTarget the component that shows a busy cursor while the
     *                         preview is computed. It is the window of the
     *                         dialog for the initial preview, and the GUI
     *                         panel otherwise.
     */
    void startPreview(Filter filter, boolean initialPreview, Component busyCursorTarget);

    /**
     * Called when the user accepts the dialog (OK), to make the previewed
     * result the permanent state of this layer, and to end the session.
     * <p>
     * It runs after the dialog has been closed, but before the GUI is
     * disposed. Implementations must return to the normal (non-previewing)
     * state and register an undo edit if the layer has changed:
     * <ul>
     *   <li>A {@link Drawable} backs up the pixels that are about to change (so
     *   that the operation can be undone), replaces its image with the preview
     *   image, and refreshes its icon. Accepting while "Show Original" is
     *   checked still commits the preview.</li>
     *   <li>An {@link AdjustmentLayer} has nothing to commit, because its filter
     *   is already live. It records a "changed" edit if the filter settings
     *   differ from the backup (but not if the layer was just added), and then
     *   discards the backup. If "Show Original" is checked, the modified
     *   settings are kept, not discarded.</li>
     * </ul>
     *
     * @param filterName the name of the filter, used for naming the history
     *                   edit (adjustment layers use their own name instead)
     */
    void onFilterDialogAccepted(String filterName);

    /**
     * Called when the dialog is canceled (Cancel button, Esc, or closing the
     * window), to discard the effects of the session and to end it.
     * It is also called to roll back a session if the setup of the dialog
     * failed after {@link #startPreviewing()} had succeeded, in which case
     * it can happen before any preview was computed.
     * <p>
     * Afterwards, the layer must be in the same state as before
     * {@link #startPreviewing()}, and no history edit is created.
     * A {@link Drawable} does this by calling {@link #stopPreviewing()}, and
     * an {@link AdjustmentLayer} restores its filter from the backup (and
     * adapts it to the current image).
     */
    void onFilterDialogCanceled();

    /**
     * Commits the result of a filter that was run with a context that is not
     * a preview. It is called by {@link Drawable#runFilter(Filter, FilterContext)},
     * and it is expected to be in the normal (non-previewing) state.
     * <p>
     * The implementation replaces the layer's pixels with the result (only
     * within the selection, if there is one), refreshes the display and the
     * icon, and, if {@link FilterContext#needsUndo()} is true for the given
     * context, adds an undoable history edit that uses {@code src} as its
     * backup.
     * <p>
     * It is never called on an {@link AdjustmentLayer}, which throws an
     * {@link IllegalStateException} if it happens.
     *
     * @param src        the image that was passed to the filter. It must not
     *                   have been modified, so that it can serve as the undo backup.
     * @param dest       the image returned by the filter. It must not be
     *                   the layer's own image.
     * @param context    the context in which the filter was run
     * @param filterName the name of the filter, used for naming the history edit
     */
    void filterWithoutDialogFinished(BufferedImage src, BufferedImage dest, FilterContext context, String filterName);

    /**
     * Runs the given filter with the given context, showing the busy cursor
     * on the main window.
     * <p>
     * Equivalent to
     * {@link #startFilter(Filter, FilterContext, Component)}
     * with {@link PixelitorWindow#get()} as the busy cursor target.
     */
    default void startFilter(Filter filter, FilterContext context) {
        startFilter(filter, context, PixelitorWindow.get());
    }

    /**
     * Runs the given filter with the given context.
     * <p>
     * This is the common entry point for running a filter without a dialog
     * (also used for the preview runs of a dialog session). It
     * delegates the actual work to {@link #runFilter(Filter, FilterContext)},
     * and around it
     * <ul>
     *   <li>shows a busy cursor on the given component while the task runs,</li>
     *   <li>reports the total elapsed time (including the application of the
     *   result) via {@link Messages}, and</li>
     *   <li>registers the filter as the last used filter with
     *   {@link Filters#setLastFilter(Filter)}, regardless of the context.
     *   (This makes it available to "Repeat Last", except for
     *   filters such as Fade that are excluded there.)</li>
     * </ul>
     *
     * @param filter           the filter to run
     * @param context          the context that determines how the result is
     *                         used: as a preview or as a final result, and
     *                         whether it needs an undo edit
     * @param busyCursorTarget the component that shows a busy cursor while
     *                         the filter is running
     */
    default void startFilter(Filter filter, FilterContext context, Component busyCursorTarget) {
        long startTime = System.nanoTime();

        Runnable task = () -> runFilter(filter, context);
        GUIUtils.runWithBusyCursor(task, busyCursorTarget);

        long totalTime = (System.nanoTime() - startTime) / 1_000_000;
        Messages.showPerformanceMessage(filter.getName(), totalTime);

        Filters.setLastFilter(filter);
    }

    /**
     * Does the actual work of applying a filter in the given context, without
     * any cursor handling, timing, or last-filter bookkeeping. These are added
     * by {@link #startFilter(Filter, FilterContext, Component)}, which is
     * normally the right method to call.
     * <p>
     * A {@link Drawable} implements it by running the filter on its filter
     * source image. If the context is a preview, the result is shown as the
     * preview image (see {@link Drawable#changePreviewImage}), otherwise it is
     * committed with
     * {@link #filterWithoutDialogFinished(BufferedImage, BufferedImage, FilterContext, String)}.
     * Failures are not propagated to the caller: they are reported to the
     * user (or rethrown only in the random GUI test).
     * <p>
     * An {@link AdjustmentLayer} implements it by delegating to
     * {@link #startFilterInteractively(Filter, boolean)}, so it must only be
     * given filters that have a GUI. For a filter without a GUI this would
     * recurse endlessly.
     *
     * @param filter  the filter to run
     * @param context the context that determines how the result is used
     */
    void runFilter(Filter filter, FilterContext context);

    /**
     * Starts a filter the way a user would: filters that have a GUI are
     * configured in a modal dialog with a live preview, and all other
     * filters are applied immediately. It is used when the filter is started
     * from the menu, with "Show Last", or when an adjustment layer is edited.
     * <p>
     * For a {@link FilterWithGUI}, a full dialog session is run (see the
     * class documentation), and the method returns after the dialog has been
     * closed. If the setup of the dialog fails with an exception, the
     * partially initialized session is rolled back as if it had been canceled,
     * and the exception is rethrown. Any other filter is run immediately in
     * the {@link FilterContext#FILTER_WITHOUT_DIALOG} context, which makes
     * the change undoable.
     * <p>
     * The receiver is expected to be the active filterable of the active
     * view: the dialog changes the cursor of that view, and the
     * "Show Original" checkbox finds its target with
     * {@link Views#getActiveFilterable()}.
     * It must be called on the EDT.
     *
     * @param filter        the filter to start
     * @param resetSettings for filters with a GUI, true if the GUI should
     *                      reset the filter settings to their defaults (as
     *                      when starting from the menu), or false to keep the
     *                      current ones (as when editing the filter of an
     *                      adjustment layer). It has no effect on filters
     *                      without a GUI.
     * @return true if the filter was not canceled, that is, if it was
     * applied without a dialog or if the dialog was accepted
     */
    default boolean startFilterInteractively(Filter filter, boolean resetSettings) {
        assert Threads.calledOnEDT();

        if (filter instanceof FilterWithGUI fwg) {
            return startFilterWithGUI(fwg, resetSettings);
        }
        startFilter(filter, FILTER_WITHOUT_DIALOG);
        return true;
    }

    /**
     * Runs a complete dialog session for a filter with a GUI.
     * <p>
     * The setup (finishing the current tool operation, starting the preview
     * session, creating the GUI and building the dialog) is done with a busy
     * cursor. The cursors are restored before showing the dialog, because it is
     * modal. If anything fails during the setup, the session is rolled back with
     * {@link #rollbackFilterDialog(boolean, FilterGUI, Throwable)}.
     *
     * @return true if the dialog was accepted
     */
    private boolean startFilterWithGUI(FilterWithGUI filter, boolean resetSettings) {
        PixelitorWindow window = PixelitorWindow.get();
        View view = Views.getActive();

        // save the view cursor set by the current
        // tool so that it can be restored later
        Cursor toolViewCursor = view.getCursor();

        // how far the setup got, needed for the rollback if it fails
        boolean previewStarted = false;
        FilterGUI gui = null;
        try {
            JDialog dialog;
            DialogBuilder dialogBuilder;
            try {
                window.setCursor(Cursors.BUSY);
                view.setCursor(Cursors.BUSY);

                Tools.forceFinish();

                startPreviewing();
                previewStarted = true;

                gui = filter.createGUI(this, resetSettings);
                MouseZoomMethod.active.installOnOther(gui);
                ZoomMenu.setupZoomKeys(gui);

                dialogBuilder = createFilterDialogBuilder(filter, gui);
                dialog = dialogBuilder.build();
            } finally {
                // the dialog is modal, so the cursors must
                // be restored before it is shown
                window.setCursor(Cursors.DEFAULT);
                view.setCursor(toolViewCursor);
            }

            GUIUtils.showDialog(dialog, FRAME_RIGHT);
            return dialogBuilder.wasAccepted();
        } catch (Throwable t) {
            // The dialog never ran to completion, so neither its OK/cancel
            // action nor its close action has cleaned up. (Exceptions thrown
            // by those actions occur in the event handler, not here.)
            rollbackFilterDialog(previewStarted, gui, t);
            throw t;
        }
    }

    /**
     * Creates the builder of the modal dialog that hosts the filter GUI,
     * and connects the dialog events to the session callbacks:
     * <ul>
     *   <li>when the dialog becomes visible, the initial preview is
     *   requested,</li>
     *   <li>OK calls {@link #onFilterDialogAccepted(String)},</li>
     *   <li>Cancel, Esc and closing the window call
     *   {@link #onFilterDialogCanceled()}, and</li>
     *   <li>whichever way the dialog is closed, the GUI is disposed afterwards.</li>
     * </ul>
     * The dialog also gets the menu bar of the filter (presets and help), if any,
     * scrollbars, and the copy shortcuts.
     */
    private DialogBuilder createFilterDialogBuilder(FilterWithGUI filter, FilterGUI gui) {
        return new DialogBuilder()
            .title(filter.getName())
            .menuBar(filter.getMenuBar())
            .name("filterDialog")
            .content(gui)
            .withScrollbars()
            .enableCopyShortcuts()
            .onVisibleAction(() -> gui.startPreview(true))
            .okAction(() -> onFilterDialogAccepted(filter.getName()))
            .cancelAction(this::onFilterDialogCanceled)
            // runs after the ok/cancel action, once the modal dialog
            // has closed => clean up resources
            .closeAction(gui::dispose);
    }

    /**
     * Cleans up after a dialog session that failed before the dialog could
     * run to completion.
     * <p>
     * If the preview session was started, it is canceled in the same way
     * as when the user cancels the dialog, and the GUI (if it was already
     * created) is disposed. Failures during the rollback do not hide the
     * original problem: they are added to it as suppressed exceptions.
     *
     * @param previewStarted true if {@link #startPreviewing()} completed
     * @param gui            the GUI, or null if it hasn't been created yet
     * @param cause          the exception that caused the rollback
     */
    private void rollbackFilterDialog(boolean previewStarted,
                                      FilterGUI gui,
                                      Throwable cause) {
        if (previewStarted) {
            try {
                // same cleanup as when the user cancels the dialog
                onFilterDialogCanceled();
            } catch (Throwable t) {
                cause.addSuppressed(t);
            }
        }
        if (gui != null) {
            try {
                gui.dispose();
            } catch (Throwable t) {
                cause.addSuppressed(t);
            }
        }
    }
}
