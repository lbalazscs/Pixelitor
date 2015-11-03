package pixelitor.filters;

import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * A test filter that always return an image with a single color
 */
public class OneColorFilter extends Filter {
    private final Color color;

    public OneColorFilter(Color color) {
        this.color = color;
    }

    @Override
    protected BufferedImage transform(BufferedImage src, BufferedImage dest) {
        dest = ImageUtils.createCompatibleDest(src);
        Graphics2D g = dest.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, src.getWidth(), src.getHeight());
        g.dispose();
        return dest;
    }

    @Override
    public void randomizeSettings() {
    }
}
