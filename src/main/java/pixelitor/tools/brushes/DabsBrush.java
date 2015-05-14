/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

/**
 * A brush that works by putting down dabs
 */
public abstract class DabsBrush extends AbstractBrush {
    private SpacingStrategy spacingStrategy;
    protected final AngleSettings angleSettings;
    private final DabsStrategy dabsStrategy;

    protected DabsBrush(SpacingStrategy spacingStrategy, AngleSettings angleSettings, boolean refreshBrushForEachDab) {
        this.spacingStrategy = spacingStrategy;
        this.angleSettings = angleSettings;
        this.dabsStrategy = new LinearDabsStrategy(this, spacingStrategy, angleSettings, refreshBrushForEachDab);
    }

    /**
     * Sets up the brush stamp. Depending on the type of brush, it can be
     * called at the beginning of a stroke or before each dab.
     */
    abstract void setupBrushStamp(double x, double y);

    public abstract void putDab(double x, double y, double theta);

    @Override
    public void onDragStart(int x, int y) {
        dabsStrategy.onDragStart(x, y);
        updateComp(x, y);
    }

    @Override
    public void onNewMousePoint(int x, int y) {
        dabsStrategy.onNewMousePoint(x, y);
        updateComp(x, y);
    }

    @Override
    public void setRadius(int radius) {
        super.setRadius(radius);
        spacingStrategy.setRadius(radius);
    }

    public void changeSpacing(SpacingStrategy spacingStrategy) {
        spacingStrategy.setRadius(radius);
        this.spacingStrategy = spacingStrategy;
        dabsStrategy.changeSpacing(spacingStrategy);
    }
}
