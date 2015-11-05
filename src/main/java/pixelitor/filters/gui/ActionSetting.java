/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
import java.awt.event.ActionListener;

/**
 * Creates a button that executes an action when pushed
 */
public class ActionSetting implements FilterSetting {
    private final ActionListener actionListener;
    private final Icon icon;
    private final String toolTipText;
    private final String name;
    private ParamAdjustmentListener adjustmentListener;
    private OrderedExecutionButton button;

    private boolean enabledByFilterLogic = true;
    private boolean enabledByAnimationSetting = true;

    // most actions should be available in the final animation settings
    private boolean ignoreFinalAnimationSettingMode = true;

    public ActionSetting(String name, ActionListener actionListener, String toolTipText) {
        this(name, actionListener, null, toolTipText);
    }

    public ActionSetting(String name, ActionListener actionListener, Icon icon, String toolTipText) {
        this.name = name;
        this.actionListener = actionListener;
        this.icon = icon;
        this.toolTipText = toolTipText;
    }

    @Override
    public JComponent createGUI() {
        button = new OrderedExecutionButton(getName(), actionListener, adjustmentListener, icon);
        if(toolTipText != null) {
            button.setToolTipText(toolTipText);
        }
        button.setEnabled(shouldBeEnabled());
        return button;
    }

    @Override
    public void setEnabled(boolean b, EnabledReason reason) {
        switch (reason) {
            case APP_LOGIC:
                enabledByFilterLogic = b;
                break;
            case FINAL_ANIMATION_SETTING:
                if (ignoreFinalAnimationSettingMode) {
                    return;
                }
                enabledByAnimationSetting = b;
                break;
        }
        if (button != null) {
            button.setEnabled(shouldBeEnabled());
        }
    }

    private boolean shouldBeEnabled() {
        return enabledByFilterLogic && enabledByAnimationSetting;
    }

    @Override
    public int getNrOfGridBagCols() {
        return 1;
    }

    public void setIgnoreFinalAnimationSettingMode(boolean ignoreFinalAnimationSettingMode) {
        this.ignoreFinalAnimationSettingMode = ignoreFinalAnimationSettingMode;
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        this.adjustmentListener = listener;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return String.format("%s[name = '%s']",
                getClass().getSimpleName(), getName());
    }

    /**
     * A button that executes first its ActionListener, and after then its ParamAdjustmentListener
     */
    private static class OrderedExecutionButton extends JButton {
        private OrderedExecutionButton(String name, ActionListener actionListener, ParamAdjustmentListener adjustmentListener, Icon icon) {
            super(name);

            if (icon != null) {
                setIcon(icon);
            }

            addActionListener(e -> {
                actionListener.actionPerformed(e);
                adjustmentListener.paramAdjusted();
            });
        }
    }
}
