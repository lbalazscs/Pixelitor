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

package pixelitor.filters.gui;

import pixelitor.filters.painters.AreaEffects;
import pixelitor.filters.painters.EffectsPanel;
import pixelitor.gui.utils.OKDialog;

import javax.swing.*;
import java.awt.Rectangle;

import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

/**
 * Shape effects in a dialog
 */
public class EffectsParam extends AbstractFilterParam {
    private EffectsPanel effectsPanel;
    private final boolean separateDialog;

    public EffectsParam(String name) {
        super(name, IGNORE_RANDOMIZE); // randomize() is not implemented!
        this.separateDialog = true;
    }

    @Override
    public JComponent createGUI() {
        assert adjustmentListener != null;
        effectsPanel = new EffectsPanel(adjustmentListener, null);

        if (separateDialog) {
            DefaultButton button = new DefaultButton(effectsPanel);
            effectsPanel.setDefaultButton(button);

            ConfigureParamGUI configureParamGUI = new ConfigureParamGUI(owner -> {
                OKDialog effectsDialog = new OKDialog(owner, "Effects", "Close");
                effectsDialog.setupGUI(effectsPanel);
                return effectsDialog;
            }, button);

            paramGUI = configureParamGUI;
            setParamGUIEnabledState();
            return configureParamGUI;
        } else {
            effectsPanel.setBorder(BorderFactory.createTitledBorder("Effects"));
            return effectsPanel;
        }
    }

    public AreaEffects getEffects() {
        // if a GUI filter is executing without a GUI
        // (for example in a RandomGUITest), the panel needs to be created here
        if (effectsPanel == null) {
            effectsPanel = new EffectsPanel(adjustmentListener, null);
        }

        effectsPanel.updateEffectsFromGUI();
        return effectsPanel.getEffects();
    }

    @Override
    public void randomize() {

    }

    @Override
    public void considerImageSize(Rectangle bounds) {

    }

    @Override
    public ParamState copyState() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setState(ParamState state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canBeAnimated() {
        return false;
    }

    @Override
    public int getNrOfGridBagCols() {
        return separateDialog ? 2 : 1;
    }

    @Override
    public boolean isSetToDefault() {
        if (effectsPanel != null) {
            return effectsPanel.isSetToDefault();
        }
        return true;
    }

    @Override
    public void reset(boolean triggerAction) {
        if (effectsPanel != null) {
            effectsPanel.reset(triggerAction);
        }
    }
}
