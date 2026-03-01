/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import javax.swing.text.Document;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.beans.PropertyChangeListener;
import java.util.function.Predicate;

import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * A {@link LayerUI} for validating {@link JTextField}s.
 * Paints a red "X" on the text field if its content fails validation.
 */
public class TFValidationLayerUI extends LayerUI<JTextField> {
    private static final int ICON_SIZE = 8;
    private static final int PADDING = 10;

    private final Predicate<JTextField> validator;

    // cached validation state
    private boolean isValid = true;

    // listeners stored to allow clean removal during uninstallation
    private SimpleDocumentListener docListener;
    private PropertyChangeListener propListener;

    private TFValidationLayerUI(Predicate<JTextField> validator) {
        this.validator = validator;
    }

    // used when only the red warning icon is needed,
    // depending on the result of the given predicate
    public static JLayer<JTextField> wrapWithSimpleValidation(JTextField textField, Predicate<JTextField> validator) {
        return new JLayer<>(textField, new TFValidationLayerUI(validator));
    }

    // used when in addition to the red warning icon,
    // a specific error message is also needed
    public static JLayer<JTextField> wrapWithValidation(JTextField textField, TextFieldValidator validator) {
        return new JLayer<>(textField, new TFValidationLayerUI(tf -> validator.check(tf).isValid()));
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        @SuppressWarnings("unchecked")
        JLayer<JTextField> jlayer = (JLayer<JTextField>) c;
        JTextField textField = jlayer.getView();

        // initial validation
        isValid = validator.test(textField);

        // update validation whenever text is typed/removed/changed
        docListener = new SimpleDocumentListener(e -> updateValidation(jlayer));
        textField.getDocument().addDocumentListener(docListener);

        // keep document listener attached even if the underlying
        // Document is swapped out programmatically
        propListener = e -> {
            if ("document".equals(e.getPropertyName())) {
                if (e.getOldValue() instanceof Document oldDoc) {
                    oldDoc.removeDocumentListener(docListener);
                }
                if (e.getNewValue() instanceof Document newDoc) {
                    newDoc.addDocumentListener(docListener);
                }
                updateValidation(jlayer);
            }
        };
        textField.addPropertyChangeListener("document", propListener);
    }

    @Override
    public void uninstallUI(JComponent c) {
        @SuppressWarnings("unchecked")
        JLayer<JTextField> jlayer = (JLayer<JTextField>) c;
        JTextField textField = jlayer.getView();

        // clean up listeners to prevent memory leaks
        if (docListener != null) {
            textField.getDocument().removeDocumentListener(docListener);
            docListener = null;
        }
        if (propListener != null) {
            textField.removePropertyChangeListener("document", propListener);
            propListener = null;
        }

        super.uninstallUI(c);
    }

    private void updateValidation(JLayer<JTextField> jlayer) {
        JTextField textField = jlayer.getView();
        boolean wasValid = isValid;
        isValid = validator.test(textField);

        // trigger a layer repaint if the state visibly changed
        if (wasValid != isValid) {
            jlayer.repaint();
        }
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);

        if (isValid) {
            return; // skip painting if validation passes
        }

        // paint an "X" icon on the component
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        int x = c.getWidth() - PADDING - ICON_SIZE;
        int y = (c.getHeight() - ICON_SIZE) / 2;
        g2.setPaint(RED);
        g2.fillRect(x, y, ICON_SIZE + 1, ICON_SIZE + 1);
        g2.setPaint(WHITE);
        g2.drawLine(x, y, x + ICON_SIZE, y + ICON_SIZE);
        g2.drawLine(x, y + ICON_SIZE, x + ICON_SIZE, y);

        g2.dispose();
    }
}
