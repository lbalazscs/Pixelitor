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

package pixelitor;

import pixelitor.gui.ImageComponents;
import pixelitor.history.FadeableEdit;
import pixelitor.history.History;
import pixelitor.layers.DeleteActiveLayerAction;
import pixelitor.layers.Drawable;
import pixelitor.selection.SelectionActions;
import pixelitor.utils.Utils;
import pixelitor.utils.test.Events;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.Optional;

/**
 * Runtime assertions (a kind of "design by contract")
 * that run only in developer mode.
 * They are run by the Build setting or by the assertions
 */
public final class ConsistencyChecks {
    private ConsistencyChecks() { // do not instantiate
    }

    public static void checkAll(Composition comp, boolean checkImageCoversCanvas) {
        assert comp != null;

        selectionActionsEnabledCheck(comp);
        assert fadeWouldWorkOn(comp);
        if (checkImageCoversCanvas) {
            assert imageCoversCanvas(comp);
        }
        assert layerDeleteActionEnabled();
    }

    public static boolean fadeWouldWorkOn(Composition comp) {
        Drawable dr = comp.getActiveDrawableOrNull();
        if (dr == null) {
            // nothing to check
            return true;
        }
        return fadeWouldWorkOn(dr);
    }

    @SuppressWarnings("SameReturnValue")
    public static boolean fadeWouldWorkOn(Drawable dr) {
        assert dr != null;
        if (!History.canFade(dr)) {
            return true;
        }
        Optional<FadeableEdit> edit = History.getPreviousEditForFade(dr);
        if (edit.isPresent()) {
            BufferedImage current = dr.getSelectedSubImage(false, true);

            FadeableEdit fadeableEdit = edit.get();
            BufferedImage previous = fadeableEdit.getBackupImage();
            if (previous == null) {
                // soft reference expired
                return true;
            }

            boolean differentWidth = current.getWidth() != previous.getWidth();
            boolean differentHeight = current.getHeight() != previous.getHeight();
            if (differentWidth || differentHeight) {
                Composition comp = dr.getComp();
                Events.postProgramError("fadeWouldWorkOn problem", comp, null);

                Utils.debugImage(current, "current");
                Utils.debugImage(previous, "previous");
                String lastFadeableOp = History.getLastEditName();

                String historyCompName = fadeableEdit.getComp().getName();
                String activeCompName = ImageComponents.getActiveCompOrNull().getName();
                throw new IllegalStateException("'Fade " + lastFadeableOp + "' would not work now:"
                        + "\nFadeableEdit class = " + fadeableEdit.getClass().getName() + ", and name = " + fadeableEdit.getName()
                        + "\n current selected dimensions: " + current.getWidth() + "x" + current.getHeight() + ", "
                        + "history dimensions: " + previous.getWidth() + "x" + previous.getHeight()
                        + "\nchecked composition = " + comp.getName() + "(hasSelection = " + comp.hasSelection()
                        + (comp.hasSelection() ? ", selection bounds = " + comp.getSelection().getShapeBounds() : "") + ")"
                        + "\nchecked composition canvas = " + comp.getCanvas().getImBounds()
                        + "\nhistory composition = " + historyCompName
                        + "\nactive composition = " + activeCompName
                        + "\n"


                );
            }

        }
        return true;
    }

    private static void selectionActionsEnabledCheck(Composition comp) {
        if (!SwingUtilities.isEventDispatchThread()) {
            return;
        }

        if (comp.hasSelection()) {
            if (!SelectionActions.areEnabled()) {
                throw new IllegalStateException(comp.getName()
                        + " has selection, but selection actions are disabled, thread is "
                        + Thread.currentThread().getName());
            }
        }
    }

    @SuppressWarnings("SameReturnValue")
    public static boolean imageCoversCanvas(Composition comp) {
        comp.forEachDrawable(ConsistencyChecks::imageCoversCanvas);
        return true;
    }

    public static boolean imageCoversCanvas(Drawable dr) {
        Composition comp = dr.getComp();
        BufferedImage image = dr.getImage();

        Canvas canvas = comp.getCanvas();
        if (canvas == null) {
            // can happen during the loading of pxc files
            return true;
        }
        int canvasWidth = canvas.getImWidth();
        int canvasHeight = canvas.getImHeight();

        int txAbs = -dr.getTX();

        int imageWidth = image.getWidth();
        if (txAbs + canvasWidth > imageWidth) {
            return throwImageDoesNotCoverCanvasException(dr);
        }

        int tyAbs = -dr.getTY();
        int imageHeight = image.getHeight();

        if (tyAbs + canvasHeight > imageHeight) {
            return throwImageDoesNotCoverCanvasException(dr);
        }

        return true;
    }

    private static boolean throwImageDoesNotCoverCanvasException(Drawable dr) {
        Composition comp = dr.getComp();
        BufferedImage img = dr.getImage();
        int canvasWidth = comp.getCanvasImWidth();
        int canvasHeight = comp.getCanvasImHeight();
        int imageWidth = img.getWidth();
        int imageHeight = img.getHeight();
        int tx = dr.getTX();
        int ty = dr.getTY();
        String className = dr.getClass().getSimpleName();
        String msg = String.format("canvasWidth = %d, canvasHeight = %d, " +
                        "imageWidth = %d, imageHeight = %d, tx = %d, ty = %d, class = %s",
                canvasWidth, canvasHeight, imageWidth, imageHeight, tx, ty, className);

        throw new IllegalStateException(msg);
    }

    @SuppressWarnings("SameReturnValue")
    public static boolean layerDeleteActionEnabled() {
        DeleteActiveLayerAction action = DeleteActiveLayerAction.INSTANCE;
        if (action == null) {
            // can be null at startup because this check is
            // called while constructing the DeleteActiveLayerAction
            return true;
        }

        Composition comp = ImageComponents.getActiveCompOrNull();
        if (comp == null) {
            return true;
        }

        boolean enabled = action.isEnabled();
        int numLayers = comp.getNumLayers();
        if (enabled) {
            if (numLayers <= 1) {
                throw new IllegalStateException("delete layer enabled for " + comp.getName() + ", but numLayers = " + numLayers);
            }
        } else { // disabled
            if (numLayers >= 2) {
                throw new IllegalStateException("delete layer disabled for " + comp.getName() + ", but numLayers = " + numLayers);
            }
        }
        return true;
    }
}