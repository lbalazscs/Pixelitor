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

package pixelitor.filters.gui;

import java.awt.Rectangle;
import java.util.Objects;

import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;

/**
 * A base class for {@link FilterParam} implementations.
 */
public abstract class AbstractFilterParam implements FilterParam {
    private final String name;
    protected ParamAdjustmentListener adjustmentListener;
    private boolean enabledByAnimation = true;
    private boolean enabledByAppLogic = true;
    protected ParamGUI paramGUI;
    private RandomizePolicy randomizePolicy;

    // an extra action button to the right of the normal GUI,
    // typically some randomization, which will be enabled
    // only for certain values of this filter parameter
    protected FilterButtonModel action;

    AbstractFilterParam(String name, RandomizePolicy randomizePolicy) {
        this.name = Objects.requireNonNull(name);
        this.randomizePolicy = randomizePolicy;
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
    public void considerImageSize(Rectangle bounds) {
        // by default do nothing, most controls are unaffected
    }

    @Override
    public void setEnabled(boolean b, EnabledReason reason) {
        switch (reason) {
            case APP_LOGIC:
                enabledByAppLogic = b;
                break;
            case FINAL_ANIMATION_SETTING:
                if (canBeAnimated()) {
                    // the whole point of the final animation setting mode
                    // is to disable/enable the filter params that can't be animated
                    return;
                }
                enabledByAnimation = b;
                break;
        }

        setEnabledState();
    }

    protected void setEnabledState() {
        if (paramGUI != null) {
            setGUIEnabledState();
        }
    }

    protected void setGUIEnabledState() {
        boolean b = shouldBeEnabled();
        paramGUI.setEnabled(b);
    }

    protected boolean shouldBeEnabled() {
        return enabledByAppLogic && enabledByAnimation;
    }

    @Override
    public boolean allowRandomize() {
        return randomizePolicy == ALLOW_RANDOMIZE;
    }

    @Override
    public void randomize() {
        if (allowRandomize()) {
            doRandomize();
        }
    }

    /**
     * Randomize the settings without checking the permission,
     * and without triggering the filter
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
        }
    }

    @Override
    public String getResetToolTip() {
        return "<html>Reset the value of <b>" + name + "</b>";
    }
}
