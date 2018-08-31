package pixelitor.filters.curves.lx;

import com.jhlabs.image.Curve;
import com.jhlabs.image.ImageMath;

import java.awt.*;
import java.awt.geom.Path2D;

public class LxCurve {
    public final static int MAX_KNOTS = 16;
    public final static float RADIUS = 0.04F;
    public final static float NEARBY_RADIUS = 0.08F;
    public Curve curve = new Curve();
    LxCurveType curveType;
    int width = 255;
    int height = 255;
    int[] curvePlotData;
    boolean isDirty = true;
    boolean active = false;
    Graphics2D gr;
    BasicStroke curveStroke = new BasicStroke(1);
    BasicStroke pointStroke = new BasicStroke(2);

    public LxCurve(LxCurveType curveType) {
        this.curveType = curveType;
    }

    public void setSize(int width, int height){
        this.width = width;
        this.height = height;
    }

    public void init() {
        curve.x = new float[] {0.0F, 0.3F, 0.6F, 0.9F, 1.0F};
        curve.y = new float[] {0.0F, 0.2F, 0.8F, 0.9F, 1.0F};
        isDirty = true;
    }

    public void reset() {
        curve.x = new float[] {0, 1};
        curve.y = new float[] {0, 1};
        isDirty = true;
    }

    private void initCurvePlotData() {
        if (isDirty) {
            isDirty = false;
            curvePlotData = curve.makeTable();
        }
    }

    private void preparePoint(Point.Float p) {
        p.x = ImageMath.clamp(p.x, 0, 1);
        p.y = ImageMath.clamp(p.y, 0, 1);
    }

    private boolean isClose(Point.Float p, Point.Float q)  {
        if(Math.abs(p.x - q.x) < NEARBY_RADIUS) {
            return true;
        }
        return false;
    }

    public int addPoint(Point.Float p) {
        int lastIndex = curve.x.length - 1;
        int index = curve.findKnotPos(p.x);

        // cannot add at first/last position
        if (index <=0 || index > lastIndex) {
            return -1;
        }

        // if point is too close to next/prev -> replace the nearest
        int prevIndex = index-1;
        if (isClose(p, new Point.Float(curve.x[prevIndex], curve.y[prevIndex]))) {
            setPointPosition(prevIndex, p);
            return prevIndex;
        } else if (isClose(p, new Point.Float(curve.x[index], curve.y[index]))) {
            setPointPosition(index, p);
            return index;
        }

        // check for max limit
        if (curve.x.length >= MAX_KNOTS) {
            return -1;
        }

        isDirty = true;
        return curve.addKnot(p.x, p.y);
    }

    public void deletePoint(int index) {
        if (index <=0 || index >= curve.x.length -1) {
            return;
        }
        isDirty = true;
        curve.removeKnot(index);
    }

    /**
     * Set new point location
     * @param index point index
     * @param p normalized point data
     */
    public void setPointPosition(int index, Point.Float p){
        int lastIndex = curve.x.length - 1;

        if (index <0 || index > lastIndex) {
            return;
        }

        if (index == 0) {
            // first point can't change x axis
            p.x = 0.0F;
        } else if(index == lastIndex) {
            // last point can't change x axis
            p.x = 1.0F;
        } else {
            // check prev/next index
            if (p.x < curve.x[index-1]) {
                p.x = curve.x[index-1] + 0.001F;
            } else if (p.x > curve.x[index+1]) {
                p.x = curve.x[index+1] - 0.001F;
            }
        }

        curve.x[index] = ImageMath.clamp(p.x, 0, 1);
        curve.y[index] = ImageMath.clamp(p.y, 0, 1);
        isDirty = true;
    }

    private boolean isOver(Point.Float p, Point.Float q)  {
        if(Math.abs(p.x - q.x) < RADIUS) {
            if(Math.abs(p.y - q.y) < RADIUS) {
                return true;
            }
        }
        return false;
    }

    public boolean isOverPoint(Point.Float p) {
        for (int i = 0; i < curve.x.length; i++) {
            if (isOver(p, new Point.Float(curve.x[i], curve.y[i]))) {
                return true;
            }
        }

        return false;
    }

    public boolean isOverPoint(int index) {
        Point.Float p = new Point.Float(curve.x[index], curve.y[index]);
        for (int i = 0; i < curve.x.length; i++) {
            if (i != index && isOver(p, new Point.Float(curve.x[i], curve.y[i]))) {
                return true;
            }
        }

        return false;
    }

    public int getPointIndexAt(Point.Float p) {
        for (int i = 0; i < curve.x.length; i++) {
            if (isOver(p, new Point.Float(curve.x[i], curve.y[i]))) {
                return i;
            }
        }

        return -1;
    }

    public Point.Float getPointAt(Point.Float p) {
        for (int i = 0; i < curve.x.length; i++) {
            Point.Float p2 = new Point.Float(curve.x[i], curve.y[i]);
            if (isOver(p, p2)) {
                return p2;
            }
        }

        return null;
    }

    public void setG2D(Graphics2D gr) {
        this.gr = gr;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void draw() {
        drawCurve();
        if (active) {
            drawPoints();
        }
    }

    private void drawCurve() {
        initCurvePlotData();
        Path2D.Float path2D = new Path2D.Float();
        path2D.moveTo(0, ((float) curvePlotData[0] / 255) * height);
        for (int i = 0; i < curvePlotData.length; i++) {
            float x = ((float) i / 255) * width;
            float y = ((float) curvePlotData[i] / 255) * height;
            path2D.lineTo(x, y);
        }

        gr.setColor(active ? this.curveType.color : this.curveType.colorInactive);
        gr.setStroke(curveStroke);
        gr.draw(path2D);
    }

    private void drawPoints() {
        gr.setColor(Color.black);
        gr.setStroke(pointStroke);
        for (int i = 0; i < curve.x.length; i++) {
            gr.drawOval(
                    (int)(curve.x[i] * width) - 6,
                    (int)(curve.y[i] * height) - 6,
                    12,
                    12
            );
        }
    }
}