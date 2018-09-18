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

package pixelitor.tools.pen;

import pixelitor.Composition;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.PathNode;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

import static pixelitor.tools.pen.PenTool.path;

public interface PenToolMode {
    void mousePressed(PMouseEvent e);

    void mouseDragged(PMouseEvent e);

    void mouseReleased(PMouseEvent e);

    // return true if needs repainting
    boolean mouseMoved(MouseEvent e, ImageComponent ic);

    void paint(Graphics2D g);

    String getToolMessage();

    void start();

    default void modeStarted(Path path) {
        if (path != null) {
            path.setPreferredPenToolMode(this);
        }
    }

    default void modeEnded() {
        if (path != null) {
            Composition comp = ImageComponents.getActiveCompOrNull();
            if (comp != null) {
                comp.repaint();
            }
        }
    }

    default DebugNode createDebugNode() {
        DebugNode node = new DebugNode("PenToolMode " + toString(), this);

        if (path != null) {
            node.add(new PathNode(path));
        } else {
            node.addBoolean("Has Path", false);
        }

        return node;
    }

    // enum-like variables to make the code more readable
    PathBuilder BUILD = PathBuilder.INSTANCE;
    PathEditor EDIT = PathEditor.INSTANCE;
}
