/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.colors.FgBgColors;
import pixelitor.gui.ImageComponent;
import pixelitor.layers.Drawable;
import pixelitor.utils.Cursors;
import pixelitor.utils.Messages;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * The color picker tool
 */
public class ColorPickerTool extends Tool {
    private static final String SAMPLE_LABEL_TEXT = "Sample Only the Active Layer/Mask";
    private final JCheckBox sampleLayerOnly = new JCheckBox(SAMPLE_LABEL_TEXT);

    public ColorPickerTool() {
        super('i', "Color Picker", "color_picker_tool_icon.png",
                "click to pick the foreground color, Alt-click (or right-click) to pick the background color",
                Cursors.CROSSHAIR, false, true, false, ClipStrategy.CANVAS);
    }

    @Override
    public void initSettingsPanel() {
        settingsPanel.add(sampleLayerOnly);
    }


    @Override
    public void mousePressed(MouseEvent e, ImageComponent ic) {
        sampleColor(e, ic, e.isAltDown() || SwingUtilities.isRightMouseButton(e));
    }


    @Override
    public void mouseDragged(MouseEvent e, ImageComponent ic) {
        sampleColor(e, ic, e.isAltDown() || SwingUtilities.isRightMouseButton(e));
    }

    @Override
    public void mouseReleased(MouseEvent e, ImageComponent ic) {

    }

    public void sampleColor(MouseEvent e, ImageComponent ic, boolean selectBackground) {
        int x = (int) ic.componentXToImageSpace(e.getX());
        int y = (int) ic.componentYToImageSpace(e.getY());

        BufferedImage img;
        boolean isGray = false;
        if (sampleLayerOnly.isSelected()) {
            if (!ic.activeIsDrawable()) {
                return;
            }

            Drawable dr = ic.getComp().getActiveDrawable();
            img = dr.getImage();
            isGray = img.getType() == BufferedImage.TYPE_BYTE_GRAY;

            x -= dr.getTX();
            y -= dr.getTY();
        } else {
            img = ic.getComp().getCompositeImage();
        }
        int imgWidth = img.getWidth();
        int imgHeight = img.getHeight();

        if (x < imgWidth && y < imgHeight && x >= 0 && y >= 0) {
            int rgb = img.getRGB(x, y);

            showColorInStatusBar(x, y, rgb, isGray);

            Color sampledColor = new Color(rgb);
            if (selectBackground) {
                FgBgColors.setBG(sampledColor);
            } else {
                FgBgColors.setFG(sampledColor);
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

            msg += String.format(", alpha = %d, red = %d, green = %d, blue = %d, hue = %.2f, saturation = %.2f, brightness = %.2f", a, r, g, b, hsbValues[0], hsbValues[1], hsbValues[2]);
        }

        Messages.showInStatusBar(msg);
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = super.getDebugNode();

        node.addBoolean(SAMPLE_LABEL_TEXT, sampleLayerOnly.isSelected());

        return node;
    }
}
