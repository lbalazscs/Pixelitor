/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

/**
 * A customizable convolution
 */
public class Convolve extends FilterWithGUI {
    private final String filterName;

    private float[] kernelMatrix;
    private final int matrixOrder;

    private Convolve(int matrixOrder, String filterName) {
        this.matrixOrder = matrixOrder;
        this.filterName = filterName;
    }

    public void setKernelMatrix(float[] kernelMatrix) {
        if (kernelMatrix.length != matrixOrder * matrixOrder) {
            throw new IllegalArgumentException("kernelMatrix.length = " + kernelMatrix.length + ", size = " + matrixOrder);
        }
        this.kernelMatrix = kernelMatrix;
    }

    public float[] getKernelMatrix() {
        return kernelMatrix;
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        Kernel kernel = new Kernel(matrixOrder, matrixOrder, kernelMatrix);
        BufferedImageOp convolveOp = createConvolveOp(kernel);
        try {
            convolveOp.filter(src, dest);
        } catch (ImagingOpException e) {
            Messages.showException(e);
        }

        return dest;
    }

    @Override
    public FilterGUI createGUI(Filterable layer, boolean reset) {
        return new CustomConvolveGUI(this, layer, reset);
    }

    @Override
    public void randomize() {
        kernelMatrix = createRandomKernelMatrix(matrixOrder);
    }

    private BufferedImageOp createConvolveOp(Kernel kernel) {
        var filter = new ConvolveFilter(kernel, filterName);
        filter.setEdgeAction(ConvolveFilter.CLAMP_EDGES);
        filter.setPremultiplyAlpha(false);
        return filter;
    }

    public int getMatrixOrder() {
        return matrixOrder;
    }

    public static FilterAction createFilterAction(int size) {
        String name = getFilterName(size, size);
        return new FilterAction(name, () -> new Convolve(size, name));
    }

    private static String getFilterName(int width, int height) {
        return "Custom " + width + 'x' + height + " Convolution";
    }

    @Override
    public boolean canHaveUserPresets() {
        return false;
    }

    @Override
    public boolean canBeSmart() {
        return false;
    }
}
