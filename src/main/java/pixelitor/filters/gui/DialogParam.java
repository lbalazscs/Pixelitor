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

import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GUIUtils;

import javax.swing.*;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Map;

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

        setParamGUIEnabledState();
        return (JComponent) paramGUI;
    }

    private JDialog createDialog(JDialog owner) {
        JPanel p = GUIUtils.arrangeParamsVertically(Arrays.asList(children));
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
    public void randomize() {
        for (FilterParam child : children) {
            child.randomize();
        }
    }

    @Override
    public void considerImageSize(Rectangle bounds) {
        for (FilterParam child : children) {
            child.considerImageSize(bounds);
        }
    }

    @Override
    public ParamState copyState() {
        return new CompositeState(children);
    }

    @Override
    public void setState(ParamState state) {

    }

    @Override
    public boolean canBeAnimated() {
        return false;
    }

    @Override
    public int getNumGridBagCols() {
        return 2;
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

    static class CompositeState implements ParamState {
        private Map<String, ParamState> childStates;

        public CompositeState(FilterParam[] children) {
            for (FilterParam child : children) {
                if (child.canBeAnimated()) {
                    childStates.put(child.getName(), child.copyState());
                }
            }
        }

        @Override
        public CompositeState interpolate(ParamState endState, double progress) {
            CompositeState end = (CompositeState) endState;
            // TODO
            return null;
        }
    }
}
