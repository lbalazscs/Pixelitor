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
    public static ColorSwatchButton lastClickedSwatch = null;

    // Grid positions.
    private final int gridX;
    private final int gridY;

    private static final Dimension size = new Dimension(SIZE, SIZE);
    private Color color;
    private boolean raised = true;
    private boolean marked = false;

    public ColorSwatchButton(Color color, ColorSwatchClickHandler clickHandler, int gridX, int gridY) {
        assert clickHandler != null;

        this.gridX = gridX;
        this.gridY = gridY;
        setColor(color);

        setPreferredSize(size);
        setMinimumSize(size);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isControlDown()) {
                    marked = false;
                } else {
                    marked = true;
                    regularClick(e);
                }
                raised = false;
                repaint();
            }

            private void regularClick(MouseEvent e) {
                ColorSwatchButton prev = lastClickedSwatch;
                lastClickedSwatch = ColorSwatchButton.this;
                if (prev != null) {
                    prev.repaint();
                }

                Color newColor = ColorSwatchButton.this.color;
                clickHandler.handle(newColor, e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                raised = true;
                repaint();
            }
        });
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
