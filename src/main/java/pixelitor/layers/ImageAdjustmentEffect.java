package pixelitor.layers;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Both adjustment layers and watermarked text layers perform
 * image adjustments. Here is the common functionality.
 */
public interface ImageAdjustmentEffect {
    // some Layer methods that will be used by adjustImageWithMasksAndBlending
    boolean isNormalAndOpaque();

    void setupDrawingComposite(Graphics2D g, boolean isFirstVisibleLayer);

    LayerMask getMask();

    // the common functionality
    default BufferedImage adjustImageWithMasksAndBlending(BufferedImage imgSoFar, boolean isFirstVisibleLayer) {
        if (isFirstVisibleLayer) {
            return imgSoFar; // there's nothing we can do
        }
        BufferedImage transformed = adjustImage(imgSoFar);
        LayerMask layerMask = getMask();
        if (layerMask != null) {
            layerMask.applyToImage(transformed);
        }
        if (layerMask == null && isNormalAndOpaque()) {
            return transformed;
        } else {
            Graphics2D g = imgSoFar.createGraphics();
            setupDrawingComposite(g, isFirstVisibleLayer);
            g.drawImage(transformed, 0, 0, null);
            g.dispose();
            return imgSoFar;
        }
    }

    // this needs to be implemented by subtypes
    // the returned object must be different from the argument
    BufferedImage adjustImage(BufferedImage src);
}
