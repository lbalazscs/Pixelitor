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

import javax.swing.*;

/**
 * A {@link FilterParam} that can have a "Linked" checkbox.
 */
public interface Linkable extends FilterParam {
    /**
     * Returns the model of the "Linked" checkbox,
     * or null if this instance isn't linkable.
     */
    ButtonModel getLinkedModel();

    String getLinkedToolTip();

    /**
     * Whether this particular instance has a "Linked" checkbox.
     */
    default boolean isLinkable() {
        return getLinkedModel() != null;
    }

    /**
     * Whether the checkbox is selected.
     */
    default boolean isLinked() {
        ButtonModel model = getLinkedModel();
        return model != null && model.isSelected();
    }

    /**
     * Does nothing if this instance isn't linkable.
     */
    default void setLinked(boolean linked) {
        ButtonModel model = getLinkedModel();
        if (model != null) {
            model.setSelected(linked);
        }
    }
}
