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

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

import static pixelitor.utils.ImageUtils.isWithinBounds;

/**
 * A specialized {@link ImagePanel} that allows users to pick colors
 * by clicking or dragging on the displayed image.
 * Used as the left panel in "Mask from Color Range",
 * to sample colors from the source image.
 */
public class ColorPickerPanel extends ImagePanel {
    public ColorPickerPanel(BufferedImage img,
                            Consumer<Color> colorSelectionHandler) {
        super(true);

        replaceImage(img);
        initMouseHandlers(colorSelectionHandler);
    }

    private void initMouseHandlers(Consumer<Color> colorSelectionHandler) {
        var ma = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                sampleColorAt(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                sampleColorAt(e.getX(), e.getY());
            }

            private void sampleColorAt(int x, int y) {
                if (isWithinBounds(x, y, image)) {
                    int rgb = image.getRGB(x, y);
                    colorSelectionHandler.accept(new Color(rgb));
                }
            }

        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }
}
