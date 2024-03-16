package pixelitor.filters.truchets;

import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.RandomizePolicy;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;
import static pixelitor.filters.gui.TransparencyPolicy.USER_ONLY_TRANSPARENCY;

public class NeoTruchet extends ParametrizedFilter {

    public static final String NAME = "zxc Truchet Tiles";

    private final RangeParam sizeParam = new RangeParam("Tile Size", 2, 20, 100);
    private final RangeParam widthParam = new RangeParam("Line Width", 1, 3, 20);
    private final TruchetParam truchetParam = new TruchetParam("Truchet Param", RandomizePolicy.ALLOW_RANDOMIZE);
    private final ColorParam bgColor = new ColorParam("Background Color", WHITE, USER_ONLY_TRANSPARENCY);
    private final ColorParam fgColor = new ColorParam("Foreground Color", BLACK, USER_ONLY_TRANSPARENCY);
    private final BooleanParam showTileBoundary = new BooleanParam("Show Tile Boundary", false, IGNORE_RANDOMIZE);
    private final BooleanParam showSwatchBoundary = new BooleanParam("Show Swatch Boundary", false, IGNORE_RANDOMIZE);

    public NeoTruchet() {
        super(true);

        setParams(
            sizeParam,
            widthParam,
            truchetParam,
            bgColor,
            fgColor,
            showTileBoundary,
            showSwatchBoundary
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
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        int numSwatchesHor = W / w + 1;
        int numSwatchesVer = H / h + 1;

        int widthCovered = (W - numSwatchesHor * w) / 2;
        int heightCovered = (H - numSwatchesVer * h) / 2;

        for (int hi = 0; hi < numSwatchesVer; hi++) {
            for (int wi = 0; wi < numSwatchesHor; wi++) {
                int x = widthCovered + wi * w, y = heightCovered + hi * h;
                g.translate(x, y);
                swatch.draw(g, tileSize, lineWidth);
                g.translate(-x, -y);
            }
        }

        if (showTileBoundary.isChecked()) {
            g.setColor(Color.RED);
            for (int hi = 0; hi < numSwatchesVer; hi++) {
                int y = heightCovered + hi * h;
                for (int i = 0; i < swatch.getHeight(1); i++) {
                    g.drawLine(0, y + i * tileSize, dest.getWidth(), y + i * tileSize);
                }
            }
            for (int wi = 0; wi < numSwatchesHor; wi++) {
                int x = widthCovered + wi * w;
                for (int i = 0; i < swatch.getWidth(1); i++) {
                    g.drawLine(x + i * tileSize, 0, x + i * tileSize, dest.getHeight());
                }
            }
        }

        if (showSwatchBoundary.isChecked()) {
            g.setColor(Color.GREEN);
            for (int hi = 0; hi < numSwatchesVer; hi++) {
                int y = heightCovered + hi * h;
                g.drawLine(0, y, dest.getWidth(), y);
            }
            for (int wi = 0; wi < numSwatchesHor; wi++) {
                int x = widthCovered + wi * w;
                g.drawLine(x, 0, x, dest.getHeight());
            }
        }

        return dest;
    }
}
