/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public interface View {
    double componentXToImageSpace(double mouseX);

    double componentYToImageSpace(double mouseY);

    double imageXToComponentSpace(double x);

    double imageYToComponentSpace(double y);

    Rectangle2D fromComponentToImageSpace(Rectangle input);

    Rectangle fromImageToComponentSpace(Rectangle2D input);

    AffineTransform getImageToComponentTransform();

    AffineTransform getComponentToImageTransform();

    void repaint();
}
