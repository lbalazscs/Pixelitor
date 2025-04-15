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

package pixelitor.tools.pen;

import pixelitor.AppMode;
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.View;
import pixelitor.gui.utils.VectorIcon;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.ResourceBundle;

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_MITER;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.tools.pen.AnchorPointType.CUSP;
import static pixelitor.tools.pen.AnchorPointType.SYMMETRIC;
import static pixelitor.tools.pen.BuildState.DRAGGING_LAST_CONTROL;
import static pixelitor.tools.pen.BuildState.DRAG_EDITING_PREVIOUS;
import static pixelitor.tools.pen.BuildState.IDLE;
import static pixelitor.tools.pen.BuildState.MOVE_EDITING_PREVIOUS;
import static pixelitor.tools.pen.BuildState.MOVING_TO_NEXT_ANCHOR;
import static pixelitor.tools.pen.PathActions.setActionsEnabled;
import static pixelitor.tools.util.DraggablePoint.activePoint;
import static pixelitor.tools.util.DraggablePoint.lastActive;

public class PenTool extends PathTool {
    private static final String SHOW_RUBBER_BAND_TEXT = "Show Rubber Band";

    private final JLabel rubberBandLabel = new JLabel(SHOW_RUBBER_BAND_TEXT + ":");
    private final JCheckBox rubberBandCB = new JCheckBox("", true);
    private boolean showRubberBand = true;

    // The last mouse position. Important when the moving point
    // has to be restored after an undo
    private double lastX;
    private double lastY;

    public PenTool() {
        super("Pen",
            "<b>click, drag</b> and repeat to create a path, " +
                "<b>Ctrl-click</b> or close it to finish it. " +
                "<b>Ctrl-drag</b> moves points, " +
                "<b>Alt-drag</b> breaks handles, " +
                "<b>Shift-drag</b> constrains angles.",
            Cursors.DEFAULT);
    }

    @Override
    public void initSettingsPanel(ResourceBundle resources) {
        settingsPanel.add(rubberBandLabel);
        settingsPanel.add(rubberBandCB);
        rubberBandCB.addActionListener(e ->
            showRubberBand = rubberBandCB.isSelected());
        rubberBandCB.setName("rubberBandCB");

        // also add the common buttons
        super.initSettingsPanel(resources);
    }

    @Override
    public void reset() {
        Composition comp = Views.getActiveComp();
        setActionsEnabled(comp == null ? false : comp.hasActivePath());
    }

    @Override
    protected void toolActivated(View view) {
        super.toolActivated(view);
        Path path = null;
        if (view != null) {
            Composition comp = view.getComp();
            path = comp.getActivePath();

            // the coordinates might have changed while using another tool,
            // but other tools don't update the path component coordinates
            coCoordsChanged(view);
            if (path != null) {
                view.repaint();
            }    
        } else {
            assert path == null;
        }
        setActionsEnabled(path != null);
    }

    @Override
    protected void toolDeactivated(View view) {
        super.toolDeactivated(view);

        Path path = view.getComp().getActivePath();
        if (path != null) {
            assert path.checkInvariants();
            if (!path.getActiveSubpath().isFinished()) {
                path.finishActiveSubpath(false);
            } else {
                path.assertStateIs(IDLE);
            }

            lastActive = null;
            Views.repaintActive(); // visually hide the path
        }
    }

