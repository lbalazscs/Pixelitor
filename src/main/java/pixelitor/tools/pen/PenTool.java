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
import pixelitor.utils.debug.PathNode;
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

/**
 * The Pen Tool
 */
public class PenTool extends Tool {

    private JComboBox<PenToolMode> modeChooser;
    private final AbstractAction toSelectionAction;

    private Path path;
    private PenToolMode mode = PenToolMode.BUILD;

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
        enableConvertToSelection(false);
    }

    @Override
    public void initSettingsPanel() {
        modeChooser = new JComboBox<>(new PenToolMode[]{
                PenToolMode.BUILD, PenToolMode.EDIT});
        modeChooser.addActionListener(e -> onModeChooserAction());
        settingsPanel.addWithLabel("Mode:", modeChooser);

        settingsPanel.addButton(toSelectionAction, "toSelectionButton",
                "Convert the active path to a selection");
    }

    private void onModeChooserAction() {
        if (getSelectedMode() == PenToolMode.BUILD) {
            startBuilding();
            Messages.showInStatusBar("<html>Pen Tool: " + PathBuilder.BUILDER_HELP_MESSAGE);
        } else {
            startEditing(true, true);
            Messages.showInStatusBar("<html>Pen Tool: " + PathEditor.EDIT_HELP_MESSAGE);
        }
    }

    private void startBuilding() {
        changeMode(PenToolMode.BUILD);
        Composition comp = ImageComponents.getActiveCompOrNull();
        Path p = null;
        if (comp != null) {
            p = comp.getActivePath();
        }
        setPath(p, "startBuilding");
        enableConvertToSelection(p != null);
        ImageComponents.repaintActive();
    }

    public void startEditing(boolean pathWasBuiltInteractively,
                             boolean calledFromModeChooser) {
        if (!hasPath()) {
            EventQueue.invokeLater(() -> {
                if (!RandomGUITest.isRunning()) {
                    Dialogs.showInfoDialog("No Path", "There is no path to edit.");
                }
                modeChooser.setSelectedItem(PenToolMode.BUILD);
            });
            return;
        }

        if (!calledFromModeChooser) {
            modeChooser.setSelectedItem(PenToolMode.EDIT);
        }

        path.changeTypesForEditing(pathWasBuiltInteractively);
        changeMode(PenToolMode.EDIT);
        mode.setPath(path, "PT.startEditing");
        enableConvertToSelection(true);
        ImageComponents.repaintActive();
    }

    @Override
    public void resetStateToInitial() {
        modeChooser.setSelectedItem(PenToolMode.BUILD);
    }

    @Override
    public String getStatusBarMessage() {
        return getSelectedMode().getToolMessage();
    }

    private PenToolMode getSelectedMode() {
        return (PenToolMode) modeChooser.getSelectedItem();
    }

    public void convertToSelection(boolean addToHistory) {
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
        resetStateToInitial();
        Tools.SELECTION.activate();

        if (addToHistory) {
            History.addEdit(new ConvertPathToSelectionEdit(comp, oldPath, selectionEdit));
        }
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        if (path == null) {
            assert mode instanceof PathBuilder;
            setPath(new Path(e.getComp()), "PT.mousePressed");
        }
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
        if (path != null) {
            path.coCoordsChanged(ic);
        }
    }

    @Override
    public void escPressed() {
// TODO what should Esc do? Certainly not resetting the state, because
// this would ruin the undo stack
//        resetStateToInitial();
    }

    public void setPath(Path path, String reason) {
        if (path == null) {  // undo
            modeChooser.setSelectedItem(PenToolMode.BUILD);
        }
        this.path = path;
        mode.setPath(this.path, "PT.setPath(" + reason + ")");
    }

    public boolean hasPath() {
        return path != null;
    }

    @Override
    protected void toolStarted() {
        super.toolStarted();

        Composition comp = ImageComponents.getActiveCompOrNull();
        if (comp != null) {
            Path path = comp.getActivePath();
            if (path == null && mode == PenToolMode.EDIT) {
                modeChooser.setSelectedItem(PenToolMode.BUILD);
            }
            setPath(path, "toolStarted");
            comp.repaint();
        }
        mode.modeStarted();
    }

    @Override
    protected void toolEnded() {
        super.toolEnded();
        mode.modeEnded();

        Composition comp = ImageComponents.getActiveCompOrNull();
        if (comp != null) {
            comp.setActivePath(path);
        }

//        changeMode(PenToolMode.BUILD); // to make sure that edit mode has never null path
//        setPath(null, "toolEnded");
        if (comp != null) {
            comp.repaint();
        }
    }

    // this is called only by the undo mechanism
    public void setBuilderState(PathBuilder.State state, String reason) {
        if (mode != PenToolMode.BUILD) {
            modeChooser.setSelectedItem(PenToolMode.BUILD);
        }
        PathBuilder pb = (PathBuilder) mode;
        pb.setState(state, reason);
    }

    public void enableConvertToSelection(boolean b) {
        toSelectionAction.setEnabled(b);
    }

    // This method should not be called directly, only through
    // the combo box event handler. Otherwise the mode and the
    // the combo box get out of sync.
    private void changeMode(PenToolMode mode) {
        if (this.mode != mode) {
//            System.out.println("PenTool::changeMode: " + red(this.mode) + " => " + green(mode));
            this.mode.modeEnded();
            mode.modeStarted();
        }
        this.mode = mode;
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = super.getDebugNode();

        node.add(mode.createDebugNode());
        if (path != null) {
            node.add(new PathNode(path));
        } else {
            node.addBoolean("Has Path", false);
        }

        return node;
    }
}
