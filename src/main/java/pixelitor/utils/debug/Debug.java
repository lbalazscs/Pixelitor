/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.*;
import pixelitor.colors.Colors;
import pixelitor.colors.FillType;
import pixelitor.filters.Filter;
import pixelitor.filters.painters.TextSettings;
import pixelitor.filters.util.Filters;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.View;
import pixelitor.gui.utils.AlignmentSelector;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.io.FileUtils;
import pixelitor.layers.*;
import pixelitor.tools.Tools;
import pixelitor.tools.gui.ToolButton;
import pixelitor.tools.pen.Path;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.Shapes;
import pixelitor.utils.Utils;
import pixelitor.utils.input.*;

import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static javax.swing.BorderFactory.createEmptyBorder;
import static pixelitor.Views.addNew;
import static pixelitor.Views.findCompByName;
import static pixelitor.utils.Threads.calledOutsideEDT;

/**
 * Debugging-related static utility methods
 */
public class Debug {
    private Debug() {
        // shouldn't be instantiated
    }

    public static String dataBufferTypeToString(int type) {
        return switch (type) {
            case DataBuffer.TYPE_BYTE -> "BYTE";
            case DataBuffer.TYPE_USHORT -> "USHORT";
            case DataBuffer.TYPE_SHORT -> "SHORT";
            case DataBuffer.TYPE_INT -> "INT";
            case DataBuffer.TYPE_FLOAT -> "FLOAT";
            case DataBuffer.TYPE_DOUBLE -> "DOUBLE";
            case DataBuffer.TYPE_UNDEFINED -> "UNDEFINED";
            default -> "unrecognized (" + type + ")";
        };
    }

    static String transparencyToString(int transparency) {
        return switch (transparency) {
            case Transparency.OPAQUE -> "OPAQUE";
            case Transparency.BITMASK -> "BITMASK";
            case Transparency.TRANSLUCENT -> "TRANSLUCENT";
            default -> "unrecognized (" + transparency + ")";
        };
    }

    public static String bufferedImageTypeToString(int type) {
        return switch (type) {
            case BufferedImage.TYPE_3BYTE_BGR -> "3BYTE_BGR";
            case BufferedImage.TYPE_4BYTE_ABGR -> "4BYTE_ABGR";
            case BufferedImage.TYPE_4BYTE_ABGR_PRE -> "4BYTE_ABGR_PRE";
            case BufferedImage.TYPE_BYTE_BINARY -> "BYTE_BINARY";
            case BufferedImage.TYPE_BYTE_GRAY -> "BYTE_GRAY";
            case BufferedImage.TYPE_BYTE_INDEXED -> "BYTE_INDEXED";
            case BufferedImage.TYPE_CUSTOM -> "CUSTOM";
            case BufferedImage.TYPE_INT_ARGB -> "INT_ARGB";
            case BufferedImage.TYPE_INT_ARGB_PRE -> "INT_ARGB_PRE";
            case BufferedImage.TYPE_INT_BGR -> "INT_BGR";
            case BufferedImage.TYPE_INT_RGB -> "INT_RGB";
            case BufferedImage.TYPE_USHORT_555_RGB -> "USHORT_555_RGB";
            case BufferedImage.TYPE_USHORT_565_RGB -> "USHORT_565_RGB";
            case BufferedImage.TYPE_USHORT_GRAY -> "USHORT_GRAY";
            default -> "unrecognized (" + type + ")";
        };
    }

