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

package pixelitor.tools;

import pixelitor.tools.brushes.*;

import java.util.IdentityHashMap;
import java.util.function.Supplier;

import static pixelitor.tools.brushes.AngleSettings.NOT_ANGLED;

/**
 * The brush types in the brush and eraser tools.
 */
public enum BrushType {
    HARD("Hard", null) {
        @Override
        public Brush createBrush(AbstractBrushTool tool, double radius) {
            return new HardBrush(radius);
        }
    }, SOFT("Soft", null) {
        @Override
        public Brush createBrush(AbstractBrushTool tool, double radius) {
            return new ImageDabsBrush(radius,
                ImageBrushType.SOFT, 0.25, NOT_ANGLED);
        }
    }, WOBBLE("Wobble", null) {
        @Override
        public Brush createBrush(AbstractBrushTool tool, double radius) {
            return new WobbleBrush(radius);
        }
    }, CALLIGRAPHY("Calligraphy", CalligraphyBrushSettings::new) {
        @Override
        public Brush createBrush(AbstractBrushTool tool, double radius) {
            return new CalligraphyBrush(radius, (CalligraphyBrushSettings) getSettings(tool));
        }
    }, REALISTIC("Realistic", null) {
        @Override
        public Brush createBrush(AbstractBrushTool tool, double radius) {
            return new ImageDabsBrush(radius,
                ImageBrushType.REAL, 0.05, NOT_ANGLED);
        }
    }, HAIR("Hair", null) {
        @Override
        public Brush createBrush(AbstractBrushTool tool, double radius) {
            return new ImageDabsBrush(radius,
                ImageBrushType.HAIR, 0.02, NOT_ANGLED);
        }
    }, SHAPE("Shapes", ShapeDabsBrushSettings::new) {
        @Override
        public Brush createBrush(AbstractBrushTool tool, double radius) {
            return new ShapeDabsBrush(radius, (ShapeDabsBrushSettings) getSettings(tool));
        }
    }, SPRAY("Spray Shapes", SprayBrushSettings::new) {
        @Override
        public Brush createBrush(AbstractBrushTool tool, double radius) {
            return new SprayBrush(radius, (SprayBrushSettings) getSettings(tool));
        }
    }, CONNECT("Connect", ConnectBrushSettings::new) {
        @Override
        public Brush createBrush(AbstractBrushTool tool, double radius) {
            return new ConnectBrush((ConnectBrushSettings) getSettings(tool), radius);
        }
    }, OUTLINE_CIRCLE("Circles", OutlineBrushSettings::new) {
        @Override
        public Brush createBrush(AbstractBrushTool tool, double radius) {
            return new OutlineBrush(this, radius, (OutlineBrushSettings) getSettings(tool));
        }
    }, OUTLINE_SQUARE("Squares", OutlineBrushSettings::new) {
        @Override
        public Brush createBrush(AbstractBrushTool tool, double radius) {
            return new OutlineBrush(this, radius, (OutlineBrushSettings) getSettings(tool));
        }
    }, ONE_PIXEL("One Pixel", OnePixelBrushSettings::new) {
        @Override
        public Brush createBrush(AbstractBrushTool tool, double radius) {
            return new OnePixelBrush((OnePixelBrushSettings) getSettings(tool));
        }

        @Override
        public boolean hasRadius() {
            return false;
        }
    };

    private final String displayName;
    private final boolean hasSettings;
    private final Supplier<BrushSettings> settingsFactory;

    // The settings are shared between the symmetry-brushes of a
    // tool, but they are different between the different tools
    private IdentityHashMap<AbstractBrushTool, BrushSettings> settingsByTool;

    BrushType(String displayName, Supplier<BrushSettings> settingsFactory) {
        this.displayName = displayName;
        this.hasSettings = settingsFactory != null;
        this.settingsFactory = settingsFactory;
    }

    public abstract Brush createBrush(AbstractBrushTool tool, double radius);

    @Override
    public String toString() {
        return displayName;
    }

    public boolean hasRadius() {
        return true; // overridden if necessary
    }

    public boolean hasSettings() {
        return hasSettings;
    }

    /**
     * Returns the settings tied to the {@link AbstractBrushTool} and {@link BrushType} combination
     */
    public BrushSettings getSettings(AbstractBrushTool tool) {
        assert hasSettings;

        BrushSettings settings = null;
        if (settingsByTool == null) {
            settingsByTool = new IdentityHashMap<>();
        } else {
            settings = settingsByTool.get(tool);
        }
        if (settings == null) {
            settings = settingsFactory.get();
            settings.setTool(tool);
            settingsByTool.put(tool, settings);
        }
        return settings;
    }
}
