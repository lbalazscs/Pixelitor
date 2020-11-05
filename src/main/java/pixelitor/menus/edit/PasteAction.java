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

package pixelitor.menus.edit;

import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.Optional;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE;
import static pixelitor.utils.Texts.i18n;

/**
 * Pastes an image from the system clipboard
 */
public class PasteAction extends AbstractAction {
    private final PasteDestination destination;

    public PasteAction(PasteDestination destination) {
        super(i18n(destination.toResourceKey()));

        this.destination = destination;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            getImageFromClipboard().ifPresent(this::pasteImage);
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }

    private void pasteImage(BufferedImage pastedImage) {
        // pastedImage = ImageUtils.toCompatibleImage(pastedImage);

        int type = pastedImage.getType();
        if (type != TYPE_INT_ARGB_PRE) {
            // needs conversion in the case of
            // images pasted from other apps
            // TODO why not convert to sys compatible image?
            pastedImage = ImageUtils.convertToARGB_PRE(pastedImage, true);
        } else {
            // if a layer was pasted from Pixelitor,
            // then we get back here the same object reference
            try {
                pastedImage = ImageUtils.copyImage(pastedImage);
            } catch (IllegalArgumentException ex) {
                WritableRaster raster = pastedImage.getRaster();
                int minX = raster.getMinX();
                int minY = raster.getMinY();
                System.out.println("PasteAction.actionPerformed minX = " + minX + ", minY = " + minY);
                throw ex;
            }
        }

        destination.paste(pastedImage);
    }

    private static Optional<BufferedImage> getImageFromClipboard() {
        Transferable clipboardContents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        if (clipboardContents == null) {
            Messages.showInfo("Paste", "There is nothing to paste.");
            return Optional.empty();
        }

        BufferedImage pastedImage = null;
        if (clipboardContents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            try {
                pastedImage = (BufferedImage) clipboardContents.getTransferData(DataFlavor.imageFlavor);
            } catch (UnsupportedFlavorException | IOException ex) {
                Messages.showException(ex);
            }
        } else {
            Messages.showInfo("Paste", "The clipboard content is not an image.");
            return Optional.empty();
        }
        return Optional.of(pastedImage);
    }
}
