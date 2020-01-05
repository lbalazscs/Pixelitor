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

import org.jdesktop.swingx.VerticalLayout;
import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.io.FileChoosers;
import pixelitor.io.LayerAnimation;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.FlowLayout;
import java.io.File;

import static java.awt.FlowLayout.LEFT;
import static java.lang.Integer.parseInt;
import static javax.swing.BorderFactory.createEmptyBorder;
import static pixelitor.io.FileChoosers.gifFilter;

public class AnimGifExport {
    private AnimGifExport() {
    }

    public static void start(JFrame dialogOwner) {
        var comp = OpenImages.getActiveComp();
        if (comp.getNumLayers() < 2) {
            Messages.showInfo("Only one layer",
                    "Animation frames are based on the layers of the image.\n" +
                            comp.getName() + " has only one layer.");
            return;
        }

        var p = new ExportPanel(comp.getNumLayers());
        new DialogBuilder()
                .title("Export Animated GIF")
                .owner(dialogOwner)
                .content(p)
                .okText("Export")
                .okAction(() -> export(comp, p.getDelayMillis(), p.isPingPong()))
                .show();
    }

    private static void export(Composition activeComp, int delayMillis, boolean pingPong) {
        File file = FileChoosers.selectSaveFileForSpecificFormat(gifFilter);
        if (file != null) {
            var animation = new LayerAnimation(activeComp,
                    delayMillis, pingPong);
            animation.saveToFile(file);
            Messages.showFileSavedMessage(file);
        }
    }

    static class ExportPanel extends JPanel {
        private final JTextField delayTF;
        private final JCheckBox pingPongCB;

        public ExportPanel(int nrLayers) {
            setBorder(createEmptyBorder(10, 10, 10, 10));
            setLayout(new VerticalLayout(10));

            add(new JLabel(" Animation frames are based on the layers of the image. "));

            var settingsPanel = new JPanel();
            settingsPanel.setLayout(new FlowLayout(LEFT, 10, 10));
            settingsPanel.add(new JLabel("Delay Between Frames (Milliseconds):"));
            delayTF = new JTextField("200", 4);
            settingsPanel.add(delayTF);

            add(settingsPanel);

            if (nrLayers > 2) {
                pingPongCB = new JCheckBox("Ping Pong Animation");
            } else {
                pingPongCB = new JCheckBox("Ping Pong Animation (min 3 layers needed)");
                pingPongCB.setEnabled(false);
            }
            add(pingPongCB);
        }

        private int getDelayMillis() {
            return parseInt(delayTF.getText().trim());
        }

        private boolean isPingPong() {
            return pingPongCB.isSelected();
        }
    }
}
