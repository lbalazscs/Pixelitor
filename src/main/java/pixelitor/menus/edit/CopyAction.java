/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.utils.OpenImageEnabledAction;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Result;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;

import static pixelitor.utils.Texts.i18n;

/**
 * Copies the image given by the {@link CopySource}
 * to the system clipboard
 */
public class CopyAction extends OpenImageEnabledAction {
    public static final CopyAction COPY_LAYER = new CopyAction(CopySource.LAYER_OR_MASK);
    public static final CopyAction COPY_COMPOSITE = new CopyAction(CopySource.COMPOSITE);

    private final CopySource source;

    private CopyAction(CopySource source) {
        super(i18n(source.toResourceKey()));
        this.source = source;
    }

    @Override
    public void onClick() {
        OpenImages.onActiveComp(this::copyToClipboard);
    }

    private void copyToClipboard(Composition comp) {
        Result<BufferedImage, String> result = source.getImage(comp);
        if (!result.isOK()) {
            String msg = "Could not copy because " + result.errorDetail();
            Dialogs.showErrorDialog("Error", msg);
            return;
        }
        BufferedImage activeImage = result.get();
        // make a copy, because otherwise changing the image
        // will also change the clipboard contents
        BufferedImage copy = ImageUtils.copySubImage(activeImage);
        Transferable imageTransferable = new ImageTransferable(copy);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        try {
            // TODO JDK bug? a stack trace is printed, but the image is copied.
            clipboard.setContents(imageTransferable, null);
        } catch (IllegalStateException e) {
            // ignore, see issue #181
        }
    }
}

