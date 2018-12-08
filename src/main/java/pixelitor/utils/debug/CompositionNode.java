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
import pixelitor.guides.Guides;
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
            "composite image", compositeImage);
        add(imageNode);

        Paths paths = comp.getPaths();
        if (paths == null) {
            addBoolean("has paths", false);
        } else {
            add(new PathsNode(paths));
        }

        Guides guides = comp.getGuides();
        if (guides == null) {
            addBoolean("has guides", false);
        } else {
            add(new GuidesNode(guides));
        }

        addInt("num layers", comp.getNumLayers());
        addQuotedString("name", comp.getName());

        String filePath = "";
        File file = comp.getFile();
        if (file != null) {
            filePath = file.getAbsolutePath();
        }

        addQuotedString("file", filePath);

        addBoolean("dirty", comp.isDirty());

        if (comp.hasBuiltSelection()) {
            add(comp.getBuiltSelection().createDebugNode("Built Selection"));
        } else {
            addBoolean("has built selection", false);
        }

        if (comp.hasSelection()) {
            add(comp.getSelection().createDebugNode("Selection"));
        } else {
            addBoolean("has selection", false);
        }

        int canvasWidth = comp.getCanvasImWidth();
        addInt("canvas im width", canvasWidth);
        int canvasHeight = comp.getCanvasImHeight();
        addInt("canvas im height", canvasHeight);
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