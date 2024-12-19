/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.utils.DropDownSlider;
import pixelitor.layers.BlendingMode;
import pixelitor.layers.Layer;
import pixelitor.utils.Rnd;

import javax.swing.*;
import java.awt.Composite;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;

import static java.awt.FlowLayout.LEFT;
import static pixelitor.utils.Texts.i18n;

/**
 * A panel for configuring opacity and blending mode settings.
 */
public class BlendingModePanel extends JPanel {
    private static final String OPACITY_LABEL_TEXT = i18n("opacity") + ":";

    // threshold above which opacity is considered fully opaque
    public static final float FULLY_OPAQUE_THRESHOLD = 0.999f;

    protected final DropDownSlider opacityDDSlider;
    protected final JComboBox<BlendingMode> bmCombo;
    private final JLabel opacityLabel;
    private final JLabel bmLabel;

    // the available blending modes when editing a layer (as opposed
    // to a layer group, which has an extra "Pass Through" blending mode)
    protected final ComboBoxModel<BlendingMode> layerModel;

    public BlendingModePanel(boolean detailedLabel) {
        super(new FlowLayout(LEFT));

        opacityLabel = new JLabel(OPACITY_LABEL_TEXT);
        add(opacityLabel);
        opacityDDSlider = new DropDownSlider(0, 100, 100);
        add(opacityDDSlider);

        if (detailedLabel) {
            bmLabel = new JLabel("%, Blending Mode:", SwingConstants.LEFT);
        } else {
            bmLabel = new JLabel("%", SwingConstants.LEFT);
        }
        add(bmLabel);

        layerModel = new DefaultComboBoxModel<>(BlendingMode.LAYER_MODES);
        bmCombo = new JComboBox<>(layerModel);

        // show all modes without scrolling (+1 for PASS_THROUGH in layer groups)
        bmCombo.setMaximumRowCount(layerModel.getSize() + 1);

        bmCombo.setFocusable(false);

        // This generic name is only useful if the panel is in a
        // dialog, the layer subclass sets another name for layers.
        bmCombo.setName("bm");

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

    public void setBlendingMode(BlendingMode bm, Layer newLayer) {
        bmCombo.setSelectedItem(bm);
    }

    public Composite getComposite() {
        return getBlendingMode().getComposite(getOpacity());
    }

    public boolean isNormalAndOpaque() {
        return getBlendingMode() == BlendingMode.NORMAL
            && getOpacity() > FULLY_OPAQUE_THRESHOLD;
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(false);
        opacityLabel.setEnabled(b);
        opacityDDSlider.setEnabled(b);
        bmLabel.setEnabled(b);
        bmCombo.setEnabled(b);
    }

    public void addActionListener(ActionListener al) {
        opacityDDSlider.addActionListener(al);
        bmCombo.addActionListener(al);
    }

    public void randomize() {
        int randomIndex = Rnd.nextInt(bmCombo.getModel().getSize());
        bmCombo.setSelectedIndex(randomIndex);

        int newOpacity = Rnd.nextInt(100);
        opacityDDSlider.setValue(newOpacity);
    }

    public void saveStateTo(UserPreset preset) {
        preset.putFloat("Opacity", getOpacity());
        preset.put("Blending Mode", getBlendingMode().toString());
    }

    public void loadStateFrom(UserPreset preset) {
        setOpacity(preset.getFloat("Opacity", 1.0f));
        setBlendingMode(preset.getEnum("Blending Mode", BlendingMode.class), null);
    }
}
