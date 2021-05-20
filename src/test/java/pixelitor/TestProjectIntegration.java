package pixelitor;

import com.jhlabs.math.Noise;
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class TestProjectIntegration {

    private TestProjectIntegration() {
    }

    static ArrayList<File> files = new ArrayList<>();

    static String root = System.getProperty("user.dir");

    public static void main(String[] args) {

        FailOnThreadViolationRepaintManager.install();

        initialiseImages(root, 3);

        Pixelitor.main(getArgs());

        Runtime.getRuntime().addShutdownHook(new Thread(TestProjectIntegration::dispose));

    }

    private static String[] getArgs() {
        String[] args = new String[files.size() + 2];
        args[0] = "-PP='" + root + "'";
        args[1] = "-PI";
        for (int i = 0; i < files.size(); i++) args[i + 2] = files.get(i).getAbsolutePath();
        return args;
    }

    private static void initialiseImages(String root, int images) {
        for (int i = 0; i < images; i++) {
            String name = new String[]{"Fish", "Bird", "Melon", "Cat", "Noise", "Staple", "Bread"}[(int) (Math.random() * 7)] + " " + i;
            createImage(name, root);
        }
    }

    private static void createImage(String name, String root) {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);

        Noise.reseed();

        for (int i = 0; i < image.getWidth(); i++)
            for (int j = 0; j < image.getHeight(); j++)
                image.setRGB(i, j, new Color((Noise.noise2(i / 20f, j / 20f) + 1) / 3f, 0f, 0f).getRGB());

        try {
            ImageIO.write(image, "png", mark(root + File.separator + "tests" + File.separator + name + ".png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private static File mark(String path) {
        File f = new File(path);
        files.add(f);
        return f;
    }

    private static void dispose() {
        files.forEach(File::delete);
    }

}
