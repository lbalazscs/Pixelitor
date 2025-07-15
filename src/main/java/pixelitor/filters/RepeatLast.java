/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters;

import pixelitor.FilterContext;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.filters.util.Filters;
import pixelitor.layers.Drawable;
import pixelitor.layers.SmartObject;
import pixelitor.menus.DrawableAction;
import pixelitor.utils.Messages;
import pixelitor.utils.Texts;

import static pixelitor.filters.util.Filters.getLastFilter;

/**
 * The action that repeats the last used filter, either with or without its dialog.
 */
public class RepeatLast extends DrawableAction {
    private static final String REPEAT_LAST_DEFAULT_NAME = Texts.i18n("repeat_last_def");
    private static final String SHOW_LAST_DEFAULT_NAME = Texts.i18n("show_last_def");

    // an instance that repeats the last filter without showing its dialog
    public static final RepeatLast REPEAT_LAST_ACTION = new RepeatLast(false);

    // an instance that shows the dialog of the last used filter
    public static final RepeatLast SHOW_LAST_ACTION = new RepeatLast(true);

    private final boolean showDialog;

    private RepeatLast(boolean showDialog) {
        super(showDialog ? SHOW_LAST_DEFAULT_NAME : REPEAT_LAST_DEFAULT_NAME,
            showDialog, true);
        this.showDialog = showDialog;
        setEnabled(false);
    }

    /**
     * Adds the last used filter as a smart filter to the smart object.
     */
    @Override
    protected void applyToSmartObject(SmartObject so) {
        // if the active layer is a smart object, add the last filter as a new smart filter
        getLastFilter().ifPresent(lastFilter -> {
            if (lastFilter.canBeSmart()) {
                so.tryAddingSmartFilter(lastFilter.copy());
            } else {
                Messages.showFilterCantBeSmartMessage(getName());
            }
        });
    }

    /**
     * Applies the last used filter to the drawable.
     */
    @Override
    protected void process(Drawable dr) {
        getLastFilter().ifPresent(filter -> {
            if (showDialog) {
                // shows the filter's configuration dialog before applying
                dr.startFilter(filter, false);
            } else {
                // re-applies the filter with the last used settings
                dr.startFilter(filter, FilterContext.REPEAT_LAST);
            }
        });
    }

    /**
     * Enables or disables the action based on whether a last filter is available.
     */
    @Override
    public void setEnabled(boolean newValue) {
        if (newValue) {
            boolean hasLastFilter = showDialog ? Filters.hasLastGUIFilter() : Filters.hasLastFilter();
            super.setEnabled(hasLastFilter);
        } else {
            super.setEnabled(false);
        }
    }

    /**
     * Updates the action's text and enabled state after a filter has been used.
     */
    public static void update(Filter lastFilter) {
        Object[] lastFilterNameArgs = {lastFilter.getName()};

        REPEAT_LAST_ACTION.setText(Texts.formatI18N("repeat_last", lastFilterNameArgs));
        REPEAT_LAST_ACTION.setEnabled(true);

        if (lastFilter instanceof FilterWithGUI) {
            SHOW_LAST_ACTION.setText(Texts.formatI18N("show_last", lastFilterNameArgs) + "...");
            SHOW_LAST_ACTION.setEnabled(true);
        } else {
            // can't show a dialog for a filter without a GUI
            SHOW_LAST_ACTION.setText(SHOW_LAST_DEFAULT_NAME);
            SHOW_LAST_ACTION.setEnabled(false);
        }
    }
}
