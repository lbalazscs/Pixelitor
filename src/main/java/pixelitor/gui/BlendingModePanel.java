/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.gui;

import pixelitor.gui.utils.DropDownSlider;
import pixelitor.layers.BlendingMode;
import pixelitor.utils.Rnd;

import javax.swing.*;
import java.awt.Composite;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;

import static java.awt.FlowLayout.LEFT;
import static pixelitor.utils.Texts.i18n;

/**
 * A GUI selector for opacity and blending mode.
 * Used by tools and layers.
 */
public class BlendingModePanel extends JPanel {
    public static final String OPACITY = i18n("opacity") + ":";

    protected final DropDownSlider opacityDDSlider;
    protected final JComboBox<BlendingMode> bmCombo;
    private final JLabel opacityLabel;
    private final JLabel bmLabel;

    public BlendingModePanel(boolean forTools) {
        setLayout(new FlowLayout(LEFT));
        opacityLabel = new JLabel(OPACITY);
        add(opacityLabel);
        opacityDDSlider = new DropDownSlider(0, 100, 100, true);

        if (!forTools) {
            opacityDDSlider.setTFName("layerOpacity");
        }

        add(opacityDDSlider);

        if (forTools) {
            bmLabel = new JLabel("%, Blending Mode:", SwingConstants.LEFT);
        } else {
            bmLabel = new JLabel("%", SwingConstants.LEFT);
        }
        add(bmLabel);

        BlendingMode[] blendingModes = BlendingMode.values();
        bmCombo = new JComboBox<>(blendingModes);
        bmCombo.setFocusable(false);
        bmCombo.setMaximumRowCount(blendingModes.length);
        if (!forTools) {
            bmCombo.setName("layerBM");
        }
        add(bmCombo);
    }

    public float getOpacity() {
        return opacityDDSlider.getValue() / 100.0f;
    }

    public void setOpacity(float f) {
        opacityDDSlider.setValue((int) (f * 100.0f));
    }

    public BlendingMode getBlendingMode() {
        return (BlendingMode) bmCombo.getSelectedItem();
    }

    public void setBlendingMode(BlendingMode bm) {
        bmCombo.setSelectedItem(bm);
    }

    public Composite getComposite() {
        return getBlendingMode().getComposite(getOpacity());
    }

    public void randomize() {
        BlendingMode[] blendingModes = BlendingMode.values();
        int randomIndex = Rnd.nextInt(blendingModes.length);
        bmCombo.setSelectedIndex(randomIndex);

        int newOpacity = Rnd.nextInt(100);
        opacityDDSlider.setValue(newOpacity);
    }

    @Override
    public void setEnabled(boolean b) {
        opacityLabel.setEnabled(b);
        opacityDDSlider.setEnabled(b);
        bmLabel.setEnabled(b);
        bmCombo.setEnabled(b);
    }

    public void addActionListener(ActionListener al) {
        opacityDDSlider.addActionListener(al);
        bmCombo.addActionListener(al);
    }
}
