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

package pixelitor.filters.gui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.FlowLayout;

public class TextParamGUI extends JPanel implements ParamGUI {
    private final JTextField tf;

    public TextParamGUI(TextParam param, String defaultValue, ParamAdjustmentListener adjustmentListener) {
        tf = new JTextField(defaultValue);

        setLayout(new FlowLayout(FlowLayout.LEFT));
        add(new JLabel(getName() + ": "));

        if (adjustmentListener != null) {
            tf.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    if (param.isTrigger()) {
                        adjustmentListener.paramAdjusted();
                    }
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    if (param.isTrigger()) {
                        adjustmentListener.paramAdjusted();
                    }
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    if (param.isTrigger()) {
                        adjustmentListener.paramAdjusted();
                    }
                }
            });
        }
        add(tf);
    }

    @Override
    public void updateGUI() {
        // ok if empty
    }

    @Override
    public boolean isEnabled() {
        return tf.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        tf.setEnabled(enabled);
    }

    public String getText() {
        return tf.getText();
    }

    public void setText(String s) {
        tf.setText(s);
    }

    @Override
    public void setToolTip(String tip) {
        tf.setToolTipText(tip);
    }
}