    static String colorSpaceTypeToString(int type) {
        return switch (type) {
            case ColorSpace.TYPE_2CLR -> "2CLR";
            case ColorSpace.TYPE_3CLR -> "3CLR";
            case ColorSpace.TYPE_4CLR -> "4CLR";
            case ColorSpace.TYPE_5CLR -> "5CLR";
            case ColorSpace.TYPE_6CLR -> "6CLR";
            case ColorSpace.TYPE_7CLR -> "7CLR";
            case ColorSpace.TYPE_8CLR -> "8CLR";
            case ColorSpace.TYPE_9CLR -> "9CLR";
            case ColorSpace.TYPE_ACLR -> "ACLR";
            case ColorSpace.TYPE_BCLR -> "BCLR";
            case ColorSpace.TYPE_CCLR -> "CCLR";
            case ColorSpace.TYPE_CMY -> "CMY";
            case ColorSpace.TYPE_CMYK -> "CMYK";
            case ColorSpace.TYPE_DCLR -> "DCLR";
            case ColorSpace.TYPE_ECLR -> "ECLR";
            case ColorSpace.TYPE_FCLR -> "FCLR";
            case ColorSpace.TYPE_GRAY -> "GRAY";
            case ColorSpace.TYPE_HLS -> "HLS";
            case ColorSpace.TYPE_HSV -> "HSV";
            case ColorSpace.TYPE_Lab -> "Lab";
            case ColorSpace.TYPE_Luv -> "Luv";
            case ColorSpace.TYPE_RGB -> "RGB";
            case ColorSpace.TYPE_XYZ -> "XYZ";
            case ColorSpace.TYPE_YCbCr -> "YCbCr";
            case ColorSpace.TYPE_Yxy -> "Yxy";
            default -> "unrecognized (" + type + ")";
        };
    }

    public static boolean isRGB(ColorModel cm) {
        if (cm instanceof DirectColorModel dcm
            && cm.getTransferType() == DataBuffer.TYPE_INT) {

            return dcm.getRedMask() == 0x00_FF_00_00
                && dcm.getGreenMask() == 0x00_00_FF_00
                && dcm.getBlueMask() == 0x00_00_00_FF
                && (dcm.getNumComponents() == 3 || dcm.getAlphaMask() == 0xFF_00_00_00);
        }

        return false;
    }

    public static boolean isBGR(ColorModel cm) {
        if (cm instanceof DirectColorModel dcm &&
            cm.getTransferType() == DataBuffer.TYPE_INT) {

            return dcm.getRedMask() == 0x00_00_00_FF
                && dcm.getGreenMask() == 0x00_00_FF_00
                && dcm.getBlueMask() == 0x00_FF_00_00
                && (dcm.getNumComponents() == 3 || dcm.getAlphaMask() == 0xFF_00_00_00);
        }

        return false;
    }

    public static String modifiersToString(MouseEvent e) {
        return modifiersToString(e.isControlDown(), e.isAltDown(), e.isShiftDown(),
            SwingUtilities.isRightMouseButton(e), e.isPopupTrigger());
    }

    public static String modifiersToString(Modifiers modifiers, boolean popup) {
        return modifiersToString(modifiers.ctrl(), modifiers.alt(), modifiers.shift(), modifiers.button(), popup);
    }

    public static String modifiersToString(Ctrl control, Alt alt, Shift shift,
                                           MouseButton button, boolean popup) {
        return modifiersToString(control.isDown(), alt.isDown(), shift.isDown(),
            button.isRight(), popup);
    }

    public static String modifiersToString(boolean ctrl, boolean alt, boolean shift,
                                           boolean right, boolean popup) {
        StringBuilder msg = new StringBuilder(25);
        if (ctrl) {
            msg.append(Ansi.red("ctrl-"));
        }
        if (alt) {
            msg.append(Ansi.green("alt-"));
        }
        if (shift) {
            msg.append(Ansi.blue("shift-"));
        }
        if (right) {
            msg.append(Ansi.yellow("right-"));
        }
        if (popup) {
            msg.append(Ansi.cyan("popup-"));
        }
        return msg.toString();
    }

    public static void debugImage(Image img) {
        debugImage(img, "Debug");
    }

