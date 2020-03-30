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

import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GUIUtils;

import javax.swing.*;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;

/**
 * A composite {@link FilterParam} which
 * can show its children in a dialog
 */
public class DialogParam extends AbstractFilterParam {
    private final FilterParam[] children;
    private DefaultButton defaultButton;

    public DialogParam(FilterParam... children) {
        this("More Settings", children);
    }

    public DialogParam(String name, FilterParam... children) {
        super(name, ALLOW_RANDOMIZE);
        this.children = children;
    }

    @Override
    public JComponent createGUI() {
        defaultButton = new DefaultButton(this);

        paramGUI = new ConfigureParamGUI(this::createDialog, defaultButton);

        setGUIEnabledState();
        return (JComponent) paramGUI;
    }

    private JDialog createDialog(JDialog owner) {
        JPanel p = GUIUtils.arrangeVertically(List.of(children));
        JDialog d = new DialogBuilder()
                .owner(owner)
                .content(p)
                .title(getName())
                .withScrollbars()
                .okText("Close")
                .noCancelButton()
                .build();
        return d;
    }

    @Override
    protected void doRandomize() {
        for (FilterParam child : children) {
            child.randomize();
        }
        updateDefaultButtonState();
    }

    @Override
    public void considerImageSize(Rectangle bounds) {
        for (FilterParam child : children) {
            child.considerImageSize(bounds);
        }
    }

    @Override
    public CompositeState copyState() {
        return new CompositeState(children);
    }

    @Override
    public void setState(ParamState<?> state) {
        CompositeState newStates = (CompositeState) state;
        Iterator<ParamState<?>> stateIterator = newStates.iterator();
        for (FilterParam child : children) {
            if (child.canBeAnimated()) {
                child.setState(stateIterator.next());
            }
        }
    }

    @Override
    public boolean canBeAnimated() {
        for (FilterParam child : children) {
            if (child.canBeAnimated()) {
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
    public boolean isSetToDefault() {
        return Arrays.stream(children)
                .allMatch(Resettable::isSetToDefault);
    }

    @Override
    public void reset(boolean trigger) {
        for (FilterParam param : children) {
            param.reset(false);
        }
        if (trigger) {
            adjustmentListener.paramAdjusted();
        } else {
            // this class updates the default button state
            // simply by putting a decorator on the adjustment
            // listeners, no this needs to be called here manually
            updateDefaultButtonState();
        }
    }

    private void updateDefaultButtonState() {
        if (defaultButton != null) {
            defaultButton.updateIcon();
        }
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        ParamAdjustmentListener decoratedListener = () -> {
            updateDefaultButtonState();
            listener.paramAdjusted();
        };

        super.setAdjustmentListener(decoratedListener);

        for (FilterParam child : children) {
            child.setAdjustmentListener(decoratedListener);
        }
    }

    @Override
    public Object getParamValue() {
        List<Object> childValues = Stream.of(children)
                .map(FilterParam::getParamValue)
                .collect(toList());
        return childValues;
    }
}
