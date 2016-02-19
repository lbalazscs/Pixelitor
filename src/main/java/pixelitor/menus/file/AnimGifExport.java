/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.Composition;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.utils.OKCancelDialog;
import pixelitor.io.FileChoosers;
import pixelitor.io.LayerAnimationFrames;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.FlowLayout;
import java.io.File;

public class AnimGifExport {
    private AnimGifExport() {
    }

    public static void start(JFrame dialogParent) {
        Composition activeComp = ImageComponents.getActiveCompOrNull();
        int nrLayers = activeComp.getNrLayers();
        if(nrLayers < 2) {
            Messages.showInfo("Only one layer",
                    "Animation frames are based on the layers of the image.\n" +
                            activeComp.getName() + " has only one layer.");
            return;
        }

        ExportPanel p = new ExportPanel(activeComp.getNrLayers());
        OKCancelDialog d = new OKCancelDialog(p, dialogParent, "Export Animated GIF", "Export", "Cancel", false) {
            @Override
            protected void dialogAccepted() {
                close();
                export(activeComp, p.getDelayMillis(), p.isPingPong());
            }
        };
        d.setVisible(true);
    }

    private static void export(Composition activeComp, int delayMillis, boolean pingPong) {
        File file = FileChoosers.selectSaveFileForSpecificFormat(FileChoosers.gifFilter);
        if (file != null) {
            LayerAnimationFrames animation = new LayerAnimationFrames(activeComp,
                    delayMillis, pingPong);
            animation.saveToFile(file);
            Messages.showFileSavedMessage(file);
        }
    }

    static class ExportPanel extends JPanel {
        private final JTextField delayTF;
        private final JCheckBox pingPongCB;

        public ExportPanel(int nrLayers) {
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

        private int getDelayMillis() {
            return Integer.parseInt(delayTF.getText());
        }

        private boolean isPingPong() {
            return pingPongCB.isSelected();
        }
    }
}
