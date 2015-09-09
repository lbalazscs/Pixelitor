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

package pixelitor.io;

import pixelitor.PixelitorWindow;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Manages external files dropped on the JDesktopPane
 */
public class DropListener extends DropTargetAdapter {
    public DropListener() {
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        Transferable transferable = dtde.getTransferable();
        DataFlavor[] flavors = transferable.getTransferDataFlavors();
        for (DataFlavor flavor : flavors) {
            if (flavor.equals(DataFlavor.imageFlavor)) {
                return;
            }
            if (flavor.isFlavorJavaFileListType()) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY);

                try {
                    @SuppressWarnings("unchecked")
                    List<File> list = (List<File>) transferable.getTransferData(flavor);
                    dropFiles(list);
                } catch (UnsupportedFlavorException | IOException e) {
                    Messages.showException(e);
                    dtde.rejectDrop();
                }
                dtde.dropComplete(true);
                return;
            }
        }

        // DataFlavor not recognized
        dtde.rejectDrop();
    }

    private static void dropFiles(List<File> list) {
        for (File file : list) {
            if (file.isDirectory()) {
                String question = String.format("You have dropped the folder \"%s\". " +
                        "Do you want to open all image files inside it?", file.getName());
                int answer = JOptionPane.showConfirmDialog(PixelitorWindow.getInstance(),
                        question,
                        "Question", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (answer == JOptionPane.YES_OPTION) {
                    OpenSaveManager.openAllImagesInDir(file);
                } else if (answer == JOptionPane.NO_OPTION) {
                    // do nothing
                }
            } else if (file.isFile()) {
                OpenSaveManager.openFile(file);
            }
        }
    }
}
