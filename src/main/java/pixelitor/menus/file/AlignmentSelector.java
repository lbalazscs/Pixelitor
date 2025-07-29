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

package pixelitor.menus.file;

import pixelitor.gui.utils.Themes;
import pixelitor.gui.utils.VectorIcon;
import pixelitor.tools.gui.ToolButton;

import javax.swing.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A panel with a 3x3 grid of toggle buttons to select an alignment.
 */
public class AlignmentSelector extends JPanel {
    private static final Insets BUTTON_MARGIN = new Insets(2, 2, 2, 2);
    private static final int ICON_SIZE = 20;

    public enum HorizontalAlignment {LEFT, CENTER, RIGHT}

    public enum VerticalAlignment {TOP, CENTER, BOTTOM}

    public record Alignment(HorizontalAlignment hAlign, VerticalAlignment vAlign) {
    }

    private final JToggleButton[][] buttons = new JToggleButton[3][3];
    private final List<ActionListener> listeners = new ArrayList<>();

    public AlignmentSelector() {
        super(new GridLayout(3, 3));
        setBorder(BorderFactory.createEtchedBorder());

        VerticalAlignment[] vAligns = VerticalAlignment.values();
        HorizontalAlignment[] hAligns = HorizontalAlignment.values();

        ButtonGroup buttonGroup = new ButtonGroup();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                JToggleButton button = createButton(hAligns[col], vAligns[row]);
                buttons[row][col] = button;
                buttonGroup.add(button);
                add(button);
            }
        }
        // set default selection
        setAlignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
    }

    private JToggleButton createButton(HorizontalAlignment hAlign, VerticalAlignment vAlign) {
        VectorIcon icon = createAlignmentIcon(ICON_SIZE, ICON_SIZE, hAlign, vAlign);
        JToggleButton button = new JToggleButton(icon);

        Icon selectedIcon = Themes.getActive().isDark()
            ? icon.copyWithColor(ToolButton.darkThemeSelectedColor)
            : icon;
        button.setSelectedIcon(selectedIcon);

        button.setMargin(BUTTON_MARGIN);
        // e.g. "top-left"
        String tooltip = vAlign.name().toLowerCase() + "-" + hAlign.name().toLowerCase();
        button.setToolTipText(tooltip);

        button.addActionListener(this::fireActionPerformed);
        return button;
    }

    private void fireActionPerformed(ActionEvent e) {
        for (ActionListener listener : listeners) {
            // forward the event with this as the source
            listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
        }
    }

    /**
     * Adds an action listener to be notified of selection changes.
     */
    public void addActionListener(ActionListener listener) {
        listeners.add(listener);
    }

    /**
     * Returns the currently selected alignment.
     */
    public Alignment getAlignment() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (buttons[row][col].isSelected()) {
                    return new Alignment(HorizontalAlignment.values()[col], VerticalAlignment.values()[row]);
                }
            }
        }

        // ButtonGroup guarantees that a button is always selected
        throw new IllegalStateException("No alignment button is selected");
    }

    /**
     * Sets the currently selected alignment.
     */
    public void setAlignment(HorizontalAlignment hAlign, VerticalAlignment vAlign) {
        buttons[vAlign.ordinal()][hAlign.ordinal()].setSelected(true);
    }

    /**
     * Sets the currently selected alignment from an Alignment object.
     */
    public void setAlignment(Alignment alignment) {
        setAlignment(alignment.hAlign(), alignment.vAlign());
    }

    /**
     * Creates a vector icon representing an alignment.
     */
    private static VectorIcon createAlignmentIcon(int width, int height, HorizontalAlignment hAlign, VerticalAlignment vAlign) {
        // the icon consists of an outer stroked square and an inner
        // filled square whose position indicates the alignment
        final int PADDING = 2;
        int outerSize = Math.min(width, height) - 2 * PADDING;
        // inner square is smaller for better visual separation
        int innerSize = Math.max(1, outerSize / 2 - 2);

        int outerX = PADDING;
        int outerY = PADDING;

        // the horizontal position of the inner square
        int innerX = switch (hAlign) {
            case LEFT -> outerX + PADDING;
            case CENTER -> outerX + (outerSize - innerSize) / 2;
            case RIGHT -> outerX + outerSize - innerSize - PADDING;
        };

        // the vertical position of the inner square
        int innerY = switch (vAlign) {
            case TOP -> outerY + PADDING;
            case CENTER -> outerY + (outerSize - innerSize) / 2;
            case BOTTOM -> outerY + outerSize - innerSize - PADDING;
        };

        Consumer<Graphics2D> painter = g -> {
            // the outer stroked square representing the whole space
            g.drawRect(outerX, outerY, outerSize, outerSize);
            // the inner filled square representing the content
            g.fillRect(innerX, innerY, innerSize, innerSize);
        };

        Color iconColor = Themes.getActive().isDark() ? Themes.LIGHT_ICON_COLOR : Color.BLACK;
        return new VectorIcon(iconColor, width, height, painter);
    }
}
