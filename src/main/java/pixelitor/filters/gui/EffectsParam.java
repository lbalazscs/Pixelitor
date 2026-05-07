/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import static pixelitor.gui.GUIText.CLOSE_DIALOG;

/**
 * A {@link FilterParam} that configures shape effects via a dialog.
 */
public class EffectsParam extends AbstractFilterParam {
    private EffectsPanel effectsPanel;

    public EffectsParam(String name) {
        // ignore randomize because effects (especially
        // inner glow) can be very slow in shape filters
        super(name, RandomizeMode.IGNORE);
    }

    @Override
    public JComponent createGUI() {
        assert adjustmentListener != null;
        ensureEffectsPanelCreated();

        var resetButton = new ResetButton(effectsPanel);
        effectsPanel.setResetButton(resetButton);

        var launcherGUI = new DialogLauncherGUI(
            this::configureDialog, resetButton);

        paramGUI = launcherGUI;
        syncWithGui();
        return launcherGUI;
    }

    /**
     * Configures the effects dialog using the provided builder.
     */
    public void configureDialog(DialogBuilder builder) {
        ensureEffectsPanelCreated();
        builder
            .title("Effects")
            .content(effectsPanel)
            .withScrollbars()
            .okText(CLOSE_DIALOG)
            .noCancelButton();
    }

    /**
     * Gets the current effects configuration from the GUI panel.
     */
    public AreaEffects getEffects() {
        ensureEffectsPanelCreated();
        return effectsPanel.getEffects();
    }

    private void ensureEffectsPanelCreated() {
        if (effectsPanel == null) {
            effectsPanel = new EffectsPanel(null);
            if (adjustmentListener != null) { // the listener was set before this
                effectsPanel.setAdjustmentListener(adjustmentListener);
            }
        }
    }

    /**
     * Sets the effects configuration and updates the GUI panel.
     */
    public void setEffects(AreaEffects effects) {
        assert effectsPanel != null;
        if (effectsPanel == null) { // probably never true
            ensureEffectsPanelCreated();
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
        ensureEffectsPanelCreated(); // can be necessary in unit tests
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
                    EventQueue.invokeAndWait(this::ensureEffectsPanelCreated);
                } catch (InterruptedException | InvocationTargetException e) {
                    Messages.showException(e);
                }
            } else {
                // for safety
                ensureEffectsPanelCreated();
            }
        }
        effectsPanel.loadStateFrom(preset);
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        if (effectsPanel == null) { // can happen in tests
            ensureEffectsPanelCreated();
        }
        effectsPanel.saveStateTo(preset);
    }

    @Override
    public boolean isAnimatable() {
        return true;
    }

    @Override
    public boolean isAtDefault() {
        if (effectsPanel != null) {
            return effectsPanel.isAtDefault();
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
    public String getValueAsString() {
        return getEffects().toString();
    }
}
