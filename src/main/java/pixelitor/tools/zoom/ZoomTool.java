package pixelitor.tools.zoom;

import pixelitor.AppContext;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.AutoZoom;
import pixelitor.gui.View;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.guides.GuidesRenderer;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.tools.DragTool;
import pixelitor.tools.DragToolState;
import pixelitor.tools.Tool;
import pixelitor.tools.crop.CompositionGuide;
import pixelitor.tools.crop.CompositionGuideType;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PRectangle;
import pixelitor.utils.Cursors;
import pixelitor.utils.Messages;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.*;

import static java.awt.AlphaComposite.SRC_OVER;
import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_MITER;
import static java.awt.Color.BLACK;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.WEST;
import static pixelitor.tools.DragToolState.*;

public class ZoomTool extends DragTool {

    private final RangeParam maskOpacity = new RangeParam("Mask Opacity (%)", 0, 75, 100);
    private DragToolState state = NO_INTERACTION;
    private ZoomBox zoomBox;
    private Composite maskComposite = AlphaComposite.getInstance(SRC_OVER, maskOpacity.getPercentageValF());
    private JComboBox<CompositionGuideType> guidesCB;

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
        addMaskOpacitySelector();

        settingsPanel.addSeparator();
        addGuidesSelector();

        if (AppContext.isDevelopment()) {
            JButton b = new JButton("Dump State");
            b.addActionListener(e -> {
                View view = OpenImages.getActiveView();
                Canvas canvas = view.getCanvas();
                System.out.println("ZoomTool::actionPerformed: canvas = " + canvas);
                System.out.println("ZoomTool::initSettingsPanel: state = " + state);
                System.out.println("ZoomTool::initSettingsPanel: dumpBox = " + zoomBox);
            });
            settingsPanel.add(b);
        }
    }

    private void addMaskOpacitySelector() {
        maskOpacity.addChangeListener(e -> maskOpacityChanged());
        SliderSpinner maskOpacitySpinner = new SliderSpinner(
                maskOpacity, WEST, false);
        settingsPanel.add(maskOpacitySpinner);
    }

    private void maskOpacityChanged() {
        float alpha = maskOpacity.getPercentageValF();
        // because of a swing bug, the slider can get out of range?
        if (alpha < 0.0f) {
            System.out.printf("ZoomTool::maskOpacityChanged: alpha = %.2f%n", alpha);
            alpha = 0.0f;
            maskOpacity.setValue(0);
        } else if (alpha > 1.0f) {
            System.out.printf("ZoomTool::maskOpacityChanged: alpha = %.2f%n", alpha);
            alpha = 1.0f;
            maskOpacity.setValue(100);
        }
        maskComposite = AlphaComposite.getInstance(SRC_OVER, alpha);
        OpenImages.repaintActive();
    }

    private void addGuidesSelector() {
        guidesCB = GUIUtils.createComboBox(CompositionGuideType.values());
        guidesCB.setToolTipText("<html>Composition guides." +
                "<br><br>Press <b>O</b> to select the next guide." +
                "<br>Press <b>Shift-O</b> to change the orientation.");
        guidesCB.addActionListener(e -> OpenImages.repaintActive());
        settingsPanel.addComboBox("Guides:", guidesCB, "guidesCB");
    }

