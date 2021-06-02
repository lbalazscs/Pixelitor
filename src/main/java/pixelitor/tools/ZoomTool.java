package pixelitor.tools;

import pixelitor.AppContext;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.gui.AutoZoom;
import pixelitor.gui.View;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.tools.DragTool;
import pixelitor.tools.DragToolState;
import pixelitor.tools.Tool;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PRectangle;
import pixelitor.utils.Cursors;
import pixelitor.utils.Shapes;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_MITER;
import static pixelitor.tools.DragToolState.*;

public class ZoomTool extends DragTool {

    private DragToolState state = NO_INTERACTION;
    private PRectangle box;

    public ZoomTool() { // Do I need this false in super call?
        super("Zoom", 'Z', "zoom_tool.png",
                "<b>click</b> to zoom in, " +
                        "<b>right-click</b> (or <b>Alt-click</b>) to zoom out." +
                        "<b>drag</b> to select an area.",
                Cursors.HAND, false);
        spaceDragStartPoint = true;
    }

    @Override
    public void initSettingsPanel() {
        if (AppContext.isDevelopment()) {
            settingsPanel.add(new JButton("Dump State") {{
                addActionListener(e -> {
                    System.out.println("ZoomTool::actionPerformed: canvas = " + OpenImages.getActiveView().getCanvas());
                    System.out.println("ZoomTool::initSettingsPanel: state = " + state);
                    System.out.println("ZoomTool::initSettingsPanel: dumpBox = " + box);
                });
            }});
        }
    }

    @Override
    public void mouseClicked(PMouseEvent e) {
        Point mousePos = e.getPoint();
        View view = e.getView();

        if (e.isRight() || (e.isLeft() && e.isAltDown())) {
            view.decreaseZoom(mousePos);
        } else {
            view.increaseZoom(mousePos);
        }
    }

    @Override
    public void dragStarted(PMouseEvent e) {
        if (state == NO_INTERACTION) {
            setState(INITIAL_DRAG);
        } else if (state == INITIAL_DRAG) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void ongoingDrag(PMouseEvent e) {
        e.repaint();
    }

    @Override
    public void dragFinished(PMouseEvent e) {
        if (state == NO_INTERACTION) {
            return;
        }

        if (state == INITIAL_DRAG) {
            if (box != null) {
                throw new IllegalStateException();
            }

            View view = e.getView();
            box = PRectangle.positiveFromCo(userDrag.toCoRect(), view);
            setState(TRANSFORM);

            executeZoomCommand(view);
            e.consume();
        }
    }

    @Override
    public void paintOverImage(Graphics2D g2, Composition comp) {
        if (state == NO_INTERACTION) {
            return;
        }
        PRectangle zoomRect = getZoomRect();
        if (zoomRect == null) {
            return;
        }

        Shapes.drawVisibly(g2, zoomRect.getCo());
    }

    /**
     * Returns the zoom rectangle
     */
    public PRectangle getZoomRect() {
        if (state == INITIAL_DRAG) {
            return userDrag.toPosPRect();
        } else if (state == TRANSFORM) {
            return box;
        }
        // initial state
        return null;
    }

    @Override
    protected void toolEnded() {
        super.toolEnded();
        resetInitialState();
    }

    @Override
    public void resetInitialState() {
        box = null;
        setState(NO_INTERACTION);

        OpenImages.repaintActive();
        OpenImages.setCursorForAll(Cursors.HAND);
    }

    private void setState(DragToolState newState) {
        state = newState;
    }

    @Override
    public void compReplaced(Composition newComp, boolean reloaded) {
        if (reloaded) {
            resetInitialState();
        }
    }

    @Override
    public void coCoordsChanged(View view) {
        if (box != null && state == TRANSFORM) {
            box.coCoordsChanged(view);
        }
    }

    @Override
    public void imCoordsChanged(AffineTransform at, Composition comp) {
        if (box != null && state == TRANSFORM) {
            box.imCoordsChanged(comp.getView(), at);
        }
    }

    private void executeZoomCommand(View view) {

        Rectangle2D zoomRect = getZoomRect().getIm();
        if (zoomRect.isEmpty()) {
            resetInitialState();
            return;
        }

        Canvas c = new Canvas((int) zoomRect.getWidth(), (int) zoomRect.getHeight());

        view.setZoom(ZoomLevel.calcZoom(c, AutoZoom.FIT_SPACE, true));

        view.scrollRectToVisible(getZoomRect().getCo());

        resetInitialState();
    }

    @Override
    public DebugNode createDebugNode() {
        var node = super.createDebugNode();
        node.addString("state", state.toString());
        return node;
    }

    @Override
    public Icon createIcon() {
        return new ZoomToolIcon();
    }

    private static class ZoomToolIcon extends Tool.ToolIcon {

        @Override
        public void paintIcon(Graphics2D g) {
            // based on zoom_tool.svg

            Ellipse2D circle = new Ellipse2D.Double(9, 1, 18, 18);
            g.setStroke(new BasicStroke(2.0f, CAP_BUTT, JOIN_MITER, 4));
            g.draw(circle);

            Line2D hor = new Line2D.Double(12.0, 10.0, 24.0, 10.0);
            g.draw(hor);

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
