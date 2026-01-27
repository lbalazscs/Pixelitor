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

import pixelitor.utils.Rnd;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.lang.String.format;
import static pixelitor.filters.gui.RandomizeMode.ALLOW_RANDOMIZE;
import static pixelitor.filters.gui.RandomizeMode.IGNORE_RANDOMIZE;

/**
 * A filter parameter for managing a boolean value, represented by a checkbox in the GUI.
 */
public class BooleanParam extends AbstractFilterParam {
    private final boolean defaultValue;
    private boolean value;
    private final boolean addResetButton;
    private List<ItemListener> pendingItemListeners;

    public BooleanParam(String name) {
        this(name, false, ALLOW_RANDOMIZE);
    }

    public BooleanParam(String name, boolean defaultValue) {
        this(name, defaultValue, ALLOW_RANDOMIZE);
    }

    public BooleanParam(String name, boolean defaultValue, RandomizeMode randomizeMode) {
        this(name, defaultValue, randomizeMode, false);
    }

    public BooleanParam(String name, boolean defaultValue, RandomizeMode randomizeMode, boolean addResetButton) {
        super(name, randomizeMode);

        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.addResetButton = addResetButton;
    }

    @Override
    public JComponent createGUI() {
        var gui = new BooleanParamGUI(this, addResetButton, sideButtonModel);
        paramGUI = gui;
        syncWithGui();

        if (pendingItemListeners != null) {
            // add any item listeners that were queued before the GUI was created
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
     * Configures another {@link FilterSetting} to be enabled when this one is checked.
     */
    public void setupEnableOtherIfChecked(FilterSetting other) {
        setupDependentOther(other, true);
    }

    /**
     * Configures another {@link FilterSetting} to be disabled when this one is checked.
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
    public boolean isAtDefault() {
        return defaultValue == value;
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
        return value;
    }

    /**
     * Returns the checked state in the format expected by G'MIC
     */
    public String isCheckedStr() {
        return value ? "1" : "0";
    }

    public void setValue(boolean newValue, boolean updateGUI, boolean trigger) {
        if (value == newValue) {
            return;
        }

        value = newValue;
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
        return value ? BooleanParamState.YES : BooleanParamState.NO;
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
    public String getValueAsString() {
        return isChecked() ? "yes" : "no";
    }

    @Override
    public String toString() {
        return format("%s[name = '%s', value = %s]",
            getClass().getSimpleName(), getName(), value);
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
