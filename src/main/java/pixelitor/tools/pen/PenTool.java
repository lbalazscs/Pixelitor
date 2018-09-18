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

import pixelitor.Build;
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
import pixelitor.utils.VisibleForTesting;
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
import static pixelitor.tools.pen.PenToolMode.BUILD;
import static pixelitor.tools.pen.PenToolMode.EDIT;

/**
 * The Pen Tool
 */
public class PenTool extends Tool {
    private final ComboBoxModel<PenToolMode> modeModel =
            new DefaultComboBoxModel<>(new PenToolMode[]{BUILD, EDIT});

    private final AbstractAction toSelectionAction;
    private final AbstractAction traceAction;

    private final JLabel rubberBandLabel = new JLabel("Show Rubber Band:");
    private final JCheckBox rubberBandCB = new JCheckBox("", true);

    private PenToolMode mode = BUILD;
    private boolean ignoreModeChooser = false;

    public static Path path;

    private boolean rubberBand = true;

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
                TracePathPanel.showInDialog(path);
            }
        };
        enableActionsBasedOnFinishedPath(false);
    }

    @Override
    public void initSettingsPanel() {
        JComboBox modeChooser = new JComboBox<>(modeModel);

        modeChooser.addActionListener(e -> onModeChooserAction());
        settingsPanel.addWithLabel("Mode:", modeChooser);

        settingsPanel.add(rubberBandLabel);
        settingsPanel.add(rubberBandCB);
        rubberBandCB.addActionListener(e ->
                rubberBand = rubberBandCB.isSelected());
        rubberBandCB.setName("rubberBandCB");

        settingsPanel.addButton(toSelectionAction, "toSelectionButton",
                "Convert the active path to a selection");

//        settingsPanel.addButton(traceAction, "traceAction",
//                "Trace the path with a stroke or with a tool");

        if (Build.CURRENT.isDevelopment()) {
            JButton dumpButton = new JButton("dump");
            dumpButton.addActionListener(e -> path.dump());
            settingsPanel.add(dumpButton);
        }
    }

    public void setModeChooserCombo(PenToolMode mode) {
        modeModel.setSelectedItem(mode);
    }

    private void onModeChooserAction() {
        if (ignoreModeChooser) {
            return;
        }
        Path activePath = ImageComponents.getActivePathOrNull();
        assert activePath == PenTool.path : "active path = " + activePath
                + ", PenTool.path = " + PenTool.path;

        if (modeModel.getSelectedItem() == BUILD) {
            startBuilding(true);
        } else {
            startEditing(true);
        }
    }

    public void startBuilding(boolean calledFromModeChooser) {
        if (!calledFromModeChooser) {
            ignoreModeChooser = true;
            setModeChooserCombo(BUILD);
            ignoreModeChooser = false;
        }
        changeMode(BUILD, path);
        enableActionsBasedOnFinishedPath(path != null);
        ImageComponents.repaintActive();

        assert checkPathConsistency();
    }

    public void startEditing(boolean calledFromModeChooser) {
        if (path == null) {
            if (Build.isTesting()) {
                throw new IllegalStateException("start editing with null path");
            }
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
            ignoreModeChooser = true;
            setModeChooserCombo(EDIT);
            ignoreModeChooser = false;
        }

        changeMode(EDIT, path);
        enableActionsBasedOnFinishedPath(true);
        ImageComponents.repaintActive();

        assert checkPathConsistency();
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

        PenToolMode oldMode = mode;

        removePath();
        Tools.SELECTION.activate();

        if (addToHistory) {
            History.addEdit(new ConvertPathToSelectionEdit(
                    comp, oldPath, selectionEdit, oldMode));
        }
        assert checkPathConsistency();
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
        if (path != null) {
            path.coCoordsChanged(ic);
        }
    }

    @Override
    public void activeImageHasChanged(ImageComponent oldIC, ImageComponent newIC) {
        if (oldIC != null) { // is null if the first image is opened with active pen tool
            Composition oldComp = oldIC.getComp();
            Path oldPath = oldComp.getActivePath();
            if (oldPath != null) {
                oldPath.setPreferredPenToolMode(mode);
            }
        }

        super.activeImageHasChanged(oldIC, newIC);

        assert ImageComponents.getActiveIC() == newIC;
        assert checkPathConsistency();
    }

    @Override
    public void compReplaced(Composition oldComp, Composition newComp) {
        setPathFromComp();
    }

    @Override
    public void resetStateToInitial() {
        setPathFromComp();

        assert checkPathConsistency();
    }

    @Override
    protected void toolStarted() {
        super.toolStarted();

        Path compPath = setPathFromComp();

        mode.modeStarted(compPath);

        assert checkPathConsistency();
    }

    private static boolean checkPathConsistency() {
        assert path == ImageComponents.getActivePathOrNull()
                : "tool path = " + path + ", active path = " + ImageComponents.getActivePathOrNull();
        Composition activeComp = ImageComponents.getActiveCompOrNull();
        if (activeComp == null) {
            return true;
        }
        if (path != null && path.getComp() != activeComp) {
            throw new IllegalStateException("foreign path " + path
                    + ", path comp = " + path.getComp().toPathDebugString()
                    + ", active comp = " + activeComp.toPathDebugString());
        }
        return true;
    }

    private Path setPathFromComp() {
        Path compPath = null;
        Composition comp = ImageComponents.getActiveCompOrNull();
        if (comp == null) {
            path = null;
        } else {
            compPath = comp.getActivePath();
            if (compPath == null) {
                path = null;
                if (mode == EDIT) {
                    startBuilding(false);
                }
            } else {
                path = compPath;
                PenToolMode preferredMode = compPath.getPreferredPenToolMode();
                if (preferredMode != null) {
                    preferredMode.start();
                }
            }
            comp.repaint();
        }
        return compPath;
    }

    public static Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        if (path == null) {
            removePath();
            return;
        }
        PenTool.path = path;

        assert checkPathConsistency();
    }

    public void removePath() {
        PenTool.path = null;
        ImageComponents.setActivePath(null);
        if (mode == EDIT) {
            startBuilding(false);
        }
    }

    @Override
    protected void toolEnded() {
        super.toolEnded();
        mode.modeEnded();

        assert checkPathConsistency();
    }

    public void enableActionsBasedOnFinishedPath(boolean b) {
        toSelectionAction.setEnabled(b);
        traceAction.setEnabled(b);
    }

    public boolean showRubberBand() {
        return rubberBand && mode == BUILD;
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = super.getDebugNode();

        node.add(mode.createDebugNode());

        return node;
    }
}
