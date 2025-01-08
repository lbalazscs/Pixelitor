/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.utils;

import pixelitor.filters.gui.IntChoiceParam;

import static com.jhlabs.image.ImageMath.clamp;

/**
 * Static utility methods related to dithering.
 * See https://tannerhelland.com/2012/12/28/dithering-eleven-algorithms-source-code.html
 */
public class Dithering {
    public static final int DITHER_FLOYD_STEINBERG = 0;
    public static final int DITHER_STUCKI = 1;
    public static final int DITHER_BURKES = 2;
    public static final int DITHER_SIERRA = 3;

    private Dithering() {
    }

    public static void ditherFloydSteinberg(int[] inputPixels, int i, int width, int length, double error) {
        // 7 to the right
        if (i + 1 < length) {
            addError(inputPixels, i + 1, error * 7.0 / 16);
        }

        int belowIndex = i + width;
        if (belowIndex < length) {
            if (belowIndex - 1 >= 0) {
                addError(inputPixels, belowIndex - 1, error * 3.0 / 16);
            }
            addError(inputPixels, belowIndex, error * 5.0 / 16);
            if (belowIndex + 1 < length) {
                addError(inputPixels, belowIndex + 1, error / 16);
            }
        }
    }

    public static void ditherFloydSteinbergRGB(int[] inputPixels, int i, int width, int length, double errorR, double errorG, double errorB) {
        // 7 to the right
        if (i + 1 < length) {
            addErrorRGB(inputPixels, i + 1, errorR * 7.0 / 16, errorG * 7.0 / 16, errorB * 7.0 / 16);
        }

        int belowIndex = i + width;
        if (belowIndex < length) {
            if (belowIndex - 1 >= 0) {
                addErrorRGB(inputPixels, belowIndex - 1, errorR * 3.0 / 16, errorG * 3.0 / 16, errorB * 3.0 / 16);
            }
            addErrorRGB(inputPixels, belowIndex, errorR * 5.0 / 16, errorG * 5.0 / 16, errorB * 5.0 / 16);
            if (belowIndex + 1 < length) {
                addErrorRGB(inputPixels, belowIndex + 1, errorR / 16, errorG / 16, errorB / 16);
            }
        }
    }

    public static void ditherStucki(int[] inputPixels, int i, int width, int length, double error) {
        if (i + 1 < length) {
            addError(inputPixels, i + 1, error * 8.0 / 42);
        }
        if (i + 2 < length) {
            addError(inputPixels, i + 2, error * 4.0 / 42);
        }

        int belowIndex = i + width;
        if (belowIndex < length) {
            if (belowIndex - 2 >= 0) {
                addError(inputPixels, belowIndex - 2, error * 2.0 / 42);
            }
            if (belowIndex - 1 >= 0) {
                addError(inputPixels, belowIndex - 1, error * 4.0 / 42);
            }
            addError(inputPixels, belowIndex, error * 8.0 / 42);
            if (belowIndex + 1 < length) {
                addError(inputPixels, belowIndex + 1, error * 4.0 / 42);
            }
            if (belowIndex + 2 < length) {
                addError(inputPixels, belowIndex + 2, error * 2.0 / 42);
            }
        }

        int below2Index = i + 2 * width;
        if (below2Index < length) {
            if (below2Index - 2 >= 0) {
                addError(inputPixels, below2Index - 2, error / 42);
            }
            if (below2Index - 1 >= 0) {
                addError(inputPixels, below2Index - 1, error * 2.0 / 42);
            }
            addError(inputPixels, below2Index, error * 4.0 / 42);
            if (below2Index + 1 < length) {
                addError(inputPixels, below2Index + 1, error * 2.0 / 42);
            }
            if (below2Index + 2 < length) {
                addError(inputPixels, below2Index + 2, error / 42);
            }
        }
    }

