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

package pixelitor.utils;

import net.jafama.FastMath;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.Layer;

import javax.swing.*;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.lang.String.format;

/**
 * Utility class with static methods
 */
public final class Utils {
    private static final int NUM_BYTES_IN_KILOBYTE = 1_024;
    public static final int NUM_BYTES_IN_MEGABYTE = 1_048_576;
    private static final CompletableFuture<?>[] EMPTY_CF_ARRAY = new CompletableFuture<?>[0];

    private static String[] fontNames;
    private static final CountDownLatch fontNamesLoaded = new CountDownLatch(1);

    private Utils() {
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

    public static String bytesToString(int bytes) {
        if (bytes < NUM_BYTES_IN_KILOBYTE) {
            return bytes + " bytes";
        } else if (bytes < NUM_BYTES_IN_MEGABYTE) {
            float kiloBytes = ((float) bytes) / NUM_BYTES_IN_KILOBYTE;
            return format("%.2f kilobytes", kiloBytes);
        } else {
            float megaBytes = ((float) bytes) / NUM_BYTES_IN_MEGABYTE;
            return format("%.2f megabytes", megaBytes);
        }
    }

    public static int getMaxHeapMb() {
        long heapMaxSize = Runtime.getRuntime().maxMemory();
        return (int) (heapMaxSize / NUM_BYTES_IN_MEGABYTE);
    }

    public static int getUsedMemoryMb() {
        long usedMemory = Runtime.getRuntime().totalMemory();
        return (int) (usedMemory / NUM_BYTES_IN_MEGABYTE);
    }

    public static void copyStringToClipboard(String text) {
        Toolkit.getDefaultToolkit()
            .getSystemClipboard()
            .setContents(new StringSelection(text), null);
    }

    /**
     * Input: an angle between -PI and PI, as returned form Math.atan2
     * Output: an angle between 0 and 2*PI, and in the intuitive
     * (counter-clockwise) direction
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

    public static double toIntuitiveDegrees(double angle) {
        double degrees = Math.toDegrees(angle);
        if (degrees <= 0) {
            degrees = -degrees;
        } else {
            degrees = 360.0 - degrees;
        }
        return degrees;
    }

    public static Point2D offsetFromPolar(double distance, double angle) {
        double offsetX = distance * FastMath.cos(angle);
        double offsetY = distance * FastMath.sin(angle);

        return new Point2D.Double(offsetX, offsetY);
    }

    public static double clamp01(double d) {
        if (d < 0) {
            return 0;
        }
        if (d > 1) {
            return 1;
        }
        return d;
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

    public static void makeSureAssertionsAreEnabled() {
        boolean assertsEnabled = false;
        //noinspection AssertWithSideEffects
        assert assertsEnabled = true;
        if (!assertsEnabled) {
            throw new IllegalStateException("assertions not enabled");
        }
    }

    public static void sleep(int duration, TimeUnit unit) {
        try {
            Thread.sleep(unit.toMillis(duration));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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

    public static String keyEventAsText(KeyEvent e) {
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

    public static String formatMillis(long millis) {
        long seconds = millis / 1000;
        long s = seconds % 60;
        long m = (seconds / 60) % 60;
        long h = (seconds / (60 * 60)) % 24;

        return format("%d:%02d:%02d", h, m, s);
    }

    /**
     * Creates names of type "something copy", "something copy 2"
     */
    public static String createCopyName(String orig) {
        String copyString = "copy";

        // could be longer or shorter in other languages
        int copyStringLength = copyString.length();

        int index = orig.lastIndexOf(copyString);
        if (index == -1) {
            // "name" => "name copy"
            return orig + ' ' + copyString;
        }
        if (index == orig.length() - copyStringLength) {
            // "name copy" => "name copy 2"
            return orig + " 2";
        }

        // "name copy x" => "name copy y", where y = x + 1
        String afterCopyPart = orig.substring(index + copyStringLength);

        int copyNr;
        try {
            copyNr = Integer.parseInt(afterCopyPart.trim());
        } catch (NumberFormatException e) {
            // the part after copy was not a number...
            return orig + ' ' + copyString;
        }
        copyNr++;

        return orig.substring(0, index + copyStringLength) + ' ' + copyNr;
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
        assert fontNamesLoaded.getCount() == 1;
        fontNames = GraphicsEnvironment
            .getLocalGraphicsEnvironment()
            .getAvailableFontFamilyNames();
        fontNamesLoaded.countDown();
    }

    public static void preloadUnitTestFontNames() {
        assert fontNamesLoaded.getCount() == 1;
        fontNames = new String[]{Font.DIALOG, Font.DIALOG_INPUT,
            Font.MONOSPACED, Font.SANS_SERIF, Font.SERIF};
        fontNamesLoaded.countDown();
    }

    public static String[] getAvailableFontNames() {
        // make sure that all font names are loaded
        try {
            boolean ok = fontNamesLoaded.await(5, TimeUnit.MINUTES);
            if (!ok) {
                throw new RuntimeException("Timeout: the font names could not be loaded");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return fontNames;
    }

    /**
     * Returns a new CompletableFuture that is completed when all the
     * CompletableFutures in the given list complete
     */
    public static CompletableFuture<Void> allOf(List<? extends CompletableFuture<?>> list) {
        if (list.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.allOf(list.toArray(EMPTY_CF_ARRAY));
    }

    public static double parseDouble(String s) throws ParseException {
        return NumberFormat.getInstance().parse(s).doubleValue();
    }

    public static String removePrefix(String s, String prefix) {
        if (s != null && prefix != null && s.startsWith(prefix)) {
            return s.substring(prefix.length());
        }
        return s;
    }

    // adds an "a" or "an" before the given word
    public static String addArticle(String s) {
        String article = switch (s.charAt(0)) {
            case 'a', 'e', 'i', 'o', 'u', 'h' -> "an ";
            default -> "a ";
        };
        return article + s;
    }

    // maps the interval [inStart,inEnd] onto the interval [outStart,outEnd]
    public static double mapInterval(double inStart, double inEnd, double outStart, double outEnd, double x) {
        double slope = (outEnd - outStart) / (inEnd - inStart);
        return outStart + slope * (x - inStart);
    }

    public static Rectangle calcUnionOfContentBounds(List<Layer> layers, boolean includeTransparent) {
        Rectangle bounds = null;
        for (Layer layer : layers) {
            if (layer instanceof ContentLayer contentLayer) {
                Rectangle layerBounds = contentLayer.getContentBounds(includeTransparent);
                if (layerBounds == null) {
                    continue;
                }
                if (bounds == null) {
                    bounds = layerBounds;
                } else {
                    bounds = bounds.union(layerBounds);
                }
            }
        }
        return bounds;
    }
}

