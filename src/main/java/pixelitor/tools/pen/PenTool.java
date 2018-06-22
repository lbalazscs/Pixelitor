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
import pixelitor.history.History;
import pixelitor.history.NewSelectionEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.history.SelectionChangeEdit;
import pixelitor.selection.Selection;
import pixelitor.tools.ClipStrategy;
import pixelitor.tools.PMouseEvent;
import pixelitor.tools.Tool;
import pixelitor.utils.Cursors;

import javax.swing.*;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.tools.pen.PenTool.State.BUILDING;
import static pixelitor.tools.pen.PenTool.State.EDITING;

/**
 * The Pen Tool
 */
public class PenTool extends Tool {

    enum State {BUILDING, EDITING}

    private State state = BUILDING;

    private JComboBox<String> modeChooser;
    private final AbstractAction toSelectionAction;
    private Path path = new Path();
    private PenToolMode mode = new PathBuilder(path);

    public PenTool() {
        super('p', "Pen", "pen_tool_icon.png",
                "<b>click</b> and <b>drag</b> to create a Bezier curve. <b>Ctrl-click</b> to finish. Press <b>Esc</b> to start from scratch.", Cursors.DEFAULT, false, true, ClipStrategy.INTERNAL_FRAME);
        toSelectionAction = new AbstractAction("Convert to Selection") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Shape shape = path.toImageSpaceShape();
                Composition comp = ImageComponents.getActiveCompOrNull();
                if (comp != null) {
                    Selection selection = comp.getSelection();
                    PixelitorEdit edit;
                    if (selection != null) {
                        Shape backupSelectionShape = selection.getShape();
                        selection.setShape(shape);
                        boolean stillThereIsSelection = selection.clipToCompSize(comp);
                        if (!stillThereIsSelection) {
                            return;
                        }
                        edit = new SelectionChangeEdit("Selection Change", comp, backupSelectionShape);
                    } else {
                        selection = comp.createSelectionFromShape(shape);
                        boolean stillThereIsSelection = selection.clipToCompSize(comp);
                        if (!stillThereIsSelection) {
                            return;
                        }
                        edit = new NewSelectionEdit(comp, selection.getShape());
                    }
                    History.addEdit(edit);
                }
                resetStateToInitial();
            }
        };
        toSelectionAction.setEnabled(false);
    }

    @Override
    public void initSettingsPanel() {
        modeChooser = new JComboBox<>(new String[]{"Build", "Edit"});
        modeChooser.addActionListener(e -> {
            if (modeChooser.getSelectedItem().equals("Build")) {
                setState(BUILDING);
            } else {
                setState(EDITING);
            }
        });
        settingsPanel.addWithLabel("Mode:", modeChooser);

        settingsPanel.addButton(toSelectionAction,
                "Convert the active path to a selection");

        if (Build.CURRENT.isDevelopment()) {
            settingsPanel.addButton(new AbstractAction("dump") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    path.dump();
                }
            });
        }
    }

    private void setState(State state) {
        this.state = state;
        if (state == EDITING) {
            path.changeTypeFromSymmetricToSmooth();
            mode = new PathEditor(path);
            toSelectionAction.setEnabled(true);
            ImageComponents.repaintActive();
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
    public void paintOverImage(Graphics2D g2, Canvas canvas, ImageComponent ic, AffineTransform componentTransform, AffineTransform imageTransform) {
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        mode.paint(g2);
    }

    @Override
    public void icSizeChanged(ImageComponent ic) {
        path.icResized(ic);
    }

    @Override
    public void escPressed() {
        resetStateToInitial();
    }

    @Override
    public void resetStateToInitial() {
        modeChooser.setSelectedIndex(0);
        toSelectionAction.setEnabled(false);
        state = BUILDING;
        path = new Path();
        mode = new PathBuilder(path);
        ImageComponents.repaintActive();
    }
}
