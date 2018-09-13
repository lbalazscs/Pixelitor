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

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.utils.Dialogs;
import pixelitor.history.History;
import pixelitor.history.PixelitorEdit;
import pixelitor.tools.ClipStrategy;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.pen.history.ConvertPathToSelectionEdit;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;
import pixelitor.utils.Messages;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.tools.pen.PathBuilder.State.DRAGGING_THE_CONTROL_OF_LAST;
import static pixelitor.tools.pen.PathBuilder.State.MOVING_TO_NEXT_ANCHOR;
import static pixelitor.tools.pen.PenToolMode.BUILD;
import static pixelitor.tools.pen.PenToolMode.EDIT;

/**
 * The Pen Tool
 */
public class PenTool extends Tool {

    private final JComboBox<PenToolMode> modeChooser = new JComboBox<>(
            new PenToolMode[]{BUILD, EDIT});

    private final AbstractAction toSelectionAction;
    private final AbstractAction traceAction;

    private JCheckBox rubberBandCB;

    private PenToolMode mode = BUILD;

    public PenTool() {
        super("Pen", 'p', "pen_tool_icon.png",
                "", // getStatusBarMessage() is overridden
                Cursors.DEFAULT, false, true,
                ClipStrategy.INTERNAL_FRAME);
        toSelectionAction = new AbstractAction("Convert to Selection") {
            @Override
            public void actionPerformed(ActionEvent e) {
                convertToSelection(true);
            }
        };
        traceAction = new AbstractAction("Trace...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                TracePathPanel.showInDialog(mode.getPath());
            }
        };
        enableActionsBasedOnFinishedPath(false);
    }

    @Override
    public void initSettingsPanel() {
        modeChooser.addActionListener(e -> onModeChooserAction());
        settingsPanel.addWithLabel("Mode:", modeChooser);

        rubberBandCB = new JCheckBox("", true);
        rubberBandCB.addActionListener(e ->
                BUILD.setShowRubberBand(rubberBandCB.isSelected()));
        settingsPanel.addWithLabel("Show Rubber Band: ", rubberBandCB, "rubberBandCB");

        settingsPanel.addButton(toSelectionAction, "toSelectionButton",
                "Convert the active path to a selection");

//        settingsPanel.addButton(traceAction, "traceAction",
//                "Trace the path with a stroke or with a tool");
    }

    public void setModeChooserCombo(PenToolMode mode) {
        modeChooser.setSelectedItem(mode);
    }

    private void onModeChooserAction() {
        Path path = ImageComponents.getActivePathOrNull();
        if (getSelectedMode() == BUILD) {
            startBuilding(path, true);
        } else {
            startEditing(path, true);
        }
    }

    private void startBuilding(Path path,
                               boolean calledFromModeChooser) {
        if (!calledFromModeChooser) {
            setModeChooserCombo(EDIT);
        }
        changeMode(BUILD, path);
        enableActionsBasedOnFinishedPath(path != null);
        ImageComponents.repaintActive();
    }

    public void startEditing(Path path,
                             boolean calledFromModeChooser) {
        if (path == null) {
            EventQueue.invokeLater(() -> {
                if (!RandomGUITest.isRunning()) {
                    Dialogs.showInfoDialog("No Path",
                            "<html>There is no path to edit. " +
                                    "You can create a path<ul>" +
                                    "<li>in build mode</li>" +
                                    "<li>by converting a selection into a path</li>" +
                                    "</ul>");
                }
                setModeChooserCombo(BUILD);
            });
            return;
        }

        if (!calledFromModeChooser) {
            setModeChooserCombo(EDIT);
        }

        path.changeTypesForEditing();
        changeMode(EDIT, path);
        enableActionsBasedOnFinishedPath(true);
        ImageComponents.repaintActive();
    }

    // This method should not be called directly,
    // otherwise the mode and the the combo box get out of sync.
    private void changeMode(PenToolMode mode, Path path) {
        if (this.mode != mode) {
//            System.out.println("PenTool::changeMode: " + red(this.mode) + " => " + green(mode));
//            Thread.dumpStack();
            this.mode.modeEnded();
            mode.modeStarted(path);
        }
        this.mode = mode;
        Messages.showInStatusBar(mode.getToolMessage());
    }

    @Override
    public void resetStateToInitial() {
        setPath(ImageComponents.getActivePathOrNull(), "resetStateToInitial");
    }

    @Override
    public String getStatusBarMessage() {
        return getSelectedMode().getToolMessage();
    }

    private PenToolMode getSelectedMode() {
        return (PenToolMode) modeChooser.getSelectedItem();
    }

    private void convertToSelection(boolean addToHistory) {
        Path path = mode.getPath();
        Path oldPath = path;

        Shape shape = path.toImageSpaceShape();
        Composition comp = ImageComponents.getActiveCompOrNull();

        PixelitorEdit selectionEdit = comp.setSelectionFromShapeComplete(shape);
        if (selectionEdit == null) {
            Dialogs.showInfoDialog("No Selection",
                    "No selection was created because the path is outside the canvas.");
            return;
        }

        comp.setActivePath(null);
        PenToolMode oldMode = mode;
        if (mode == EDIT) {
            startBuilding(null, false);
        }
        setPath(null, "convertToSelection");
        Tools.SELECTION.activate();

        if (addToHistory) {
            History.addEdit(new ConvertPathToSelectionEdit(
                    comp, oldPath, selectionEdit, oldMode));
        }
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
    public void mouseMoved(MouseEvent e, ImageComponent ic) {
        if (mode.mouseMoved(e, ic)) {
            ic.repaint();
        }
    }

    @Override
    public void paintOverImage(Graphics2D g2, Canvas canvas, ImageComponent ic,
                               AffineTransform componentTransform,
                               AffineTransform imageTransform) {
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        mode.paint(g2);
    }

    @Override
    public void coCoordsChanged(ImageComponent ic) {
        Path path = mode.getPath();
        if (path != null) {
            path.coCoordsChanged(ic);
        }
    }

    public void setPath(Path path, String reason) {
        if (path == null) {  // undo
            if (mode == EDIT) {
                throw new IllegalStateException("The path cannot be null in Edit mode");
            }
        }
        mode.setPath(path, "PT.setPath(" + reason + ")");
    }

    @Override
    protected void toolStarted() {
        super.toolStarted();

        Path compPath = null;
        Composition comp = ImageComponents.getActiveCompOrNull();
        if (comp != null) {
            compPath = comp.getActivePath();
            if (compPath == null && mode == EDIT) {
                startBuilding(null, false);
            }
            mode.setPath(compPath, "toolStarted");
            comp.repaint();
        }
        mode.modeStarted(compPath);
    }

    @Override
    protected void toolEnded() {
        super.toolEnded();
        mode.modeEnded();
    }

    // this is called only by the undo mechanism
    public void setBuilderState(PathBuilder.State state, String reason) {
        if (mode != BUILD) {
            // if a build action is undone now, but the mode is
            // not build anymore, then change back
            startBuilding(mode.getPath(), false);
        }
        PathBuilder pb = (PathBuilder) mode;
        pb.setState(state, reason);
    }

    // this is called only by the undo mechanism
    public void setPathBuildingInProgressState(String reason) {
        boolean mouseDown = Tools.EventDispatcher.isMouseDown();
        reason += ("(mouseDown = " + mouseDown + ")");
        if (mouseDown) {
            setBuilderState(DRAGGING_THE_CONTROL_OF_LAST, reason);
        } else {
            setBuilderState(MOVING_TO_NEXT_ANCHOR, reason);
        }
    }

    public void enableActionsBasedOnFinishedPath(boolean b) {
        toSelectionAction.setEnabled(b);
        traceAction.setEnabled(b);
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = super.getDebugNode();

        node.add(mode.createDebugNode());

        return node;
    }
}
