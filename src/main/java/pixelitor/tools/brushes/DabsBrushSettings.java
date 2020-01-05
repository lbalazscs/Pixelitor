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

package pixelitor.tools.brushes;

import javax.swing.*;

/**
 * The settings of a {@link DabsBrush}
 */
public class DabsBrushSettings extends BrushSettings {
    private AngleSettings angleSettings;
    private SpacingStrategy spacingStrategy;

    public DabsBrushSettings(AngleSettings angleSettings, SpacingStrategy spacingStrategy) {
        this.angleSettings = angleSettings;
        this.spacingStrategy = spacingStrategy;
    }

    public void changeSpacing(SpacingStrategy spacingStrategy) {
        this.spacingStrategy = spacingStrategy;
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

    public SpacingStrategy getSpacingStrategy() {
        return spacingStrategy;
    }

    @Override
    protected JPanel createConfigPanel() {
        // This class will be abstract and this method will be
        // unimplemented when all dabs brushes will have settings,
        // and will use subclasses of this class. In the meantime
        // this method is never called for brushes without settings.
        throw new UnsupportedOperationException();
    }
}