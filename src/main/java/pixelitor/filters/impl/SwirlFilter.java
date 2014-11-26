package pixelitor.filters.impl;

import com.jhlabs.image.TransformFilter;
import net.jafama.FastMath;

import java.awt.image.BufferedImage;

/**
 * Based on http://stackoverflow.com/questions/225548/resources-for-image-distortion-algorithms
 */
public class SwirlFilter extends TransformFilter {
    private float amount;
    private float radius2;
    private float centerX;
    private float centerY;
    private float zoom;
    private int cx;
    private int cy;
    private float rotateResultAngle;

    public void setCenterX(float centerX) {
        this.centerX = centerX;
    }

    public void setCenterY(float centerY) {
        this.centerY = centerY;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public void setRadius(float radius) {
        this.radius2 = radius * radius;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        cx = (int) (centerX * src.getWidth());
        cy = (int) (centerY * src.getHeight());

        return super.filter(src, dst);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        float u, v;
        int dx = x - cx;
        int dy = y - cy;
        float angle = (float) (amount * FastMath.exp(-(dx * dx + dy * dy) / (radius2)));

        angle -= rotateResultAngle;

        double sin = FastMath.sin(angle);
        double cos = FastMath.cos(angle);

        u = (float) (cos * dx + sin * dy) / zoom;
        v = (float) (-sin * dx + cos * dy) / zoom;
        out[0] = (u + cx) ;
        out[1] = (v + cy );
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    public void setRotateResultAngle(float rotateResultAngle) {
        this.rotateResultAngle = rotateResultAngle;
    }
}
