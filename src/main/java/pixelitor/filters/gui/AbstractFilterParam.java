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

package pixelitor.filters.gui;

import pixelitor.utils.debug.DebugNode;

import java.util.Objects;

import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;

/**
 * A base class for implementations of {@link FilterParam}.
 */
public abstract class AbstractFilterParam implements FilterParam {
    private final String name;
    protected ParamAdjustmentListener adjustmentListener;
    private boolean enabledByAnimation = true;
    private boolean enabledByAppLogic = true;
    protected ParamGUI paramGUI;
    private RandomizePolicy randomizePolicy;
    private String toolTip;
    private String presetKey;

    // If this is not null, it's the model of an additional action button
    // to the right of the normal GUI. Typically it's used for randomization,
    // and it's enabled only for specific values of this filter parameter.
    protected FilterButtonModel action;

    AbstractFilterParam(String name, RandomizePolicy randomizePolicy) {
        this.name = Objects.requireNonNull(name);
        this.randomizePolicy = randomizePolicy;
    }

    /**
     * Called only by subclasses, after initializing the GUI.
     */
    protected void guiCreated() {
        updateGUIEnabledState();
        if (toolTip != null) {
            paramGUI.setToolTip(toolTip);
        }
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        adjustmentListener = listener;
        if (action != null) {
            action.setAdjustmentListener(listener);
        }
    }

    public FilterParam withAction(FilterButtonModel action) {
        this.action = action;
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPresetKey() {
        if (presetKey != null) {
            return presetKey;
        }
        return getName();
    }

    public void setPresetKey(String presetKey) {
        this.presetKey = presetKey;
    }

    @Override
    public void setEnabled(boolean b, EnabledReason reason) {
        switch (reason) {
            case APP_LOGIC -> enabledByAppLogic = b;
            case ANIMATION_ENDING_STATE -> {
                if (isAnimatable()) {
                    // the purpose of the animation ending mode is to
                    // disable/enable the filter params that can't be animated
                    return;
                }
                enabledByAnimation = b;
            }
        }

        updateEnabledState();
    }

    protected void updateEnabledState() {
        if (paramGUI != null) {
            updateGUIEnabledState();
        }
    }

    private void updateGUIEnabledState() {
        paramGUI.setEnabled(isEnabled());
    }

    public boolean isEnabled() {
        return enabledByAppLogic && enabledByAnimation;
    }

    @Override
    public boolean canRandomize() {
        return randomizePolicy == ALLOW_RANDOMIZE && enabledByAppLogic;
    }

    @Override
    public void randomize() {
        if (canRandomize()) {
            doRandomize();
        }
    }

    /**
     * Randomizes the parameter without checking for permission,
     * and without triggering the filter.
     */
    protected abstract void doRandomize();

    @Override
    public void setRandomizePolicy(RandomizePolicy policy) {
        randomizePolicy = policy;
    }

    @Override
    public void setToolTip(String tip) {
        if (paramGUI != null) {
            paramGUI.setToolTip(tip);
        } else {
            // Store for later if the GUI is not created yet
            toolTip = tip;
        }
    }

    @Override
    public String getResetToolTip() {
        return "<html>Reset the value of <b>" + name + "</b>";
    }

    @Override
    public boolean isComplex() {
        return false;
    }

    // used to determine whether two filters are equal
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractFilterParam that = (AbstractFilterParam) o;
        return Objects.equals(getParamValue(), that.getParamValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getParamValue());
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = new DebugNode(key, this);

        node.addString("name", name);
        node.addString("value", getParamValue().toString());

        return node;
    }
}
