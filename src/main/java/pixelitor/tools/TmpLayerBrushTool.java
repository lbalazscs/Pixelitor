/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

import pixelitor.Composition;
import pixelitor.layers.ImageLayer;
import pixelitor.utils.BlendingModePanel;

import java.awt.image.BufferedImage;

/**
 * A brush tool that draws each stroke into a temporary layer
 * and therefore has blending mode and opacity settings
 */
public abstract class TmpLayerBrushTool extends AbstractBrushTool {
    protected BlendingModePanel blendingModePanel;

    public TmpLayerBrushTool(char activationKeyChar, String name, String iconFileName, String toolMessage) {
        super(activationKeyChar, name, iconFileName, toolMessage);
    }

    @Override
    protected void createGraphics(Composition comp, ImageLayer layer) {
        graphics = layer.createTmpDrawingLayer(blendingModePanel.getComposite(), respectSelection).getGraphics();
        brush.setTarget(comp, graphics);
    }

    protected void addBlendingModePanel() {
        blendingModePanel = new BlendingModePanel(true);
        toolSettingsPanel.add(blendingModePanel);
    }

    @Override
    void mergeTmpLayer(Composition comp) {
        if (graphics != null) {
            ImageLayer imageLayer = comp.getActiveImageLayer();
            imageLayer.mergeTmpDrawingImageDown();
        }
    }


    @Override
    BufferedImage getOriginalImage(Composition comp) {
        // it can simply return the layer image because
        // the drawing was on the temporary layer
        return comp.getActiveImageLayer().getImage();
    }
}
