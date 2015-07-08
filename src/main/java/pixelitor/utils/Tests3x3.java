package pixelitor.utils;

import pixelitor.Composition;
import pixelitor.ImageComponents;
import pixelitor.PixelitorWindow;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMask;
import pixelitor.menus.view.ZoomLevel;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Support for testing with small (3x3) images,
 * where it is feasible to check every pixel
 */
public class Tests3x3 {
    public static final int RED = Color.RED.getRGB();
    public static final int GREEN = Color.GREEN.getRGB();
    public static final int BLUE = Color.BLUE.getRGB();

    public static final int WHITE = Color.WHITE.getRGB();
    public static final int GRAY = Color.GRAY.getRGB();
    public static final int BLACK = Color.BLACK.getRGB();

    public static final int SEMI_TRANSPARENT_WHITE = rgbToPackedInt(128, 255, 0, 0);
    public static final int SEMI_TRANSPARENT_GRAY = rgbToPackedInt(128, 0, 255, 0);
    public static final int SEMI_TRANSPARENT_BLACK = rgbToPackedInt(128, 0, 0, 255);

    public static BufferedImage getStandardImage() {
        BufferedImage img = ImageUtils.createCompatibleImage(3, 3);
        img.setRGB(0, 0, RED);
        img.setRGB(1, 0, GREEN);
        img.setRGB(2, 0, BLUE);

        img.setRGB(0, 1, WHITE);
        img.setRGB(1, 1, GRAY);
        img.setRGB(2, 1, BLACK);

        img.setRGB(0, 2, SEMI_TRANSPARENT_WHITE);
        img.setRGB(1, 2, SEMI_TRANSPARENT_GRAY);
        img.setRGB(2, 2, SEMI_TRANSPARENT_BLACK);

        return img;
    }

    public static BufferedImage getStandardMaskImage() {
        BufferedImage img = new BufferedImage(3, 3, BufferedImage.TYPE_BYTE_GRAY);
        img.setRGB(0, 0, WHITE);
        img.setRGB(1, 0, WHITE);
        img.setRGB(2, 0, WHITE);

        img.setRGB(0, 1, GRAY);
        img.setRGB(1, 1, WHITE);
        img.setRGB(2, 1, GRAY);

        img.setRGB(0, 2, BLACK);
        img.setRGB(1, 2, GRAY);
        img.setRGB(2, 2, BLACK);

        return img;
    }

    private static int rgbToPackedInt(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static void addStandardImageWithMask() {
        BufferedImage img = getStandardImage();
        BufferedImage maskImg = getStandardMaskImage();

        Composition comp = Composition.fromImage(img, null, "3x3 Test");
        Layer layer = comp.getLayer(0);
        LayerMask mask = new LayerMask(comp, maskImg, layer);
        layer.addMaskBack(mask);
        PixelitorWindow.getInstance().addComposition(comp);
        comp.getIC().setZoom(ZoomLevel.Z6400, true);
    }

    // creates expected results from actual ones for regression tests
    public static String getExpectedFromActual(BufferedImage actual) {
        String s = "BufferedImage getExpectedImageAfter OP() {\n";
        int width = actual.getWidth();
        int height = actual.getHeight();
        s += String.format("BufferedImage img = ImageUtils.createCompatibleImage(3, 3);\n",
                width, height);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = actual.getRGB(x, y);
                int a = (rgb >>> 24) & 0xFF;
                int r = (rgb >>> 16) & 0xFF;
                int g = (rgb >>> 8) & 0xFF;
                int b = rgb & 0xFF;

                s += String.format("    img.setRGB(%d, %d, rgbToPackedInt(%d, %d, %d, %d);\n",
                        x, y, a, r, g, b);
            }
        }

        s += "    return img;\n}\n";
        return s;
    }

    public static void dumpCompositeOfActive() {
        BufferedImage img = ImageComponents.getActiveComp().get().calculateCompositeImage();
        String actual = getExpectedFromActual(img);
        System.out.println(String.format("Tests3x3::dumpCompositeOfActive: actual = '%s'", actual));
    }
}
