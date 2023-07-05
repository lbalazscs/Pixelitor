/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.GUIMode;
import pixelitor.Views;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.View;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.PAction;
import pixelitor.gui.utils.VectorIcon;
import pixelitor.history.History;
import pixelitor.history.PixelitorEdit;
import pixelitor.io.IO;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.pen.history.ConvertPathToSelectionEdit;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;
import pixelitor.utils.Messages;
import pixelitor.utils.VisibleForTesting;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.*;

import static java.awt.BasicStroke.*;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.tools.pen.PenToolMode.*;

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
    private boolean ignoreModeChooser = false;

    public static Path path;

    private boolean rubberBand = true;

    private static final Action traceWithBrushAction = new TraceAction(
        "Stroke with Brush", Tools.BRUSH);
    private static final Action traceWithEraserAction = new TraceAction(
        "Stroke with Eraser", Tools.ERASER);
    private static final Action traceWithSmudgeAction = new TraceAction(
        "Stroke with Smudge", Tools.SMUDGE);

    private static final Action deletePath = new PAction("Delete Path", new Runnable() {
        @Override
        public void run() {
            path.delete();
        }
    });

    public PenTool() {
        super("Pen", 'P',
            "", // getStatusBarMessage() is overridden
            Cursors.DEFAULT);

        pixelSnapping = true;
        toSelectionAction = new PAction("Convert to Selection", this::convertToSelection);
        exportSVGAction = new PAction("Export SVG...", PenTool::exportSVG);

        enableActions(false);
    }

    @Override
    public void initSettingsPanel() {
        JComboBox<PenToolMode> modeChooser = new JComboBox<>(modeModel);

        modeChooser.addActionListener(e -> onModeChooserAction());
        settingsPanel.addComboBox("Mode:", modeChooser, "modeChooser");

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
            "Stroke the path using the current settings of the Brush Tool");
        settingsPanel.addButton(traceWithEraserAction, "traceWithEraser",
            "Stroke the path using the current settings of the Eraser Tool");
        settingsPanel.addButton(traceWithSmudgeAction, "traceWithSmudge",
            "Stroke the path using the current settings of the Smudge Tool");
        settingsPanel.addButton(deletePath, "deletePath",
            "Delete the path");
    }

    private PenToolMode getSelectedMode() {
        return (PenToolMode) modeModel.getSelectedItem();
    }

    // TODO should be private and startMode should be always called instead?
    //   also see changeMode
    public void setModeChooserCombo(PenToolMode mode) {
        modeModel.setSelectedItem(mode);
    }

    private void onModeChooserAction() {
        if (ignoreModeChooser) {
            return;
        }

        assert Views.activePathIs(path) :
            "path = " + path + ", active path = " + Views.getActivePath();

        PenToolMode selectedMode = getSelectedMode();
        if (selectedMode == BUILD) {
            startBuilding(true);
        } else {
            startMode(selectedMode, true);
        }
    }

    public void startBuilding(boolean calledFromModeChooser) {
        if (!calledFromModeChooser) {
            ignoreModeChooser = true;
            setModeChooserCombo(BUILD);
            ignoreModeChooser = false;
        }
        changeMode(BUILD);
        enableActions(hasPath());
        Views.repaintActive();

        assert checkPathConsistency();
    }

    public void startMode(PenToolMode mode, boolean calledFromModeChooser) {
        if (path == null && mode != BUILD) {
            if (GUIMode.isUnitTesting()) {
                throw new IllegalStateException("start restricted mode with null path");
            }
            if (RandomGUITest.isRunning()) {
                // can happen when randomizing the tool settings
                return;
            }
            EventQueue.invokeLater(() -> {
                String requestedAction = mode == EDIT ? "edit" : "transform";
                Dialogs.showInfoDialog("No Path",
                    "<html>There is no path to " + requestedAction + ". " +
                        "You can create a path<ul>" +
                        "<li>in build mode</li>" +
                        "<li>by converting a selection into a path</li>" +
                        "</ul>");
                setModeChooserCombo(BUILD);
            });
            return;
        }

        if (!calledFromModeChooser) {
            ignoreModeChooser = true;
            setModeChooserCombo(mode);
            ignoreModeChooser = false;
        }

        changeMode(mode);
        enableActions(true);
        Views.repaintActive();

        assert checkPathConsistency();
    }

    // This method should not be called directly,
    // otherwise the mode and the combo box get out of sync.
    private void changeMode(PenToolMode mode) {
        if (this.mode != mode) {
            this.mode.modeEnded(Views.getActiveComp());
            mode.modeStarted(this.mode);
        }
        this.mode = mode;

        rubberBandLabel.setEnabled(mode == BUILD);
        rubberBandCB.setEnabled(mode == BUILD);

        Messages.showInStatusBar(mode.getToolMessage());
        assert checkPathConsistency();
    }

    @VisibleForTesting
    public PenToolMode getMode() {
        return mode;
    }

    @Override
    public String getStatusBarMessage() {
        return mode.getToolMessage();
    }

    @VisibleForTesting
    public void convertToSelection() {
        Path oldPath = path;

        Shape shape = path.toImageSpaceShape();
        var comp = Views.getActiveComp();

        PixelitorEdit selectionEdit = comp.changeSelection(shape);
        if (selectionEdit == null) {
            Dialogs.showInfoDialog(comp.getDialogParent(), "No Selection",
                "No selection was created because the path is outside the canvas.");
            return;
        }

        PenToolMode oldMode = mode;
        removePath();
        History.add(new ConvertPathToSelectionEdit(
            comp, oldPath, selectionEdit, oldMode));
        assert checkPathConsistency();

        Tools.SELECTION.activate();
    }

    private static void exportSVG() {
        IO.saveSVG(path.toImageSpaceShape(), null, "path.svg");
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
            mode.modeEnded(oldComp);
        }

        super.viewActivated(oldView, newView);
        path = newView.getComp().getActivePath();

        if (restartMode) {
            mode.modeStarted(mode);
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
        var comp = Views.getActiveComp();
        setPathFromComp(comp);

        assert checkPathConsistency();
    }

    @Override
    protected void toolStarted() {
        super.toolStarted();

        View view = Views.getActive();
        if (view != null) {
            setPathFromComp(view.getComp());

            // the coordinates might have changed while using another tool,
            // but other tools don't update the path component coordinates
            coCoordsChanged(view);
        } else {
            assert path == null;
        }

        mode.modeStarted(null);

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
            setNullPath();
        } else {
            Path compPath = comp.getActivePath();
            if (compPath == null) {
                setNullPath();
            } else {
                path = compPath;
                PenToolMode preferredMode = compPath.getPreferredPenToolMode();
                if (preferredMode != null && preferredMode != mode) {
                    preferredMode.start();
                }
            }
            comp.repaint();
        }
        enableActions(path != null);
    }

    private void setNullPath() {
        path = null;
        if (mode.requiresExistingPath()) {
            startBuilding(false);
        }
    }

    public static Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        if (path == null) { // can happen when undoing
            removePath();
            return;
        }
        PenTool.path = path;

        assert checkPathConsistency();
    }

    public void removePath() {
        Views.setActivePath(null);
        setNullPath();
        enableActions(false);
    }

    @Override
    protected void toolEnded() {
        super.toolEnded();
        mode.modeEnded(Views.getActiveComp());

        assert checkPathConsistency();
    }

    public void enableActions(boolean b) {
        toSelectionAction.setEnabled(b);
        exportSVGAction.setEnabled(b);

        traceWithBrushAction.setEnabled(b);
        traceWithEraserAction.setEnabled(b);
        traceWithSmudgeAction.setEnabled(b);
//        traceAction.setEnabled(b);

        deletePath.setEnabled(b);
    }

    @VisibleForTesting
    public boolean arePathActionsEnabled() {
        return toSelectionAction.isEnabled();
    }

    public boolean showRubberBand() {
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
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        String modeString = preset.get("Mode");
        for (PenToolMode mode : MODES) {
            if (mode.toString().equals(modeString)) {
                startMode(mode, false);
                break;
            }
        }

        rubberBandCB.setSelected(preset.getBoolean(SHOW_RUBBER_BAND_TEXT));
    }

    @Override
    public String getStateInfo() {
        return mode + ", hasPath=" + hasPath();
    }

    @Override
    public DebugNode createDebugNode(String key) {
        var node = super.createDebugNode(key);

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
