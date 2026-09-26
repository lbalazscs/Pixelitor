/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.io;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import pixelitor.Composition;
import pixelitor.ImageMode;
import pixelitor.compactions.Outsets;
import pixelitor.layers.*;
import pixelitor.progress.ProgressTracker;
import pixelitor.progress.StatusBarProgressTracker;
import pixelitor.progress.SubtaskProgressTracker;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Thumbnails;
import pixelitor.utils.Utils;

import javax.imageio.ImageIO;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static java.lang.Integer.parseInt;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Export/import and thumbnail support for the OpenRaster file format.
 */
public class OpenRaster {
    private static final String MERGED_IMAGE_PATH = "mergedimage.png";
    private static final String THUMBNAIL_PATH = "Thumbnails/thumbnail.png";
    private static final String STACK_XML_PATH = "stack.xml";
    private static final String MIME_TYPE_PATH = "mimetype";
    private static final String MIME_TYPE = "image/openraster";
    private static final int THUMBNAIL_MAX_DIMENSION = 256;
    private static final String UTF8_BOM_CHARACTER = "\uFEFF";
    private static final String XML_ROOT_ELEMENT = "image";

    private OpenRaster() {
    }

    /**
     * Writes a composition to an OpenRaster file, wrapping IOExceptions in UncheckedIOException.
     */
    public static void uncheckedWrite(Composition comp, File outputFile) {
        try {
            write(comp, outputFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes a composition to an OpenRaster file.
     */
    public static void write(Composition comp, File outputFile) throws IOException {
        var mainTracker = new StatusBarProgressTracker("Writing " + outputFile.getName(), 100);

        try (var zipStream = new ZipOutputStream(
            new BufferedOutputStream(
                new FileOutputStream(outputFile)))) {

            // write the mimetype first and uncompressed
            writeMimetypeEntry(zipStream);

            // +1 for the merged image, and +1 for the thumbnail
            int totalImages = comp.getNumORAExportableImages() + 2;
            double progressPerImage = 1.0 / totalImages;

            // writes the layer images
            StringBuilder stackXML = createStackXMLRoot(comp);
            writeLayerHierarchy(comp, mainTracker, zipStream, stackXML, progressPerImage, 0);
            stackXML.append("</image>");

            // writes the merged image
            zipStream.putNextEntry(new ZipEntry(MERGED_IMAGE_PATH));
            var mergedTracker = new SubtaskProgressTracker(progressPerImage, mainTracker);
            var compositeImg = comp.getCompositeImage();
            TrackedIO.writeToStream(compositeImg, zipStream, "PNG", mergedTracker);
            zipStream.closeEntry();

            // writes the thumbnail image
            zipStream.putNextEntry(new ZipEntry(THUMBNAIL_PATH));
            var thumbTracker = new SubtaskProgressTracker(progressPerImage, mainTracker);
            var thumb = createORAThumbnail(compositeImg);
            TrackedIO.writeToStream(thumb, zipStream, "PNG", thumbTracker);
            zipStream.closeEntry();

            // writes the stack.xml file
            zipStream.putNextEntry(new ZipEntry(STACK_XML_PATH));
            zipStream.write(stackXML.toString().getBytes(UTF_8));
            zipStream.closeEntry();
        }
        mainTracker.finished();
    }

    private static void writeMimetypeEntry(ZipOutputStream zipStream) throws IOException {
        byte[] content = MIME_TYPE.getBytes(UTF_8);
        var entry = new ZipEntry(MIME_TYPE_PATH);
        entry.setMethod(ZipEntry.STORED); // must not be compressed
        entry.setSize(content.length);
        entry.setCompressedSize(content.length);
        var crc32 = new CRC32();
        crc32.update(content);
        entry.setCrc(crc32.getValue());

        zipStream.putNextEntry(entry);
        zipStream.write(content);
        zipStream.closeEntry();
    }

    private static StringBuilder createStackXMLRoot(Composition comp) {
        return new StringBuilder(String.format("""
            <?xml version='1.0' encoding='UTF-8'?>
            <image w="%d" h="%d">
            """, comp.getCanvasWidth(), comp.getCanvasHeight()));
    }

    // recursively writes the layers of the given holder
    private static int writeLayerHierarchy(LayerHolder holder,
                                           StatusBarProgressTracker mainTracker,
                                           ZipOutputStream zipStream,
                                           StringBuilder stackXML,
                                           double progressPerImage,
                                           int imageId) throws IOException {
        stackXML.append(getStackStartTag(holder));

        int numLayers = holder.getNumLayers();
        // reverse iteration because OpenRaster defines the
        // first child of a <stack> as the top-most visual layer
        for (int i = numLayers - 1; i >= 0; i--) {
            Layer layer = holder.getLayer(i);
            if (layer instanceof LayerGroup group) {
                // recursively writes layer groups
                imageId = writeLayerHierarchy(group, mainTracker, zipStream, stackXML, progressPerImage, imageId);
            } else if (layer.canExportORAImage()) {
                // writes exportable layers
                var subTracker = new SubtaskProgressTracker(progressPerImage, mainTracker);
                writeLayer(layer, imageId, zipStream, subTracker, stackXML);
                imageId++;
            }
            // skips non-exportable layers (such as adjustment layers)
        }

        stackXML.append("</stack>");
        return imageId;
    }

    /**
     * Returns the opening XML tag for the layer stack represented by the given holder.
     */
    private static String getStackStartTag(LayerHolder holder) {
        if (holder instanceof Composition) {
            return "<stack>\n";
        } else if (holder instanceof LayerGroup group) {
            BlendingMode blendingMode = group.getBlendingMode();
            return String.format(Locale.ROOT,
                "<stack composite-op=\"%s\" name=\"%s\" opacity=\"%f\" visibility=\"%s\" isolation=\"%s\">\n",
                blendingMode.toSVGName(),
                escapeXml(group.getName()),
                group.getOpacity(),
                getVisibilityAsORAString(group),
                blendingMode == BlendingMode.PASS_THROUGH ? "auto" : "isolate");
        } else {
            // should not be called for smart objects
            throw new IllegalStateException("Unexpected holder: " + holder.getClass().getName());
        }
    }

    /**
     * Returns the visibility of the given layer as an OpenRaster string.
     */
    private static String getVisibilityAsORAString(Layer layer) {
        return layer.isVisible() ? "visible" : "hidden";
    }

    private static void writeLayer(Layer layer,
                                   int imageId,
                                   ZipOutputStream zipStream,
                                   ProgressTracker pt,
                                   StringBuilder stackXML) throws IOException {
        ORAImageInfo imageInfo = layer.getORAImageInfo();

        String xml = String.format(Locale.ROOT,
            "<layer name=\"%s\" visibility=\"%s\" composite-op=\"%s\" " +
                "opacity=\"%f\" src=\"data/%d.png\" x=\"%d\" y=\"%d\"/>\n",
            escapeXml(layer.getName()),
            getVisibilityAsORAString(layer),
            layer.getBlendingMode().toSVGName(),
            layer.getOpacity(),
            imageId,
            imageInfo.tx(),
            imageInfo.ty());
        stackXML.append(xml);

        var entry = new ZipEntry("data/" + imageId + ".png");
        zipStream.putNextEntry(entry);

        TrackedIO.writeToStream(imageInfo.exportedImage(), zipStream, "PNG", pt);

        zipStream.closeEntry();
    }

    /**
     * Reads an OpenRaster file and creates a composition.
     */
    public static Composition read(File file) throws IOException, ParserConfigurationException, SAXException {
        var mainTracker = new StatusBarProgressTracker("Reading " + file.getName(), 100);
        Map<String, BufferedImage> imagesByPath = new HashMap<>();
        String stackXML = null;

        try (ZipFile zipFile = new ZipFile(file)) {
            // first iterate to count the image files...
            int numImageFiles = countImageFiles(zipFile);
            double progressPerImage = 1.0 / numImageFiles;

            // ...then iterate again to actually read the files
            var fileEntries = zipFile.entries();
            while (fileEntries.hasMoreElements()) {
                ZipEntry entry = fileEntries.nextElement();
                String name = entry.getName();

                if (name.equals(STACK_XML_PATH)) {
                    InputStream is = zipFile.getInputStream(entry);
                    stackXML = new String(is.readAllBytes(), UTF_8);
                } else if (name.equals(MERGED_IMAGE_PATH)) {
                    // no need to read it
                } else if (name.equals(THUMBNAIL_PATH)) {
                    // no need to read it
                } else if (FileUtils.hasPNGExtension(name)) {
                    var subTracker = new SubtaskProgressTracker(progressPerImage, mainTracker);
                    var stream = zipFile.getInputStream(entry);
                    var image = TrackedIO.readFromStream(stream, subTracker);
                    imagesByPath.put(name, image);
                }
            }
        }

        if (stackXML == null) {
            throw new IllegalStateException("No stack.xml found in " + file.getAbsolutePath());
        }

        Element rootElement = loadXMLFromString(stackXML).getDocumentElement();
        String rootNodeName = rootElement.getNodeName();
        if (!rootNodeName.equals(XML_ROOT_ELEMENT)) {
            throw new IllegalStateException(String.format(
                "stack.xml root element is '%s', expected: 'image'", rootNodeName));
        }

        int compWidth = parseInt(rootElement.getAttribute("w").trim());
        int compHeight = parseInt(rootElement.getAttribute("h").trim());

        var comp = Composition.createEmpty(compWidth, compHeight, ImageMode.RGB);
        comp.setFile(file);
        comp.initDebugName();

        Node mainStackElement = rootElement.getFirstChild();
        // ignore text nodes caused by whitespace
        while (mainStackElement != null && !(mainStackElement instanceof Element)) {
            mainStackElement = mainStackElement.getNextSibling();
        }
        if (mainStackElement == null) {
            throw new IllegalStateException("No root <stack> element found in stack.xml");
        }

        readHolder(mainStackElement, comp, imagesByPath);

        mainTracker.finished();

        return comp;
    }

    // reads a stack element
    private static void readHolder(Node stackNode, LayerHolder parent, Map<String, BufferedImage> imagesByPath) {
        assert stackNode.getNodeName().equals("stack");

        NodeList childNodes = stackNode.getChildNodes();
        for (int i = childNodes.getLength() - 1; i >= 0; i--) { // stack.xml contains layers in reverse order
            Node child = childNodes.item(i);
            String childNodeName = child.getNodeName();
            if (childNodeName.equals("stack")) { // a stack child must be a layer group
                Element childElem = (Element) child;
                String groupName = childElem.getAttribute("name");
                LayerGroup group = new LayerGroup(parent.getComp(), groupName);
                group.setHolder(parent);
                parent.addLayerWithoutUI(group);
                readBasicAttributes(childElem, group);

                if (childElem.getAttribute("isolation").equals("auto")) {
                    group.setBlendingMode(BlendingMode.PASS_THROUGH);
                }

                readHolder(child, group, imagesByPath);
            } else if (childNodeName.equals("layer")) {
                readLayer(parent, (Element) child, imagesByPath);
            }
        }
    }

    private static void readLayer(LayerHolder holder, Element element, Map<String, BufferedImage> imagesByPath) {
        String layerName = element.getAttribute("name");

        String src = element.getAttribute("src");
        BufferedImage image = imagesByPath.get(src);
        if (image == null) {
            throw new IllegalStateException("Missing image for layer '" + layerName + "': " + src);
        }
        image = ImageUtils.toSysCompatibleImage(image);

        int tx = Utils.parseInt(element.getAttribute("x"), 0);
        int ty = Utils.parseInt(element.getAttribute("y"), 0);

        ImageLayer layer = new ImageLayer(holder.getComp(), image, layerName, 0, 0);
        // Pixelitor doesn't support > 0 translations for image layers
        // (i.e. image layers where the image doesn't fully cover the canvas)
        // therefore the image must be enlarged
        // Also, Krita can export 1x1 PNGs for untouched paint layers (without translation)
        layer.forceTranslation(tx, ty);
        layer.enlargeCanvas(Outsets.createZero());

        readBasicAttributes(element, layer);

        holder.addLayerWithoutUI(layer);
    }

    private static void readBasicAttributes(Element element, Layer layer) {
        layer.setVisible(readVisibilityAttribute(element));

        layer.setBlendingMode(BlendingMode.fromSVGName(
            element.getAttribute("composite-op")));

        layer.setOpacity(Utils.parseFloat(
            element.getAttribute("opacity"), 1.0f));
    }

    private static boolean readVisibilityAttribute(Element element) {
        String layerVisibility = element.getAttribute("visibility");
        if (layerVisibility.isEmpty()) {
            //workaround: paint.net exported files use "visible" attribute instead of "visibility"
            layerVisibility = element.getAttribute("visible");
        }

        // a layer is visible unless explicitly marked otherwise
        return layerVisibility.isEmpty() || layerVisibility.equals("visible");
    }

    /**
     * Counts the PNG image files that have to be decoded during
     * importing (ignoring merged and thumbnail images).
     */
    private static int countImageFiles(ZipFile zipFile) {
        Enumeration<? extends ZipEntry> fileEntries = zipFile.entries();
        int numImageFiles = 0;
        while (fileEntries.hasMoreElements()) {
            ZipEntry entry = fileEntries.nextElement();
            String name = entry.getName();

            if (FileUtils.hasPNGExtension(name) && !name.equals(MERGED_IMAGE_PATH) && !name.equals(THUMBNAIL_PATH)) {
                numImageFiles++;
            }
        }
        return numImageFiles;
    }

    private static Document loadXMLFromString(String xml)
        throws ParserConfigurationException, IOException, SAXException {

        if (xml.startsWith(UTF8_BOM_CHARACTER)) {
            // paint.net exported xml files start with this
            // see http://www.rgagnon.com/javadetails/java-handle-utf8-file-with-bom.html
            xml = xml.substring(1);
        }

        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        var builder = factory.newDocumentBuilder();
        var inputSource = new InputSource(new StringReader(xml));
        return builder.parse(inputSource);
    }

    public static BufferedImage createORAThumbnail(BufferedImage src) {
        // Create a thumbnail according to the OpenRaster spec:
        // "It must be a non-interlaced PNG with 8 bits per channel
        // of at most 256x256 pixels. It should be as big as possible
        // without upscaling or changing the aspect ratio."
        Dimension thumbSize = Thumbnails.calcThumbDimensions(
            src.getWidth(), src.getHeight(), THUMBNAIL_MAX_DIMENSION, false);
        return ImageUtils.resize(src, thumbSize.width, thumbSize.height);
    }

    /**
     * Reads only the thumbnail from an OpenRaster file.
     */
    public static BufferedImage readThumbnail(File file) throws IOException {
        try (ZipFile zipFile = new ZipFile(file)) {
            ZipEntry thumbnailEntry = zipFile.getEntry(THUMBNAIL_PATH);
            if (thumbnailEntry == null) {
                return null; // thumbnail not found
            }

            try (InputStream inputStream = zipFile.getInputStream(thumbnailEntry)) {
                return ImageIO.read(inputStream);
            }
        }
    }

    // assumes that we only use double-quote delimiting for the attributes
    private static String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
