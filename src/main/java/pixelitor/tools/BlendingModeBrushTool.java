/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
 * A brush tool that can have blending mode controls. The blending mode
 * is disabled when editing layer masks.
 */
public abstract class BlendingModeBrushTool extends AbstractBrushTool {
    private BlendingModePanel blendingModePanel;

    protected BlendingModeBrushTool(char activationKeyChar, String name, String iconFileName, String toolMessage, Cursor cursor) {
        super(activationKeyChar, name, iconFileName, toolMessage, cursor);
        drawStrategy = DrawStrategy.TMP_LAYER;
    }

    public void setupMaskDrawing(boolean isMask) {
        if (isMask) {
            drawStrategy = DrawStrategy.DIRECT;
            blendingModePanel.setEnabled(false);
        } else {
            drawStrategy = DrawStrategy.TMP_LAYER;
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

        node.addFloatChild("Opacity", blendingModePanel.getOpacity());
        node.addQuotedStringChild("Blending Mode", blendingModePanel.getBlendingMode().toString());

        return node;
    }
}
