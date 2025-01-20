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
import pixelitor.gui.utils.VectorIcon;
import pixelitor.layers.Drawable;
import pixelitor.layers.Layer;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.Shapes;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ResourceBundle;

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_MITER;
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
    public void paintOverView(Graphics2D g2, Composition comp) {
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
    public boolean canHaveUserPresets() {
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
    public VectorIcon createIcon() {
        return new ColorPickerToolIcon();
    }

    private static class ColorPickerToolIcon extends Tool.ToolIcon {
        private static final Color GLASS_COLOR = new Color(0x68_00_00_00, true);

        @Override
        public void paintIcon(Graphics2D g) {
            // based on color_picker_tool.svg
            Path2D glassPath = new Path2D.Double();

            glassPath.moveTo(15.487128, 10.694453);
            glassPath.lineTo(1.8488811, 24.332703);
            glassPath.curveTo(1.8488811, 24.332703, 0.9396646, 25.241873, 1.8488811, 26.151114);
            glassPath.curveTo(2.7580976, 27.060343, 3.667314, 26.151114, 3.667314, 26.151114);
            glassPath.lineTo(17.305561, 12.512863);
            glassPath.closePath();

            g.setColor(GLASS_COLOR);
            g.fill(glassPath);

            g.setColor(color);

            g.setStroke(new BasicStroke(0.9106483f, CAP_BUTT, JOIN_MITER, 4));
            g.draw(glassPath);

            Path2D handlePath = new Path2D.Double();
            handlePath.moveTo(13.668696, 7.966804);
            handlePath.lineTo(16.396345, 5.239154);
            handlePath.lineTo(18.214779, 7.057564);
            handlePath.curveTo(18.214779, 7.057564, 21.90847, 1.6428838, 22.76086, 1.6022639);
            handlePath.curveTo(23.694431, 1.642764, 26.438318, 4.549124, 26.397728, 5.239154);
            handlePath.curveTo(26.397728, 6.132134, 20.942429, 9.785213, 20.942429, 9.785213);
            handlePath.lineTo(22.76086, 11.603653);
            handlePath.lineTo(20.03321, 14.331303);
            handlePath.closePath();

            g.setColor(color);

            g.fill(handlePath);
            g.setStroke(new BasicStroke(0.90921646f, CAP_BUTT, JOIN_MITER, 4));
            g.draw(handlePath);
        }
    }
}
