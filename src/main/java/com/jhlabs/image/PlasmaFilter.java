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

import java.awt.Rectangle;
import java.util.Date;
import java.util.Random;

public class PlasmaFilter extends WholeImageFilter {
    public static final int DO_PLASMA_CALL_PER_UNIT = 200_000;
    private int doPlasmaCalls = 0;

    public float turbulence = 1.0f;
    private float scaling = 0.0f;
	private Colormap colormap = new LinearColormap();
	private Random random;
	private long seed = 567;
	private boolean useColormap = false;

    private boolean lessColors = false;

	public PlasmaFilter(String filterName) {
		super(filterName);
        random = new Random();
	}

    public void setLessColors(boolean lessColors) {
        this.lessColors = lessColors;
    }

    /**
     * Specifies the turbulence of the texture.
     * @param turbulence the turbulence of the texture.
     * @min-value 0
     * @max-value 10
     * @see #getTurbulence
     */
	public void setTurbulence(float turbulence) {
		this.turbulence = turbulence;
	}

	/**
     * Returns the turbulence of the effect.
     * @return the turbulence of the effect.
     * @see #setTurbulence
     */
	public float getTurbulence() {
		return turbulence;
	}

	public void setScaling(float scaling) {
		this.scaling = scaling;
	}

	public float getScaling() {
		return scaling;
	}

    /**
     * Set the colormap to be used for the filter.
     * @param colormap the colormap
     * @see #getColormap
     */
	public void setColormap(Colormap colormap) {
		this.colormap = colormap;
	}

    /**
     * Get the colormap to be used for the filter.
     * @return the colormap
     * @see #setColormap
     */
	public Colormap getColormap() {
		return colormap;
	}

	public void setUseColormap(boolean useColormap) {
		this.useColormap = useColormap;
	}

	public boolean getUseColormap() {
		return useColormap;
	}

	public void setSeed(int seed) {
		this.seed = seed;
	}

	public int getSeed() {
		return (int)seed;
	}

	public void randomize() {
		seed = new Date().getTime();
	}

	private int randomRGB() {
			int r = random.nextInt(256);
			int g = random.nextInt(256);
			int b = random.nextInt(256);
			return 0xff000000 | (r << 16) | (g << 8) | b;
	}

	private int changeColor(int rgb, float amount) {
        if(amount < 0.1f) {
            return rgb;
        }

		int r = (rgb >> 16) & 0xff;
		int g = (rgb >> 8) & 0xff;
		int b = rgb & 0xff;


        if(lessColors) {
            int d = (int) (amount * (random.nextFloat() - 0.5));

            int r1 = r + d;
            int g1 = g + d;
            int b1 = b + d;

            // this method is very frequently called, the filter runs 7% faster if PixelUtils.clamp is inlined
            r = r1 > 255 ? 255 : (r1 < 0 ? 0 : r1);
            g = g1 > 255 ? 255 : (g1 < 0 ? 0 : g1);
            b = b1 > 255 ? 255 : (b1 < 0 ? 0 : b1);
        } else {
            // strangely this branch seems to run faster if PixelUtils.clamp is NOT inlined....
            int r1 = r + (int) (amount * (random.nextFloat() - 0.5));
            int g1 = g + (int) (amount * (random.nextFloat() - 0.5));
            int b1 = b + (int) (amount * (random.nextFloat() - 0.5));

            r = PixelUtils.clamp(r1);
            g = PixelUtils.clamp(g1);
            b = PixelUtils.clamp(b1);

//            r = r1 < 0 ? 0 : (r1 > 255 ? 255 : r1);
//            g = g1 < 0 ? 0 : (g1 > 255 ? 255 : g1);
//            b = b1 < 0 ? 0 : (b1 > 255 ? 255 : b1);
        }



		return 0xff000000 | (r << 16) | (g << 8) | b;
	}

	private static int average(int rgb1, int rgb2) {
        int a1 = (rgb1 >> 24) & 0xff;
        int r1 = (rgb1 >> 16) & 0xff;
        int g1 = (rgb1 >> 8) & 0xff;
        int b1 = rgb1 & 0xff;

//        int a2 = (rgb2 >> 24) & 0xff;
        int r2 = (rgb2 >> 16) & 0xff;
        int g2 = (rgb2 >> 8) & 0xff;
        int b2 = rgb2 & 0xff;

        r1 = (r1 + r2) / 2;
        g1 = (g1 + g2) / 2;
        b1 = (b1 + b2) / 2;

		return (a1 << 24) | (r1 << 16) | (g1 << 8) | b1;
	}

