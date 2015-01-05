/*
 * Copyright (c) 2015 Laszlo Balazs-Csiki
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

import org.jdesktop.swingx.VerticalLayout;
import pixelitor.AppLogic;
import pixelitor.Composition;
import pixelitor.ImageComponents;
import pixelitor.io.FileChoosers;
import pixelitor.io.LayerAnimationFrames;
import pixelitor.utils.Dialogs;
import pixelitor.utils.OKCancelDialog;

import javax.swing.*;
import java.awt.FlowLayout;
import java.io.File;

public class AnimGifExportPanel extends JPanel {
    private JTextField delayTF;
    private JCheckBox pingPongCB;

    public AnimGifExportPanel(int nrLayers) {
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setLayout(new VerticalLayout(10));

        add(new JLabel(" Animation frames are based on the layers of the image. "));

        JPanel settings = new JPanel();
        settings.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        settings.add(new JLabel("Delay Between Frames (Milliseconds):"));
        delayTF = new JTextField("200", 4);
        settings.add(delayTF);

        add(settings);

        if (nrLayers > 2) {
            pingPongCB = new JCheckBox("Ping Pong Animation");
        } else {
            pingPongCB = new JCheckBox("Ping Pong Animation (min 3 layers needed)");
            pingPongCB.setEnabled(false);
        }
        add(pingPongCB);
    }

    public int getDelayMillis() {
        return Integer.parseInt(delayTF.getText());
    }

    public boolean isPingPong() {
        return pingPongCB.isSelected();
    }

    public static void showInDialog(JFrame parent) {
        final Composition activeComp = ImageComponents.getActiveComp().get();
        int nrLayers = activeComp.getNrLayers();
        if(nrLayers < 2) {
            Dialogs.showInfoDialog("Only one layer",
                    "Animation frames are based on the layers of the image.\n" +
                            activeComp.getName() + " has only one layer.");
            return;
        }

        final AnimGifExportPanel p = new AnimGifExportPanel(activeComp.getNrLayers());
        OKCancelDialog d = new OKCancelDialog(p, parent, "Export Animated GIF", "Export", "Cancel", false) {
            @Override
            protected void dialogAccepted() {
                close();
                File file = FileChoosers.selectSaveFileForSpecificFormat(FileChoosers.gifFilter);
                if(file != null) {
                    LayerAnimationFrames animation = new LayerAnimationFrames(activeComp,
                            p.getDelayMillis(), p.isPingPong());
                    animation.saveToFile(file);
                    AppLogic.showFileSavedMessage(file);
                }
            }
        };
        d.setVisible(true);
    }
}
