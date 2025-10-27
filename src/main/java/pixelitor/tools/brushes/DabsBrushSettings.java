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
    private RotationSettings rotationSettings;
    private Spacing spacing;

    public DabsBrushSettings(RotationSettings rotationSettings, Spacing spacing) {
        this.rotationSettings = rotationSettings;
        this.spacing = spacing;
    }

    public void setSpacing(Spacing spacing) {
        this.spacing = spacing;
        notifyBrushes();
    }

    public void setAngleSettings(RotationSettings rotationSettings) {
        this.rotationSettings = rotationSettings;
        notifyBrushes();
    }

    /**
     * Returns true if the brush angle should follow the stroke direction.
     */
    public boolean isDirectional() {
        return rotationSettings.isDirectional();
    }

    public RotationSettings getAngleSettings() {
        return rotationSettings;
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
        rotationSettings.saveStateTo(preset);
        spacing.saveStateTo(preset);
    }

    @Override
    public void loadStateFrom(UserPreset preset) {
        rotationSettings = RotationSettings.fromPreset(preset);
        spacing.loadStateFrom(preset);
    }
}
