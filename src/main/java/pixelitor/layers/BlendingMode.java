/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
package pixelitor.layers;

import com.jhlabs.composite.*;
import pixelitor.gui.GUIText;

import java.awt.AlphaComposite;
import java.awt.Composite;

import static java.awt.AlphaComposite.DST_OUT;

/**
 * The blending modes
 */
public enum BlendingMode {
    PASS_THROUGH("Pass Through", "") {
        @Override
        public Composite getComposite(float opacity) {
            throw new IllegalStateException();
        }
    }, NORMAL("Normal", "svg:src-over") {
        @Override
        public Composite getComposite(float opacity) {
            return AlphaComposite.SrcOver.derive(opacity);
        }
    }, DARKEN("Darken", "svg:darken") {
        @Override
        public Composite getComposite(float opacity) {
            return new DarkenComposite(opacity);
        }
    }, MULTIPLY("Multiply", "svg:multiply") {
        @Override
        public Composite getComposite(float opacity) {
            return new MultiplyComposite(opacity);
        }
    }, COLOR_BURN("Color Burn", "svg:color-burn") {
        @Override
        public Composite getComposite(float opacity) {
            return new ColorBurnComposite(opacity);
        }
    }, LIGHTEN("Lighten", "svg:lighten") {
        @Override
        public Composite getComposite(float opacity) {
            return new LightenComposite(opacity);
        }
    }, SCREEN("Screen", "svg:screen") {
        @Override
        public Composite getComposite(float opacity) {
            return new ScreenComposite(opacity);
        }
    }, COLOR_DODGE("Color Dodge", "svg:color-dodge") {
        @Override
        public Composite getComposite(float opacity) {
            return new ColorDodgeComposite(opacity);
        }
    }, LINEAR_DODGE("Linear Dodge (Add)", "svg:plus") {
        @Override
        public Composite getComposite(float opacity) {
            return new AddComposite(opacity);
        }
    }, OVERLAY("Overlay", "svg:overlay") {
        @Override
        public Composite getComposite(float opacity) {
            return new OverlayComposite(opacity);
        }
    }, SOFT_LIGHT("Soft Light", "svg:soft-light") {
        @Override
        public Composite getComposite(float opacity) {
            return new SoftLightComposite(opacity);
        }
    }, HARD_LIGHT("Hard Light", "svg:hard-light") {
        @Override
        public Composite getComposite(float opacity) {
            return new HardLightComposite(opacity);
        }
    }, DIFFERENCE("Difference", "svg:difference") {
        @Override
        public Composite getComposite(float opacity) {
            return new DifferenceComposite(opacity);
        }
    }, EXCLUSION("Exclusion", "svg:exclusion") {
        @Override
        public Composite getComposite(float opacity) {
            return new ExclusionComposite(opacity);
        }
    }, HUE(GUIText.HUE, "svg:hue") {
        @Override
        public Composite getComposite(float opacity) {
            return new HueComposite(opacity);
        }
    }, SATURATION(GUIText.SATURATION, "svg:saturation") {
        @Override
        public Composite getComposite(float opacity) {
            return new SaturationComposite(opacity);
        }
    }, COLOR(GUIText.COLOR, "svg:color") {
        @Override
        public Composite getComposite(float opacity) {
            return new ColorComposite(opacity);
        }
    }, LUMINOSITY(GUIText.BRIGHTNESS, "svg:luminosity") {
        @Override
        public Composite getComposite(float opacity) {
            return new ValueComposite(opacity);
        }
    }, ERASE("Erase", "svg:dst-out") {
        @Override
        public Composite getComposite(float opacity) {
            return AlphaComposite.getInstance(DST_OUT, opacity);
        }
    };

    public static final BlendingMode[] ALL_MODES = values();
    public static final BlendingMode[] LAYER_MODES;

    static {
        // skip the first mode (pass through)
        LAYER_MODES = new BlendingMode[ALL_MODES.length - 1];
        for (int i = 0; i < LAYER_MODES.length; i++) {
            LAYER_MODES[i] = ALL_MODES[i + 1];
        }
    }

    public static final String PRESET_KEY = "Blending Mode";
    private final String displayName;
    private final String svgName; // used by the OpenRaster import-export

    BlendingMode(String displayName, String svgName) {
        this.displayName = displayName;
        this.svgName = svgName;
    }

    public abstract Composite getComposite(float opacity);

    public static BlendingMode fromSVGName(String svgName) {
        for (BlendingMode mode : LAYER_MODES) {
            String modeSVGName = mode.toSVGName();
            if (modeSVGName.equals(svgName)) {
                return mode;
            }
        }
        return NORMAL;
    }

    public String toSVGName() {
        return svgName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
