/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.ImageComponents;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.OKCancelDialog;
import pixelitor.io.FileChoosers;
import pixelitor.io.OpenRaster;
import pixelitor.io.OpenSaveManager;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

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

    public static void showInDialog(JFrame parent) {
        Composition comp = ImageComponents.getActiveCompOrNull();
        if (comp.getNumLayers() < 2) {
            boolean exportAnyway = Dialogs.showYesNoQuestionDialog("Only one layer", comp.getName() + " has only one layer.\n" +
                    "Are you sure that you want to export it in a layered format?");
            if(!exportAnyway) {
                return;
            }
        }

        OpenRasterExportPanel p = new OpenRasterExportPanel();
        OKCancelDialog d = new OKCancelDialog(p, parent, "Export OpenRaster", "Export", "Cancel", false) {
            @Override
            protected void okAction() {
                close();
                File file = FileChoosers.selectSaveFileForSpecificFormat(FileChoosers.oraFilter);
                if(file != null) {
                    boolean addMergedImage = p.exportMergedImage();
                    try {
                        OpenRaster.writeOpenRaster(comp, file, addMergedImage);
                        OpenSaveManager.afterSaveActions(comp, file, true);
                    } catch (IOException e) {
                        Messages.showException(e);
                    }
                }
            }
        };
        d.setVisible(true);
    }
}