    public boolean showPathPreview() {
        return showRubberBand;
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        Composition comp = e.getComp();
        Path path = comp.getActivePath();

        if (path == null) {
            path = new Path(comp, true);
            comp.setActivePath(path);
        }

        BuildState state = path.getBuildState();
        if (state == DRAGGING_LAST_CONTROL) {
            state = handleUnexpectedDragState("mousePressed", e.getView(), path);
        }

        double x = e.getCoX();
        double y = e.getCoY();
        lastX = x;
        lastY = y;

        if (state == IDLE) {
            if (e.isControlDown()) {
                if (handleCtrlPressBeforeSubpath(e.isAltDown(), x, y, path)) {
                    return;
                }
            } else if (e.isAltDown()) {
                if (handleAltPressBeforeSubpath(x, y, path)) {
                    return;
                }
            }

            SubPath subPath = path.startNewSubpath();
            AnchorPoint anchor = new AnchorPoint(e, subPath);
            subPath.addStartingAnchor(anchor, true);
            anchor.ctrlOut.mousePressed(x, y);
        } else if (state.isMoving()) {
            if (state == MOVING_TO_NEXT_ANCHOR) {
                assert path.hasMovingPoint();
            }

            if (e.isControlDown()) {
                handleCtrlPressWhileMoving(e, e.isAltDown(), x, y, path);
                return;
            }

            boolean altDownHitNothing = false;
            if (e.isAltDown()) {
                DraggablePoint handle = path.findHandleAt(x, y, true);
                if (handle != null) {
                    if (handle instanceof ControlPoint cp) {
                        breakAndStartMoving(cp, x, y, path);
                        return;
                    } else if (handle instanceof AnchorPoint ap) {
                        startDraggingOutNewHandles(ap, x, y, path);
                        return;
                    }
                } else {
                    altDownHitNothing = true;
                }
            }

            if (path.tryClosing(x, y)) {
                e.repaint();
                return;
            }
            finalizeMovingPoint(x, y, altDownHitNothing, e.isShiftDown(), path);
        }

        path.setBuildState(DRAGGING_LAST_CONTROL);
        assert path.checkInvariants();
    }

    private static void finalizeMovingPoint(double x, double y, boolean altDownHitNothing, boolean shiftDown, Path path) {
        path.getMovingPoint().mouseReleased(x, y, shiftDown);
        AnchorPoint anchor = path.convertMovingPointToAnchor();
        if (altDownHitNothing) {
            anchor.setType(CUSP);
        }
        anchor.ctrlOut.mousePressed(x, y);
    }

    private static boolean handleCtrlPressBeforeSubpath(boolean altDown,
                                                        double x, double y, Path path) {
        // if we are over an old point, just move it
        DraggablePoint handle = path.findHandleAt(x, y, altDown);
        if (handle != null) {
            startMovingPrevious(handle, x, y, path);
            return true;
        }
        return false;
    }

    private static boolean handleAltPressBeforeSubpath(double x, double y, Path path) {
        // if only alt is down, then break the control points
        DraggablePoint handle = path.findHandleAt(x, y, true);
        if (handle != null) {
            if (handle instanceof ControlPoint cp) {
                breakAndStartMoving(cp, x, y, path);
                return true;
            } else if (handle instanceof AnchorPoint ap) {
                startDraggingOutNewHandles(ap, x, y, path);
                return true;
            }
        }
        return false;
    }

    private static void handleCtrlPressWhileMoving(PMouseEvent e, boolean altDown,
                                                   double x, double y, Path path) {
        DraggablePoint handle = path.findHandleAt(x, y, altDown);
        if (handle != null) {
            startMovingPrevious(handle, x, y, path);
        } else {
            // control is down, but nothing was hit
            path.finishSubPathByCtrlClick(e.getComp());
        }
    }

    private static void startDraggingOutNewHandles(AnchorPoint ap, double x, double y, Path path) {
        ap.retractHandles();
        // drag the retracted handles out
        ap.setType(SYMMETRIC);
        startMovingPrevious(ap.ctrlOut, x, y, path);
    }

    private static void breakAndStartMoving(ControlPoint cp, double x, double y, Path path) {
        cp.breakOrDragOut();

        // after breaking, move it as usual
        startMovingPrevious(cp, x, y, path);
    }

