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

package pixelitor.tools;

import pixelitor.tools.brushes.*;

import javax.swing.*;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static pixelitor.tools.brushes.AngleSettings.ANGLE_AWARE_NO_JITTER;
import static pixelitor.tools.brushes.AngleSettings.NOT_ANGLE_AWARE;

/**
 * The brush types the user can use
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
            return new ImageDabsBrush(radius, ImageBrushType.SOFT, 0.25, NOT_ANGLE_AWARE);
        }
    }, WOBBLE("Wobble", false) {
        @Override
        public Brush createBrush(Tool tool, double radius) {
            return new WobbleBrush(radius);
        }
    }, CALLIGRAPHY("Calligraphy", true) {
        @Override
        public Brush createBrush(Tool tool, double radius) {
            var settings = (CalligraphyBrushSettings) findSettings(
                    tool, CalligraphyBrushSettings::new);
            return new CalligraphyBrush(radius, settings);
        }
    }, REALISTIC("Realistic", false) {
        @Override
        public Brush createBrush(Tool tool, double radius) {
            return new ImageDabsBrush(radius, ImageBrushType.REAL, 0.05, NOT_ANGLE_AWARE);
        }
    }, HAIR("Hair", false) {
        @Override
        public Brush createBrush(Tool tool, double radius) {
            return new ImageDabsBrush(radius, ImageBrushType.HAIR, 0.02, NOT_ANGLE_AWARE);
        }
    }, SHAPE("Shapes", true) {
        @Override
        public Brush createBrush(Tool tool, double radius) {
            var settings = (ShapeDabsBrushSettings) findSettings(
                    tool, this::createShapeDabsBrushSettings);
            return new ShapeDabsBrush(radius, settings);
        }

        private ShapeDabsBrushSettings createShapeDabsBrushSettings() {
            var shapeType = ShapeDabsBrushSettingsPanel.DEFAULT_SHAPE;
            double spacingRatio = ShapeDabsBrushSettingsPanel.DEFAULT_SPACING_RATIO;
            var spacing = new RadiusRatioSpacing(spacingRatio);
            return new ShapeDabsBrushSettings(
                    ANGLE_AWARE_NO_JITTER,
                    spacing,
                    shapeType
            );
        }

        //    }, ARROW("Image-Based Arrow") {
//        @Override
//        public Brush createBrush(Tool tool, double radius) {
//            return new ImageDabsBrush(radius, ImageBrushType.ARROW, 2.5, true);
//        }
//    }, GREEK("Image-Based Greek") {
//        @Override
//        public Brush createBrush(Tool tool, double radius) {
//            return new ImageDabsBrush(radius, ImageBrushType.GREEK, 2.0, true);
//        }
    }, SPRAY("Spray Shapes", true) {
        @Override
        public Brush createBrush(Tool tool, double radius) {
            var settings = (SprayBrushSettings) findSettings(
                    tool, SprayBrushSettings::new);
            return new SprayBrush(radius, settings);
        }
    }, CONNECT("Connect", true) {
        @Override
        public Brush createBrush(Tool tool, double radius) {
            var settings = (ConnectBrushSettings) findSettings(
                    tool, ConnectBrushSettings::new);
            return new ConnectBrush(settings, radius);
        }
    }, OUTLINE_CIRCLE("Circles", true) {
        @Override
        public Brush createBrush(Tool tool, double radius) {
            var settings = (OutlineBrushSettings) findSettings(
                    tool, OutlineBrushSettings::new);
            return new OutlineCircleBrush(radius, settings);
        }
    }, OUTLINE_SQUARE("Squares", true) {
        @Override
        public Brush createBrush(Tool tool, double radius) {
            var settings = (OutlineBrushSettings) findSettings(
                    tool, OutlineBrushSettings::new);
            return new OutlineSquareBrush(radius, settings);
        }
    }, ONE_PIXEL("One Pixel", true) {
        @Override
        public Brush createBrush(Tool tool, double radius) {
            var settings = (OnePixelBrushSettings) findSettings(
                    tool, OnePixelBrushSettings::new);
            return new OnePixelBrush(settings);
        }

        @Override
        public boolean sizeCanBeSet() {
            return false;
        }
    };

    private final String guiName;
    private final boolean hasSettings;

    // The settings must be shared between the symmetry-brushes of a
    // tool, but they must be different between the different tools
    private Map<Tool, BrushSettings> settingsByTool;

    BrushType(String guiName, boolean hasSettings) {
        this.guiName = guiName;
        this.hasSettings = hasSettings;
    }

    public abstract Brush createBrush(Tool tool, double radius);

    @Override
    public String toString() {
        return guiName;
    }

    public boolean sizeCanBeSet() {
        return true; // intended to be overridden if necessary
    }

    public boolean hasSettings() {
        return hasSettings;
    }

    public JPanel getConfigPanel(Tool tool) {
        assert hasSettings; // otherwise the button is not enabled
        assert settingsByTool != null; // already initialized

        var settingsForTool = settingsByTool.get(tool);

        assert settingsForTool != null; // already initialized

        return settingsForTool.getConfigPanel();
    }

    protected BrushSettings findSettings(Tool tool, Supplier<BrushSettings> settingsCreator) {
        BrushSettings settings = null;
        if (settingsByTool == null) {
            settingsByTool = new IdentityHashMap<>();
        } else {
            settings = settingsByTool.get(tool);
        }
        if (settings == null) {
            settings = settingsCreator.get();
            settings.setTool(tool);
            settingsByTool.put(tool, settings);
        }
        assert settings != null;
        return settings;
    }
}
