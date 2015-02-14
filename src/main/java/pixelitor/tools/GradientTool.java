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

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.ImageDisplay;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.TmpDrawingLayer;
import pixelitor.utils.BlendingModePanel;

import javax.swing.*;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

import static pixelitor.Composition.ImageChangeActions.FULL;

/**
 * The gradient tool
 */
public class GradientTool extends Tool {
    private boolean thereWasDragging = false;

    private static final String NO_CYCLE_AS_STRING = "No Cycle";
    private static final String REFLECT_AS_STRING = "Reflect";
    private static final String REPEAT_AS_STRING = "Repeat";

    private JComboBox<GradientColorType> colorTypeSelector;
    private JComboBox<GradientType> typeSelector;
    private JComboBox<String> cycleMethodSelector;
    //    private RangeParam opacityParam;
    private JCheckBox invertCheckBox;
    private BlendingModePanel blendingModePanel;

    GradientTool() {
        super('g', "Gradient", "gradient_tool_icon.png", "click and drag to draw a gradient",
                Cursor.getDefaultCursor(), true, true, true, ClipStrategy.IMAGE_ONLY);
    }

    @Override
    public void initSettingsPanel() {
        toolSettingsPanel.add(new JLabel("Type: "));
        typeSelector = new JComboBox<>(GradientType.values());
        toolSettingsPanel.add(typeSelector);

        toolSettingsPanel.add(new JLabel("Cycling: "));
        // cycle methods cannot be put directly in the JComboBox, because they would be all uppercase
        cycleMethodSelector = new JComboBox<>(new String[]{
                NO_CYCLE_AS_STRING,
                REFLECT_AS_STRING,
                REPEAT_AS_STRING});
        toolSettingsPanel.add(cycleMethodSelector);

        toolSettingsPanel.addSeparator();

        toolSettingsPanel.add(new JLabel("Color: "));
        colorTypeSelector = new JComboBox<>(GradientColorType.values());
        toolSettingsPanel.add(colorTypeSelector);

        toolSettingsPanel.add(new JLabel("Invert: "));
        invertCheckBox = new JCheckBox();
        toolSettingsPanel.add(invertCheckBox);

        toolSettingsPanel.addSeparator();

        blendingModePanel = new BlendingModePanel(true);
        toolSettingsPanel.add(blendingModePanel);
    }

    @Override
    public void toolMousePressed(MouseEvent e, ImageDisplay ic) {

    }

    @Override
    public void toolMouseDragged(MouseEvent e, ImageDisplay ic) {
        thereWasDragging = true;  // the gradient will be drawn only when the mouse is released
        ic.repaint();
    }

    @Override
    public void toolMouseReleased(MouseEvent e, ImageDisplay ic) {
        if (thereWasDragging) {
            Composition comp = ic.getComp();

            saveImageForUndo(comp);
            drawGradient((ImageLayer) comp.getActiveLayer(),
                    (GradientType) typeSelector.getSelectedItem(),
                    (GradientColorType) colorTypeSelector.getSelectedItem(),
                    getCycleMethodFromString((String) cycleMethodSelector.getSelectedItem()),
                    blendingModePanel.getComposite(),
                    userDrag,
                    invertCheckBox.isSelected()
            );

            thereWasDragging = false;
            comp.imageChanged(FULL);
        }
    }

    @Override
    public boolean mouseClicked(MouseEvent e, ImageDisplay ic) {
        if (super.mouseClicked(e, ic)) {
            return true;
        }
        thereWasDragging = false;
        return false;
    }

    public static void drawGradient(ImageLayer layer, GradientType type, GradientColorType colorType, MultipleGradientPaint.CycleMethod cycleMethod, Composite composite, UserDrag userDrag, boolean invert) {
        if (userDrag.isClick()) {
            return;
        }

        TmpDrawingLayer tmpDrawingLayer = layer.createTmpDrawingLayer(composite, true);

        Graphics2D g = tmpDrawingLayer.getGraphics();

        // repeated gradients are still jaggy
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color[] colors = {colorType.getStartColor(invert), colorType.getEndColor(invert)};

        Paint gradient = type.getGradient(userDrag, colors, cycleMethod);

        g.setPaint(gradient);

        int width = tmpDrawingLayer.getWidth();
        int height = tmpDrawingLayer.getHeight();
        g.fillRect(0, 0, width, height);
        g.dispose();
        layer.mergeTmpDrawingImageDown();
    }

    @Override
    public void paintOverImage(Graphics2D g2, Canvas canvas, ImageDisplay callingIC, AffineTransform unscaledTransform) {
        if (thereWasDragging) {
            g2.setXORMode(Color.BLACK);
            userDrag.drawLine(g2);
        }
    }

    private static MultipleGradientPaint.CycleMethod getCycleMethodFromString(String s) {
        switch (s) {
            case NO_CYCLE_AS_STRING:
                return MultipleGradientPaint.CycleMethod.NO_CYCLE;
            case REFLECT_AS_STRING:
                return MultipleGradientPaint.CycleMethod.REFLECT;
            case REPEAT_AS_STRING:
                return MultipleGradientPaint.CycleMethod.REPEAT;
        }
        throw new IllegalStateException("should not get here");
    }
}
