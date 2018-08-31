package pixelitor.filters.curves;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.EnumMap;

/**
 * Represents set of [RGB,R,G,B] curves
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ToneCurves {
    private EnumMap<ToneCurveType, ToneCurve> curve = new EnumMap(ToneCurveType.class);
    private ToneCurveType activeCurveType = ToneCurveType.RGB;
    private Graphics2D gr;
    private BasicStroke gridStroke = new BasicStroke(1);
    private int width = 295;
    private int height = 295;
    private int curveWidth = 255;
    private int curveHeight = 255;
    private int curvePadding = 10;
    private int axisPadding = 20;
    private int axisSize = 10;
    private int gridDensity = 4;

    public ToneCurves() {
        curve.put(ToneCurveType.RGB, new ToneCurve(ToneCurveType.RGB));
        curve.put(ToneCurveType.RED, new ToneCurve(ToneCurveType.RED));
        curve.put(ToneCurveType.GREEN, new ToneCurve(ToneCurveType.GREEN));
        curve.put(ToneCurveType.BLUE, new ToneCurve(ToneCurveType.BLUE));
        setActiveCurve(ToneCurveType.RGB);
    }

    public ToneCurve getCurve(ToneCurveType curveType) {
        return curve.get(curveType);
    }

    public ToneCurve getActiveCurve() {
        return curve.get(activeCurveType);
    }

    public void setActiveCurve(ToneCurveType curveType) {
        curve.get(this.activeCurveType).setActive(false);
        curve.get(curveType).setActive(true);
        this.activeCurveType = curveType;
    }

    public void setSize(int width, int height){
        this.width = width;
        this.height = height;
        this.curveWidth = width - 2 * curvePadding - axisPadding;
        this.curveHeight = height - 2 * curvePadding - axisPadding;
        for (ToneCurveType type: curve.keySet()) {
            curve.get(type).setSize(this.curveWidth, this.curveHeight);
        }
    }

    public void reset() {
        for (ToneCurveType type: curve.keySet()) {
            curve.get(type).reset();
        }
    }

    public void normalizePoint(Point.Float p) {
        p.x -= curvePadding + axisPadding;
        p.y -= curvePadding;

        p.y = this.curveHeight - p.y;
        p.x /= this.curveWidth;
        p.y /= this.curveHeight;
    }

    public void setG2D(Graphics2D gr) {
        this.gr = gr;
        for (ToneCurveType type: curve.keySet()) {
            curve.get(type).setG2D(gr);
        }
    }

    public void draw() {
        gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // clear background
        gr.setColor(Color.WHITE);
        gr.fillRect(0, 0, width, height);

        // apply curvePadding, and prepare for y-axis up drawing
        AffineTransform transform = gr.getTransform();
        AffineTransform curveTransform = new AffineTransform();
        curveTransform.translate(curvePadding + axisPadding, curvePadding);
        curveTransform.translate(0, curveHeight);
        curveTransform.scale(1.0, -1.0);
        gr.setTransform(curveTransform);

        drawGrid();
        drawDiagonal();
        drawScales();
        drawCurves();

        gr.setTransform(transform);
    }

    private void drawGrid() {
        Path2D.Float lightPath2D = new Path2D.Float();
        Path2D.Float darkPath2D = new Path2D.Float();
        Path2D.Float path2D;

        float gridWidth = (float) curveWidth / gridDensity;
        float gridHeight = (float) curveHeight / gridDensity;
        for (int i = 0; i <= gridDensity; i++) {
            path2D = i % 2 == 0 ? darkPath2D : lightPath2D;
            // horizontal
            path2D.moveTo(0, i * gridHeight);
            path2D.lineTo(curveWidth, i * gridHeight);

            // vertical
            path2D.moveTo(i * gridWidth, 0);
            path2D.lineTo(i * gridWidth, curveHeight);
        }

        gr.setStroke(gridStroke);

        gr.setColor(Color.LIGHT_GRAY);
        gr.draw(lightPath2D);

        gr.setColor(Color.GRAY);
        gr.draw(darkPath2D);
    }

    private void drawScales() {
        // draw horizontal
        Rectangle.Float rectH = new Rectangle.Float(0, -axisPadding, curveWidth, axisSize);
        GradientPaint gradientH = new GradientPaint(0, 0, Color.BLACK, curveWidth, 0, Color.WHITE);
        gr.setPaint(gradientH);
        gr.fill(rectH);
        gr.setColor(Color.LIGHT_GRAY);
        gr.draw(rectH);

        // draw vertical
        Rectangle.Float rectV = new Rectangle.Float(-axisPadding, 0, axisSize, curveHeight);
        gradientH = new GradientPaint(0, 0, Color.BLACK, 0, curveHeight, Color.WHITE);
        gr.setPaint(gradientH);
        gr.fill(rectV);
        gr.setColor(Color.LIGHT_GRAY);
        gr.draw(rectV);
    }

    private void drawDiagonal() {
        gr.setColor(Color.GRAY);
        gr.setStroke(gridStroke);
        gr.drawLine(0, 0, curveWidth, curveHeight);
    }

    private void drawCurves() {
        // on back draw inactive curves
        for (ToneCurveType type: curve.keySet()) {
            if (type != activeCurveType) {
                curve.get(type).draw();
            }
        }

        // on top draw active curve
        curve.get(activeCurveType).draw();
    }
}
