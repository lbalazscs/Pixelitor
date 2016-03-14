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

package pixelitor.tools;

import pixelitor.gui.ImageComponent;

import java.awt.event.MouseEvent;

/**
 * A MouseEvent replacement with scaled coordinates
 */
public class PMouseEvent {
    private boolean shiftDown;

    // event coordinates in image space
    private final double x;
    private final double y;

    public PMouseEvent(MouseEvent e, ImageComponent ic) {
        int mouseX = e.getX();
        int mouseY = e.getY();
        x = ic.componentXToImageSpace(mouseX);
        y = ic.componentYToImageSpace(mouseY);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    boolean wasShiftDown() {
        return shiftDown;
    }
}
