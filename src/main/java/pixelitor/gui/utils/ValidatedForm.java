/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

package pixelitor.gui.utils;

import javax.swing.*;
import java.awt.LayoutManager;

/**
 * A form with validity check
 */
public abstract class ValidatedForm extends JPanel {
    protected ValidatedForm() {
    }

    protected ValidatedForm(LayoutManager layoutManager) {
        super(layoutManager);
    }

    /**
     * Performs the validation
     *
     * @return true if the data entered by the user is OK
     */
    public abstract boolean isDataValid();

    public abstract String getErrorMessage();
}
