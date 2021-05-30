package pixelitor.tools;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.gui.AutoZoom;
import pixelitor.gui.View;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PRectangle;
import pixelitor.utils.Cursors;
import pixelitor.utils.Shapes;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_MITER;
import static pixelitor.tools.DragToolState.*;

public class ZoomTool_new extends DragTool {

    private final JToggleButton zoomType = new JToggleButton();
    private PRectangle rect = null;
    private DragToolState state = NO_INTERACTION;

    public ZoomTool_new() {
        super("Zoom", 'Z', "zoom_tool.png",
                "<b>click</b> to zoom in, " +
                        "<b>right-click</b> (or <b>Alt-click</b>) to zoom out." +
                        "<b>drag</b> to select an area.",
                Cursors.HAND, false);
        spaceDragStartPoint = true;
    }

    public static void ZoomToFit(View view, PRectangle rect) {

        Rectangle2D rectIm = rect.getIm();
        if (rectIm.isEmpty()) {
            return;
        }

        Canvas temp = new Canvas((int) rectIm.getWidth(), (int) rectIm.getHeight());

        view.setZoom(ZoomLevel.calcZoom(temp, AutoZoom.FIT_SPACE, true));
        view.scrollRectToVisible(rect.getCo());
    }

    @Override
    public void initSettingsPanel() {
        zoomType.setIcon(ZoomToolIcon.POSITIVE);
        zoomType.setPressedIcon(ZoomToolIcon.NEGETIVE);
        settingsPanel.add(zoomType);
    }

    @Override
    public void dragStarted(PMouseEvent e) {
        if (state == NO_INTERACTION) {
            state = INITIAL_DRAG;
            return;
        }

        if (state == INITIAL_DRAG) {
            throw new IllegalStateException("Drag started while in state 'INITIAL_DRAG'");
        }

        if (state == TRANSFORM) {
            throw new IllegalStateException("There is no 'TRANSFORM' state for Zoom tool!");
        }
    }

    @Override
    public void ongoingDrag(PMouseEvent e) {
//            Shapes.drawVisibly(g, getSelectedCoRect());  // TODO?

        // No special actions needed here, unless want a
        // special behaviour with Alt/Shift key events.

//        if (userDrag != null) {
//            userDrag.setStartFromCenter(e.isAltDown());
//            userDrag.setEquallySized(e.isShiftDown());
//        }
    }

    @Override
    public void dragFinished(PMouseEvent e) {
        var comp = e.getComp();
        comp.update();

        if (state == NO_INTERACTION) {
            return;
        }

        if (state == INITIAL_DRAG) {

            // TODO: check if size is too less, and determine it as a click

            Rectangle r = userDrag.toCoRect();
            PRectangle rect = PRectangle.positiveFromCo(r, e.getView());

            rect.makeCoPositive();
            rect.recalcIm(e.getView());

            ZoomToFit(e.getView(), rect);
            
            resetInitialState();
            return;
        }

        if (state == TRANSFORM) {
            throw new IllegalStateException("There is no 'TRANSFORM' state for Zoom tool!");
        }
    }

    @Override
    public void coCoordsChanged(View view) {
        if (rect != null) {
            rect.coCoordsChanged(view);
        }
    }

    @Override
    public void imCoordsChanged(AffineTransform at, Composition comp) {
        if (rect != null) {
            rect.imCoordsChanged(comp.getView(), at);
        }
    }

    @Override
    public void resetInitialState() {
        rect = null;
        state = NO_INTERACTION;

        OpenImages.repaintActive();
        OpenImages.setCursorForAll(Cursors.DEFAULT);
    }

    @Override
    public Icon createIcon() {
        return ZoomToolIcon.POSITIVE;
    }

    private static class ZoomToolIcon extends Tool.ToolIcon {

        public static final ToolIcon POSITIVE = new ZoomToolIcon(true);
        public static final ToolIcon NEGETIVE = new ZoomToolIcon(false);

        private final boolean drawMinus;

        public ZoomToolIcon(boolean drawMinus) {
            this.drawMinus = drawMinus;
        }

        @Override
        public void paintIcon(Graphics2D g) {
            // based on zoom_tool.svg

            Ellipse2D circle = new Ellipse2D.Double(9, 1, 18, 18);
            g.setStroke(new BasicStroke(2.0f, CAP_BUTT, JOIN_MITER, 4));
            g.draw(circle);

            if (drawMinus) {
                Line2D hor = new Line2D.Double(12.0, 10.0, 24.0, 10.0);
                g.draw(hor);
            }

            Line2D ver = new Line2D.Double(18.0, 16.0, 18.0, 4.0);
            g.draw(ver);

            Path2D shape = new Path2D.Float();
            shape.moveTo(13.447782, 17.801485);
            shape.lineTo(4.73615, 26.041084);
            shape.curveTo(4.73615, 26.041084, 2.9090462, 26.923565, 1.9954941, 26.041084);
            shape.curveTo(1.0819423, 25.158604, 1.9954941, 23.393635, 1.9954941, 23.393635);
            shape.lineTo(11.043547, 14.977204);

            g.setStroke(new BasicStroke(1.7017335f, CAP_BUTT, JOIN_MITER, 4));
            g.draw(shape);
        }
    }

}
