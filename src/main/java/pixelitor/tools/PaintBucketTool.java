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

package pixelitor.tools;

import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.GUIText;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.history.History;
import pixelitor.history.PartialImageEdit;
import pixelitor.layers.Drawable;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_MITER;
import static pixelitor.colors.FgBgColors.getBGColor;
import static pixelitor.colors.FgBgColors.getFGColor;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.WEST;

/**
 * The paint bucket tool.
 */
public class PaintBucketTool extends Tool {
    private static final String ACTION_LOCAL = "Local";
    private static final String ACTION_GLOBAL = "Global";

    private static final String FILL_FOREGROUND = GUIText.FG_COLOR;
    private static final String FILL_BACKGROUND = GUIText.BG_COLOR;
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
            "paint_bucket_tool.png",
            "<b>click</b> to fill with the selected color.",
            Cursors.DEFAULT);
    }

    @Override
    public void initSettingsPanel() {
        settingsPanel.add(new SliderSpinner(toleranceParam, WEST, false));

        settingsPanel.addComboBox(GUIText.FILL_WITH + ":", fillCB, "fillCB");
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

        boolean grayScale = false;
        if (image.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            grayScale = true;
        }

        BufferedImage workingImage;
        if (grayScale) {
            workingImage = ImageUtils.toSysCompatibleImage(image);
        } else if (thereIsSelection) {
            workingImage = ImageUtils.copyImage(image);
        } else {
            workingImage = image;
        }

        String fill = (String) fillCB.getSelectedItem();
        int rgbAtMouse = workingImage.getRGB(x, y);

        int fillRGB;
        if (fill.equals(FILL_FOREGROUND)) {
            fillRGB = getFGColor().getRGB();
        } else if (fill.equals(FILL_BACKGROUND)) {
            fillRGB = getBGColor().getRGB();
        } else if (fill.equals(FILL_TRANSPARENT)) {
            fillRGB = 0x00000000;
        } else if (fill.equals(FILL_CLICKED)) {
            fillRGB = rgbAtMouse;
        } else {
            throw new IllegalStateException("fill = " + fill);
        }

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
            } else if (grayScale) {
                dr.setImage(ImageUtils.convertToGrayScaleImage(workingImage));
            }
            comp.update();
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
    public boolean allowOnlyDrawables() {
        return true;
    }

    @Override
    public DebugNode createDebugNode() {
        var node = super.createDebugNode();

        node.addInt("tolerance", toleranceParam.getValue());
        node.addQuotedString("fill with", (String) fillCB.getSelectedItem());
        node.addQuotedString("action", (String) actionCB.getSelectedItem());

        return node;
    }

    @Override
    public Icon createIcon() {
        return new PaintBucketToolIcon();
    }

    private static class PaintBucketToolIcon extends Tool.ToolIcon {
        @Override
        public void paintIcon(Graphics2D g) {
            // the shape is based on paint_bucket_tool.svg
            Path2D shape = new Path2D.Float();

            // bucket
            shape.moveTo(5.4289136, 12.759313);
            shape.lineTo(14.020062, 25.454193);
            shape.lineTo(26.406328, 18.948254);
            shape.lineTo(20.734503, 4.768684);
            shape.closePath();

            g.setStroke(new BasicStroke(1.3f, CAP_BUTT, JOIN_MITER, 4));
            g.draw(shape);

            // handle
            shape = new Path2D.Float();
            shape.moveTo(14.87057, 12.192133);
            shape.curveTo(14.87057, 12.192133, 11.7013235, 3.5592537, 13.550051, 2.5583534);
            shape.curveTo(15.398779, 1.5574434, 16.939384, 6.8122234, 16.939384, 6.8122234);
            shape.lineTo(16.939384, 6.8122234);

            g.draw(shape);

            // paint
            shape = new Path2D.Float();
            shape.moveTo(8.19497, 10.853959);
            shape.curveTo(5.256423, 10.799759, 0.59281015, 13.51276, 0.6504288, 15.789537);
            shape.curveTo(0.70804787, 18.066315, 1.8028003, 18.152325, 2.8399348, 18.206568);
            shape.curveTo(3.9284532, 18.238678, 4.7648406, 17.252796, 4.862978, 16.437378);
            shape.curveTo(4.978212, 15.298976, 5.03873, 14.855405, 5.635888, 13.452499);
            shape.curveTo(5.828665, 12.999517, 8.19497, 10.853959, 8.19497, 10.853959);
            shape.closePath();

            g.fill(shape);
        }
    }
}
