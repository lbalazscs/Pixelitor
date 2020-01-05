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

import static java.lang.String.format;

/**
 * A model for a button on a filter GUI, which runs a task
 * when pushed, before triggering the filter
 */
public class FilterButtonModel implements FilterSetting {
    private final Runnable beforeTriggeringTask;
    private final Icon icon;
    private final String toolTipText;
    private final String lookupName; // for AssertJSwing tests
    private final String text;
    private ParamAdjustmentListener adjustmentListener;
    private OrderedExecutionButton button;

    private boolean enabledByFilterLogic = true;
    private boolean enabledByAnimationSetting = true;

    // most actions should be available in the final animation settings
    private boolean ignoreFinalAnimationSettingMode = true;

    public FilterButtonModel(String text, Runnable beforeTriggeringTask, String toolTipText) {
        this(text, beforeTriggeringTask, null, toolTipText, null);
    }

    public FilterButtonModel(String text, Runnable beforeTriggeringTask, Icon icon,
                             String toolTipText, String lookupName) {
        this.text = text;
        this.beforeTriggeringTask = beforeTriggeringTask;
        this.icon = icon;
        this.toolTipText = toolTipText;
        this.lookupName = lookupName;
    }

    @Override
    public JComponent createGUI() {
        button = new OrderedExecutionButton(text, beforeTriggeringTask, adjustmentListener, icon);
        if(toolTipText != null) {
            button.setToolTipText(toolTipText);
        }
        button.setEnabled(shouldBeEnabled());
        if (lookupName != null) {
            button.setName(lookupName);
        }
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
    public int getNumGridBagCols() {
        return 1;
    }

    public void setIgnoreFinalAnimationSettingMode(boolean ignoreFinalAnimationSettingMode) {
        this.ignoreFinalAnimationSettingMode = ignoreFinalAnimationSettingMode;
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        adjustmentListener = listener;
    }

    @Override
    public String getName() {
        return text;
    }

    @Override
    public String toString() {
        return format("%s[name = '%s']", getClass().getSimpleName(), getName());
    }

    /**
     * A button that runs first its ActionListener (to do its
     * specific job), and after then its ParamAdjustmentListener
     * (typically to trigger a filter preview)
     */
    private static class OrderedExecutionButton extends JButton {
        private OrderedExecutionButton(String name, Runnable beforeTriggeringTask,
                                       ParamAdjustmentListener adjustmentListener,
                                       Icon icon) {
            super(name);

            if (icon != null) {
                setIcon(icon);
            }

            addActionListener(e -> {
                beforeTriggeringTask.run();
                adjustmentListener.paramAdjusted();
            });
        }
    }
}
