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

package pixelitor.selection;

import pixelitor.gui.utils.Dialogs;
import pixelitor.history.PixelitorEdit;

import java.util.Objects;

import static pixelitor.selection.SelectionChangeResult.Status.CANCELLED;
import static pixelitor.selection.SelectionChangeResult.Status.OUT_OF_BOUNDS;
import static pixelitor.selection.SelectionChangeResult.Status.SUCCESS;

/**
 * Encapsulates the result of a selection change operation.
 */
public final class SelectionChangeResult {
    /**
     * The possible outcomes of a selection change.
     */
    public enum Status {
        /**
         * The selection was changed successfully.
         */
        SUCCESS,
        /**
         * The user cancelled the operation.
         */
        CANCELLED,
        /**
         * The input shape was empty or outside the canvas.
         */
        OUT_OF_BOUNDS
    }

    private final Status status;
    private final PixelitorEdit edit;

    /**
     * Private constructor to create a new result instance.
     */
    private SelectionChangeResult(Status status, PixelitorEdit edit) {
        this.status = status;
        this.edit = edit;
    }

    /**
     * Creates a result for a successful selection change
     * (new selection, modified selection, or deselect).
     */
    public static SelectionChangeResult success(PixelitorEdit edit) {
        return new SelectionChangeResult(SUCCESS, Objects.requireNonNull(edit));
    }

    /**
     * Creates a result for a user-cancelled operation.
     */
    public static SelectionChangeResult cancelled() {
        return new SelectionChangeResult(CANCELLED, null);
    }

    /**
     * Creates a result for an out-of-bounds or empty selection.
     */
    public static SelectionChangeResult outOfBounds() {
        return new SelectionChangeResult(OUT_OF_BOUNDS, null);
    }

    public Status getStatus() {
        return status;
    }

    public PixelitorEdit getEdit() {
        // this should only be called for a successful result
        if (status != SUCCESS) {
            throw new IllegalStateException("status = " + status);
        }
        return edit;
    }

    public boolean isSuccess() {
        return status == SUCCESS;
    }

    public void showInfoDialog(String source) {
        switch (status) {
            // should not be called for success
            case SUCCESS -> throw new IllegalStateException();
            case CANCELLED -> {
                // do nothing, as the user intentionally cancelled
            }
            case OUT_OF_BOUNDS -> Dialogs.showInfoDialog("No Selection",
                String.format("No selection was created because the %s is outside the canvas.", source));
        }
    }
}
