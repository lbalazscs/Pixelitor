package pixelitor.tools;

import java.awt.Graphics2D;

/**
 * Different tools can have different clipping requirements, which are
 * represented here
 */
public enum ClipStrategy {
    FULL_AREA {
        @Override
        public void setClip(Graphics2D g) {
            // clear the clipping
            g.setClip(null);
        }
    }, IMAGE_ONLY {
        @Override
        public void setClip(Graphics2D g) {
            // empty: the image clipping has been already set
        }
    };

    public abstract void setClip(Graphics2D g);
}
