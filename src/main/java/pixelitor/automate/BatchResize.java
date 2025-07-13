/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
 * The batch resize functionality.
 */
public class BatchResize {
    private BatchResize() { // do not instantiate
    }

    public static void showDialog(String dialogTitle) {
        var batchResizePanel = new BatchResizePanel();
        new DialogBuilder()
            .validatedContent(batchResizePanel)
            .title(dialogTitle)
            .okAction(() -> dialogAccepted(batchResizePanel))
            .show();
    }

    private static void dialogAccepted(BatchResizePanel panel) {
        panel.rememberSettings();

        int maxWidth = panel.getNewWidth();
        int maxHeight = panel.getNewHeight();

        var resizeAction = new Resize(maxWidth, maxHeight, true);
        new BatchProcessor(resizeAction, "Batch Resize...").processFiles();
    }

    /**
     * The panel for batch resize settings.
     */
    static class BatchResizePanel extends ValidatedPanel {
        private final OpenSaveDirsPanel openSaveDirsPanel;
        private final JTextField widthTF;
        private final JTextField heightTF;

        private static final int DEFAULT_WIDTH = 300;
        private static final int DEFAULT_HEIGHT = 300;
        private static final String WIDTH_LABEL = "Max Width";
        private static final String HEIGHT_LABEL = "Max Height";

        private BatchResizePanel() {
            var sizePanel = new JPanel();

            IntDocumentFilter documentFilter = new IntDocumentFilter();

            widthTF = addTextField(WIDTH_LABEL + ":", "widthTF", DEFAULT_WIDTH, sizePanel, documentFilter);
            heightTF = addTextField(HEIGHT_LABEL + ":", "heightTF", DEFAULT_HEIGHT, sizePanel, documentFilter);

            setLayout(new BoxLayout(this, Y_AXIS));
            add(sizePanel);
            openSaveDirsPanel = new OpenSaveDirsPanel();
            add(openSaveDirsPanel);
        }

        private static JTextField addTextField(String label, String name, int defaultValue, JPanel sizePanel, IntDocumentFilter documentFilter) {
            sizePanel.add(new JLabel(label));

            JTextField tf = new JTextField(String.valueOf(defaultValue), 5);
            tf.setName(name);
            // the JLayer shows immediate visual feedback for invalid input
            sizePanel.add(createPositiveIntLayer(label, tf));
            documentFilter.applyOn(tf);

            return tf;
        }

        @Override
        public ValidationResult validateSettings() {
            return openSaveDirsPanel.validateSettings()
                .requirePositiveInt(widthTF.getText(), WIDTH_LABEL)
                .requirePositiveInt(heightTF.getText(), HEIGHT_LABEL);
        }

        private void rememberSettings() {
            openSaveDirsPanel.rememberSettings();
        }

        private int getNewWidth() {
            // validation already ensured this is a valid positive integer
            return Integer.parseInt(widthTF.getText().trim());
        }

        private int getNewHeight() {
            // validation already ensured this is a valid positive integer
            return Integer.parseInt(heightTF.getText().trim());
        }
    }
}
