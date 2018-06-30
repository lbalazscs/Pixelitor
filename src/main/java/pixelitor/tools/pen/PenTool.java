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
import pixelitor.tools.ClipStrategy;
import pixelitor.tools.PMouseEvent;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.utils.Cursors;

import javax.swing.*;
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
    private static final String MODE_BUILD = "Build";
    private static final String MODE_EDIT = "Edit";

    private JComboBox<String> modeChooser;
    private final AbstractAction toSelectionAction;
    private Path path = new Path();
    private PenToolMode mode = new PathBuilder(path);
    private boolean ignoreModeChooserAction = false;

    public PenTool() {
        super('p', "Pen", "pen_tool_icon.png",
                "<b>click</b> and <b>drag</b> to create a Bezier curve. <b>Ctrl-click</b> to finish. Press <b>Esc</b> to start from scratch.", Cursors.DEFAULT, false, true, ClipStrategy.INTERNAL_FRAME);
        toSelectionAction = new AbstractAction("Convert to Selection") {
            @Override
            public void actionPerformed(ActionEvent e) {
                convertToSelection();
            }
        };
        toSelectionAction.setEnabled(false);
    }

    private void convertToSelection() {
        Shape shape = path.toImageSpaceShape();
        Composition comp = ImageComponents.getActiveCompOrNull();
        if (comp != null) {
            comp.setSelectionFromShapeComplete(shape);
        }
        resetStateToInitial();
        Tools.SELECTION.getButton().doClick();
    }

    @Override
    public void initSettingsPanel() {
        modeChooser = new JComboBox<>(new String[]{MODE_BUILD, MODE_EDIT});
        modeChooser.addActionListener(e -> {
            if (ignoreModeChooserAction) {
                return;
            }
            if (modeChooser.getSelectedItem().equals("Build")) {
                resetStateToInitial();
            } else {
                startEditing(true);
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

    public void startEditing(boolean pathWasBuiltInteractively) {
        ignoreModeChooserAction = true;
        modeChooser.setSelectedItem(MODE_EDIT);
        ignoreModeChooserAction = false;

        path.changeTypesForEditing(pathWasBuiltInteractively);
        mode = new PathEditor(path);
        toSelectionAction.setEnabled(true);
        ImageComponents.repaintActive();
    }

    @Override
    public void resetStateToInitial() {
        ignoreModeChooserAction = true;
        modeChooser.setSelectedItem(MODE_BUILD);
        ignoreModeChooserAction = false;

        toSelectionAction.setEnabled(false);
        path = new Path();
        mode = new PathBuilder(path);
        ImageComponents.repaintActive();
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
        path.viewSizeChanged(ic);
    }

    @Override
    public void escPressed() {
        resetStateToInitial();
    }

    public void setPath(Path path) {
        this.path = path;
    }
}
