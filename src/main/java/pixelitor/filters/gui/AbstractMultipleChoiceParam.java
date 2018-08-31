/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import java.awt.Rectangle;
import java.util.function.Predicate;

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
        ComboBoxParamGUI gui = new ComboBoxParamGUI(this, action);
        paramGUI = gui;
        setParamGUIEnabledState();
        return gui;
    }

    @Override
    public int getNumGridBagCols() {
        return 2;
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

    /**
     * Sets up the automatic enabling of another {@link FilterSetting}
     * depending on the selected item of this one.
     */
    public void setupEnableOtherIf(FilterSetting other,
                                   Predicate<E> condition) {
        // disable by default
        other.setEnabled(false, EnabledReason.APP_LOGIC);
        addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                if (condition.test((E) getSelectedItem())) {
                    other.setEnabled(true, EnabledReason.APP_LOGIC);
                } else {
                    other.setEnabled(false, EnabledReason.APP_LOGIC);
                }
            }
        });
    }

    /**
     * Sets up the automatic disabling of another {@link FilterSetting}
     * depending on the selected item of this one.
     */
    public void setupDisableOtherIf(FilterSetting other,
                                    Predicate<E> condition) {
        addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                if (condition.test((E) getSelectedItem())) {
                    other.setEnabled(false, EnabledReason.APP_LOGIC);
                } else {
                    other.setEnabled(true, EnabledReason.APP_LOGIC);
                }
            }
        });
    }
}
