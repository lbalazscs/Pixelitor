package pixelitor.filters.impl;

import com.jhlabs.image.SwirlMethod;
import com.jhlabs.image.TransformFilter;
import net.jafama.FastMath;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/**
 * Based on http://stackoverflow.com/questions/225548/resources-for-image-distortion-algorithms
 */
public class SwirlFilter extends TransformFilter implements SwirlMethod {
    private float swirlAmount;
    private float radius;
    private float radius2;
    private float centerX;
    private float centerY;
    private float zoom;
    private float cx;
    private float cy;
    private float rotateResultAngle;
    private float pinchBulgeAmount;

    @Override
    public void setCenterX(float centerX) {
        this.centerX = centerX;
    }

    @Override
    public void setCenterY(float centerY) {
        this.centerY = centerY;
    }

    @Override
    public void setSwirlAmount(float swirlAmount) {
        this.swirlAmount = swirlAmount;
    }

    @Override
    public void setRadius(float radius) {
        this.radius = radius;
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
        float dx = x - cx;
        float dy = y - cy;
        float angle = (float) (swirlAmount * FastMath.exp(-(dx * dx + dy * dy) / (radius2)));

        angle -= rotateResultAngle;

        double sin = FastMath.sin(angle);
        double cos = FastMath.cos(angle);

        u = (float) (cos * dx + sin * dy) / zoom;
        v = (float) (-sin * dx + cos * dy) / zoom;
        out[0] = (u + cx) ;
        out[1] = (v + cy );
    }

    @Override
    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    @Override
    public void setRotateResultAngle(float rotateResultAngle) {
        this.rotateResultAngle = rotateResultAngle;
    }

    @Override
    public void setPinchBulgeAmount(float pinchBulgeAmount) {
        this.pinchBulgeAmount = pinchBulgeAmount;
    }

    @Override
    public Shape[] getAffectedAreaShapes() {
        return new Shape[]{
                new Ellipse2D.Float(cx - radius, cy - radius, 2 * radius, 2 * radius)
        };
    }
}
