/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.utils.Icons;

import javax.swing.*;

import static java.lang.String.format;

/**
 * A model for a button on a filter GUI that runs an action
 * when pushed, before triggering the filter preview.
 */
public class FilterButtonModel implements FilterSetting {
    private final Runnable action;
    private final Icon icon;
    private final String toolTipText;
    private final String lookupName; // for AssertJSwing tests
    private final String text;
    private ParamAdjustmentListener adjustmentListener;
    private JButton button;

    private boolean enabledByFilterLogic = true;
    private boolean enabledByAnimationSetting = true;

    // most actions should be available in the final animation settings
    private boolean ignoreFinalAnimationSettingMode = true;

    private final boolean triggerFilter;

    public FilterButtonModel(String text, Runnable action, String toolTipText) {
        this(text, action, null, toolTipText, null, true);
    }

    public FilterButtonModel(String text, Runnable action, Icon icon,
                             String toolTipText, String lookupName) {
        this(text, action, icon, toolTipText, lookupName, true);
    }

    public FilterButtonModel(String text, Runnable action, Icon icon,
                             String toolTipText, String lookupName,
                             boolean triggerFilter) {
        this.text = text;
        this.action = action;
        this.icon = icon;
        this.toolTipText = toolTipText;
        this.lookupName = lookupName;
        this.triggerFilter = triggerFilter;
    }

    public static FilterButtonModel createReseed(Runnable reseedTask) {
        return createReseed(reseedTask, "Reseed",
            "Reinitialize the randomness");
    }

    public static FilterButtonModel createReseed(Runnable reseedTask,
                                                 String text, String toolTip) {
        var filterAction = new FilterButtonModel(text, reseedTask,
            Icons.getTwoDicesIcon(), toolTip, "reseed");
        filterAction.setIgnoreFinalAnimationSettingMode(false);
        return filterAction;
    }

    /**
     * The returned action only re-runs the filter
     * (can be useful when using ThreadLocalRandom)
     */
    public static FilterButtonModel createNoOpReseed() {
        return createReseed(() -> {});
    }

    @Override
    public JComponent createGUI() {
        button = new JButton(text, icon);
        if (triggerFilter) {
            button.addActionListener(e -> {
                // first run the given task...
                action.run();
                // ... and then trigger the filter preview
                adjustmentListener.paramAdjusted();
            });
        } else {
            // just the given task
            button.addActionListener(e -> action.run());
        }
        if (toolTipText != null) {
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
            case APP_LOGIC -> enabledByFilterLogic = b;
            case FINAL_ANIMATION_SETTING -> {
                if (ignoreFinalAnimationSettingMode) {
                    return;
                }
                enabledByAnimationSetting = b;
            }
        }
        if (button != null) {
            button.setEnabled(shouldBeEnabled());
        }
    }

    private boolean shouldBeEnabled() {
        return enabledByFilterLogic && enabledByAnimationSetting;
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
}
