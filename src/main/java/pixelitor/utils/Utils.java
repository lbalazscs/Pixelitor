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

package pixelitor.utils;

import net.jafama.FastMath;
import pixelitor.Build;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.gui.View;
import pixelitor.utils.debug.Ansi;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.VolatileImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.awt.image.BufferedImage.TYPE_4BYTE_ABGR_PRE;
import static java.lang.String.format;
import static pixelitor.OpenImages.addAsNewComp;
import static pixelitor.OpenImages.findCompByName;

/**
 * Utility class with static methods
 */
public final class Utils {
    private static final int BYTES_IN_1_KILOBYTE = 1_024;
    private static final int BYTES_IN_1_MEGABYTE = 1_048_576;
    private static final CompletableFuture<?>[] EMPTY_CF_ARRAY = new CompletableFuture<?>[0];

    private Utils() {
    }

    /**
     * Replaces all the special characters in s string with an underscore
     */
    public static String toFileName(String s) {
        return s.replaceAll("[^A-Za-z0-9_]", "_");
    }

    public static String float2String(float f) {
        if (f == 0.0f) {
            return "";
        }
        return format("%.3f", f);
    }

    public static float string2float(String s) throws NotANumberException {
        String trimmed = s.trim();
        if (trimmed.isEmpty()) {
            return 0.0f;
        }

        NumberFormat nf = NumberFormat.getInstance();
        Number number;
        try {
            // try locale-specific parsing
            number = nf.parse(trimmed);
        } catch (ParseException e) {
            NumberFormat englishFormat = NumberFormat.getInstance(Locale.ENGLISH);
            try {
                // second chance: English
                number = englishFormat.parse(trimmed);
            } catch (ParseException e1) {
                throw new NotANumberException(s);
            }
        }
        return number.floatValue();
    }

    public static void throwTestException() {
        if (Build.isDevelopment()) {
            throw new IllegalStateException("Test");
        }
    }

    public static void throwTestIOException() throws IOException {
        if (Build.isDevelopment()) {
            throw new IOException("Test");
        }
    }

    public static void throwTestError() {
        if (Build.isDevelopment()) {
            throw new AssertionError("Test");
        }
    }

    public static String bytesToString(int bytes) {
        if (bytes < BYTES_IN_1_KILOBYTE) {
            return bytes + " bytes";
        } else if (bytes < BYTES_IN_1_MEGABYTE) {
            float kiloBytes = ((float) bytes) / BYTES_IN_1_KILOBYTE;
            return format("%.2f kilobytes", kiloBytes);
        } else {
            float megaBytes = ((float) bytes) / BYTES_IN_1_MEGABYTE;
            return format("%.2f megabytes", megaBytes);
        }
    }

    public static int getMaxHeapInMegabytes() {
        long heapMaxSize = Runtime.getRuntime().maxMemory();
        return (int) (heapMaxSize / BYTES_IN_1_MEGABYTE);
    }

    public static int getUsedMemoryInMegabytes() {
        long usedMemory = Runtime.getRuntime().totalMemory();
        return (int) (usedMemory / BYTES_IN_1_MEGABYTE);
    }

    public static void copyStringToClipboard(String text) {
        Transferable stringSelection = new StringSelection(text);

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    /**
     * Input: an angle between -PI and PI, as returned form Math.atan2
     * Output: an angle between 0 and 2*PI, and in the intuitive direction
     */
    public static double atan2AngleToIntuitive(double angleInRadians) {
        double angle;
        if (angleInRadians <= 0) {
            angle = -angleInRadians;
        } else {
            angle = Math.PI * 2 - angleInRadians;
        }
        return angle;
    }

    /**
     * The inverse function of atan2AngleToIntuitive
     */
    public static double intuitiveToAtan2Angle(double radians) {
        if (radians > Math.PI) {
            return 2 * Math.PI - radians;
        } else {
            return -radians;
        }
    }

    public static Point2D offsetFromPolar(double distance, double angle) {
        double offsetX = distance * FastMath.cos(angle);
        double offsetY = distance * FastMath.sin(angle);

        return new Point.Double(offsetX, offsetY);
    }

    public static float parseFloat(String input, float defaultValue) {
        if ((input != null) && !input.isEmpty()) {
            return Float.parseFloat(input);
        }
        return defaultValue;
    }

    public static int parseInt(String input, int defaultValue) {
        if (input != null) {
            input = input.trim();
            if (!input.isEmpty()) {
                return Integer.parseInt(input);
            }
        }
        return defaultValue;
    }

    public static void debugImage(Image img) {
        debugImage(img, "Debug");
    }

    public static void debugImage(Image img, String name) {
        // make sure the this is called on the EDT
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(() -> debugImage(img, name));
            return;
        }

        BufferedImage copy = copyToBufferedImage(img);

        View previousView = OpenImages.getActiveView();

        findCompByName(name).ifPresentOrElse(
                comp -> replaceImageInDebugComp(comp, copy),
                () -> addAsNewComp(copy, null, name));

        if (previousView != null) {
            OpenImages.setActiveView(previousView, true);
        }
    }

