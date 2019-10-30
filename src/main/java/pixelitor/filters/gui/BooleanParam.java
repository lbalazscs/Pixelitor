/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
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
    private final boolean addDefaultButton;
    private List<ActionListener> actionListenerList;

    public BooleanParam(String name, boolean defaultV) {
        this(name, defaultV, ALLOW_RANDOMIZE);
    }

    public BooleanParam(String name, boolean defaultV, RandomizePolicy randomizePolicy) {
        this(name, defaultV, randomizePolicy, false);
    }

    public BooleanParam(String name, boolean defaultV, RandomizePolicy randomizePolicy, boolean addDefaultButton) {
        super(name, randomizePolicy);
        this.defaultValue = defaultV;
        currentValue = defaultV;
        this.addDefaultButton = addDefaultButton;
    }

    @Override
    public JComponent createGUI() {
        BooleanParamGUI gui = new BooleanParamGUI(this, addDefaultButton, action);
        paramGUI = gui;
        setParamGUIEnabledState();

        if (actionListenerList != null) {
            // some action listeners for the GUI
            // were temporarily stored here

            for (ActionListener listener : actionListenerList) {
                gui.addActionListener(listener);
            }
            actionListenerList.clear();
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
        other.setEnabled(isChecked(), EnabledReason.APP_LOGIC);
        addActionListener(e -> {
            // strangely this seems to work without invokeLater,
            // but for safety call it after the pending events are processed
            EventQueue.invokeLater(() ->
                    other.setEnabled(isChecked(), EnabledReason.APP_LOGIC));
        });
    }

    /**
     * Sets up the automatic disabling of another {@link FilterSetting}
     * whenever this one is checked.
     */
    public void setupDisableOtherIfChecked(FilterSetting other) {
        other.setEnabled(!isChecked(), EnabledReason.APP_LOGIC);

        addActionListener(e -> {
            // invoke later because when this listener is called,
            // isChecked does not contain yet the updated value.
            EventQueue.invokeLater(() ->
                    other.setEnabled(!isChecked(), EnabledReason.APP_LOGIC));
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
    public int getNumGridBagCols() {
        return 2;
    }

    @Override
    public void randomize() {
        if (randomizePolicy.allow()) {
            boolean randomValue = Rnd.nextBoolean();
            setValue(randomValue, true, false);
        }
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
    public void considerImageSize(Rectangle bounds) {
    }

    @Override
    public boolean canBeAnimated() {
        return false;
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
    public String toString() {
        return format("%s[name = '%s', currentValue = %s]",
                getClass().getSimpleName(), getName(), currentValue);
    }

    public void addActionListener(ActionListener actionListener) {
        if (paramGUI != null) {
            // if a GUI was already created, pass the listener to it
            BooleanParamGUI selector = (BooleanParamGUI) paramGUI;
            selector.addActionListener(actionListener);
            return;
        }

        // if there is no GUI, store the listener so that
        // it can be added to the GUI as soon as the GUI is created
        if (actionListenerList == null) {
            actionListenerList = new ArrayList<>(2);
        }
        actionListenerList.add(actionListener);
    }
}
