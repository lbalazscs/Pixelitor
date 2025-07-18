/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.gui;

import org.jdesktop.swingx.painter.CheckerboardPainter;
import pixelitor.Canvas;
import pixelitor.*;
import pixelitor.gui.utils.Dialogs;
import pixelitor.history.CompositionReplacedEdit;
import pixelitor.history.History;
import pixelitor.io.FileIO;
import pixelitor.io.IOTasks;
import pixelitor.layers.*;
import pixelitor.menus.view.ZoomControl;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.selection.SelectionActions;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.util.PPoint;
import pixelitor.tools.util.PRectangle;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Lazy;
import pixelitor.utils.Messages;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.Debuggable;
import pixelitor.utils.test.Assertions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.concurrent.CompletableFuture;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static pixelitor.utils.Threads.callInfo;
import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.onEDT;

/**
 * The GUI component that shows a {@link Composition} inside a {@link ViewContainer}.
 */
public class View extends JComponent implements MouseListener, MouseMotionListener, Debuggable {
    private Composition comp;
    private Canvas canvas;

    private ZoomLevel zoomLevel = ZoomLevel.ACTUAL_SIZE;
    private double zoomScale = 1.0f;

    private ViewContainer viewContainer = null;
    private final LayersPanel layersPanel;
    private Navigator navigator;

    private MaskViewMode maskViewMode;

    private static final CheckerboardPainter checkerBoardPainter
        = ImageUtils.createCheckerboardPainter();

    // Coordinates of the canvas origin within the view (for centering).
    // They can't have floating-point precision, otherwise the checkerboard
    // and the image might be painted on slightly different coordinates.
    private int canvasStartX;
    private int canvasStartY;

    // cached coordinate transformation matrices
    private final Lazy<AffineTransform> imToCo = Lazy.of(this::createImToCoTransform);
    private final Lazy<AffineTransform> coToIm = Lazy.of(this::createCoToImTransform);

    private static boolean pixelGridVisible = false;

    // true if the snapping preference is set and the tool also approves
    private static boolean pixelSnapping = false;

    public View(Composition comp) {
        assert !AppMode.isUnitTesting() : "Swing component in unit test";
        assert comp != null;

        setComp(comp);
        setZoom(ZoomLevel.calcBestFitZoom(canvas, null, false));

        layersPanel = new LayersPanel();
        addMouseListeners();
    }

    private void addMouseListeners() {
        addMouseListener(this);
        addMouseMotionListener(this);

        MouseZoomMethod.ACTIVE.installOnView(this);
    }

    private void setComp(Composition comp) {
        assert comp != null;
        assert comp.getView() == null;

        this.comp = comp;
        this.canvas = comp.getCanvas();

        comp.setView(this);
    }

    public boolean isActive() {
        return Views.getActive() == this;
    }

    public CompletableFuture<Composition> checkForExternalModifications() {
        return comp.checkForExternalModifications();
    }

    /**
     * Initiates an asynchronous reload of the composition from its associated file.
     */
    public CompletableFuture<Composition> reloadCompAsync() {
        assert isActive();

        File file = comp.getFile();
        if (file == null) {
            Messages.showError("Cannot Reload", String.format(
                "<html>The image <b>%s</b> can't be reloaded because it wasn't yet saved.",
                comp.getName()));
            return CompletableFuture.completedFuture(null);
        }

        String filePath = file.getAbsolutePath();
        if (!file.exists()) {
            Messages.showError("File Not Found", String.format(
                "<html>Cannot reload <b>%s</b> because the file" +
                    "<br><b>%s</b>" +
                    "<br>no longer exists.",
                comp.getName(), filePath), getDialogParent());
            return CompletableFuture.completedFuture(null);
        }

        // prevent concurrent reloads of the same file
        if (IOTasks.isPathProcessing(filePath)) {
            Messages.showInfo("Reload Busy", "The file " + file.getName() + " is currently being accessed.");
            return CompletableFuture.completedFuture(null);
        }
        IOTasks.markPathForReading(filePath);

        return FileIO.loadCompAsync(file)
            .thenApplyAsync(this::handleReloadedComp, onEDT)
            .whenComplete((composition, exception) -> {
                IOTasks.markReadingComplete(filePath);
                FileIO.handleFileReadErrors(exception);
            });
    }

