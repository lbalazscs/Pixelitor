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

import pixelitor.tools.util.PPoint;
import pixelitor.utils.debug.DebugNode;

/**
 * Abstract base class for brushes that apply discrete dabs (marks).
 */
public abstract class DabsBrush extends AbstractBrush {
    private final Spacing spacing;
    protected final DabsBrushSettings settings;
    private final DabsStrategy dabsStrategy;

    protected DabsBrush(double radius, Spacing spacing,
                        AngleSettings angleSettings,
                        boolean refreshBrushForEachDab) {
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
     * Prepares the brush stamp. This method may be called at the start
     * of a stroke or before each dab, depending on the brush type.
     */
    abstract void initBrushStamp(PPoint p);

    /**
     * Applies a single dab at the given location and angle.
     */
    public abstract void putDab(PPoint currentPoint, double angle);

    @Override
    public void startAt(PPoint p) {
        super.startAt(p);
        dabsStrategy.onStrokeStart(p);
        repaintComp(p);
    }

    @Override
    public void initDrawing(PPoint p) {
        super.initDrawing(p);
        initBrushStamp(p);
    }

    @Override
    public void continueTo(PPoint p) {
        dabsStrategy.onNewStrokePoint(p);
        repaintComp(p);
        this.setPrevious(p);
    }

    @Override
    public void setPrevious(PPoint previous) {
        super.setPrevious(previous);
        dabsStrategy.setPrevious(previous);
    }

    @Override
    public void settingsChanged() {
        dabsStrategy.settingsChanged();
    }

    @Override
    public void dispose() {
        settings.unregisterBrush(this);
    }

    public DabsBrushSettings getSettings() {
        return settings;
    }

    @Override
    public double getPreferredSpacing() {
        return spacing.getSpacing(radius);
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.addBoolean("angled", settings.isAngled());
        node.addBoolean("jittered",
            settings.getAngleSettings().isJitterEnabled());
        node.addDouble("spacing", getPreferredSpacing());

        return node;
    }
}