    public static void debugImage(Image img, String name) {
        // make sure that this is called on the EDT
        if (calledOutsideEDT()) {
            EventQueue.invokeLater(() -> debugImage(img, name));
            return;
        }

        BufferedImage copy = ImageUtils.copyToBufferedImage(img);
        View previousView = Views.getActive();

        findCompByName(name).ifPresentOrElse(
            comp -> replaceImageInDebugComp(comp, copy),
            () -> addNew(copy, null, name));

        if (previousView != null) {
            Views.activate(previousView);
        }
    }

    // Color's toString doesn't include the alpha
    public static String debugColor(Color c) {
        return String.format("r=%d,g=%d,b=%d,a=%d",
            c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }

    private static void replaceImageInDebugComp(Composition comp, BufferedImage newImg) {
        Canvas canvas = comp.getCanvas();
        comp.getActiveDrawableOrThrow().setImage(newImg);

        if (canvas.hasDifferentSizeThan(newImg)) {
            canvas.resize(newImg.getWidth(), newImg.getHeight(), comp.getView(), true);
        }

        comp.repaint();
    }

    public static void debugShape(Shape shape, String name) {
        // create a copy
        Path2D shapeCopy = new Path2D.Double(shape);

        Rectangle shapeBounds = shape.getBounds();
        int imgWidth = shapeBounds.x + shapeBounds.width + 50;
        int imgHeight = shapeBounds.y + shapeBounds.height + 50;

        BufferedImage debugImg = ImageUtils.createSysCompatibleImage(imgWidth, imgHeight);

        Graphics2D g = debugImg.createGraphics();
        Colors.fillWith(Color.WHITE, g, imgWidth, imgHeight);
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(3));
        g.draw(shapeCopy);
        g.dispose();

        debugImage(debugImg, name);
    }

