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

import pixelitor.Composition;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.GUIText;
import pixelitor.gui.View;
import pixelitor.layers.Drawable;
import pixelitor.layers.Layer;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.Shapes;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static java.lang.String.format;
import static pixelitor.colors.FgBgColors.setBGColor;
import static pixelitor.colors.FgBgColors.setFGColor;
import static pixelitor.gui.utils.SliderSpinner.LabelPosition.WEST;
import static pixelitor.utils.ImageUtils.isGrayscale;
import static pixelitor.utils.ImageUtils.isWithinBounds;

/**
 * The color picker tool
 */
public class ColorPickerTool extends Tool {
    private static final String SAMPLE_LABEL_TEXT = "Sample Only the Active Layer/Mask";
    private static final String HELP_TEXT =
        "<b>click</b> to pick the foreground color, " +
            "<b>Alt-click</b> (or <b>right-click</b>) to pick the background color.";

    private final JCheckBox layerOnlySamplingCB = new JCheckBox();
    private final RangeParam samplingRadius = new RangeParam(GUIText.RADIUS,
        0, 0, 10, false, WEST);

    // the sampling bounds if the sampling radius > 0
    private int startX;
    private int endX;
    private int startY;
    private int endY;
    private boolean paintSamplingBounds = false;

    public ColorPickerTool() {
        super("Color Picker", 'I', HELP_TEXT, Cursors.CROSSHAIR);
    }

    @Override
    public void initSettingsPanel(ResourceBundle resources) {
        settingsPanel.add(new JLabel(SAMPLE_LABEL_TEXT + ":"));
        settingsPanel.add(layerOnlySamplingCB);

        settingsPanel.addSeparator();

        settingsPanel.addParam(samplingRadius);
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        sampleColor(e, e.isAltDown() || e.isRight());
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        sampleColor(e, e.isAltDown() || e.isRight());
    }

    @Override
    public void mouseReleased(PMouseEvent e) {
        if (paintSamplingBounds) {
            // stop painting
            paintSamplingBounds = false;
            e.getView().repaint();
        }
    }

    public void sampleColor(PMouseEvent e, boolean selectBackground) {
        View view = e.getView();

        // image-space coordinates relative to the canvas
        int x = (int) e.getImX();
        int y = (int) e.getImY();

        BufferedImage srcImg;
        boolean isGray = false;
        Composition comp = view.getComp();
        if (layerOnlySamplingCB.isSelected()) {
            Drawable dr = comp.getActiveDrawable();
            if (dr == null) {
                return;
            }
            srcImg = dr.getImage();
            isGray = isGrayscale(srcImg);

            // make them relative to the drawable's image
            x -= dr.getTx();
            y -= dr.getTy();
        } else {
            srcImg = comp.getCompositeImage();
        }

        if (!isWithinBounds(x, y, srcImg)) {
            return;
        } else {
            int sampledRGB = sampleImage(srcImg, x, y, samplingRadius.getValue());

            if (paintSamplingBounds) {
                view.repaint();
            }
            showColorInfo(x, y, sampledRGB, isGray);

            Color pickedColor = new Color(sampledRGB);
            if (selectBackground) {
                setBGColor(pickedColor);
            } else {
                setFGColor(pickedColor);
            }
        }
    }

    private int sampleImage(BufferedImage srcImg, int x, int y, int radius) {
        int width = srcImg.getWidth();
        int height = srcImg.getHeight();
        int[] pixels = ImageUtils.getPixels(srcImg);

        // if radius is 0, sample only the pixel under the mouse
        if (radius == 0) {
            paintSamplingBounds = false;
            return pixels[x + y * width];
        }
        paintSamplingBounds = true;

        // calculate the sampling bounds
        startX = Math.max(0, x - radius);
        endX = Math.min(width - 1, x + radius);
        startY = Math.max(0, y - radius);
        endY = Math.min(height - 1, y + radius);

        // accumulate values for each channel
        int a = 0, r = 0, g = 0, b = 0;
        int sampledPixels = 0;
        for (int sy = startY; sy <= endY; sy++) {
            for (int sx = startX; sx <= endX; sx++) {
                int rgb = pixels[sx + sy * width];
                a += (rgb >> 24) & 0xff;
                r += (rgb >> 16) & 0xff;
                g += (rgb >> 8) & 0xff;
                b += rgb & 0xff;
                sampledPixels++;
            }
        }

        // return the average color
        a /= sampledPixels;
        r /= sampledPixels;
        g /= sampledPixels;
        b /= sampledPixels;
        int avg = (a << 24) | (r << 16) | (g << 8) | b;
        return avg;
    }

    private static void showColorInfo(int x, int y, int rgb, boolean isGray) {
        int a = (rgb >>> 24) & 0xFF;
        int r = (rgb >>> 16) & 0xFF;
        int g = (rgb >>> 8) & 0xFF;
        int b = rgb & 0xFF;

        String msg = "x = " + x + ", y = " + y;
        if (isGray) {
            msg += ", gray = " + r;
        } else {
            float[] hsbValues = Color.RGBtoHSB(r, g, b, null);
            msg += format(", alpha = %d, red = %d, green = %d, blue = %d, " +
                    "hue = %.2f, saturation = %.2f, brightness = %.2f",
                a, r, g, b, hsbValues[0], hsbValues[1], hsbValues[2]);
        }

        Messages.showPlainStatusMessage(msg);
    }

    @Override
    public void editingTargetChanged(Layer activeLayer) {
        if (layerOnlySamplingCB.isSelected()) {
            // don't show the values for the old layer
            Messages.clearStatusBar();
        }
    }

    @Override
    public void paintOverCanvas(Graphics2D g2, Composition comp) {
        if (!paintSamplingBounds) {
            return;
        }
        Rectangle2D imSamplingRect;
        if (layerOnlySamplingCB.isSelected()) {
            // tranlate the sampling bounds to be relative to the canvas
            Drawable dr = comp.getActiveDrawable();
            if (dr == null) {
                return;
            }
            imSamplingRect = new Rectangle2D.Double(
                startX + dr.getTx(), startY + dr.getTx(),
                endX - startX + 1, endY - startY + 1);
        } else {
            imSamplingRect = new Rectangle2D.Double(
                startX, startY,
                endX - startX + 1, endY - startY + 1);
        }
        Rectangle2D coSamplingRect = comp.getView().imageToComponentSpace2(imSamplingRect);
        Shapes.drawVisibly(g2, coSamplingRect);
    }

    @Override
    public boolean supportsUserPresets() {
        return false;
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);
        node.addBoolean(SAMPLE_LABEL_TEXT, layerOnlySamplingCB.isSelected());
        return node;
    }

    @Override
    public Consumer<Graphics2D> createIconPainter() {
        return ToolIcons::paintColorPickerIcon;
    }
}
