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
import pixelitor.ImageComponents;
import pixelitor.ImageDisplay;
import pixelitor.layers.ImageLayer;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Utils;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * The erase tool.
 */
public class EraseTool extends AbstractBrushTool {
    private BufferedImage copyBeforeStart;

    public EraseTool() {
        super('e', "Erase", "erase_tool_icon.gif", "click and drag to erase pixels");
//        useFillOval = true;
    }

    @Override
    public void toolMouseReleased(MouseEvent e, ImageDisplay ic) {
        super.toolMouseReleased(e, ic);
        copyBeforeStart.flush();
        copyBeforeStart = null;
    }

    @Override
    void initDrawingGraphics(ImageLayer layer) {
        // uses the graphics of the buffered image contained in the layer
        BufferedImage drawImage = layer.createCompositionSizedSubImage();
        drawingGraphics = drawImage.createGraphics();
        if (respectSelection) {
            layer.getComposition().setSelectionClipping(drawingGraphics, null);
        }
        brushes.setDrawingGraphics(drawingGraphics);
    }

    @Override
    public void setupGraphics(Graphics2D g, Paint p) {
        // the color does not matter as long as AlphaComposite.CLEAR is used
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OUT, 1.0f));

        BufferedImage image = ImageComponents.getActiveImageLayer().get().getImage();

        Utils.checkRasterMinimum(image); // TODO remove if raster bug is corrected

        copyBeforeStart = ImageUtils.copyImage(image);
    }

    @Override
    BufferedImage getFullUntouchedImage(Composition comp) {
        if (copyBeforeStart == null) {
            throw new IllegalStateException("EraseTool: copyBeforeStart == null");
        }

        return copyBeforeStart;
    }

    @Override
    void mergeTmpLayer(Composition comp) {
        // do nothing - this tool draws directly into the image
    }
}