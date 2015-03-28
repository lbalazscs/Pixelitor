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

package pixelitor.menus.file;

import pixelitor.Composition;
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
        OKCancelDialog d = new OKCancelDialog(getSettingsPanel(), "Screen Capture") {
            @Override
            protected void dialogAccepted() {
                capture();
                close();
            }
        };
        d.setVisible(true);
    }

    public JPanel getSettingsPanel() {
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        GridBagHelper gbHelper = new GridBagHelper(p);

        hidePixelitorCB = new JCheckBox();
        hidePixelitorCB.setSelected(true);
        gbHelper.addLabelWithControl("Hide Pixelitor", hidePixelitorCB);

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
            }

            int type = screenCapture.getType();
            if (type != BufferedImage.TYPE_INT_ARGB_PRE) {
                screenCapture = ImageUtils.convertToARGB_PRE(screenCapture, true);
            }

            String name = "Screen Capture " + captureCount;
            Composition comp = Composition.fromImage(screenCapture, null, name);
            window.addComposition(comp);

            captureCount++;
        } catch (Exception ex) {
            Dialogs.showExceptionDialog(ex);
        }
    }
}
