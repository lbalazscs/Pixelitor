/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.FilterParam;
import pixelitor.filters.gui.UserPreset;

import javax.swing.*;
import java.util.function.Consumer;

/**
 * The settings of a {@link DabsBrush}.
 */
public class DabsBrushSettings extends BrushSettings {
    private AngleSettings angleSettings;
    private Spacing spacing;

    public DabsBrushSettings(AngleSettings angleSettings, Spacing spacing) {
        this.angleSettings = angleSettings;
        this.spacing = spacing;
    }

    public void setSpacing(Spacing spacing) {
        this.spacing = spacing;
        notifyBrushes();
    }

    public void setAngleSettings(AngleSettings angleSettings) {
        this.angleSettings = angleSettings;
        notifyBrushes();
    }

    /**
     * Returns true if the brush angle should follow the stroke direction.
     */
    public boolean isAngled() {
        return angleSettings.isAngled();
    }

    public AngleSettings getAngleSettings() {
        return angleSettings;
    }

    public Spacing getSpacingStrategy() {
        return spacing;
    }

    @Override
    protected JPanel createConfigPanel() {
        // only subclasses use GUI configuration
        throw new UnsupportedOperationException();
    }

    @Override
    protected void forEachParam(Consumer<FilterParam> consumer) {
        // this class doesn't use params, therefore the methods
        // that would call this method are overridden 
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        angleSettings.saveStateTo(preset);
        spacing.saveStateTo(preset);
    }

    @Override
    public void loadStateFrom(UserPreset preset) {
        angleSettings.loadStateFrom(preset);
        spacing.loadStateFrom(preset);
    }
}