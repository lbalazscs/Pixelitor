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

import pixelitor.tools.util.PPoint;
import pixelitor.utils.debug.DebugNode;

/**
 * An abstract superclass for brushes that work by putting down dabs
 */
public abstract class DabsBrush extends AbstractBrush {
    private final Spacing spacing;
    protected DabsBrushSettings settings;
    private final DabsStrategy dabsStrategy;

    protected DabsBrush(double radius, Spacing spacing,
                        AngleSettings angleSettings, boolean refreshBrushForEachDab) {
        super(radius);
        this.spacing = spacing;
        settings = new DabsBrushSettings(angleSettings, spacing);
        dabsStrategy = new LinearDabsStrategy(this,
            spacing,
            angleSettings,
            refreshBrushForEachDab);
        settings.registerBrush(this);
    }

    protected DabsBrush(double radius, DabsBrushSettings settings) {
        super(radius);
        this.settings = settings;
        spacing = settings.getSpacingStrategy();
        dabsStrategy = new LinearDabsStrategy(this,
            spacing,
            settings.getAngleSettings(),
            false);
        settings.registerBrush(this);
    }

    /**
     * Sets up the brush stamp. Depending on the type of brush, it can be
     * called at the beginning of a stroke or before each dab.
     */
    abstract void setupBrushStamp(PPoint p);

    public abstract void putDab(PPoint p, double theta);

    @Override
    public void startAt(PPoint p) {
        super.startAt(p);
        dabsStrategy.onStrokeStart(p);
        repaintComp(p);
    }

    @Override
    public void initDrawing(PPoint p) {
        super.initDrawing(p);
        setupBrushStamp(p);
    }

    @Override
    public void continueTo(PPoint p) {
        dabsStrategy.onNewStrokePoint(p);
        repaintComp(p);
        rememberPrevious(p);
    }

    public DabsBrushSettings getSettings() {
        return settings;
    }

    @Override
    public void rememberPrevious(PPoint previous) {
        super.rememberPrevious(previous);
        dabsStrategy.rememberPrevious(previous);
    }

    @Override
    public void settingsChanged() {
        dabsStrategy.settingsChanged();
    }

    @Override
    public void dispose() {
        settings.unregisterBrush(this);
    }

    @Override
    public DebugNode createDebugNode() {
        var node = super.createDebugNode();

        node.addBoolean("angle aware", settings.isAngleAware());
        node.addBoolean("jitter aware",
            settings.getAngleSettings().shouldJitterAngle());
        node.addDouble("spacing", spacing.getSpacing(radius));

        return node;
    }

    @Override
    public double getPreferredSpacing() {
        return spacing.getSpacing(radius);
    }
}
