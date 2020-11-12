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

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.function.Predicate;

import static java.lang.String.format;

/**
 * Base class for filter params that have a JComboBox as their GUI
 */
public abstract class AbstractMultipleChoiceParam<E>
    extends AbstractFilterParam implements ComboBoxModel<E> {

    protected AbstractMultipleChoiceParam(String name,
                                          RandomizePolicy randomizePolicy) {
        super(name, randomizePolicy);
    }

    @Override
    public JComponent createGUI() {
        var gui = new ComboBoxParamGUI<E>(this, action);
        paramGUI = gui;
        setGUIEnabledState();
        return gui;
    }

    @Override
    public boolean canBeAnimated() {
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ChoiceParamState<E> copyState() {
        return new ChoiceParamState<>((E) getSelectedItem());
    }

    @Override
    public void setState(ParamState<?> state, boolean updateGUI) {
        @SuppressWarnings("unchecked")
        ChoiceParamState<E> paramState = (ChoiceParamState<E>) state;
        setSelectedItem(paramState.getValue(), false);
    }

    @Override
    public void setState(String savedValue) {
        int numEntries = getSize();
        for (int i = 0; i < numEntries; i++) {
            E item = getElementAt(i);
            if (item.toString().equals(savedValue)) {
                setSelectedItem(item, false);
                break;
            }
        }
    }

    public abstract void setSelectedItem(E value, boolean trigger);

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

    public static class ChoiceParamState<E> implements ParamState<ChoiceParamState<E>> {
        final E value;

        public ChoiceParamState(E value) {
            this.value = value;
        }

        @Override
        public ChoiceParamState<E> interpolate(ChoiceParamState<E> endState, double progress) {
            throw new UnsupportedOperationException();
        }

        public E getValue() {
            return value;
        }

        @Override
        public String toSaveString() {
            return value.toString();
        }

        @Override
        public String toString() {
            return format("%s[value=%s]",
                getClass().getSimpleName(), value);
        }
    }
}
