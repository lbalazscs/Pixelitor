package pixelitor.filters;

import com.jhlabs.image.InvertAlphaFilter;

import java.awt.image.BufferedImage;

/**
 * Invert Transparency
 */
public class InvertTransparency extends Filter {
    private final InvertAlphaFilter filter;

    public InvertTransparency() {
        filter = new InvertAlphaFilter();
    }

    @Override
    protected BufferedImage transform(BufferedImage src, BufferedImage dest) {
        return filter.filter(src, dest);
    }

    @Override
    public void randomizeSettings() {

    }
}