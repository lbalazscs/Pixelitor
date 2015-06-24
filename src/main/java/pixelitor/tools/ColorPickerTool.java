/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

import pixelitor.AppLogic;
import pixelitor.ImageDisplay;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layers;

import javax.swing.*;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * The color picker tool
 */
public class ColorPickerTool extends Tool {
    private final JCheckBox sampleLayerOnly = new JCheckBox("Sample Active Layer Only");

    public ColorPickerTool() {
        super('i', "Color Picker", "color_picker_tool_icon.png",
                "click to pick the foreground color, Alt-click (or right-click) to pick the background color",
                Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR), false, true, false, ClipStrategy.IMAGE_ONLY);
    }

    @Override
    public void initSettingsPanel() {
        settingsPanel.add(sampleLayerOnly);
    }


    @Override
    public void mousePressed(MouseEvent e, ImageDisplay ic) {
        sampleColor(e, ic, e.isAltDown() || SwingUtilities.isRightMouseButton(e));
    }


    @Override
    public void mouseDragged(MouseEvent e, ImageDisplay ic) {
        sampleColor(e, ic, e.isAltDown() || SwingUtilities.isRightMouseButton(e));
    }

    @Override
    public void mouseReleased(MouseEvent e, ImageDisplay ic) {

    }

    public void sampleColor(MouseEvent e, ImageDisplay ic, boolean selectBackground) {
        int x = ic.componentXToImageSpace(e.getX());
        int y = ic.componentYToImageSpace(e.getY());

        BufferedImage img;
        if (sampleLayerOnly.isSelected()) {
            if (!Layers.activeIsImageLayer()) {
                return;
            }

            ImageLayer layer = ic.getComp().getActiveImageLayer();
            img = layer.getImage();

            x -= layer.getTranslationX();
            y -= layer.getTranslationY();
        } else {
            img = ic.getComp().getCompositeImage();
        }
        int imgWidth = img.getWidth();
        int imgHeight = img.getHeight();

        if (x < imgWidth && y < imgHeight && x >= 0 && y >= 0) {
            int rgb = img.getRGB(x, y);

            showColorInStatusBar(x, y, rgb);

            Color sampledColor = new Color(rgb);
            if (selectBackground) {
                FgBgColorSelector.INSTANCE.setBgColor(sampledColor);
            } else {
                FgBgColorSelector.INSTANCE.setFgColor(sampledColor);
            }
        }
    }

    private static void showColorInStatusBar(int x, int y, int rgb) {
        int a = (rgb >>> 24) & 0xFF;
        int r = (rgb >>> 16) & 0xFF;
        int g = (rgb >>> 8) & 0xFF;
        int b = (rgb) & 0xFF;

        String msg = "x = " + x + ", y = " + y + ", alpha = " + a + ", red = " + r + ", green = " + g + ", blue = " + b;
        AppLogic.setStatusMessage(msg);
    }
}
