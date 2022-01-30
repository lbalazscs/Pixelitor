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

package pixelitor.tools.brushes;

import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.FilterParam;

import javax.swing.*;
import java.awt.FlowLayout;
import java.util.function.Consumer;

import static pixelitor.utils.AngleUnit.CCW_DEGREES;

/**
 * The settings of a {@link CalligraphyBrush}
 */
public class CalligraphyBrushSettings extends BrushSettings {
    private static final double INTUITIVE_45 = 5.497787143782138;

    private AngleParam angleParam;

    @Override
    protected JPanel createConfigPanel() {
        var p = new JPanel(new FlowLayout());
        angleParam = new AngleParam("Angle", 45, CCW_DEGREES);
        p.add(angleParam.createGUI("angle"));

        return p;
    }

    public double getAngle() {
        if (angleParam != null) {
            return angleParam.getValueInRadians();
        }
        return INTUITIVE_45;
    }

    @Override
    protected void forEachParam(Consumer<FilterParam> consumer) {
        consumer.accept(angleParam);
    }
}