    public static void ditherStuckiRGB(int[] inputPixels, int i, int width, int length, double errorR, double errorG, double errorB) {
        if (i + 1 < length) {
            addErrorRGB(inputPixels, i + 1, errorR * 8.0 / 42, errorG * 8.0 / 42, errorB * 8.0 / 42);
        }
        if (i + 2 < length) {
            addErrorRGB(inputPixels, i + 2, errorR * 4.0 / 42, errorG * 4.0 / 42, errorB * 4.0 / 42);
        }

        int belowIndex = i + width;
        if (belowIndex < length) {
            if (belowIndex - 2 >= 0) {
                addErrorRGB(inputPixels, belowIndex - 2, errorR * 2.0 / 42, errorG * 2.0 / 42, errorB * 2.0 / 42);
            }
            if (belowIndex - 1 >= 0) {
                addErrorRGB(inputPixels, belowIndex - 1, errorR * 4.0 / 42, errorG * 4.0 / 42, errorB * 4.0 / 42);
            }
            addErrorRGB(inputPixels, belowIndex, errorR * 8.0 / 42, errorG * 8.0 / 42, errorB * 8.0 / 42);
            if (belowIndex + 1 < length) {
                addErrorRGB(inputPixels, belowIndex + 1, errorR * 4.0 / 42, errorG * 4.0 / 42, errorB * 4.0 / 42);
            }
            if (belowIndex + 2 < length) {
                addErrorRGB(inputPixels, belowIndex + 2, errorR * 2.0 / 42, errorG * 2.0 / 42, errorB * 2.0 / 42);
            }
        }

        int below2Index = i + 2 * width;
        if (below2Index < length) {
            if (below2Index - 2 >= 0) {
                addErrorRGB(inputPixels, below2Index - 2, errorR / 42, errorG / 42, errorB / 42);
            }
            if (below2Index - 1 >= 0) {
                addErrorRGB(inputPixels, below2Index - 1, errorR * 2.0 / 42, errorG * 2.0 / 42, errorB * 2.0 / 42);
            }
            addErrorRGB(inputPixels, below2Index, errorR * 4.0 / 42, errorG * 4.0 / 42, errorB * 4.0 / 42);
            if (below2Index + 1 < length) {
                addErrorRGB(inputPixels, below2Index + 1, errorR * 2.0 / 42, errorG * 2.0 / 42, errorB * 2.0 / 42);
            }
            if (below2Index + 2 < length) {
                addErrorRGB(inputPixels, below2Index + 2, errorR / 42, errorG / 42, errorB / 42);
            }
        }
    }

    public static void ditherBurkes(int[] inputPixels, int i, int width, int length, double error) {
        if (i + 1 < length) {
            addError(inputPixels, i + 1, error * 8.0 / 32);
        }
        if (i + 2 < length) {
            addError(inputPixels, i + 2, error * 4.0 / 32);
        }

        int belowIndex = i + width;
        if (belowIndex < length) {
            if (belowIndex - 2 >= 0) {
                addError(inputPixels, belowIndex - 2, error * 2.0 / 32);
            }
            if (belowIndex - 1 >= 0) {
                addError(inputPixels, belowIndex - 1, error * 4.0 / 32);
            }
            addError(inputPixels, belowIndex, error * 8.0 / 32);
            if (belowIndex + 1 < length) {
                addError(inputPixels, belowIndex + 1, error * 4.0 / 32);
            }
            if (belowIndex + 2 < length) {
                addError(inputPixels, belowIndex + 2, error * 2.0 / 32);
            }
        }
    }

    public static void ditherBurkesRGB(int[] inputPixels, int i, int width, int length, double errorR, double errorG, double errorB) {
        if (i + 1 < length) {
            addErrorRGB(inputPixels, i + 1, errorR * 8.0 / 32, errorG * 8.0 / 32, errorB * 8.0 / 32);
        }
        if (i + 2 < length) {
            addErrorRGB(inputPixels, i + 2, errorR * 4.0 / 32, errorG * 4.0 / 32, errorB * 4.0 / 32);
        }

        int belowIndex = i + width;
        if (belowIndex < length) {
            if (belowIndex - 2 >= 0) {
                addErrorRGB(inputPixels, belowIndex - 2, errorR * 2.0 / 32, errorG * 2.0 / 32, errorB * 2.0 / 32);
            }
            if (belowIndex - 1 >= 0) {
                addErrorRGB(inputPixels, belowIndex - 1, errorR * 4.0 / 32, errorG * 4.0 / 32, errorB * 4.0 / 32);
            }
            addErrorRGB(inputPixels, belowIndex, errorR * 8.0 / 32, errorG * 8.0 / 32, errorB * 8.0 / 32);
            if (belowIndex + 1 < length) {
                addErrorRGB(inputPixels, belowIndex + 1, errorR * 4.0 / 32, errorG * 4.0 / 32, errorB * 4.0 / 32);
            }
            if (belowIndex + 2 < length) {
                addErrorRGB(inputPixels, belowIndex + 2, errorR * 2.0 / 32, errorG * 2.0 / 32, errorB * 2.0 / 32);
            }
        }
    }

    public static void ditherSierra(int[] inputPixels, int i, int width, int length, double error) {
        if (i + 1 < length) {
            addError(inputPixels, i + 1, error * 5.0 / 32);
        }
        if (i + 2 < length) {
            addError(inputPixels, i + 2, error * 3.0 / 32);
        }

        int belowIndex = i + width;
        if (belowIndex < length) {
            if (belowIndex - 2 >= 0) {
                addError(inputPixels, belowIndex - 2, error * 2.0 / 32);
            }
            if (belowIndex - 1 >= 0) {
                addError(inputPixels, belowIndex - 1, error * 4.0 / 32);
            }
            addError(inputPixels, belowIndex, error * 5.0 / 32);
            if (belowIndex + 1 < length) {
                addError(inputPixels, belowIndex + 1, error * 4.0 / 32);
            }
            if (belowIndex + 2 < length) {
                addError(inputPixels, belowIndex + 2, error * 2.0 / 32);
            }
        }

        int below2Index = i + 2 * width;
        if (below2Index < length) {
            if (below2Index - 1 >= 0) {
                addError(inputPixels, below2Index - 1, error * 2.0 / 32);
            }
            addError(inputPixels, below2Index, error * 3.0 / 32);
            if (below2Index + 1 < length) {
                addError(inputPixels, below2Index + 1, error * 2.0 / 32);
            }
        }
    }

