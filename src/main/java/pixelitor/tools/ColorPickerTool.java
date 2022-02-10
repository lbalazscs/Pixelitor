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

package pixelitor.tools;

import pixelitor.Composition;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.View;
import pixelitor.layers.Drawable;
import pixelitor.layers.Layer;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;
import pixelitor.utils.Messages;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_MITER;
import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;
import static java.lang.String.format;
import static pixelitor.colors.FgBgColors.setBGColor;
import static pixelitor.colors.FgBgColors.setFGColor;

/**
 * The color picker tool
 */
public class ColorPickerTool extends Tool {
    private static final String SAMPLE_LABEL_TEXT = "Sample Only the Active Layer/Mask";
    private static final String HELP_TEXT =
        "<b>click</b> to pick the foreground color, " +
        "<b>Alt-click</b> (or <b>right-click</b>) to pick the background color.";

    private final JCheckBox sampleLayerOnly = new JCheckBox(SAMPLE_LABEL_TEXT);

    public ColorPickerTool() {
        super("Color Picker", 'I', "color_picker_tool.png",
            HELP_TEXT, Cursors.CROSSHAIR);
    }

    @Override
    public void initSettingsPanel() {
        settingsPanel.add(sampleLayerOnly);
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
    }

    public void sampleColor(PMouseEvent e, boolean selectBackground) {
        View view = e.getView();
        int x = (int) e.getImX();
        int y = (int) e.getImY();

        BufferedImage img;
        boolean isGray = false;
        Composition comp = view.getComp();
        if (sampleLayerOnly.isSelected()) {
            Drawable dr = comp.getActiveDrawable();
            if (dr == null) {
                return;
            }
            img = dr.getImage();
            isGray = img.getType() == TYPE_BYTE_GRAY;

            x -= dr.getTx();
            y -= dr.getTy();
        } else {
            img = comp.getCompositeImage();
        }

        if (x < img.getWidth() && y < img.getHeight() && x >= 0 && y >= 0) {
            int rgb = img.getRGB(x, y);

            showColorInStatusBar(x, y, rgb, isGray);

            Color sampledColor = new Color(rgb);
            if (selectBackground) {
                setBGColor(sampledColor);
            } else {
                setFGColor(sampledColor);
            }
        }
    }

    private static void showColorInStatusBar(int x, int y, int rgb, boolean isGray) {
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

        Messages.showPlainInStatusBar(msg);
    }

    @Override
    public void editingTargetChanged(Layer layer) {
        if (sampleLayerOnly.isSelected()) {
            // don't show the values for the old layer
            Messages.showPlainInStatusBar("");
        }
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
    public DebugNode createDebugNode() {
        var node = super.createDebugNode();

        node.addBoolean(SAMPLE_LABEL_TEXT, sampleLayerOnly.isSelected());

        return node;
    }

    @Override
    public Icon createIcon() {
        return new ColorPickerToolIcon();
    }

    private static class ColorPickerToolIcon extends Tool.ToolIcon {
        @Override
        public void paintIcon(Graphics2D g) {
            // based on color_picker_tool.svg
            Path2D shape = new Path2D.Float();

            shape.moveTo(15.487128, 10.694453);
            shape.lineTo(1.8488811, 24.332703);
            shape.curveTo(1.8488811, 24.332703, 0.9396646, 25.241873, 1.8488811, 26.151114);
            shape.curveTo(2.7580976, 27.060343, 3.667314, 26.151114, 3.667314, 26.151114);
            shape.lineTo(17.305561, 12.512863);
            shape.closePath();

            g.setPaint(new Color(0x68000000, true));
            g.fill(shape);
            g.setPaint(new Color(0xA5000000, true));
            g.setStroke(new BasicStroke(0.9106483f, CAP_BUTT, JOIN_MITER, 4));
            g.draw(shape);

            shape = new Path2D.Float();
            shape.moveTo(13.668696, 7.966804);
            shape.lineTo(16.396345, 5.239154);
            shape.lineTo(18.214779, 7.057564);
            shape.curveTo(18.214779, 7.057564, 21.90847, 1.6428838, 22.76086, 1.6022639);
            shape.curveTo(23.694431, 1.642764, 26.438318, 4.549124, 26.397728, 5.239154);
            shape.curveTo(26.397728, 6.132134, 20.942429, 9.785213, 20.942429, 9.785213);
            shape.lineTo(22.76086, 11.603653);
            shape.lineTo(20.03321, 14.331303);
            shape.closePath();

            g.setPaint(Color.BLACK);
            g.fill(shape);
            g.setStroke(new BasicStroke(0.90921646f, CAP_BUTT, JOIN_MITER, 4));
            g.draw(shape);
        }
    }
}