    private boolean doPlasma(int x1, int y1, int x2, int y2, int[] pixels, int stride, int depth, int scale) {
        doPlasmaCalls++;
        if (doPlasmaCalls == DO_PLASMA_CALL_PER_UNIT) {
            doPlasmaCalls = 0;
            pt.unitDone();
        }

		int mx, my;

		if (depth == 0) {
			int ml, mr, mt, mb, mm, t;

            int tl = pixels[y1 * stride + x1];
            int bl = pixels[y2 * stride + x1];
            int tr = pixels[y1 * stride + x2];
            int br = pixels[y2 * stride + x2];

			float amount = ((256.0f / (2.0f * scale)) * turbulence);

			mx = (x1 + x2) / 2;
			my = (y1 + y2) / 2;

			if (mx == x1 && mx == x2 && my == y1 && my == y2) {
                return true;
            }

			if (mx != x1 || mx != x2) {
                // left
				ml = average(tl, bl);
				ml = changeColor(ml, amount);
                pixels[my * stride + x1] = ml;

                if (x1 != x2){
                    // right
					mr = average(tr, br);
					mr = changeColor(mr, amount);
                    pixels[my * stride + x2] = mr;
                }
			}

			if (my != y1 || my != y2){
				if (x1 != mx || my != y2){
                    // bottom
					mb = average(bl, br);
					mb = changeColor(mb, amount);
                    pixels[y2 * stride + mx] = mb;
                }

				if (y1 != y2){
                    // top
					mt = average(tl, tr);
					mt = changeColor(mt, amount);
                    pixels[y1 * stride + mx] = mt;
                }
			}

			if (y1 != y2 || x1 != x2) {
                // middle pixel
				mm = average(tl, br);
				t = average(bl, tr);
				mm = average(mm, t);
				mm = changeColor(mm, amount);
                pixels[my * stride + mx] = mm;
            }

			if (x2-x1 < 3 && y2-y1 < 3) {
                return false;
            }
			return true;
		}

		mx = (x1 + x2) / 2;
		my = (y1 + y2) / 2;

        // top left
		doPlasma(x1, y1, mx, my, pixels, stride, depth-1, scale+1);
        // bottom left
		doPlasma(x1, my, mx ,y2, pixels, stride, depth-1, scale+1);
        // top right
		doPlasma(mx, y1, x2 , my, pixels, stride, depth-1, scale+1);
        // bottom right
		return doPlasma(mx, my, x2, y2, pixels, stride, depth-1, scale+1);
	}

	@Override
    protected int[] filterPixels( int width, int height, int[] inPixels, Rectangle transformedSpace ) {
		int[] outPixels = new int[width * height];

		random.setSeed(seed);

		int w1 = width-1;
		int h1 = height-1;
        /*
         * Puts in the seed pixels - one in each
         * corner, and one in the center of each edge, plus one in the
         * center of the image.
         */
        outPixels[0 * width + 0] = randomRGB();
        outPixels[0 * width + w1] = randomRGB();
        outPixels[h1 * width + 0] = randomRGB();
        outPixels[h1 * width + w1] = randomRGB();
        outPixels[h1/2 * width + w1/2] = randomRGB();
        outPixels[h1/2 * width + 0] = randomRGB();
        outPixels[h1/2 * width + w1] = randomRGB();
        outPixels[0 * width + w1/2] = randomRGB();
        outPixels[h1 * width + w1/2] = randomRGB();

        int estimatedDoPlasmaCalls = estimateDoPlasmaCalls(width, height);
        int workUnits = estimatedDoPlasmaCalls / DO_PLASMA_CALL_PER_UNIT;
        pt = createProgressTracker(workUnits);

        doPlasmaCalls = 0;

        /*
         * Now we recurse through the image, going further each time.
         */
        int depth = 1;
        while (doPlasma(0, 0, width - 1, height - 1, outPixels, width, depth, 0)) {
            depth++;
        }

        if (useColormap && colormap != null) {
            int index = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    outPixels[index] = colormap.getColor((outPixels[index] & 0xff) / 255.0f);
                    index++;
                }
            }
        }
        finishProgressTracker();
        return outPixels;
    }

    private static int estimateDoPlasmaCalls(int width, int height) {
        // some logarithmic formula could be found
        // instead of these empirical values
        int maxSize = Math.max(width, height);
        int estimatedDoPlasmaCalls = 7278;
        if (maxSize <= 129) {
            // doesn't care about smaller thresholds, they
            // don't have a progress bar anyway
            estimatedDoPlasmaCalls = 7278;
        } else if (maxSize <= 257) {
            estimatedDoPlasmaCalls = 29123;
        } else if (maxSize <= 513) {
            estimatedDoPlasmaCalls = 116504;
        } else if (maxSize <= 1025) {
            estimatedDoPlasmaCalls = 466029;
        } else if (maxSize <= 2049) {
            estimatedDoPlasmaCalls = 1864130;
        } else if (maxSize <= 4097) {
            estimatedDoPlasmaCalls = 7456535;
        } else if (maxSize <= 8193) {
            estimatedDoPlasmaCalls = 29826156;
        } else {
            // doesn't care about bigger thresholds
            // we don't expect images with a
            // max size of more than 16 000 pixels
            estimatedDoPlasmaCalls = 119304641;
        }
        return estimatedDoPlasmaCalls;
    }

    public String toString() {
        return "Texture/Plasma...";
	}
}