    /**
     * Handles a successfully reloaded composition by replacing the current one.
     */
    private Composition handleReloadedComp(Composition newComp) {
        assert calledOnEDT() : callInfo();
        assert newComp != comp;
        assert !newComp.hasSelection();

        if (comp.isSmartObjectContent()) {
            // If the old composition was smart object content, re-link the
            // owners to the new one. Owner is a transient field in Composition,
            // so this must be done even when reloading from a PXC file.
            for (SmartObject owner : comp.getOwners()) {
                owner.setContent(newComp);
            }
        }
        comp.closeAllNestedComps();

        // do this before actually replacing so that the old comp is
        // deselected before its view is set to null
        History.add(new CompositionReplacedEdit(
            "Reload", this, comp, newComp, null, true));
        replaceComp(newComp, MaskViewMode.NORMAL, true);

        // the view was active when the reloading started, but since
        // the reloading was asynchronous, this could have changed
        if (isActive()) {
            SelectionActions.update(newComp);
        }

        Messages.showStatusMessage(String.format(
            "The image <b>%s</b> was reloaded from the file <b>%s</b>.",
            newComp.getName(), newComp.getFile().getAbsolutePath()));

        return newComp;
    }

    // the simple form of replacing, used by multi-layer edits
    public void replaceComp(Composition newComp) {
        replaceComp(newComp, getMaskViewMode(), false);
    }

    public void replaceComp(Composition newComp,
                            MaskViewMode newMaskViewMode,
                            boolean reloaded) {
        assert newComp != null;

        Composition oldComp = comp;
        setComp(newComp);
        oldComp.dispose();

        newComp.createLayerUIs();

        // Evaluates all smart objects. It's better to do it here than
        // have it triggered by async updateIconImage calls when the
        // layers are added to the GUI, which could trigger multiple
        // parallel evaluations of a smart object.
        newComp.getCompositeImage();

        newComp.addLayersToUI();

        if (isActive()) {
            updateUIForNewComp(newComp, newMaskViewMode, reloaded);
        }

        if (newComp.isSmartObjectContent()) {
            for (SmartObject owner : newComp.getOwners()) {
                owner.propagateContentChanges(newComp, true);
            }
        }

        revalidate(); // update the scrollbars if the new comp has a different size
        canvasCoSizeChanged();
        repaint();
    }

    /**
     * Updates all UI elements when a new composition becomes active in this view.
     */
    private void updateUIForNewComp(Composition newComp, MaskViewMode newMaskViewMode, boolean reloaded) {
        LayersContainer.showLayersFor(this);

        Layers.activeCompChanged(newComp, reloaded);

        // is this needed?
        newMaskViewMode.activate(this, newComp.getActiveLayer());

        repaintNavigator(true);
        HistogramsPanel.updateFrom(newComp);
        PixelitorWindow.get().updateTitle(newComp);
        Tools.compReplaced(newComp, reloaded);
    }

    @Override
    public Dimension getPreferredSize() {
        if (comp.hasNoLayers()) {
            return super.getPreferredSize();
        } else {
            return canvas.getCoSize();
        }
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Tools.EventDispatcher.mouseClicked(e, this);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        Tools.EventDispatcher.mouseEntered(e, this);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        Tools.EventDispatcher.mouseExited(e, this);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Tools.EventDispatcher.mousePressed(e, this);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Tools.EventDispatcher.mouseReleased(e, this);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Tools.EventDispatcher.mouseDragged(e, this);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Tools.EventDispatcher.mouseMoved(e, this);
    }

    public void setViewContainer(ViewContainer container) {
        viewContainer = container;
        updateContainerSize();
    }