//    @Override
//    public void mouseClicked(PMouseEvent e) {
//        Point mousePos = e.getPoint();
//        View view = e.getView();
//        if (e.isLeft()) {
//            if (e.isAltDown()) {
//                view.decreaseZoom(mousePos);
//            } else {
//                view.increaseZoom(mousePos);
//            }
//        } else if (e.isRight()) {
//            view.decreaseZoom(mousePos);
//        }
//    }

    @Override
    public void dragStarted(PMouseEvent e) {
        if (state == NO_INTERACTION) {
            setState(INITIAL_DRAG);
        } else if (state == INITIAL_DRAG) {
            throw new IllegalStateException();
        }

        if (state == TRANSFORM) {
            assert zoomBox != null;
            zoomBox.mousePressed(e);
        }
    }

    @Override
    public void ongoingDrag(PMouseEvent e) {
        if (state == TRANSFORM) {
            zoomBox.mouseDragged(e);
        } else if (userDrag != null) {
            userDrag.setStartFromCenter(e.isAltDown());
            userDrag.setEquallySized(e.isShiftDown());
        }

        // in the USER_DRAG state this will also
        // cause the painting of the darkening overlay
        e.repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e, View view) {
        super.mouseMoved(e, view);
        if (state == TRANSFORM) {
            zoomBox.mouseMoved(e, view);
        }
    }

    @Override
    public void dragFinished(PMouseEvent e) {
        var comp = e.getComp();
        comp.update();

        switch (state) {
            case NO_INTERACTION:
                break;
            case INITIAL_DRAG:
                if (zoomBox != null) {
                    throw new IllegalStateException();
                }

                Rectangle r = userDrag.toCoRect();
                PRectangle rect = PRectangle.positiveFromCo(r, e.getView());

                zoomBox = new ZoomBox(rect, e.getView());

                setState(TRANSFORM);

                if (executeZoomCommand(e.getView())) {
                    e.consume();
                }
                break;
            case TRANSFORM:
                if (zoomBox == null) {
                    throw new IllegalStateException();
                }
                zoomBox.mouseReleased(e);
                break;
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

        paintDarkMask(g2, comp, zoomRect);

        if (state == TRANSFORM) {
            paintBox(g2, zoomRect);
        }
    }

    // Paint the semi-transparent dark area outside the zoom rectangle.
    // All calculations are in component space.
    private void paintDarkMask(Graphics2D g2, Composition comp, PRectangle zoomRect) {

        // The Swing clip ensures that we can't draw outside the component,
        // even if the canvas is outside of it (scrollbars)
        Shape origClip = g2.getClip();
        Color origColor = g2.getColor();
        Composite origComposite = g2.getComposite();

        View view = comp.getView();
        Rectangle2D coCanvasBounds = comp.getCanvas().getCoBounds(view);

        Rectangle2D visibleCanvasArea = coCanvasBounds.createIntersection(
                view.getVisiblePart());

        Path2D maskAreaClip = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        maskAreaClip.append(visibleCanvasArea, false);
        maskAreaClip.append(zoomRect.getCo(), false); // subtract the zoom rect

        g2.setColor(BLACK);
        g2.setComposite(maskComposite);
        g2.setClip(maskAreaClip);
        g2.fill(visibleCanvasArea);

        g2.setColor(origColor);
        g2.setComposite(origComposite);
        g2.setClip(origClip);
    }

    // Paint the handles and the guides.
    private void paintBox(Graphics2D g2, PRectangle zoomRect) {
        zoomBox.paint(g2);
    }

    /**
     * Returns the zoom rectangle
     */
    public PRectangle getZoomRect() {
        if (state == INITIAL_DRAG) {
            return userDrag.toPosPRect();
        } else if (state == TRANSFORM) {
            return zoomBox.getRect();
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
        zoomBox = null;
        setState(NO_INTERACTION);

        OpenImages.repaintActive();
        OpenImages.setCursorForAll(Cursors.DEFAULT);
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
        if (zoomBox != null && state == TRANSFORM) {
            zoomBox.coCoordsChanged(view);
        }
    }

    @Override
    public void imCoordsChanged(AffineTransform at, Composition comp) {
        if (zoomBox != null && state == TRANSFORM) {
            zoomBox.imCoordsChanged(at, comp);
        }
    }

    @Override
    public boolean arrowKeyPressed(ArrowKey key) {
        if (state == TRANSFORM) {
            View view = OpenImages.getActiveView();
            if (view != null) {
                zoomBox.arrowKeyPressed(key, view);
                return true;
            }
        }
        return false;
    }

    @Override
    public void escPressed() {
        executeCancelCommand();
    }

    // return true if a zoom was executed
    private boolean executeZoomCommand(View view) {

        Rectangle2D zoomRect = getZoomRect().getIm();
        if (zoomRect.isEmpty()) {
            return false;
        }

//        Rectangle bounds = OpenImages.getActiveComp().getCanvas().getBounds();
//bounds = view.imageToComponentSpace(bounds);

        Canvas c = new Canvas((int) zoomRect.getWidth(), (int) zoomRect.getHeight());

//        ZoomLevel.Z18;
//        view.setZoom(ZoomLevel.valueOf());
        view.setZoom(ZoomLevel.calcZoom(c, AutoZoom.FIT_SPACE, true));
        view.scrollRectToVisible(getZoomRect().getCo());

        resetInitialState();
        return true;
    }

    private void executeCancelCommand() {
        if (state != TRANSFORM) {
            return;
        }

        resetInitialState();
        Messages.showPlainInStatusBar("Zoom canceled.");
    }

    @Override
    public void otherKeyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
//            if (executeZoomCommand()) { // TODO
//                e.consume();
//            }
            // otherwise the Enter might be used elsewhere,
            // for example by the layer name editor
        } else if (e.getKeyCode() == KeyEvent.VK_O) {
            if (e.isControlDown()) {
                // ignore Ctrl-O see issue #81
                return;
            }
            if (e.isShiftDown()) {
                // Shift-O: change the orientation
                // within the current composition guide family
                if (state == TRANSFORM) {
                    OpenImages.repaintActive();
                    e.consume();
                }
            } else {
                // O: advance to the next composition guide
                selectTheNextCompositionGuide();
                e.consume();
            }
        }
    }

    private void selectTheNextCompositionGuide() {
        int index = guidesCB.getSelectedIndex();
        int itemCount = guidesCB.getItemCount();
        int nextIndex;
        if (index == itemCount - 1) {
            nextIndex = 0;
        } else {
            nextIndex = index + 1;
        }
        guidesCB.setSelectedIndex(nextIndex);
    }

    @Override
    public DebugNode createDebugNode() {
        var node = super.createDebugNode();

        node.addFloat("mask opacity", maskOpacity.getPercentageValF());
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
