/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import javax.swing.*;

/**
 * The settings of a {@link DabsBrush}
 */
public class DabsBrushSettings extends BrushSettings {
    private AngleSettings angleSettings;
    private Spacing spacing;

    public DabsBrushSettings(AngleSettings angleSettings, Spacing spacing) {
        this.angleSettings = angleSettings;
        this.spacing = spacing;
    }

    public void changeSpacing(Spacing spacing) {
        this.spacing = spacing;
        notifyBrushes();
    }

    public void changeAngleSettings(AngleSettings angleSettings) {
        this.angleSettings = angleSettings;
        notifyBrushes();
    }

    public boolean isAngleAware() {
        return angleSettings.isAngleAware();
    }

    public AngleSettings getAngleSettings() {
        return angleSettings;
    }

    public Spacing getSpacingStrategy() {
        return spacing;
    }

    @Override
    protected JPanel createConfigPanel() {
        // shouldn't be called for brushes without settings.
        throw new UnsupportedOperationException();
    }
}