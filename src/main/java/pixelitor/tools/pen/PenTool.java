/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.AppContext;
import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.gui.View;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.PAction;
import pixelitor.history.History;
import pixelitor.history.PixelitorEdit;
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
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.tools.pen.PenToolMode.*;

/**
 * The pen tool.
 */
public class PenTool extends Tool {
    private final ComboBoxModel<PenToolMode> modeModel =
        new DefaultComboBoxModel<>(new PenToolMode[]{BUILD, EDIT, TRANSFORM});

    private final Action toSelectionAction;
    private final Action dumpPathAction;

    private final JLabel rubberBandLabel = new JLabel("Show Rubber Band:");
    private final JCheckBox rubberBandCB = new JCheckBox("", true);

    private PenToolMode mode = BUILD;
    private boolean ignoreModeChooser = false;

    public static Path path;

    private boolean rubberBand = true;

    private static final Action traceWithBrushAction = new TraceAction(
        "Stroke with Current Brush", Tools.BRUSH);
    private static final Action traceWithEraserAction = new TraceAction(
        "Stroke with Current Eraser", Tools.ERASER);
    private static final Action traceWithSmudgeAction = new TraceAction(
        "Stroke with Current Smudge", Tools.SMUDGE);

    private static final Action deletePath = new PAction("Delete Path") {
        @Override
        public void onClick() {
            path.delete();
        }
    };

    public PenTool() {
        super("Pen", 'P', "pen_tool.png",
            "", // getStatusBarMessage() is overridden
            Cursors.DEFAULT);

        toSelectionAction = new PAction("Convert to Selection") {
            @Override
            public void onClick() {
                convertToSelection();
            }
        };
        dumpPathAction = new PAction("Dump") {
            @Override
            public void onClick() {
                assert hasPath();
                path.dump();
            }
        };
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
            "Convert the active path to a selection");

//        settingsPanel.addButton(traceAction, "traceAction",
//                "Trace the path with a stroke or with a tool");

        settingsPanel.addButton(traceWithBrushAction, "traceWithBrush",
            "Stroke the path using the current settings of the Brush Tool");
        settingsPanel.addButton(traceWithEraserAction, "traceWithEraser",
            "Stroke the path using the current settings of the Eraser Tool");
        settingsPanel.addButton(traceWithSmudgeAction, "traceWithSmudge",
            "Stroke the path using the current settings of the Smudge Tool");
        settingsPanel.addButton(deletePath, "deletePath",
            "Delete the path");

        if (AppContext.isDevelopment()) {
            settingsPanel.addButton(dumpPathAction, "dumpPathAction", "");
        }
    }

    public void setModeChooserCombo(PenToolMode mode) {
        modeModel.setSelectedItem(mode);
    }

    private void onModeChooserAction() {
        if (ignoreModeChooser) {
            return;
        }

        assert OpenImages.activePathIs(path) :
            "path = " + path + ", active path = " + OpenImages.getActivePath();

        PenToolMode selectedMode = (PenToolMode) modeModel.getSelectedItem();
        if (selectedMode == BUILD) {
            startBuilding(true);
        } else {
            startRestrictedMode(selectedMode, true);
        }
    }

    public void startBuilding(boolean calledFromModeChooser) {
        if (!calledFromModeChooser) {
            ignoreModeChooser = true;
            setModeChooserCombo(BUILD);
            ignoreModeChooser = false;
        }
        changeMode(BUILD, path);
        enableActions(hasPath());
        OpenImages.repaintActive();

        assert checkPathConsistency();
    }

    // starts either the editing or the transforming mode
    public void startRestrictedMode(PenToolMode mode, boolean calledFromModeChooser) {
        if (path == null) {
            if (AppContext.isUnitTesting()) {
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

        changeMode(mode, path);
        enableActions(true);
        OpenImages.repaintActive();

        assert checkPathConsistency();
    }

    // This method should not be called directly,
    // otherwise the mode and the the combo box get out of sync.
    private void changeMode(PenToolMode mode, Path path) {
        if (this.mode != mode) {
            this.mode.modeEnded();
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
        var comp = OpenImages.getActiveComp();

        PixelitorEdit selectionEdit = comp.changeSelection(shape);
        if (selectionEdit == null) {
            Dialogs.showInfoDialog("No Selection",
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
        View view = OpenImages.getActiveView();
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
    public void imCoordsChanged(AffineTransform at, Composition comp) {
        if (hasPath()) {
            mode.imCoordsChanged(at, comp);
        }
    }

    @Override
    public void viewActivated(View oldCV, View newCV) {
        if (oldCV != null) { // is null if the first image is opened with active pen tool
            Composition oldComp = oldCV.getComp();
            Path oldPath = oldComp.getActivePath();
            if (oldPath != null) {
                oldPath.setPreferredPenToolMode(mode);
            }
        }

        super.viewActivated(oldCV, newCV);
        path = newCV.getComp().getActivePath();

        assert OpenImages.getActiveView() == newCV;
        assert checkPathConsistency();
    }

    @Override
    public void compReplaced(Composition newComp, boolean reloaded) {
        if (newComp.isActive()) {
            // reloading is asynchronous, the view might not be active anymore
            setPathFromComp(newComp);
            mode.compReplaced(newComp);
        }
    }

    @Override
    public void resetInitialState() {
        var comp = OpenImages.getActiveComp();
        setPathFromComp(comp);

        assert checkPathConsistency();
    }

    @Override
    protected void toolStarted() {
        super.toolStarted();

        View view = OpenImages.getActiveView();
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
        assert OpenImages.activePathIs(path) :
            "path = " + path + ", active path = " + OpenImages.getActivePath();

        Composition activeComp = OpenImages.getActiveComp();
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
        OpenImages.setActivePath(null);
        setNullPath();
        enableActions(false);
    }

    @Override
    protected void toolEnded() {
        super.toolEnded();
        mode.modeEnded();

        assert checkPathConsistency();
    }

    // TODO enable them while building, as soon as the path != null
    public void enableActions(boolean b) {
        toSelectionAction.setEnabled(b);

        traceWithBrushAction.setEnabled(b);
        traceWithEraserAction.setEnabled(b);
        traceWithSmudgeAction.setEnabled(b);
//        traceAction.setEnabled(b);

        deletePath.setEnabled(b);
        dumpPathAction.setEnabled(b);
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
    public String getStateInfo() {
        return mode + ", hasPath=" + hasPath();
    }

    @Override
    public DebugNode createDebugNode() {
        var node = super.createDebugNode();

        node.add(mode.createDebugNode());

        return node;
    }
}
