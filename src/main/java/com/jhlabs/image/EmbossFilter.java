/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jhlabs.image;

import java.util.Objects;

/**
 * Embosses an image by treating its luminance as a height map and applying
 * Lambertian (diffuse) shading from a configurable light direction,
 * optionally with a Blinn-Phong specular highlight on top (see {@link LightingMode}).
 */
public class EmbossFilter extends WholeImageFilter {
    /**
     * The surface finish of the embossed relief. The diffuse shading is the same in
     * all modes; the modes differ only in the white Blinn-Phong highlight that is
     * added on top of it. Each mode carries its own highlight parameters.
     */
    public enum LightingMode {
        /**
         * Diffuse shading only, without any specular highlight.
         */
        MATTE("Matte", 0.0, 1.0), // the shininess is unused, because there is no highlight

        /**
         * A small, sharp and bright highlight (polished metal / chrome).
         * The intensity covers the whole 8-bit range, so the core of the highlight is pure white.
         */
        METALLIC("Metallic", 255.0, 128.0),

        /**
         * A soft, fairly wide highlight (plastic / wet paint).
         */
        GLOSSY("Glossy", 192.0, 32.0);

        private final String displayName;
        // peak brightness of the specular highlight, on the same 0..255 scale as the shade
        private final double specularIntensity;

        // Blinn-Phong exponent: higher values => smaller, sharper highlights
        // (roughly: 32 = plastic / wet paint, 128+ = chrome)
        private final double shininess;

        LightingMode(String displayName, double specularIntensity, double shininess) {
            this.displayName = displayName;
            this.specularIntensity = specularIntensity;
            this.shininess = shininess;
        }

