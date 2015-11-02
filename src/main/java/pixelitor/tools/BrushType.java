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

package pixelitor.tools;

import pixelitor.tools.brushes.*;

import javax.swing.*;
import java.util.IdentityHashMap;
import java.util.Map;

import static pixelitor.tools.brushes.AngleSettings.ANGLE_AWARE_NO_JITTER;
import static pixelitor.tools.brushes.AngleSettings.NOT_ANGLE_AWARE;

/**
 * The brush types the user can use
 */
public enum BrushType {
    IDEAL("Hard", false) {
        @Override
        public Brush createBrush(Tool tool, int radius) {
            return new IdealBrush(radius);
        }
    }, SOFT("Soft", false) {
        @Override
        public Brush createBrush(Tool tool, int radius) {
            return new ImageDabsBrush(radius, ImageBrushType.SOFT, 0.25, NOT_ANGLE_AWARE);
        }
    }, WOBBLE("Wobble", false) {
        @Override
        public Brush createBrush(Tool tool, int radius) {
            return new WobbleBrush(radius);
        }
    }, CALLIGRAPHY("Calligraphy", false) {
        @Override
        public Brush createBrush(Tool tool, int radius) {
            return new CalligraphyBrush(radius);
        }
    }, REALISTIC("Realistic", false) {
        @Override
        public Brush createBrush(Tool tool, int radius) {
            return new ImageDabsBrush(radius, ImageBrushType.REAL, 0.05, NOT_ANGLE_AWARE);
        }
    }, HAIR("Hair", false) {
        @Override
        public Brush createBrush(Tool tool, int radius) {
            return new ImageDabsBrush(radius, ImageBrushType.HAIR, 0.02, NOT_ANGLE_AWARE);
        }
    }, SHAPE("Shape", true) {
        @Override
        public Brush createBrush(Tool tool, int radius) {
            ShapeDabsBrushSettings settings = (ShapeDabsBrushSettings) findSettings(tool);
            if (settings == null) {
                ShapeType shapeType = BrushSettingsPanel.SHAPE_SELECTED_BY_DEFAULT;
                double spacingRatio = BrushSettingsPanel.DEFAULT_SPACING_RATIO;
                AngleSettings angleSettings = ANGLE_AWARE_NO_JITTER;
                RadiusRatioSpacing spacing = new RadiusRatioSpacing(spacingRatio);

                ShapeDabsBrush shapeDabsBrush = new ShapeDabsBrush(radius, shapeType, spacing, angleSettings);
                settings = (ShapeDabsBrushSettings) shapeDabsBrush.getSettings();
                settingsByTool.put(tool, settings);
                return shapeDabsBrush;
            } else {
                Brush shapeDabsBrush = new ShapeDabsBrush(radius, settings);
                return shapeDabsBrush;
            }
        }

        //    }, ARROW("Image-Based Arrow") {
//        @Override
//        public Brush createBrush(Tool tool, int radius) {
//            return new ImageDabsBrush(radius, ImageBrushType.ARROW, 2.5, true);
//        }
//    }, GREEK("Image-Based Greek") {
//        @Override
//        public Brush createBrush(Tool tool, int radius) {
//            return new ImageDabsBrush(radius, ImageBrushType.GREEK, 2.0, true);
//        }
    }, OUTLINE_CIRCLE("Circles", false) {
        @Override
        public Brush createBrush(Tool tool, int radius) {
            return new OutlineCircleBrush(radius);
        }
    }, OUTLINE_SQUARE("Squares", false) {
        @Override
        public Brush createBrush(Tool tool, int radius) {
            return new OutlineSquareBrush(radius);
        }
    }, ONE_PIXEL("One Pixel", true) {
        @Override
        public Brush createBrush(Tool tool, int radius) {
            OnePixelBrushSettings settings = (OnePixelBrushSettings) findSettings(tool);
            if (settings == null) {
                settings = new OnePixelBrushSettings();
                settingsByTool.put(tool, settings);
                return new OnePixelBrush(settings);
            } else {
                return new OnePixelBrush(settings);
            }


        }

        @Override
        public boolean sizeCanBeSet() {
            return false;
        }
    };

    private final String guiName;
    private final boolean hasSettings;

    // The settings must be shared between the symmetry-brushes of a tool, but
    // they must be different between the different tools
    protected Map<Tool, BrushSettings> settingsByTool;


    BrushType(String guiName, boolean hasSettings) {
        this.guiName = guiName;
        this.hasSettings = hasSettings;
    }

    public abstract Brush createBrush(Tool tool, int radius);

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

    public JPanel getSettingsPanel(Tool tool) {
        assert hasSettings; // otherwise the button is not enabled
        assert settingsByTool != null; // already initialized

        BrushSettings settings = settingsByTool.get(tool);

        assert settings != null; // already initialized

        return settings.getConfigurationPanel();
    }

    protected BrushSettings findSettings(Tool tool) {
        if (settingsByTool == null) {
            settingsByTool = new IdentityHashMap<>();
            return null;
        } else {
            return settingsByTool.get(tool);
        }
    }
}
