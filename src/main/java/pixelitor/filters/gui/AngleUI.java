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
import java.awt.geom.Ellipse2D;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * A GUI component for selecting angles through a full 360-degree range.
 * Displays a circle with an arrow that can be rotated to any angle
 * by dragging with the mouse.
 */
public class AngleUI extends AbstractAngleUI {
    public AngleUI(AngleParam angleParam) {
        super(angleParam);

        // the center is at the middle of the component
        centerX = SELECTOR_SIZE / 2;
        centerY = SELECTOR_SIZE / 2;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        boolean darkTheme = Themes.getActive().isDark();

        // draw the outer circle
        setupOuterColor(g2, darkTheme);
        g2.draw(new Ellipse2D.Float(0, 0, SELECTOR_SIZE, SELECTOR_SIZE));

        // draw the direction arrow
        setupArrowColor(g2, darkTheme);
        double angle = model.getValueInRadians();
        float radius = SELECTOR_SIZE / 2.0f;
        float endX = (float) (centerX + radius * Math.cos(angle));
        float endY = (float) (centerY + radius * Math.sin(angle));
        drawArrow(g2, angle, centerX, centerY, endX, endY);
    }
}
