/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import com.bric.geom.ShapeStringUtils;
import pixelitor.AppMode;
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.View;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.TaskAction;
import pixelitor.gui.utils.VectorIcon;
import pixelitor.history.History;
import pixelitor.history.PixelitorEdit;
import pixelitor.io.FileIO;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.pen.history.ConvertPathToSelectionEdit;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;
import pixelitor.utils.Messages;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.util.ResourceBundle;

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.CAP_ROUND;
import static java.awt.BasicStroke.JOIN_MITER;
import static java.awt.BasicStroke.JOIN_ROUND;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.tools.pen.PenToolMode.BUILD;
import static pixelitor.tools.pen.PenToolMode.EDIT;
import static pixelitor.tools.pen.PenToolMode.TRANSFORM;

/**
 * The pen tool.
 */
public class PenTool extends Tool {
    private static final PenToolMode[] MODES = {BUILD, EDIT, TRANSFORM};
    private final ComboBoxModel<PenToolMode> modeModel =
        new DefaultComboBoxModel<>(MODES);

    private final Action toSelectionAction;
    private final Action exportSVGAction;

    private static final String SHOW_RUBBER_BAND_TEXT = "Show Rubber Band";
    private final JLabel rubberBandLabel = new JLabel(SHOW_RUBBER_BAND_TEXT + ":");
    private final JCheckBox rubberBandCB = new JCheckBox("", true);

    private PenToolMode mode = BUILD;
    private boolean suppressModeChangeEvents = false;

    public static Path path;

    private boolean rubberBand = true;

    private static final Action traceWithBrushAction = new TraceAction(
        "Stroke with Brush", Tools.BRUSH);
    private static final Action traceWithEraserAction = new TraceAction(
        "Stroke with Eraser", Tools.ERASER);
    private static final Action traceWithSmudgeAction = new TraceAction(
        "Stroke with Smudge", Tools.SMUDGE);

    private static final Action deletePathAction = new TaskAction(
        "Delete Path", PenTool::deletePath);

    public PenTool() {
        super("Pen", 'P',
            "", // getStatusBarMessage() is overridden
            Cursors.DEFAULT);

        pixelSnapping = true;
        toSelectionAction = new TaskAction("Convert to Selection", this::convertToSelection);
        exportSVGAction = new TaskAction("Export SVG...", PenTool::exportSVG);

        setActionsEnabled(false);
    }

    @Override
    public void initSettingsPanel(ResourceBundle resources) {
        JComboBox<PenToolMode> modeSelector = new JComboBox<>(modeModel);

        modeSelector.addActionListener(e -> handleUIModeChange());
        settingsPanel.addComboBox("Mode:", modeSelector, "modeSelector");

        settingsPanel.add(rubberBandLabel);
        settingsPanel.add(rubberBandCB);
        rubberBandCB.addActionListener(e ->
            rubberBand = rubberBandCB.isSelected());
        rubberBandCB.setName("rubberBandCB");

        settingsPanel.addButton(toSelectionAction, "toSelectionButton",
            "Convert the path to a selection");
        settingsPanel.addButton(exportSVGAction, "exportSVGButton",
            "Export the path to an SVG file");
        settingsPanel.addButton(traceWithBrushAction, "traceWithBrush",
            "Stroke the path using the current Brush Tool settings");
        settingsPanel.addButton(traceWithEraserAction, "traceWithEraser",
            "Stroke the path using the current Eraser Tool settings");
        settingsPanel.addButton(traceWithSmudgeAction, "traceWithSmudge",
            "Stroke the path using the current Smudge Tool settings");
        settingsPanel.addButton(deletePathAction, "deletePath",
            "Delete the path");
    }

    private PenToolMode getSelectedMode() {
        return (PenToolMode) modeModel.getSelectedItem();
    }

    // TODO should be private and startMode should be always called instead?
    //   also see changeMode
    public void setModeInUI(PenToolMode mode) {
        modeModel.setSelectedItem(mode);
    }

    private void handleUIModeChange() {
        if (suppressModeChangeEvents) {
            return;
        }

        assert Views.activePathIs(path) :
            "path = " + path + ", active path = " + Views.getActivePath();

        activateMode(getSelectedMode(), true);
    }

