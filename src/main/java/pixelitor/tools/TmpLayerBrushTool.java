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

import java.awt.Composite;
import java.awt.Cursor;

/**
 * A brush tool that draws each stroke into a temporary layer
 * and therefore has blending mode and opacity settings
 */
public abstract class TmpLayerBrushTool extends AbstractBrushTool {
    private BlendingModePanel blendingModePanel;

    protected TmpLayerBrushTool(char activationKeyChar, String name, String iconFileName, String toolMessage, Cursor cursor) {
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
}
