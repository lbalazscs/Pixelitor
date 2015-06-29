package pixelitor.filters;

import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.utils.ImageUtils;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;

/**
 * For some filters it makes sense to apply them to a
 * downscaled image, and then scale the image back.
 */
public class ResizingHelper {
    public static final int BILINEAR_FAST = 0;
    public static final int BILINEAR12 = 1;
    public static final int BILINEAR11 = 2;

    private BufferedImage src;
    private final int srcWidth;
    private final int srcHeight;
    private double resizeFactor = 1.0;

    public ResizingHelper(BufferedImage src) {
        this.src = src;
        srcWidth = src.getWidth();
        srcHeight = src.getHeight();
    }

    public boolean shouldResize() {
        boolean resize = false;

        int numPixels = srcWidth * srcHeight;
        int resizeThreshold = 600_000;

        if (numPixels > resizeThreshold) {
            int ratio = numPixels / resizeThreshold;
            resize = true;
            resizeFactor = 1 + Math.sqrt(ratio);
        }
        return resize;
    }

    public double getResizeFactor() {
        return resizeFactor;
    }

    public BufferedImage invoke(IntChoiceParam detailQuality, BufferedImageOp filter) {
        assert resizeFactor > 1.0;
        BufferedImage dest;

        BufferedImage smallSrc = getDownscaledSource();

        BufferedImage smallDest = filter.filter(smallSrc, null);

        int quality = detailQuality.getValue();
        if (quality == BILINEAR_FAST) {
            dest = ImageUtils.createCompatibleDest(src);
            Graphics2D g2 = dest.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.scale(resizeFactor, resizeFactor);
            g2.drawImage(smallDest, 0, 0, null);
            g2.dispose();
        } else {
            Object hint;
            double step = 0;
            if (quality == BILINEAR11) {
                hint = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
                step = 1.1;
            } else if (quality == BILINEAR12) {
                hint = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
                step = 1.2;
            } else {
                throw new IllegalStateException("quality = " + quality);
            }
            dest = ImageUtils.enlargeSmooth(smallDest, srcWidth, srcHeight, hint, step);
        }
        return dest;
    }

    public BufferedImage getDownscaledSource() {
        // For the downscaling there is no quality improvement if it is done
        // in multiple steps, so this is always done the fast way.
        int smallWidth = (int) (srcWidth / resizeFactor);
        int smallHeight = (int) (srcHeight / resizeFactor);
        BufferedImage smallSrc = new BufferedImage(smallWidth, smallHeight, src.getType());
        Graphics2D g = smallSrc.createGraphics();
        g.scale(1.0 / resizeFactor, 1.0 / resizeFactor);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return smallSrc;
    }

    public static IntChoiceParam createQualityParam() {
        return new IntChoiceParam("Detail Quality", new IntChoiceParam.Value[]{
                new IntChoiceParam.Value("Faster", ResizingHelper.BILINEAR_FAST),
                new IntChoiceParam.Value("Better", ResizingHelper.BILINEAR11),
        });
    }
}
