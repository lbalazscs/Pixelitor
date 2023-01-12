/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.utils.VectorIcon;
import pixelitor.layers.Drawable;
import pixelitor.layers.Layer;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;
import pixelitor.utils.Messages;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.*;
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
        super("Color Picker", 'I', HELP_TEXT, Cursors.CROSSHAIR);
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
    public void activeLayerChanged(Layer layer) {
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
    public DebugNode createDebugNode(String key) {
        var node = super.createDebugNode(key);

        node.addBoolean(SAMPLE_LABEL_TEXT, sampleLayerOnly.isSelected());

        return node;
    }

    @Override
    public VectorIcon createIcon() {
        return new ColorPickerToolIcon();
    }

    private static class ColorPickerToolIcon extends Tool.ToolIcon {
        @Override
        public void paintIcon(Graphics2D g) {
//            boolean dark = Themes.getCurrent().isDark();

            // based on color_picker_tool.svg
            Path2D glassPath = new Path2D.Float();

            glassPath.moveTo(15.487128, 10.694453);
            glassPath.lineTo(1.8488811, 24.332703);
            glassPath.curveTo(1.8488811, 24.332703, 0.9396646, 25.241873, 1.8488811, 26.151114);
            glassPath.curveTo(2.7580976, 27.060343, 3.667314, 26.151114, 3.667314, 26.151114);
            glassPath.lineTo(17.305561, 12.512863);
            glassPath.closePath();

            g.setColor(new Color(0x68_00_00_00, true));
            g.fill(glassPath);

//            if (dark) {
//                g.setPaint(Themes.LIGHT_ICON_COLOR);
//            } else {
//                g.setPaint(new Color(0xA5_00_00_00, true));
//            }
            g.setColor(color);

            g.setStroke(new BasicStroke(0.9106483f, CAP_BUTT, JOIN_MITER, 4));
            g.draw(glassPath);

            Path2D handlePath = new Path2D.Float();
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

//            if (dark) {
//                g.setColor(Themes.LIGHTER_ICON_COLOR);
//            } else {
//                g.setColor(Color.BLACK);
//            }
            g.fill(handlePath);
            g.setStroke(new BasicStroke(0.90921646f, CAP_BUTT, JOIN_MITER, 4));
            g.draw(handlePath);
        }
    }
}
