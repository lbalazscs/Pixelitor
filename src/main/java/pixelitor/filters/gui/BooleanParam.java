/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.utils.Rnd;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

/**
 * A filter parameter for a boolean value.
 */
public class BooleanParam extends AbstractFilterParam {
    private final boolean defaultValue;
    private boolean currentValue;
    private final boolean addResetButton;
    private List<ItemListener> itemListeners;

    public BooleanParam(String name, boolean defaultV) {
        this(name, defaultV, ALLOW_RANDOMIZE);
    }

    public BooleanParam(String name, boolean defaultV, RandomizePolicy randomizePolicy) {
        this(name, defaultV, randomizePolicy, false);
    }

    public BooleanParam(String name, boolean defaultV, RandomizePolicy randomizePolicy, boolean addResetButton) {
        super(name, randomizePolicy);
        defaultValue = defaultV;
        currentValue = defaultV;
        this.addResetButton = addResetButton;
    }

    @Override
    public JComponent createGUI() {
        var gui = new BooleanParamGUI(this, addResetButton, action);
        paramGUI = gui;
        guiCreated();

        if (itemListeners != null) {
            // The item listeners for the GUI were temporarily stored here.
            // This is also necessary because currently createGUI() is called
            // for each new filter invocation => the new GUI has to be set up.
            for (ItemListener listener : itemListeners) {
                gui.addItemListener(listener);
            }
        }

        return gui;
    }

    public static BooleanParam forHPSharpening() {
        return new BooleanParam("High-Pass Sharpening",
            false, IGNORE_RANDOMIZE);
    }

    /**
     * Sets up the automatic enabling of another {@link FilterSetting}
     * whenever this one is checked.
     */
    public void setupEnableOtherIfChecked(FilterSetting other) {
        setupOther(other, true);
    }

    /**
     * Sets up the automatic disabling of another {@link FilterSetting}
     * whenever this one is checked.
     */
    public void setupDisableOtherIfChecked(FilterSetting other) {
        setupOther(other, false);
    }

    private void setupOther(FilterSetting other, boolean enable) {
        other.setEnabled(enable ? isChecked() : !isChecked(), EnabledReason.APP_LOGIC);

        // an item listener because a change listener fires too much, even for
        // rollover, and an action listener ignores changes caused by randomize
        addItemListener(e -> {
            // isChecked() is not returning the correct new value yet
            boolean checked = e.getStateChange() == ItemEvent.SELECTED;
            other.setEnabled(enable ? checked : !checked, EnabledReason.APP_LOGIC);
        });
    }

    @Override
    public boolean isSetToDefault() {
        return defaultValue == currentValue;
    }

    @Override
    public void reset(boolean trigger) {
        setValue(defaultValue, true, trigger);
    }

    @Override
    protected void doRandomize() {
        boolean randomValue = Rnd.nextBoolean();
        setValue(randomValue, true, false);
    }

    public boolean isChecked() {
        return currentValue;
    }

    public void setValue(boolean newValue, boolean updateGUI, boolean trigger) {
        if (currentValue != newValue) {
            currentValue = newValue;
            if (trigger && adjustmentListener != null) {
                adjustmentListener.paramAdjusted();
            }
        }
        if (updateGUI && paramGUI != null) {
            paramGUI.updateGUI();
        }
    }

    @Override
    public boolean canBeAnimated() {
        return false;
    }

    @Override
    public ParamState<?> copyState() {
        return currentValue ? BooleanParamState.YES : BooleanParamState.NO;
    }

    @Override
    public void loadStateFrom(ParamState<?> state, boolean updateGUI) {
        boolean newValue = switch ((BooleanParamState) state) {
            case YES -> true;
            case NO -> false;
        };
        setValue(newValue, updateGUI, false);
    }

    @Override
    public void loadStateFrom(String savedValue) {
        boolean newValue = switch (savedValue) {
            case "yes" -> true;
            case "no" -> false;
            default -> throw new IllegalStateException("Unexpected value: " + savedValue);
        };
        setValue(newValue, true, false);
    }

    // actionListener doesn't react to programmatic setSelected calls
    private void addItemListener(ItemListener itemListener) {
        if (paramGUI != null) {
            // if a GUI was already created, pass the listener to it
            ((BooleanParamGUI) paramGUI).addItemListener(itemListener);
            return;
        }

        // if there is no GUI, store the listener so that
        // it can be added to the GUI as soon as the GUI is created
        if (itemListeners == null) {
            itemListeners = new ArrayList<>(2);
        }
        itemListeners.add(itemListener);
    }

    @Override
    public Object getParamValue() {
        return isChecked();
    }

    @Override
    public String toString() {
        return format("%s[name = '%s', currentValue = %s]",
            getClass().getSimpleName(), getName(), currentValue);
    }

    public enum BooleanParamState implements ParamState<BooleanParamState> {
        YES, NO;

        @Serial
        private static final long serialVersionUID = 1L;

        private final String saveString;

        BooleanParamState() {
            this.saveString = super.toString().toLowerCase();
        }

        @Override
        public BooleanParamState interpolate(BooleanParamState endState, double progress) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toSaveString() {
            return saveString;
        }
    }
}
