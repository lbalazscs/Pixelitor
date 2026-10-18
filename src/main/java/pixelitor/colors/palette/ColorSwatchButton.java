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

package pixelitor.colors.palette;

import pixelitor.colors.ColorHistory;
import pixelitor.layers.LayerGUI;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A color swatch in a {@link PalettePanel} or in the {@link ColorHistory} dialog.
 */
public final class ColorSwatchButton extends JComponent {
    public static final int SIZE = 32;
    private static final Dimension SIZE_DIM = new Dimension(SIZE, SIZE);

    private Color color;
    private boolean raised = true;

    // clicked since the mark was last cleared
    private boolean marked = false;

    private final ColorSwatchClickHandler clickHandler;
    private final SwatchSelection selection;

    public ColorSwatchButton(Color color,
                             ColorSwatchClickHandler clickHandler,
                             SwatchSelection selection) {
        assert color != null;
        assert clickHandler != null;
        assert selection != null;

        this.clickHandler = clickHandler;
        this.selection = selection;

        setColor(color);
        setOpaque(true);

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
                    performClickAction(e);
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

    private void performClickAction(MouseEvent e) {
        // Select before running the handler: the handler can cause the
        // container to be rebuilt (for example by changing the color
        // history), discarding this swatch. Selecting beforehand allows
        // the container to preserve the selection across the rebuild.
        // Selecting afterwards would select a swatch that is already gone.
        selection.select(this);

        clickHandler.handle(color, e);
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        assert color != null;
        this.color = color;
        marked = false;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        g.setColor(color);
        g.fill3DRect(0, 0, w, h, raised);
        if (selection.isSelected(this)) {
            paintSelectionBorder(g, w, h);
        }
        if (marked) {
            paintMark(g);
        }
    }

    private void paintSelectionBorder(Graphics g, int w, int h) {
        g.draw3DRect(1, 1, w - 3, h - 3, raised);
        g.draw3DRect(2, 2, w - 5, h - 5, raised);
    }

    // draws a small rectangle in the top-left corner
    private static void paintMark(Graphics g) {
        g.setColor(LayerGUI.SELECTED_COLOR);
        g.fillRect(1, 1, 7, 7);
        g.setColor(LayerGUI.UNSELECTED_COLOR);
        g.fillRect(3, 3, 3, 3);
    }
}
