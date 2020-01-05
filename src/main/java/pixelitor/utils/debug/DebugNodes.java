/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Canvas;
import pixelitor.gui.ImageFrame;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.View;
import pixelitor.guides.Guides;
import pixelitor.tools.pen.Path;
import pixelitor.tools.pen.Paths;
import pixelitor.tools.pen.SubPath;
import pixelitor.utils.Utils;

import java.awt.GraphicsEnvironment;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
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
        var node = new DebugNode("System", device);

        node.addString("Java version", System.getProperty("java.version"));
        node.addString("Java vendor", System.getProperty("java.vendor"));
        node.addString("OS name", System.getProperty("os.name"));

        var displayMode = device.getDisplayMode();

        int width = displayMode.getWidth();
        int height = displayMode.getHeight();
        int bitDepth = displayMode.getBitDepth();
        node.addInt("display width", width);
        node.addInt("display height", height);
        node.addInt("display bit depth", bitDepth);

        var pw = PixelitorWindow.getInstance();
        node.addInt("app window width", pw.getWidth());
        node.addInt("app window height", pw.getHeight());

        node.addString("max memory", Utils.getMaxHeapInMegabytes() + " Mb");
        node.addString("used memory", Utils.getUsedMemoryInMegabytes() + " Mb");

        var configuration = device.getDefaultConfiguration();
        var defaultColorModel = configuration.getColorModel();

        var colorModelNode = createColorModelNode("default color model", defaultColorModel);
        node.add(colorModelNode);

        return node;
    }

    public static DebugNode createViewNode(String name, View view) {
        var node = new DebugNode(name, view);

        var comp = view.getComp();
        node.add(new CompositionNode(comp));

        node.addQuotedString("name", comp.getName());

        node.addQuotedString("mask view mode", view.getMaskViewMode().toString());

        int width = view.getWidth();
        node.addInt("view width", width);
        int height = view.getHeight();
        node.addInt("view height", height);

        var viewContainer = view.getViewContainer();
        if (viewContainer instanceof ImageFrame) {
            var frame = (ImageFrame) viewContainer;
            int frameWidth = frame.getWidth();
            node.addInt("frame width", frameWidth);
            int frameHeight = frame.getHeight();
            node.addInt("frame height", frameHeight);
        }

        node.addString("zoom level", view.getZoomLevel().toString());
        Canvas canvas = view.getCanvas();
        int zoomedCanvasWidth = canvas.getCoWidth();
        node.addInt("zoomed canvas width", zoomedCanvasWidth);
        int zoomedCanvasHeight = canvas.getCoHeight();
        node.addInt("zoomed canvas height", zoomedCanvasHeight);
//        boolean bigCanvas = view.isBigCanvas();
//        node.addBooleanChild("bigCanvas", bigCanvas);
//        boolean optimizedDrawingEnabled = view.getViewContainer().isOptimizedDrawingEnabled();
//        node.addBoolean("optimizedDrawingEnabled", optimizedDrawingEnabled);

        return node;
    }

    public static DebugNode createBufferedImageNode(String name, BufferedImage image) {
        var node = new DebugNode(name, image);

        node.add(createColorModelNode("color model", image.getColorModel()));
        node.add(createRasterNode(image.getRaster()));
        node.addString("type", DebugUtils.bufferedImageTypeAsString(image.getType()));
        node.addInt("width", image.getWidth());
        node.addInt("height", image.getHeight());
        node.addBoolean("alpha premultiplied", image.isAlphaPremultiplied());

        return node;
    }

    public static DebugNode createRasterNode(WritableRaster raster) {
        var node = new DebugNode("writable raster", raster);

        node.addClass();

        var sampleModel = raster.getSampleModel();
        node.add(createSampleModelNode(sampleModel));

        var dataBuffer = raster.getDataBuffer();
        node.add(createDataBufferNode(dataBuffer));

        return node;
    }

    static DebugNode createSampleModelNode(SampleModel sampleModel) {
        var node = new DebugNode("sample model", sampleModel);

        node.addClass();

        int width = sampleModel.getWidth();
        node.addInt("width", width);

        int height = sampleModel.getHeight();
        node.addInt("height", height);

        int dataType = sampleModel.getDataType();
        node.addString("data type", DebugUtils.dateBufferTypeAsString(dataType));

        int numBands = sampleModel.getNumBands();
        node.addInt("num bands", numBands);

        int transferType = sampleModel.getTransferType();
        node.addString("transfer type", DebugUtils.dateBufferTypeAsString(transferType));

        int numDataElements = sampleModel.getNumDataElements();
        node.addInt("num data elements", numDataElements);

        return node;
    }

    static DebugNode createDataBufferNode(DataBuffer dataBuffer) {
        var node = new DebugNode("data buffer", dataBuffer);

        node.addClass();

        int numBanks = dataBuffer.getNumBanks();
        node.addInt("num banks", numBanks);

        int type = dataBuffer.getDataType();
        node.addString("type", DebugUtils.dateBufferTypeAsString(type));

        int size = dataBuffer.getSize();
        node.addInt("size", size);

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

        node.addString("transfer type", DebugUtils.dateBufferTypeAsString(
                colorModel.getTransferType()));

        node.addString("transparency", DebugUtils.transparencyAsString(
                colorModel.getTransparency()));

        node.addBoolean("is RGB", DebugUtils.isRgbColorModel(colorModel));
        node.addBoolean("is BGR", DebugUtils.isBgrColorModel(colorModel));

        return node;
    }

    public static DebugNode createColorSpaceNode(ColorSpace colorSpace) {
        var node = new DebugNode("color space", colorSpace);

        node.addClass();

        int numComponents = colorSpace.getNumComponents();
        node.addInt("num components", numComponents);

        int type = colorSpace.getType();
        node.addString("type", DebugUtils.colorSpaceTypeAsString(type));

        boolean is_sRGB = colorSpace.isCS_sRGB();
        node.addBoolean("is sRGB", is_sRGB);

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
        node.addString("build state", path.getBuildState().toString());

        var activeSubpath = path.getActiveSubpath();
        for (int i = 0; i < numSubpaths; i++) {
            var subPath = path.getSubPath(i);
            if (subPath == activeSubpath) {
                node.add(createSubpathNode(
                        "active subpath " + subPath.getId(),
                        subPath));
            } else {
                node.add(createSubpathNode(subPath));
            }
        }

        return node;
    }

    public static DebugNode createSubpathNode(SubPath subPath) {
        return createSubpathNode("subpath " + subPath.getId(), subPath);
    }

    public static DebugNode createSubpathNode(String name, SubPath subPath) {
        var node = new DebugNode(name, subPath);

        node.addString("name", subPath.getId());
        node.addBoolean("closed", subPath.isClosed());
        node.addBoolean("finished", subPath.isFinished());
        node.addBoolean("has moving point", subPath.hasMovingPoint());

        return node;
    }
}