    public static void debugRaster(Raster raster, String name) {
        ColorModel colorModel = switch (raster.getNumBands()) {
            // normal color image
            case 4 -> new DirectColorModel(
                ColorSpace.getInstance(ColorSpace.CS_sRGB), 32,
                0x00_FF_00_00, 0x00_00_FF_00, 0x00_00_00_FF, 0xFF_00_00_00,
                true, DataBuffer.TYPE_INT);
            // grayscale image
            case 1 -> new ComponentColorModel(
                ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[]{8},
                false, true,
                Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
            default -> throw new IllegalStateException("numBands = " + raster.getNumBands());
        };

        Raster correctlyTranslated = raster.createChild(
            raster.getMinX(), raster.getMinY(),
            raster.getWidth(), raster.getHeight(),
            0, 0, null);
        BufferedImage debugImage = new BufferedImage(colorModel,
            (WritableRaster) correctlyTranslated, true, null);

        debugImage(debugImage, name);
    }

    public static void debugRasterWithEmptySpace(Raster raster) {
        // This image will include the minX/minY part of the Raster
        BufferedImage debugImage = new BufferedImage(
            raster.getMinX() + raster.getWidth(),
            raster.getMinY() + raster.getHeight(),
            BufferedImage.TYPE_4BYTE_ABGR_PRE);
        debugImage.setData(raster);

        debugImage(debugImage);
    }

    public static void keepSwitchingToolsRandomly() {
        Runnable backgroundTask = () -> {
            //noinspection InfiniteLoopStatement
            while (true) {
                Utils.sleep(1, TimeUnit.SECONDS);
                GUIUtils.invokeAndWait(() ->
                    Tools.getRandomTool().activate());
            }
        };
        new Thread(backgroundTask).start();
    }

    public static void sendKeyPress(PixelitorWindow pw, boolean ctrl, int keyCode, char keyChar) {
        int modifiers = 0;
        if (ctrl) {
            modifiers = Ctrl.PRESSED.modify(modifiers);
        }
        pw.dispatchEvent(new KeyEvent(pw, KeyEvent.KEY_PRESSED,
            System.currentTimeMillis(), modifiers, keyCode, keyChar));
    }

    public static void addTestPath() {
        var shape = new Rectangle2D.Double(100, 100, 300, 100);

        Path path = Shapes.shapeToPath(shape, Views.getActive());

        Views.getActiveComp().setActivePath(path);
        Tools.NODE.activate();
    }

    public static void showAddTextLayerDialog() {
        AddTextLayerAction.INSTANCE.actionPerformed(null);
    }

    public static void addMaskAndShowIt() {
        AddLayerMaskAction.INSTANCE.actionPerformed(null);
        View view = Views.getActive();
        Layer layer = view.getComp().getActiveLayer();
        MaskViewMode.VIEW_MASK.activate(view, layer);
    }

    public static void addNewImageWithMask() {
        NewImage.addNewImage(FillType.WHITE, 600, 400, "Test");
        Views.getActiveLayer().addMask(LayerMaskAddType.PATTERN);
    }

    public static void copyInternalState() {
        AppNode node = new AppNode();
        Utils.copyStringToClipboard(node.toJSON());
        Messages.showStatusMessage("Internal state copied to the clipboard.");
    }

    public static void showInternalState(String dialogTitle) {
        AppNode node = new AppNode();

        JTree tree = new JTree(node);

        JLabel explainLabel = new JLabel(
            "<html>If you are reporting a bug that cannot be reproduced," +
                "<br>please include the following information:");
        explainLabel.setBorder(createEmptyBorder(5, 5, 5, 5));

        JPanel form = new JPanel(new BorderLayout());
        form.add(explainLabel, NORTH);
        form.add(new JScrollPane(tree), CENTER);

        GUIUtils.showCopyTextToClipboardDialog(form,
            node::toJSON, dialogTitle);
    }

    public static void showTree(Debuggable debuggable, String name) {
        debuggable.createDebugNode(name).showInDialog("Internal State");
    }

    public static String pageFormatAsString(PageFormat pageFormat) {
        int orientation = pageFormat.getOrientation();
        String orientationString = switch (orientation) {
            case PageFormat.LANDSCAPE -> "Landscape";
            case PageFormat.PORTRAIT -> "Portrait";
            case PageFormat.REVERSE_LANDSCAPE -> "Reverse Landscape";
            default -> "Unexpected orientation " + orientation;
        };
        String paperString = paperAsString(pageFormat.getPaper());
        return "PageFormat[" + orientationString + ", " + paperString + "]";
    }

    public static String paperAsString(Paper paper) {
        return String.format("Paper[%.1fx%.1f, area = %.0f, %.0f, %.0f, %.0f]",
            paper.getWidth(), paper.getHeight(),
            paper.getImageableX(), paper.getImageableY(),
            paper.getImageableWidth(), paper.getImageableHeight());
    }

    public static void throwTestException() {
        if (AppMode.isDevelopment()) {
            throw new IllegalStateException("Test");
        }
    }

    public static void throwTestIOException() throws IOException {
        if (AppMode.isDevelopment()) {
            throw new IOException("Test");
        }
    }

    public static void throwTestError() {
        if (AppMode.isDevelopment()) {
            throw new AssertionError("Test");
        }
    }

    public static String debugJComponent(JComponent c) {
        return String.format("""
                size = %s
                preferredSize = %s
                minimumSize = %s
                maximumSize = %s
                insets = %s
                border = %s
                border insets = %s
                doubleBuffered = %s
                """,
            dimensionAsString(c.getSize()),
            dimensionAsString(c.getPreferredSize()),
            dimensionAsString(c.getMinimumSize()),
            dimensionAsString(c.getMaximumSize()),
            c.getInsets().toString(),
            c.getBorder().toString(),
            c.getBorder().getBorderInsets(c).toString(),
            c.isDoubleBuffered());
    }

    private static String dimensionAsString(Dimension d) {
        return d.width + "x" + d.height;
    }

    public static void serializeAllFilters() {
        Filters.forEachSmartFilter(Debug::serialize);
    }

    public static void deserializeAllFilters() {
        Filters.forEachSmartFilter(Debug::deserialize);
    }

    private static void serialize(Filter filter) {
        File out = createFileForSerializedFilter(filter);
        String path = out.getAbsolutePath();
        System.out.printf("SerTest::serialize: path = '%s'%n", path);
        try (FileOutputStream fos = new FileOutputStream(out)) {
            try (ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                oos.writeObject(filter);
            }
        } catch (IOException e) {
            Messages.showException(e);
        }
    }

    private static void deserialize(Filter filter) {
        File in = createFileForSerializedFilter(filter);
        String path = in.getAbsolutePath();
        System.out.printf("SerTest::deserialize: path = '%s'%n", path);

        try (FileInputStream fis = new FileInputStream(in)) {
            try (ObjectInputStream ois = new ObjectInputStream(fis)) {
                Object object = ois.readObject();
                if (object.getClass() != filter.getClass()) {
                    throw new IllegalStateException();
                }
            }
        } catch (Exception e) {
            Messages.showException(e);
        }
    }

    private static File createFileForSerializedFilter(Filter filter) {
        String fileName = FileUtils.sanitizeToFileName(filter.getName()) + ".ser";
        return new File("ser", fileName);
    }

    public static void debugAllDebugNames() {
        // make a copy, because this could add new views in the future
        List<View> origViews = List.copyOf(Views.getAll());
        for (View view : origViews) {
            String debugName = view.getComp().getDebugName();
            System.out.println("Debug::debugAllImages: debugName = " + debugName);
        }
    }

    public static void addAllSmartFilters(Composition comp) {
        Filters.forEachSmartFilter(filter ->
            addSmartFilter(comp, filter));
    }

    private static void addSmartFilter(Composition comp, Filter filter) {
        TextSettings settings = new TextSettings();
        settings.randomize();
        settings.setText(filter.getName());
        settings.setWatermark(false);
        TextLayer textLayer = new TextLayer(comp, "", settings);
        textLayer.updateLayerName();

        SmartObject smartObject = new SmartObject(textLayer);
        smartObject.setOpacity(0.2f, false, true);
        comp.add(smartObject);
        SmartFilter sf = new SmartFilter(filter, smartObject.getContent(), smartObject);
        smartObject.addSmartFilter(sf, false, true);

        smartObject.updateIconImage();
    }

    public static String mouseEventAsString(MouseEvent e) {
        String action = switch (e.getID()) {
            case MouseEvent.MOUSE_CLICKED -> "CLICKED";
            case MouseEvent.MOUSE_PRESSED -> "PRESSED";
            case MouseEvent.MOUSE_RELEASED -> "RELEASED";
            case MouseEvent.MOUSE_DRAGGED -> "DRAGGED";
            case MouseEvent.MOUSE_MOVED -> "MOVED";
            case MouseEvent.MOUSE_WHEEL -> "WHEEL";
            case MouseEvent.MOUSE_ENTERED -> "ENTERED";
            case MouseEvent.MOUSE_EXITED -> "EXITED";
            default -> throw new IllegalStateException("Unexpected value: " + e.getID());
        };
        return modifiersToString(e) + action + String.format(" at (%d, %d), click count = %d, c = %s",
            e.getX(), e.getY(), e.getClickCount(),
            debugComponent(e.getComponent()));
    }

    private static String debugComponent(Component c) {
        String descr = c.getClass().getSimpleName();
        if (c instanceof View) {
            descr += "(name = " + c.getName() + ")";
        } else if (c instanceof ToolButton b) {
            descr += "(name = " + b.getTool().getName() + ")";
        }

        return descr;
    }

    public static String mlpAlignmentToString(int alignment) {
        return switch (alignment) {
            case AlignmentSelector.LEFT -> "left";
            case AlignmentSelector.CENTER -> "center";
            case AlignmentSelector.RIGHT -> "right";
            default -> throw new IllegalStateException("Unexpected value: " + alignment);
        };
    }
}
