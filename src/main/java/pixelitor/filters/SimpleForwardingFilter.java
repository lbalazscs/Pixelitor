package pixelitor.filters;

import com.jhlabs.image.AbstractBufferedImageOp;

import java.awt.image.BufferedImage;

/**
 * A filter without parameters that delegates the
 * filtering to an AbstractBufferedImageOp
 */
public class SimpleForwardingFilter extends Filter {
    AbstractBufferedImageOp delegate;

    public SimpleForwardingFilter(AbstractBufferedImageOp delegate) {
        this.delegate = delegate;
    }

    @Override
    protected BufferedImage transform(BufferedImage src, BufferedImage dest) {
        return delegate.filter(src, dest);
    }

    @Override
    public void randomizeSettings() {

    }
}
