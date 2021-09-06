/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import java.util.List;
import java.util.function.Predicate;

/**
 * Base class for filter params that have a JComboBox as their GUI
 */
public abstract class AbstractMultipleChoiceParam<E>
    extends AbstractFilterParam implements ComboBoxModel<E> {
    protected List<E> choices;
    protected E defaultChoice;
    protected E currentChoice;
    private final EventListenerList listenerList = new EventListenerList();

    protected AbstractMultipleChoiceParam(String name,
                                          E[] choices,
                                          RandomizePolicy randomizePolicy) {
        this(name, List.of(choices), choices[0], randomizePolicy);
    }

    protected AbstractMultipleChoiceParam(String name,
                                          List<E> choices,
                                          E defaultChoice,
                                          RandomizePolicy randomizePolicy) {
        super(name, randomizePolicy);
        this.choices = choices;
        this.defaultChoice = defaultChoice;
        currentChoice = defaultChoice;
    }

    @Override
    public JComponent createGUI() {
        var gui = new ComboBoxParamGUI<E>(this, action);
        paramGUI = gui;
        afterGUICreation();
        return gui;
    }

    @Override
    public boolean isSetToDefault() {
        return defaultChoice.equals(currentChoice);
    }

    @Override
    public void reset(boolean trigger) {
        setSelectedItem(defaultChoice, trigger);
    }

    @Override
    protected void doRandomize() {
        E choice = Rnd.chooseFrom(choices);
        setCurrentChoice(choice, false);
    }

    private void setCurrentChoice(E currentChoice, boolean trigger) {
        setSelectedItem(currentChoice, trigger);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setSelectedItem(Object item) {
        setSelectedItem((E) item, true);
    }

    public void setSelectedItem(E item, boolean trigger) {
        if (!currentChoice.equals(item)) {
            currentChoice = item;
            fireContentsChanged(this, -1, -1);
            if (trigger) {
                if (adjustmentListener != null) {  // it's null when called from randomize
                    adjustmentListener.paramAdjusted();
                }
            }
        }
    }

    @Override
    public Object getSelectedItem() {
        return currentChoice;
    }

    public E getSelected() {
        return currentChoice;
    }

    @Override
    public Object getParamValue() {
        return currentChoice;
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
        listenerList.add(ListDataListener.class, l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        listenerList.remove(ListDataListener.class, l);
    }

    private void fireContentsChanged(Object source, int index0, int index1) {
        Object[] listeners = listenerList.getListenerList();
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
    public boolean canBeAnimated() {
        return false;
    }

    @Override
    public String getResetToolTip() {
        return super.getResetToolTip() + " to " + defaultChoice;
    }

    @Override
    @SuppressWarnings("unchecked")
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

    private void setupOther(FilterSetting other, Predicate<E> condition, boolean enable) {
        other.setEnabled(!enable, EnabledReason.APP_LOGIC);
        addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
            }

            @Override
            @SuppressWarnings("unchecked")
            public void contentsChanged(ListDataEvent e) {
                if (condition.test((E) getSelectedItem())) {
                    other.setEnabled(enable, EnabledReason.APP_LOGIC);
                } else {
                    other.setEnabled(!enable, EnabledReason.APP_LOGIC);
                }
            }
        });
    }

    public String getDebugInfo() {
        return String.format("choices = %s, default = %s, current = %s",
            choices, defaultChoice, currentChoice);
    }

    public record ChoiceParamState<E>(E value) implements ParamState<ChoiceParamState<E>> {
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
