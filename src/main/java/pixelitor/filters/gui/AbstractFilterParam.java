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

import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.util.Objects;

/**
 * A base class for implementations of {@link FilterParam}.
 */
public abstract class AbstractFilterParam implements FilterParam {
    private String name;
    protected ParamAdjustmentListener adjustmentListener;
    private boolean enabledByAnimation = true;
    private boolean enabledByFilterLogic = true;
    private boolean enabledByParent = true;
    protected ParamGUI paramGUI;
    private RandomizeMode randomizeMode;
    private String toolTip;
    private String presetKey;
    private String legacyPresetKey;

    // If this is not null, it's the model for an additional action button
    // to the right of the normal GUI. Typically, it's used for randomization,
    // and it's enabled only for specific values of this filter parameter.
    protected FilterButtonModel sideButtonModel;

    protected AbstractFilterParam(String name, RandomizeMode randomizeMode) {
        this.name = Objects.requireNonNull(name);
        this.randomizeMode = randomizeMode;
    }

    /**
     * Finalizes the GUI's setup by synchronizing its state with this model.
     * Must be called by subclasses after creating the GUI.
     */
    protected final <G extends JComponent & ParamGUI> G installGUI(G gui) {
        paramGUI = gui;
        updateGUIEnabledState();
        if (toolTip != null) {
            // if a tooltip was set before the GUI was created,
            // apply that stored tooltip to the newly created GUI
            gui.setToolTip(toolTip);
        }
        return gui;
    }

    @Override
    public void guiDisposed() {
        if (paramGUI == null) {
            throw new IllegalStateException();
        }
        adjustmentListener = null;
        paramGUI = null;
        if (sideButtonModel != null) {
            sideButtonModel.guiDisposed();
        }
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        adjustmentListener = listener;
        if (sideButtonModel != null) {
            sideButtonModel.setAdjustmentListener(listener);
        }
    }

    public FilterParam withSideButton(FilterButtonModel button) {
        sideButtonModel = button;

        if (adjustmentListener != null) {
            button.setAdjustmentListener(adjustmentListener);
        }
        button.setEnabled(enabledByFilterLogic, EnabledReason.FILTER_LOGIC);
        button.setEnabled(enabledByAnimation, EnabledReason.ANIMATION_ENDING_STATE);
        button.setEnabled(enabledByParent, EnabledReason.PARENT_PARAM);

        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String displayName) {
        this.name = displayName;
    }

    @Override
    public String getPresetKey() {
        if (presetKey != null) {
            // using a separate key allows the display name
            // to be changed without breaking users' saved presets
            return presetKey;
        }
        return getName();
    }

    @Override
    public String getLegacyPresetKey() {
        return legacyPresetKey;
    }

    public void setPresetKey(String presetKey) {
        this.presetKey = presetKey;
    }

    public FilterParam withPresetKey(String presetKey) {
        this.presetKey = presetKey;
        return this;
    }

    public FilterParam withLegacyPresetKey(String presetKey) {
        this.legacyPresetKey = presetKey;
        return this;
    }

    @Override
    public void setEnabled(boolean enabled, EnabledReason reason) {
        switch (reason) {
            case FILTER_LOGIC -> enabledByFilterLogic = enabled;
            case ANIMATION_ENDING_STATE -> {
                // animation ending mode only affects non-animatable parameters
                if (!isAnimatable()) {
                    enabledByAnimation = enabled;
                }
            }
            case PARENT_PARAM -> enabledByParent = enabled;
        }

        if (paramGUI != null) {
            updateGUIEnabledState();
        }
        if (sideButtonModel != null) {
            sideButtonModel.setEnabled(enabled, reason);
        }
    }

    private void updateGUIEnabledState() {
        paramGUI.setEnabled(isEnabled());
    }

    /**
     * Notifies the adjustment listener (if there is one), which
     * restarts the filter preview.
     */
    public void triggerFilter() {
        if (adjustmentListener != null) {
            adjustmentListener.paramAdjusted();
        }
    }

    public void updateGUI() {
        if (paramGUI != null) {
            paramGUI.updateGUI();
        }
    }

    @Override
    public boolean isEnabled() {
        return enabledByFilterLogic && enabledByAnimation && enabledByParent;
    }

    @Override
    public boolean shouldRandomize() {
        return randomizeMode == RandomizeMode.ALLOW && enabledByFilterLogic;
    }

    @Override
    public void randomize() {
        if (shouldRandomize()) {
            doRandomize();
        }
    }

    /**
     * Randomizes the parameter without checking if randomization is allowed,
     * and without triggering the filter.
     */
    protected abstract void doRandomize();

    @Override
    public void setEmbedded(FilterParam.Layout layout) {
        // do nothing by default
    }

    @Override
    public void setRandomizeMode(RandomizeMode mode) {
        randomizeMode = mode;
    }

    @Override
    public void setToolTip(String tip) {
        toolTip = tip; // always remember, so re-created GUIs get it too
        if (paramGUI != null) {
            paramGUI.setToolTip(tip);
        }
    }

    @Override
    public String getResetToolTip() {
        return "<html>Reset the value of <b>" + name + "</b>";
    }

    public FilterButtonModel getSideButtonModel() {
        return sideButtonModel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractFilterParam that = (AbstractFilterParam) o;
        return name.equals(that.name) && getValueAsString().equals(that.getValueAsString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, getValueAsString());
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = new DebugNode(key, this);

        node.addString("name", name);
        node.addString("value", getValueAsString());

        return node;
    }
}