    public ViewContainer getViewContainer() {
        return viewContainer;
    }

    public void close() {
        if (viewContainer != null) {
            // this will also cause the calling of Views.viewClosed via
            // ImageFrame.internalFrameClosed
            viewContainer.close();
        }
    }

    public void showLayersUI() {
        LayersContainer.showLayersFor(this);
    }

    public void updateTitle() {
        if (viewContainer != null) {
            viewContainer.updateTitle(this);
        }
    }

    // used only by the frames ui
    public String createTitleWithZoom() {
        return comp.getName() + " - " + zoomLevel;
    }

    /**
     * Removes a specific layer's UI representation from the layers panel.
     */
    public void removeLayerUI(LayerUI ui) {
        layersPanel.removeLayerGUI((LayerGUI) ui);
    }

    /**
     * Reorders a top-level layer's UI representation in the layers panel.
     */
    public void reorderLayerUI(int oldIndex, int newIndex) {
        layersPanel.reorderLayer(oldIndex, newIndex);
    }

    /**
     * Notifies the LayersPanel to update the size of its thumbnails.
     */
    public void updateThumbSize(int newThumbSize) {
        layersPanel.updateThumbSize(newThumbSize);
        comp.updateAllIconImages();
    }

    public Composition getComp() {
        return comp;
    }

    @Override
    public String getName() {
        return comp.getName();
    }

