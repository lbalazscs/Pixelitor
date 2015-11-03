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

package pixelitor.filters;

import pixelitor.menus.NamedAction;

import java.awt.event.ActionEvent;

import static pixelitor.ChangeReason.REPEAT_LAST;
import static pixelitor.filters.FilterUtils.getLastExecutedFilter;

public class RepeatLast extends NamedAction {
    public static final RepeatLast INSTANCE = new RepeatLast();

    private RepeatLast() {
        super("Repeat Last Operation");
        setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        getLastExecutedFilter()
                .ifPresent(filter -> filter.execute(REPEAT_LAST));
    }
}
