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

package pixelitor.menus.edit;

import pixelitor.Composition;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.ViewEnabledAction;
import pixelitor.utils.Error;
import pixelitor.utils.*;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;

import static pixelitor.utils.Threads.onEDT;

/**
 * The action of copying an image from a specified {@link CopySource}
 * to the system clipboard.
 */
public class CopyAction extends ViewEnabledAction {
    public static final CopyAction COPY_LAYER = new CopyAction(CopySource.LAYER_OR_MASK);
    public static final CopyAction COPY_COMPOSITE = new CopyAction(CopySource.COMPOSITE);

    private CopyAction(CopySource source) {
        super(source.getName(), comp -> copy(comp, source));
    }

    private static void copy(Composition comp, CopySource source) {
        switch (source.getImage(comp)) {
            case Success<BufferedImage, ?>(var img) -> startAsyncCopy(img);
            case Error<?, String>(var errorMsg) -> Dialogs.showErrorDialog(
                "Copy Failed", "Could not copy because " + errorMsg);
        }
    }

    private static void startAsyncCopy(BufferedImage img) {
        // make a copy, because otherwise changing the image
        // will also change the clipboard contents
        BufferedImage imageCopy = ImageUtils.copySubImage(img);

        ProgressHandler progressHandler = Messages.startProgress("Copying to clipboard", -1);
        CompletableFuture.runAsync(() -> transferToClipboard(imageCopy))
            .thenRunAsync(() -> postCopyEDTActions(progressHandler), onEDT)
            .exceptionally(Messages::showExceptionOnEDT);
    }

    private static void transferToClipboard(BufferedImage image) {
        Transferable imageTransferable = new ImageTransferable(image);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        try {
            clipboard.setContents(imageTransferable, null);
        } catch (IllegalStateException e) {
            // ignore, see issue #181
        }
    }

    private static void postCopyEDTActions(ProgressHandler handler) {
        handler.stopProgress();
        Messages.showStatusMessage("Image copied to the clipboard.");
    }
}

