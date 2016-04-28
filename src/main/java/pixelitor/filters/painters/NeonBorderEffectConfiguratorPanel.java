/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

package pixelitor.filters.painters;

import com.bric.swing.ColorPicker;
import com.bric.swing.ColorSwatch;
import pixelitor.colors.ColorUtils;
import pixelitor.gui.PixelitorWindow;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 *
 */
public class NeonBorderEffectConfiguratorPanel extends SimpleEffectConfiguratorPanel {
    private Color innerColor;
    private final Color defaultInnerColor;
    private final ColorSwatch innerColorSwatch;

    NeonBorderEffectConfiguratorPanel(boolean defaultSelected, Color defaultColor, Color innerColor, int defaultWidth) {
        super("Neon Border", defaultSelected, defaultColor, defaultWidth);

        this.innerColor = innerColor;
        this.defaultInnerColor = innerColor;
        innerColorSwatch = new ColorSwatch(this.innerColor, BUTTON_SIZE);

        innerColorSwatch.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!e.isPopupTrigger()) {
                    Color selectedColor = ColorPicker.showDialog(PixelitorWindow.getInstance(), "Select Color", NeonBorderEffectConfiguratorPanel.this.innerColor, true);
                    if (selectedColor != null) { // ok was pressed
                        setNewInnerColor(selectedColor, true);
                    }
                }
            }
        });

        ColorUtils.setupFilterColorsPopupMenu(this, innerColorSwatch,
                this::getInnerColor, c -> setNewInnerColor(c, true));

        gbHelper.addLabelWithControl("Inner Color:", innerColorSwatch);
    }

    private void setNewInnerColor(Color selectedColor, boolean trigger) {
        this.innerColor = selectedColor;
        innerColorSwatch.setForeground(this.innerColor);
        innerColorSwatch.paintImmediately(0, 0, BUTTON_SIZE, BUTTON_SIZE);

        if (trigger && adjustmentListener != null) {
            adjustmentListener.paramAdjusted();
        }
        updateDefaultButtonState();
    }

    public Color getInnerColor() {
        return innerColor;
    }

    @Override
    public boolean isSetToDefault() {
        return super.isSetToDefault()
                && innerColor.equals(defaultInnerColor);
    }

    @Override
    public void reset(boolean triggerAction) {
        super.reset(false);
        setNewInnerColor(defaultInnerColor, triggerAction);
    }
}
