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

import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.gui.utils.Dialogs;
import pixelitor.utils.Messages;
import pixelitor.utils.Result;
import pixelitor.utils.Texts;
import pixelitor.utils.debug.DebugNodes;

import javax.swing.*;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;

import static java.lang.String.format;

/**
 * Copies the image given by the {@link CopySource}
 * to the system clipboard
 */
public class CopyAction extends AbstractAction {
    private final CopySource source;

    public CopyAction(CopySource source) {
        super(Texts.get(source.toResourceKey()));
        this.source = source;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            OpenImages.onActiveComp(this::copyToClipboard);
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }

    private void copyToClipboard(Composition comp) {
        Result<BufferedImage, String> result = source.getImage(comp);
        if (!result.isOK()) {
            String msg = "Could not copy because " + result.getError();
            Dialogs.showErrorDialog("Error", msg);
            return;
        }
        BufferedImage activeImage = result.get();
        Transferable imageTransferable = new ImageTransferable(activeImage);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        try {
            // Sun Jan 04 08:00:30 CET 2015, RandomGUITest:
            // java.awt.image.RasterFormatException: Incorrect scanline stride: 12
            // at sun.awt.image.ByteComponentRaster.verify(ByteComponentRaster.java:894)
            // at sun.awt.image.ByteComponentRaster.<init>(ByteComponentRaster.java:201)
            // https://bugs.openjdk.java.net/browse/JDK-8041558

            // TODO A different issue in September 2018: OpenJDK 11 prints an
            // exception stack trace here, but the image copied.
            clipboard.setContents(imageTransferable, null);
        } catch (RasterFormatException rfe) {
            rfe.printStackTrace();
            var node = DebugNodes.createBufferedImageNode("active image", activeImage);
            String s = node.toDetailedString();
            System.out.println(format(
                    "CopyAction: RasterFormatException in actionPerformed: %s", s));
        }
    }
}