    @Override
    public void paint(Graphics g) {
        try {
            // no borders, no children, double-buffering is happening
            // in the parent
            paintComponent(g);
        } catch (OutOfMemoryError e) {
            Dialogs.showOutOfMemoryDialog(e);
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        // save current transform (component space)
        var componentTransform = g2.getTransform();

        // apply canvas offset
        g2.translate(canvasStartX, canvasStartY);

        // paint zoom-independent checkerboard pattern
        boolean showMask = maskViewMode.showMask();
        if (!showMask) {
            checkerBoardPainter.paint(g2, this,
                canvas.getCoWidth(), canvas.getCoHeight());
        }

        // apply canvas zoom
        g2.scale(zoomScale, zoomScale);

        // after the translation and scaling, we are in image space

        if (showMask) {
            LayerMask mask = comp.getActiveLayer().getMask();
            assert mask != null : "no mask in " + maskViewMode;
            mask.paint(g2, true);
        } else {
            g2.drawImage(comp.getCompositeImage(), 0, 0, null);

            if (maskViewMode.showRubylith()) {
                LayerMask mask = comp.getActiveLayer().getMask();
                assert mask != null : "no mask in " + maskViewMode;
                mask.paintAsRubylith(g2);
            }
        }

        comp.paintSelection(g2);

        g2.setTransform(componentTransform);
        // now we are back in "component space"

        paintOverlays(g2);
    }

    /**
     * Paints overlays that appear on top of the image content.
     */
    private void paintOverlays(Graphics2D g2) {
        if (pixelGridVisible && allowPixelGrid()) {
            drawPixelGrid(g2);
        }

        comp.drawGuides(g2);

        if (isActive()) {
            Tools.getActive().paintOverCanvas(g2, comp);
        }
    }

    /**
     * Draws the pixel grid lines in component space.
     */
    private void drawPixelGrid(Graphics2D g2) {
        // use XOR mode for visibility on any background
        g2.setColor(WHITE);
        g2.setXORMode(BLACK);

        double pixelSize = zoomScale;
        assert pixelSize > 1;

        Rectangle visibleRect = getVisibleRegion();

        double startX = canvasStartX;
        if (visibleRect.x > 0) {
            startX += Math.floor(visibleRect.x / pixelSize) * pixelSize;
        }

        double endX = startX + Math.min(
            visibleRect.width + pixelSize, canvas.getCoWidth()) - 1;

        double startY = canvasStartY;
        if (visibleRect.y > 0) {
            startY += Math.floor(visibleRect.y / pixelSize) * pixelSize;
        }

        double endY = startY + Math.min(
            visibleRect.height + pixelSize, canvas.getCoHeight()) - 1;

        // vertical lines
        for (double x = startX + pixelSize; x < endX; x += pixelSize) {
            g2.draw(new Line2D.Double(x, startY, x, endY));
        }

        // horizontal lines
        for (double y = startY + pixelSize; y < endY; y += pixelSize) {
            g2.draw(new Line2D.Double(startX, y, endX, y));
        }

        // stop the XOR mode
        g2.setPaintMode();
    }

    /**
     * Enables or disables the visibility of the pixel grid.
     */
    public static void setPixelGridVisible(boolean visible) {
        if (pixelGridVisible == visible) {
            return;
        }
        pixelGridVisible = visible;
        if (visible) {
            ImageArea.pixelGridEnabled();
        } else {
            Views.repaintVisible();
        }
    }

    public boolean allowPixelGrid() {
        return zoomLevel.allowPixelGrid();
    }

    public void paintImmediately() {
        paintImmediately(getX(), getY(), getWidth(), getHeight());
    }

    /**
     * Repaints only a region of the image
     */
    public void repaintRegion(PPoint start, PPoint end, double thickness) {
        double startX = start.getCoX();
        double startY = start.getCoY();
        double endX = end.getCoX();
        double endY = end.getCoY();

        // make sure that the start coordinates are smaller
        if (endX < startX) {
            double tmp = startX;
            startX = endX;
            endX = tmp;
        }
        if (endY < startY) {
            double tmp = startY;
            startY = endY;
            endY = tmp;
        }

        // the thickness is derived from the brush radius, therefore
        // it still needs to be converted into component space
        thickness = zoomScale * thickness;

        startX = startX - thickness;
        endX = endX + thickness;
        startY = startY - thickness;
        endY = endY + thickness;

        // add 1 to the width and height because
        // casting to int will round them downwards
        double repWidth = endX - startX + 1;
        double repHeight = endY - startY + 1;

        repaint((int) startX, (int) startY, (int) repWidth, (int) repHeight);
    }

    /**
     * Repaints only a region of the image
     */
    public void repaintRegion(PRectangle area) {
        repaint(area.getCo());
    }

    public void ensurePositiveLocation() {
        if (viewContainer != null) {
            viewContainer.ensurePositiveLocation();
        }
    }

    public MaskViewMode getMaskViewMode() {
        return maskViewMode;
    }

    public boolean setMaskViewModeInternal(MaskViewMode maskViewMode) {
        // it is important not to call this directly,
        // it should be a part of a mask activation
        assert Assertions.callingClassIs("MaskViewMode");

        boolean changed = this.maskViewMode != maskViewMode;
        if (changed) {
            this.maskViewMode = maskViewMode;
            repaint();
        }
        return changed;
    }

    /**
     * Called when the canvas component-space size changed (zoom, resize, crop, etc.)
     */
    public void canvasCoSizeChanged() {
        assert ConsistencyChecks.imageCoversCanvas(comp);

        updateContainerSize();
        updateCanvasLocation();
    }

    private void updateContainerSize() {
        if (viewContainer instanceof ImageFrame frame) {
            frame.setToCanvasSize();
        }
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public int getCanvasStartX() {
        return canvasStartX;
    }

    public int getCanvasStartY() {
        return canvasStartY;
    }

    public double getZoomScale() {
        return zoomScale;
    }

    public ZoomLevel getZoomLevel() {
        return zoomLevel;
    }

    /**
     * Sets the zoom level using an automatic zoom calculation type.
     */
    public void setZoom(AutoZoom autoZoom) {
        setZoom(ZoomLevel.calcBestFitZoom(canvas, autoZoom, true));
    }

    /**
     * Sets the zoom level, maintaining the current view center.
     */
    public void setZoom(ZoomLevel newZoom) {
        setZoom(newZoom, null);
    }

    /**
     * Sets the zoom level while maintaining focus on a specific component-space point.
     */
    public void setZoom(ZoomLevel newZoom, Point coFocusPoint) {
        if (zoomLevel == newZoom) {
            return;
        }

        ZoomLevel prevZoom = zoomLevel;
        this.zoomLevel = newZoom;
        zoomScale = newZoom.getViewScale();
        canvas.recalcCoSize(this, true);

        if (ImageArea.isActiveMode(ImageArea.Mode.FRAMES)) {
            updateTitle();
        } else {
            revalidate(); // ensure scrollbars update in tabbed UI
        }

        if (viewContainer != null) {
            adjustScrollbarsAfterZoom(prevZoom, newZoom, coFocusPoint);
        }

        if (isActive()) {
            ZoomControl.get().updateZoom(zoomLevel);
        }
    }

    private void adjustScrollbarsAfterZoom(ZoomLevel prevZoom,
                                           ZoomLevel newZoom,
                                           Point coFocusPoint) {
        Rectangle visibleRegion = getVisibleRegion();
        Point zoomCenter;
        if (coFocusPoint != null) { // started from a mouse event
            zoomCenter = coFocusPoint;
        } else {
            zoomCenter = new Point(
                visibleRegion.x + visibleRegion.width / 2,
                visibleRegion.y + visibleRegion.height / 2);
        }
        // the center coordinates were generated BEFORE the zooming,
        // now find the corresponding coordinates after zooming
        Point2D imCenter = fromComponentToImageSpace(zoomCenter, prevZoom);
        zoomCenter = fromImageToComponentSpace(imCenter, newZoom);

        Rectangle targetRegion = new Rectangle(
            zoomCenter.x - visibleRegion.width / 2,
            zoomCenter.y - visibleRegion.height / 2,
            visibleRegion.width,
            visibleRegion.height
        );
        scrollRectToVisible(targetRegion);

        repaint();
    }

    /**
     * Zooms the view to fit a specific rectangle.
     */
    public void zoomToRegion(PRectangle rect) {
        Rectangle2D zoomRect = rect.getIm();
        if (zoomRect.isEmpty()) {
            return;
        }

        Canvas tmpCanvas = new Canvas(
            (int) zoomRect.getWidth(),
            (int) zoomRect.getHeight());

        setZoom(ZoomLevel.calcBestFitZoom(tmpCanvas, AutoZoom.FIT_SPACE, true));
        scrollRectToVisible(imageToComponentSpace(zoomRect));
    }

    public void zoomIn() {
        zoomIn(null);
    }

    public void zoomIn(Point coFocusPoint) {
        setZoom(zoomLevel.zoomIn(), coFocusPoint);
    }

    public void zoomOut() {
        zoomOut(null);
    }

    public void zoomOut(Point coFocusPoint) {
        setZoom(zoomLevel.zoomOut(), coFocusPoint);
    }

    // It seems that all Swing resizing goes through this method, so we don't
    // have to listen to componentResized events, which might come too late.
    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);

        updateCanvasLocation();
        repaint();
    }

