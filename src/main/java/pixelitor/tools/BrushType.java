/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import javax.swing.*;
import java.util.IdentityHashMap;
import java.util.function.Supplier;

import static pixelitor.tools.brushes.AngleSettings.NOT_ANGLED;

/**
 * The brush types in the brush and eraser tools.
 */
public enum BrushType {
    HARD("Hard", false) {
        @Override
        public Brush createBrush(Tool tool, double radius) {
            return new HardBrush(radius);
        }
    }, SOFT("Soft", false) {
        @Override
        public Brush createBrush(Tool tool, double radius) {
            return new ImageDabsBrush(radius,
                ImageBrushType.SOFT, 0.25, NOT_ANGLED);
        }
    }, WOBBLE("Wobble", false) {
        @Override
        public Brush createBrush(Tool tool, double radius) {
            return new WobbleBrush(radius);
        }
    }, CALLIGRAPHY("Calligraphy", true) {
        @Override
        public Brush createBrush(Tool tool, double radius) {
            return new CalligraphyBrush(radius, getSettings(tool));
        }

        @Override
        public CalligraphyBrushSettings getSettings(Tool tool) {
            return (CalligraphyBrushSettings) findSettings(
                tool, CalligraphyBrushSettings::new);
        }
    }, REALISTIC("Realistic", false) {
        @Override
        public Brush createBrush(Tool tool, double radius) {
            return new ImageDabsBrush(radius,
                ImageBrushType.REAL, 0.05, NOT_ANGLED);
        }
    }, HAIR("Hair", false) {
        @Override
        public Brush createBrush(Tool tool, double radius) {
            return new ImageDabsBrush(radius,
                ImageBrushType.HAIR, 0.02, NOT_ANGLED);
        }
    }, SHAPE("Shapes", true) {
        @Override
        public Brush createBrush(Tool tool, double radius) {
            return new ShapeDabsBrush(radius, getSettings(tool));
        }

        @Override
        public ShapeDabsBrushSettings getSettings(Tool tool) {
            return (ShapeDabsBrushSettings) findSettings(tool, ShapeDabsBrushSettings::new);
        }

    }, SPRAY("Spray Shapes", true) {
        @Override
        public Brush createBrush(Tool tool, double radius) {
            return new SprayBrush(radius, getSettings(tool));
        }

        @Override
        public SprayBrushSettings getSettings(Tool tool) {
            return (SprayBrushSettings) findSettings(tool, SprayBrushSettings::new);
        }
    }, CONNECT("Connect", true) {
        @Override
        public Brush createBrush(Tool tool, double radius) {
            return new ConnectBrush(getSettings(tool), radius);
        }

        @Override
        public ConnectBrushSettings getSettings(Tool tool) {
            return (ConnectBrushSettings) findSettings(tool, ConnectBrushSettings::new);
        }
    }, OUTLINE_CIRCLE("Circles", true) {
        @Override
        public Brush createBrush(Tool tool, double radius) {
            return new OutlineCircleBrush(radius, getSettings(tool));
        }

        @Override
        public OutlineBrushSettings getSettings(Tool tool) {
            return (OutlineBrushSettings) findSettings(tool, OutlineBrushSettings::new);
        }
    }, OUTLINE_SQUARE("Squares", true) {
        @Override
        public Brush createBrush(Tool tool, double radius) {
            return new OutlineSquareBrush(radius, getSettings(tool));
        }

        @Override
        public OutlineBrushSettings getSettings(Tool tool) {
            return (OutlineBrushSettings) findSettings(tool, OutlineBrushSettings::new);
        }
    }, ONE_PIXEL("One Pixel", true) {
        @Override
        public Brush createBrush(Tool tool, double radius) {
            return new OnePixelBrush(getSettings(tool));
        }

        @Override
        public OnePixelBrushSettings getSettings(Tool tool) {
            return (OnePixelBrushSettings) findSettings(tool, OnePixelBrushSettings::new);
        }

        @Override
        public boolean hasRadius() {
            return false;
        }
    };

    private final String guiName;
    private final boolean hasSettings;

    // The settings are shared between the symmetry-brushes of a
    // tool, but they are different between the different tools
    private IdentityHashMap<Tool, BrushSettings> settingsByTool;

    BrushType(String guiName, boolean hasSettings) {
        this.guiName = guiName;
        this.hasSettings = hasSettings;
    }

    public abstract Brush createBrush(Tool tool, double radius);

    @Override
    public String toString() {
        return guiName;
    }

    public boolean hasRadius() {
        return true; // overridden if necessary
    }

    public boolean hasSettings() {
        return hasSettings;
    }

    public JPanel getConfigPanel(Tool tool) {
        assert hasSettings; // otherwise the button isn't enabled
        assert settingsByTool != null; // already initialized

        var settings = settingsByTool.get(tool);

        assert settings != null; // already initialized

        return settings.getConfigPanel();
    }


    public BrushSettings getSettings(Tool tool) {
        // overridden for brush types with settings
        throw new UnsupportedOperationException();
    }

    protected BrushSettings findSettings(Tool tool,
                                         Supplier<BrushSettings> settingsFactory) {
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
