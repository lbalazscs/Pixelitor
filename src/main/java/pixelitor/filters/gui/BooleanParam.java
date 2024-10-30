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

import pixelitor.utils.Rnd;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.lang.String.format;
import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

/**
 * A filter parameter for managing a boolean value.
 * It's represented by a checkbox in the GUI.
 */
public class BooleanParam extends AbstractFilterParam {
    private final boolean defaultValue;
    private boolean currentValue;
    private final boolean addResetButton;
    private List<ItemListener> pendingItemListeners;

    public BooleanParam(String name, boolean defaultValue) {
        this(name, defaultValue, ALLOW_RANDOMIZE);
    }

    public BooleanParam(String name, boolean defaultValue, RandomizePolicy randomizePolicy) {
        this(name, defaultValue, randomizePolicy, false);
    }

    public BooleanParam(String name, boolean defaultValue, RandomizePolicy randomizePolicy, boolean addResetButton) {
        super(name, randomizePolicy);

        this.defaultValue = defaultValue;
        this.currentValue = defaultValue;
        this.addResetButton = addResetButton;
    }

    @Override
    public JComponent createGUI() {
        var gui = new BooleanParamGUI(this, addResetButton, action);
        paramGUI = gui;
        guiCreated();

        if (pendingItemListeners != null) {
            // The item listeners for the GUI were temporarily stored here.
            for (ItemListener listener : pendingItemListeners) {
                gui.addItemListener(listener);
            }
            pendingItemListeners = null; 
        }

        return gui;
    }

    public static BooleanParam forHPSharpening() {
        return new BooleanParam("High-Pass Sharpening",
            false, IGNORE_RANDOMIZE);
    }

    /**
     * Configures another  {@link FilterSetting} to be enabled
     * when this one is checked.
     */
    public void setupEnableOtherIfChecked(FilterSetting other) {
        setupDependentOther(other, true);
    }

    /**
     * Configures another  {@link FilterSetting} to be disabled
     * when this one is checked.
     */
    public void setupDisableOtherIfChecked(FilterSetting other) {
        setupDependentOther(other, false);
    }

    private void setupDependentOther(FilterSetting other, boolean enableWhenChecked) {
        other.setEnabled(enableWhenChecked
            ? isChecked()
            : !isChecked());

        // Uses an ItemListener because a ChangeListener fires too much, even for
        // rollover, and an ActionListener ignores changes caused by randomize
        addItemListener(e -> {
            // isChecked() isn't returning the correct new value yet
            boolean checked = e.getStateChange() == ItemEvent.SELECTED;
            other.setEnabled(enableWhenChecked
                ? checked
                : !checked);
        });
    }

    @Override
    public boolean hasDefault() {
        return defaultValue == currentValue;
    }

    @Override
    public void reset(boolean trigger) {
        setValue(defaultValue, true, trigger);
    }

    @Override
    protected void doRandomize() {
        setValue(Rnd.nextBoolean(), true, false);
    }

    public boolean isChecked() {
        return currentValue;
    }

    public String isCheckedStr() {
        return currentValue ? "1" : "0";
    }

    public void setValue(boolean newValue, boolean updateGUI, boolean trigger) {
        if (currentValue == newValue) {
            return;
        }

        currentValue = newValue;
        if (trigger && adjustmentListener != null) {
            adjustmentListener.paramAdjusted();
        }
        if (updateGUI && paramGUI != null) {
            paramGUI.updateGUI();
        }
    }

    @Override
    public boolean isAnimatable() {
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

    private void addItemListener(ItemListener itemListener) {
        if (paramGUI != null) {
            // if a GUI was already created, pass the listener to it
            ((BooleanParamGUI) paramGUI).addItemListener(itemListener);
            return;
        }

        // If there is no GUI, store the listener so that
        // it can be added to the GUI as soon as the GUI is created.
        if (pendingItemListeners == null) {
            pendingItemListeners = new ArrayList<>(2);
        }
        pendingItemListeners.add(itemListener);
    }

    @Override
    public Boolean getParamValue() {
        return isChecked();
    }

    @Override
    public String toString() {
        return format("%s[name = '%s', currentValue = %s]",
            getClass().getSimpleName(), getName(), currentValue);
    }

    /**
     * Represents the possible states of a {@link BooleanParam}.
     */
    public enum BooleanParamState implements ParamState<BooleanParamState> {
        YES, NO;

        @Serial
        private static final long serialVersionUID = 1L;

        private final String saveString;

        BooleanParamState() {
            this.saveString = super.toString().toLowerCase(Locale.ENGLISH);
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