    // recalculates coordinates when the view's layout changes
    private void updateCanvasLocation() {
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        int canvasCoWidth = canvas.getCoWidth();
        int canvasCoHeight = canvas.getCoHeight();

        // ensure the view is at least as big as the canvas
        if (viewWidth < canvasCoWidth || viewHeight < canvasCoHeight) {
            setSize(Math.max(viewWidth, canvasCoWidth),
                Math.max(viewHeight, canvasCoHeight));

            // setSize will call this method again after setting the size
            return;
        }

        // center the canvas within the view
        canvasStartX = (int) ((viewWidth - canvasCoWidth) / 2.0);
        canvasStartY = (int) ((viewHeight - canvasCoHeight) / 2.0);

        // one can zoom an inactive image with the mouse wheel,
        // but the tools are interacting only with the active image
        if (isActive()) {
            Tools.coCoordsChanged(this);
        }
        comp.coCoordsChanged();

        imToCo.invalidate();
        coToIm.invalidate();
    }

    public static boolean isPixelSnapping() {
        return pixelSnapping;
    }

    public static void toolSnappingChanged(boolean newValue, boolean force) {
        if (force) {
            pixelSnapping = newValue;
        } else {
            pixelSnapping = newValue && AppPreferences.getFlag(AppPreferences.FLAG_PIXEL_SNAP);
        }
    }