    public void activateMode(PenToolMode newMode, boolean userInitiated) {
        if (path == null && newMode.requiresExistingPath()) {
            handleInvalidModeChange(newMode);
            return;
        }

        // update the mode combo box if this wasn't trieggered by it
        if (!userInitiated) {
            suppressModeChangeEvents = true;
            setModeInUI(newMode);
            suppressModeChangeEvents = false;
        }

        // switch the mode
        if (this.mode != newMode) {
            this.mode.modeDeactivated(Views.getActiveComp());
            newMode.modeActivated(this.mode);
        }
        this.mode = newMode;

        // additional state updates
        rubberBandLabel.setEnabled(newMode == BUILD);
        rubberBandCB.setEnabled(newMode == BUILD);
        Messages.showStatusMessage(newMode.getToolMessage());
        setActionsEnabled(hasPath());
        Views.repaintActive();

        assert checkPathConsistency();
    }

    private void handleInvalidModeChange(PenToolMode mode) {
        if (AppMode.isUnitTesting()) {
            throw new IllegalStateException("start restricted mode with null path");
        }
        if (RandomGUITest.isRunning()) {
            // can happen when randomizing the tool settings
            return;
        }
        EventQueue.invokeLater(() -> {
            String action = mode == EDIT ? "edit" : "transform";
            Dialogs.showInfoDialog("No Path",
                "<html>There is no path to " + action + ". " +
                    "You can create a path<ul>" +
                    "<li>in build mode</li>" +
                    "<li>by converting a selection into a path</li>" +
                    "</ul>");
            setModeInUI(BUILD);
        });
    }

    public PenToolMode getMode() {
        return mode;
    }

    public boolean modeIs(PenToolMode otherMode) {
        return mode == otherMode;
    }

    public boolean modeIsNot(PenToolMode otherMode) {
        return mode != otherMode;
    }

    @Override
    public String getStatusBarMessage() {
        return mode.getToolMessage();
    }

    public void convertToSelection() {
        Path oldPath = path;

        Shape shape = path.toImageSpaceShape();
        Composition comp = Views.getActiveComp();

        PixelitorEdit selectionEdit = comp.changeSelection(shape);
        if (selectionEdit == null) {
            Dialogs.showInfoDialog(comp.getDialogParent(), "No Selection",
                "No selection was created because the path is outside the canvas.");
            return;
        }

        PenToolMode oldMode = mode;
        removePath();
        comp.pathChanged(true);
        History.add(new ConvertPathToSelectionEdit(
            comp, oldPath, selectionEdit, oldMode));
        assert checkPathConsistency();

        Tools.SELECTION.activate();
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        mode.mousePressed(e);
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        mode.mouseDragged(e);
        e.repaint();
    }

    @Override
    public void mouseReleased(PMouseEvent e) {
        mode.mouseReleased(e);
        e.repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e, View view) {
        if (mode.mouseMoved(e, view)) {
            view.repaint();
        }
    }

    @Override
    public void paintOverImage(Graphics2D g2, Composition comp) {
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        mode.paint(g2);
    }

    @Override
    public boolean arrowKeyPressed(ArrowKey key) {
        View view = Views.getActive();
        if (view == null) {
            return false;
        }
        if (mode.arrowKeyPressed(key, view)) {
            view.repaint();
            return true;
        }
        return false;
    }

    @Override
    public void coCoordsChanged(View view) {
        if (hasPath()) {
            path.coCoordsChanged(view);
            mode.coCoordsChanged(view);
        }
    }

    @Override
    public void imCoordsChanged(AffineTransform at, View view) {
        if (hasPath()) {
            mode.imCoordsChanged(at, view);
        }
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        boolean restartMode = false;

        if (oldView != null) { // is null if the first image is opened with active pen tool
            Composition oldComp = oldView.getComp();
            Path oldPath = oldComp.getActivePath();
            if (oldPath != null) {
                oldPath.setPreferredPenToolMode(mode);
            }

            restartMode = true;
            // Even if the mode doesn't change, end it with the
            // old composition and start it with the new one.
            mode.modeDeactivated(oldComp);
        }

        super.viewActivated(oldView, newView);
        path = newView.getComp().getActivePath();

        if (restartMode) {
            mode.modeActivated(mode);
        }

        assert Views.getActive() == newView;
        assert checkPathConsistency();
    }

