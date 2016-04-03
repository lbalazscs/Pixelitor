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

package pixelitor.tools;

import pixelitor.Composition;
import pixelitor.colors.FillType;
import pixelitor.filters.gui.AddDefaultButton;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.layers.ImageLayer;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;

import static pixelitor.Composition.ImageChangeActions.FULL;
import static pixelitor.colors.FillType.BACKGROUND;
import static pixelitor.colors.FillType.FOREGROUND;
import static pixelitor.colors.FillType.TRANSPARENT;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.WEST;

/**
 * A paint bucket tool.
 */
public class PaintBucketTool extends Tool {
    private final RangeParam toleranceParam = new RangeParam("Tolerance", 0, 20, 255);
    private JComboBox<FillType> fillComboBox;

    public PaintBucketTool() {
        super('p', "Paint Bucket", "paint_bucket_tool_icon.png", "click to fill with the selected color",
                Cursor.getDefaultCursor(), true, true, false, ClipStrategy.IMAGE_ONLY);
    }

    @Override
    public void initSettingsPanel() {
        settingsPanel.add(new SliderSpinner(toleranceParam, WEST, AddDefaultButton.NO));

        fillComboBox = new JComboBox<>(new FillType[]{FOREGROUND, BACKGROUND, TRANSPARENT});
        settingsPanel.addWithLabel("Fill With:", fillComboBox);
    }

    private Color getFillColor() {
        FillType fillType = getFillType();
        return fillType.getColor();
    }

    private FillType getFillType() {
        return (FillType) fillComboBox.getSelectedItem();
    }

    @Override
    public void mousePressed(MouseEvent e, ImageComponent ic) {
        // do nothing
    }

    @Override
    public void mouseDragged(MouseEvent e, ImageComponent ic) {
        // do nothing
    }

    @Override
    public void mouseReleased(MouseEvent e, ImageComponent ic) {
        double x = userDrag.getEndX();
        double y = userDrag.getEndY();

        Composition comp = ic.getComp();
        ImageLayer layer = comp.getActiveMaskOrImageLayer();

        int tx = layer.getTX();
        int ty = layer.getTY();

        x -= tx;
        y -= ty;

        AffineTransform translationTransform = null;
        if (tx != 0 || ty != 0) {
            translationTransform = AffineTransform.getTranslateInstance(-tx, -ty);
        }

        BufferedImage image = layer.getImage();
        BufferedImage original = ImageUtils.copyImage(image);
        BufferedImage workingCopy = ImageUtils.copyImage(image);

        Color newColor = getFillColor();

        Rectangle replacedArea = scanlineFloodFill(workingCopy, (int) x, (int) y, newColor, toleranceParam.getValue());

        if (replacedArea != null) { // something was replaced
            ToolAffectedArea affectedArea = new ToolAffectedArea(layer, replacedArea, true);
            saveSubImageForUndo(original, affectedArea);

            Graphics2D g = image.createGraphics();
            comp.applySelectionClipping(g, translationTransform);
            g.setComposite(AlphaComposite.Src);
            g.drawImage(workingCopy, 0, 0, null);
            g.dispose();

            comp.imageChanged(FULL);
            layer.updateIconImage();
        }

        workingCopy.flush();
    }

    /**
     * Uses the "Scanline fill" algorithm described at
     * http://en.wikipedia.org/wiki/Flood_fill
     */
    private static Rectangle scanlineFloodFill(BufferedImage img, int x, int y, Color newColor, int tolerance) {
        int minX = x;
        int maxX = x;
        int minY = y;
        int maxY = y;

        int imgHeight = img.getHeight();
        int imgWidth = img.getWidth();

        if (x < 0 || x >= imgWidth || y < 0 || y >= imgHeight) {
            return null;
        }

        int rgbToBeReplaced = img.getRGB(x, y);
        int newRGB = newColor.getRGB();

        if (rgbToBeReplaced == newRGB) {
            return null;
        }

        int[] pixels = ImageUtils.getPixelsAsArray(img);

        // Needed because the tolerance: we cannot assume that
        // if the pixel is within the target range, it has been processed
        boolean[] checkedPixels = new boolean[pixels.length];
        for (int i = 0; i < checkedPixels.length; i++) {
            checkedPixels[i] = false;
        }

        Deque<Point> stack = new ArrayDeque<>(); // the double-ended queue is used as a simple LIFO stack
        stack.push(new Point(x, y));

        while (!stack.isEmpty()) {
            Point p = stack.pop();

            x = p.x;
            y = p.y;

            // find the last replaceable point to the left
            int scanlineMinX = x - 1;
            int offset = y * imgWidth;
            while ((scanlineMinX >= 0) && similarColor(pixels[scanlineMinX + offset], rgbToBeReplaced, tolerance)) {
                scanlineMinX--;
            }
            scanlineMinX++;

            // find the last replaceable point to the right
            int scanlineMaxX = x + 1;
            while ((scanlineMaxX < img.getWidth()) && similarColor(pixels[scanlineMaxX + offset], rgbToBeReplaced, tolerance)) {
                scanlineMaxX++;
            }
            scanlineMaxX--;

            // set the minX, maxX, minY, maxY variables that will be used to calculate the affected area
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
                // if there are multiple pixels to be replaced that are horizontal neighbours,
                // only one of them has to be inspected later
                boolean pointsInLine = false;

                int upOffset = (y - 1) * imgWidth;

                for (int i = scanlineMinX; i <= scanlineMaxX; i++) {
                    int upIndex = i + upOffset;
                    boolean shouldBeReplaced = !checkedPixels[upIndex] && similarColor(pixels[upIndex], rgbToBeReplaced, tolerance);

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
                    boolean shouldBeReplaced = !checkedPixels[downIndex] && similarColor(pixels[downIndex], rgbToBeReplaced, tolerance);

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

    private static boolean similarColor(int color1, int color2, int tolerance) {
        if (color1 == color2) {
            return true;
        }

        int r1 = (color1 >>> 16) & 0xFF;
        int g1 = (color1 >>> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (color2 >>> 16) & 0xFF;
        int g2 = (color2 >>> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        return (r2 <= r1 + tolerance) && (r2 >= r1 - tolerance) &&
                (g2 <= g1 + tolerance) && (g2 >= g1 - tolerance) &&
                (b2 <= b1 + tolerance) && (b2 >= b1 - tolerance);
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = super.getDebugNode();

        node.addIntChild("Tolerance", toleranceParam.getValue());
        node.addQuotedStringChild("Fill With", getFillType().toString());

        return node;
    }
}
