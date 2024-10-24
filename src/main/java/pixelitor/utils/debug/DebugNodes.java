/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.PixelitorWindow;
import pixelitor.utils.AngleUnit;
import pixelitor.utils.MemoryInfo;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.File;
import java.util.Map;

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

        node.addQuotedString("max memory", MemoryInfo.getMaxHeapMb() + " Mb");
        node.addQuotedString("used memory", MemoryInfo.getTotalMemoryMb() + " Mb");

        node.add(createColorModelNode("default color model",
            device.getDefaultConfiguration().getColorModel()));

        return node;
    }

    public static DebugNode createBufferedImageNode(String name, BufferedImage image) {
        var node = new DebugNode(name, image);

        node.add(createColorModelNode("color model", image.getColorModel()));
        node.add(createRasterNode("raster", image.getRaster()));
        node.addString("type", Debug.bufferedImageTypeToString(image.getType()));
        node.addInt("width", image.getWidth());
        node.addInt("height", image.getHeight());
        node.addBoolean("alpha premultiplied", image.isAlphaPremultiplied());

        return node;
    }

    public static DebugNode createRasterNode(String key, Raster raster) {
        var node = new DebugNode(key, raster);

        node.addClass();
        node.addInt("width", raster.getWidth());
        node.addInt("height", raster.getHeight());
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
            Debug.dataBufferTypeToString(sampleModel.getDataType()));
        node.addInt("num bands", sampleModel.getNumBands());
        node.addString("transfer type",
            Debug.dataBufferTypeToString(sampleModel.getTransferType()));
        node.addInt("num data elements", sampleModel.getNumDataElements());

        return node;
    }

    private static DebugNode createDataBufferNode(DataBuffer dataBuffer) {
        var node = new DebugNode("data buffer", dataBuffer);

        node.addClass();
        node.addInt("num banks", dataBuffer.getNumBanks());
        node.addString("type", Debug.dataBufferTypeToString(dataBuffer.getDataType()));
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
            Debug.dataBufferTypeToString(colorModel.getTransferType()));
        node.addString("transparency",
            Debug.transparencyToString(colorModel.getTransparency()));
        node.addBoolean("is RGB", Debug.isRGB(colorModel));
        node.addBoolean("is BGR", Debug.isBGR(colorModel));

        return node;
    }

    private static DebugNode createColorSpaceNode(ColorSpace colorSpace) {
        var node = new DebugNode("color space", colorSpace);

        node.addClass();
        node.addInt("num components", colorSpace.getNumComponents());
        node.addString("type", Debug.colorSpaceTypeToString(colorSpace.getType()));
        node.addBoolean("sRGB", colorSpace.isCS_sRGB());

        return node;
    }

    public static DebugNode createTransformNode(String name, AffineTransform at) {
        DebugNode node = new DebugNode(name, at);

        // These are affected by rotation, they correspond
        // to the actual scaling only if there is no rotation.
        node.addDouble("scaleX (m00)", at.getScaleX());
        node.addDouble("scaleY (m11)", at.getScaleY());

        node.addDouble("shearX (m01)", at.getShearX());
        node.addDouble("shearY (m10)", at.getShearY());
        node.addDouble("translateX (m02)", at.getTranslateX());
        node.addDouble("translateY (m12)", at.getTranslateY());

        // the rotation-independent sx*sy
        node.addDouble("overall scaling", at.getDeterminant());

        // the scaling-independent rotation angle
        double angleRad = Math.atan2(at.getShearY(), at.getScaleX());
        node.addDouble("angle (degrees)", AngleUnit.RADIANS.toIntuitiveDegrees(angleRad));

        return node;
    }

    public static DebugNode createRectangleNode(String name, Rectangle rect) {
        DebugNode node = new DebugNode(name, rect);

        node.addInt("x", rect.x);
        node.addInt("y", rect.y);
        node.addInt("width", rect.width);
        node.addInt("height", rect.height);

        return node;
    }

    public static DebugNode createFileNode(String name, File file) {
        DebugNode node = new DebugNode(name, file);

        node.addQuotedString("path", file.getAbsolutePath());
        node.addBoolean("exists", file.exists());

        return node;
    }

    public static DebugNode createFontNode(String name, Font font) {
        DebugNode node = new DebugNode(name, font);

        node.addQuotedString("family name", font.getName());
        node.addQuotedString("face name", font.getFontName());
        node.addInt("size", font.getSize());
        node.addBoolean("bold", font.isBold());
        node.addBoolean("italic", font.isItalic());

        Map<TextAttribute, ?> attributes = font.getAttributes();
        DebugNode attrNode = new DebugNode("attributes", attributes);
        attributes.forEach((key, value) ->
            attrNode.addAsQuotedString(key.toString(), value));
        node.add(attrNode);

        return node;
    }
}
