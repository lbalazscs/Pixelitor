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

package pixelitor.colors.palette;

import pixelitor.layers.LayerGUI;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A color swatch in a {@link PalettePanel}.
 */
public class ColorSwatchButton extends JComponent {
    public static final int SIZE = 32;
    private static final Dimension SIZE_DIM = new Dimension(SIZE, SIZE);

    // tracks the last-clicked swatch across all instances
    public static ColorSwatchButton lastClickedSwatch = null;

    // the coordinates of this swatch in its grid
    private final int gridX;
    private final int gridY;

    private Color color;
    private boolean raised = true;
    private boolean marked = false;

    public ColorSwatchButton(Color color, ColorSwatchClickHandler clickHandler, int gridX, int gridY) {
        assert clickHandler != null;

        this.gridX = gridX;
        this.gridY = gridY;
        setColor(color);

        setPreferredSize(SIZE_DIM);
        setMinimumSize(SIZE_DIM);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isControlDown()) {
                    // ctrl-click unmarks the swatch
                    marked = false;
                } else {
                    // regular click marks the swatch and triggers the click action
                    marked = true;
                    performClickAction(e, clickHandler);
                }
                raised = false;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                raised = true;
                repaint();
            }
        });
    }

    private void performClickAction(MouseEvent e, ColorSwatchClickHandler clickHandler) {
        ColorSwatchButton previouslyClicked = lastClickedSwatch;
        lastClickedSwatch = this;
        if (previouslyClicked != null) {
            previouslyClicked.repaint();
        }

        clickHandler.handle(this.color, e);
    }

    public void setColor(Color color) {
        this.color = color;
        setBackground(color);
        setForeground(color);
        marked = false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(color);
        g.fill3DRect(0, 0, SIZE, SIZE, raised);
        if (this == lastClickedSwatch) {
            paintSelectionBorder(g);
        }
        if (marked) {
            paintMark(g);
        }
    }

    private void paintSelectionBorder(Graphics g) {
        g.draw3DRect(1, 1, SIZE - 3, SIZE - 3, raised);
        g.draw3DRect(2, 2, SIZE - 5, SIZE - 5, raised);
    }

    private static void paintMark(Graphics g) {
        // draws a small rectangle in the top-left corner
        g.setColor(LayerGUI.SELECTED_COLOR);
        g.fillRect(1, 1, 7, 7);
        g.setColor(LayerGUI.UNSELECTED_COLOR);
        g.fillRect(3, 3, 3, 3);
    }

    public int getGridX() {
        return gridX;
    }

    public int getGridY() {
        return gridY;
    }
}
