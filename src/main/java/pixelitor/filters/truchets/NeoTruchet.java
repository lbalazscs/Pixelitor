package pixelitor.filters.truchets;

import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.RandomizePolicy;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ImageUtils;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;
import static pixelitor.filters.gui.TransparencyPolicy.USER_ONLY_TRANSPARENCY;

public class NeoTruchet extends ParametrizedFilter {

    public static final String NAME = "zxc Truchet Tiles";

    private final RangeParam sizeParam = new RangeParam("Tile Size", 2, 20, 100);
    private final RangeParam widthParam = new RangeParam("Line Width", 1, 3, 20);
    private final TruchetParam truchetParam = new TruchetParam("Truchet Param", RandomizePolicy.ALLOW_RANDOMIZE);
    private final ColorParam bgColor = new ColorParam("Background Color", WHITE, USER_ONLY_TRANSPARENCY);
    private final ColorParam fgColor = new ColorParam("Foreground Color", BLACK, USER_ONLY_TRANSPARENCY);
    private final BooleanParam showBoundary = new BooleanParam("Show Tile Boundary", false, IGNORE_RANDOMIZE);

    public NeoTruchet() {
        super(true);

        setParams(
            sizeParam,
            widthParam,
            truchetParam,
            bgColor,
            fgColor,
            showBoundary
        );
    }

    @Override
    protected BufferedImage transform(BufferedImage src, BufferedImage dest) {
        TruchetSwatch swatch = truchetParam.getSwatch();
        int tileSize = sizeParam.getValue();
        int lineWidth = widthParam.getValue();

        dest = ImageUtils.copyImage(src);

        int W = dest.getWidth();
        int H = dest.getHeight();
        int w = swatch.getWidth(tileSize);
        int h = swatch.getHeight(tileSize);

        Graphics2D g = dest.createGraphics();
        g.setColor(bgColor.getColor());
        g.fillRect(0, 0, W, H);
        g.setColor(fgColor.getColor());

        int numTilesHor = W / w + 1;
        int numTilesVer = H / h + 1;

        int widthCovered = W - numTilesHor * w;
        int heightCovered = H - numTilesVer * h;

        for (int hi = 0; hi < numTilesVer; hi++) {
            for (int wi = 0; wi < numTilesHor; wi++) {
                g.translate(widthCovered + wi * w, heightCovered + hi * h);
                swatch.draw(g, tileSize, lineWidth);
                g.translate(-(widthCovered + wi * w), -(heightCovered + hi * h));
            }
        }

        return dest;
    }
}
