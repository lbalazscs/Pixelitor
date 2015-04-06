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

package pixelitor.filters.gui;

import javax.swing.*;
import java.awt.Rectangle;

/**
 * A filter parameter that can be configured by the user
 */
public interface GUIParam extends Resettable {
    String getName();

    void setAdjustmentListener(ParamAdjustmentListener listener);

    JComponent createGUI();

    int getNrOfGridBagCols();

    /**
     * Sets a random value without triggering the filter
     */
    void randomize();

    /**
     * Sets the filter-triggering policy
     */
    void setTrigger(Trigger trigger);

    void considerImageSize(Rectangle bounds);

    ParamState copyState();

    void setState(ParamState state);

    boolean canBeAnimated();

    /**
     * A GUI parameter can be disabled for two reasons:
     * because of the filter logic and because non-animatable
     * parameters should be disabled in the final animation dialogs.
     * The following methods are for these two scenarios
     */
    void setEnabledLogically(boolean b);
    void setFinalAnimationSettingMode(boolean b); // implemented for the non-animatable params

    /**
     * Whether the running of the filter should be triggered
     * when the value of this GUIParam changes
     */
    enum Trigger {
        DO, DONT;

        static Trigger fromBoolean(boolean b) {
            return b ? DO : DONT;
        }
    }

}
