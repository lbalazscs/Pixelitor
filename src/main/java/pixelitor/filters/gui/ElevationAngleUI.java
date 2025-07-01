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
package pixelitor.filters.gui;

import pixelitor.gui.utils.Themes;

import java.awt.Graphics;
import java.awt.Graphics2D;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * A GUI component for selecting elevation angles between 0 and 90 degrees with the mouse.
 */
public class ElevationAngleUI extends AbstractAngleUI {
    public ElevationAngleUI(ElevationAngleParam param) {
        super(param);

        // the rotation center is at the bottom-left corner
        centerX = 0;
        centerY = SELECTOR_SIZE;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        boolean darkTheme = Themes.getActive().isDark();

        // draw lines representing 0 degrees (horizon) and 90 degrees (zenith)
        setupOuterColor(g2, darkTheme);
        g2.drawLine(0, 0, 0, SELECTOR_SIZE);
        g2.drawLine(0, SELECTOR_SIZE, SELECTOR_SIZE, SELECTOR_SIZE);

        // draw the elevation indicator arrow
        setupArrowColor(g2, darkTheme);
        double angle = model.getValueInRadians();
        float radius = SELECTOR_SIZE;
        float endX = (float) (centerX + radius * Math.cos(angle));
        float endY = (float) (centerY + radius * Math.sin(angle));
        // the arrow is drawn from the direction of the mouse back
        // to the rotation center, representing the incoming light
        drawArrow(g2, Math.PI + angle, endX, endY, centerX, centerY);
    }
}