    private static BufferedImage copyToBufferedImage(Image img) {
        BufferedImage copy;
        if (img instanceof BufferedImage) {
            BufferedImage bufferedImage = (BufferedImage) img;
            if (bufferedImage.getColorModel() instanceof IndexColorModel) {
                copy = ImageUtils.convertToARGB(bufferedImage, false);
            } else {
                copy = ImageUtils.copyImage(bufferedImage);
            }
        } else if (img instanceof VolatileImage) {
            VolatileImage volatileImage = (VolatileImage) img;
            copy = volatileImage.getSnapshot();
        } else {
            throw new UnsupportedOperationException("img class is " + img.getClass().getName());
        }
        return copy;
    }

    private static void replaceImageInDebugComp(Composition comp, BufferedImage copy) {
        Canvas canvas = comp.getCanvas();
        comp.getActiveDrawableOrThrow().setImage(copy);
        if (canvas.getImWidth() != copy.getWidth()
                || canvas.getImHeight() != copy.getHeight()) {
            canvas.changeImSize(copy.getWidth(), copy.getHeight(), comp.getView());
        }

        comp.repaint();
    }

    public static void debugShape(Shape shape, String name) {
        // create a copy
        Path2D shapeCopy = new Path2D.Double(shape);

        Rectangle shapeBounds = shape.getBounds();
        int imgWidth = shapeBounds.x + shapeBounds.width + 50;
        int imgHeight = shapeBounds.y + shapeBounds.height + 50;
        BufferedImage img = ImageUtils.createSysCompatibleImage(imgWidth, imgHeight);
        Drawer.on(img)
                .fillWith(Color.WHITE)
                .useAA()
                .draw(g -> {
                    g.setColor(Color.BLACK);
                    g.setStroke(new BasicStroke(3));
                    g.draw(shapeCopy);
                });
        debugImage(img, name);
    }

