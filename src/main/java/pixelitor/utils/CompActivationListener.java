/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.utils;

import pixelitor.gui.View;

/**
 * Listener for events related to changing the active composition
 */
public interface CompActivationListener {
    /**
     * Called when the active image changes either because the user
     * switches to another image or because a new image was opened.
     */
    void compActivated(View oldView, View newView);

    /**
     * Called when the user has closed all images
     */
    void allCompsClosed();
}
