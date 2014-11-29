/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor;

import org.jdesktop.swingx.painter.CheckerboardPainter;
import pixelitor.layers.ImageLayer;
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
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;
import java.io.File;

/**
 * The GUI component that shows a composition
 */
public class ImageComponent extends JComponent implements MouseListener, MouseMotionListener {
    private double viewScale = 1.0f;
    private Canvas canvas;
    private ZoomLevel zoomLevel = ZoomLevel.Z100;

    private InternalImageFrame internalFrame = null;

    private static final Color BG_GRAY = new Color(200, 200, 200);
    private static final CheckerboardPainter checkerBoardPainter = new CheckerboardPainter(BG_GRAY, Color.WHITE);

    private LayersPanel layersPanel;

    private final Composition comp;

    private boolean layerMaskEditing = false;

    // the start of the image if the ImageComponent is resized to bigger
    // than the canvas, and the image needs to be centralized
    double drawStartX;
    double drawStartY;

    /**
     * Called when a regular file (jpeg, png, etc.) is opened or when
     * a new composition is created or something is pasted
     * If the file argument is not null, then the name argument is ignored
     */
    public ImageComponent(File file, String name, BufferedImage baseLayerImage) {
        if (baseLayerImage == null) {
            throw new IllegalArgumentException("baseLayerImage is null");
        }

        int width = baseLayerImage.getWidth();
        int height = baseLayerImage.getHeight();
        canvas = new Canvas(this, width, height);
        comp = new Composition(this, file, name, canvas);

        init(width, height);
    }

    /**
     * Called when a Composition is deserialized or an OpenRaster file is opened
     */
    public ImageComponent(File file, Composition comp) {
        this.comp = comp;
        comp.setImageComponent(this);

        // file is transient in Composition because the pxc file can be renamed
        comp.setFile(file);

        canvas = comp.getCanvas();
        canvas.setIc(this);

        init(canvas.getWidth(), canvas.getHeight());
    }

    /**
     * This method is not called from the constructor so that the active ImageComponent can be set before calling this
     */
    public void addBaseLayer(BufferedImage baseLayerImage) {
        canvas.updateSize(baseLayerImage.getWidth(), baseLayerImage.getHeight());
        ImageLayer newLayer = new ImageLayer(comp, baseLayerImage, null);

        comp.addLayer(newLayer, false, true, false);
    }

