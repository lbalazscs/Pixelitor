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

package pixelitor.tools;

import pixelitor.gui.BlendingModePanel;
import pixelitor.utils.debug.DebugNode;

import java.awt.Composite;
import java.awt.Cursor;

/**
 * An {@link AbstractBrushTool} tool that can have blending mode controls.
 * The blending mode controls are disabled when editing layer masks.
 */
public abstract class BlendingModeBrushTool extends AbstractBrushTool {
    private BlendingModePanel blendingModePanel;

    protected BlendingModeBrushTool(String name, char activationKeyChar,
                                    String iconFileName, String toolMessage,
                                    Cursor cursor) {
        super(name, activationKeyChar, iconFileName, toolMessage, cursor);
        drawDestination = DrawDestination.TMP_LAYER;
    }

    public void setupMaskEditing(boolean isMask) {
        if (isMask) {
            drawDestination = DrawDestination.DIRECT;
            blendingModePanel.setEnabled(false);
        } else {
            drawDestination = DrawDestination.TMP_LAYER;
            blendingModePanel.setEnabled(true);
        }
    }

    @Override
    protected Composite getComposite() {
        return blendingModePanel.getComposite();
    }

    protected void addBlendingModePanel() {
        blendingModePanel = new BlendingModePanel(true);
        settingsPanel.add(blendingModePanel);
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = super.getDebugNode();

        node.addFloat("Opacity", blendingModePanel.getOpacity());
        node.addQuotedString("Blending Mode",
                blendingModePanel.getBlendingMode().toString());

        return node;
    }
}