    public static void debugRaster(Raster raster, String name) {
        ColorModel colorModel;
        int numBands = raster.getNumBands();

        if (numBands == 4) { // normal color image
            colorModel = new DirectColorModel(
                    ColorSpace.getInstance(ColorSpace.CS_sRGB),
                    32,
                    0x00ff0000,// Red
                    0x0000ff00,// Green
                    0x000000ff,// Blue
                    0xff000000,// Alpha
                    true,       // Alpha Premultiplied
                    DataBuffer.TYPE_INT
            );
        } else if (numBands == 1) { // grayscale image
            ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
            int[] nBits = {8};
            colorModel = new ComponentColorModel(cs, nBits, false, true,
                    Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        } else {
            throw new IllegalStateException("numBands = " + numBands);
        }

        Raster correctlyTranslated = raster.createChild(
                raster.getMinX(), raster.getMinY(),
                raster.getWidth(), raster.getHeight(),
                0, 0, null);
        BufferedImage debugImage = new BufferedImage(colorModel,
                (WritableRaster) correctlyTranslated, true, null);
        debugImage(debugImage, name);
    }

    public static void debugRasterWithEmptySpace(Raster raster) {
        BufferedImage debugImage = new BufferedImage(
                raster.getMinX() + raster.getWidth(),
                raster.getMinY() + raster.getHeight(),
                TYPE_4BYTE_ABGR_PRE);
        debugImage.setData(raster);
        debugImage(debugImage);
    }

    public static void makeSureAssertionsAreEnabled() {
        boolean assertsEnabled = false;
        //noinspection AssertWithSideEffects
        assert assertsEnabled = true;
        if (!assertsEnabled) {
            throw new IllegalStateException("assertions not enabled");
        }
    }

    @VisibleForTesting
    public static void sleep(int duration, TimeUnit unit) {
        try {
            Thread.sleep(unit.toMillis(duration));
        } catch (InterruptedException e) {
            throw new IllegalStateException("interrupted!");
        }
    }

    public static String keystrokeAsText(KeyStroke keyStroke) {
        String s = "";
        int modifiers = keyStroke.getModifiers();
        if (modifiers > 0) {
            s = InputEvent.getModifiersExText(modifiers);
            s += " ";
        }
        int keyCode = keyStroke.getKeyCode();
        if (keyCode != 0) {
            s += KeyEvent.getKeyText(keyCode);
        } else {
            s += keyStroke.getKeyChar();
        }

        return s;
    }

    public static String formatMillis(long millis) {
        long seconds = millis / 1000;
        long s = seconds % 60;
        long m = (seconds / 60) % 60;
        long h = (seconds / (60 * 60)) % 24;
        return format("%d:%02d:%02d", h, m, s);
    }

    /**
     * Creates the name of the duplicated layers and compositions
     */
    public static String createCopyName(String orig) {
        String copyString = "copy";

        // could be longer or shorter in other languages
        int copyStringLength = copyString.length();

        int index = orig.lastIndexOf(copyString);
        if (index == -1) {
            return orig + ' ' + copyString;
        }
        if (index == orig.length() - copyStringLength) {
            // it ends with the copyString - this was the first copy
            return orig + " 2";
        }
        String afterCopyString = orig.substring(index + copyStringLength);

        int copyNr;
        try {
            copyNr = Integer.parseInt(afterCopyString.trim());
        } catch (NumberFormatException e) {
            // the part after copy was not a number...
            return orig + ' ' + copyString;
        }

        copyNr++;

        return orig.substring(0, index + copyStringLength) + ' ' + copyNr;
    }

    /**
     * Quick allMatch for arrays (without creating Streams)
     */
    public static <T> boolean allMatch(T[] array, Predicate<? super T> predicate) {
        for (T element : array) {
            if (!predicate.test(element)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Quick allMatch for lists (without creating Streams)
     */
    public static <T> boolean allMatch(List<T> list, Predicate<? super T> predicate) {
        for (T element : list) {
            if (!predicate.test(element)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Quick anyMatch for arrays (without creating Streams)
     */
    public static <T> boolean anyMatch(T[] array, Predicate<? super T> predicate) {
        for (T element : array) {
            if (predicate.test(element)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Quick anyMatch for lists (without creating Streams)
     */
    public static <T> boolean anyMatch(List<T> list, Predicate<? super T> predicate) {
        for (T element : list) {
            if (predicate.test(element)) {
                return true;
            }
        }
        return false;
    }

    public static int getJavaMainVersion() {
        return Runtime.Version.parse(System.getProperty("java.version")).feature();
    }

    public static Point2D constrainEndPoint(double relX, double relY, double mouseX, double mouseY) {
        double dx = mouseX - relX;
        double dy = mouseY - relY;

        double adx = Math.abs(dx);
        double ady = Math.abs(dy);

        if (adx > 2 * ady) {
            mouseY = relY; // constrain to 0 or 180 degrees
        } else if (ady > 2 * adx) {
            mouseX = relX; // constrain to 90 or 270 degrees
        } else {
            if (dx > 0) {
                if (dy > 0) {
                    double avg = (dx + dy) / 2.0;
                    mouseX = relX + avg;
                    mouseY = relY + avg; // 315 degrees
                } else {
                    double avg = (dx - dy) / 2.0;
                    mouseX = relX + avg;
                    mouseY = relY - avg; // 45 degrees
                }
            } else { // dx <= 0
                if (dy > 0) {
                    double avg = (-dx + dy) / 2.0;
                    mouseX = relX - avg;
                    mouseY = relY + avg; // 225 degrees
                } else {
                    double avg = (-dx - dy) / 2.0;
                    mouseX = relX - avg;
                    mouseY = relY - avg; // 135 degrees
                }
            }
        }
        return new Point2D.Double(mouseX, mouseY);
    }

    /**
     * Transforms a Callable into a Supplier by wrapping
     * the checked exceptions in runtime exceptions
     */
    public static <T> Supplier<T> toSupplier(Callable<T> callable) {
        return () -> {
            try {
                return callable.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static void preloadFontNames() {
        // the results are cached, no need to cache them here
        getAvailableFontNames();
    }

    public static String[] getAvailableFontNames() {
        return GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
    }

    /**
     * Returns a new CompletableFuture that is completed when all of the
     * CompletableFutures in the given list complete
     */
    public static <T> CompletableFuture<Void> allOfList(List<CompletableFuture<?>> list) {
        return CompletableFuture.allOf(list.toArray(EMPTY_CF_ARRAY));
    }

    public static String debugMouseModifiers(MouseEvent e) {
        boolean altDown = e.isAltDown();
        boolean controlDown = e.isControlDown();
        boolean shiftDown = e.isShiftDown();
        boolean rightMouse = SwingUtilities.isRightMouseButton(e);
        StringBuilder msg = new StringBuilder(25);
        if (controlDown) {
            msg.append(Ansi.red("ctrl-"));
        }
        if (altDown) {
            msg.append(Ansi.green("alt-"));
        }
        if (shiftDown) {
            msg.append(Ansi.blue("shift-"));
        }
        if (rightMouse) {
            msg.append(Ansi.yellow("right-"));
        }
        if (e.isPopupTrigger()) {
            msg.append(Ansi.cyan("popup-"));
        }
        return msg.toString();
    }

    public static void debugCall(String... args) {
        debugCall(false, args);
    }

    public static void debugCallAndCaller(String... args) {
        debugCall(true, args);
    }

    private static void debugCall(boolean printCaller, String... args) {
        StackTraceElement[] fullTrace = new Throwable().getStackTrace();
        StackTraceElement me = fullTrace[2];

        String threadName;
        if (EventQueue.isDispatchThread()) {
            threadName = "EDT";
        } else {
            threadName = Thread.currentThread().getName();
        }

        String argsAsOneString;
        if (args.length == 0) {
            argsAsOneString = "";
        } else if (args.length == 1) {
            argsAsOneString = args[0];
        } else {
            argsAsOneString = Arrays.toString(args);
        }

        String className = me.getClassName();
        // strip the package name
        className = className.substring(className.lastIndexOf('.') + 1);

        if (printCaller) {
            StackTraceElement caller = fullTrace[3];
            String callerClassName = caller.getClassName();
            callerClassName = callerClassName.substring(callerClassName.lastIndexOf('.') + 1);

            System.out.printf("%s.%s(%s) on %s by %s.%s\n"
                    , className
                    , me.getMethodName()
                    , Ansi.yellow(argsAsOneString)
                    , threadName
                    , callerClassName
                    , caller.getMethodName());
        } else {
            System.out.printf("%s.%s(%s) on %s\n"
                    , className
                    , me.getMethodName()
                    , Ansi.yellow(argsAsOneString)
                    , threadName);
        }

    }

    public static String debugKeyEvent(KeyEvent e) {
        String keyText = KeyEvent.getKeyText(e.getKeyCode());
        int modifiers = e.getModifiersEx();
        if (modifiers != 0) {
            String modifiersText = InputEvent.getModifiersExText(modifiers);
            if (keyText.equals(modifiersText)) { // the key itself is the modifier
                return modifiersText;
            }
            return modifiersText + "+" + keyText;
        }
        return keyText;
    }

    public static double parseDouble(String s) throws NumberFormatException {
        // don't accept strings that end with an 'f' or 'd',
        // which are accepted by Double.parseDouble(s)
        if (s.indexOf('f') != -1) {
            throw new NumberFormatException();
        }
        if (s.indexOf('F') != -1) {
            throw new NumberFormatException();
        }
        if (s.indexOf('d') != -1) {
            throw new NumberFormatException();
        }
        if (s.indexOf('D') != -1) {
            throw new NumberFormatException();
        }
        return Double.parseDouble(s);
    }
}

