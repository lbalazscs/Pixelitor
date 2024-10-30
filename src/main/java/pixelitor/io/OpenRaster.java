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
import pixelitor.utils.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * OpenRaster file format support.
 * Only image layers are saved as the format doesn't cover
 * other layer types or layer masks.
 */
public class OpenRaster {
    private static final String MERGED_IMAGE_NAME = "mergedimage.png";
    private static final String THUMBNAIL_IMAGE_NAME = "Thumbnails/thumbnail.png";

    private OpenRaster() {
    }

    public static void uncheckedWrite(Composition comp, File outFile) {
        try {
            write(comp, outFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void write(Composition comp, File outFile) throws IOException {
        var mainTracker = new StatusBarProgressTracker("Writing " + outFile.getName(), 100);

        var fos = new FileOutputStream(outFile);
        var zos = new ZipOutputStream(fos);

        // +1 for the merged image, and +1 for the thumbnail
        int numImages = comp.getNumORAExportableImages() + 2;
        double workRatio = 1.0 / numImages;

        StringBuilder stackXML = new StringBuilder(format("""
            <?xml version='1.0' encoding='UTF-8'?>
            <image w="%d" h="%d">
            """, comp.getCanvasWidth(), comp.getCanvasHeight()));
        writeHolder(comp, mainTracker, zos, stackXML, workRatio, 0);
        stackXML.append("</image>");

        // add the merged image
        zos.putNextEntry(new ZipEntry(MERGED_IMAGE_NAME));
        var mergedTracker = new SubtaskProgressTracker(workRatio, mainTracker);
        var img = comp.getCompositeImage();
        TrackedIO.writeToStream(img, zos, "PNG", mergedTracker);
        zos.closeEntry();

        // add the thumbnail image
        zos.putNextEntry(new ZipEntry(THUMBNAIL_IMAGE_NAME));
        var thumbTracker = new SubtaskProgressTracker(workRatio, mainTracker);
        var thumb = createORAThumbnail(comp.getCompositeImage());
        TrackedIO.writeToStream(thumb, zos, "PNG", thumbTracker);
        zos.closeEntry();

        // write the stack.xml file
        zos.putNextEntry(new ZipEntry("stack.xml"));
        zos.write(stackXML.toString().getBytes(UTF_8));
        zos.closeEntry();

        // write the mimetype
        zos.putNextEntry(new ZipEntry("mimetype"));
        zos.write("image/openraster".getBytes(UTF_8));
        zos.closeEntry();
        zos.close();

        mainTracker.finished();
    }

    private static int writeHolder(LayerHolder holder, StatusBarProgressTracker mainTracker, ZipOutputStream zos, StringBuilder stackXML, double workRatio, int uniqueId) throws IOException {
        stackXML.append(holder.getORAStackXML());

        int numLayers = holder.getNumLayers();
        // Reverse iteration: in stack.xml the first element in a stack is the uppermost.
        for (int i = numLayers - 1; i >= 0; i--) {
            Layer layer = holder.getLayer(i);
            if (layer instanceof LayerGroup group) {
                uniqueId = writeHolder(group, mainTracker, zos, stackXML, workRatio, uniqueId);
            } else if (layer.exportsORAImage()) {
                var subTracker = new SubtaskProgressTracker(workRatio, mainTracker);
                writeLayer(layer, uniqueId, zos, subTracker, stackXML);
                uniqueId++;
            }
        }

        stackXML.append("</stack>");
        return uniqueId;
    }

    private static void writeLayer(Layer layer,
                                   int uniqueId,
                                   ZipOutputStream zos,
                                   ProgressTracker pt,
                                   StringBuilder stackXML) throws IOException {
        TranslatedImage translatedImage = layer.getTranslatedImage();

        String xml = format(Locale.ENGLISH,
            "<layer name=\"%s\" visibility=\"%s\" composite-op=\"%s\" " +
                "opacity=\"%f\" src=\"data/%d.png\" x=\"%d\" y=\"%d\"/>\n",
            layer.getName(),
            layer.getVisibilityAsORAString(),
            layer.getBlendingMode().toSVGName(),
            layer.getOpacity(),
            uniqueId,
            translatedImage.tx(),
            translatedImage.ty());
        stackXML.append(xml);

        var entry = new ZipEntry(format("data/%d.png", uniqueId));
        zos.putNextEntry(entry);

        TrackedIO.writeToStream(translatedImage.img(), zos, "PNG", pt);

        zos.closeEntry();
    }

    public static Composition read(File file) throws IOException, ParserConfigurationException, SAXException {
        String stackXML = null;
        var mainTracker = new StatusBarProgressTracker("Reading " + file.getName(), 100);
        Map<String, BufferedImage> images = new HashMap<>();
        try (ZipFile zipFile = new ZipFile(file)) {
            // first iterate to count the image files...
            int numImageFiles = countNumImageFiles(zipFile);
            double workRatio = 1.0 / numImageFiles;

            // ...then iterate again to actually read the files
            var fileEntries = zipFile.entries();
            while (fileEntries.hasMoreElements()) {
                ZipEntry entry = fileEntries.nextElement();
                String name = entry.getName();

                if (name.equalsIgnoreCase("stack.xml")) {
                    stackXML = extractString(zipFile.getInputStream(entry));
                } else if (name.equalsIgnoreCase(MERGED_IMAGE_NAME)) {
                    // no need to read it
                } else if (name.equalsIgnoreCase(THUMBNAIL_IMAGE_NAME)) {
                    // no need to read it
                } else if (FileUtils.hasPNGExtension(name)) {
                    var subTracker = new SubtaskProgressTracker(workRatio, mainTracker);
                    var stream = zipFile.getInputStream(entry);
                    var image = TrackedIO.readFromStream(stream, subTracker);
                    images.put(name, image);
                }
            }
        }

        if (stackXML == null) {
            throw new IllegalStateException("No stack.xml found in " + file.getAbsolutePath());
        }

        Element doc = loadXMLFromString(stackXML).getDocumentElement();
        doc.normalize();
        String docNodeName = doc.getNodeName();
        if (!docNodeName.equals("image")) {
            throw new IllegalStateException(format(
                "stack.xml root element is '%s', expected: 'image'",
                docNodeName));
        }

        int compWidth = parseInt(doc.getAttribute("w").trim());
        int compHeight = parseInt(doc.getAttribute("h").trim());

        var comp = Composition.createEmpty(compWidth, compHeight, ImageMode.RGB);
        comp.setFile(file);
        comp.createDebugName();

        Node mainStackElement = doc.getFirstChild();
        // make sure that text nodes caused by whitespace are ignored
        while (!(mainStackElement instanceof Element)) {
            mainStackElement = mainStackElement.getNextSibling();
        }

        readHolder(mainStackElement, comp, images);

        mainTracker.finished();

        return comp;
    }

    // reads a stack element
    private static void readHolder(Node stackNode, LayerHolder parent, Map<String, BufferedImage> images) {
        assert stackNode.getNodeName().equals("stack");

        NodeList childNodes = stackNode.getChildNodes();
        for (int i = childNodes.getLength() - 1; i >= 0; i--) { // stack.xml contains layers in reverse order
            Node child = childNodes.item(i);
            String childNodeName = child.getNodeName();
            if (childNodeName.equals("stack")) {
                Element childElem = (Element) child;
                String groupName = childElem.getAttribute("name");
                LayerGroup group = new LayerGroup(parent.getComp(), groupName);
                group.setHolder(parent);
                parent.addLayerNoUI(group);
                readBasicAttributes(childElem, group);

                String isolation = childElem.getAttribute("isolation");
                if (isolation != null) {
                    if (isolation.equals("auto")) {
                        group.setBlendingMode(BlendingMode.PASS_THROUGH);
                    }
                }

                readHolder(child, group, images);
            } else if (childNodeName.equals("layer")) {
                readLayer(images, parent, (Element) child);
            }
        }
    }

    private static void readLayer(Map<String, BufferedImage> images, LayerHolder holder, Element element) {
        BufferedImage image = images.get(element.getAttribute("src"));
        image = ImageUtils.toSysCompatibleImage(image);

        int tx = Utils.parseInt(element.getAttribute("x"), 0);
        int ty = Utils.parseInt(element.getAttribute("y"), 0);
        String layerName = element.getAttribute("name");

        ImageLayer layer = new ImageLayer(holder.getComp(), image, layerName, 0, 0);
        // Pixelitor doesn't support > 0 translations for image layers
        // (i.e. image layers where the image doesn't fully cover the canvas)
        // therefore the image must be enlarged
        // Also, Krita can export 1x1 pngs for untouched paint layers (without translation)
        layer.forceTranslation(tx, ty);
        layer.enlargeCanvas(Outsets.createZero());

        readBasicAttributes(element, layer);

        holder.addLayerNoUI(layer);
    }

    private static void readBasicAttributes(Element element, Layer layer) {
        String layerVisibility = element.getAttribute("visibility");
        if (layerVisibility == null || layerVisibility.isEmpty()) {
            //workaround: paint.net exported files use "visible" attribute instead of "visibility"
            layerVisibility = element.getAttribute("visible");
        }
        boolean visibility = layerVisibility == null || layerVisibility.equals("visible");
        layer.setVisible(visibility);

        layer.setBlendingMode(BlendingMode.fromSVGName(
            element.getAttribute("composite-op")));

        layer.setOpacity(Utils.parseFloat(
            element.getAttribute("opacity"), 1.0f));
    }

    private static int countNumImageFiles(ZipFile zipFile) {
        Enumeration<? extends ZipEntry> fileEntries = zipFile.entries();
        int numImageFiles = 0;
        while (fileEntries.hasMoreElements()) {
            ZipEntry entry = fileEntries.nextElement();
            String name = entry.getName();
            String nameLC = name.toLowerCase(Locale.ROOT);

            if (nameLC.endsWith("png") && !name.equals(MERGED_IMAGE_NAME) && !name.equals(THUMBNAIL_IMAGE_NAME)) {
                numImageFiles++;
            }
        }
        return numImageFiles;
    }

    private static Document loadXMLFromString(String xml)
        throws ParserConfigurationException, IOException, SAXException {

        if (xml.startsWith("\uFEFF")) { // starts with UTF BOM character
            // paint.net exported xml files start with this
            // see http://www.rgagnon.com/javadetails/java-handle-utf8-file-with-bom.html
            xml = xml.substring(1);
        }

        var factory = DocumentBuilderFactory.newInstance();
        var builder = factory.newDocumentBuilder();
        var inputSource = new InputSource(new StringReader(xml));
        return builder.parse(inputSource);
    }

    private static String extractString(InputStream is) {
        String retVal;
        try (Scanner s = new Scanner(is, UTF_8).useDelimiter("\\A")) {
            retVal = s.hasNext() ? s.next() : "";
        }
        return retVal;
    }

    private static BufferedImage createORAThumbnail(BufferedImage src) {
        // "It must be a non-interlaced PNG with 8 bits per channel
        // of at most 256x256 pixels. It should be as big as possible
        // without upscaling or changing the aspect ratio."
        Dimension thumbSize = ImageUtils.calcThumbDimensions(src.getWidth(), src.getHeight(), 256, false);
        return ImageUtils.resize(src, thumbSize.width, thumbSize.height);
    }
}
