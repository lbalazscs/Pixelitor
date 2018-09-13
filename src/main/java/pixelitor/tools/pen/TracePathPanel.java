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

import pixelitor.filters.gui.StrokeParam;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.Drawable;
import pixelitor.tools.Tools;

import javax.swing.*;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.event.ActionListener;

import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.GridBagConstraints.WEST;

public class TracePathPanel extends JPanel {
    private final Path path;
    private final Drawable dr;
    private final StrokeParam strokeParam = new StrokeParam("");
    private JDialog owner;
    private final JRadioButton strokeRB;
    private final JComboBox paintToolCB;
    private final JButton showStrokeSettingsButton;

    private TracePathPanel(Path path, Drawable dr) {
        super(new GridBagLayout());
        this.path = path;
        this.dr = dr;

        strokeParam.setAdjustmentListener(this::trace);

        strokeRB = new JRadioButton("Trace with a Stroke:");
        JRadioButton toolRB = new JRadioButton("Trace with a Paint Tool:");
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(strokeRB);
        buttonGroup.add(toolRB);
        strokeRB.setSelected(true);

        paintToolCB = new JComboBox(new String[]{
                "Brush",
                "Eraser",
                "Smudge",
                "Clone Stamp",
        });
        paintToolCB.addActionListener(e -> trace());

        showStrokeSettingsButton = new JButton("Stroke Settings...");
        showStrokeSettingsButton.addActionListener(e -> showStrokeSettingsDialog());

        GridBagConstraints gbc = new GridBagConstraints(
                0, 0, 1, 1,
                1.0, 1.0, WEST, HORIZONTAL,
                new Insets(2, 2, 2, 2), 0, 0);
        add(strokeRB, gbc);
        gbc.gridx = 1;
        add(showStrokeSettingsButton, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(toolRB, gbc);
        gbc.gridx = 1;
        add(paintToolCB, gbc);

        ActionListener radioListener = e -> {
            setEnabledState();
            trace();
        };
        strokeRB.addActionListener(radioListener);
        toolRB.addActionListener(radioListener);

        trace();
    }

    private void setEnabledState() {
        boolean strokeChosen = strokeRB.isSelected();
        showStrokeSettingsButton.setEnabled(strokeChosen);
        paintToolCB.setEnabled(!strokeChosen);
    }

    private void setOwner(JDialog owner) {
        this.owner = owner;
    }

    private void showStrokeSettingsDialog() {
        assert owner != null;
        JDialog d = StrokeParam.createSettingsDialog(strokeParam, owner);
        GUIUtils.showDialog(d);
    }

    private void trace() {
        Shape shape = path.toImageSpaceShape();
        if (strokeRB.isSelected()) {
            System.out.println("TracePathPanel::trace: tracing with stroke, width = "
                    + strokeParam.getStrokeWidth());

        } else {
            System.out.println("TracePathPanel::trace: tracing with the tool "
                    + paintToolCB.getSelectedItem());
            Tools.BRUSH.trace(dr, shape);
        }
    }

    private void dialogAccepted() {
        // create an image edit from the backup image and the current one
    }

    private void dialogCanceled() {
        // restore the backup image
    }

    public static void showInDialog(Path path) {
        TracePathPanel p = new TracePathPanel(path, ImageComponents.getActiveDrawableOrNull());
        JDialog d = new DialogBuilder()
                .title("Trace Path")
                .content(p)
                .okText("Close")
                .okAction(p::dialogAccepted)
                .cancelAction(p::dialogCanceled)
                .build();
        p.setOwner(d);
        GUIUtils.showDialog(d);
    }
}
