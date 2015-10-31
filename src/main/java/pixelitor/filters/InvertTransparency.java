package pixelitor.filters;

import com.jhlabs.image.InvertAlphaFilter;

import java.awt.image.BufferedImage;

/**
 * Invert Transparency
 */
public class InvertTransparency extends Filter {
    private InvertAlphaFilter filter;

    public InvertTransparency() {
        super("Invert Transparency");
    }

    @Override
    protected BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new InvertAlphaFilter();
        }
        return filter.filter(src, dest);
    }

    @Override
    public void randomizeSettings() {

    }
}