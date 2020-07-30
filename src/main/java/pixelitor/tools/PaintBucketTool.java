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

package pixelitor.tools;

import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.history.History;
import pixelitor.history.PartialImageEdit;
import pixelitor.layers.Drawable;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;

import static pixelitor.colors.FgBgColors.getBGColor;
import static pixelitor.colors.FgBgColors.getFGColor;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.WEST;

/**
 * The paint bucket tool.
 */
public class PaintBucketTool extends Tool {
    private static final String ACTION_LOCAL = "Local";
    private static final String ACTION_GLOBAL = "Global";

    private static final String FILL_FOREGROUND = "Foreground Color";
    private static final String FILL_BACKGROUND = "Background Color";
    private static final String FILL_TRANSPARENT = "Transparent";
    private static final String FILL_CLICKED = "Clicked Pixel Color";

    private final RangeParam toleranceParam = new RangeParam("Tolerance", 0, 20, 255);
    private final JComboBox<String> fillCB = new JComboBox<>(
            new String[]{FILL_FOREGROUND, FILL_BACKGROUND,
                    FILL_TRANSPARENT, FILL_CLICKED}
    );
    private final JComboBox<String> actionCB = new JComboBox<>(
            new String[]{ACTION_LOCAL, ACTION_GLOBAL});

    public PaintBucketTool() {
        super("Paint Bucket", 'N',
                "paint_bucket_tool_icon.png",
                "<b>click</b> to fill with the selected color.",
                Cursors.DEFAULT, true,
                true, ClipStrategy.CANVAS);
    }

    @Override
    public void initSettingsPanel() {
        settingsPanel.add(new SliderSpinner(toleranceParam, WEST, false));

        settingsPanel.addComboBox("Fill With:", fillCB, "fillCB");
        settingsPanel.addComboBox("Action:", actionCB, "actionCB");
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        // do nothing
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        // do nothing
    }

    @Override
    public void mouseReleased(PMouseEvent e) {
        int x = (int) e.getImX();
        int y = (int) e.getImY();

        var comp = e.getComp();
        Drawable dr = comp.getActiveDrawableOrThrow();

        int tx = dr.getTx();
        int ty = dr.getTy();

        x -= tx;
        y -= ty;

        BufferedImage image = dr.getImage();

        int imgHeight = image.getHeight();
        int imgWidth = image.getWidth();
        if (x < 0 || x >= imgWidth || y < 0 || y >= imgHeight) {
            return;
        }

        BufferedImage backupForUndo = ImageUtils.copyImage(image);
        boolean thereIsSelection = comp.hasSelection();
        BufferedImage workingImage;
        if (thereIsSelection) {
            workingImage = ImageUtils.copyImage(image);
        } else {
            workingImage = image;
        }

        String fill = (String) fillCB.getSelectedItem();
        int rgbAtMouse = workingImage.getRGB(x, y);
        int fillRGB = switch (fill) {
            case FILL_FOREGROUND -> getFGColor().getRGB();
            case FILL_BACKGROUND -> getBGColor().getRGB();
            case FILL_TRANSPARENT -> 0x00000000;
            case FILL_CLICKED -> rgbAtMouse;
            default -> throw new IllegalStateException("fill = " + fill);
        };

        String action = (String) actionCB.getSelectedItem();
        int tolerance = toleranceParam.getValue();
        Rectangle replacedArea = switch (action) {
            case ACTION_LOCAL -> scanlineFloodFill(workingImage,
                x, y, tolerance, rgbAtMouse, fillRGB);
            case ACTION_GLOBAL -> globalReplaceColor(workingImage,
                tolerance, rgbAtMouse, fillRGB);
            default -> throw new IllegalStateException("action = " + action);
        };

        if (replacedArea != null) { // something was replaced
            PartialImageEdit edit = History.createPartialImageEdit(replacedArea, backupForUndo, dr,
                true, getName());
            if (edit != null) {
                History.add(edit);
            }

            if (thereIsSelection) {
                Graphics2D g = image.createGraphics();

                // the selection is relative to the canvas,
                // so go to the canvas start
                g.translate(-tx, -ty);
                comp.applySelectionClipping(g);
                g.translate(tx, ty); // go back

                // makes "fill with transparency" possible
                g.setComposite(AlphaComposite.Src);

                g.drawImage(workingImage, 0, 0, null);
                g.dispose();
                workingImage.flush();
            }
            comp.imageChanged();
            dr.updateIconImage();
        }
    }