    @Override
    public void compReplaced(Composition newComp, boolean reloaded) {
        if (newComp.isActive()) {
            // reloading is asynchronous, the view might not be active anymore
            setPathFromComp(newComp);
            mode.compReplaced();
        }
    }

    @Override
    public void resetInitialState() {
        setPathFromComp(Views.getActiveComp());

        assert checkPathConsistency();
    }

    @Override
    protected void toolActivated() {
        super.toolActivated();

        View view = Views.getActive();
        if (view != null) {
            setPathFromComp(view.getComp());

            // the coordinates might have changed while using another tool,
            // but other tools don't update the path component coordinates
            coCoordsChanged(view);
        } else {
            assert path == null;
        }

        mode.modeActivated(null);

        assert checkPathConsistency();
    }

    @SuppressWarnings("SameReturnValue")
    public static boolean checkPathConsistency() {
        assert Views.activePathIs(path) :
            "path = " + path + ", active path = " + Views.getActivePath();

        Composition activeComp = Views.getActiveComp();
        if (activeComp == null) {
            return true;
        }
        if (hasPath() && path.getComp() != activeComp) {
            throw new IllegalStateException("foreign path " + path
                + ", path comp = " + path.getComp().toPathDebugString()
                + ", active comp = " + activeComp.toPathDebugString());
        }
        if (hasPath()) {
            path.checkConsistency();
        }
        return true;
    }

    private void setPathFromComp(Composition comp) {
        if (comp == null) {
            clearToolPath();
        } else {
            Path compPath = comp.getActivePath();
            if (compPath == null) {
                clearToolPath();
            } else {
                path = compPath;
                PenToolMode preferredMode = compPath.getPreferredPenToolMode();
                if (preferredMode != null && preferredMode != mode) {
                    activateMode(preferredMode, false);
                }
            }
            comp.repaint();
        }
        setActionsEnabled(path != null);
    }

    private static void deletePath() {
        path.delete();
    }

    // removes a path from the tool and from the composition,
    // without adding a history edit.
    public void removePath() {
        Views.setActivePath(null);
        clearToolPath();
        setActionsEnabled(false);
    }

    private void clearToolPath() {
        path = null;
        if (mode.requiresExistingPath()) {
            activateMode(BUILD, false);
        }
    }

    public static Path getPath() {
        return path;
    }

    public void setPath(Path newPath) {
        if (newPath == null) { // can happen when undoing
            removePath();
            return;
        }
        path = newPath;

        assert checkPathConsistency();
    }

    @Override
    protected void toolDeactivated() {
        super.toolDeactivated();
        mode.modeDeactivated(Views.getActiveComp());

        assert checkPathConsistency();
    }

    public void setActionsEnabled(boolean b) {
        toSelectionAction.setEnabled(b);
        exportSVGAction.setEnabled(b);

        traceWithBrushAction.setEnabled(b);
        traceWithEraserAction.setEnabled(b);
        traceWithSmudgeAction.setEnabled(b);

        deletePathAction.setEnabled(b);
    }

    public boolean arePathActionsEnabled() {
        return toSelectionAction.isEnabled();
    }

    public boolean showPathPreview() {
        return rubberBand && mode == BUILD;
    }

    public static boolean hasPath() {
        return path != null;
    }

    public static Action getTraceWithBrushAction() {
        return traceWithBrushAction;
    }

    public static Action getTraceWithEraserAction() {
        return traceWithEraserAction;
    }

    public static Action getTraceWithSmudgeAction() {
        return traceWithSmudgeAction;
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        preset.put("Mode", getSelectedMode().toString());
        preset.putBoolean(SHOW_RUBBER_BAND_TEXT, rubberBandCB.isSelected());
        if (hasPath()) {
            preset.put("Path", ShapeStringUtils.toString(path.toImageSpaceShape()));
        }
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        String modeString = preset.get("Mode");
        for (PenToolMode toolMode : MODES) {
            if (toolMode.toString().equals(modeString)) {
                activateMode(toolMode, false);
                break;
            }
        }

        rubberBandCB.setSelected(preset.getBoolean(SHOW_RUBBER_BAND_TEXT));

        String pathString = preset.get("Path");
        if (pathString != null) {
            Composition comp = Views.getActiveComp();
            if (comp != null) {
                Shape pathShape = ShapeStringUtils.createGeneralPath(pathString);
                comp.createPathFromShape(pathShape, false, false);
            }
        }
    }

