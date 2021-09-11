package pixelitor.filters;

import com.jhlabs.image.GaussianFilter;
import org.jdesktop.swingx.graphics.ColorUtilities;
import pixelitor.filters.gui.FilterParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.ArrayList;

public class ComicBook extends ParametrizedFilter {

    public static final String NAME = "Comic Book Effect";

    private final RangeParam stepsParam = new RangeParam("Steps", 1, 4, 20);
    private final RangeParam blurRadiusParam = new RangeParam("Detail", 1, 66, 100);
    private final RangeParam edgesParam = new RangeParam("Edge Amount", 0, 66, 100);

    // Essentially threshold is a value in the range [0-256]. For any value of bias below the threshold, the corresponding color will be set to Black.
    // However, only values around [120-135] seems visually logical. Therefore this parameter focuses on picking fine values within that range.
    private final RangeParam thresholdParam = new RangeParam("Threshold Cut Off", 0, 50, 100);

    public ComicBook() {
        super(true);

        setParams(stepsParam, blurRadiusParam, edgesParam, thresholdParam);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {

        BufferedImage blurredI = blur(src, 30f - blurRadiusParam.getValue() / 100f * 30f);
        BufferedImage edgesI = edges(blurredI, edgesParam.getValue());
        BufferedImage grayI = gray(edgesI);
        BufferedImage stairedI = stairs(blurredI, stepsParam.getValue());
        BufferedImage finalI = threshold(grayI, stairedI, (int) (120 + 15 * thresholdParam.getPercentageValF()));

        return finalI;
    }

    public static BufferedImage blur(BufferedImage src, float radius) {
        GaussianFilter blur = new GaussianFilter(radius, NAME);
        blur.setPremultiplyAlpha(false);
        blur.setUseAlpha(false);
        return blur.filter(src, null);
    }

    public static BufferedImage edges(BufferedImage src, float radius) {
        BufferedImage blurredI = blur(src, radius);
        return ImageUtils.getHighPassFilteredImage(src, blurredI);
    }

    public static BufferedImage gray(BufferedImage src) {
        BufferedImage out = ImageUtils.createImageWithSameCM(src);

        int[] src_pixels = ImageUtils.getPixelsAsArray(src);
        int[] out_pixels = ImageUtils.getPixelsAsArray(out);

        for (int i = 0; i < src_pixels.length; i++) {
            int rgb = src_pixels[i];
            int r = (rgb >>> 16) & 0xFF;
            int g = (rgb >>> 8) & 0xFF;
            int b = rgb & 0xFF;
            r = (r + g + b) / 3;
            rgb = packSingleValue(r);
            out_pixels[i] = rgb;
        }

        return out;
    }

    public static BufferedImage stairs(BufferedImage src, int stair_steps) {

        Stairs stairs = new Stairs(stair_steps);
        BufferedImage out = ImageUtils.copyImage(src);
        int[] pixels = ImageUtils.getPixelsAsArray(out);
        for (int i = 0; i < pixels.length; i++) {

            int rgb = pixels[i];
            int r = (rgb >>> 16) & 0xFF;
            int g = (rgb >>> 8) & 0xFF;
            int b = rgb & 0xFF;

            float[] hsl = ColorUtilities.RGBtoHSL(r, g, b);
            hsl[2] = stairs.get(hsl[2]);

            int[] rgbs = ColorUtilities.HSLtoRGB(hsl[0], hsl[1], hsl[2], null);
            r = rgbs[0];
            g = rgbs[1];
            b = rgbs[2];

            rgb = ((0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
            pixels[i] = rgb;
        }
        return out;
    }

    public static BufferedImage threshold(BufferedImage bias_src, BufferedImage src, int threshold) {
        BufferedImage out = ImageUtils.copyImage(src);
        int[] bias_pix = ImageUtils.getPixelsAsArray(bias_src);
        int[] out_pix = ImageUtils.getPixelsAsArray(out);

        for (int i = 0; i < bias_pix.length; i++) {
            int bias = bias_pix[i] & 0xFF; // Just the b is enough because here we know that the image is gray scaled.
            if (bias < threshold) {
                out_pix[i] = Color.BLACK.getRGB();
            }
        }

        return out;
    }

    public static int packSingleValue(int c) {
        return ((0xFF) << 24) | ((c & 0xFF) << 16) | ((c & 0xFF) << 8) | (c & 0xFF);
    }

    public static class Stairs {

        private final float step_size;
        private final float[] points;

        public Stairs(int stairs) {
            this.step_size = 1f / stairs;
            float start = (step_size / 2);
            points = new float[stairs];

            int stepIndex = 0;
            for (; stepIndex < stairs; stepIndex++) {
                points[stepIndex] = start + stepIndex * step_size;
            }
        }

        public float get(float v) {
            for (int i = 0; i < points.length; i++) {
                if (v < points[i]) {
                    return i * step_size;
                }
            }
            return 1;
        }

    }

}
