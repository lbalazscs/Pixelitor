/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static pixelitor.tools.DragToolState.IDLE;
import static pixelitor.tools.DragToolState.INITIAL_DRAG;

/**
 * The Zoom Tool.
 */
public class ZoomTool extends DragTool {
    public ZoomTool() {
        super("Zoom", 'Z',
            "<b>click</b> to zoom in, " +
                "<b>right-click</b> (or <b>Alt-click</b>) to zoom out. " +
                "<b>Drag</b> to select an area.",
            Cursors.HAND, false);
        repositionOnSpace = true;
    }

    @Override
    public void initSettingsPanel(ResourceBundle resources) {
        settingsPanel.addAutoZoomButtons();
    }

    @Override
    public void mouseClicked(PMouseEvent e) {
        Point mousePos = e.getPoint();
        View view = e.getView();

        if (e.isRight() || (e.isLeft() && e.isAltDown())) {
            view.zoomOut(mousePos);
        } else {
            view.zoomIn(mousePos);
        }
    }

    @Override
    protected void dragStarted(PMouseEvent e) {
        if (state == IDLE) {
            setState(INITIAL_DRAG);
        } else if (state == INITIAL_DRAG) {
            throw new IllegalStateException();
        }
    }

    @Override
    protected void ongoingDrag(PMouseEvent e) {
        // triggers painting a drag rectangle over the canvas
        e.repaint();
    }

    @Override
    protected void dragFinished(PMouseEvent e) {
        if (state == INITIAL_DRAG) {
            // zoom the view to the dragged zoom rectangle
            View view = e.getView();
            PRectangle zoomRect = drag.toPosPRect(view);
            view.zoomToRegion(zoomRect);

            // we are done
            reset();
            e.consume();
        }
    }

    @Override
    public void paintOverCanvas(Graphics2D g2, Composition comp) {
        if (state == INITIAL_DRAG) {
            PRectangle zoomRect = drag.toPosPRect(comp.getView());
            Shapes.drawVisibly(g2, zoomRect.getCo());
        }
    }

    @Override
    protected void toolDeactivated(View view) {
        super.toolDeactivated(view);
        reset();
    }

    @Override
    public void reset() {
        setState(IDLE);
        Views.repaintActive();
    }

    private void setState(DragToolState newState) {
        state = newState;
    }

    @Override
    public void compReplaced(Composition newComp, boolean reloaded) {
        if (reloaded) {
            reset();
        }
    }

    @Override
    public boolean supportsUserPresets() {
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
    public Consumer<Graphics2D> createIconPainter() {
        return ToolIcons::paintZoomIcon;
    }
}
