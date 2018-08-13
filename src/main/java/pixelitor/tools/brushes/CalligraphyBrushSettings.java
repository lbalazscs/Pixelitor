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

package pixelitor.tools.brushes;

import pixelitor.filters.gui.AngleParam;

import javax.swing.*;
import java.awt.FlowLayout;

/**
 * The settings of a {@link CalligraphyBrush}
 */
public class CalligraphyBrushSettings implements BrushSettings {
    private static final double DEFAULT_ANGLE = -Math.PI / 4.0;

    private AngleParam angleParam;

    @Override
    public JPanel getConfigPanel() {
        JPanel p = new JPanel(new FlowLayout());
        angleParam = new AngleParam("Angle", DEFAULT_ANGLE);
        p.add(angleParam.createGUI());

        return p;
    }

    public double getAngle() {
        if (angleParam != null) {
            return angleParam.getValueInRadians();
        }
        return DEFAULT_ANGLE;
    }
}
