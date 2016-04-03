/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import pixelitor.AppLogic;
import pixelitor.Build;
import pixelitor.Composition;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.FilterSetting;
import pixelitor.gui.BlendingModePanel;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static java.awt.image.BufferedImage.TYPE_4BYTE_ABGR_PRE;

public final class Utils {
    private static final int BYTES_IN_1_MEGABYTE = 1048576;
    private static final int BYTES_IN_1_KILOBYTE = 1024;

    private static final Cursor BUSY_CURSOR = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    private static final Cursor DEFAULT_CURSOR = Cursor.getDefaultCursor();
    private static final int WAIT_CURSOR_DELAY = 300; // in milliseconds

    /**
     * Utility class with static methods
     */
    private Utils() {
    }

    public static void executeWithBusyCursor(Runnable task) {
        executeWithBusyCursor(PixelitorWindow.getInstance(), task);
    }

    /**
     * Executes a task with busy cursor
     */
    public static void executeWithBusyCursor(Component parent, Runnable task) {
        Timer timer = new Timer();
        TimerTask startBusyCursorTask = new TimerTask() {
            @Override
            public void run() {
                parent.setCursor(BUSY_CURSOR);
            }
        };

        try {
            // if after WAIT_CURSOR_DELAY the original task is still running,
            // set the cursor to the delay cursor
            timer.schedule(startBusyCursorTask, WAIT_CURSOR_DELAY);
            task.run(); // on the current thread!
        } finally {
            // when the original task has stopped running, the cursor is reset
            timer.cancel();
            parent.setCursor(DEFAULT_CURSOR);
        }
    }

    public static void setShowOriginal(boolean b) {
        Composition comp = ImageComponents.getActiveCompOrNull();
        comp.getActiveMaskOrImageLayer().setShowOriginal(b);
    }

    /**
     * Replaces all the special characters in s string with an underscore
     */
    public static String toFileName(String s) {
        return s.replaceAll("[^A-Za-z0-9_]", "_");
    }

    public static void randomizeGUIWidgetsOn(JPanel panel) {
        int count = panel.getComponentCount();
        Random rand = new Random();

        for (int i = 0; i < count; i++) {
            Component child = panel.getComponent(i);
            //noinspection ChainOfInstanceofChecks
            if (child instanceof JComboBox) {
                @SuppressWarnings("rawtypes")
                JComboBox box = (JComboBox) child;

                int itemCount = box.getItemCount();
                box.setSelectedIndex(rand.nextInt(itemCount));
            } else if (child instanceof JCheckBox) {
                JCheckBox box = (JCheckBox) child;
                box.setSelected(rand.nextBoolean());
            } else if (child instanceof SliderSpinner) {
                SliderSpinner spinner = (SliderSpinner) child;
                spinner.getModel().randomize();
            } else if (child instanceof BlendingModePanel) {
                BlendingModePanel bmp = (BlendingModePanel) child;
                bmp.randomize();
            }
        }
    }