        boolean hasSpecular() {
            return specularIntensity > 0.0;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    // so that normalized vectors scale to [0, 255] without rounding
    // up to 256, guaranteeing that the calculated shade fits in 8 bits
    private static final float PIXEL_SCALE = 255.9f;

    // the light vector components based on azimuth and elevation
    private final int Lx;
    private final int Ly;

    private final int Nz2;  // normal Z squared
    private final int NzLz; // normal Z * light Z

    // the half-vector H = normalize(L + V) for the fixed view vector V = (0, 0, 1).
    // Unlike L, it is a unit vector, so that N.H / |N| is the cosine needed by Blinn-Phong.
    // (V has no X/Y components, so the orientation of the Y axis doesn't matter here.)
    private final double Hx;
    private final double Hy;
    private final double NzHz; // normal Z * half-vector Z

    private final boolean texture;
    private final boolean specular;

    // the parameters of the specular highlight, copied from the LightingMode
    // so that the per-pixel code doesn't have to go through the enum
    private final double specularIntensity;
    private final double shininess;

    // below this cos(N, H) the highlight is truncated to 0, so the expensive Math.pow can be skipped
    private final double minSpecularCos;

    // the diffuse shading and specular highlight of flat areas and image edges,
    // where the surface normal is (0, 0, 1)
    private final int flatShade;
    private final int flatSpecular;

    /**
     * Constructs an EmbossFilter.
     *
     * @param filterName   the name of this filter
     * @param azimuth      direction of the light source, in radians
     * @param elevation    angle of the light source above the image plane, in radians
     * @param bumpHeight   controls the apparent depth of the embossed relief;
     *                     larger values produce a more pronounced effect
     * @param texture      if true, blend the shading with the original colors;
     *                     if false, produce a pure grayscale relief
     * @param lightingMode the surface finish: {@link LightingMode#MATTE} for diffuse shading only,
     *                     {@link LightingMode#GLOSSY} to add a soft Blinn-Phong highlight (a "wet paint" /
     *                     plastic look), or {@link LightingMode#METALLIC} to add a small, sharp, bright
     *                     one (a chrome look). The highlight is white, even if the texture is blended in.
     */
    public EmbossFilter(String filterName,
                        float azimuth,
                        float elevation,
                        float bumpHeight,
                        boolean texture,
                        LightingMode lightingMode) {
        super(filterName);

        Objects.requireNonNull(lightingMode, "lightingMode");

        // the UI ensures that bumpHeight can only have the following
        // values (exponential growth sequence): 0.02, 0.04, 0.08, 0.16, 0.32,
        // 0.64, 1.28, 2.56, 5.12, 10.24, 20.48, 40.96, 81.92, 163.84, 327.68
        assert bumpHeight > 0.019 && bumpHeight < 328;
        assert elevation >= 0 && elevation <= ImageMath.PI_F / 2;

        // rotate by π so that azimuth 0 lights from the left, matching the UI arrow direction
        azimuth += ImageMath.PI_F;

        // larger bumpHeight => smaller normalZ => the gradient terms (normalX, normalY)
        // dominate the surface normal more strongly => a more pronounced 3D relief
        float depthScale = 3 * bumpHeight;

        double cosElevation = Math.cos(elevation);
        double lx = Math.cos(azimuth) * cosElevation;
        double ly = Math.sin(azimuth) * cosElevation;
        double lz = Math.sin(elevation);
        this.Lx = (int) (lx * PIXEL_SCALE);
        this.Ly = (int) (ly * PIXEL_SCALE);
        this.flatShade = (int) (lz * PIXEL_SCALE);

        int Nz = (int) (6 * 255 / depthScale);
        this.Nz2 = Nz * Nz;
        this.NzLz = Nz * this.flatShade;

        // H = normalize(L + V) with V = (0, 0, 1). The length is never 0, because lz >= 0.
        double hLength = Math.sqrt(lx * lx + ly * ly + (lz + 1) * (lz + 1));
        double hz = (lz + 1) / hLength;
        this.Hx = lx / hLength;
        this.Hy = ly / hLength;
        this.NzHz = Nz * hz;

        this.texture = texture;

        // the fields used by specularHighlight() must be assigned before it is called
        this.specular = lightingMode.hasSpecular();
        this.specularIntensity = lightingMode.specularIntensity;
        this.shininess = lightingMode.shininess;
        // with no highlight, the cutoff is never consulted, but an infinite
        // one is the consistent value: every cosine is below it
        this.minSpecularCos = specular
            ? Math.pow(1.0 / specularIntensity, 1.0 / shininess)
            : Double.POSITIVE_INFINITY;

        // a flat surface has the normal (0, 0, 1), so N.H = Hz
        this.flatSpecular = specular ? specularHighlight(hz) : 0;
    }

    /**
     * Returns the brightness (0..specularIntensity) of the Blinn-Phong highlight
     * for the given cosine of the angle between the surface normal and the half-vector.
     */
    private int specularHighlight(double cosNH) {
        if (cosNH < minSpecularCos) {
            return 0; // also covers negative cosines
        }
        return (int) (specularIntensity * Math.pow(cosNH, shininess));
    }

    @Override
    protected int[] filterPixels(int width, int height, int[] inPixels) {
        pt = createProgressTracker(height);

        // a bump map is derived from the brightness values of the
        // input image, and represents the surface's height map
        int[] bumpPixels = ImageMath.calcLuminanceInt(inPixels);

        int[] outPixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            boolean interiorRow = y > 0 && y < height - 1;
            for (int x = 0; x < width; x++) {
                int i = y * width + x;

                // baseline shading and highlight for edge pixels and flat areas
                int shade = flatShade;
                int spec = flatSpecular;

                if (interiorRow && x > 0 && x < width - 1) {
                    int above = i - width;
                    int below = i + width;

                    // surface normal gradient kernel
                    int nx = bumpPixels[above - 1] + bumpPixels[i - 1] + bumpPixels[below - 1]
                        - bumpPixels[above + 1] - bumpPixels[i + 1] - bumpPixels[below + 1];
                    int ny = bumpPixels[below - 1] + bumpPixels[below] + bumpPixels[below + 1]
                        - bumpPixels[above - 1] - bumpPixels[above] - bumpPixels[above + 1];

                    if (nx != 0 || ny != 0) {
                        // dot product between the surface normal and the light vector
                        int nDotL = nx * Lx + ny * Ly + NzLz;

                        if (nDotL < 0) {
                            shade = 0; // shadow
                            spec = 0;  // facing away from light: no specular reflection
                        } else {
                            // surface is facing the light
                            double normN = Math.sqrt(nx * nx + ny * ny + Nz2);
                            shade = (int) (nDotL / normN);

                            // Blinn-Phong: cos(N, H) = (N . H) / |N|
                            spec = specular
                                ? specularHighlight((nx * Hx + ny * Hy + NzHz) / normN)
                                : 0;
                        }
                    }
                }

                int rgb = inPixels[i];
                int a = rgb & 0xFF_00_00_00;
                if (texture) {
                    // blend the shading with the original image's colors
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    // the highlight is added after modulating the color, because a specular
                    // reflection has the color of the light, not the color of the surface
                    r = Math.min(255, ((r * shade) >> 8) + spec);
                    g = Math.min(255, ((g * shade) >> 8) + spec);
                    b = Math.min(255, ((b * shade) >> 8) + spec);
                    outPixels[i] = a | (r << 16) | (g << 8) | b;
                } else {
                    // create grayscale output
                    shade = Math.min(255, shade + spec);
                    outPixels[i] = a | (shade << 16) | (shade << 8) | shade;
                }
            }
            pt.unitDone();
        }
        finishProgressTracker();

        return outPixels;
    }
}
