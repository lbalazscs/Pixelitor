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

package pixelitor.gui;

import javax.swing.*;
import java.beans.PropertyVetoException;

/**
 * A component that contains an {@link ImageComponent}
 * (not directly, but inside a JScrollPane)
 * It can be either a JInternalFrame or a tab in a JTabbedPane.
 * Some of the methods make sense only for internal frames, not for tabs.
 */
public interface ImageWindow {
    /**
     * Sets the location and size of the internal frame.
     */
    void setSize(int locX, int locY, int width, int height);

    JScrollPane getScrollPane();

    void dispose();

    void setSelected(boolean b) throws PropertyVetoException;

    void setTitle(String title);

    void makeSureItIsVisible();
}
