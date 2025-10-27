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
import pixelitor.Views;
import pixelitor.colors.Colors;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.TaskAction;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.menus.view.ZoomMenu;
import pixelitor.tools.util.PRectangle;
import pixelitor.utils.Cursors;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Shapes;
import pixelitor.utils.ViewActivationListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import static pixelitor.menus.view.ZoomLevel.ACTUAL_SIZE;
import static pixelitor.menus.view.ZoomLevel.EIGHTH_SIZE;
import static pixelitor.menus.view.ZoomLevel.HALF_SIZE;
import static pixelitor.menus.view.ZoomLevel.QUARTER_SIZE;

/**
 * A component that displays a thumbnail of the active composition, and
 * allows the user to pan a zoomed-in image by dragging the "view box",
 * or to zoom to a specific area by Ctrl-dragging to create a new view box.
 */
public class Navigator extends JComponent
    implements MouseListener, MouseMotionListener, ViewActivationListener {

    private static final int DEFAULT_SIZE = 300;
    private static final BasicStroke VIEW_BOX_STROKE = new BasicStroke(3);
    private static final CheckerboardPainter checkerboardPainter
        = ImageUtils.createCheckerboardPainter();

    // The currently active View being navigated.
    // Null if all images are closed.
    private View view;

    private int viewWidth;
    private int viewHeight;
    private JScrollPane scrollPane;

    // the view box represents the view's viewport, in the navigator's component space
    private Rectangle viewBoxRect;
    private static Color viewBoxColor = Color.RED;

    // the new area to zoom into, created by Ctrl-dragging
    private Rectangle targetBoxRect;

    private Point dragStartPoint;
    private Point dragStartBoxLoc; // the location of the view box when the drag started

    private boolean dragging = false;
    private boolean areaZooming = false;

    private int preferredWidth;
    private int preferredHeight;

    private double thumbnailScale;
    private int thumbWidth;
    private int thumbHeight;

    private final AdjustmentListener viewScrollListener;
    private static JDialog dialog;
    private JPopupMenu contextMenu;

    // if not null, the scaling is determined by this, rather
    // than being calculated to fit the navigator's current size
    private ZoomLevel fixedZoom = null;

    private static Navigator instance;

    private Navigator(View view) {
        viewScrollListener = e ->
            SwingUtilities.invokeLater(this::syncViewBoxPosition);

        recalculateSize(view, true, true, true);

        addMouseListener(this);
        addMouseMotionListener(this);
        Views.addActivationListener(this);

        addResizeListener();
        addZoomingSupport();

        addContextMenu();
    }

    private void addContextMenu() {
        contextMenu = new JPopupMenu();
        ZoomLevel[] levels = {ACTUAL_SIZE, HALF_SIZE, QUARTER_SIZE, EIGHTH_SIZE};
        for (ZoomLevel level : levels) {
            contextMenu.add(new TaskAction("Navigator Zoom: " + level, () ->
                resizeDialogForZoom(level)));
        }
        contextMenu.addSeparator();
        contextMenu.add(new TaskAction("View Box Color...", () ->
            Colors.selectColorWithDialog(this,
                "View Box Color", viewBoxColor, true,
                this::setViewBoxColor)));
    }

    private void setViewBoxColor(Color newColor) {
        viewBoxColor = newColor;
        repaint();
    }

    // changes the size of the containing dialog to match
    // the given zoom level for the thumbnail
    private void resizeDialogForZoom(ZoomLevel zoom) {
        Canvas canvas = view.getCanvas();
        double scale = zoom.getScale();
        preferredWidth = (int) (scale * canvas.getWidth());
        preferredHeight = (int) (scale * canvas.getHeight());

        JDialog ancestor = GUIUtils.getDialogAncestor(this);
        ancestor.setTitle("Navigator - " + zoom);

        fixedZoom = zoom; // set the exact zoom only temporarily

        // force pack() to use the current preferred
        // size instead of some cached value
        invalidate();

        ancestor.pack();
    }

    private void showPopup(MouseEvent e) {
        contextMenu.show(this, e.getX(), e.getY());
    }

    private void addResizeListener() {
        // update size calculations if the navigator is resized
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (view != null) { // it's null if all images are closed
                    recalculateSize(view, false, false, true);
                }
            }
        });
    }

    private void addZoomingSupport() {
        MouseZoomMethod.ACTIVE.installOnOther(this);
        ZoomMenu.setupZoomKeys(this);
    }

    public static void showInDialog(View view) {
        if (dialog != null && dialog.isVisible()) {
            dialog.setVisible(false);
            dialog.dispose();
        }

        assert view.isActive();
        instance = new Navigator(view);

        dialog = new DialogBuilder()
            .title("Navigator")
            .content(instance)
            .notModal()
            .noOKButton()
            .noCancelButton()
            .cancelAction(instance::dispose) // when it's closed with X
            .show()
            .getDialog();
    }

    public static void setMouseZoomMethod(MouseZoomMethod newZoomMethod) {
        if (dialog == null || !dialog.isVisible()) {
            return;
        }
        assert instance != null;
        assert instance.view != null;
        newZoomMethod.installOnOther(instance);
    }

    /**
     * Recalculates the navigator size and the scaling factors when the view or canvas changes.
     */
    public void recalculateSize(View sourceView,
                                boolean newView,
                                boolean canvasSizeChanged,
                                boolean navigatorResized) {
        assert newView || canvasSizeChanged || navigatorResized : "why did you call me?";

        if (newView) {
            attachToView(sourceView);
        }

        if (fixedZoom == null) {
            JDialog ancestor = GUIUtils.getDialogAncestor(this);
            if (ancestor != null) { // is null during the initial construction
                ancestor.setTitle("Navigator");
            }
        }

        if (newView) {
            updateThumbnailMetrics(sourceView, DEFAULT_SIZE, DEFAULT_SIZE);
        } else if (canvasSizeChanged || navigatorResized) {
            updateThumbnailMetrics(sourceView, getWidth(), getHeight());
        } else {
            throw new IllegalStateException();
        }

        preferredWidth = thumbWidth;
        preferredHeight = thumbHeight;

        syncViewBoxPosition();

        if (canvasSizeChanged) {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.pack();
            }
        }

        repaint();
    }

    private void attachToView(View newView) {
        if (this.view != null) {
            detachFromView();
        }

        this.view = newView;
        scrollPane = newView.getViewContainer().getScrollPane();
        view.setNavigator(this);
        scrollPane.getHorizontalScrollBar().addAdjustmentListener(viewScrollListener);
        scrollPane.getVerticalScrollBar().addAdjustmentListener(viewScrollListener);
    }

    private void detachFromView() {
        view.setNavigator(null);
        scrollPane.getHorizontalScrollBar().removeAdjustmentListener(viewScrollListener);
        scrollPane.getVerticalScrollBar().removeAdjustmentListener(viewScrollListener);

        view = null;
    }

    // updates the view box rectangle position based on the view
    private void syncViewBoxPosition() {
        if (dragging) {
            // no need to update the rectangle if the change
            // was caused by this navigator
            return;
        }

        JViewport viewport = scrollPane.getViewport();
        Rectangle viewRect = viewport.getViewRect();

        Dimension d = viewport.getViewSize();
        viewWidth = d.width;
        viewHeight = d.height;

        double scaleX = (double) thumbWidth / viewWidth;
        double scaleY = (double) thumbHeight / viewHeight;

        int boxX = (int) (viewRect.x * scaleX);
        int boxY = (int) (viewRect.y * scaleY);
        int boxWidth = (int) (viewRect.width * scaleX);
        int boxHeight = (int) (viewRect.height * scaleY);

        viewBoxRect = new Rectangle(boxX, boxY, boxWidth, boxHeight);
        repaint();
    }

    // calculates the rectangle in the view's coordinate space
    // that corresponds to the navigator's view box
    private Rectangle getScaledViewRect() {
        double scaleX = (double) viewWidth / thumbWidth;
        double scaleY = (double) viewHeight / thumbHeight;

        int x = (int) (viewBoxRect.x * scaleX);
        int y = (int) (viewBoxRect.y * scaleY);
        int width = (int) (viewBoxRect.width * scaleX);
        int height = (int) (viewBoxRect.height * scaleY);

        return new Rectangle(x, y, width, height);
    }

    // scrolls the view to match the current position of the view box
    private void scrollView() {
        view.scrollRectToVisible(getScaledViewRect());
    }

    // zooms the main view to the region defined by the target box rectangle
    private void areaZoom() {
        view.zoomToRegion(PRectangle.fromCo(getScaledViewRect(), view));
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (view == null) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        var origTransform = g2.getTransform();

        // draw the thumbnail scaled down
        checkerboardPainter.paint(g2, null, thumbWidth, thumbHeight);
        g2.scale(thumbnailScale, thumbnailScale);
        g2.drawImage(view.getComp().getCompositeImage(), 0, 0, null);

        // draw the viewport indicator box with the original transform
        g2.setTransform(origTransform);
        g2.setStroke(VIEW_BOX_STROKE);
        g2.setColor(viewBoxColor);
        g2.draw(viewBoxRect);

        if (targetBoxRect != null) {
            Shapes.drawVisibly(g2, targetBoxRect);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            showPopup(e);
            return;
        }

        Point point = e.getPoint();
        if (view != null) {
            if (e.isControlDown()) {
                areaZooming = true;
                dragging = true;
                targetBoxRect = new Rectangle(viewBoxRect);
            } else if (viewBoxRect.contains(point)) {
                areaZooming = false;
                dragging = true;
            }

            if (dragging) {
                dragStartPoint = point;
                dragStartBoxLoc = viewBoxRect.getLocation();
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (dragStartPoint != null) {
            assert dragging;

            Point mouseNow = e.getPoint();
            int dx = mouseNow.x - dragStartPoint.x;
            int dy = mouseNow.y - dragStartPoint.y;

            int newBoxX = dragStartBoxLoc.x + dx;
            int newBoxY = dragStartBoxLoc.y + dy;

            if (areaZooming) {
                // ensure the zoom area remains within the image
                mouseNow.x = Math.clamp(mouseNow.x, 0, thumbWidth);
                mouseNow.y = Math.clamp(mouseNow.y, 0, thumbHeight);

                int x = Math.min(dragStartPoint.x, mouseNow.x);
                int y = Math.min(dragStartPoint.y, mouseNow.y);
                int w = Math.max(dragStartPoint.x, mouseNow.x) - x;
                int h = Math.max(dragStartPoint.y, mouseNow.y) - y;

                updateTargetBox(x, y, w, h);
            } else {
                // prevent the view box from being dragged outside the thumbnail
                if (newBoxX < 0) {
                    newBoxX = 0;
                }
                if (newBoxY < 0) {
                    newBoxY = 0;
                }
                if (newBoxX + viewBoxRect.width > thumbWidth) {
                    newBoxX = thumbWidth - viewBoxRect.width;
                }
                if (newBoxY + viewBoxRect.height > thumbHeight) {
                    newBoxY = thumbHeight - viewBoxRect.height;
                }

                updateViewBoxLocation(newBoxX, newBoxY);
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            showPopup(e);
        }
        dragStartPoint = null;
        dragging = false;

        if (areaZooming) {
            viewBoxRect = targetBoxRect;
            targetBoxRect = null;
            areaZoom();
            syncViewBoxPosition();
        } else {
            repaint();
        }

        areaZooming = false;
    }

    private void updateViewBoxLocation(int newBoxX, int newBoxY) {
        if (newBoxX != viewBoxRect.x || newBoxY != viewBoxRect.y) {
            viewBoxRect.setLocation(newBoxX, newBoxY);
            repaint();
            scrollView();
        }
    }

    private void updateTargetBox(int x, int y, int width, int height) {
        targetBoxRect.setBounds(x, y, width, height);
        repaint();
    }

    private void updateThumbnailMetrics(View view, int width, int height) {
        Canvas canvas = view.getCanvas();
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        if (fixedZoom != null) {
            thumbnailScale = fixedZoom.getScale();

            fixedZoom = null; // was set only temporarily
        } else {
            double xScaling = width / (double) canvasWidth;
            double yScaling = height / (double) canvasHeight;

            thumbnailScale = Math.min(xScaling, yScaling);
        }

        thumbWidth = (int) (canvasWidth * thumbnailScale);
        thumbHeight = (int) (canvasHeight * thumbnailScale);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // not interested
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // not interested
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // not interested
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        setCursorAt(e.getX(), e.getY());
    }

    private void setCursorAt(int x, int y) {
        if (viewBoxRect.contains(x, y)) {
            setCursor(Cursors.HAND);
        } else {
            setCursor(Cursors.DEFAULT);
        }
    }

    @Override
    public void allViewsClosed() {
        detachFromView();
        repaint();
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        recalculateSize(newView, true, true, false);
    }

    private void dispose() {
        Views.removeActivationListener(this);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(preferredWidth, preferredHeight);
    }
}
