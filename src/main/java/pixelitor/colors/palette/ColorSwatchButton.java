/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.layers.LayerButton;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A button in a {@link PalettePanel}, representing a single color
 */
public class ColorSwatchButton extends JComponent {
    public static final int SIZE = 32;

    // Grid positions.
    private final int xPos;
    private final int yPos;

    private boolean marked = false;
    public static ColorSwatchButton last = null;

    private static final Dimension size = new Dimension(SIZE, SIZE);
    private Color color;
    private boolean raised = true;

    public ColorSwatchButton(Color color, ColorSwatchClickHandler clickHandler, int xPos, int yPos) {
        assert clickHandler != null;

        this.xPos = xPos;
        this.yPos = yPos;
        setColor(color);

        setPreferredSize(size);
        setMinimumSize(size);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                boolean ctrlClick = e.isControlDown();
                if (ctrlClick) {
                    marked = false;
                } else {
                    marked = true;
                    regularClick(e);
                }
                raised = false;
                repaint();
            }

            private void regularClick(MouseEvent e) {
                ColorSwatchButton prev = last;
                last = ColorSwatchButton.this;
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
        if (this == last) {
            g.draw3DRect(1, 1, SIZE - 3, SIZE - 3, raised);
            g.draw3DRect(2, 2, SIZE - 5, SIZE - 5, raised);
        }
        if (marked) {
            g.setColor(LayerButton.SELECTED_COLOR);
            g.fillRect(1, 1, 7, 7);
            g.setColor(LayerButton.UNSELECTED_COLOR);
            g.fillRect(3, 3, 3, 3);
        }
    }

    public int getXPos() {
        return xPos;
    }

    public int getYPos() {
        return yPos;
    }
}
