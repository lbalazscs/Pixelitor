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

package pixelitor.gui.utils;

import pixelitor.filters.gui.ParamAdjustmentListener;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

import static pixelitor.gui.utils.VectorIcon.LIGHT_FG;
import static pixelitor.tools.gui.ToolButton.darkThemeSelectedColor;

public class AlignmentSelector extends JPanel {
    private final JToggleButton leftAlign;
    private final JToggleButton centerAlign;
    private final JToggleButton rightAlign;

    public static final int LEFT = 1;
    public static final int CENTER = 2;
    public static final int RIGHT = 3;

    private static final int ICON_SIZE = 18;
    private static final int PADDING = 4;
    private static final Dimension BUTTON_SIZE = new Dimension(30, 30);

    private static Shape[] iconShapes;

    public AlignmentSelector(int defaultAlignment, ParamAdjustmentListener adjustmentListener) {
        setLayout(new GridLayout(1, 3, 0, 0));

        boolean darkTheme = Themes.getActive().isDark();
        ButtonGroup group = new ButtonGroup();

        if (iconShapes == null) {
            createIconShapes();
        }

        leftAlign = addButton(0, group, darkTheme);
        centerAlign = addButton(1, group, darkTheme);
        rightAlign = addButton(2, group, darkTheme);

        setSelected(defaultAlignment);

        // bind to the listener only after setting the default selection
        leftAlign.addActionListener(e -> adjustmentListener.paramAdjusted());
        centerAlign.addActionListener(e -> adjustmentListener.paramAdjusted());
        rightAlign.addActionListener(e -> adjustmentListener.paramAdjusted());
    }

    private static void createIconShapes() {
        iconShapes = new Shape[3];
        iconShapes[0] = createIconShape(LEFT);
        iconShapes[1] = createIconShape(CENTER);
        iconShapes[2] = createIconShape(RIGHT);
    }

    private JToggleButton addButton(int shapeIndex, ButtonGroup group, boolean darkTheme) {
        VectorIcon icon = createAlignmentIcon(shapeIndex, darkTheme);
        JToggleButton button = new JToggleButton(icon);
        button.setPreferredSize(BUTTON_SIZE);

        VectorIcon disabledIcon = icon.copyWithColor(Color.GRAY);
        button.setDisabledIcon(disabledIcon);

        if (darkTheme) {
            button.setSelectedIcon(icon.copyWithColor(darkThemeSelectedColor));
            button.setDisabledSelectedIcon(disabledIcon);
        }

        button.setFocusPainted(false);
        button.setMargin(new Insets(PADDING, PADDING, PADDING, PADDING));
        group.add(button);
        add(button);

        return button;
    }

    private static VectorIcon createAlignmentIcon(int shapeIndex, boolean darkTheme) {
        Shape shape = iconShapes[shapeIndex];

        Color color = darkTheme ? Themes.LIGHT_ICON_COLOR : LIGHT_FG;
        return VectorIcon.createFilled(shape, color, ICON_SIZE, ICON_SIZE);
    }

    private static Path2D createIconShape(int alignment) {
        int lineHeight = 2;
        int lineGap = 2;

        // 5 lines, and 4 gaps between them
        assert (5 * lineHeight) + (4 * lineGap) == ICON_SIZE;

        // TODO the 3 shapes could be cached
        Path2D shape = new Path2D.Double();
        for (int i = 0; i < 5; i++) {
            int y = i * (lineHeight + lineGap);
            int lineWidth = (i % 2 == 0) ? ICON_SIZE : (int) (ICON_SIZE * 0.6);

            double x = switch (alignment) {
                case CENTER -> (ICON_SIZE - lineWidth) / 2.0;  // center all lines
                case RIGHT -> ICON_SIZE - lineWidth;         // align to right
                case LEFT -> 0;                               // align to left
                default -> throw new IllegalStateException("Unexpected value: " + alignment);
            };

            // create a horizontal line as a rectangle that can be filled
            shape.moveTo(x, y);
            shape.lineTo(x, y + lineHeight);
            shape.lineTo(x + lineWidth, y + lineHeight);
            shape.lineTo(x + lineWidth, y);
            shape.closePath();
        }
        return shape;
    }

    public int getSelectedAlignment() {
        if (centerAlign.isSelected()) {
            return CENTER;
        }
        if (rightAlign.isSelected()) {
            return RIGHT;
        }
        return LEFT;
    }

    public void setSelected(int alignment) {
        JToggleButton selectedButton = switch (alignment) {
            case LEFT -> leftAlign;
            case CENTER -> centerAlign;
            case RIGHT -> rightAlign;
            default -> throw new IllegalStateException("alignment: " + alignment);
        };
        selectedButton.setSelected(true);
    }

    @Override
    public void setEnabled(boolean enabled) {
        leftAlign.setEnabled(enabled);
        centerAlign.setEnabled(enabled);
        rightAlign.setEnabled(enabled);
    }
}
