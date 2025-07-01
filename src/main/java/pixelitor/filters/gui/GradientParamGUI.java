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

package pixelitor.filters.gui;

import com.bric.swing.GradientSlider;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.beans.PropertyChangeEvent;
import java.util.Set;

import static com.bric.swing.MultiThumbSlider.HORIZONTAL;

/**
 * The GUI for a {@link GradientParam}.
 */
class GradientParamGUI extends JPanel implements ParamGUI {
    private static final String USE_BEVEL = "GradientSlider.useBevel";
    private static final Set<String> IGNORED_PROPERTIES = Set.of(
        "ancestor", "selected thumb", "enabled", "preferredSize",
        "graphicsConfiguration", "UI", USE_BEVEL
    );

    private final GradientSlider gradientSlider;
    private final GradientParam model;
    private final ResetButton resetButton;

    public GradientParamGUI(GradientParam model) {
        super(new FlowLayout());
        this.model = model;

        // first, create and assign the gradient slider instance
        this.gradientSlider = new GradientSlider(HORIZONTAL,
            model.getThumbPositions(), model.getColors());

        // now that the field is assigned, configure it
        gradientSlider.addPropertyChangeListener(this::sliderPropertyChanged);
        gradientSlider.putClientProperty(USE_BEVEL, "true");
        gradientSlider.setPreferredSize(new Dimension(250, 30));
        add(gradientSlider);

        this.resetButton = new ResetButton(model);
        add(resetButton);
    }

    private void sliderPropertyChanged(PropertyChangeEvent evt) {
        if (shouldNotifyModel(evt)) {
            model.setValues(gradientSlider.getThumbPositions(), gradientSlider.getColors(), true);
        }
    }

    private boolean shouldNotifyModel(PropertyChangeEvent evt) {
        if (gradientSlider.isValueAdjusting()) {
            return false;
        }
        return !IGNORED_PROPERTIES.contains(evt.getPropertyName());
    }

    @Override
    public void updateGUI() {
        gradientSlider.setValues(model.getThumbPositions(), model.getColors());
        resetButton.updateState();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled); // so that isEnabled() works
        gradientSlider.setEnabled(enabled);
        resetButton.setEnabled(enabled);
    }

    @Override
    public void setToolTip(String tip) {
        gradientSlider.setToolTipText(tip);
    }

    @Override
    public boolean isEnabled() {
        return gradientSlider.isEnabled();
    }

    @Override
    public int getNumLayoutColumns() {
        return 2;
    }
}
