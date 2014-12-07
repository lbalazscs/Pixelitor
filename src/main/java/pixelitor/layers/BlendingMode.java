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
    NORMAL {
        @Override
        public Composite getComposite(float opacity) {
            return AlphaComposite.SrcOver.derive(opacity);
        }

        @Override
        public String toString() {
            return "Normal";
        }

        @Override
        public String toSVGName() {
            return "svg:src-over";
        }
    }, DARKEN {
        @Override
        public Composite getComposite(float opacity) {
//            return BlendComposite.Darken;
            return new DarkenComposite(opacity);
        }

        @Override
        public String toString() {
            return "Darken";
        }

        @Override
        public String toSVGName() {
            return "svg:darken";
        }
    },
    MULTIPLY {
        @Override
        public Composite getComposite(float opacity) {
//            return BlendComposite.Multiply;
            return new MultiplyComposite(opacity);
        }

        @Override
        public String toString() {
            return "Multiply";
        }

        @Override
        public String toSVGName() {
            return "svg:multiply";
        }
    }, COLOR_BURN {
        @Override
        public Composite getComposite(float opacity) {
//            return BlendComposite.ColorBurn;
            return new ColorBurnComposite(opacity);
        }

        @Override
        public String toString() {
            return "Color Burn";
        }

        @Override
        public String toSVGName() {
            return "svg:color-burn";
        }
    }, LIGHTEN {
        @Override
        public Composite getComposite(float opacity) {
            return BlendComposite.Lighten;
        }

        @Override
        public String toString() {
            return "Lighten";
        }

        @Override
        public String toSVGName() {
            return "svg:lighten";
        }
    }, SCREEN {
        @Override
        public Composite getComposite(float opacity) {
//            return BlendComposite.Screen;
            return new ScreenComposite(opacity);
        }

        @Override
        public String toString() {
            return "Screen";
        }


        @Override
        public String toSVGName() {
            return "svg:screen";
        }
    }, COLOR_DODGE {
        @Override
        public Composite getComposite(float opacity) {
            return BlendComposite.ColorDodge;
        }

        @Override
        public String toString() {
            return "Color Dodge";
        }


        @Override
        public String toSVGName() {
            return "svg:color-dodge";
        }
    }, LINEAR_DODGE {
        @Override
        public Composite getComposite(float opacity) {
            return BlendComposite.Add;
        }

        @Override
        public String toString() {
            return "Linear Dodge (Add)";
        }

        @Override
        public String toSVGName() {
            return "svg:plus";
        }
    }, OVERLAY {
        @Override
        public Composite getComposite(float opacity) {
//            return BlendComposite.Overlay;
            return new OverlayComposite(opacity);
        }

        @Override
        public String toString() {
            return "Overlay";
        }

        @Override
        public String toSVGName() {
            return "svg:overlay";
        }
    }, SOFT_LIGHT {
        @Override
        public Composite getComposite(float opacity) {
//            return BlendComposite.SoftLight;
            return new SoftLightComposite(opacity);
        }

        @Override
        public String toString() {
            return "Soft Light";
        }

        @Override
        public String toSVGName() {
            return "svg:soft-light";
        }
    }, HARD_LIGHT {
        @Override
        public Composite getComposite(float opacity) {
//            return BlendComposite.HardLight;
            return new HardLightComposite(opacity);
        }

        @Override
        public String toString() {
            return "Hard Light";
        }

        @Override
        public String toSVGName() {
            return "svg:hard-light";
        }
    }, DIFFERENCE {
        @Override
        public Composite getComposite(float opacity) {
            return BlendComposite.Difference;
        }

        @Override
        public String toString() {
            return "Difference";
        }

        @Override
        public String toSVGName() {
            return "svg:difference";
        }
    }, EXCLUSION {
        @Override
        public Composite getComposite(float opacity) {
            return BlendComposite.Exclusion;
        }

        @Override
        public String toString() {
            return "Exclusion";
        }

        @Override
        public String toSVGName() {
            return "svg:exclusion";
        }
    }, HUE {
        @Override
        public Composite getComposite(float opacity) {
//            return BlendComposite.Hue;
            return new HueComposite(opacity);
        }

        @Override
        public String toString() {
            return "Hue";
        }

        @Override
        public String toSVGName() {
            return "svg:hue";
        }
    }, SATURATION {
        @Override
        public Composite getComposite(float opacity) {
//            return BlendComposite.Saturation;
            return new SaturationComposite(opacity);
        }

        @Override
        public String toString() {
            return "Saturation";
        }

        @Override
        public String toSVGName() {
            return "svg:saturation";
        }
    }, COLOR {
        @Override
        public Composite getComposite(float opacity) {
//            return BlendComposite.Color;
            return new ColorComposite(opacity);
        }

        @Override
        public String toString() {
            return "Color";
        }

        @Override
        public String toSVGName() {
            return "svg:color";
        }
    }, LUMINOSITY {
        @Override
        public Composite getComposite(float opacity) {
//            return BlendComposite.Luminosity;
            return new ValueComposite(opacity);
        }

        @Override
        public String toString() {
//            return "Luminosity";
            return "Value";
        }

        @Override
        public String toSVGName() {
            return "svg:luminosity";
        }
    };

    public abstract Composite getComposite(float opacity);
    public abstract String toSVGName();

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
