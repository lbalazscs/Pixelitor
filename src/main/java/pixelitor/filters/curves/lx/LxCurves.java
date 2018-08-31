package pixelitor.filters.curves.lx;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;

public class LxCurves {
    public static final int RGB = 0;
    public static final int RED = 1;
    public static final int GREEN = 2;
    public static final int BLUE = 3;

    public LxCurve[] curve = new LxCurve[4];
    public int activeCurve = RGB;
    Graphics2D gr;
    BasicStroke gridStroke = new BasicStroke(1);
    int width = 255;
    int height = 255;
    int gridDensity = 4;
    int padding = 10;
    AffineTransform curveTransform = new AffineTransform();

    public LxCurves() {
        curve[RGB] = new LxCurve(LxCurveType.RGB);
        curve[RED] = new LxCurve(LxCurveType.RED);
        curve[GREEN] = new LxCurve(LxCurveType.GREEN);
        curve[BLUE] = new LxCurve(LxCurveType.BLUE);
        setActiveCurve(RGB);

        // padding
        curveTransform = new AffineTransform();
        curveTransform.translate(padding, padding);
        curveTransform.translate(0, height);
        curveTransform.scale(1.0, -1.0);

    }

    public LxCurve getActiveCurve() {
        return curve[activeCurve];
    }

    public void setActiveCurve(int activeCurve) {
        curve[this.activeCurve].setActive(false);
        curve[activeCurve].setActive(true);
        this.activeCurve = activeCurve;
    }

    public void setSize(int width, int height){
        this.width = width - 2*padding;
        this.height = height - 2*padding;
        for (int i = 0; i < 4; i++) {
            curve[i].setSize(this.width, this.height);
        }
    }

    public void reset() {
        for (int i = 0; i < 4; i++) {
            curve[i].reset();
        }
    }

    public void normalizePoint(Point.Float p) {
        p.x -= padding;
        p.y -= padding;

        p.y = height - p.y;
        p.x /= width;
        p.y /= height;
    }

    public void setG2D(Graphics2D gr) {
        this.gr = gr;
        for (int i = 0; i < 4; i++) {
            curve[i].setG2D(gr);
        }
    }

    public void draw() {
        gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // clear background
        gr.setColor(Color.WHITE);
        gr.fillRect(0,0,width + 2*padding, height + 2*padding);

        // apply padding
        AffineTransform transform = gr.getTransform();
        gr.setTransform(curveTransform);

        drawGrid();
        drawDiagonal();
        drawHistogram();
        drawCurves();

        gr.setTransform(transform);
    }

    public void drawBackground() {
        gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gr.setColor(Color.WHITE);
        gr.fillRect(0,0,width,height);
    }

    private void drawGrid() {
        Path2D.Float path2D = new Path2D.Float();
        float gridWidth = (float) width / gridDensity;
        float gridHeight = (float) height / gridDensity;
        for (int i = 0; i <= gridDensity; i++) {
            // horizontal
            path2D.moveTo(0, i * gridHeight);
            path2D.lineTo(width, i * gridHeight);

            // vertical
            path2D.moveTo(i * gridWidth, 0);
            path2D.lineTo(i * gridWidth, height);
        }

        gr.setColor(Color.LIGHT_GRAY);
        gr.setStroke(gridStroke);
        gr.draw(path2D);
    }

    private void drawDiagonal() {
        gr.setColor(Color.GRAY);
        gr.setStroke(gridStroke);
        gr.drawLine(0, 0, width, height);
    }

    private void drawHistogram() {
        // nothing
    }

    private void drawCurves() {
        // on back inactive curves
        for (int i = 3; i >= 0; i--) {
            if (i == activeCurve) continue;
            curve[i].draw();
        }

        // on top active curve
        curve[activeCurve].draw();
    }
}
