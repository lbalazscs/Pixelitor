/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.layers.Drawable;
import pixelitor.menus.DrawableAction;

import static pixelitor.ChangeReason.REPEAT_LAST;
import static pixelitor.filters.FilterUtils.getLastFilter;

/**
 * The "Repeat" action, which repeats the last edit.
 * Currently only the filters can be repeated.
 */
public class RepeatLast extends DrawableAction {
    public static final RepeatLast INSTANCE = new RepeatLast();

    private RepeatLast() {
        super("Repeat Last Operation");
        setEnabled(false);
    }

    @Override
    protected void process(Drawable dr) {
        getLastFilter()
                .ifPresent(filter -> filter.startOn(dr, REPEAT_LAST));
    }
}