    public static void ditherSierraRGB(int[] inputPixels, int i, int width, int length, double errorR, double errorG, double errorB) {
        if (i + 1 < length) {
            addErrorRGB(inputPixels, i + 1, errorR * 5.0 / 32, errorG * 5.0 / 32, errorB * 5.0 / 32);
        }
        if (i + 2 < length) {
            addErrorRGB(inputPixels, i + 2, errorR * 3.0 / 32, errorG * 3.0 / 32, errorB * 3.0 / 32);
        }

        int belowIndex = i + width;
        if (belowIndex < length) {
            if (belowIndex - 2 >= 0) {
                addErrorRGB(inputPixels, belowIndex - 2, errorR * 2.0 / 32, errorG * 2.0 / 32, errorB * 2.0 / 32);
            }
            if (belowIndex - 1 >= 0) {
                addErrorRGB(inputPixels, belowIndex - 1, errorR * 4.0 / 32, errorG * 4.0 / 32, errorB * 4.0 / 32);
            }
            addErrorRGB(inputPixels, belowIndex, errorR * 5.0 / 32, errorG * 5.0 / 32, errorB * 5.0 / 32);
            if (belowIndex + 1 < length) {
                addErrorRGB(inputPixels, belowIndex + 1, errorR * 4.0 / 32, errorG * 4.0 / 32, errorB * 4.0 / 32);
            }
            if (belowIndex + 2 < length) {
                addErrorRGB(inputPixels, belowIndex + 2, errorR * 2.0 / 32, errorG * 2.0 / 32, errorB * 2.0 / 32);
            }
        }

        int below2Index = i + 2 * width;
        if (below2Index < length) {
            if (below2Index - 1 >= 0) {
                addErrorRGB(inputPixels, below2Index - 1, errorR * 2.0 / 32, errorG * 2.0 / 32, errorB * 2.0 / 32);
            }
            addErrorRGB(inputPixels, below2Index, errorR * 3.0 / 32, errorG * 3.0 / 32, errorB * 3.0 / 32);
            if (below2Index + 1 < length) {
                addErrorRGB(inputPixels, below2Index + 1, errorR * 2.0 / 32, errorG * 2.0 / 32, errorB * 2.0 / 32);
            }
        }
    }

    /**
     * Adds the given error value to the given pixel.
     */
    private static void addError(int[] pixels, int index, double value) {
        int rgb = pixels[index];
        int intVal = (int) value;
        int r = clamp(((rgb >>> 16) & 0xFF) + intVal, 0, 255);
        int g = clamp(((rgb >>> 8) & 0xFF) + intVal, 0, 255);
        int b = clamp((rgb & 0xFF) + intVal, 0, 255);
        pixels[index] = r << 16 | g << 8 | b;
    }

    /**
     * Adds the given R, G, B error values to the given pixel.
     */
    private static void addErrorRGB(int[] pixels, int index,
                                    double valueR, double valueG, double valueB) {
        int rgb = pixels[index];
        int r = clamp(((rgb >>> 16) & 0xFF) + (int) valueR, 0, 255);
        int g = clamp(((rgb >>> 8) & 0xFF) + (int) valueG, 0, 255);
        int b = clamp((rgb & 0xFF) + (int) valueB, 0, 255);
        pixels[index] = r << 16 | g << 8 | b;
    }

    public static IntChoiceParam createDitheringChoices() {
        return new IntChoiceParam("Dithering Method", new IntChoiceParam.Item[]{
            new IntChoiceParam.Item("Floyd–Steinberg", DITHER_FLOYD_STEINBERG),
            new IntChoiceParam.Item("Stucki", DITHER_STUCKI),
            new IntChoiceParam.Item("Burkes", DITHER_BURKES),
            new IntChoiceParam.Item("Sierra", DITHER_SIERRA),
        });
    }

    public static void ditherRGB(int ditheringMethod, int[] inputPixels, int i, int width, int length, double errorR, double errorG, double errorB) {
        // distribute the error to neighboring pixels
        switch (ditheringMethod) {
            case DITHER_FLOYD_STEINBERG:
                ditherFloydSteinbergRGB(inputPixels, i, width, length, errorR, errorG, errorB);
                break;
            case DITHER_STUCKI:
                ditherStuckiRGB(inputPixels, i, width, length, errorR, errorG, errorB);
                break;
            case DITHER_BURKES:
                ditherBurkesRGB(inputPixels, i, width, length, errorR, errorG, errorB);
                break;
            case DITHER_SIERRA:
                ditherSierraRGB(inputPixels, i, width, length, errorR, errorG, errorB);
                break;
        }
    }
}
