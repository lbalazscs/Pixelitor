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

/**
 * Embosses an image by treating its luminance as a height map and applying
 * Lambertian (diffuse) shading from a configurable light direction.
 */
public class EmbossFilter extends WholeImageFilter {
    private static final float PIXEL_SCALE = 255.9f;

    // the light vector components based on azimuth and elevation
    private final int Lx;
    private final int Ly;
    private final int Lz;

    private final int Nz2;
    private final int NzLz;
    private final boolean texture;

    /**
     * Constructs an EmbossFilter.
     *
     * @param filterName the name of this filter
     * @param azimuth    direction of the light source, in radians
     * @param elevation  angle of the light source above the image plane, in radians
     * @param bumpHeight controls the apparent depth of the embossed relief;
     *                   larger values produce a more pronounced effect
     * @param texture    if true, blend the shading with the original colors;
     *                   if false, produce a pure grayscale relief
     */
    public EmbossFilter(String filterName,
                        float azimuth,
                        float elevation,
                        float bumpHeight,
                        boolean texture) {
        super(filterName);

        assert bumpHeight > 0;
        assert elevation >= 0 && elevation <= ImageMath.HALF_PI;

        // rotate by π so that azimuth 0 lights from the left, matching the UI arrow direction
        azimuth += ImageMath.PI;

        // larger bumpHeight => smaller normalZ => the gradient terms (normalX, normalY)
        // dominate the surface normal more strongly => a more pronounced 3D relief
        float depthScale = 3 * bumpHeight;

        this.Lx = (int) (Math.cos(azimuth) * Math.cos(elevation) * PIXEL_SCALE);
        this.Ly = (int) (Math.sin(azimuth) * Math.cos(elevation) * PIXEL_SCALE);
        this.Lz = (int) (Math.sin(elevation) * PIXEL_SCALE);

        int Nz = (int) (6 * 255 / depthScale);
        this.Nz2 = Nz * Nz;
        this.NzLz = Nz * Lz;

        this.texture = texture;
    }

    @Override
    protected int[] filterPixels(int width, int height, int[] inPixels) {
        pt = createProgressTracker(height);

        // a bump map is derived from the brightness values of the
        // input image, and represents the surface's height map
        int[] bumpPixels = ImageMath.calcLuminanceInt(inPixels);

        // surface normal vector: perpendicular to the surface at each point
        int Nx;
        int Ny;

        // the default shading intensity for areas where the surface
        // normal is undefined or the pixel is at the edge of the image
        int defaultShade = Lz;

        int[] outPixels = new int[width * height];
        int index = 0;
        int bumpIndex = 0;
        for (int y = 0; y < height; y++, bumpIndex += width) {
            int r1 = bumpIndex - width;  // previous row
            int r2 = bumpIndex;          // current row
            int r3 = bumpIndex + width;  // next row

            boolean yInBounds = y > 0 && y < height - 1;
            for (int x = 0; x < width; x++, r1++, r2++, r3++) {
                int shade; // the calculated intensity of reflected light at a specific pixel

                if (yInBounds && x > 0 && x < width - 1) {
                    Nx = bumpPixels[r1 - 1] + bumpPixels[r2 - 1] + bumpPixels[r3 - 1] - bumpPixels[r1 + 1] - bumpPixels[r2 + 1] - bumpPixels[r3 + 1];
                    Ny = bumpPixels[r3 - 1] + bumpPixels[r3] + bumpPixels[r3 + 1] - bumpPixels[r1 - 1] - bumpPixels[r1] - bumpPixels[r1 + 1];

                    // the dot product between the surface normal and the light vector
                    int NdotL;

                    if (Nx == 0 && Ny == 0) {
                        // baseline shading for areas without significant detail
                        shade = defaultShade;
                    } else if ((NdotL = Nx * Lx + Ny * Ly + NzLz) < 0) {
                        shade = 0; // shadow
                    } else {
                        // positive dot product => the angle between the two
                        // vectors is less than 90 degrees => the surface
                        // is facing the light and will appear illuminated
                        shade = (int) (NdotL / Math.sqrt(Nx * Nx + Ny * Ny + Nz2));
                    }
                } else {
                    // use the default shade for edge pixels
                    shade = defaultShade;
                }

                int rgb = inPixels[index];
                int a = rgb & 0xFF_00_00_00;
                if (texture) {
                    // blend the shading with the original image's colors
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    r = (r * shade) >> 8;
                    g = (g * shade) >> 8;
                    b = (b * shade) >> 8;
                    outPixels[index++] = a | (r << 16) | (g << 8) | b;
                } else {
                    // create grayscale output
                    outPixels[index++] = a | (shade << 16) | (shade << 8) | shade;
                }
            }
            pt.unitDone();
        }
        finishProgressTracker();

        return outPixels;
    }
}