    public static void snappingSettingChanged(boolean newValue) {
        AppPreferences.setFlag(AppPreferences.FLAG_PIXEL_SNAP, newValue);
        Tool activeTool = Tools.getActive();
        if (activeTool == Tools.CROP) {
            pixelSnapping = true; // the crop tool always snaps
        } else {
            pixelSnapping = newValue && activeTool.hasPixelSnapping();
        }
    }

    public double componentXToImageSpace(double coX) {
        if (pixelSnapping) {
            return (int) (((coX - canvasStartX) / zoomScale) + 0.5);
        } else {
            return ((coX - canvasStartX) / zoomScale);
        }
    }

    public double componentYToImageSpace(double coY) {
        if (pixelSnapping) {
            return (int) (((coY - canvasStartY) / zoomScale) + 0.5);
        } else {
            return ((coY - canvasStartY) / zoomScale);
        }
    }

    public double imageXToComponentSpace(double imX) {
        return canvasStartX + imX * zoomScale;
    }

    public double imageYToComponentSpace(double imY) {
        return canvasStartY + imY * zoomScale;
    }

    public Point2D componentToImageSpace(Point2D co) {
        return new Point2D.Double(
            componentXToImageSpace(co.getX()),
            componentYToImageSpace(co.getY()));
    }

    public Point2D imageToComponentSpace(Point2D im) {
        return new Point2D.Double(
            imageXToComponentSpace(im.getX()),
            imageYToComponentSpace(im.getY()));
    }

    private Point2D fromComponentToImageSpace(Point co, ZoomLevel zoom) {
        double zoomViewScale = zoom.getViewScale();
        return new Point2D.Double(
            (co.x - canvasStartX) / zoomViewScale,
            (co.y - canvasStartY) / zoomViewScale
        );
    }

    private Point fromImageToComponentSpace(Point2D im, ZoomLevel zoom) {
        double zoomViewScale = zoom.getViewScale();
        return new Point(
            (int) (canvasStartX + im.getX() * zoomViewScale),
            (int) (canvasStartY + im.getY() * zoomViewScale)
        );
    }

    public Rectangle2D componentToImageSpace(Rectangle2D co) {
        double imX = componentXToImageSpace(co.getX());
        double imY = componentYToImageSpace(co.getY());
        double imWidth, imHeight;
        if (pixelSnapping) {
            imWidth = (int) ((co.getWidth() / zoomScale) + 0.5);
            imHeight = (int) ((co.getHeight() / zoomScale) + 0.5);
        } else {
            imWidth = co.getWidth() / zoomScale;
            imHeight = co.getHeight() / zoomScale;
        }
        return new Rectangle2D.Double(imX, imY, imWidth, imHeight);
    }

    public Rectangle imageToComponentSpace(Rectangle2D im) {
        return new Rectangle(
            (int) imageXToComponentSpace(im.getX()),
            (int) imageYToComponentSpace(im.getY()),
            (int) (im.getWidth() * zoomScale),
            (int) (im.getHeight() * zoomScale)
        );
    }

    public Rectangle2D imageToComponentSpace2(Rectangle2D im) {
        return new Rectangle2D.Double(
            imageXToComponentSpace(im.getX()),
            imageYToComponentSpace(im.getY()),
            im.getWidth() * zoomScale,
            im.getHeight() * zoomScale
        );
    }

    public AffineTransform getImageToComponentTransform() {
        return imToCo.get();
    }

