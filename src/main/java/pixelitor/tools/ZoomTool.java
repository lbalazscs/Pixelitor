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

package pixelitor.tools;

import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.View;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PRectangle;
import pixelitor.utils.Cursors;
import pixelitor.utils.Shapes;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_MITER;
import static pixelitor.tools.DragToolState.*;

public class ZoomTool extends DragTool {
    private DragToolState state = NO_INTERACTION;
    private PRectangle box;

    public ZoomTool() { // Do I need this false in super call?
        super("Zoom", 'Z', "zoom_tool.png",
            "<b>click</b> to zoom in, " +
                "<b>right-click</b> (or <b>Alt-click</b>) to zoom out. " +
                "<b>Drag</b> to select an area.",
            Cursors.HAND, false);
        spaceDragStartPoint = true;
    }

    @Override
    public void initSettingsPanel() {
        settingsPanel.addAutoZoomButtons();
    }

    @Override
    public void mouseClicked(PMouseEvent e) {
        Point mousePos = e.getPoint();
        View view = e.getView();

        if (e.isRight() || (e.isLeft() && e.isAltDown())) {
            view.decreaseZoom(mousePos);
        } else {
            view.increaseZoom(mousePos);
        }
    }

    @Override
    public void dragStarted(PMouseEvent e) {
        if (state == NO_INTERACTION) {
            setState(INITIAL_DRAG);
        } else if (state == INITIAL_DRAG) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void ongoingDrag(PMouseEvent e) {
        e.repaint();
    }

    @Override
    public void dragFinished(PMouseEvent e) {
        if (state == NO_INTERACTION) {
            return;
        }

        if (state == INITIAL_DRAG) {
            if (box != null) {
                throw new IllegalStateException();
            }

            View view = e.getView();
            box = PRectangle.positiveFromCo(drag.toCoRect(), view);
            setState(TRANSFORM);

            view.zoomToRect(getZoomRect(view));
            resetInitialState();

            e.consume();
        }
    }

    @Override
    public void paintOverImage(Graphics2D g2, Composition comp) {
        if (state == NO_INTERACTION) {
            return;
        }
        PRectangle zoomRect = getZoomRect(comp.getView());
        if (zoomRect == null) {
            return;
        }

        Shapes.drawVisibly(g2, zoomRect.getCo());
    }

    private PRectangle getZoomRect(View view) {
        if (state == INITIAL_DRAG) {
            return drag.toPosPRect(view);
        } else if (state == TRANSFORM) {
            return box;
        }
        // initial state
        return null;
    }

    @Override
    protected void toolEnded() {
        super.toolEnded();
        resetInitialState();
    }

    @Override
    public void resetInitialState() {
        box = null;
        setState(NO_INTERACTION);

        Views.repaintActive();
        Views.setCursorForAll(Cursors.HAND);
    }

    private void setState(DragToolState newState) {
        state = newState;
    }

    @Override
    public void compReplaced(Composition newComp, boolean reloaded) {
        if (reloaded) {
            resetInitialState();
        }
    }

    @Override
    public void coCoordsChanged(View view) {
        if (box != null && state == TRANSFORM) {
            box.coCoordsChanged(view);
        }
    }

    @Override
    public void imCoordsChanged(AffineTransform at, Composition comp) {
        if (box != null && state == TRANSFORM) {
            box.imCoordsChanged(comp.getView(), at);
        }
    }

    @Override
    public boolean canHaveUserPresets() {
        return false;
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DebugNode createDebugNode() {
        var node = super.createDebugNode();
        node.addString("state", state.toString());
        return node;
    }

    @Override
    public Icon createIcon() {
        return new ZoomToolIcon();
    }

    private static class ZoomToolIcon extends Tool.ToolIcon {
        @Override
        public void paintIcon(Graphics2D g) {
            // based on zoom_tool.svg

            Ellipse2D circle = new Ellipse2D.Double(9, 1, 18, 18);
            g.setStroke(new BasicStroke(2.0f, CAP_BUTT, JOIN_MITER, 4));
            g.draw(circle);

            Line2D hor = new Line2D.Double(12.0, 10.0, 24.0, 10.0);
            g.draw(hor);

            Line2D ver = new Line2D.Double(18.0, 16.0, 18.0, 4.0);
            g.draw(ver);

            Path2D shape = new Path2D.Float();
            shape.moveTo(13.447782, 17.801485);
            shape.lineTo(4.73615, 26.041084);
            shape.curveTo(4.73615, 26.041084, 2.9090462, 26.923565, 1.9954941, 26.041084);
            shape.curveTo(1.0819423, 25.158604, 1.9954941, 23.393635, 1.9954941, 23.393635);
            shape.lineTo(11.043547, 14.977204);

            g.setStroke(new BasicStroke(1.7017335f, CAP_BUTT, JOIN_MITER, 4));
            g.draw(shape);
        }
    }
}
