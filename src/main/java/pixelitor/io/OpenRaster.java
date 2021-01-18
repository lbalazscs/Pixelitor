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

package pixelitor.io;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import pixelitor.Composition;
import pixelitor.ImageMode;
import pixelitor.layers.BlendingMode;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.utils.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
 * Only image layers are saved as the format does not cover
 * other layer types or layer masks.
 */
public class OpenRaster {
    private static final String MERGED_IMAGE_NAME = "mergedimage.png";

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

        String stackXML = format("""
            <?xml version='1.0' encoding='UTF-8'?>
            <image w="%d" h="%d">
            <stack>
            """, comp.getCanvasWidth(), comp.getCanvasHeight());

        int numLayers = comp.getNumLayers();
        int numImageLayers = comp.getNumImageLayers() + 1; // +1 for the merged image
        double workRatio = 1.0 / numImageLayers;

        // Reverse iteration: in stack.xml the first element in a stack is the uppermost.
        for (int i = numLayers - 1; i >= 0; i--) {
            Layer layer = comp.getLayer(i);
            if (layer instanceof ImageLayer) {
                ImageLayer imageLayer = (ImageLayer) layer;
                var subTracker = new SubtaskProgressTracker(workRatio, mainTracker);
                stackXML += writeLayer(imageLayer, i, zos, subTracker);
            }
        }

        // add merged image
        zos.putNextEntry(new ZipEntry(MERGED_IMAGE_NAME));
        var subTaskTracker = new SubtaskProgressTracker(workRatio, mainTracker);
        var img = comp.getCompositeImage();
        TrackedIO.writeToStream(img, zos, "PNG", subTaskTracker);
        zos.closeEntry();

        stackXML += "</stack>\n</image>";

        // write the stack.xml file
        zos.putNextEntry(new ZipEntry("stack.xml"));
        zos.write(stackXML.getBytes(UTF_8));
        zos.closeEntry();

        // write the mimetype
        zos.putNextEntry(new ZipEntry("mimetype"));
        zos.write("image/openraster".getBytes(UTF_8));
        zos.closeEntry();
        zos.close();

        mainTracker.finished();
    }

    private static String writeLayer(ImageLayer layer,
                                     int layerIndex,
                                     ZipOutputStream zos,
                                     ProgressTracker pt) throws IOException {
        String stackXML = format(Locale.ENGLISH,
            "<layer name=\"%s\" visibility=\"%s\" composite-op=\"%s\" " +
                "opacity=\"%f\" src=\"data/%d.png\" x=\"%d\" y=\"%d\"/>\n",
            layer.getName(),
            layer.getVisibilityAsORAString(),
            layer.getBlendingMode().toSVGName(),
            layer.getOpacity(),
            layerIndex,
            layer.getTx(),
            layer.getTy());

        var entry = new ZipEntry(format("data/%d.png", layerIndex));
        zos.putNextEntry(entry);
        BufferedImage image = layer.getImage();

        TrackedIO.writeToStream(image, zos, "PNG", pt);

        zos.closeEntry();
        return stackXML;
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
                    // no need for that
                } else if (FileUtils.hasPNGExtension(name)) {
                    var subTracker = new SubtaskProgressTracker(workRatio, mainTracker);
                    var stream = zipFile.getInputStream(entry);
                    var image = TrackedIO.readFromStream(stream, subTracker);
                    images.put(name, image);
                }
            }
        }

        if (stackXML == null) {
            throw new IllegalStateException("No stack.xml found.");
        }

        Element doc = loadXMLFromString(stackXML).getDocumentElement();
        doc.normalize();
        String documentElementNodeName = doc.getNodeName();
        if (!documentElementNodeName.equals("image")) {
            throw new IllegalStateException(format(
                "stack.xml root element is '%s', expected: 'image'",
                documentElementNodeName));
        }

        int compWidth = parseInt(doc.getAttribute("w").trim());
        int compHeight = parseInt(doc.getAttribute("h").trim());

        var comp = Composition.createEmpty(compWidth, compHeight, ImageMode.RGB);
        comp.setFile(file);

        NodeList layers = doc.getElementsByTagName("layer");
        for (int i = layers.getLength() - 1; i >= 0; i--) { // stack.xml contains layers in reverse order
            Node node = layers.item(i);
            Element element = (Element) node;

            String layerName = element.getAttribute("name");
            String layerVisibility = element.getAttribute("visibility");
            String layerVisible = element.getAttribute("visible");
            String layerBlendingMode = element.getAttribute("composite-op");
            String layerOpacity = element.getAttribute("opacity");
            String layerImageSource = element.getAttribute("src");
            String layerX = element.getAttribute("x");
            String layerY = element.getAttribute("y");

            BufferedImage image = images.get(layerImageSource);
            image = ImageUtils.toSysCompatibleImage(image);

            if (layerVisibility == null || layerVisibility.isEmpty()) {
                //workaround: paint.net exported files use "visible" attribute instead of "visibility"
                layerVisibility = layerVisible;
            }
            boolean visibility = layerVisibility == null || layerVisibility.equals("visible");

            int tx = Utils.parseInt(layerX, 0);
            int ty = Utils.parseInt(layerY, 0);
            ImageLayer layer = new ImageLayer(comp, image, layerName,
                null, tx, ty);

            layer.setVisible(visibility, false);
            BlendingMode blendingMode = BlendingMode.fromSVGName(layerBlendingMode);

            layer.setBlendingMode(blendingMode, false);
            float opacity = Utils.parseFloat(layerOpacity, 1.0f);
            layer.setOpacity(opacity, false);

            comp.addLayerInInitMode(layer);
        }

        mainTracker.finished();

        return comp;
    }

    private static int countNumImageFiles(ZipFile zipFile) {
        Enumeration<? extends ZipEntry> fileEntries = zipFile.entries();
        int numImageFiles = 0;
        while (fileEntries.hasMoreElements()) {
            ZipEntry entry = fileEntries.nextElement();
            String name = entry.getName().toLowerCase();
            if (name.endsWith("png") && !name.equals(MERGED_IMAGE_NAME)) {
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
        try (Scanner s = new Scanner(is, UTF_8.name()).useDelimiter("\\A")) {
            retVal = s.hasNext() ? s.next() : "";
        }
        return retVal;
    }
}