    private static void startMovingPrevious(DraggablePoint point, double x, double y, Path path) {
        point.setActive(true);
        point.mousePressed(x, y);
        path.setBuildState(DRAG_EDITING_PREVIOUS);
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        Composition comp = e.getComp();
        Path path = comp.getActivePath();

        if (path == null) {
            return;
        }
        BuildState state = path.getBuildState();
        if (state == IDLE) {
            return;
        }

        if (state.isMoving()) {
            state = handleUnexpectedMoveState("mouseDragged", e.getView(), state, path);
            if (state == IDLE) {
                return;
            }
        }

        double x = e.getCoX();
        double y = e.getCoY();
        lastX = x;
        lastY = y;

        if (state == DRAG_EDITING_PREVIOUS) {
            activePoint.mouseDragged(x, y, e.isShiftDown());
            path.moveMovingPointTo(x, y, true);
            return;
        }

        boolean breakHandle = e.isAltDown();
        AnchorPoint last = path.getLastAnchor();
        if (breakHandle) {
            last.setType(CUSP);
        } else {
            last.setType(SYMMETRIC);
        }
        last.ctrlOut.mouseDragged(x, y, e.isShiftDown());

        path.setBuildState(DRAGGING_LAST_CONTROL);
        e.repaint();
    }

    @Override
    public void mouseReleased(PMouseEvent e) {
        Composition comp = e.getComp();
        Path path = comp.getActivePath();

        if (path == null) {
            return;
        }
        BuildState state = path.getBuildState();
        if (state == IDLE) {
            return;
        }

        if (state.isMoving()) {
            state = handleUnexpectedMoveState("mouseReleased", e.getView(), state, path);
            if (state == IDLE) {
                return;
            }
        }
        assert state.isDragging() : "state = " + state;

        double x = e.getCoX();
        double y = e.getCoY();
        lastX = x;
        lastY = y;

        if (state == DRAG_EDITING_PREVIOUS) {
            activePoint.mouseReleased(x, y, e.isShiftDown());
            activePoint.createMovedEdit(e.getComp()).ifPresent(path::handleMoved);
            if (path.getPrevBuildState() == IDLE) {
                path.setBuildState(IDLE);
            } else {
                if (e.isControlDown() || e.isAltDown()) {
                    path.setBuildState(MOVE_EDITING_PREVIOUS);
                } else {
                    path.setBuildState(MOVING_TO_NEXT_ANCHOR);
                }
            }

            e.repaint();
            return;
        } else if (state == DRAGGING_LAST_CONTROL) {
            AnchorPoint last = path.getLastAnchor();
            ControlPoint ctrlOut = last.ctrlOut;
            ctrlOut.mouseReleased(x, y, e.isShiftDown());
            if (!ctrlOut.isRetracted()) {
                last.changeTypeFromSymToSmooth();
            }

            SubPath subpath = path.getActiveSubpath();
            MovingPoint moving = new MovingPoint(x, y, last, last.getView());
            moving.mousePressed(x, y);
            subpath.setMovingPoint(moving);
        }

        if (e.isControlDown() || e.isAltDown()) {
            path.setBuildState(MOVE_EDITING_PREVIOUS);
        } else {
            path.setBuildState(MOVING_TO_NEXT_ANCHOR);
        }

        assert path.checkInvariants();
        e.repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e, View view) {
        Composition comp = view.getComp();
        Path path = comp.getActivePath();

        if (path == null) {
            return;
        }
        BuildState state = path.getBuildState();
        if (state == DRAGGING_LAST_CONTROL) {
            state = handleUnexpectedDragState("mouseMoved", view, path);
        }

        int x = e.getX();
        int y = e.getY();
        lastX = x;
        lastY = y;

        if (e.isControlDown() || e.isAltDown()) {
            if (state != IDLE) {
                path.setBuildState(MOVE_EDITING_PREVIOUS);
            }

            DraggablePoint handle = path.findHandleAt(x, y, e.isAltDown());
            if (handle != null) {
                handle.setActive(true);
            } else {
                activePoint = null;
            }
        } else {
            if (state == IDLE) {
                return;
            }

            path.setBuildState(MOVING_TO_NEXT_ANCHOR);
            path.getMovingPoint().mouseDragged(x, y, e.isShiftDown());

            AnchorPoint first = path.getFirstAnchor();
            if (first.contains(x, y)) {
                first.setActive(true);
            } else {
                activePoint = null;
            }
        }

        view.repaint();
    }

