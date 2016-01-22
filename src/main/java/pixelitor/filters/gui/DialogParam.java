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

import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.OKDialog;

import javax.swing.*;
import java.awt.Rectangle;
import java.util.Arrays;

import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;

/**
 * A composite FilterParam which can show its children in a dialog
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
        paramGUI = new ConfigureParamGUI(owner -> {
            JDialog dialog = createDialog(owner);
            GUIUtils.centerOnScreen(dialog);
            return dialog;
        }, defaultButton);

        setParamGUIEnabledState();
        return (JComponent) paramGUI;
    }

    private JDialog createDialog(JDialog owner) {
        JPanel p = GUIUtils.arrangeParamsInVerticalGridBag(Arrays.asList(children));
        OKDialog d = new OKDialog(owner, getName(), "Close");
        d.setupGUI(p);
        return d;
    }

    @Override
    public void randomize() {

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
        for (FilterParam child : children) {
            if (!child.isSetToDefault()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void reset(boolean triggerAction) {
        for (FilterParam param : children) {
            param.reset(false);
        }
        if (triggerAction) {
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
            defaultButton.updateState();
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
}
