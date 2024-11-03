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
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.io.Serial;
import java.util.List;
import java.util.function.Predicate;

/**
 * Base class for filter params that have a JComboBox as their GUI.
 */
public class ListParam<E> extends AbstractFilterParam implements ComboBoxModel<E> {
    protected List<E> choices;
    protected E defaultValue;
    protected E selectedValue;
    private final EventListenerList eventListeners = new EventListenerList();

    public ListParam(String name, E[] choices) {
        this(name, choices, RandomizePolicy.ALLOW_RANDOMIZE);
    }

    public ListParam(String name, E[] choices, E defaultValue) {
        this(name, List.of(choices), defaultValue, RandomizePolicy.ALLOW_RANDOMIZE);
    }

    public ListParam(String name, E[] choices, RandomizePolicy randomizePolicy) {
        this(name, List.of(choices), choices[0], randomizePolicy);
    }

    public ListParam(String name, List<E> choices, E defaultValue,
                     RandomizePolicy randomizePolicy) {
        super(name, randomizePolicy);
        this.choices = choices;
        this.defaultValue = defaultValue;
        selectedValue = defaultValue;
    }

    @Override
    public JComponent createGUI() {
        var gui = new ComboBoxParamGUI<>(this, action);
        paramGUI = gui;
        guiCreated();
        return gui;
    }

    @Override
    public boolean hasDefault() {
        return defaultValue.equals(selectedValue);
    }

    @Override
    public void reset(boolean trigger) {
        setSelectedItem(defaultValue, trigger);
    }

    @Override
    protected void doRandomize() {
        E choice = Rnd.chooseFrom(choices);
        setSelectedValue(choice, false);
    }

    private void setSelectedValue(E currentChoice, boolean trigger) {
        setSelectedItem(currentChoice, trigger);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setSelectedItem(Object item) {
        setSelectedItem((E) item, true);
    }

    /**
     * Sets the the selected value and optionally triggers the adjustment listener.
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

    @Override
    public Object getSelectedItem() {
        return selectedValue;
    }

    public E getSelected() {
        return selectedValue;
    }

    @Override
    public Object getParamValue() {
        return selectedValue;
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
    public ListParamState<E> copyState() {
        return new ListParamState<>(getSelected());
    }

    @Override
    public void loadStateFrom(ParamState<?> state, boolean updateGUI) {
        @SuppressWarnings("unchecked")
        ListParamState<E> paramState = (ListParamState<E>) state;
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
     * Sets up the automatic enabling of another {@link FilterSetting}
     * depending on the selected item of this one.
     */
    public void setupEnableOtherIf(FilterSetting other,
                                   Predicate<E> condition) {
        setupOther(other, condition, true);
    }

    /**
     * Sets up the automatic disabling of another {@link FilterSetting}
     * depending on the selected item of this one.
     */
    public void setupDisableOtherIf(FilterSetting other,
                                    Predicate<E> condition) {
        setupOther(other, condition, false);
    }

    @SuppressWarnings("unchecked")
    private void setupOther(FilterSetting other, Predicate<E> condition, boolean enable) {
        other.setEnabled(!enable);
        addOnChangeTask(() -> {
            if (condition.test((E) getSelectedItem())) {
                other.setEnabled(enable);
            } else {
                other.setEnabled(!enable);
            }
        });
    }

    public void setupMaximizeOtherOnChange(RangeParam other, int maxValue) {
        addOnChangeTask(() -> {
            if (other.getValue() > maxValue) {
                other.setValueNoTrigger(maxValue);
            }
        });
    }

    // Adds a listener that will be notified when the selected value changes.
    private void addOnChangeTask(Runnable task) {
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
     * Encapsulates the state of an {@link ListParam} as a memento object.
     */
    public record ListParamState<E>(E value) implements ParamState<ListParamState<E>> {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public ListParamState<E> interpolate(ListParamState<E> endState, double progress) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toSaveString() {
            return value.toString();
        }
    }
}