    private AffineTransform createImToCoTransform() {
        var at = new AffineTransform();
        at.translate(canvasStartX, canvasStartY);
        at.scale(zoomScale, zoomScale);
        return at;
    }

    public AffineTransform getComponentToImageTransform() {
        return coToIm.get();
    }

    private AffineTransform createCoToImTransform() {
        var at = new AffineTransform();
        double s = 1.0 / zoomScale;
        at.scale(s, s);
        at.translate(-canvasStartX, -canvasStartY);
        return at;
    }

    /**
     * Returns (in component space) how much of this {@link View} is currently
     * visible considering that the JScrollPane might show only a part of it.
     */
    public Rectangle getVisibleRegion() {
        return viewContainer.getScrollPane().getViewport().getViewRect();
    }

    public void addLayerToGUI(Layer layer, int index) {
        assert calledOnEDT() : callInfo();

        // can be cast outside unit tests
        LayerGUI layerGUI = (LayerGUI) layer.createUI();

        try {
            // otherwise loading multi-layer files makes the comp dirty
            layerGUI.setReactToItemEvents(false);
            layersPanel.addLayerGUI(layerGUI, index);
            layerGUI.updateSelectionState();
        } finally {
            layerGUI.setReactToItemEvents(true);
        }

        if (isActive() && comp.isHolderOfActiveLayer()) {
            Layers.numLayersChanged(comp, comp.getNumLayers());
        }
    }

    /**
     * The return value is changed only in the unit tests using mocked views
     */
    @SuppressWarnings({"MethodMayBeStatic", "SameReturnValue"})
    public boolean isMock() {
        return false;
    }

    public LayersPanel getLayersPanel() {
        return layersPanel;
    }

    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }

    public void repaintNavigator(boolean canvasSizeChanged) {
        assert calledOnEDT() : callInfo();

        if (navigator == null) {
            return;
        }
        if (canvasSizeChanged) {
            // defer until all pending events have been processed
            SwingUtilities.invokeLater(() -> {
                if (navigator != null) { // check again for safety
                    // will also repaint
                    navigator.recalculateSize(this, false,
                        true, false);
                }
            });
        } else {
            // call now, repainting calls will be coalesced anyway
            navigator.repaint();
        }
    }

    /**
     * Returns the bounds of the visible part of the canvas
     * in screen coordinates
     */
    public Rectangle getVisibleCanvasBoundsOnScreen() {
        // the canvas bounds relative to this view
        Rectangle canvasBounds = canvas.getCoBounds(this);

        // take scrollbars into account
        Rectangle visibleCanvas = canvasBounds.intersection(getVisibleRegion());
        if (visibleCanvas.isEmpty()) {
            throw new IllegalStateException("canvas not visible");
        }

        // convert to screen coordinates
        Point offset = getLocationOnScreen();
        visibleCanvas.translate(offset.x, offset.y);
        return visibleCanvas;
    }

    /**
     * Returns the component that should be used as a parent in dialogs.
     * The View itself is not a good parent component, because its center could be
     * anywhere when zoomed in.
     */
    public Component getDialogParent() {
        return (Component) viewContainer;
    }

    public boolean checkInvariants() {
        // calls getComp() instead of accessing the field
        // so that it works in mocked views.
        if (getComp().getView() != this) {
            throw new AssertionError("view error in " + comp.getName());
        }
        return true;
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = new DebugNode(key, this);

        node.add(getComp().createDebugNode("composition"));
        node.addNullableDebuggable("canvas", getCanvas());
        node.addAsString("zoom level", getZoomLevel());

        node.addInt("view width", getWidth());
        node.addInt("view height", getHeight());

        if (viewContainer instanceof ImageFrame frame) {
            node.addInt("frame width", frame.getWidth());
            node.addInt("frame height", frame.getHeight());
        }

        node.addQuotedString("mask view mode", getMaskViewMode().toString());

        return node;
    }

    @Override
    public String toString() {
        return "View of " + comp.getDebugName();
    }
}
