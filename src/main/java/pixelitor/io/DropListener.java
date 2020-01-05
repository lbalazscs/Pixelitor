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

package pixelitor.io;

import pixelitor.OpenImages;
import pixelitor.gui.utils.Dialogs;
import pixelitor.utils.Messages;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static java.lang.String.format;

/**
 * Manages external files drag-and-dropped on the app
 */
public class DropListener extends DropTargetAdapter {
    private final Destination destination;

    /**
     * Determines what happens with the dropped files
     */
    public enum Destination {
        /**
         * Open the dropped files as new images
         */
        NEW_IMAGES {
            @Override
            public void handleDrop(List<File> list) {
                for (File file : list) {
                    if (file.isDirectory()) {
                        String question = format("<html>You have dropped the folder <b>\"%s\"</b>." +
                                "<br>Do you want to open all image files inside it?", file.getName());

                        if (Dialogs.showYesNoQuestionDialog("Question", question)) {
                            OpenSave.openAllImagesInDir(file);
                        }
                    } else if (file.isFile()) {
                        OpenSave.openFileAsync(file);
                    }
                }
            }
        },
        /**
         * Open the dropped files as new image layers in the active composition
         */
        NEW_LAYERS {
            @Override
            public void handleDrop(List<File> list) {
                var comp = OpenImages.getActiveComp();
                if (comp == null) {
                    // if there is no active composition,
                    // fall back to opening the files as new images
                    NEW_IMAGES.handleDrop(list);
                    return;
                }

                for (File file : list) {
                    if (file.isDirectory()) {
                        String question = format("You have dropped the folder \"%s\".\n" +
                                "Do you want all image files inside it to be added as layers to "
                                + comp.getName() + "?", file.getName());

                        if (Dialogs.showYesNoQuestionDialog("Question", question)) {
                            OpenSave.addAsLayersAllImagesInDir(file, comp);
                        }
                    } else if (file.isFile()) {
                        OpenSave.loadToNewImageLayerAsync(file, comp);
                    }
                }
            }
        };

        public abstract void handleDrop(List<File> list);
    }

    public DropListener(Destination destination) {
        this.destination = destination;
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        handleOngoingDrag(dtde);
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        handleOngoingDrag(dtde);
    }

    private static void handleOngoingDrag(DropTargetDragEvent dtde) {
        if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY);
        } else {
            dtde.rejectDrag();
        }
    }

    @Override
    public void drop(DropTargetDropEvent e) {
        Transferable transferable = e.getTransferable();
        DataFlavor[] flavors = transferable.getTransferDataFlavors();
        for (DataFlavor flavor : flavors) {
            if (flavor.equals(DataFlavor.imageFlavor)) {
                // it is unclear how this could be used
                e.rejectDrop();
                return;
            }
            if (flavor.isFlavorJavaFileListType()) {
                // this is where we get after dropping a file or directory
                e.acceptDrop(DnDConstants.ACTION_COPY);

                try {
                    @SuppressWarnings("unchecked")
                    List<File> list = (List<File>) transferable.getTransferData(flavor);
                    destination.handleDrop(list);
                } catch (UnsupportedFlavorException | IOException ex) {
                    Messages.showException(ex);
                    e.rejectDrop();
                }
                e.dropComplete(true);
                return;
            }
        }

        // DataFlavor not recognized
        e.rejectDrop();
    }
}
