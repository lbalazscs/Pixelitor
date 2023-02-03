/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.utils.IntDocumentFilter;
import pixelitor.gui.utils.ValidatedPanel;
import pixelitor.gui.utils.ValidationResult;

import javax.swing.*;

import static javax.swing.BoxLayout.Y_AXIS;
import static pixelitor.gui.utils.TextFieldValidator.createPositiveIntLayer;

/**
 * The batch resize functionality
 */
public class BatchResize {
    private BatchResize() { // do not instantiate
    }

    public static void showDialog() {
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
        Automate.processFiles(resizeAction, "Batch Resize...");
    }

    /**
     * The GUI for batch resize
     */
    static class BatchResizePanel extends ValidatedPanel {
        private final OpenSaveDirsPanel openSaveDirsPanel;
        private final JTextField widthTF;
        private final JTextField heightTF;

        private static final int DEFAULT_WIDTH = 300;
        private static final int DEFAULT_HEIGHT = 300;

        private BatchResizePanel() {
            var sizePanel = new JPanel();

            IntDocumentFilter documentFilter = new IntDocumentFilter();

            widthTF = addTextField("Max Width:", "widthTF", DEFAULT_WIDTH, sizePanel, documentFilter);
            heightTF = addTextField("Max Height:", "heightTF", DEFAULT_HEIGHT, sizePanel, documentFilter);

            setLayout(new BoxLayout(this, Y_AXIS));
            add(sizePanel);
            openSaveDirsPanel = new OpenSaveDirsPanel();
            add(openSaveDirsPanel);
        }

        private static JTextField addTextField(String label, String name, int defaultValue, JPanel sizePanel, IntDocumentFilter documentFilter) {
            sizePanel.add(new JLabel(label));

            JTextField tf = new JTextField(String.valueOf(defaultValue), 5);
            tf.setName(name);
            sizePanel.add(createPositiveIntLayer(label, tf, false));
            documentFilter.useFor(tf);
            return tf;
        }

        @Override
        public ValidationResult validateSettings() {
            return openSaveDirsPanel.validateSettings()
                .addErrorIf(widthTF.getText().trim().isEmpty(),
                    "The 'width' field is empty")
                .addErrorIf(heightTF.getText().trim().isEmpty(),
                    "The 'height' field is empty");
        }

        private void saveValues() {
            openSaveDirsPanel.rememberValues();
        }

        private int getNewWidth() {
            try {
                return Integer.parseInt(widthTF.getText());
            } catch (NumberFormatException e) {
                widthTF.setText(String.valueOf(DEFAULT_WIDTH));
                return DEFAULT_WIDTH;
            }
        }

        private int getNewHeight() {
            try {
                return Integer.parseInt(heightTF.getText());
            } catch (NumberFormatException e) {
                heightTF.setText(String.valueOf(DEFAULT_HEIGHT));
                return DEFAULT_HEIGHT;
            }
        }
    }
}
