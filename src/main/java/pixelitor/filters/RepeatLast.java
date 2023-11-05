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

package pixelitor.filters;

import pixelitor.FilterContext;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.filters.util.Filters;
import pixelitor.layers.Drawable;
import pixelitor.menus.DrawableAction;

import java.util.Optional;

import static pixelitor.filters.util.Filters.getLastFilter;

/**
 * The "Repeat" action, which repeats the last edit.
 * Currently, only filters can be repeated.
 */
public class RepeatLast extends DrawableAction {
    public static final RepeatLast REPEAT_LAST_ACTION = new RepeatLast(false);
    public static final RepeatLast SHOW_LAST_ACTION = new RepeatLast(true);

    private static final String REPEAT_LAST_DEFAULT_NAME = "Repeat Last";
    private static final String SHOW_LAST_DEFAULT_NAME = "Show Last";

    private final boolean showDialog;

    private RepeatLast(boolean showDialog) {
        // TODO This should be available for smart objects; however, some technical
        // problems currently prevent it. Smart objects are handled in
        // FilterAction (because it might need to create a new filter instance),
        // but here we have a Filter that isn't guaranteed to have
        // an action (deserialized smart filter). Perhaps an action could be searched
        // by the filter name.
        super(showDialog ? SHOW_LAST_DEFAULT_NAME : REPEAT_LAST_DEFAULT_NAME,
            showDialog, false);
        this.showDialog = showDialog;
        setEnabled(false);
    }

    @Override
    protected void process(Drawable dr) {
        Optional<Filter> lastFilter = getLastFilter();
        if (showDialog) {
            lastFilter.ifPresent(filter -> dr.startFilter(filter, false));
        } else {
            lastFilter.ifPresent(filter -> dr.startFilter(filter, FilterContext.REPEAT_LAST));
        }
    }

    @Override
    public void setEnabled(boolean newValue) {
        // This is called both when images are opened/closed AND when filters are running
        if (newValue) {
            boolean hasLast;
            if (showDialog) {
                hasLast = Filters.hasLastGUIFilter();
            } else {
                hasLast = Filters.hasLastFilter();
            }
            super.setEnabled(hasLast);
        } else {
            super.setEnabled(false);
        }
    }

    public static void update(Filter lastFilter) {
        REPEAT_LAST_ACTION.setText("Repeat " + lastFilter.getName());
        REPEAT_LAST_ACTION.setEnabled(true);

        if (lastFilter instanceof FilterWithGUI) {
            SHOW_LAST_ACTION.setText("Show " + lastFilter.getName() + "...");
            SHOW_LAST_ACTION.setEnabled(true);
        } else {
            // can't show a filter GUI for a filter without a GUI
            SHOW_LAST_ACTION.setText(REPEAT_LAST_DEFAULT_NAME);
            SHOW_LAST_ACTION.setEnabled(false);
        }
    }
}
