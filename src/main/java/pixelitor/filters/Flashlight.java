package pixelitor.filters;

import com.jhlabs.image.ImageMath;
import com.jhlabs.image.PointFilter;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.BlurredEllipse;

import java.awt.image.BufferedImage;

/**
 * Flashlight
 */
public class Flashlight extends FilterWithParametrizedGUI {
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final GroupedRangeParam radius = new GroupedRangeParam("Radius", 1, 1000, 200, false);
    private final RangeParam penumbraMultiplier = new RangeParam("Penumbra (Radius %)", 1, 500, 50);
    private final IntChoiceParam bg = new IntChoiceParam("Background",
            new IntChoiceParam.Value[]{
                    new IntChoiceParam.Value("Black", Impl.BG_BLACK),
                    new IntChoiceParam.Value("Transparent", Impl.BG_TRANSPARENT),
            }
    );

    private Impl filter;

    public Flashlight() {
        super("Flashlight", true, false);
        setParamSet(new ParamSet(
                center,
                radius.adjustRangeToImageSize(1.0),
                penumbraMultiplier,
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
        filter.setRadius(radius.getValueAsDouble(0),
                radius.getValueAsDouble(1),
                (penumbraMultiplier.getValueAsDouble() + 100.0) / 100.0);
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
                return ImageMath.mixColors((float) outside, bgPixel, rgb);
            }
        }

        public void setCenter(double cx, double cy) {
            this.cx = cx;
            this.cy = cy;
        }

        public void setRadius(double innerRadiusX, double innerRadiusY, double penumbraMultiplier) {
            this.innerRadiusX = innerRadiusX;
            this.innerRadiusY = innerRadiusY;
            this.outerRadiusX = innerRadiusX * penumbraMultiplier;
            this.outerRadiusY = innerRadiusY * penumbraMultiplier;
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
