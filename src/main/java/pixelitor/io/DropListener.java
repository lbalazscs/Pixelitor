/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.gui.utils.Dialogs;
import pixelitor.utils.Messages;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static java.lang.String.format;

/**
 * Manages external files drag-and-dropped on the app
 */
public class DropListener extends DropTargetAdapter {
    private final DropAction dropAction;

    /**
     * Defines how the dropped files should be handled.
     */
    public enum DropAction {
        /**
         * Opens each dropped file as a new image.
         */
        OPEN_AS_NEW_IMAGES {
            @Override
            protected void handleDrop(List<File> files, Component target) {
                for (File file : files) {
                    openFileAsNewImage(file, target);
                }
            }
        },
        /**
         * Adds each dropped file as a new layer in the active composition.
         */
        ADD_AS_NEW_LAYERS {
            @Override
            protected void handleDrop(List<File> files, Component target) {
                Composition comp = Views.getActiveComp();
                if (comp == null) {
                    // If there is no active composition,
                    // fall back to opening the files as new images
                    OPEN_AS_NEW_IMAGES.handleDrop(files, target);
                    return;
                }

                for (File file : files) {
                    addFileAsNewLayer(file, comp, target);
                }
            }
        };

        protected abstract void handleDrop(List<File> files, Component target);
    }

    public DropListener(DropAction dropAction) {
        this.dropAction = dropAction;
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
    public void drop(DropTargetDropEvent dropEvent) {
        Transferable transferable = dropEvent.getTransferable();
        DataFlavor[] flavors = transferable.getTransferDataFlavors();

        for (DataFlavor flavor : flavors) {
            if (flavor.equals(DataFlavor.imageFlavor)) {
                // it is unclear how this could be used
                dropEvent.rejectDrop();
                return;
            }

            if (flavor.isFlavorJavaFileListType()) {
                // this is where we get after dropping a file or directory
                dropEvent.acceptDrop(DnDConstants.ACTION_COPY);
                try {
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) transferable.getTransferData(flavor);
                    dropAction.handleDrop(files, dropEvent.getDropTargetContext().getComponent());
                } catch (UnsupportedFlavorException | IOException ex) {
                    Messages.showException(ex);
                    dropEvent.rejectDrop();
                }
                dropEvent.dropComplete(true);
                return;
            }
        }

        // No recognized data flavors, reject the drop.
        dropEvent.rejectDrop();
    }

    private static void openFileAsNewImage(File file, Component target) {
        if (file.isDirectory()) {
            String question = format("<html>You have dropped the folder <b>\"%s\"</b>."
                + "<br>Do you want to open all image files inside it?", file.getName());

            if (Dialogs.showYesNoQuestionDialog(target, "Dropped Folder", question)) {
                FileIO.openAllSupportedImagesInDir(file);
            }
        } else if (file.isFile()) {
            if (!Files.isReadable(file.toPath())) {
                Dialogs.showFileNotReadableError(target, file);
                return;
            }
            FileIO.openFileAsync(file, true);
        }
    }

    private static void addFileAsNewLayer(File file, Composition comp, Component target) {
        if (file.isDirectory()) {
            String question = format("You have dropped the folder \"%s\".\n" +
                    "Do you want all image files inside it to be added as layers to \"%s\"?",
                file.getName(), comp.getName());

            if (Dialogs.showYesNoQuestionDialog(target, "Dropped Folder", question)) {
                FileIO.addAllImagesInDirAsLayers(file, comp);
            }
        } else if (file.isFile()) {
            if (!Files.isReadable(file.toPath())) {
                Dialogs.showFileNotReadableError(target, file);
                return;
            }
            FileIO.addNewImageLayerAsync(file, comp);
        }
    }
}
