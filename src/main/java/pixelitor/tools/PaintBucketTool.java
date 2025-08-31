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

package pixelitor.tools;

import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.UserPreset;
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
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_MITER;
import static pixelitor.colors.FgBgColors.getBGColor;
import static pixelitor.colors.FgBgColors.getFGColor;
import static pixelitor.gui.utils.SliderSpinner.LabelPosition.WEST;
import static pixelitor.utils.ImageUtils.isGrayscale;

/**
 * The paint bucket tool.
 */
public class PaintBucketTool extends Tool {
    private enum Action {
        LOCAL("Local"),   // flood fill around clicked pixel
        GLOBAL("Global"); // replace throughout image

        private final String displayName;

        Action(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private enum Fill {
        FOREGROUND(GUIText.FG_COLOR),
        BACKGROUND(GUIText.BG_COLOR),
        TRANSPARENT("Transparent"),
        CLICKED("Clicked Pixel Color");

        private final String displayName;

        Fill(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final RangeParam colorTolerance = new RangeParam("Tolerance", 0, 20, 255);
    private final JComboBox<Fill> fillCB = new JComboBox<>(Fill.values());
    private final JComboBox<Action> actionCB = new JComboBox<>(Action.values());

    public PaintBucketTool() {
        super("Paint Bucket", 'N',
            "<b>click</b> to fill with the selected color.",
            Cursors.DEFAULT);
    }

    @Override
    public void initSettingsPanel(ResourceBundle resources) {
        settingsPanel.add(new SliderSpinner(colorTolerance, WEST, false));

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
        var comp = e.getComp();
        Drawable dr = comp.getActiveDrawableOrThrow();

        int tx = dr.getTx();
        int ty = dr.getTy();

        // the click position relative to image coordinates
        int x = (int) e.getImX() - tx;
        int y = (int) e.getImY() - ty;

        BufferedImage targetImage = dr.getImage();
        int imgHeight = targetImage.getHeight();
        int imgWidth = targetImage.getWidth();
        if (x < 0 || x >= imgWidth || y < 0 || y >= imgHeight) {
            return;
        }

        BufferedImage backupForUndo = ImageUtils.copyImage(targetImage);
        boolean hasSelection = comp.hasSelection();

        boolean targetIsGrayscale = isGrayscale(targetImage);

        BufferedImage workingImage;
        if (targetIsGrayscale) {
            // the algorithms are implemented only for RGBA images
            workingImage = ImageUtils.toSysCompatibleImage(targetImage);
        } else if (hasSelection) {
            // copy to be able to apply only the selected modifications
            workingImage = ImageUtils.copyImage(targetImage);
        } else {
            // we can modify the original image directly
            workingImage = targetImage;
        }

        int rgbAtMouse = workingImage.getRGB(x, y);

        int fillRGB = switch (getSelectedFill()) {
            case Fill.FOREGROUND -> getFGColor().getRGB();
            case Fill.BACKGROUND -> getBGColor().getRGB();
            case Fill.TRANSPARENT -> 0x00_00_00_00;
            case Fill.CLICKED -> rgbAtMouse;
        };

        int tolerance = colorTolerance.getValue();
        Rectangle modifiedArea = switch (getSelectedAction()) {
            case LOCAL -> scanlineFloodFill(workingImage,
                x, y, tolerance, fillRGB);
            case GLOBAL -> globalReplaceColor(workingImage,
                tolerance, rgbAtMouse, fillRGB);
        };

        if (modifiedArea != null) { // something was modified
            PartialImageEdit edit = PartialImageEdit.create(modifiedArea, backupForUndo, dr,
                true, getName());
            if (edit != null) {
                History.add(edit);
            }

            if (hasSelection) {
                // apply the changes through the selection clip

                Graphics2D g = targetImage.createGraphics();

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
            } else if (targetIsGrayscale) {
                dr.setImage(ImageUtils.convertToGrayscaleImage(workingImage));
            }
            dr.update();
            dr.updateIconImage();
        }
    }

    /**
     * Fills an area using the generic scanline flood-fill utility.
     *
     * @return A Rectangle representing the bounding box of the modified area.
     */
    private static Rectangle scanlineFloodFill(BufferedImage img,
                                               int x, int y, int tolerance,
                                               int newRGB) {
        int imgHeight = img.getHeight();
        int imgWidth = img.getWidth();
        int[] pixels = ImageUtils.getPixels(img);

        // Initialize with the starting point.
        Rectangle bounds = new Rectangle(x, y, 1, 1);

        ImageUtils.floodFill(pixels, imgWidth, imgHeight, x, y, tolerance,
            // define the action: fill pixels and expand the bounding box
            (segY, segX1, segX2) -> {
                // expand the bounding box to include the new segment
                bounds.add(segX1, segY);
                bounds.add(segX2, segY);

                // fill the pixels in the segment with the new color
                int offset = segY * imgWidth;
                for (int i = segX1; i <= segX2; i++) {
                    pixels[offset + i] = newRGB;
                }
            });

        // The flood fill utility needs the start color to work, but if the start pixel
        // itself doesn't match (e.g., tolerance 0 and start color != start color),
        // the fill won't happen. We check if the start pixel was changed.
        if (pixels[y * imgWidth + x] != newRGB) {
            // The starting pixel itself was not similar enough to be filled.
            // This can happen if the start color is the same as the fill color.
            // We check if the original color was similar to itself.
            int originalColor = img.getRGB(x, y);
            if (ImageUtils.isSimilar(originalColor, originalColor, tolerance)) {
                pixels[y * imgWidth + x] = newRGB;
            } else {
                return null; // nothing was changed
            }
        }

        bounds.grow(1, 1);
        return bounds;
    }

    private static Rectangle globalReplaceColor(BufferedImage img,
                                                int tolerance,
                                                int rgbAtMouse, int newRGB) {
        int[] pixels = ImageUtils.getPixels(img);
        for (int i = 0; i < pixels.length; i++) {
            if (ImageUtils.isSimilar(pixels[i], rgbAtMouse, tolerance)) {
                pixels[i] = newRGB;
            }
        }

        // Return the replaced area, which is the entire image.
        return new Rectangle(0, 0, img.getWidth(), img.getHeight());
    }

    @Override
    public boolean allowOnlyDrawables() {
        return true;
    }

    private Fill getSelectedFill() {
        return (Fill) fillCB.getSelectedItem();
    }

    private Action getSelectedAction() {
        return (Action) actionCB.getSelectedItem();
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        colorTolerance.saveStateTo(preset);
        preset.put("Fill", getSelectedFill().toString());
        preset.put("Action", getSelectedAction().toString());
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        colorTolerance.loadStateFrom(preset);
        fillCB.setSelectedItem(preset.getEnum("Fill", Fill.class));
        actionCB.setSelectedItem(preset.getEnum("Action", Action.class));
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.addInt("tolerance", colorTolerance.getValue());
        node.addQuotedString("fill", getSelectedFill().toString());
        node.addQuotedString("action", getSelectedAction().toString());

        return node;
    }

    @Override
    public Consumer<Graphics2D> createIconPainter() {
        return g -> {
            // the shape is based on paint_bucket_tool.svg
            Path2D shape = new Path2D.Double();

            // bucket
            shape.moveTo(5.4289136, 12.759313);
            shape.lineTo(14.020062, 25.454193);
            shape.lineTo(26.406328, 18.948254);
            shape.lineTo(20.734503, 4.768684);
            shape.closePath();

            g.setStroke(new BasicStroke(1.3f, CAP_BUTT, JOIN_MITER, 4));
            g.draw(shape);

            // handle
            shape = new Path2D.Double();
            shape.moveTo(14.87057, 12.192133);
            shape.curveTo(14.87057, 12.192133, 11.7013235, 3.5592537, 13.550051, 2.5583534);
            shape.curveTo(15.398779, 1.5574434, 16.939384, 6.8122234, 16.939384, 6.8122234);
            shape.lineTo(16.939384, 6.8122234);

            g.draw(shape);

            // paint
            shape = new Path2D.Double();
            shape.moveTo(8.19497, 10.853959);
            shape.curveTo(5.256423, 10.799759, 0.59281015, 13.51276, 0.6504288, 15.789537);
            shape.curveTo(0.70804787, 18.066315, 1.8028003, 18.152325, 2.8399348, 18.206568);
            shape.curveTo(3.9284532, 18.238678, 4.7648406, 17.252796, 4.862978, 16.437378);
            shape.curveTo(4.978212, 15.298976, 5.03873, 14.855405, 5.635888, 13.452499);
            shape.curveTo(5.828665, 12.999517, 8.19497, 10.853959, 8.19497, 10.853959);
            shape.closePath();

            g.fill(shape);
        };
    }
}
