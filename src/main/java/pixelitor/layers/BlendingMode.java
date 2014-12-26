/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.layers;

import com.jhlabs.composite.ColorBurnComposite;
import com.jhlabs.composite.ColorComposite;
import com.jhlabs.composite.DarkenComposite;
import com.jhlabs.composite.HardLightComposite;
import com.jhlabs.composite.HueComposite;
import com.jhlabs.composite.MultiplyComposite;
import com.jhlabs.composite.OverlayComposite;
import com.jhlabs.composite.SaturationComposite;
import com.jhlabs.composite.ScreenComposite;
import com.jhlabs.composite.SoftLightComposite;
import com.jhlabs.composite.ValueComposite;
import org.jdesktop.swingx.graphics.BlendComposite;

import java.awt.AlphaComposite;
import java.awt.Composite;

/**
 * The blending modes
 */
public enum BlendingMode {
    NORMAL("Normal", "svg:src-over") {
        @Override
        public Composite getComposite(float opacity) {
            return AlphaComposite.SrcOver.derive(opacity);
        }
    }, DARKEN("Darken", "svg:darken") {
        @Override
        public Composite getComposite(float opacity) {
//            return BlendComposite.Darken;
            return new DarkenComposite(opacity);
        }
    },
    MULTIPLY("Multiply", "svg:multiply") {
        @Override
        public Composite getComposite(float opacity) {
//            return BlendComposite.Multiply;
            return new MultiplyComposite(opacity);
        }
    }, COLOR_BURN("Color Burn", "svg:color-burn") {
        @Override
        public Composite getComposite(float opacity) {
//            return BlendComposite.ColorBurn;
            return new ColorBurnComposite(opacity);
        }
    }, LIGHTEN("Lighten", "svg:lighten") {
        @Override
        public Composite getComposite(float opacity) {
            return BlendComposite.Lighten;
        }
    }, SCREEN("Screen", "svg:screen") {
        @Override
        public Composite getComposite(float opacity) {
//            return BlendComposite.Screen;
            return new ScreenComposite(opacity);
        }
    }, COLOR_DODGE("Color Dodge", "svg:color-dodge") {
        @Override
        public Composite getComposite(float opacity) {
            return BlendComposite.ColorDodge;
        }
    }, LINEAR_DODGE("Linear Dodge (Add)", "svg:plus") {
        @Override
        public Composite getComposite(float opacity) {
            return BlendComposite.Add;
        }
    }, OVERLAY("Overlay", "svg:overlay") {
        @Override
        public Composite getComposite(float opacity) {
//            return BlendComposite.Overlay;
            return new OverlayComposite(opacity);
        }
    }, SOFT_LIGHT("Soft Light", "svg:soft-light") {
        @Override
        public Composite getComposite(float opacity) {
//            return BlendComposite.SoftLight;
            return new SoftLightComposite(opacity);
        }
    }, HARD_LIGHT("Hard Light", "svg:hard-light") {
        @Override
        public Composite getComposite(float opacity) {
//            return BlendComposite.HardLight;
            return new HardLightComposite(opacity);
        }
    }, DIFFERENCE("Difference", "svg:difference") {
        @Override
        public Composite getComposite(float opacity) {
            return BlendComposite.Difference;
        }
    }, EXCLUSION("Exclusion", "svg:exclusion") {
        @Override
        public Composite getComposite(float opacity) {
            return BlendComposite.Exclusion;
        }
    }, HUE("Hue", "svg:hue") {
        @Override
        public Composite getComposite(float opacity) {
//            return BlendComposite.Hue;
            return new HueComposite(opacity);
        }
    }, SATURATION("Saturation", "svg:saturation") {
        @Override
        public Composite getComposite(float opacity) {
//            return BlendComposite.Saturation;
            return new SaturationComposite(opacity);
        }
    }, COLOR("Color", "svg:color") {
        @Override
        public Composite getComposite(float opacity) {
//            return BlendComposite.Color;
            return new ColorComposite(opacity);
        }
    }, LUMINOSITY("Value", "svg:luminosity") {
        @Override
        public Composite getComposite(float opacity) {
//            return BlendComposite.Luminosity;
            return new ValueComposite(opacity);
        }
    };

    private final String guiName;
    private final String svgName;

    BlendingMode(String guiName, String svgName) {
        this.guiName = guiName;
        this.svgName = svgName;
    }

    public abstract Composite getComposite(float opacity);

    public String toSVGName() {
        return svgName;
    }

    @Override
    public String toString() {
        return guiName;
    }

    public static BlendingMode fromSVGName(String svgName) {
        BlendingMode[] values = values();
        for (BlendingMode mode : values) {
            String modeSVGName = mode.toSVGName();
            if(modeSVGName.equals(svgName)) {
                return mode;
            }
        }
        return NORMAL;
    }
}
