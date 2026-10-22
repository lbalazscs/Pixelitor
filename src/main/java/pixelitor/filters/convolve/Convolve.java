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
import pixelitor.filters.Filter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.util.FilterAction;
import pixelitor.utils.Messages;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImagingOpException;
import java.awt.image.Kernel;
import java.io.Serial;
import java.util.function.Supplier;

/**
 * A customizable convolution filter. The kernel size is determined
 * by the concrete subclasses ({@link Convolve3x3}, {@link Convolve5x5}),
 * which must have public no-arg constructors in order to be smart filters.
 */
public abstract class Convolve extends ParametrizedFilter {
    @Serial
    private static final long serialVersionUID = 1L;

    private final ConvolveParam kernelParam;

    protected Convolve(int kernelSize) {
        super(false);

        kernelParam = new ConvolveParam("Kernel", kernelSize);
        initParams(kernelParam);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int size = kernelParam.getKernelSize();
        Kernel kernel = new Kernel(size, size, kernelParam.getKernel());
        BufferedImageOp convolveOp = createConvolveOp(kernel);

        try {
            convolveOp.filter(src, dest);
        } catch (ImagingOpException e) {
            Messages.showException(e);
        }

        return dest;
    }

    private BufferedImageOp createConvolveOp(Kernel kernel) {
        var filter = new ConvolveFilter(getName(), kernel);
        filter.setPremultiplyAlpha(false);
        return filter;
    }

    public static FilterAction createFilterAction(int size) {
        String name = "Custom " + size + 'x' + size + " Convolution";
        Supplier<Filter> factory = switch (size) {
            case 3 -> Convolve3x3::new;
            case 5 -> Convolve5x5::new;
            default -> throw new IllegalArgumentException("size = " + size);
        };
        return new FilterAction(name, factory);
    }
}
