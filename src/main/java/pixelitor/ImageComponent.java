/*
 * Copyright 2015 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor;

import org.jdesktop.swingx.painter.CheckerboardPainter;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerButton;
import pixelitor.layers.LayersContainer;
import pixelitor.layers.LayersPanel;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.utils.Dialogs;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.debug.ImageComponentNode;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;

/**
 * The GUI component that shows a composition
 */
public class ImageComponent extends JComponent implements MouseListener, MouseMotionListener, ImageDisplay {
    private double viewScale = 1.0f;
    private final Canvas canvas;
    private ZoomLevel zoomLevel = ZoomLevel.Z100;

    private InternalImageFrame internalFrame = null;

    private static final Color BG_GRAY = new Color(200, 200, 200);
    private static final CheckerboardPainter checkerBoardPainter = new CheckerboardPainter(BG_GRAY, Color.WHITE);

    private final LayersPanel layersPanel;

    private final Composition comp;

    private boolean layerMaskEditing = false;

    // the start of the image if the ImageComponent is resized to bigger
    // than the canvas, and the image needs to be centralized
    double drawStartX;
    double drawStartY;

    public ImageComponent(Composition comp) {
        assert comp != null;

        this.comp = comp;
        this.canvas = comp.getCanvas();
        comp.setImageComponent(this);

        setupFitScreenZoomSize(canvas.getWidth(), canvas.getHeight(), false);

        layersPanel = new LayersPanel();

        addListeners();
    }

