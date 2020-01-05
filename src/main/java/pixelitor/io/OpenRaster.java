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

package pixelitor.io;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import pixelitor.Composition;
import pixelitor.layers.BlendingMode;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.ProgressTracker;
import pixelitor.utils.StatusBarProgressTracker;
import pixelitor.utils.SubtaskProgressTracker;
import pixelitor.utils.Utils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * OpenRaster file format support.
 * Only image layers are saved as the format does not cover other layer types.
 */
public class OpenRaster {
    private static final String MERGED_IMAGE_NAME = "mergedimage.png";

    private OpenRaster() {
    }

    public static void uncheckedWrite(Composition comp, File outFile, boolean addMergedImage) {
        try {
            write(comp, outFile, addMergedImage);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void write(Composition comp, File outFile, boolean addMergedImage) throws IOException {
        ProgressTracker pt = new StatusBarProgressTracker("Writing " + outFile.getName(), 100);

        FileOutputStream fos = new FileOutputStream(outFile);
        ZipOutputStream zos = new ZipOutputStream(fos);

        String stackXML = format("<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<image w=\"%d\" h=\"%d\">\n" +
                "<stack>\n", comp.getCanvasImWidth(), comp.getCanvasImHeight());

        int numLayers = comp.getNumLayers();
        int numImageLayers = comp.getNumImageLayers();
        if (addMergedImage) {
            numImageLayers++;
        }
        double workRatio = 1.0 / numImageLayers;

        // Reverse iteration: in stack.xml the first element in a stack is the uppermost.
        for (int i = numLayers - 1; i >= 0; i--) {
            Layer layer = comp.getLayer(i);
            if (layer instanceof ImageLayer) {
                ImageLayer imageLayer = (ImageLayer) layer;
                ProgressTracker spt = new SubtaskProgressTracker(workRatio, pt);
                stackXML += writeLayer(imageLayer, i, zos, spt);
            }
        }

        if (addMergedImage) {
            zos.putNextEntry(new ZipEntry(MERGED_IMAGE_NAME));
            ProgressTracker subTaskTracker = new SubtaskProgressTracker(workRatio, pt);
            BufferedImage img = comp.getCompositeImage();
            TrackedIO.writeToStream(img, zos, "PNG", subTaskTracker);
            zos.closeEntry();
        }

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

        pt.finished();
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
        ZipEntry entry = new ZipEntry(format("data/%d.png", layerIndex));
        zos.putNextEntry(entry);
        BufferedImage image = layer.getImage();

        TrackedIO.writeToStream(image, zos, "PNG", pt);

        zos.closeEntry();
        return stackXML;
    }

    public static Composition read(File file) throws IOException, ParserConfigurationException, SAXException {
        String stackXML = null;
        ProgressTracker pt = new StatusBarProgressTracker("Reading " + file.getName(), 100);
        Map<String, BufferedImage> images = new HashMap<>();
        try (ZipFile zipFile = new ZipFile(file)) {
            // first iterate to count the image files...
            int numImageFiles = countNumImageFiles(zipFile);
            double workRatio = 1.0 / numImageFiles;

            // ...then iterate again to actually read the files
            Enumeration<? extends ZipEntry> fileEntries = zipFile.entries();
            while (fileEntries.hasMoreElements()) {
                ZipEntry entry = fileEntries.nextElement();
                String name = entry.getName();

                if (name.equalsIgnoreCase("stack.xml")) {
                    stackXML = extractString(zipFile.getInputStream(entry));
                } else if (name.equalsIgnoreCase(MERGED_IMAGE_NAME)) {
                    // no need for that
                } else if (FileUtils.hasPNGExtension(name)) {
                    ProgressTracker spt = new SubtaskProgressTracker(workRatio, pt);
                    InputStream stream = zipFile.getInputStream(entry);
                    BufferedImage image = TrackedIO.readFromStream(stream, spt);
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

        var comp = Composition.createEmpty(compWidth, compHeight);
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
            // TODO assuming that there is no layer mask
            ImageLayer layer = new ImageLayer(comp, image, layerName,
                    null, tx, ty);

            layer.setVisible(visibility, false, false);
            BlendingMode blendingMode = BlendingMode.fromSVGName(layerBlendingMode);

            layer.setBlendingMode(blendingMode, false, false, false);
            float opacity = Utils.parseFloat(layerOpacity, 1.0f);
            layer.setOpacity(opacity, false, false, false);

            comp.addLayerInInitMode(layer);
        }

        pt.finished();

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

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }

    private static String extractString(InputStream is) {
        String retVal;
        try (Scanner s = new Scanner(is, UTF_8.name()).useDelimiter("\\A")) {
            retVal = s.hasNext() ? s.next() : "";
        }
        return retVal;
    }
}
