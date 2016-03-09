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

package pixelitor.menus.edit;

import pixelitor.Composition;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.utils.Dialogs;
import pixelitor.layers.AdjustmentLayer;
import pixelitor.layers.Layer;
import pixelitor.utils.Messages;
import pixelitor.utils.debug.BufferedImageNode;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;

/**
 * Copies an image to the system clipboard
 */
public class CopyAction extends AbstractAction {
    private final CopySource source;

    public CopyAction(CopySource source) {
        super(source.toString());
        this.source = source;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            ImageComponent ic = ImageComponents.getActiveIC();
            Composition comp = ic.getComp();

            if (source == CopySource.LAYER) {
                Layer layer = comp.getActiveLayer();
                if (layer instanceof AdjustmentLayer) {
                    if (!RandomGUITest.isRunning()) {
                        Dialogs.showErrorDialog("Adjustment Layer", "Adjustment layers cannot be copied");
                    }
                    return;
                }
            }

            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            BufferedImage activeImage = source.getImage(comp);
            Transferable imageTransferable = new ImageTransferable(activeImage);

            try {
                // Sun Jan 04 08:00:30 CET 2015, RandomGUITest:
                // java.awt.image.RasterFormatException: Incorrect scanline stride: 12
                // at sun.awt.image.ByteComponentRaster.verify(ByteComponentRaster.java:894)
                // at sun.awt.image.ByteComponentRaster.<init>(ByteComponentRaster.java:201)
                // https://bugs.openjdk.java.net/browse/JDK-8041558
                clipboard.setContents(imageTransferable, null);
            } catch (RasterFormatException rfe) {
                rfe.printStackTrace();
                BufferedImageNode node = new BufferedImageNode(activeImage);
                String s = node.toDetailedString();
                System.out.println(String.format("CopyAction::RasterFormatException in actionPerformed: s = '%s'", s));
            }
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }
}

