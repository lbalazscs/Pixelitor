/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.layers.LayerGUI;

/**
 * Helper class for testing {@link LayerGUI} objects in
 * AssertJ Swing tests
 */
public class LayerGUIFixture extends JToggleButtonFixture {
    public LayerGUIFixture(Robot robot, LayerGUI target) {
        super(robot, target);
    }

    public void setOpenEye(boolean b) {
        EDT.run(() -> ((LayerGUI) target()).setOpenEye(b));
        robot().waitForIdle();
    }

    private boolean isEyeOpen() {
        return EDT.call(() -> ((LayerGUI) target()).isEyeOpen());
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
