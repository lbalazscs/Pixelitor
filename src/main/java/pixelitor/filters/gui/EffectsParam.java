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

package pixelitor.filters.gui;

import pixelitor.filters.painters.AreaEffects;
import pixelitor.filters.painters.EffectsPanel;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;

import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;
import static pixelitor.gui.GUIText.CLOSE_DIALOG;

/**
 * A {@link FilterParam} for shape effects in a dialog
 */
public class EffectsParam extends AbstractFilterParam {
    private EffectsPanel effectsPanel;
    private final boolean separateDialog;

    public EffectsParam(String name) {
        // ignore randomize because effects (especially
        // inner glow) can be very slow in shape filters
        super(name, IGNORE_RANDOMIZE);
        separateDialog = true;
    }

    @Override
    public JComponent createGUI() {
        assert adjustmentListener != null;
        ensureEffectsPanelIsCreated();

        if (separateDialog) {
            var resetButton = new ResetButton(effectsPanel);
            effectsPanel.setResetButton(resetButton);

            var configureParamGUI = new ConfigureParamGUI(
                this::configureDialog, resetButton);

            paramGUI = configureParamGUI;
            guiCreated();
            return configureParamGUI;
        } else {
            effectsPanel.setBorder(createTitledBorder("Effects"));
            return effectsPanel;
        }
    }

    public void configureDialog(DialogBuilder builder) {
        ensureEffectsPanelIsCreated();
        builder
            .title("Effects")
            .content(effectsPanel)
            .withScrollbars()
            .okText(CLOSE_DIALOG)
            .noCancelButton()
            .build();
    }

    public AreaEffects getEffects() {
        ensureEffectsPanelIsCreated();

        return effectsPanel.getEffects();
    }

    private void ensureEffectsPanelIsCreated() {
        if (effectsPanel == null) {
            effectsPanel = new EffectsPanel(null);
            if (adjustmentListener != null) { // the listener was set before this
                effectsPanel.setAdjustmentListener(adjustmentListener);
            }
        }
    }

    public void setEffects(AreaEffects effects) {
        if (effectsPanel == null) { // probably never true
            ensureEffectsPanelIsCreated();
        }

        effectsPanel.setEffects(effects);
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        super.setAdjustmentListener(listener);

        if (effectsPanel != null) { // if the panel was initialized first
            effectsPanel.setAdjustmentListener(listener);
        }
    }

    @Override
    protected void doRandomize() {
        ensureEffectsPanelIsCreated(); // can be necessary in unit tests
        effectsPanel.randomize();
    }

    @Override
    public AreaEffects copyState() {
        return getEffects();
    }

    @Override
    public void loadStateFrom(ParamState<?> state, boolean updateGUI) {
        setEffects((AreaEffects) state);
    }

    @Override
    public void loadStateFrom(String savedValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadStateFrom(UserPreset preset) {
        if (effectsPanel == null) {
            if (!EventQueue.isDispatchThread()) {
                // happens while loading smart filters from pxc
                try {
                    EventQueue.invokeAndWait(this::ensureEffectsPanelIsCreated);
                } catch (InterruptedException | InvocationTargetException e) {
                    Messages.showException(e);
                }
            } else {
                // for safety
                ensureEffectsPanelIsCreated();
            }
        }
        effectsPanel.loadStateFrom(preset);
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        if (effectsPanel == null) { // can happen in tests
            ensureEffectsPanelIsCreated();
        }
        effectsPanel.saveStateTo(preset);
    }

    @Override
    public boolean isAnimatable() {
        return true;
    }

    @Override
    public boolean hasDefault() {
        if (effectsPanel != null) {
            return effectsPanel.hasDefault();
        }
        return true;
    }

    @Override
    public void reset(boolean trigger) {
        if (effectsPanel != null) {
            effectsPanel.reset(trigger);
        }
    }

    @Override
    public AreaEffects getParamValue() {
        return getEffects();
    }
}
