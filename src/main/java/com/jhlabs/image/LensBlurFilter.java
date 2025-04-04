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

import com.jhlabs.math.FFT;
import net.jafama.FastMath;

import java.awt.image.BufferedImage;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * A filter which use FFTs to simulate lens blur on an image.
 */
public class LensBlurFilter extends AbstractBufferedImageOp {
    private float radius = 10;
    private float bloom = 2;
    private float bloomThreshold = 192;
    private static final float angle = 0;
    private int sides = 5;

    public LensBlurFilter(String filterName) {
        super(filterName);
    }

    /**
     * Set the radius of the kernel, and hence the amount of blur.
     *
     * @param radius the radius of the blur in pixels.
     */
    public void setRadius(float radius) {
        this.radius = radius;
    }

    /**
     * Set the number of sides of the aperture.
     *
     * @param sides the number of sides
     */
    public void setSides(int sides) {
        this.sides = sides;
    }

    /**
     * Set the bloom factor.
     *
     * @param bloom the bloom factor
     */
    public void setBloom(float bloom) {
        this.bloom = bloom;
    }

    /**
     * Set the bloom threshold.
     *
     * @param bloomThreshold the bloom threshold
     */
    public void setBloomThreshold(float bloomThreshold) {
        this.bloomThreshold = bloomThreshold;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();
        int rows = 1, cols = 1;
        int log2rows = 0, log2cols = 0;
        int iradius = (int) Math.ceil(radius);

        int tileWidth = iradius < 32 ? Math.min(128, width + 2 * iradius) : Math.min(256, width + 2 * iradius);
        int tileHeight = iradius < 32 ? Math.min(128, height + 2 * iradius) : Math.min(256, height + 2 * iradius);

        if (dst == null) {
            dst = new BufferedImage(width, height, TYPE_INT_ARGB);
        }

        while (rows < tileHeight) {
            rows *= 2;
            log2rows++;
        }
        while (cols < tileWidth) {
            cols *= 2;
            log2cols++;
        }
        int w = cols;
        int h = rows;

        tileWidth = w;
        tileHeight = h;//FIXME-tileWidth, w, and cols are always all the same

        FFT fft = new FFT(Math.max(log2rows, log2cols));

        int[] rgb = new int[w * h];
        float[][] mask = new float[2][w * h];
        float[][] gb = new float[2][w * h];
        float[][] ar = new float[2][w * h];

        // Create the kernel
        double polyAngle = Math.PI / sides;
        double polyScale = 1.0f / FastMath.cos(polyAngle);
        double r2 = radius * radius;
        double rangle = Math.toRadians(angle);
        float total = 0;
        int i = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double dx = x - w / 2.0f;
                double dy = y - h / 2.0f;
                double r = dx * dx + dy * dy;
                double f = r < r2 ? 1 : 0;
                if (f != 0) {
                    r = Math.sqrt(r);
                    if (sides != 0) {
                        double a = FastMath.atan2(dy, dx) + rangle;
                        a = ImageMath.mod(a, polyAngle * 2) - polyAngle;
                        f = FastMath.cos(a) * polyScale;
                    } else {
                        f = 1;
                    }
                    f = f * r < radius ? 1 : 0;
                }
                total += (float) f;

                mask[0][i] = (float) f;
                mask[1][i] = 0;
                i++;
            }
        }

        // Normalize the kernel
        i = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                mask[0][i] /= total;
                i++;
            }
        }

        fft.transform2D(mask[0], mask[1], w, h, true);

        int workUnits = 0;
        // count the work units the same was as the code does...
        for (int tileY = -iradius; tileY < height; tileY += tileHeight - 2 * iradius) {
            workUnits++;
        }
        pt = createProgressTracker(workUnits);

        for (int tileY = -iradius; tileY < height; tileY += tileHeight - 2 * iradius) {
            for (int tileX = -iradius; tileX < width; tileX += tileWidth - 2 * iradius) {
//                System.out.println("Tile: "+tileX+" "+tileY+" "+tileWidth+" "+tileHeight);

                // Clip the tile to the image bounds
                int tx = tileX, ty = tileY, tw = tileWidth, th = tileHeight;
                int fx = 0, fy = 0;
                if (tx < 0) {
                    tw += tx;
                    fx -= tx;
                    tx = 0;
                }
                if (ty < 0) {
                    th += ty;
                    fy -= ty;
                    ty = 0;
                }
                if (tx + tw > width) {
                    tw = width - tx;
                }
                if (ty + th > height) {
                    th = height - ty;
                }
                src.getRGB(tx, ty, tw, th, rgb, fy * w + fx, w);
                // getRGB(src, tx, ty, tw, th, rgb);

                // Create a float array from the pixels. Any pixels off the edge of the source image get duplicated from the edge.
                i = 0;
                for (int y = 0; y < h; y++) {
                    int imageY = y + tileY;
                    int j;
                    if (imageY < 0) {
                        j = fy;
                    } else if (imageY > height) {
                        j = fy + th - 1;
                    } else {
                        j = y;
                    }
                    j *= w;
                    for (int x = 0; x < w; x++) {
                        int imageX = x + tileX;
                        int k;
                        if (imageX < 0) {
                            k = fx;
                        } else if (imageX > width) {
                            k = fx + tw - 1;
                        } else {
                            k = x;
                        }
                        k += j;

                        ar[0][i] = ((rgb[k] >> 24) & 0xff);
                        float r = ((rgb[k] >> 16) & 0xff);
                        float g = ((rgb[k] >> 8) & 0xff);
                        float b = (rgb[k] & 0xff);

                        // Bloom...
                        if (r > bloomThreshold) {
                            r *= bloom;
                        }
                        if (g > bloomThreshold) {
                            g *= bloom;
                        }
                        if (b > bloomThreshold) {
                            b *= bloom;
                        }

                        ar[1][i] = r;
                        gb[0][i] = g;
                        gb[1][i] = b;

                        i++;
                    }
                }

                // Transform into frequency space
                fft.transform2D(ar[0], ar[1], cols, rows, true);
                fft.transform2D(gb[0], gb[1], cols, rows, true);

                // Multiply the transformed pixels by the transformed kernel
                i = 0;
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        float re = ar[0][i];
                        float im = ar[1][i];
                        float rem = mask[0][i];
                        float imm = mask[1][i];
                        ar[0][i] = re * rem - im * imm;
                        ar[1][i] = re * imm + im * rem;

                        re = gb[0][i];
                        im = gb[1][i];
                        gb[0][i] = re * rem - im * imm;
                        gb[1][i] = re * imm + im * rem;
                        i++;
                    }
                }

                // Transform back
                fft.transform2D(ar[0], ar[1], cols, rows, false);
                fft.transform2D(gb[0], gb[1], cols, rows, false);

                // Convert back to RGB pixels, with quadrant remapping
                int row_flip = w >> 1;
                int col_flip = h >> 1;
                int index = 0;

                int workaroundMax = w * h - 1;

                //FIXME-don't bother converting pixels off image edges
                for (int y = 0; y < w; y++) {
                    int ym = y ^ row_flip;
                    int yi = ym * cols;
                    for (int x = 0; x < w; x++) {
                        int xm = yi + (x ^ col_flip);

                        // Laszlo: not sure what is happening here, but for certain small images
                        // with unusual image proportions (for example for any 100*20 input image)
                        // we get an ArrayIndexOutOfBoundsException
                        // This break does not result in a good-looking image, but at least
                        // it avoids the exceptions during the automatic tests
                        if (xm > workaroundMax) {
                            break;
                        }

                        int a = (int) ar[0][xm];
                        int r = (int) ar[1][xm];
                        int g = (int) gb[0][xm];
                        int b = (int) gb[1][xm];

                        // Clamp high pixels due to blooming
                        if (r > 255) {
                            r = 255;
                        }
                        if (g > 255) {
                            g = 255;
                        }
                        if (b > 255) {
                            b = 255;
                        }
                        int argb = (a << 24) | (r << 16) | (g << 8) | b;
                        rgb[index++] = argb;
                    }
                }

                // Clip to the output image
                tx = tileX + iradius;
                ty = tileY + iradius;
                tw = tileWidth - 2 * iradius;
                th = tileHeight - 2 * iradius;
                if (tx + tw > width) {
                    tw = width - tx;
                }
                if (ty + th > height) {
                    th = height - ty;
                }

                dst.setRGB(tx, ty, tw, th, rgb, iradius * w + iradius, w);
                // setRGB(dst, tx, ty, tw, th, rgb);
            }
            pt.unitDone();
        }
        finishProgressTracker();

        return dst;
    }

    @Override
    public String toString() {
        return "Blur/Lens Blur...";
    }
}
