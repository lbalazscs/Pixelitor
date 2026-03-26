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
 * A class to emboss an image.
 */
public class EmbossFilter extends WholeImageFilter {
    private static final float PIXEL_SCALE = 255.9f;

    private final float azimuth;
    private final float elevation;
    private final boolean texture;
    private final float width45;

    public EmbossFilter(String filterName,
                        float azimuth,
                        float elevation,
                        float bumpHeight,
                        boolean texture) {
        super(filterName);

        this.azimuth = azimuth + ImageMath.PI;
        this.elevation = elevation;
        this.texture = texture;
        this.width45 = 3 * bumpHeight;
    }

    @Override
    protected int[] filterPixels(int width, int height, int[] inPixels) {
        pt = createProgressTracker(height);

        // a bump map is derived from the brightness values of the
        // input image, and represents the the surface's height map
        int[] bumpPixels = new int[inPixels.length];
        for (int i = 0; i < inPixels.length; i++) {
            bumpPixels[i] = ImageMath.calcLuminanceInt(inPixels[i]);
        }

        // the light vector components based on azimuth and elevation
        int Lx = (int) (Math.cos(azimuth) * Math.cos(elevation) * PIXEL_SCALE);
        int Ly = (int) (Math.sin(azimuth) * Math.cos(elevation) * PIXEL_SCALE);
        int Lz = (int) (Math.sin(elevation) * PIXEL_SCALE);

        // surface normal vector: perpendicular to the surface at each point
        int Nx;
        int Ny;
        int Nz = (int) (6 * 255 / width45); // can be precomputed: depends only on the depth
        int Nz2 = Nz * Nz;

        int NzLz = Nz * Lz; // can be precomputed

        // the default shading intensity for areas where the surface
        // normal is undefined or the pixel is at the edge of the image
        int background = Lz;

        int[] outPixels = new int[width * height];
        int index = 0;
        int bumpIndex = 0;
        for (int y = 0; y < height; y++, bumpIndex += width) {
            int s1 = bumpIndex - width;  // previous row
            int s2 = bumpIndex;          // current row
            int s3 = bumpIndex + width;  // next row

            boolean yInBounds = y > 0 && y < height - 1;
            for (int x = 0; x < width; x++, s1++, s2++, s3++) {
                int shade; // the calculated intensity of reflected light at a specific pixel

                if (yInBounds && x > 0 && x < width - 1) {
                    Nx = bumpPixels[s1 - 1] + bumpPixels[s2 - 1] + bumpPixels[s3 - 1] - bumpPixels[s1 + 1] - bumpPixels[s2 + 1] - bumpPixels[s3 + 1];
                    Ny = bumpPixels[s3 - 1] + bumpPixels[s3] + bumpPixels[s3 + 1] - bumpPixels[s1 - 1] - bumpPixels[s1] - bumpPixels[s1 + 1];

                    // the dot product between the surface normal and the light vector
                    int NdotL;

                    if (Nx == 0 && Ny == 0) {
                        // baseline shading for areas without significant detail
                        shade = background;
                    } else if ((NdotL = Nx * Lx + Ny * Ly + NzLz) < 0) {
                        shade = 0; // shadow
                    } else {
                        // positive dot product => the angle between the two
                        // vectors is is less than 90 degrees => the surface
                        // is facing the light and will appear illuminated
                        shade = (int) (NdotL / Math.sqrt(Nx * Nx + Ny * Ny + Nz2));
                    }
                } else {
                    // use the background shade for edge pixels
                    shade = background;
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
