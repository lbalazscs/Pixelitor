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

package pixelitor.menus.file;

import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * The screen capture.
 */
public class ScreenCaptureAction extends AbstractAction {
    private JCheckBox hidePixelitorCB;
    private static int captureCount = 1;

    public ScreenCaptureAction() {
        super("Screen Capture...");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new DialogBuilder()
            .content(getSettingsPanel())
            .title("Screen Capture")
            .okAction(this::capture)
            .show();
    }

    private JPanel getSettingsPanel() {
        var p = new JPanel();
        p.setLayout(new GridBagLayout());
        var gbh = new GridBagHelper(p);

        hidePixelitorCB = new JCheckBox();
        hidePixelitorCB.setSelected(true);
        gbh.addLabelAndControl("Hide Pixelitor", hidePixelitorCB);

        return p;
    }

    private void capture() {
        try {
            boolean hide = hidePixelitor();
            if (hide) {
                hideApp();
            }

            BufferedImage screenCapture = createCapturedImage();

            if (hide) {
                unHideApp();
            }

            addAsNewComp(screenCapture);
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }

    private static BufferedImage createCapturedImage() throws AWTException {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        return new Robot().createScreenCapture(new Rectangle(screenSize));
    }

    private static void hideApp() {
        PixelitorWindow.getInstance().iconify();
        Utils.sleep(500, TimeUnit.MILLISECONDS);
    }

    private static void unHideApp() {
        PixelitorWindow.getInstance().deiconify();
    }

    private boolean hidePixelitor() {
        return hidePixelitorCB.isSelected();
    }

    private static void addAsNewComp(BufferedImage screenCapture) {
        // necessary even though Composition.fromImage later calls
        // toSysCompatibleImage, because without this, the image will
        // be RGB, with no support for transparency
        int type = screenCapture.getType();
        if (type != TYPE_INT_ARGB) {
            screenCapture = ImageUtils.convertToARGB(screenCapture, true);
        }

        String name = "Screen Capture " + captureCount++;
        var comp = Composition.fromImage(screenCapture, null, name);
        comp.setDirty(true);
        OpenImages.addAsNewComp(comp);
    }
}
