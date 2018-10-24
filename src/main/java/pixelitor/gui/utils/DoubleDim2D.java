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

package pixelitor.gui.utils;

import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;

/**
 * The abstract JDK {@link Dimension2D} class doesn't have
 * a subclass with double precision in the JDK, so this is one.
 */
public class DoubleDim2D extends Dimension2D {
    private double width;
    private double height;

    public DoubleDim2D(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public DoubleDim2D(Rectangle2D rect) {
        this.width = rect.getWidth();
        this.height = rect.getHeight();
    }

    @Override
    public double getWidth() {
        return width;
    }

    @Override
    public double getHeight() {
        return height;
    }

    @Override
    public void setSize(double width, double height) {
        this.width = width;
        this.height = height;
    }
}
