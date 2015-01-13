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
package pixelitor.automate;

import pixelitor.Composition;
import pixelitor.PixelitorWindow;
import pixelitor.filters.comp.CompositionUtils;
import pixelitor.utils.CompositionAction;
import pixelitor.utils.IntTextField;
import pixelitor.utils.ValidatedDialog;
import pixelitor.utils.ValidatedForm;

import javax.swing.*;

/**
 * The batch filter functionality
 */
public class BatchFilter {
    private BatchFilter() { // do not instantiate
    }

    public static void runBatchFilter() {
        BatchFilterPanel p = new BatchFilterPanel();
        ValidatedDialog chooser = new ValidatedDialog(p, PixelitorWindow.getInstance(), "Batch Filter");
        chooser.setVisible(true);
        if (!chooser.isOkPressed()) {
            return;
        }
        p.saveValues();

        final int maxWidth = p.getWidthValue();
        final int maxHeight = p.getHeightValue();

        CompositionAction resizeAction = new CompositionAction() {
            @Override
            public void process(Composition comp) {
                CompositionUtils.resize(comp, maxWidth, maxHeight, true);
            }
        };
        Automate.processEachFile(resizeAction, true, "Batch Filter...");
    }

    static class BatchFilterPanel extends ValidatedForm {
        private String errorMessage;
        private final OpenSaveDirsPanel openSaveDirsPanel = new OpenSaveDirsPanel(false);
        private final IntTextField withTextField;
        private final IntTextField heightTextField;

        BatchFilterPanel() {
            JPanel dimensionsPanel = new JPanel();
            dimensionsPanel.add(new JLabel("Max Width:"));
            withTextField = new IntTextField(5);
            withTextField.setText("300");
            dimensionsPanel.add(withTextField);
            dimensionsPanel.add(new JLabel("Max Height:"));
            heightTextField = new IntTextField(5);
            heightTextField.setText("300");
            dimensionsPanel.add(heightTextField);

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            add(dimensionsPanel);
            add(openSaveDirsPanel);
        }

        @Override
        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public boolean isDataValid() {
            if (!openSaveDirsPanel.isDataValid()) {
                errorMessage = openSaveDirsPanel.getErrorMessage();
                return false;
            }
            if (withTextField.getText().trim().isEmpty()) {
                errorMessage = "The 'width' field is empty";
                return false;
            }
            if (heightTextField.getText().trim().isEmpty()) {
                errorMessage = "The 'height' field is empty";
                return false;
            }

            return true;
        }

        public void saveValues() {
            openSaveDirsPanel.saveValues();
        }

        public int getWidthValue() {
            return withTextField.getIntValue();
        }

        public int getHeightValue() {
            return heightTextField.getIntValue();
        }
    }
}
