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

import pixelitor.gui.GUIText;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.Filterable;

import javax.swing.*;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;

/**
 * A composite {@link FilterParam} that groups child parameters
 * and displays them in a modal dialog.
 */
public class DialogParam extends AbstractFilterParam {
    private final FilterParam[] children;
    private ResetButton resetButton;

    public DialogParam(String name, FilterParam... children) {
        super(name, ALLOW_RANDOMIZE);
        this.children = children;
    }

    @Override
    public JComponent createGUI() {
        resetButton = new ResetButton(this);
        paramGUI = new ConfigureParamGUI(this::configureDialog, resetButton);
        guiCreated();
        return (JComponent) paramGUI;
    }

    private void configureDialog(DialogBuilder builder) {
        builder
            .content(GUIUtils.createVerticalPanel(List.of(children)))
            .title(getName())
            .withScrollbars()
            .okText(GUIText.CLOSE_DIALOG)
            .noCancelButton();
    }

    @Override
    protected void doRandomize() {
        for (FilterParam child : children) {
            child.randomize();
        }
        updateResetButtonState();
    }

    @Override
    public void adaptToContext(Filterable layer, boolean changeValue) {
        for (FilterParam child : children) {
            child.adaptToContext(layer, changeValue);
        }
    }

    @Override
    public CompositeParamState copyState() {
        return new CompositeParamState(children);
    }

    @Override
    public void loadStateFrom(ParamState<?> state, boolean updateGUI) {
        CompositeParamState newStates = (CompositeParamState) state;
        Iterator<ParamState<?>> stateIterator = newStates.iterator();
        for (FilterParam child : children) {
            // this matching only works for animation
            if (child.isAnimatable()) {
                child.loadStateFrom(stateIterator.next(), updateGUI);
            }
        }
    }

    @Override
    public void loadStateFrom(String savedValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        for (FilterParam child : children) {
            preset.put(child.getName(), child.copyState().toSaveString());
        }
    }

    @Override
    public void loadStateFrom(UserPreset preset) {
        for (FilterParam child : children) {
            String savedString = preset.get(child.getPresetKey());
            if (savedString != null) { // presets don't have to include everything
                child.loadStateFrom(savedString);
            }
        }
        updateResetButtonState();
    }

    @Override
    public boolean isAnimatable() {
        for (FilterParam child : children) {
            if (child.isAnimatable()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setEnabled(boolean b, EnabledReason reason) {
        // call super to set the enabled state of the launching button
        super.setEnabled(b, reason);

        for (FilterParam child : children) {
            child.setEnabled(b, reason);
        }
    }

    @Override
    public boolean hasDefault() {
        for (FilterParam child : children) {
            if (!child.hasDefault()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void reset(boolean trigger) {
        for (FilterParam param : children) {
            param.reset(false);
        }
        if (trigger) {
            adjustmentListener.paramAdjusted();
        } else {
            // This class updates the reset button state
            // by putting a decorator on the adjustment
            // listeners, so this needs to be called here manually.
            updateResetButtonState();
        }
    }

    private void updateResetButtonState() {
        if (resetButton != null) {
            resetButton.updateState();
        }
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        ParamAdjustmentListener decoratedListener = () -> {
            updateResetButtonState();
            listener.paramAdjusted();
        };

        super.setAdjustmentListener(decoratedListener);

        for (FilterParam child : children) {
            child.setAdjustmentListener(decoratedListener);
        }
    }

    @Override
    public List<Object> getParamValue() {
        return Stream.of(children)
            .map(FilterParam::getParamValue)
            .collect(toList());
    }
}
