package pixelitor.filters;

import java.awt.image.BufferedImage;

/**
 * A test filter that always returns the source
 */
public class NoOpFilter extends Filter {
    public NoOpFilter() {
    }

    @Override
    protected BufferedImage transform(BufferedImage src, BufferedImage dest) {
        return src;
    }

    @Override
    public void randomizeSettings() {
    }
}