    private void init(int width, int height) {
        setupFitScreenZoomSize(width, height, false);

        addMouseListener(this);
        addMouseMotionListener(this);

        layersPanel = new LayersPanel();

        // make sure that the image is drawn at the middle
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
//                double previousDrawStartX = drawStartX;
//                double previousDrawStartY = drawStartY;

                updateDrawStart();

//                double deltaDrawStartX = drawStartX - previousDrawStartX;
//                double deltaDrawStartY = drawStartY - previousDrawStartY;
                if(Tools.getCurrentTool() == Tools.CROP) {
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

    public double getViewScale() {
        return viewScale;
    }

    public void setInternalFrameTitle() {
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

    public void addLayerButton(LayerButton layerButton, int newLayerIndex) {
        layersPanel.addLayerButton(layerButton, newLayerIndex);
    }

    public void deleteLayerButton(LayerButton button) {
        layersPanel.deleteLayerButton(button);
    }

    public Composition getComp() {
        return comp;
    }

    public void changeLayerOrderInTheGUI(int oldIndex, int newIndex) {
        layersPanel.changeLayerOrder(oldIndex, newIndex);
    }

    @Override
    public void paint(Graphics g) {
        try {
            paintComponent(g);  // no borders, no children - but TODO consider double-buffering
        } catch (OutOfMemoryError e) {
            ExceptionHandler.showOutOfMemoryDialog();
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
    private static boolean adjustClipBoundsForImage(Graphics g, double drawStartX, double drawStartY, int maxWidth, int maxHeight) {
        Rectangle clipBounds = g.getClipBounds();
        Rectangle imageRect = new Rectangle((int)drawStartX, (int)drawStartY, maxWidth, maxHeight);

        clipBounds = clipBounds.intersection(imageRect);
        g.setClip(clipBounds);
        return false;
    }

    /**
     * Repaints only a region of the image, called from the brush tools
     */
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

    public void canvasSizeChanged() {
        assert ConsistencyChecks.translationCheck(comp);

        if (internalFrame != null) {
            internalFrame.setNewSize(canvas.getZoomedWidth(), canvas.getZoomedHeight(), -1, -1);
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

        double imageToDesktopHorizontalRatio = imageWidth/ desktopWidth;
        double imageToDesktopVerticalRatio = imageHeight / (desktopHeight - 35); // subtract because of internal frame header
        double maxImageToDesktopRatio = Math.max(imageToDesktopHorizontalRatio, imageToDesktopVerticalRatio);

        // iterate all the zoom levels until it finds the best one
        ZoomLevel bestZoom = ZoomLevel.Z100;
        ZoomLevel[] zoomLevels = ZoomLevel.values();
        for (ZoomLevel level : zoomLevels) {
            double inverseZoomRatio = 100.0 / level.getPercentValue();

            if (!alsoZoomInToFitScreen && inverseZoomRatio < 0.99) {
                // if we do not want to zoom in and we already passed the 100% zoom
                break;
            }

            if (maxImageToDesktopRatio < inverseZoomRatio) {
                bestZoom = level;
            }
        }

        // evenIfThereIsNoChange is set here to true  because
        // if layered images are dropped, this is where their size is set
        setZoom(bestZoom, true);
    }

    /**
     * @return true if there was a change in zoom
     */
    public boolean setZoom(ZoomLevel newZoomLevel, boolean evenIfThereIsNoChange) {
        if (this.zoomLevel == newZoomLevel && !evenIfThereIsNoChange) {
            return false;
        }

        this.zoomLevel = newZoomLevel;

        viewScale = newZoomLevel.getViewScale();

        canvas.updateForZoom(viewScale);

        if (internalFrame != null) {
            setInternalFrameTitle();
            internalFrame.setNewSize(canvas.getZoomedWidth(), canvas.getZoomedHeight(), -1, -1);
        }
        revalidate();
        super.repaint();
        zoomLevel.getMenuItem().setSelected(true);
        return true;
    }

    public void increaseZoom(int mouseX, int mouseY) {
        ZoomLevel oldZoom = zoomLevel;
        ZoomLevel newZoom = zoomLevel.getNext();
        if (setZoom(newZoom, false)) {
            zoomToPoint(mouseX, mouseY, oldZoom, newZoom);
        }
    }

    public void decreaseZoom(int mouseX, int mouseY) {
        ZoomLevel oldZoom = zoomLevel;
        ZoomLevel newZoom = zoomLevel.getPrevious();
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
        drawStartX = (getWidth() - canvas.getZoomedWidth())/2.0;
        drawStartY = (getHeight() - canvas.getZoomedHeight())/2.0;
    }

    public int componentXToImageSpace(int mouseX) {
        return (int) ((mouseX - drawStartX) / viewScale);
    }

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
                (int) (drawStartX + input.x  * zoomViewScale),
                (int) (drawStartY + input.y * zoomViewScale)
        );
    }

    public Rectangle fromComponentToImageSpace(Rectangle input) {
        return new Rectangle(
                componentXToImageSpace(input.x),
                componentYToImageSpace(input.y),
                (int)(input.width / viewScale),
                (int)(input.height / viewScale)
        );
    }

    public Rectangle fromImageToComponentSpace(Rectangle input) {
        return new Rectangle(
                imageXToComponentSpace(input.x),
                imageYToComponentSpace(input.y),
                (int)(input.width * viewScale),
                (int)(input.height * viewScale)
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
    public Rectangle getViewRectangle() {
        return internalFrame.getScrollPane().getViewport().getViewRect();
    }

}
