/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.gui.utils;

import javax.swing.*;
import javax.swing.plaf.LayerUI;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.function.Predicate;

import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * A {@link LayerUI} support class for validating JTextFields.
 * Paints a red X on the textfield if the content is not valid.
 */
public class TFValidationLayerUI extends LayerUI<JTextField> {
    private final Predicate<JTextField> checker;

    private TFValidationLayerUI(Predicate<JTextField> checker) {
        this.checker = checker;
    }

    // used when only the red warning icon is needed,
    // depending on the result of the given predicate
    public static JLayer<JTextField> createCheckedTF(JTextField textField, Predicate<JTextField> checker) {
        return new JLayer<>(textField, new TFValidationLayerUI(checker));
    }

    // used when in addition to the red warning icon,
    // a specific error message is also needed
    public static JLayer<JTextField> createValidatedTF(JTextField textField, TextFieldValidator validator) {
        return new JLayer<>(textField, new TFValidationLayerUI(tf -> validator.check(tf).isValid()));
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);

        @SuppressWarnings("unchecked")
        JLayer<JTextField> jLayer = (JLayer<JTextField>) c;

        JTextField textField = jLayer.getView();
        boolean isOK = checker.test(textField);
        if (!isOK) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

            // Paint the red X.
            int w = c.getWidth();
            int h = c.getHeight();
            int s = 8;
            int pad = 10;
            int x = w - pad - s;
            int y = (h - s) / 2;
            g2.setPaint(RED);
            g2.fillRect(x, y, s + 1, s + 1);
            g2.setPaint(WHITE);
            g2.drawLine(x, y, x + s, y + s);
            g2.drawLine(x, y + s, x + s, y);

            g2.dispose();
        }
    }
}