    private static BuildState handleUnexpectedDragState(String where, View view, Path path) {
        if (AppMode.isDevelopment()) {
            System.out.printf("PathBuilder::handleUnexpectedDragState: " +
                "where = '%s, active = %s'%n", where, view.isActive());
        }

        path.setBuildState(MOVING_TO_NEXT_ANCHOR);
        return path.getBuildState();
    }

    private static BuildState handleUnexpectedMoveState(String where, View view, BuildState state, Path path) {
        if (AppMode.isDevelopment()) {
            System.out.printf("PathBuilder::handleUnexpectedMoveState: " +
                "where = '%s, active = %s'%n", where, view.isActive());
        }

        BuildState dragState = IDLE;
        if (state == MOVE_EDITING_PREVIOUS) {
            dragState = DRAG_EDITING_PREVIOUS;
        }
        path.setBuildState(dragState);
        return dragState;
    }

    @Override
    public void paintOverCanvas(Graphics2D g2, Composition comp) {
        Path compPath = comp.getActivePath();
        if (compPath != null) {
            g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
            compPath.paintForBuilding(g2);
        }
    }

    @Override
    public void coCoordsChanged(View view) {
        Path path = view.getComp().getActivePath();

        if (path != null) {
            path.coCoordsChanged(view);
        }
    }

    @Override
    public void imCoordsChanged(AffineTransform at, View view) {
        //do nothing
    }

    @Override
    public boolean arrowKeyPressed(ArrowKey key) {
        View view = Views.getActive();
        if (view == null) {
            return false;
        }
        Path path = view.getComp().getActivePath();
        if (path == null) {
            // there's nothing to nudge
            return false;
        }

        // an active point is always checked first
        if (activePoint != null) {
            activePoint.arrowKeyPressed(key);
            return true;
        }

        BuildState state = path.getBuildState();
        if (state == MOVING_TO_NEXT_ANCHOR || state == DRAGGING_LAST_CONTROL) {
            // If a new subpath is being created,
            // move the most recently placed anchor point
            AnchorPoint last = path.getLastAnchor();
            if (last != null) {
                last.arrowKeyPressed(key);
                return true;
            }
        } else {
            // move the last active point
            if (lastActive != null) {
                lastActive.arrowKeyPressed(key);
                return true;
            }
        }

        return false;
    }

    public MovingPoint createMovingPoint(SubPath sp) {
        AnchorPoint last = sp.getLastAnchor();
        return new MovingPoint(lastX, lastY, last, last.getView());
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        super.saveStateTo(preset);

        preset.putBoolean(SHOW_RUBBER_BAND_TEXT, rubberBandCB.isSelected());
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        super.loadUserPreset(preset);

        rubberBandCB.setSelected(preset.getBoolean(SHOW_RUBBER_BAND_TEXT));
    }

    @Override
    public VectorIcon createIcon() {
        return new BuildPathToolIcon();
    }

    private static class BuildPathToolIcon extends ToolIcon {
        @Override
        public void paintIcon(Graphics2D g) {
            // based on pen_tool.svg

            g.setStroke(new BasicStroke(1.5f, CAP_BUTT, JOIN_MITER, 4));

            Path2D.Double body = new Path2D.Double();
            body.moveTo(20.182807, -0.58412839);
            body.lineTo(15.885317, 4.1900467);
            body.lineTo(24.480293, 13.738471);
            body.lineTo(28.77778, 8.9642464);
            g.draw(body);

            Path2D.Double head = new Path2D.Double();
            head.moveTo(18.504954, 7.3429552);
            head.curveTo(18.504954, 7.3429552, 13.526367, 11.562249, 5.7244174, 11.725182); // Control points are the start point
            head.lineTo(3.5014621, 24.583714);
            head.lineTo(17, 22.5);
            head.curveTo(17, 22.5, 17.107143, 15.553571, 21.218165, 10.083241); // Control points are the end point
            head.closePath();
            g.draw(head);

            Line2D line = new Line2D.Double(3.95, 24.0, 10.09609, 18);
            g.draw(line);

            Ellipse2D circle = new Ellipse2D.Double(9, 14.375, 4.8, 4.8);
            g.draw(circle);
        }
    }
}
