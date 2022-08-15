/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.utils.Messages;
import pixelitor.utils.ViewActivationListener;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;

import static pixelitor.utils.Texts.i18n;

/**
 * Pastes an image from the system clipboard
 */
public class PasteAction extends NamedAction.Checked implements ViewActivationListener {
    private final PasteDestination destination;

    public PasteAction(PasteDestination destination) {
        super(i18n(destination.getResourceKey()));

        this.destination = destination;
        if (destination.requiresOpenView()) {
            Views.addActivationListener(this);
            setEnabled(false);
        }
    }

    @Override
    protected void onClick() {
        getImageFromClipboard().ifPresent(this::pasteImage);
    }

    private void pasteImage(BufferedImage pastedImage) {
        // the pasted image could have an unexpected type
        // (such as RGB, without transparency ), but the
        // code executing later is responsible for converting it.
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
        return Optional.ofNullable(pastedImage);
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        assert destination.requiresOpenView();
        setEnabled(true);
    }

    @Override
    public void allViewsClosed() {
        assert destination.requiresOpenView();
        setEnabled(false);
    }
}
