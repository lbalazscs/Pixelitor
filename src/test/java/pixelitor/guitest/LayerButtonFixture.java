/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.guitest;

import org.assertj.swing.core.Robot;
import org.assertj.swing.fixture.JToggleButtonFixture;
import pixelitor.layers.LayerButton;

/**
 * Helper class for testing {@link LayerButton} objects in
 * AssertJ Swing tests
 */
public class LayerButtonFixture extends JToggleButtonFixture {
    public LayerButtonFixture(Robot robot, LayerButton target) {
        super(robot, target);
    }

    public LayerButtonFixture(Robot robot, String toggleButtonName) {
        super(robot, toggleButtonName);
    }

    public void setOpenEye(boolean b) {
        EDT.run(() -> ((LayerButton) target()).setOpenEye(b));
        robot().waitForIdle();
    }

    private boolean isEyeOpen() {
        Boolean result = EDT.call(() -> ((LayerButton) target()).isEyeOpen());
//        robot().waitForIdle();
        return result;
    }

    public void requireOpenEye() {
        if (!isEyeOpen()) {
            throw new AssertionError("closed eye");
        }
    }

    public void requireClosedEye() {
        if (isEyeOpen()) {
            throw new AssertionError("open eye");
        }
    }
}
