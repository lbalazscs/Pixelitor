/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.Rectangle;
import java.util.Random;

/**
 * Similar to IntChoiceParam, this is a model for a JComboBox,
 * but the values are coming from an enum
 */
public class EnumParam<E extends Enum<E>> extends AbstractFilterParam implements ComboBoxModel<E> {
    private final EnumComboBoxModel<E> delegateModel;
    private final E[] enumConstants;
    private E defaultValue;
    private ActionSetting action;

    public EnumParam(String name, Class<E> enumClass) {
        super(name, RandomizePolicy.ALLOW_RANDOMIZE);
        this.enumConstants = enumClass.getEnumConstants();
        defaultValue = enumConstants[0];
        delegateModel = new EnumComboBoxModel<>(enumClass);
    }

    @Override
    public JComponent createGUI() {
        ChoiceSelector choiceSelector = new ChoiceSelector(this, action);
        paramGUI = choiceSelector;
        setParamGUIEnabledState();
        return choiceSelector;
    }

    @Override
    public void randomize() {
        Random rnd = new Random();

        int randomIndex = rnd.nextInt(enumConstants.length);
        setSelectedItem(enumConstants[randomIndex], false);
    }

    @Override
    public void considerImageSize(Rectangle bounds) {

    }

    @Override
    public ParamState copyState() {
        return null;
    }

    @Override
    public void setState(ParamState state) {

    }

    @Override
    public boolean canBeAnimated() {
        return false;
    }

    @Override
    public int getNrOfGridBagCols() {
        return 2;
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
    public void setSelectedItem(Object anItem) {
        setSelectedItem((E) anItem, true);
    }

    public void selectAndSetAsDefault(E item) {
        defaultValue = item;
        setSelectedItem(item, false);
    }

    private void setSelectedItem(E value, boolean trigger) {
        delegateModel.setSelectedItem(value);

        if (trigger) {
            if (adjustmentListener != null) {
                adjustmentListener.paramAdjusted();
            }
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
    public void addListDataListener(ListDataListener l) {
        delegateModel.addListDataListener(l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        delegateModel.removeListDataListener(l);
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
    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        super.setAdjustmentListener(listener);
        if (action != null) {
            action.setAdjustmentListener(listener);
        }
    }

    public EnumParam withAction(ActionSetting action) {
        this.action = action;
        return this;
    }
}
