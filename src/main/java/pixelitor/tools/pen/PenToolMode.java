/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Build;
import pixelitor.OpenImages;
import pixelitor.gui.View;
import pixelitor.tools.Tools;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.DebugNodes;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

import static pixelitor.tools.pen.PenTool.path;

public interface PenToolMode {
    void mousePressed(PMouseEvent e);

    void mouseDragged(PMouseEvent e);

    void mouseReleased(PMouseEvent e);

    // return true if needs repainting
    boolean mouseMoved(MouseEvent e, View view);

    void paint(Graphics2D g);

    void coCoordsChanged(View view);

    void imCoordsChanged(AffineTransform at);

    String getToolMessage();

    void start();

    boolean requiresExistingPath();

    default void modeStarted(PenToolMode prevMode, Path path) {
        if (path != null) {
            path.setPreferredPenToolMode(this);
        }
        if (prevMode == TRANSFORM) {
            // in rare cases the transform boxes can
            // leave their cursor even after a mode change
            OpenImages.setCursorForAll(Tools.PEN.getStartingCursor());
        }
    }

    default void modeEnded() {
        if (PenTool.hasPath()) {
            var comp = OpenImages.getActiveComp();
            if (comp != null) {
                Path path = PenTool.getPath();
                if (path.getComp() != comp) {
                    if(Build.isDevelopment()) {
                        throw new IllegalStateException(
                                "path's comp is " + path.getComp().getName()
                                + ", active comp is " + comp.getName());
                    }
                    // the pen tools has a path but it does not belong to the
                    // active composition - happened in Mac random gui tests
                    // what can we do? at least avoid consistency errors
                    // don't use removePath, because it also removes from the active comp
                    PenTool.path = null;
                } else {
                    // should be already set
                    assert comp.getActivePath() == path;
                    //comp.setActivePath(path);
                }
                comp.repaint();
            }
        } else { // no path in the pen tool
            assert OpenImages.activePathIs(null);
        }
        DraggablePoint.lastActive = null;
    }

    /**
     * Returns true if the key event was used for something
     */
    boolean arrowKeyPressed(ArrowKey key);

    default DebugNode createDebugNode() {
        var node = new DebugNode("pen tool mode " + this, this);

        if (PenTool.hasPath()) {
            node.add(DebugNodes.createPathNode(path));
        } else {
            node.addBoolean("has path", false);
        }

        return node;
    }

    // enum-like variables to make the code more readable
    PathBuilder BUILD = PathBuilder.INSTANCE;
    PathEditor EDIT = PathEditor.INSTANCE;
    PathTransformer TRANSFORM = PathTransformer.INSTANCE;
}
