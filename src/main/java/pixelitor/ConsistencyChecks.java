/*
 * Copyright (c) 2015 Laszlo Balazs-Csiki
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
import pixelitor.utils.Optional;

import javax.swing.*;
import java.awt.image.BufferedImage;

/**
 * Consistency checks that run only in developer mode.
 * They are enabled by the Build setting or by the assertions
 */
public final class ConsistencyChecks {
    /**
     * Utility class with static methods
     */
    private ConsistencyChecks() {
    }

    public static void checkAll() {
        Optional<Composition> opt = ImageComponents.getActiveComp();
        if (opt.isPresent()) {
            Composition comp = opt.get();
            selectionCheck(comp);
            fadeCheck(comp);
            translationCheck(comp);
            layerDeleteActionEnabledCheck();
        }
    }

    /**
     * Checks whether Fade would work now
     */
    public static boolean fadeCheck(Composition comp) {
        Optional<FadeableEdit> edit = History.getPreviousEditForFade(comp);
        if (edit.isPresent()) {  // can fade
            ImageLayer layer = comp.getActiveImageLayer();
            if (layer != null) {
                BufferedImage current = layer.getImageOrSubImageIfSelected(false, true);

                BufferedImage previous = edit.get().getBackupImage();
                if(previous == null) {
                    // soft reference expired
                    return true;
                }

                boolean differentWidth = current.getWidth() != previous.getWidth();
                boolean differentHeight = current.getHeight() != previous.getHeight();
                if (differentWidth || differentHeight) {

                    String lastFadeableOp = History.getLastPresentationName();
                    throw new IllegalStateException("'Fade " + lastFadeableOp + "' would not work now:\nFadeableEdit class = " + edit.getClass().getName() + "\n" +
                            " current selected dimensions: width = " + current.getWidth() + ", height = " + current.getHeight() +
                            " history dimensions: width = " + previous.getWidth() + ", height = " + previous.getHeight()
                    );
                }
            }
        }
        return true;
    }

    private static void selectionCheck(Composition comp) {
        if(!SwingUtilities.isEventDispatchThread()) {
            if(!Build.CURRENT.isPerformanceTest()) {
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
        if (layer != null) {
            BufferedImage bufferedImage = layer.getImage();

            int x = -layer.getTranslationX();
            int canvasWidth = comp.getCanvasWidth();
            int imageWidth = bufferedImage.getWidth();
            if (x + canvasWidth > imageWidth) {
                throw new IllegalStateException("x = " + x + ", canvasWidth = " + canvasWidth + ", imageWidth = " + imageWidth + ", comp = " + comp.getName());
            }

            int y = -layer.getTranslationY();
            int canvasHeight = comp.getCanvasHeight();
            int imageHeight = bufferedImage.getHeight();

            if (y + canvasHeight > imageHeight) {
                throw new IllegalStateException("y = " + y + ", canvasHeight = " + canvasHeight + ", imageHeight = " + imageHeight + ", comp = " + comp.getName());
            }
        }
        return true;
    }

    public static boolean layerDeleteActionEnabledCheck() {
        DeleteActiveLayerAction action = DeleteActiveLayerAction.INSTANCE;
        if (action == null) {
            return true;
        }
        Composition comp = ImageComponents.getActiveComp().get();
        if (comp == null) {
            return true;
        }

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