/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
public final class FilterButtonModel implements FilterSetting {
    private final Runnable action;
    private final Icon icon;
    private final String toolTipText;
    private final String lookupName; // for AssertJSwing tests
    private final String buttonText;
    private ParamAdjustmentListener adjustmentListener;
    private JButton button;

    private boolean enabledByFilterLogic = true;
    private boolean enabledByAnimationSetting = true;
    private boolean enabledByParent = true;

    // most buttons should be enabled in the final animation settings
    private boolean affectedByAnimationMode = false;

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

        // the user should be able to reseed only when
        // configuring the initial state for the animation
        filterAction.affectedByAnimationMode = true;

        return filterAction;
    }

    /**
     * Creates a filter button model that only triggers the filter update
     * (can be useful when using ThreadLocalRandom).
     */
    public static FilterButtonModel createNoOpReseed() {
        return createReseed(() -> {});
    }

    /**
     * Creates a filter button model for exporting to an SVG file.
     */
    public static FilterButtonModel createExportSvg(Runnable exportAction) {
        return new FilterButtonModel("Export SVG...", exportAction,
            null, "Export the current image to an SVG file",
            "exportSvg", false);
    }

    @Override
    public JComponent createGUI() {
        button = new JButton(buttonText, icon);
        button.addActionListener(e -> {
            action.run();
            if (shouldTriggerFilter) {
                adjustmentListener.paramAdjusted();
            }
        });

        if (toolTipText != null) {
            button.setToolTipText(toolTipText);
        }
        button.setEnabled(shouldBeEnabled());

        if (lookupName != null) {
            button.setName(lookupName);
        }

        return button;
    }

    /**
     * Enables or disables the button for a specific reason.
     */
    @Override
    public void setEnabled(boolean enabled, EnabledReason reason) {
        switch (reason) {
            case FILTER_LOGIC -> enabledByFilterLogic = enabled;
            case ANIMATION_ENDING_STATE -> {
                if (!affectedByAnimationMode) {
                    return;
                }
                enabledByAnimationSetting = enabled;
            }
            case PARENT_PARAM -> enabledByParent = enabled;
        }
        if (button != null) {
            button.setEnabled(shouldBeEnabled());
        }
    }

    private boolean shouldBeEnabled() {
        return enabledByFilterLogic && enabledByAnimationSetting && enabledByParent;
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
    public void setName(String displayName) {
        throw new UnsupportedOperationException();
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