    @Override
    public String getStateInfo() {
        return mode + ", hasPath=" + hasPath();
    }

    private static void exportSVG() {
        FileIO.saveSVG(path.toImageSpaceShape(), null, "path.svg");
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);
        node.add(mode.createDebugNode());
        return node;
    }

    @Override
    public VectorIcon createIcon() {
        return new PenToolIcon();
    }

    private static class PenToolIcon extends Tool.ToolIcon {
        @Override
        public void paintIcon(Graphics2D g) {
            // based on pen_tool.svg
            Path2D shape = new Path2D.Float();

            // _0_0_0
            shape.moveTo(21.943825, -0.9771605);
            shape.lineTo(18.514313, 2.9093096);
            shape.lineTo(25.373335, 10.68231);
            shape.lineTo(28.802845, 6.7957993);

            g.setStroke(new BasicStroke(1.5f, CAP_BUTT, JOIN_MITER, 4));
            g.draw(shape);

            // _0_0_1
            shape = new Path2D.Float();
            shape.moveTo(20.604853, 5.4759693);
            shape.curveTo(20.604853, 5.4759693, 16.346796, 8.32926, 10.40564, 9.043369);
            shape.lineTo(8.63166, 19.511);
            shape.lineTo(19.275534, 17.76645);
            shape.curveTo(19.275534, 17.76645, 20.935696, 11.648221, 22.770067, 7.706731);

            g.draw(shape);

            // _0_0_2
            shape = new Path2D.Float();
            shape.moveTo(16.814209, 13.010571);
            shape.curveTo(16.842209, 14.07231, 16.040462, 14.956733, 15.023422, 14.986025);
            shape.curveTo(14.006381, 15.015317, 13.159144, 14.178387, 13.131027, 13.11665);
            shape.curveTo(13.10291, 12.0549135, 13.904558, 11.170394, 14.921596, 11.14098);
            shape.curveTo(15.938634, 11.111565, 16.785963, 11.948393, 16.814198, 13.010126);

            g.setStroke(new BasicStroke(1.0f, CAP_ROUND, JOIN_ROUND, 4));
            g.draw(shape);

            // _0_0_3
            Line2D line = new Line2D.Double(9, 19, 14, 14);
            g.setStroke(new BasicStroke(1.0f, CAP_BUTT, JOIN_MITER, 4));
            g.draw(line);

            // _0_0_4
            shape = new Path2D.Float();
            shape.moveTo(2.4258666, 4.2028494);
            shape.curveTo(2.26799, 18.16415, 9.739916, 25.78004, 23.500244, 25.54416);

            g.setStroke(new BasicStroke(1.0f, CAP_BUTT, JOIN_MITER, 4));
            g.draw(shape);

            // _0_0_5
            line = new Line2D.Double(10.0, 25.5, 23.5, 25.5);
            g.draw(line);

            // _0_0_6
            Rectangle2D rect = new Rectangle2D.Double(23.5, 23.5, 4.0, 4.0);
            g.setStroke(new BasicStroke(1.0f, CAP_ROUND, JOIN_MITER, 4));
            g.draw(rect);

            // _0_0_7
            rect = new Rectangle2D.Double(6.5, 23.5, 4.0, 4.0);
            g.draw(rect);

            // _0_0_8
            shape = new GeneralPath();
            shape.moveTo(2.4098015, 4.4834995);
            shape.curveTo(2.6331227, 19.03479, 2.4901965, 17.52837, 2.4901965, 17.52837);

            g.setStroke(new BasicStroke(1.0f, CAP_BUTT, JOIN_MITER, 4));
            g.draw(shape);

            // _0_0_9
            rect = new Rectangle2D.Double(0.5, 17.5, 4.0, 4.0);
            g.setStroke(new BasicStroke(1.0f, CAP_ROUND, JOIN_MITER, 4));
            g.draw(rect);

            // _0_0_10
            rect = new Rectangle2D.Double(0.5, 0.5, 4.0, 4.0);
            g.draw(rect);
        }
    }
}
