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

package pixelitor;

import org.assertj.swing.core.Robot;
import org.assertj.swing.fixture.JToggleButtonFixture;
import pixelitor.layers.LayerButton;

public class LayerButtonFixture extends JToggleButtonFixture {
    public LayerButtonFixture(Robot robot, LayerButton target) {
        super(robot, target);
    }

    public LayerButtonFixture(Robot robot, String toggleButtonName) {
        super(robot, toggleButtonName);
    }

    public void setOpenEye(boolean b) {
        // it would be nicer if we had a JCheckBoxFixture for the
        // visibility checkbox and this would happen through robot events

        ((LayerButton) target()).setOpenEye(b);
    }
}
