/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
import pixelitor.layers.ImageLayer;
import pixelitor.menus.SelectionActions;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.Optional;

import static pixelitor.ImageComponents.getActiveComp;

/**
 * Consistency checks that run only in developer mode.
 * They are enabled by the Build setting or by the assertions
 */
public final class ConsistencyChecks {
    private ConsistencyChecks() { // do not instantiate
    }

    public static void checkAll() {
        getActiveComp().ifPresent(comp -> {
            selectionCheck(comp);
// TODO commented out because of layer mask problems
//            fadeCheck(comp);
            translationCheck(comp);
            layerDeleteActionEnabledCheck();
        });
    }

    /**
     * Checks whether Fade would work now
     */
    public static boolean fadeCheck(Composition comp) {
        if (!History.canFade()) {
            return true;
        }
        Optional<FadeableEdit> edit = History.getPreviousEditForFade(comp);
        if (edit.isPresent()) {
            Optional<ImageLayer> opt = comp.getActiveImageLayerOpt();
            if (opt.isPresent()) {
                ImageLayer layer = opt.get();
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
        }
        return true;
    }

    private static void selectionCheck(Composition comp) {
        if (!SwingUtilities.isEventDispatchThread()) {
            if (!Build.CURRENT.isPerformanceTest()) {
                throw new IllegalStateException("ConsistencyChecks::selectionCheck: not on EDT");
            }
        }

        if (comp.hasSelection()) {
            if (!SelectionActions.areEnabled()) {
                throw new IllegalStateException(comp.getName()
                        + " has selection, but selection actions are disabled, thread is "
                        + Thread.currentThread().getName());
            }
        }
    }

    public static boolean translationCheck(Composition comp) {
        ImageLayer layer = comp.getActiveImageLayer();
        BufferedImage bufferedImage = layer.getImage();

        int x = -layer.getTranslationX();
        int canvasWidth = comp.getCanvasWidth();
        int imageWidth = bufferedImage.getWidth();
        if (x + canvasWidth > imageWidth + 1) { // allow one pixel difference for rounding effects
            throw new IllegalStateException("x = " + x + ", canvasWidth = " + canvasWidth + ", imageWidth = " + imageWidth + ", comp = " + comp.getName());
        }

        int y = -layer.getTranslationY();
        int canvasHeight = comp.getCanvasHeight();
        int imageHeight = bufferedImage.getHeight();

        if (y + canvasHeight > imageHeight + 1) {
            throw new IllegalStateException("y = " + y + ", canvasHeight = " + canvasHeight + ", imageHeight = " + imageHeight + ", comp = " + comp.getName());
        }

        return true;
    }

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
                throw new IllegalStateException("nrLayers = " + nrLayers);
            }
        } else { // disabled
            if (nrLayers >= 2) {
                throw new IllegalStateException("nrLayers = " + nrLayers);
            }
        }
        return true;
    }
}