    /**
     * Uses the "Scanline fill" algorithm described at
     * http://en.wikipedia.org/wiki/Flood_fill
     */
    private static Rectangle scanlineFloodFill(BufferedImage img,
                                               int x, int y, int tolerance,
                                               int rgbAtMouse, int newRGB) {
        int minX = x;
        int maxX = x;
        int minY = y;
        int maxY = y;
        int imgHeight = img.getHeight();
        int imgWidth = img.getWidth();

        int[] pixels = ImageUtils.getPixelsAsArray(img);

        // Needed because the tolerance: we cannot assume that
        // if the pixel is within the target range, it has been processed
        boolean[] checkedPixels = new boolean[pixels.length];

        // the double-ended queue is used as a simple LIFO stack
        Deque<Point> stack = new ArrayDeque<>();
        stack.push(new Point(x, y));

        while (!stack.isEmpty()) {
            Point p = stack.pop();

            x = p.x;
            y = p.y;

            // find the last replaceable point to the left
            int scanlineMinX = x - 1;
            int offset = y * imgWidth;
            while (scanlineMinX >= 0
                    && isSimilar(pixels[scanlineMinX + offset], rgbAtMouse, tolerance)) {
                scanlineMinX--;
            }
            scanlineMinX++;

            // find the last replaceable point to the right
            int scanlineMaxX = x + 1;
            while (scanlineMaxX < img.getWidth()
                    && isSimilar(pixels[scanlineMaxX + offset], rgbAtMouse, tolerance)) {
                scanlineMaxX++;
            }
            scanlineMaxX--;

            // set the minX, maxX, minY, maxY variables
            // that will be used to calculate the affected area
            if (scanlineMinX < minX) {
                minX = scanlineMinX;
            }
            if (scanlineMaxX > maxX) {
                maxX = scanlineMaxX;
            }
            if (y > maxY) {
                maxY = y;
            } else if (y < minY) {
                minY = y;
            }

            // draw a line between (scanlineMinX, y) and (scanlineMaxX, y)
            for (int i = scanlineMinX; i <= scanlineMaxX; i++) {
                int index = i + offset;
                pixels[index] = newRGB;
                checkedPixels[index] = true;
            }

            // look upwards for new points to be inspected later
            if (y > 0) {
                // if there are multiple pixels to be replaced
                // that are horizontal neighbours,
                // only one of them has to be inspected later
                boolean pointsInLine = false;

                int upOffset = (y - 1) * imgWidth;

                for (int i = scanlineMinX; i <= scanlineMaxX; i++) {
                    int upIndex = i + upOffset;
                    boolean shouldBeReplaced = !checkedPixels[upIndex]
                            && isSimilar(pixels[upIndex], rgbAtMouse, tolerance);

                    if (!pointsInLine && shouldBeReplaced) {
                        Point inspectLater = new Point(i, y - 1);
                        stack.push(inspectLater);
                        pointsInLine = true;
                    } else if (pointsInLine && !shouldBeReplaced) {
                        pointsInLine = false;
                    }
                }
            }

            // look downwards for new points to be inspected later
            if (y < imgHeight - 1) {
                boolean pointsInLine = false;
                int downOffset = (y + 1) * imgWidth;

                for (int i = scanlineMinX; i <= scanlineMaxX; i++) {
                    int downIndex = i + downOffset;
                    boolean shouldBeReplaced = !checkedPixels[downIndex]
                            && isSimilar(pixels[downIndex], rgbAtMouse, tolerance);

                    if (!pointsInLine && shouldBeReplaced) {
                        Point inspectLater = new Point(i, y + 1);
                        stack.push(inspectLater);
                        pointsInLine = true;
                    } else if (pointsInLine && !shouldBeReplaced) {
                        pointsInLine = false;
                    }
                }
            }
        }

        // return the affected area
        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static Rectangle globalReplaceColor(BufferedImage img,
                                                int tolerance,
                                                int rgbAtMouse, int newRGB) {
        int[] pixels = ImageUtils.getPixelsAsArray(img);
        for (int i = 0; i < pixels.length; i++) {
            if (isSimilar(pixels[i], rgbAtMouse, tolerance)) {
                pixels[i] = newRGB;
            }
        }

        // return the replaced area, which is the whole image
        return new Rectangle(0, 0, img.getWidth(), img.getHeight());
    }

    private static boolean isSimilar(int color1, int color2, int tolerance) {
        if (color1 == color2) {
            return true;
        }

        int a1 = (color1 >>> 24) & 0xFF;
        int r1 = (color1 >>> 16) & 0xFF;
        int g1 = (color1 >>> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >>> 24) & 0xFF;
        int r2 = (color2 >>> 16) & 0xFF;
        int g2 = (color2 >>> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        return (r2 <= r1 + tolerance) && (r2 >= r1 - tolerance) &&
                (g2 <= g1 + tolerance) && (g2 >= g1 - tolerance) &&
            (b2 <= b1 + tolerance) && (b2 >= b1 - tolerance) &&
            (a2 <= a1 + tolerance) && (a2 >= a1 - tolerance);
    }

    @Override
    public DebugNode getDebugNode() {
        var node = super.getDebugNode();

        node.addInt("tolerance", toleranceParam.getValue());
        node.addQuotedString("fill with", (String) fillCB.getSelectedItem());
        node.addQuotedString("action", (String) actionCB.getSelectedItem());

        return node;
    }
}
