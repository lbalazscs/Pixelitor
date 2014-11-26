/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.menus.edit;

import pixelitor.utils.Dialogs;

import javax.swing.*;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

/**
 * Copies the the active image to the system clipboard
 */
public class CopyAction extends AbstractAction {
    private final CopyType type;

    public CopyAction(CopyType type) {
        super(type.toString());
        this.type = type;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            BufferedImage activeImage = type.getCopySource();
            Transferable imageTransferable = new ImageTransferable(activeImage);
            clipboard.setContents(imageTransferable, null);
        } catch (Exception ex) {
            Dialogs.showExceptionDialog(ex);
        }
    }
}

