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

package pixelitor.tools.brushes;

import pixelitor.utils.debug.DebugNode;

/**
 * A brush that works by putting down dabs
 */
public abstract class DabsBrush extends AbstractBrush {
    protected DabsBrushSettings settings;
    private final DabsStrategy dabsStrategy;

    protected DabsBrush(int radius, SpacingStrategy spacingStrategy, AngleSettings angleSettings, boolean refreshBrushForEachDab) {
        super(radius);
        settings = new DabsBrushSettings(angleSettings, spacingStrategy);
        dabsStrategy = new LinearDabsStrategy(this,
                spacingStrategy,
                angleSettings,
                refreshBrushForEachDab);
        settings.registerBrush(this);
    }

    protected DabsBrush(int radius, DabsBrushSettings settings, boolean refreshBrushForEachDab) {
        super(radius);
        this.settings = settings;
        dabsStrategy = new LinearDabsStrategy(this,
                settings.getSpacingStrategy(),
                settings.getAngleSettings(),
                refreshBrushForEachDab);
        settings.registerBrush(this);
    }

    /**
     * Sets up the brush stamp. Depending on the type of brush, it can be
     * called at the beginning of a stroke or before each dab.
     */
    abstract void setupBrushStamp(double x, double y);

    public abstract void putDab(double x, double y, double theta);

    @Override
    public void onDragStart(double x, double y) {
        dabsStrategy.onDragStart(x, y);
        updateComp(x, y);
    }

    @Override
    public void onNewMousePoint(double x, double y) {
        dabsStrategy.onNewMousePoint(x, y);
        updateComp(x, y);
    }

    public DabsBrushSettings getSettings() {
        return settings;
    }

    public void setSettings(DabsBrushSettings settings) {
        this.settings = settings;
    }

    public void settingsChanged() {
        dabsStrategy.settingsChanged();
    }

    @Override
    public void dispose() {
        settings.unregisterBrush(this);
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = super.getDebugNode();

        node.addBooleanChild("Angle Aware", settings.isAngleAware());

        AngleSettings angleSettings = settings.getAngleSettings();
        node.addBooleanChild("Jitter Aware", angleSettings.shouldJitterAngle());

        SpacingStrategy spacingStrategy = settings.getSpacingStrategy();
        node.addDoubleChild("Spacing", spacingStrategy.getSpacing(radius));

        return node;
    }
}
