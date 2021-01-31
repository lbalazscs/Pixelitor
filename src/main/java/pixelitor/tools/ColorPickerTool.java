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

import pixelitor.gui.View;
import pixelitor.layers.Drawable;
import pixelitor.layers.Layer;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;
import pixelitor.utils.Messages;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.Color;
import java.awt.image.BufferedImage;

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
        super("Color Picker", 'I', "color_picker_tool_icon.png",
            HELP_TEXT, Cursors.CROSSHAIR, ClipStrategy.CANVAS);
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
        if (sampleLayerOnly.isSelected()) {
            if (!view.activeIsDrawable()) {
                return;
            }
            Drawable dr = view.getComp().getActiveDrawable();
            img = dr.getImage();
            isGray = img.getType() == TYPE_BYTE_GRAY;

            x -= dr.getTx();
            y -= dr.getTy();
        } else {
            img = view.getComp().getCompositeImage();
        }
        int imgWidth = img.getWidth();
        int imgHeight = img.getHeight();

        if (x < imgWidth && y < imgHeight && x >= 0 && y >= 0) {
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
    public void editedObjectChanged(Layer layer) {
        if (sampleLayerOnly.isSelected()) {
            // don't show the values for the old layer
            Messages.showPlainInStatusBar("");
        }
    }

    @Override
    public DebugNode createDebugNode() {
        var node = super.createDebugNode();

        node.addBoolean(SAMPLE_LABEL_TEXT, sampleLayerOnly.isSelected());

        return node;
    }
}