    private void addListeners() {
        addMouseListener(this);
        addMouseMotionListener(this);

        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.isControlDown()) {
                    int x = e.getX();
                    int y = e.getY();
                    if (e.getWheelRotation() < 0) { // up, away from the user
                        increaseZoom(x, y);
                    } else {  // down, towards the user
                        decreaseZoom(x, y);
                    }
                }
            }
        });

        // make sure that the image is drawn at the middle
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateDrawStart();

                if (Tools.getCurrentTool() == Tools.CROP) {
                    Tools.CROP.imageComponentResized(ImageComponent.this);
                }
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        if (comp.isEmpty()) {
            return super.getPreferredSize();
        } else {
            return canvas.getZoomedSize();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Tools.getCurrentTool().mouseClicked(e, this);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
//        mouseEntered is never used in the tools
    }

    @Override
    public void mouseExited(MouseEvent e) {
//        mouseExited is never used in the tools
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Tools.getCurrentTool().mousePressed(e, this);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Tools.getCurrentTool().mouseReleased(e, this);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Tools.getCurrentTool().mouseDragged(e, this);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Tools.getCurrentTool().mouseMoved(e, this);
    }

    @Override
    public String toString() {
        ImageComponentNode node = new ImageComponentNode("ImageComponent", this);
        return node.toDetailedString();
    }

    public void setInternalFrame(InternalImageFrame internalFrame) {
        this.internalFrame = internalFrame;
    }

    public InternalImageFrame getInternalFrame() {
        return internalFrame;
    }

    public void close() {
        if (internalFrame != null) {
            // this will also cause the calling of AppLogic.imageClosed via
            // InternalImageFrame.internalFrameClosed
            internalFrame.dispose();
        }
        comp.dispose();
    }

    public void onActivation() {
        try {
            getInternalFrame().setSelected(true);
        } catch (PropertyVetoException e) {
            Dialogs.showExceptionDialog(e);
        }
        LayersContainer.showLayersPanel(layersPanel);
    }

    @Override
    public double getViewScale() {
        return viewScale;
    }

    @Override
    public void updateTitle() {
        if (internalFrame != null) {
            String frameTitle = createFrameTitle();
            internalFrame.setTitle(frameTitle);
        }
    }

    public String createFrameTitle() {
        return comp.getName() + " - " + zoomLevel.toString();
    }

    public ZoomLevel getZoomLevel() {
        return zoomLevel;
    }

    @Override
    public void deleteLayerButton(LayerButton button) {
        layersPanel.deleteLayerButton(button);
    }

    @Override
    public Composition getComp() {
        return comp;
    }

    @Override
    public void changeLayerOrderInTheGUI(int oldIndex, int newIndex) {
        layersPanel.changeLayerOrderInTheGUI(oldIndex, newIndex);
    }

    @Override
    public void paint(Graphics g) {
        try {
//            long startTime = System.nanoTime();

            // no borders, no children, double-buffering is happening
            // in the parent
            paintComponent(g);

//            double estimatedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
//            System.out.println(String.format("ImageComponent::paint: estimatedSeconds = '%.2f'", estimatedSeconds));
        } catch (OutOfMemoryError e) {
            Dialogs.showOutOfMemoryDialog(e);
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        int zoomedWidth = canvas.getZoomedWidth();
        int zoomedHeight = canvas.getZoomedHeight();

        adjustClipBoundsForImage(g, drawStartX, drawStartY, zoomedWidth, zoomedHeight);

        AffineTransform unscaledTransform = g2.getTransform(); // a copy of the transform object

        g2.translate(drawStartX, drawStartY);

        if (!layerMaskEditing) {
            checkerBoardPainter.paint(g2, this, zoomedWidth, zoomedHeight);
        }

        g2.scale(viewScale, viewScale);

        BufferedImage drawnImage;
        if (layerMaskEditing) {
            drawnImage = comp.getActiveLayer().getLayerMask().getBwImage();
        } else {
            drawnImage = comp.getCompositeImage();
        }

//        g2.drawImage(compositeImage, 0, 0, null);
        ImageUtils.drawImageWithClipping(g2, drawnImage);

        // possibly allow a larger clip for the selections and tools
        Tool currentTool = Tools.getCurrentTool();
        currentTool.setClip(g2);

        comp.paintSelection(g2);

        currentTool.paintOverImage(g2, canvas, this, unscaledTransform);

        // restore original transform - is this necessary?
        g2.setTransform(unscaledTransform);
    }

    /**
     * Makes sure that not the whole area is repainted, only the image
     */
    private static void adjustClipBoundsForImage(Graphics g, double drawStartX, double drawStartY, int maxWidth, int maxHeight) {
        Rectangle clipBounds = g.getClipBounds();
        Rectangle imageRect = new Rectangle((int) drawStartX, (int) drawStartY, maxWidth, maxHeight);
        clipBounds = clipBounds.intersection(imageRect);

        g.setClip(clipBounds);
    }

    /**
     * Repaints only a region of the image, called from the brush tools
     */
    @Override
    public void updateRegion(int startX, int startY, int endX, int endY, int thickness) {
        double diff = viewScale - 1.0f;
        if (diff > 0.0001f || diff < -0.0001f) { // not the 100% view - avoids testing for floating point equality
            startX = (int) (drawStartX + viewScale * startX);
            startY = (int) (drawStartY + viewScale * startY);
            endX = (int) (drawStartX + viewScale * endX);
            endY = (int) (drawStartY + viewScale * endY);
            thickness = (int) (viewScale * thickness);
        } else { // drawStartX drawStartY has to be adjusted anyway
            startX = (int) (drawStartX + startX);
            startY = (int) (drawStartY + startY);
            endX = (int) (drawStartX + endX);
            endY = (int) (drawStartY + endY);
        }

        if (endX < startX) {
            int tmp = startX;
            startX = endX;
            endX = tmp;
        }
        if (endY < startY) {
            int tmp = startY;
            startY = endY;
            endY = tmp;
        }
        startX -= thickness;
        endX += thickness;
        startY -= thickness;
        endY += thickness;

        int repWidth = endX - startX;
        int repHeight = endY - startY;

        repaint(startX, startY, repWidth, repHeight);
    }

    public void makeSureItIsVisible() {
        if (internalFrame != null) {
            internalFrame.makeSureItIsVisible();
        }
    }

    public void setLayerMaskEditing(boolean layerMaskEditing) {
        this.layerMaskEditing = layerMaskEditing;
        repaint();
    }

    @Override
    public void canvasSizeChanged() {
        assert ConsistencyChecks.translationCheck(comp);

        if (internalFrame != null) {
            internalFrame.setSize(canvas.getZoomedWidth(), canvas.getZoomedHeight(), -1, -1);
        }
        revalidate();
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public void setupFitScreenZoomSize() {
        BufferedImage image = comp.getCompositeImage();
        int width = image.getWidth();
        int height = image.getHeight();
        setupFitScreenZoomSize(width, height, true);
    }

    public void setupFitScreenZoomSize(int imageWidth, int imageHeight, boolean alsoZoomInToFitScreen) {
        Dimension desktopSize = PixelitorWindow.getInstance().getDesktopSize();
        double desktopWidth = desktopSize.getWidth();
        double desktopHeight = desktopSize.getHeight();

        double imageToDesktopHorizontalRatio = imageWidth / desktopWidth;
        double imageToDesktopVerticalRatio = imageHeight / (desktopHeight - 35); // subtract because of internal frame header
        double maxImageToDesktopRatio = Math.max(imageToDesktopHorizontalRatio, imageToDesktopVerticalRatio);
        double idealZoomPercent = 100.0 / maxImageToDesktopRatio;
        ZoomLevel[] zoomLevels = ZoomLevel.values();
        ZoomLevel maximallyZoomedOut = zoomLevels[0];

        if (maximallyZoomedOut.getPercentValue() > idealZoomPercent) {
            // the image is so big that it will have scroll bars even if it is maximally zoomed out
            setZoom(maximallyZoomedOut, true);
            return;
        }

        ZoomLevel lastOK = maximallyZoomedOut;
        // iterate all the zoom levels from zoomed out to zoomed in
        for (ZoomLevel level : zoomLevels) {
            if (level.getPercentValue() > idealZoomPercent) {
                // found one that is too much zoomed in
                setZoom(lastOK, true);
                return;
            }
            if (!alsoZoomInToFitScreen) { // we don't want to zoom in more than 100%
                if (lastOK == ZoomLevel.Z100) {
                    setZoom(lastOK, true);
                    return;
                }
            }
            lastOK = level;
        }
        // if we get here, it means that the image is so small that even at maximal zoom
        // it fits the screen, set it then to the maximal zoom
        setZoom(lastOK, true);
    }

    /**
     * @return true if there was a change in zoom
     */
    public boolean setZoom(ZoomLevel newZoomLevel, boolean settingTheInitialSize) {
        if (this.zoomLevel == newZoomLevel && !settingTheInitialSize) {
            return false;
        }

        this.zoomLevel = newZoomLevel;

        viewScale = newZoomLevel.getViewScale();

        canvas.updateForZoom(viewScale);

        if (internalFrame != null) {
            updateTitle();
            internalFrame.setSize(canvas.getZoomedWidth(), canvas.getZoomedHeight(), -1, -1);
        }

        revalidate();

//        if(!settingTheInitialSize) {

        // we are already on the EDT, but we want to call this code
        // only after all pending AWT events have been processed
        // because then this component will have the final size
        // and updateDrawStart can calculate correct results
        Runnable r = new Runnable() {
            @Override
            public void run() {
                updateDrawStart();
                repaint();
            }
        };
        SwingUtilities.invokeLater(r);

//        }

        zoomLevel.getMenuItem().setSelected(true);
        return true;
    }

    @Override
    public void increaseZoom(int mouseX, int mouseY) {
        ZoomLevel oldZoom = zoomLevel;
        ZoomLevel newZoom = zoomLevel.zoomIn();
        if (setZoom(newZoom, false)) {
            zoomToPoint(mouseX, mouseY, oldZoom, newZoom);
        }
    }

    @Override
    public void decreaseZoom(int mouseX, int mouseY) {
        ZoomLevel oldZoom = zoomLevel;
        ZoomLevel newZoom = zoomLevel.zoomOut();
        if (setZoom(newZoom, false)) {
            zoomToPoint(mouseX, mouseY, oldZoom, newZoom);
        }
    }

    private void zoomToPoint(int mouseX, int mouseY, ZoomLevel oldZoom, ZoomLevel newZoom) {
        // the x, y coordinates were generated BEFORE the zooming
        // so we need to find the corresponding coordinates after zooming
        Point imageSpacePoint = fromComponentToImageSpace(new Point(mouseX, mouseY), oldZoom);
        Point newComponentSpacePoint = fromImageToComponentSpace(imageSpacePoint, newZoom);

        Rectangle viewRect = getViewRectangle();

        final Rectangle areaThatShouldBeVisible = new Rectangle(
                newComponentSpacePoint.x - viewRect.width / 2,
                newComponentSpacePoint.y - viewRect.height / 2,
                viewRect.width,
                viewRect.height
        );

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                scrollRectToVisible(areaThatShouldBeVisible);
//                updateDrawStart();
                repaint();
            }
        });
    }

    public void updateDrawStart() {
        int width = getWidth();
        int canvasZoomedWidth = canvas.getZoomedWidth();
        int height = getHeight();
        int canvasZoomedHeight = canvas.getZoomedHeight();

        drawStartX = (width - canvasZoomedWidth) / 2.0;
        drawStartY = (height - canvasZoomedHeight) / 2.0;
    }

    @Override
    public int componentXToImageSpace(int mouseX) {
        return (int) ((mouseX - drawStartX) / viewScale);
    }

    @Override
    public int componentYToImageSpace(int mouseY) {
        return (int) ((mouseY - drawStartY) / viewScale);
    }

    public int imageXToComponentSpace(int mouseX) {
        return (int) (drawStartX + mouseX * viewScale);
    }

    public int imageYToComponentSpace(int mouseY) {
        return (int) (drawStartY + mouseY * viewScale);
    }

    public Point fromComponentToImageSpace(Point input, ZoomLevel zoom) {
        double zoomViewScale = zoom.getViewScale();
        return new Point(
                (int) ((input.x - drawStartX) / zoomViewScale),
                (int) ((input.y - drawStartY) / zoomViewScale)
        );
    }

    public Point fromImageToComponentSpace(Point input, ZoomLevel zoom) {
        double zoomViewScale = zoom.getViewScale();
        return new Point(
                (int) (drawStartX + input.x * zoomViewScale),
                (int) (drawStartY + input.y * zoomViewScale)
        );
    }

    @Override
    public Rectangle fromComponentToImageSpace(Rectangle input) {
        return new Rectangle(
                componentXToImageSpace(input.x),
                componentYToImageSpace(input.y),
                (int) (input.width / viewScale),
                (int) (input.height / viewScale)
        );
    }

    @Override
    public Rectangle fromImageToComponentSpace(Rectangle input) {
        return new Rectangle(
                imageXToComponentSpace(input.x),
                imageYToComponentSpace(input.y),
                (int) (input.width * viewScale),
                (int) (input.height * viewScale)
        );
    }

    // TODO untested
    public AffineTransform getImageToComponentTransform() {
        AffineTransform t = new AffineTransform();
        t.translate(drawStartX, drawStartY);
        t.scale(viewScale, viewScale);
        return t;
    }

    // TODO untested
    public AffineTransform getComponentToImageTransform() {
        AffineTransform inverse = null;
        try {
            inverse = getImageToComponentTransform().createInverse();
        } catch (NoninvertibleTransformException e) {
            // should not happen
            e.printStackTrace();
        }
        return inverse;
    }

    /**
     * Returns how much of this ImageComponent is currently visible considering that
     * the JScrollPane might show only a part of it
     */
    @Override
    public Rectangle getViewRectangle() {
        return internalFrame.getScrollPane().getViewport().getViewRect();
    }

    @Override
    public void addLayerToGUI(Layer newLayer, int newLayerIndex) {
        LayerButton layerButton = newLayer.getLayerButton();
        layersPanel.addLayerButton(layerButton, newLayerIndex);

        if (ImageComponents.isActive(this)) {
            AppLogic.activeCompLayerCountChanged(comp, comp.getNrLayers());
        }
    }

}
