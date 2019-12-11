/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.OpenComps;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.Dialogs;
import pixelitor.io.FileChoosers;
import pixelitor.io.OpenRaster;

import javax.swing.*;
import java.io.File;

import static javax.swing.JOptionPane.CANCEL_OPTION;
import static javax.swing.JOptionPane.CLOSED_OPTION;
import static javax.swing.JOptionPane.NO_OPTION;
import static javax.swing.JOptionPane.QUESTION_MESSAGE;
import static javax.swing.JOptionPane.YES_OPTION;

/**
 * The configuration GUI for the export in OpenRaster format
 */
public class OpenRasterExportPanel extends JPanel {
    private final JCheckBox mergedLayersCB;

    private OpenRasterExportPanel() {
        mergedLayersCB = new JCheckBox("Add merged image? (Useful only for image viewers)", false);
        add(mergedLayersCB);
    }

    private boolean exportMergedImage() {
        return mergedLayersCB.isSelected();
    }

    public static void showInDialog(JFrame owner) {
        Composition comp = OpenComps.getActiveCompOrNull();
        if (comp.getNumLayers() < 2) {
            String msg = String.format(
                    "<html><b>%s</b> has only one layer." +
                            "<br>Are you sure that you want to export it in a layered format?",
                    comp.getName());
            boolean exportAnyway = Dialogs.showYesNoQuestionDialog(
                    "Only one layer", msg);
            if (!exportAnyway) {
                return;
            }
        }
        if (comp.getNumTextLayers() > 0) {
            String msg = String.format(
                    "<html><b>%s</b> has text layer(s)." +
                            "<br>Text layers can be preserved only in the PXC format." +
                            "<br>In the OpenRaster format they can be rasterized or ignored.",
                    comp.getName());
            String[] options = {"Rasterize", "Ignore", "Cancel"};
            int answer = Dialogs.showYesNoCancelDialog(
                    "Text Layer Found", msg, options, QUESTION_MESSAGE);
            if (answer == CANCEL_OPTION || answer == CLOSED_OPTION) {
                return;
            } else if (answer == YES_OPTION) { // rasterize
                comp = comp.createCopy(false, false);
                comp.rasterizeAllTextLayers();
            } else if (answer == NO_OPTION) { // ignore
                // they will be ignored
            } else {
                throw new IllegalStateException("answer = " + answer);
            }
        }

        Composition finalComp = comp;
        OpenRasterExportPanel p = new OpenRasterExportPanel();
        new DialogBuilder()
                .content(p)
                .owner(owner)
                .title("Export OpenRaster")
                .okText("Export")
                .okAction(() -> okPressedInDialog(finalComp, p))
                .show();
    }

    private static void okPressedInDialog(Composition comp, OpenRasterExportPanel p) {
        File file = FileChoosers.selectSaveFileForSpecificFormat(FileChoosers.oraFilter);
        if (file != null) {
            boolean addMergedImage = p.exportMergedImage();
            Runnable saveTask = () -> OpenRaster.uncheckedWrite(comp, file, addMergedImage);
            comp.saveAsync(saveTask, file, true);
        }
    }
}
