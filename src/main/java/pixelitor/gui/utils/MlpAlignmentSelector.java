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

import pixelitor.filters.gui.ParamAdjustmentListener;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

import static pixelitor.gui.utils.VectorIcon.LIGHT_FG;
import static pixelitor.tools.gui.ToolButton.darkThemeActiveIconColor;

/**
 * A panel for selecting a horizontal text alignment option
 * for multiline text or text on a path.
 */
public class MlpAlignmentSelector extends JPanel {
    private final JToggleButton leftAlignButton;
    private final JToggleButton centerAlignButton;
    private final JToggleButton rightAlignButton;

    // 0 is not used => it can be a sentinel value for old pxc files
    public static final int LEFT = 1;
    public static final int CENTER = 2;
    public static final int RIGHT = 3;

    private static final int ICON_SIZE = 18;
    private static final int PADDING = 4;
    private static final Dimension BUTTON_SIZE = new Dimension(30, 30);

    private static Shape[] iconShapes;

    public MlpAlignmentSelector(int defaultAlignment, ParamAdjustmentListener adjustmentListener) {
        setLayout(new GridLayout(1, 3, 0, 0));

        if (iconShapes == null) {
            createIconShapes();
        }

        boolean darkTheme = Themes.getActive().isDark();
        ButtonGroup group = new ButtonGroup();

        leftAlignButton = addButton(0, group, darkTheme);
        centerAlignButton = addButton(1, group, darkTheme);
        rightAlignButton = addButton(2, group, darkTheme);

        setAlignment(defaultAlignment);

        // bind to the listener only after setting the default selection
        leftAlignButton.addActionListener(e -> adjustmentListener.paramAdjusted());
        centerAlignButton.addActionListener(e -> adjustmentListener.paramAdjusted());
        rightAlignButton.addActionListener(e -> adjustmentListener.paramAdjusted());
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
            button.setSelectedIcon(icon.copyWithColor(darkThemeActiveIconColor));
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

        Path2D shape = new Path2D.Double();
        for (int i = 0; i < 5; i++) {
            int y = i * (lineHeight + lineGap);

            // alternating between full width and 60% width
            int lineWidth = (i % 2 == 0) ? ICON_SIZE : (int) (ICON_SIZE * 0.6);

            double x = switch (alignment) {
                case CENTER -> (ICON_SIZE - lineWidth) / 2.0;
                case RIGHT -> ICON_SIZE - lineWidth;
                case LEFT -> 0;
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
        if (centerAlignButton.isSelected()) {
            return CENTER;
        }
        if (rightAlignButton.isSelected()) {
            return RIGHT;
        }
        return LEFT;
    }

    public void setAlignment(int alignment) {
        JToggleButton selectedButton = switch (alignment) {
            case LEFT -> leftAlignButton;
            case CENTER -> centerAlignButton;
            case RIGHT -> rightAlignButton;
            default -> throw new IllegalStateException("alignment: " + alignment);
        };
        selectedButton.setSelected(true);
    }

    @Override
    public void setEnabled(boolean enabled) {
        leftAlignButton.setEnabled(enabled);
        centerAlignButton.setEnabled(enabled);
        rightAlignButton.setEnabled(enabled);
    }
}
