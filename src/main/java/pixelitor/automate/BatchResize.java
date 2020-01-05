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

package pixelitor.automate;

import pixelitor.compactions.Resize;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.IntTextField;
import pixelitor.gui.utils.ValidatedPanel;
import pixelitor.gui.utils.ValidationResult;
import pixelitor.io.OutputFormat;

import javax.swing.*;

import static javax.swing.BoxLayout.Y_AXIS;

/**
 * The batch resize functionality
 */
public class BatchResize {
    private BatchResize() { // do not instantiate
    }

    public static void start() {
        var batchResizePanel = new BatchResizePanel();
        new DialogBuilder()
                .validatedContent(batchResizePanel)
                .title("Batch Resize")
                .okAction(() -> dialogAccepted(batchResizePanel))
                .show();
    }

    private static void dialogAccepted(BatchResizePanel p) {
        p.saveValues();

        int maxWidth = p.getNewWidth();
        int maxHeight = p.getNewHeight();

        var resizeAction = new Resize(maxWidth, maxHeight, true);
        Automate.processEachFile(resizeAction, "Batch Resize...");
    }

    /**
     * The GUI for batch resize
     */
    static class BatchResizePanel extends ValidatedPanel {
        private final OpenSaveDirsPanel openSaveDirsPanel;
        private final IntTextField widthTF;
        private final IntTextField heightTF;

        private BatchResizePanel() {
            var sizePanel = new JPanel();

            sizePanel.add(new JLabel("Max Width:"));
            widthTF = new IntTextField(300, 5);
            widthTF.setName("widthTF");
            sizePanel.add(widthTF);

            sizePanel.add(new JLabel("Max Height:"));
            heightTF = new IntTextField(300, 5);
            heightTF.setName("heightTF");
            sizePanel.add(heightTF);

            setLayout(new BoxLayout(this, Y_AXIS));
            add(sizePanel);
            openSaveDirsPanel = new OpenSaveDirsPanel(
                    false, OutputFormat.getLastUsed());
            add(openSaveDirsPanel);
        }

        @Override
        public ValidationResult checkValidity() {
            return openSaveDirsPanel.checkValidity()
                    .addErrorIf(widthTF.getText()
                                    .trim()
                                    .isEmpty(),
                            "The 'width' field is empty")
                    .addErrorIf(heightTF.getText()
                                    .trim()
                                    .isEmpty(),
                            "The 'height' field is empty");
        }

        private void saveValues() {
            openSaveDirsPanel.rememberValues();
        }

        private int getNewWidth() {
            return widthTF.getIntValue();
        }

        private int getNewHeight() {
            return heightTF.getIntValue();
        }
    }
}
