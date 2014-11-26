/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
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
package pixelitor.menus.file;

import pixelitor.PixelitorWindow;
import pixelitor.utils.Dialogs;
import pixelitor.utils.GridBagHelper;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.OKCancelDialog;

import javax.swing.*;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

/**
 *
 */
public class ScreenCaptureAction extends AbstractAction {
    private JCheckBox hidePixelitorCB;
    private static int captureCount = 1;

    public ScreenCaptureAction() {
        super("Screen Capture...");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        OKCancelDialog d = new OKCancelDialog(getSettingsPanel(), "Screen Capture") {
            @Override
            protected void dialogAccepted() {
                super.dialogAccepted();
                capture();
                dispose();
//                setVisible(false);
            }

            @Override
            protected void dialogCanceled() {
                super.dialogCanceled();
                dispose();
            }
        };
        d.setVisible(true);
    }

    public JPanel getSettingsPanel() {
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());

        GridBagHelper.addLabel(p, "Hide Pixelitor", 0, 0);
        hidePixelitorCB = new JCheckBox();
        hidePixelitorCB.setSelected(true);
        GridBagHelper.addControl(p, hidePixelitorCB);

        return p;
    }

    private void capture() {
        try {
            Robot robot = new Robot();
            PixelitorWindow window = PixelitorWindow.getInstance();

            boolean hidePixelitor = hidePixelitorCB.isSelected();
            if (hidePixelitor) {
//                window.setVisible(false);
                window.iconify();
                Thread.sleep(500);
            }

            BufferedImage screenCapture = robot.createScreenCapture(
                    new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));

            if (hidePixelitor) {
                window.deiconify();
//                window.setVisible(true);
//                Thread.sleep(1000);
            }

            int type = screenCapture.getType();
            if (type != BufferedImage.TYPE_INT_ARGB_PRE) {
                screenCapture = ImageUtils.convertToARGB_PRE(screenCapture, true);
            }

            String name = "Screen Capture " + captureCount;
            window.addNewImage(screenCapture, null, name);
            captureCount++;
        } catch (Exception ex) {
            Dialogs.showExceptionDialog(ex);
        }

    }
}
