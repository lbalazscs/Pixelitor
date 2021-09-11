package pixelitor.filters;

import com.jhlabs.image.DoGFilter;
import com.jhlabs.image.GaussianFilter;
import org.jdesktop.swingx.graphics.ColorUtilities;
import pixelitor.AppContext;
import pixelitor.filters.gui.FilterParam;
import pixelitor.filters.gui.GroupedRangeParam;
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

    private final RangeParam dogRadius1Param = new RangeParam("DOG Radius 1", 0, 0, 20);
    private final RangeParam dogRadius2Param = new RangeParam("DOG Radius 2", 0, 10, 20);

    private final RangeParam scalingBottomParam = new RangeParam("Scaling Bottom", 0, 128, 256);
    private final RangeParam scalingTopParam = new RangeParam("Scaling Bottom", 0, 0, 256);

    private final RangeParam thresholdParam = new RangeParam("Threshold Cut Off", 0, 128, 256);

    public ComicBook() {
        super(true);

        ArrayList<FilterParam> params = new ArrayList<>();

        params.add(stepsParam);
        params.add(blurRadiusParam);

        if (AppContext.enableExperimentalFeatures) {
            params
                .add(new GroupedRangeParam("Difference Of Gaussian", new RangeParam[]{dogRadius1Param, dogRadius2Param}, false));
            params
                .add(new GroupedRangeParam("Mapping Image Values", new RangeParam[]{scalingBottomParam, scalingTopParam}, false));
            params.add(thresholdParam);
        }

        setParams(params.toArray(FilterParam[]::new));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {

        BufferedImage blurredI = blur(src, 30f - blurRadiusParam.getValue() / 100f * 30f);
        BufferedImage grayI = gray(blurredI);
        BufferedImage edgesI = edges(grayI, dogRadius1Param.getValueAsFloat(), dogRadius2Param.getValueAsFloat());
        BufferedImage scaledI = scale(edgesI, scalingBottomParam.getValue(), scalingTopParam.getValue());
        BufferedImage stairedI = stairs(blurredI, stepsParam.getValue());
        BufferedImage finalI = threshold(scaledI, stairedI, thresholdParam.getValue());

        return finalI;
    }

    public static BufferedImage blur(BufferedImage src, float radius) {
        GaussianFilter blur = new GaussianFilter(radius, NAME);
        blur.setPremultiplyAlpha(false);
        blur.setUseAlpha(false);
        return blur.filter(src, null);
    }

    public static BufferedImage gray(BufferedImage src) {
        BufferedImage out = createCompatibleDestImage(src);

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

    public static BufferedImage edges(BufferedImage src, float radius1, float radius2) {
        DoGFilter dog = new DoGFilter(NAME);
        dog.setRadius1(radius1);
        dog.setRadius2(radius2);
        return dog.filter(src, null);
    }

    public static BufferedImage scale(BufferedImage src, int bot, int top) {

        /*if (top < bot) {
            int t = top;
            top = bot;
            bot = t;
        } else */
        if (top == bot) {
            return ImageUtils.copyImage(src);
        }

        BufferedImage out = ImageUtils.copyImage(src);
        int[] pixels = ImageUtils.getPixelsAsArray(out);
        for (int i = 0; i < pixels.length; i++) {
            int rgb = pixels[i];
            int r = (rgb >>> 16) & 0xFF;
            r = mapColor(r, bot, top);
//            if (r < param.getValue()) {
//                rgb = 0;
//            } else {
//                rgb = 1;
//            }
            rgb = packSingleValue(r);
            pixels[i] = rgb;
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

    public static int mapColor(int i, float p, float q) {
        return map(i, 0, 255, p, q);
    }

    public static int map(int i, float a, float b, float p, float q) {
        return (int) ((i - a) * (q - p) / (b - a) + p);
    }

    // TODO: Wait What!? It Should be a static method!
    public static BufferedImage createCompatibleDestImage(BufferedImage src) {
        ColorModel dstCM = src.getColorModel();
        return new BufferedImage(dstCM, dstCM.createCompatibleWritableRaster(src.getWidth(), src.getHeight()), dstCM
            .isAlphaPremultiplied(), null);
    }

    public static class Stairs {

        private final float step_size;
        private final float start;
        private final float end;
        private final float[] points;

        public Stairs(int stairs) {
            this.step_size = 1f / stairs;
            this.start = (step_size / 2);
            this.end = 1 - this.start;
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
