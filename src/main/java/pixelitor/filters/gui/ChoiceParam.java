/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.io.Serial;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Base class for filter parameters that have a JComboBox in their GUI.
 */
public class ChoiceParam<E> extends AbstractFilterParam implements ComboBoxModel<E> {
    protected List<E> choices;
    protected E defaultValue;
    protected E selectedValue;
    private final EventListenerList eventListeners = new EventListenerList();

    public ChoiceParam(String name, E[] choices) {
        this(name, choices, RandomizeMode.ALLOW_RANDOMIZE);
    }

    public ChoiceParam(String name, E[] choices, E defaultValue) {
        this(name, List.of(choices), defaultValue, RandomizeMode.ALLOW_RANDOMIZE);
    }

    public ChoiceParam(String name, E[] choices, RandomizeMode randomizeMode) {
        this(name, List.of(choices), choices[0], randomizeMode);
    }

    public ChoiceParam(String name, List<E> choices, E defaultValue,
                       RandomizeMode randomizeMode) {
        super(name, randomizeMode);
        this.choices = choices;
        this.defaultValue = defaultValue;
        this.selectedValue = defaultValue;
    }

    @Override
    public JComponent createGUI() {
        var gui = new ChoiceParamGUI<>(this, sideButtonModel);
        paramGUI = gui;
        syncWithGui();
        return gui;
    }

    @Override
    public boolean isAtDefault() {
        return defaultValue.equals(selectedValue);
    }

    @Override
    public void reset(boolean trigger) {
        setSelectedItem(defaultValue, trigger);
    }

    @Override
    protected void doRandomize() {
        setSelectedValue(Rnd.chooseFrom(choices), false);
    }

    private void setSelectedValue(E newChoice, boolean trigger) {
        setSelectedItem(newChoice, trigger);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setSelectedItem(Object item) {
        setSelectedItem((E) item, true);
    }

    /**
     * Sets the selected value and optionally triggers an adjustment event.
     */
    public void setSelectedItem(E item, boolean trigger) {
        assert item != null;
        if (selectedValue.equals(item)) {
            return;
        }
        selectedValue = item;
        fireContentsChanged(this, -1, -1);

        if (trigger && adjustmentListener != null) {
            adjustmentListener.paramAdjusted();
        }
    }

    public void setChoices(List<E> newChoices, boolean keepSelection) {
        if (newChoices == null || newChoices.isEmpty()) {
            throw new IllegalArgumentException();
        }
        E oldSelection = selectedValue;
        this.choices = List.copyOf(newChoices);

        if (keepSelection && oldSelection != null && choices.contains(oldSelection)) {
            selectedValue = oldSelection;
        } else {
            selectedValue = choices.getFirst();
        }

        fireContentsChanged(this, 0, getSize() - 1);

        if (adjustmentListener != null) {
            adjustmentListener.paramAdjusted();
        }
    }

    @Override
    public Object getSelectedItem() {
        return selectedValue;
    }

    public E getSelected() {
        return selectedValue;
    }

    @Override
    public String getValueAsString() {
        return selectedValue.toString();
    }

    @Override
    public int getSize() {
        return choices.size();
    }

    @Override
    public E getElementAt(int index) {
        return choices.get(index);
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        eventListeners.add(ListDataListener.class, l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        eventListeners.remove(ListDataListener.class, l);
    }

    private void fireContentsChanged(Object source, int index0, int index1) {
        Object[] listeners = eventListeners.getListenerList();
        ListDataEvent e = null;

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ListDataListener.class) {
                if (e == null) {
                    e = new ListDataEvent(source,
                        ListDataEvent.CONTENTS_CHANGED, index0, index1);
                }
                ((ListDataListener) listeners[i + 1]).contentsChanged(e);
            }
        }
    }

    @Override
    public boolean isAnimatable() {
        return false;
    }

    @Override
    public String getResetToolTip() {
        return super.getResetToolTip() + " to " + defaultValue;
    }

    @Override
    public ChoiceParamState<E> copyState() {
        return new ChoiceParamState<>(getSelected());
    }

    @Override
    public void loadStateFrom(ParamState<?> state, boolean updateGUI) {
        @SuppressWarnings("unchecked")
        ChoiceParamState<E> paramState = (ChoiceParamState<E>) state;
        setSelectedItem(paramState.value(), false);
    }

    @Override
    public void loadStateFrom(String savedValue) {
        int numEntries = getSize();
        for (int i = 0; i < numEntries; i++) {
            E item = getElementAt(i);
            if (item.toString().equals(savedValue)) {
                setSelectedItem(item, false);
                break;
            }
        }
    }

    /**
     * Sets up the automatic enabling of another {@link FilterSetting} depending on the selected item.
     */
    public void setupEnableOtherIf(FilterSetting other,
                                   Predicate<E> condition) {
        setupOther(other, condition, true);
    }

    /**
     * Sets up the automatic disabling of another {@link FilterSetting} depending on the selected item.
     */
    public void setupDisableOtherIf(FilterSetting other,
                                    Predicate<E> condition) {
        setupOther(other, condition, false);
    }

    private void setupOther(FilterSetting other, Predicate<E> condition, boolean enable) {
        other.setEnabled(!enable);
        addOnChangeTask(() -> {
            if (condition.test(getSelected())) {
                other.setEnabled(enable);
            } else {
                other.setEnabled(!enable);
            }
        });
    }

    /**
     * Sets up the automatic limiting of a {@link RangeParam} to a maximum value that depends on the selected item.
     */
    public void setupLimitOtherToMax(RangeParam other, ToIntFunction<E> mapper) {
        addOnChangeTask(() -> {
            int maxValue = mapper.applyAsInt(getSelected());
            if (other.getValue() > maxValue) {
                other.setValueNoTrigger(maxValue);
            }
        });
    }

    /**
     * Adds a listener that will be notified when the selected value changes.
     */
    public void addOnChangeTask(Runnable task) {
        addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                task.run();
            }
        });
    }

    /**
     * Encapsulates the state of a {@link ChoiceParam} as a memento object.
     */
    public record ChoiceParamState<E>(E value) implements ParamState<ChoiceParamState<E>> {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public ChoiceParamState<E> interpolate(ChoiceParamState<E> endState, double progress) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toSaveString() {
            return value.toString();
        }
    }
}
