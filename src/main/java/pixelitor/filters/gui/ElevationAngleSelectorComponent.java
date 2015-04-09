/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
package pixelitor.filters.gui;

import java.awt.Graphics;
import java.awt.Graphics2D;

import static java.awt.Color.BLACK;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * A GUI component that can be used to select an elevation (altitude) angle with the mouse
 */
public class ElevationAngleSelectorComponent extends AbstractAngleSelectorComponent {

    public ElevationAngleSelectorComponent(ElevationAngleParam param) {
        super(param);

        cx = 0;
        cy = SIZE;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(BLACK);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        g2.drawLine(0, 0, 0, SIZE);
        g2.drawLine(0, SIZE, SIZE, SIZE);

        double angle = model.getValueInRadians();

        float radius = SIZE;
        float endX = (float) (cx + (radius * Math.cos(angle)));
        float endY = (float) (cy + (radius * Math.sin(angle)));

        drawArrow(g2, Math.PI + angle, endX, endY, cx, cy);
    }
}
