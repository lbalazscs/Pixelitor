/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * The left panel used in the "Mask from Color Range"
 */
public class ColorPickerPanel extends ImagePanel {
    public ColorPickerPanel(BufferedImage img,
                            Consumer<Color> changeListener) {
        super(true);
        setImage(img);
        var ma = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                sampleColor(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                sampleColor(e.getX(), e.getY());
            }

            private void sampleColor(int x, int y) {
                if (x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight()) {
                    int rgb = image.getRGB(x, y);
                    changeListener.accept(new Color(rgb));
                }
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }
}
