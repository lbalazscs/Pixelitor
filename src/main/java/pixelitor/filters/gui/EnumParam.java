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

import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.utils.Rnd;

import javax.swing.event.ListDataListener;

/**
 * Just like {@link IntChoiceParam}, this is a model
 * for a JComboBox, but the values are coming from an enum
 */
public class EnumParam<E extends Enum<E>> extends AbstractMultipleChoiceParam<E> {
    private final EnumComboBoxModel<E> delegateModel;
    private final E[] enumConstants;
    private E defaultValue;

    public EnumParam(String name, Class<E> enumClass) {
        super(name, RandomizePolicy.ALLOW_RANDOMIZE);
        enumConstants = enumClass.getEnumConstants();
        defaultValue = enumConstants[0];
        delegateModel = new EnumComboBoxModel<>(enumClass);
    }

    @Override
    protected void doRandomize() {
        setSelectedItem(Rnd.chooseFrom(enumConstants), false);
    }

    @Override
    public boolean isSetToDefault() {
        return getSelectedItem() == defaultValue;
    }

    @Override
    public void reset(boolean trigger) {
        setSelectedItem(defaultValue, trigger);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setSelectedItem(Object anItem) {
        setSelectedItem((E) anItem, true);
    }

    public EnumParam<E> withDefault(E item) {
        defaultValue = item;
        setSelectedItem(item, false);
        return this;
    }

    @Override
    public void setSelectedItem(E value, boolean trigger) {
        delegateModel.setSelectedItem(value);

        if (trigger && adjustmentListener != null) {
            adjustmentListener.paramAdjusted();
        }
    }

    @Override
    public int getSize() {
        return delegateModel.getSize();
    }

    @Override
    public E getElementAt(int index) {
        return delegateModel.getElementAt(index);
    }

    @Override
    public Object getSelectedItem() {
        return delegateModel.getSelectedItem();
    }

    // no need for casting with this one
    public E getSelected() {
        return delegateModel.getSelectedItem();
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        delegateModel.addListDataListener(l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        delegateModel.removeListDataListener(l);
    }

    @Override
    public String getResetToolTip() {
        return super.getResetToolTip() + " to " + defaultValue;
    }

    @Override
    public Object getParamValue() {
        return getSelectedItem();
    }
}
