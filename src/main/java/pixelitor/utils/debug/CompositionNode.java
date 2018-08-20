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

package pixelitor.utils.debug;

import pixelitor.Composition;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.TextLayer;
import pixelitor.tools.pen.Paths;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * A debugging node for an ImageComponent
 */
public class CompositionNode extends DebugNode {
    public CompositionNode(Composition comp) {
        super("Composition", comp);

        comp.forEachLayer(this::addLayerNode);

        BufferedImage compositeImage = comp.getCompositeImage();
        BufferedImageNode imageNode = new BufferedImageNode(
                "Composite Image", compositeImage);
        add(imageNode);

        Paths paths = comp.getPaths();
        if (paths == null) {
            addBoolean("Paths", false);
        } else {
            add(new PathsNode(paths));
        }

        addInt("numLayers", comp.getNumLayers());
        addQuotedString("name", comp.getName());

        String filePath = "";
        File file = comp.getFile();
        if (file != null) {
            filePath = file.getAbsolutePath();
        }

        addQuotedString("file", filePath);

        boolean dirty = comp.isDirty();
        addBoolean("dirty", dirty);

        boolean hasSelection = comp.hasSelection();
        addBoolean("hasSelection", hasSelection);

        if (hasSelection) {
            SelectionNode selectionNode = new SelectionNode(comp.getSelection());
            add(selectionNode);
        }

        int canvasWidth = comp.getCanvasImWidth();
        addInt("canvasWidth", canvasWidth);
        int canvasHeight = comp.getCanvasImHeight();
        addInt("canvasHeight", canvasHeight);
    }

    private void addLayerNode(Layer layer) {
        if (layer instanceof ImageLayer) {
            addImageLayerNode(layer);
        } else if (layer instanceof TextLayer) {
            addTextLayerNode(layer);
        } else {
            addQuotedString("Layer of class",
                    layer.getClass().getName());
        }
    }

    private void addImageLayerNode(Layer layer) {
        ImageLayer imageLayer = (ImageLayer) layer;
        ImageLayerNode node;
        if (imageLayer.isActive()) {
            node = new ImageLayerNode("ACTIVE Layer - " + layer.getName(), imageLayer);
        } else {
            node = new ImageLayerNode("Layer - " + layer.getName(), imageLayer);
        }
        add(node);
    }

    private void addTextLayerNode(Layer layer) {
        TextLayer textLayer = (TextLayer) layer;
        TextLayerNode node = new TextLayerNode("Text Layer - " + layer.getName(), textLayer);
        add(node);
    }
}