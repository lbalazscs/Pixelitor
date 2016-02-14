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

package pixelitor;

import pixelitor.gui.ImageComponents;
import pixelitor.history.FadeableEdit;
import pixelitor.history.History;
import pixelitor.layers.DeleteActiveLayerAction;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.menus.SelectionActions;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.Optional;

import static pixelitor.gui.ImageComponents.getActiveComp;

/**
 * Consistency checks that run only in developer mode.
 * They are enabled by the Build setting or by the assertions
 */
public final class ConsistencyChecks {
    private ConsistencyChecks() { // do not instantiate
    }

    public static void checkAll(boolean checkImageCoversCanvas) {
        getActiveComp().ifPresent(comp -> {
            selectionCheck(comp);
            fadeCheck(comp);
            if (checkImageCoversCanvas) {
                imageCoversCanvasCheck(comp);
            }
            layerDeleteActionEnabledCheck();
        });
    }

    public static boolean fadeCheck(Composition comp) {
        ImageLayer layer = comp.getActiveMaskOrImageLayerOrNull();
        if(layer == null) {
            // nothing to check
            return true;
        }
        return fadeCheck(layer);
    }

    /**
     * Checks whether Fade would work now
     */
    @SuppressWarnings("SameReturnValue")
    public static boolean fadeCheck(ImageLayer layer) {
        assert layer != null;
        if (!History.canFade()) {
            return true;
        }
        Optional<FadeableEdit> edit = History.getPreviousEditForFade(layer);
        if (edit.isPresent()) {
            BufferedImage current = layer.getImageOrSubImageIfSelected(false, true);

            FadeableEdit fadeableEdit = edit.get();
            BufferedImage previous = fadeableEdit.getBackupImage();
            if (previous == null) {
                // soft reference expired
                return true;
            }

            boolean differentWidth = current.getWidth() != previous.getWidth();
            boolean differentHeight = current.getHeight() != previous.getHeight();
            if (differentWidth || differentHeight) {
                Utils.debugImage(current, "current");
                Utils.debugImage(previous, "previous");
                String lastFadeableOp = History.getLastEditName();
                Composition comp = layer.getComp();
                throw new IllegalStateException("'Fade " + lastFadeableOp + "' would not work now:"
                        + "\nFadeableEdit class = " + fadeableEdit.getClass().getName() + ", and name = " + fadeableEdit.getName()
                        + "\n current selected dimensions: " + current.getWidth() + "x" + current.getHeight() + ", "
                        + "history dimensions: " + previous.getWidth() + "x" + previous.getHeight()
                        + "\nchecked composition = " + comp.getName() + "(hasSelection = " + comp.hasSelection()
                        + (comp.hasSelection() ? ", selection bounds = " + comp.getSelection().get().getShapeBounds() : "") + ")"
                        + "\nchecked composition canvas = " + comp.getCanvas().getBounds()
                        + "\nhistory composition = " + fadeableEdit.getComp().getName()
                        + "\nactive composition = " + ImageComponents.getActiveComp().get().getName()
                        + "\n"


                );
            }

        }
        return true;
    }

    private static void selectionCheck(Composition comp) {
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

    public static boolean imageCoversCanvasCheck(Composition comp) {
        int nrLayers = comp.getNrLayers();
        for (int i = 0; i < nrLayers; i++) {
            Layer layer = comp.getLayer(i);
            if (layer instanceof ImageLayer) {
                boolean b = imageCoversCanvasCheck((ImageLayer) layer);
                if (!b) {
                    return false;
                }
            }
            if (layer.hasMask()) {
                boolean b = imageCoversCanvasCheck(layer.getMask());
                if (!b) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean imageCoversCanvasCheck(ImageLayer layer) {
        Composition comp = layer.getComp();
        BufferedImage image = layer.getImage();

        int txAbs = -layer.getTX();
        int canvasWidth = comp.getCanvasWidth();
        int imageWidth = image.getWidth();
        if (txAbs + canvasWidth > imageWidth) {
            return throwImageDoesNotCoverCanvasException(layer);
        }

        int tyAbs = -layer.getTY();
        int canvasHeight = comp.getCanvasHeight();
        int imageHeight = image.getHeight();

        if (tyAbs + canvasHeight > imageHeight) {
            return throwImageDoesNotCoverCanvasException(layer);
        }

        return true;
    }

    private static boolean throwImageDoesNotCoverCanvasException(ImageLayer layer) {
        Composition comp = layer.getComp();
        BufferedImage bufferedImage = layer.getImage();
        int canvasWidth = comp.getCanvasWidth();
        int canvasHeight = comp.getCanvasHeight();
        int imageWidth = bufferedImage.getWidth();
        int imageHeight = bufferedImage.getHeight();
        int tx = layer.getTX();
        int ty = layer.getTY();
        String className = layer.getClass().getSimpleName();
        String msg = String.format("canvasWidth = %d, canvasHeight = %d, " +
                        "imageWidth = %d, imageHeight = %d, tx = %d, ty = %d, class = %s",
                canvasWidth, canvasHeight, imageWidth, imageHeight, tx, ty, className);

        throw new IllegalStateException(msg);
    }

    @SuppressWarnings("SameReturnValue")
    public static boolean layerDeleteActionEnabledCheck() {
        DeleteActiveLayerAction action = DeleteActiveLayerAction.INSTANCE;
        if (action == null) {
            return true;
        }

        Optional<Composition> optComp = getActiveComp();
        if (!optComp.isPresent()) {
            return true;
        }
        Composition comp = optComp.get();

        boolean enabled = action.isEnabled();

        int nrLayers = comp.getNrLayers();
        if (enabled) {
            if (nrLayers <= 1) {
                throw new IllegalStateException("enabled, but nrLayers = " + nrLayers);
            }
        } else { // disabled
            if (nrLayers >= 2) {
                throw new IllegalStateException("disabled, but nrLayers = " + nrLayers);
            }
        }
        return true;
    }
}