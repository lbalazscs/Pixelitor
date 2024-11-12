/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
 * A model representing a button in a filter GUI.
 * When clicked, it performs an action and can optionally
 * trigger a filter preview update.
 */
public class FilterButtonModel implements FilterSetting {
    private final Runnable action;
    private final Icon icon;
    private final String toolTipText;
    private final String lookupName; // for AssertJSwing tests
    private final String buttonText;
    private ParamAdjustmentListener adjustmentListener;
    private JButton button;

    private boolean enabledByFilterLogic = true;
    private boolean enabledByAnimationSetting = true;

    // most actions should be available in the final animation settings
    private boolean ignoreFinalAnimationSettingMode = true;

    private final boolean shouldTriggerFilter;

    public FilterButtonModel(String buttonText, Runnable action, String toolTipText) {
        this(buttonText, action, null, toolTipText, null, true);
    }

    public FilterButtonModel(String buttonText, Runnable action, Icon icon,
                             String toolTipText, String lookupName) {
        this(buttonText, action, icon, toolTipText, lookupName, true);
    }

    public FilterButtonModel(String buttonText, Runnable action, Icon icon,
                             String toolTipText, String lookupName,
                             boolean shouldTriggerFilter) {
        this.buttonText = buttonText;
        this.action = action;
        this.icon = icon;
        this.toolTipText = toolTipText;
        this.lookupName = lookupName;
        this.shouldTriggerFilter = shouldTriggerFilter;
    }

    public static FilterButtonModel createReseed(Runnable reseedTask) {
        return createReseed(reseedTask, "Reseed",
            "Reinitialize the randomness");
    }

    /**
     * Creates a custom filter button model for re-seeding
     * with given label and tooltip.
     */
    public static FilterButtonModel createReseed(Runnable reseedTask,
                                                 String text,
                                                 String toolTip) {
        var filterAction = new FilterButtonModel(text, reseedTask,
            Icons.getReseedIcon(), toolTip, "reseed");
        filterAction.setIgnoreFinalAnimationSettingMode(false);
        return filterAction;
    }

    /**
     * Creates a filter button model that only triggers the filter update
     * (can be useful when using ThreadLocalRandom).
     */
    public static FilterButtonModel createNoOpReseed() {
        return createReseed(() -> {});
    }

    @Override
    public JComponent createGUI() {
        button = new JButton(buttonText, icon);
        if (shouldTriggerFilter) {
            button.addActionListener(e -> {
                // first perform the action...
                action.run();
                // ... and then trigger the filter preview
                adjustmentListener.paramAdjusted();
            });
        } else {
            // just perform the action
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
            case ANIMATION_ENDING_STATE -> {
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
        return buttonText;
    }

    @Override
    public boolean isEnabled() {
        return enabledByFilterLogic && enabledByAnimationSetting;
    }

    @Override
    public String toString() {
        return format("%s[name = '%s']", getClass().getSimpleName(), getName());
    }
}