    public static String float2String(float f) {
        if (f == 0.0f) {
            return "";
        }
        return String.format("%.3f", f);
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
                // second chance: english
                number = englishFormat.parse(trimmed);
            } catch (ParseException e1) {
                throw new NotANumberException(s);
            }
        }
        return number.floatValue();
    }

    public static void throwTestException() {
        if (Build.CURRENT != Build.FINAL) {
            throw new IllegalStateException("Test");
        }
    }

    public static String bytesToString(int bytes) {
        if (bytes < BYTES_IN_1_KILOBYTE) {
            return bytes + " bytes";
        } else if (bytes < BYTES_IN_1_MEGABYTE) {
            float kiloBytes = ((float) bytes) / BYTES_IN_1_KILOBYTE;
            return String.format("%.2f kilobytes", kiloBytes);
        } else {
            float megaBytes = ((float) bytes) / BYTES_IN_1_MEGABYTE;
            return String.format("%.2f megabytes", megaBytes);
        }
    }

    public static int getMaxHeapInMegabytes() {
        long heapMaxSize = Runtime.getRuntime().maxMemory();
        int sizeInMegaBytes = (int) (heapMaxSize / BYTES_IN_1_MEGABYTE);
        return sizeInMegaBytes;
    }

    public static int getUsedMemoryInMegabytes() {
        long usedMemory = Runtime.getRuntime().totalMemory();
        int sizeInMegaBytes = (int) (usedMemory / BYTES_IN_1_MEGABYTE);
        return sizeInMegaBytes;
    }

    @SuppressWarnings("SameReturnValue")  // used in asserts
    public static boolean checkRasterMinimum(BufferedImage newImage) {
        if (RandomGUITest.isRunning()) {
            WritableRaster raster = newImage.getRaster();
            if ((raster.getMinX() != 0) || (raster.getMinY() != 0)) {
                throw new
                        IllegalArgumentException("Raster " + raster +
                        " has minX or minY not equal to zero: "
                        + raster.getMinX() + ' ' + raster.getMinY());
            }
        }
        return true;
    }

    public static void copyStringToClipboard(String text) {
        Transferable stringSelection = new StringSelection(text);

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    public static ProgressMonitor createPercentageProgressMonitor(String message) {
        return new ProgressMonitor(PixelitorWindow.getInstance(), message, "", 0, 100);
    }

    public static ProgressMonitor createPercentageProgressMonitor(String message, String cancelButtonText) {
        String oldText = UIManager.getString("OptionPane.cancelButtonText");
        UIManager.put("OptionPane.cancelButtonText", cancelButtonText);
        ProgressMonitor pm = new ProgressMonitor(PixelitorWindow.getInstance(), message, "", 0, 100);
        UIManager.put("OptionPane.cancelButtonText", oldText);
        return pm;
    }

    public static double transformAtan2AngleToIntuitive(double angleInRadians) {
        double angle;
        if (angleInRadians <= 0) {
            angle = -angleInRadians;
        } else {
            angle = Math.PI * 2 - angleInRadians;
        }
        return angle;
    }

    public static Point2D calculateOffset(double distance, double angle) {
        double offsetX = distance * Math.cos(angle);
        double offsetY = distance * Math.sin(angle);

        return new Point2D.Double(offsetX, offsetY);
    }

    // makes sure that the returned rectangle has positive width, height
    public static Rectangle toPositiveRect(int x1, int x2, int y1, int y2) {
        int topX, topY, width, height;

        if (x2 >= x1) {
            topX = x1;
            width = x2 - x1;
        } else {
            topX = x2;
            width = x1 - x2;
        }

        if (y2 >= y1) {
            topY = y1;
            height = y2 - y1;
        } else {
            topY = y2;
            height = y1 - y2;
        }

        Rectangle retVal = new Rectangle(topX, topY, width, height);
        return retVal;
    }

    // makes sure that the returned rectangle has positive width, height
    public static Rectangle2D toPositiveRect(Rectangle2D input) {
        double inX = input.getX();
        double inY = input.getY();
        double inWidth = input.getWidth();
        double inHeight = input.getHeight();

        if (inWidth >= 0) {
            if (inHeight >= 0) {
                return input; // should be the most common case
            } else { // negative height
                double newY = inY + inHeight;
                return new Rectangle2D.Double(inX, newY, inWidth, -inHeight);
            }
        } else { // negative width
            if (inHeight >= 0) {
                double newX = inX + inWidth;
                return new Rectangle2D.Double(newX, inY, -inWidth, inHeight);
            } else { // negative height
                double newX = inX + inWidth;
                double newY = inY + inHeight;
                return new Rectangle2D.Double(newX, newY, -inWidth, -inHeight);
            }
        }
    }

    public static float parseFloat(String input, float defaultValue) {
        if ((input != null) && !input.isEmpty()) {
            return Float.parseFloat(input);
        }
        return defaultValue;
    }

    public static int parseInt(String input, int defaultValue) {
        if ((input != null) && !input.isEmpty()) {
            return Integer.parseInt(input);
        }
        return defaultValue;
    }

    @SuppressWarnings("WeakerAccess")
    public static void debugImage(BufferedImage img) {
        debugImage(img, "Debug");
    }

    @SuppressWarnings("WeakerAccess")
    public static void debugImage(BufferedImage img, String name) {
        BufferedImage copy = ImageUtils.copyImage(img);
        ImageComponent savedIC = ImageComponents.getActiveIC();

        Optional<Composition> debugCompOpt = ImageComponents.findCompositionByName(name);
        if (debugCompOpt.isPresent()) {
            // if we already have a debug composition, simply replace the image
            Composition comp = debugCompOpt.get();
            comp.getActiveMaskOrImageLayer().setImage(copy);
            comp.repaint();
        } else {
            Composition comp = Composition.fromImage(copy, null, name);
            AppLogic.addComposition(comp);
        }

        if (savedIC != null) {
            ImageComponents.setActiveIC(savedIC, true);
        }
    }

    public static void debugShape(Shape shape, String name) {
        // create a copy
        Shape shapeCopy = new Path2D.Double(shape);

        Rectangle shapeBounds = shape.getBounds();
        int imgWidth = shapeBounds.x + shapeBounds.width + 50;
        int imgHeight = shapeBounds.y + shapeBounds.height + 50;
        BufferedImage img = ImageUtils.createSysCompatibleImage(imgWidth, imgHeight);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, imgWidth, imgHeight);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(3));
        g.draw(shapeCopy);
        g.dispose();
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

        Raster correctlyTranslated = raster.createChild(raster.getMinX(), raster.getMinY(), raster.getWidth(), raster.getHeight(), 0, 0, null);
        BufferedImage debugImage = new BufferedImage(colorModel, (WritableRaster) correctlyTranslated, true, null);
        debugImage(debugImage, name);
    }

    public static void debugRasterWithEmptySpace(Raster raster) {
        BufferedImage debugImage = new BufferedImage(raster.getMinX() + raster.getWidth(), raster.getMinY() + raster.getHeight(), TYPE_4BYTE_ABGR_PRE);
        debugImage.setData(raster);
        debugImage(debugImage);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public static GeneralPath createUnitArrow() {
        float arrowWidth = 0.3f;
        float arrowHeadWidth = 0.7f;
        float arrowHeadStart = 0.6f;

        float halfArrowWidth = arrowWidth / 2.0f;
        float halfArrowHeadWidth = arrowHeadWidth / 2;

        GeneralPath unitArrow = new GeneralPath();
        unitArrow.moveTo(0.0f, -halfArrowWidth);
        unitArrow.lineTo(0.0f, halfArrowWidth);
        unitArrow.lineTo(arrowHeadStart, halfArrowWidth);
        unitArrow.lineTo(arrowHeadStart, halfArrowHeadWidth);
        unitArrow.lineTo(1.0f, 0.0f);
        unitArrow.lineTo(arrowHeadStart, -halfArrowHeadWidth);
        unitArrow.lineTo(arrowHeadStart, -halfArrowWidth);
        unitArrow.closePath();
        return unitArrow;
    }

    public static String getRandomString(int length) {
        char[] chars = "abcdefghijklmnopqrstuvwxyz -".toCharArray();
        StringBuilder sb = new StringBuilder(length);
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        String output = sb.toString();
        return output;
    }

    public static void checkThatAssertionsAreEnabled() {
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
            s = KeyEvent.getKeyModifiersText(modifiers);
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
        return String.format("%d:%02d:%02d", h, m, s);
    }

    public static boolean callingClassIs(String name) {
        // designed to be used in assertions:
        // it checks the caller of the caller
        String callingClassName = new Exception().getStackTrace()[2].getClassName();
        return callingClassName.contains(name);
    }

    public static <T> void setupDisableOtherIf(ComboBoxModel<T> current, FilterSetting other, Predicate<T> condition) {
        current.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                if (condition.test((T) current.getSelectedItem())) {
                    other.setEnabled(false, FilterSetting.EnabledReason.APP_LOGIC);
                } else {
                    other.setEnabled(true, FilterSetting.EnabledReason.APP_LOGIC);
                }
            }
        });
    }

    public static <T> void setupEnableOtherIf(ComboBoxModel<T> current, FilterSetting other, Predicate<T> condition) {
        other.setEnabled(false, FilterSetting.EnabledReason.APP_LOGIC);
        current.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                if (condition.test((T) current.getSelectedItem())) {
                    other.setEnabled(true, FilterSetting.EnabledReason.APP_LOGIC);
                } else {
                    other.setEnabled(false, FilterSetting.EnabledReason.APP_LOGIC);
                }
            }
        });
    }

    public static void setupDisableOtherIf(BooleanParam current, FilterSetting other, Predicate<Boolean> condition) {
        current.addChangeListener(e -> {
            if (condition.test(current.isChecked())) {
                other.setEnabled(false, FilterSetting.EnabledReason.APP_LOGIC);
            } else {
                other.setEnabled(true, FilterSetting.EnabledReason.APP_LOGIC);
            }
        });
    }

    public static void setupEnableOtherIf(BooleanParam current, FilterSetting other, Predicate<Boolean> condition) {
        other.setEnabled(false, FilterSetting.EnabledReason.APP_LOGIC);
        current.addChangeListener(e -> {
            if (condition.test(current.isChecked())) {
                other.setEnabled(true, FilterSetting.EnabledReason.APP_LOGIC);
            } else {
                other.setEnabled(false, FilterSetting.EnabledReason.APP_LOGIC);
            }
        });
    }

    /**
     * Creates the name of the duplicated layers and compositions
     */
    public static String createCopyName(String orig) {
        String copyString = "copy"; // could be longer or shorter in other languages
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
}
