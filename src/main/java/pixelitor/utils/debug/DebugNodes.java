/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.ImageFrame;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.View;
import pixelitor.guides.Guides;
import pixelitor.tools.pen.Path;
import pixelitor.tools.pen.Paths;
import pixelitor.utils.Utils;

import java.awt.GraphicsEnvironment;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.util.List;

/**
 * Static factory methods for creating {@link DebugNode}s
 */
public class DebugNodes {
    private DebugNodes() {
        // shouldn't be instantiated
    }

    public static DebugNode createSystemNode() {
        var device = GraphicsEnvironment
            .getLocalGraphicsEnvironment()
            .getDefaultScreenDevice();
        var node = new DebugNode("system", device);

        node.addString("Java version", System.getProperty("java.version"));
        node.addString("Java vendor", System.getProperty("java.vendor"));
        node.addQuotedString("OS name", System.getProperty("os.name"));

        var displayMode = device.getDisplayMode();
        node.addInt("display width", displayMode.getWidth());
        node.addInt("display height", displayMode.getHeight());
        node.addInt("display bit depth", displayMode.getBitDepth());

        var pw = PixelitorWindow.get();
        node.addInt("app window width", pw.getWidth());
        node.addInt("app window height", pw.getHeight());

        node.addQuotedString("max memory", Utils.getMaxHeapMb() + " Mb");
        node.addQuotedString("used memory", Utils.getUsedMemoryMb() + " Mb");

        node.add(createColorModelNode("default color model",
            device.getDefaultConfiguration().getColorModel()));

        return node;
    }

    public static DebugNode createViewNode(String name, View view) {
        var node = new DebugNode(name, view);

        node.add(view.getComp().createDebugNode("composition"));
        node.addNullableChild("canvas", view.getCanvas());
        node.addAsString("zoom level", view.getZoomLevel());

        node.addInt("view width", view.getWidth());
        node.addInt("view height", view.getHeight());

        var viewContainer = view.getViewContainer();
        if (viewContainer instanceof ImageFrame frame) {
            node.addInt("frame width", frame.getWidth());
            node.addInt("frame height", frame.getHeight());
        }

        node.addQuotedString("mask view mode", view.getMaskViewMode().toString());

        return node;
    }

    public static DebugNode createBufferedImageNode(String name, BufferedImage image) {
        var node = new DebugNode(name, image);

        node.add(createColorModelNode("color model", image.getColorModel()));
        node.add(createRasterNode(image.getRaster()));
        node.addString("type", Debug.bufferedImageTypeAsString(image.getType()));
        node.addInt("width", image.getWidth());
        node.addInt("height", image.getHeight());
        node.addBoolean("alpha premultiplied", image.isAlphaPremultiplied());

        return node;
    }

    private static DebugNode createRasterNode(WritableRaster raster) {
        var node = new DebugNode("writable raster", raster);

        node.addClass();
        node.addInt("sample model tx", raster.getSampleModelTranslateX());
        node.addInt("sample model ty", raster.getSampleModelTranslateY());
        node.add(createSampleModelNode(raster.getSampleModel()));
        node.add(createDataBufferNode(raster.getDataBuffer()));

        return node;
    }

    private static DebugNode createSampleModelNode(SampleModel sampleModel) {
        var node = new DebugNode("sample model", sampleModel);

        node.addClass();
        node.addInt("width", sampleModel.getWidth());
        node.addInt("height", sampleModel.getHeight());
        node.addString("data type",
            Debug.dataBufferTypeAsString(sampleModel.getDataType()));
        node.addInt("num bands", sampleModel.getNumBands());
        node.addString("transfer type",
            Debug.dataBufferTypeAsString(sampleModel.getTransferType()));
        node.addInt("num data elements", sampleModel.getNumDataElements());

        return node;
    }

    private static DebugNode createDataBufferNode(DataBuffer dataBuffer) {
        var node = new DebugNode("data buffer", dataBuffer);

        node.addClass();
        node.addInt("num banks", dataBuffer.getNumBanks());
        node.addString("type", Debug.dataBufferTypeAsString(dataBuffer.getDataType()));
        node.addInt("size", dataBuffer.getSize());

        return node;
    }

    public static DebugNode createColorModelNode(String name, ColorModel colorModel) {
        var node = new DebugNode(name, colorModel);

        node.addClass();
        node.add(createColorSpaceNode(colorModel.getColorSpace()));
        node.addInt("num color components", colorModel.getNumColorComponents());
        node.addInt("num components", colorModel.getNumComponents());
        node.addBoolean("has alpha", colorModel.hasAlpha());
        node.addInt("pixel size", colorModel.getPixelSize());
        node.addString("transfer type",
            Debug.dataBufferTypeAsString(colorModel.getTransferType()));
        node.addString("transparency",
            Debug.transparencyAsString(colorModel.getTransparency()));
        node.addBoolean("is RGB", Debug.isRgbColorModel(colorModel));
        node.addBoolean("is BGR", Debug.isBgrColorModel(colorModel));

        return node;
    }

    private static DebugNode createColorSpaceNode(ColorSpace colorSpace) {
        var node = new DebugNode("color space", colorSpace);

        node.addClass();
        node.addInt("num components", colorSpace.getNumComponents());
        node.addString("type", Debug.colorSpaceTypeAsString(colorSpace.getType()));
        node.addBoolean("sRGB", colorSpace.isCS_sRGB());

        return node;
    }

    public static DebugNode createGuidesNode(String name, Guides guides) {
        var node = new DebugNode(name, guides);

        List<Double> horizontals = guides.getHorizontals();
        for (Double h : horizontals) {
            node.addDouble("horizontal", h);
        }
        List<Double> verticals = guides.getVerticals();
        for (Double v : verticals) {
            node.addDouble("vertical", v);
        }

        return node;
    }

    public static DebugNode createPathsNode(Paths paths) {
        var node = new DebugNode("paths", paths);

        Path activePath = paths.getActivePath();
        if (activePath != null) {
            node.add(createPathNode(activePath));
        } else {
            node.addBoolean("has active path", false);
        }

        return node;
    }

    public static DebugNode createPathNode(Path path) {
        var node = new DebugNode("path " + path.getId(), path);

        int numSubpaths = path.getNumSubpaths();
        node.addInt("number of subpaths", numSubpaths);
        node.addAsString("build state", path.getBuildState());

        for (int i = 0; i < numSubpaths; i++) {
            var subPath = path.getSubPath(i);
            node.add(subPath.createDebugNode());
        }

        return node;
    }
}
