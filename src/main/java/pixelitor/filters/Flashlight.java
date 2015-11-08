package pixelitor.filters;

import com.jhlabs.image.ImageMath;
import com.jhlabs.image.PointFilter;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.utils.BlurredEllipse;

import java.awt.image.BufferedImage;

/**
 * Flashlight
 */
public class Flashlight extends FilterWithParametrizedGUI {
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final GroupedRangeParam radius = new GroupedRangeParam("Radius", 1, 200, 1000, false);
    private final RangeParam softness = new RangeParam("Softness", 0, 20, 100);
    private final IntChoiceParam bg = new IntChoiceParam("Background",
            new IntChoiceParam.Value[]{
                    new IntChoiceParam.Value("Black", Impl.BG_BLACK),
                    new IntChoiceParam.Value("Transparent", Impl.BG_TRANSPARENT),
            }
    );

    private Impl filter;

    public Flashlight() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                center,
                radius.adjustRangeToImageSize(1.0),
                softness,
                bg
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new Impl();
        }

        filter.setCenter(
                src.getWidth() * center.getRelativeX(),
                src.getHeight() * center.getRelativeY()
        );

        double radiusX = radius.getValueAsDouble(0);
        double radiusY = radius.getValueAsDouble(1);
        double softnessFactor = softness.getValueAsDouble() / 100.0;
        filter.setRadius(radiusX, radiusY, softnessFactor);

        filter.setBG(bg.getValue());

        dest = filter.filter(src, dest);

        return dest;
    }

    private static class Impl extends PointFilter {
        private double cx;
        private double cy;
        private double innerRadiusX;
        private double innerRadiusY;
        private double outerRadiusX;
        private double outerRadiusY;

        public static final int BG_BLACK = 0;
        public static final int BG_TRANSPARENT = 1;

        private int bgPixel;
        private BlurredEllipse ellipse;

        @Override
        public BufferedImage filter(BufferedImage src, BufferedImage dst) {
            ellipse = new BlurredEllipse(cx, cy,
                    innerRadiusX, innerRadiusY, outerRadiusX, outerRadiusY);
            return super.filter(src, dst);
        }

        @Override
        public int filterRGB(int x, int y, int rgb) {
            double outside = ellipse.isOutside(x, y);
            if (outside == 1.0) {
                return bgPixel;
            } else if (outside == 0.0) {
                return rgb;
            } else {
                return ImageMath.mixColors((float) outside, rgb, bgPixel);
            }
        }

        public void setCenter(double cx, double cy) {
            this.cx = cx;
            this.cy = cy;
        }

        public void setRadius(double radiusX, double radiusY, double softness) {
            this.innerRadiusX = radiusX - radiusX * softness;
            this.innerRadiusY = radiusY - radiusY * softness;

            this.outerRadiusX = radiusX + radiusX * softness;
            this.outerRadiusY = radiusY + radiusY * softness;
        }

        public void setBG(int bg) {
            if (bg == BG_BLACK) {
                bgPixel = 0xFF000000;
            } else if (bg == BG_TRANSPARENT) {
                bgPixel = 0;
            } else {
                throw new IllegalArgumentException("bg = " + bg);
            }
        }
    }
}
