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

import pixelitor.filters.gui.FilterSetting.EnabledReason;

import java.util.EnumSet;
import java.util.Set;

/**
 * Tracks the independent reasons for which a {@link FilterSetting}
 * can be disabled. The setting is enabled only if no reason disables it.
 */
final class EnabledState {
    private final Set<EnabledReason> disabledBy = EnumSet.noneOf(EnabledReason.class);

    void set(EnabledReason reason, boolean enabled) {
        if (enabled) {
            disabledBy.remove(reason);
        } else {
            disabledBy.add(reason);
        }
    }

    boolean isEnabledFor(EnabledReason reason) {
        return !disabledBy.contains(reason);
    }

    boolean isEnabled() {
        return disabledBy.isEmpty();
    }
}
