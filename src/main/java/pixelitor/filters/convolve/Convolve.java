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

package pixelitor.filters.convolve;

import com.jhlabs.image.ConvolveFilter;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.filters.util.FilterAction;
import pixelitor.layers.Filterable;
import pixelitor.utils.Messages;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImagingOpException;
import java.awt.image.Kernel;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A customizable convolution filter.
 */
public class Convolve extends FilterWithGUI {
    private final String filterName;

    private float[] kernelMatrix; // the matrix as a flat array
    private final int kernelSize;

    private Convolve(int kernelSize, String filterName) {
        if (kernelSize <= 0 || kernelSize % 2 == 0) {
            throw new IllegalArgumentException("kernel size = " + kernelSize);
        }
        this.kernelSize = kernelSize;
        this.filterName = filterName;
    }

    public void setKernelMatrix(float[] kernelMatrix) {
        if (kernelMatrix.length != kernelSize * kernelSize) {
            throw new IllegalArgumentException("array length = " + kernelMatrix.length + ", kernel size = " + kernelSize);
        }
        this.kernelMatrix = kernelMatrix;
    }

    public float[] getKernelMatrix() {
        return kernelMatrix;
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        // must be set before calling this method
        assert kernelMatrix != null;

        Kernel kernel = new Kernel(kernelSize, kernelSize, kernelMatrix);
        BufferedImageOp convolveOp = createConvolveOp(kernel);

        try {
            convolveOp.filter(src, dest);
        } catch (ImagingOpException e) {
            Messages.showException(e);
        }

        return dest;
    }

    @Override
    public FilterGUI createGUI(Filterable layer, boolean resetSettings) {
        return new ConvolveGUI(this, layer, resetSettings);
    }

    @Override
    public void randomize() {
        kernelMatrix = createRandomKernel(kernelSize);
    }

    private BufferedImageOp createConvolveOp(Kernel kernel) {
        var filter = new ConvolveFilter(filterName, kernel);
        filter.setPremultiplyAlpha(false);
        return filter;
    }

    /**
     * Returns a randomized array that is on average close to being
     * normalized: each value is uniform in [-1/n, 3/n), which
     * averages to 1/n, so the sum of all n values averages to 1.
     */
    public static float[] createRandomKernel(int size) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        int numValues = size * size;
        float[] kernelValues = new float[numValues];
        for (int i = 0; i < numValues; i++) {
            kernelValues[i] = rand.nextFloat() * (4.0f / numValues) - (1.0f / numValues);
        }
        return kernelValues;
    }

    public int getKernelSize() {
        return kernelSize;
    }

    public static FilterAction createFilterAction(int size) {
        String name = "Custom " + size + 'x' + size + " Convolution";
        return new FilterAction(name, () -> new Convolve(size, name));
    }

    @Override
    public boolean supportsUserPresets() {
        return false;
    }

    @Override
    public boolean canBeSmart() {
        return false;
    }
}
