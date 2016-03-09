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

package pixelitor.gui.utils;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class ColorPickerThumbnailPanel extends ImagePanel {
    public ColorPickerThumbnailPanel(BufferedImage img,
                                     ColorSelectionListener changeListener) {
        super(true);
        setImage(img);
        MouseAdapter ma = new MouseAdapter() {
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
                    changeListener.selectedColorChanged(new Color(rgb));
                }
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }
}
