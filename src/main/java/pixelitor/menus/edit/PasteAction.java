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
package pixelitor.menus.edit;

import pixelitor.Composition;
import pixelitor.ImageComponents;
import pixelitor.PixelitorWindow;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.utils.Dialogs;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;

/**
 * Pastes an image from the system clipboard to a new image
 */
public class PasteAction extends AbstractAction {
    private static int pastedCount = 1;

    private boolean pasteAsNewLayer = false;

    public PasteAction(boolean pasteAsNewLayer) {
        super(pasteAsNewLayer ? "Paste as New Layer" : "Paste as New Image");
        this.pasteAsNewLayer = pasteAsNewLayer;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            BufferedImage pastedImage = getImageFromClipboard();
            if (pastedImage != null) {
                // pastedImage = ImageUtils.transformToCompatibleImage(pastedImage);

                int type = pastedImage.getType();
                if (type != BufferedImage.TYPE_INT_ARGB_PRE) {
                    // needs conversion in the case of
                    // images pasted from other apps
                    pastedImage = ImageUtils.convertToARGB_PRE(pastedImage, true);
                } else {
                    // if a layer was pasted from Pixelitor,
                    // then we get back here the same object reference
                    WritableRaster raster = pastedImage.getRaster();
                    int minX = raster.getMinX();
                    int minY = raster.getMinY();

                    try {
                        pastedImage = ImageUtils.copyImage(pastedImage);
                    } catch (IllegalArgumentException ex) {
                        System.out.println("PasteAction.actionPerformed minX = " + minX + ", minY = " + minY);
                        throw ex;
                    }
                }

                if (pasteAsNewLayer) {
                    Composition comp = ImageComponents.getActiveComp().get();
                    Layer newLayer = new ImageLayer(comp, pastedImage, "Pasted Layer", comp.getCanvasWidth(), comp.getCanvasHeight());

                    comp.addLayer(newLayer, true, true, false);
                } else { // paste as new image
                    String title = "Pasted Image " + pastedCount;
                    PixelitorWindow.getInstance().addNewImage(pastedImage, null, title);
                    pastedCount++;
                }
            }
        } catch (Exception ex) {
            Dialogs.showExceptionDialog(ex);
        }
    }

    private static BufferedImage getImageFromClipboard() {
        Transferable clipboardContents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        if (clipboardContents == null) {
            Dialogs.showInfoDialog("Paste", "There is nothing to paste.");
            return null;
        }

        BufferedImage pastedImage = null;
        if (clipboardContents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            try {
                pastedImage = (BufferedImage) clipboardContents.getTransferData(DataFlavor.imageFlavor);
            } catch (UnsupportedFlavorException | IOException ex) {
                Dialogs.showExceptionDialog(ex);
            }
        } else {
            Dialogs.showInfoDialog("Paste", "The clipboard content is not an image.");
            return null;
        }
        return pastedImage;
    }
}
