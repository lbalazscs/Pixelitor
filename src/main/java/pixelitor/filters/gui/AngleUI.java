/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
 * A GUI component that can be used to select
 * an angle with the mouse
 */
public class AngleUI extends AbstractAngleUI {
    public AngleUI(AngleParam angleParam) {
        super(angleParam);

        cx = SIZE / 2;
        cy = SIZE / 2;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        boolean darkTheme = Themes.getCurrent().isDark();
        Graphics2D g2 = (Graphics2D) g;
        setupOuterColor(g2, darkTheme);
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g2.draw(new Ellipse2D.Float(0, 0, SIZE, SIZE));

        double angle = model.getValueInRadians();

        int radius = SIZE / 2;
        float endX = (float) (cx + radius * Math.cos(angle));
        float endY = (float) (cy + radius * Math.sin(angle));

        setupArrowColor(g2, darkTheme);
        drawArrow(g2, angle, cx, cy, endX, endY);
    }
}
