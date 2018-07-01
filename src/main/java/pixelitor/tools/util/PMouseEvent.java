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

package pixelitor.tools.util;

import pixelitor.Composition;
import pixelitor.gui.ImageComponent;

import javax.swing.*;
import java.awt.Point;
import java.awt.event.MouseEvent;

/**
 * The "Pixelitor Mouse Event" is a wrapper around
 * the standard {@link MouseEvent} with some
 * practical added functionality
 */
public class PMouseEvent extends PPoint.Lazy {
    private final MouseEvent e;

    public PMouseEvent(MouseEvent e, ImageComponent ic) {
        super(ic, e.getX(), e.getY());
        assert e.getSource() == ic;

        this.e = e;
    }

    public JViewport getViewport() {
        return (JViewport) ic.getParent();
    }

    public Composition getComp() {
        return ic.getComp();
    }

    public MouseEvent getOrigEvent() {
        return e;
    }

    public Point getPoint() {
        return e.getPoint();
    }

    public boolean isConsumed() {
        return e.isConsumed();
    }

    public void consume() {
        e.consume();
    }

    public int getClickCount() {
        return e.getClickCount();
    }

    public boolean isShiftDown() {
        return e.isShiftDown();
    }

    public boolean isControlDown() {
        return e.isControlDown();
    }

    public boolean isPopupTrigger() {
        return e.isPopupTrigger();
    }

    public boolean isAltDown() {
        return e.isAltDown();
    }

    public boolean isLeft() {
        return SwingUtilities.isLeftMouseButton(e);
    }

    public boolean isMiddle() {
        return SwingUtilities.isMiddleMouseButton(e);
    }

    public boolean isRight() {
        return SwingUtilities.isRightMouseButton(e);
    }

    public void repaint() {
        ic.repaint();
    }
}
