/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Views;
import pixelitor.gui.View;
import pixelitor.gui.utils.NamedAction;
import pixelitor.utils.*;
import pixelitor.utils.Error;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;

import static pixelitor.utils.Texts.i18n;

/**
 * Action for pasting an image from the system clipboard to a given target.
 */
public class PasteAction extends NamedAction implements ViewActivationListener {
    private final PasteTarget pasteTarget;

    public PasteAction(PasteTarget pasteTarget) {
        super(i18n(pasteTarget.getResourceKey()));

        this.pasteTarget = pasteTarget;

        if (pasteTarget.requiresOpenView()) {
            Views.addActivationListener(this);
            setEnabled(false); // disabled by default until a view is opened
        }
    }

    @Override
    protected void onClick(ActionEvent e) {
        switch (retrieveClipboardImage()) {
            case Success<BufferedImage, ?>(BufferedImage img) -> pasteTarget.paste(img);
            case Error<?, String>(String errorMsg) -> Messages.showInfo("Paste Error", errorMsg);
        }
    }

    private static Result<BufferedImage, String> retrieveClipboardImage() {
        Transferable clipboardContents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);

        if (clipboardContents == null) {
            return Result.error("The clipboard is empty. Nothing to paste.");
        }

        if (!clipboardContents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            return Result.error("The clipboard content isn't an image.");
        }

        try {
            BufferedImage pastedImage = (BufferedImage)
                clipboardContents.getTransferData(DataFlavor.imageFlavor);
            return Result.success(pastedImage);
        } catch (UnsupportedFlavorException | IOException ex) {
            Messages.showException(ex);
            return Result.error(ex.getMessage());
        }
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        assert pasteTarget.requiresOpenView();
        setEnabled(true);
    }

    @Override
    public void allViewsClosed() {
        assert pasteTarget.requiresOpenView();
        setEnabled(false);
    }
}
