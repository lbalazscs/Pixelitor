/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

package pixelitor.gui;

import pixelitor.FgBgColors;
import pixelitor.layers.LayerButton;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ColorSwatchButton extends JComponent {
    public static final int SIZE = 32;
    private boolean marked = false;

    private static final Dimension size = new Dimension(SIZE, SIZE);
    private Color color;
    private boolean raised = true;

    public ColorSwatchButton(Color color, boolean fg) {
        setColor(color);

        setPreferredSize(size);
        setMinimumSize(size);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (fg) {
                    FgBgColors.setFG(ColorSwatchButton.this.color);
                } else {
                    FgBgColors.setBG(ColorSwatchButton.this.color);
                }
                if (SwingUtilities.isRightMouseButton(e)) {
                    marked = false;
                } else {
                    marked = true;
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
        if (marked) {
            g.setColor(LayerButton.SELECTED_COLOR);
            g.fillRect(1, 1, 7, 7);
            g.setColor(LayerButton.UNSELECTED_COLOR);
            g.fillRect(3, 3, 3, 3);
        }
    }
}
