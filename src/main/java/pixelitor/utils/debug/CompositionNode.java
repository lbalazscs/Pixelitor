/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import static pixelitor.utils.debug.DebugNodes.createBufferedImageNode;

/**
 * A debugging node for a Composition
 */
public class CompositionNode extends DebugNode {
    public CompositionNode(Composition comp) {
        super("composition", comp);

        comp.forEachLayer(this::addLayerNode);

        BufferedImage compositeImage = comp.getCompositeImage();
        add(createBufferedImageNode("composite image", compositeImage));

        Paths paths = comp.getPaths();
        if (paths == null) {
            addBoolean("has paths", false);
        } else {
            add(DebugNodes.createPathsNode(paths));
        }

        Guides guides = comp.getGuides();
        if (guides == null) {
            addBoolean("has guides", false);
        } else {
            add(DebugNodes.createGuidesNode("guides", guides));
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
            add(comp.getBuiltSelection().createDebugNode("built selection"));
        } else {
            addBoolean("has built selection", false);
        }

        if (comp.hasSelection()) {
            add(comp.getSelection().createDebugNode("selection"));
        } else {
            addBoolean("has selection", false);
        }

        int canvasWidth = comp.getCanvasWidth();
        addInt("canvas im width", canvasWidth);
        int canvasHeight = comp.getCanvasHeight();
        addInt("canvas im height", canvasHeight);
    }

    private void addLayerNode(Layer layer) {
        if (layer instanceof ImageLayer) {
            addImageLayerNode((ImageLayer) layer);
        } else if (layer instanceof TextLayer) {
            addTextLayerNode((TextLayer) layer);
        } else {
            addQuotedString("layer class", layer.getClass().getName());
        }
    }

    private void addImageLayerNode(ImageLayer layer) {
        String name = createNameForLayer(layer);

        add(new ImageLayerNode(name, layer));
    }

    private void addTextLayerNode(TextLayer layer) {
        String name = createNameForLayer(layer);

        add(new TextLayerNode(name, layer));
    }

    private static String createNameForLayer(Layer layer) {
        String name = layer.getClass().getSimpleName() + " - " + layer.getName();
        if (layer.isActive()) {
            name = "active " + name;
        }
        return name;
    }
}