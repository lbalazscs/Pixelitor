/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.history.FadeableEdit;
import pixelitor.history.History;
import pixelitor.layers.DeleteActiveLayerAction;
import pixelitor.layers.Drawable;
import pixelitor.selection.SelectionActions;
import pixelitor.utils.Utils;
import pixelitor.utils.test.Events;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Optional;

import static java.lang.String.format;

/**
 * Runtime assertions (a kind of "design by contract")
 * that run only in developer mode.
 */
public final class ConsistencyChecks {
    private ConsistencyChecks() { // do not instantiate
    }

    public static void checkAll(Composition comp, boolean checkImageCoversCanvas) {
        assert comp != null;

        selectionActionsEnabledCheck(comp);

        assert selectionShapeIsNotEmpty(comp) : "empty selection shape in " + comp.getName();
        assert selectionIsInsideCanvas(comp) : "selection outside the canvas in " + comp.getName();
        assert fadeWouldWorkOn(comp);

        if (checkImageCoversCanvas) {
            assert imageCoversCanvas(comp);
        }
        assert layerDeleteActionEnabled();
    }

    public static boolean fadeWouldWorkOn(Composition comp) {
        Drawable dr = comp.getActiveDrawable();
        if (dr == null) {
            // nothing to check
            return true;
        }
        return fadeWouldWorkOn(dr);
    }

    @SuppressWarnings("SameReturnValue")
    private static boolean fadeWouldWorkOn(Drawable dr) {
        assert dr != null;
        if (!History.canFade(dr)) {
            return true;
        }
        Optional<FadeableEdit> edit = History.getPreviousEditForFade(dr);
        if (edit.isPresent()) {
            var currentImg = dr.getSelectedSubImage(false);
            var fadeableEdit = edit.get();
            var previousImg = fadeableEdit.getBackupImage();
            if (previousImg == null) {
                // soft reference expired: fade wouldn't work, but not a bug
                return true;
            }

            if (isSizeDifferent(currentImg, previousImg)) {
                var comp = dr.getComp();
                Events.postProgramError("fadeWouldWorkOn problem", comp, null);

                Utils.debugImage(currentImg, "current");
                Utils.debugImage(previousImg, "previous");

                String lastFadeableOp = History.getLastEditName();
                throw new IllegalStateException("'Fade " + lastFadeableOp
                        + "' would not work now");
            }

        }
        return true;
    }

    private static boolean isSizeDifferent(BufferedImage imgA, BufferedImage imgB) {
        return imgA.getWidth() != imgB.getWidth()
                || imgA.getHeight() != imgB.getHeight();
    }

    public static void selectionActionsEnabledCheck(Composition comp) {
        if (!comp.isActive()) {
            return;
        }
        if (comp.hasSelection()) {
            if (!SelectionActions.areEnabled()) {
                throw new IllegalStateException(comp.getName()
                        + " has selection, but selection actions are disabled, thread is "
                        + Thread.currentThread().getName());
            }
        } else { // no selection
            if (SelectionActions.areEnabled()) {
                String msg = comp.getName() + " has no selection, ";
                if(comp.hasBuiltSelection()) {
                    msg += "(but has built selection) ";
                } else {
                    msg += "(no built selection) ";
                }

                throw new IllegalStateException(msg
                        + ", but selection actions are enabled, thread is "
                        + Thread.currentThread().getName());
            }
        }
    }

    public static boolean selectionShapeIsNotEmpty(Composition comp) {
        var selection = comp.getSelection();
        if (selection == null) {
            return true;
        }
        var selShapeBounds = selection.getShapeBounds2D();
        if (selShapeBounds.isEmpty()) {
            return false;
        }
        return true;
    }

    public static boolean selectionIsInsideCanvas(Composition comp) {
        var selection = comp.getSelection();
        if (selection == null) {
            return true;
        }
        Rectangle canvasSize = comp.getCanvasImBounds();
        // increase the size because of rounding errors
        canvasSize.grow(1, 1);
        var selShapeBounds = selection.getShapeBounds2D();

        // In principle the selection must be fully inside the canvas,
        // but this is hard to check since
        // canvasSize.contains(selShapeBounds)
        // doesn't work (the bounds are not necessarily the smallest)
        // so check that it is not fully outside
        boolean ok = !canvasSize.createIntersection(selShapeBounds).isEmpty();
        if (!ok) {
            System.out.println("\nConsistencyChecks::selectionIsInsideCanvas: no intersection: "
                    + "canvasSize = " + canvasSize
                    + ", shapeBounds = " + selShapeBounds);
        }
        return ok;
    }

    @SuppressWarnings("SameReturnValue")
    public static boolean imageCoversCanvas(Composition comp) {
        comp.forEachDrawable(ConsistencyChecks::imageCoversCanvas);
        return true;
    }

    public static boolean imageCoversCanvas(Drawable dr) {
        var canvas = dr.getComp().getCanvas();
        if (canvas == null) {
            // can happen during the loading of pxc files
            return true;
        }

        var image = dr.getImage();

        int txAbs = -dr.getTx();
        if (image.getWidth() < txAbs + canvas.getImWidth()) {
            return throwImageDoesNotCoverCanvasException(dr);
        }

        int tyAbs = -dr.getTy();
        if (image.getHeight() < tyAbs + canvas.getImHeight()) {
            return throwImageDoesNotCoverCanvasException(dr);
        }

        return true;
    }

    private static boolean throwImageDoesNotCoverCanvasException(Drawable dr) {
        var canvas = dr.getComp().getCanvas();
        var img = dr.getImage();

        String msg = format("canvas width = %d, canvas height = %d, " +
                        "image width = %d, image height = %d, " +
                        "tx = %d, ty = %d, class = %s",
                canvas.getImWidth(), canvas.getImHeight(),
                img.getWidth(), img.getHeight(),
                dr.getTx(), dr.getTy(), dr.getClass().getSimpleName());

        throw new IllegalStateException(msg);
    }

    @SuppressWarnings("SameReturnValue")
    public static boolean layerDeleteActionEnabled() {
        var action = DeleteActiveLayerAction.INSTANCE;
        if (action == null) {
            // can be null at startup because this check is
            // called while constructing the DeleteActiveLayerAction
            return true;
        }

        var comp = OpenImages.getActiveComp();
        if (comp == null) {
            return true;
        }

        boolean enabled = action.isEnabled();
        int numLayers = comp.getNumLayers();
        if (enabled) {
            if (numLayers <= 1) {
                throw new IllegalStateException("delete layer enabled for "
                        + comp.getName() + ", but numLayers = " + numLayers);
            }
        } else { // disabled
            if (numLayers >= 2) {
                throw new IllegalStateException("delete layer disabled for "
                        + comp.getName() + ", but numLayers = " + numLayers);
            }
        }
        return true;
    }
}