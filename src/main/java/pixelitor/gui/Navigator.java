/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.OpenImages;
import pixelitor.colors.Colors;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.PAction;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.menus.view.ZoomMenu;
import pixelitor.tools.util.PRectangle;
import pixelitor.utils.Cursors;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.ViewActivationListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/**
 * The navigator component that allows the user to pan a zoomed-in image.
 */
public class Navigator extends JComponent
        implements MouseListener, MouseMotionListener, ViewActivationListener {

    private static final int DEFAULT_NAVIGATOR_SIZE = 300;
    private static final BasicStroke VIEW_BOX_STROKE = new BasicStroke(3);
    private static final CheckerboardPainter checkerBoardPainter
            = ImageUtils.createCheckerboardPainter();

    // The navigated active view.
    // Null if all images are closed.
    private View view;

    private int viewWidth;
    private int viewHeight;
    private JScrollPane scrollPane;

    private Point dragStartPoint;
    private Point origRectLoc; // the view box rectangle location before starting the drag
    private Rectangle viewBoxRect;
    private static Color viewBoxColor = Color.RED;
    private Rectangle viewRatioRect;
    private static Color viewRatioColor = Color.YELLOW;
    private boolean dragging = false;
    private boolean draggingInside = false;

    private int preferredWidth;
    private int preferredHeight;

    private double imgScalingRatio;
    private int thumbWidth;
    private int thumbHeight;

    private final AdjustmentListener adjListener;
    private static JDialog dialog;
    private JPopupMenu popup;

    // if not null, the scaling factor is calculated based on this
    // explicitly given zoom level instead of the navigator size
    private ZoomLevel exactZoom = null;

    private static Navigator navigatorPanel;

    private Navigator(View view) {
        adjListener = e ->
                SwingUtilities.invokeLater(this::updateViewBoxPosition);

        recalculateSize(view, true, true, true);

        addMouseListener(this);
        addMouseMotionListener(this);
        OpenImages.addActivationListener(this);

        addNavigatorResizedListener();
        addMouseWheelZoomingSupport();

        ZoomMenu.setupZoomKeys(this);
        addPopupMenu();
    }

    private void addPopupMenu() {
        popup = new JPopupMenu();
        ZoomLevel[] levels = {ZoomLevel.Z100, ZoomLevel.Z50, ZoomLevel.Z25, ZoomLevel.Z12};
        for (ZoomLevel level : levels) {
            popup.add(new PAction("Navigator Zoom: " + level) {
                @Override
                public void onClick() {
                    setNavigatorSizeFromZoom(level);
                }
            });
        }
        popup.addSeparator();
        popup.add(new PAction("View Box Color...") {
            @Override
            public void onClick() {
                Colors.selectColorWithDialog(Navigator.this,
                        "View Box Color", viewBoxColor, true,
                        Navigator.this::setNewViewBoxColor);
            }
        });
    }

    private void setNewViewBoxColor(Color newColor) {
        viewBoxColor = newColor;
        repaint();
    }

    private void setNavigatorSizeFromZoom(ZoomLevel zoom) {
        Canvas canvas = view.getCanvas();
        double scale = zoom.getViewScale();
        preferredWidth = (int) (scale * canvas.getWidth());
        preferredHeight = (int) (scale * canvas.getHeight());

        JDialog ancestor = GUIUtils.getDialogAncestor(this);
        ancestor.setTitle("Navigator - " + zoom);

        exactZoom = zoom; // set the the exact zoom only temporarily

        // force pack() to use the current preferred
        // size instead of some cached value
        invalidate();

        ancestor.pack();
    }

    private void showPopup(MouseEvent e) {
        popup.show(this, e.getX(), e.getY());
    }

    private void addNavigatorResizedListener() {
        // update size calculations if the navigator is resized
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (view != null) { // it is null if all images are closed
                    recalculateSize(view, false, false, true);
                }
            }
        });
    }

    private void addMouseWheelZoomingSupport() {
        MouseZoomMethod.CURRENT.installOnNavigator(this, view);
    }

    public static void showInDialog() {
        if (dialog != null && dialog.isVisible()) {
            dialog.setVisible(false);
            dialog.dispose();
        }

        View view = OpenImages.getActiveView();
        navigatorPanel = new Navigator(view);

        dialog = new DialogBuilder()
                .title("Navigator")
                .content(navigatorPanel)
                .notModal()
                .noOKButton()
                .noCancelButton()
                .cancelAction(navigatorPanel::dispose) // when it is closed with X
                .show();
    }

    public static void setMouseZoomMethod(MouseZoomMethod newZoomMethod) {
        if (dialog == null || !dialog.isVisible()) {
            return;
        }
        assert navigatorPanel != null;
        assert navigatorPanel.view != null;
        newZoomMethod.installOnNavigator(navigatorPanel, navigatorPanel.view);
    }

    public void recalculateSize(View view,
                                boolean newView,
                                boolean viewSizeChanged,
                                boolean navigatorResized) {
        assert newView || viewSizeChanged || navigatorResized : "why did you call me?";

        if (newView) {
            if (this.view != null) {
                releaseImage();
            }

            this.view = view;
            scrollPane = view.getViewContainer().getScrollPane();
        }

        if (exactZoom == null) {
            JDialog ancestor = GUIUtils.getDialogAncestor(this);
            if (ancestor != null) { // is null during the initial construction
                ancestor.setTitle("Navigator");
            }
        }

        if (newView) {
            recalculateScaling(view, DEFAULT_NAVIGATOR_SIZE, DEFAULT_NAVIGATOR_SIZE);
        } else if (viewSizeChanged || navigatorResized) {
            recalculateScaling(view, getWidth(), getHeight());
        } else {
            throw new IllegalStateException();
        }

        preferredWidth = thumbWidth;
        preferredHeight = thumbHeight;

        updateViewBoxPosition();

        if (newView) {
            view.setNavigator(this);
            scrollPane.getHorizontalScrollBar().addAdjustmentListener(adjListener);
            scrollPane.getVerticalScrollBar().addAdjustmentListener(adjListener);
        }

        if (viewSizeChanged) {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.pack();
            }
        }

        repaint();
    }

    private void releaseImage() {
        view.setNavigator(null);
        scrollPane.getHorizontalScrollBar().removeAdjustmentListener(adjListener);
        scrollPane.getVerticalScrollBar().removeAdjustmentListener(adjListener);

        view = null;
    }

    // updates the view box rectangle position based on the view
    private void updateViewBoxPosition() {
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
        viewRatioRect = new Rectangle(boxX, boxY, boxWidth, boxHeight);
        repaint();
    }

    // scrolls the main composition view based on the view box position and thumb size
    private void scrollView() {
        double scaleX = (double) viewWidth / thumbWidth;
        double scaleY = (double) viewHeight / thumbHeight;

        int x = (int) (viewBoxRect.x * scaleX);
        int y = (int) (viewBoxRect.y * scaleY);
        int width = (int) (viewBoxRect.width * scaleX);
        int height = (int) (viewBoxRect.height * scaleY);

        view.scrollRectToVisible(new Rectangle(x, y, width, height));
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (view == null) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;

        checkerBoardPainter.paint(g2, null, thumbWidth, thumbHeight);

        var origTransform = g2.getTransform();

        g2.scale(imgScalingRatio, imgScalingRatio);
        BufferedImage compositeImage = view.getComp().getCompositeImage();
        g2.drawImage(compositeImage, 0, 0, null);
        g2.setTransform(origTransform);

        g2.setStroke(VIEW_BOX_STROKE);
        g2.setColor(viewRatioColor);
        g2.draw(viewRatioRect);
        g2.setColor(viewBoxColor);
        g2.draw(viewBoxRect);
    }


    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            showPopup(e);
        } else {
            Point point = e.getPoint();
            if (view != null) {
                dragStartPoint = point;
                origRectLoc = viewBoxRect.getLocation();

                if (viewBoxRect.contains(point)) {
                    draggingInside = !e.isControlDown();
                } else draggingInside = false;

                dragging = true;
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            showPopup(e);
        }

        if (!draggingInside) {

            Point mouseNow = e.getPoint();

            // initial point will always be in viewbox, else that will be an impossible drag star.

            // making sure the drag wont exceed the available space.
            if (mouseNow.x > thumbWidth) {
                mouseNow.x = thumbWidth;
            }
            if (mouseNow.y > thumbHeight) {
                mouseNow.y = thumbHeight;
            }

            int x = Math.min(dragStartPoint.x, mouseNow.x);
            int y = Math.min(dragStartPoint.y, mouseNow.y);
            int w = Math.max(dragStartPoint.x, mouseNow.x) - x;
            int h = Math.max(dragStartPoint.y, mouseNow.y) - y;

            zoomFrom(viewRatioRect);
        }

        dragStartPoint = null;
        dragging = false;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (dragStartPoint != null) {
            assert dragging;

            Point mouseNow = e.getPoint();
            int dx = mouseNow.x - dragStartPoint.x;
            int dy = mouseNow.y - dragStartPoint.y;

            int newBoxX = origRectLoc.x + dx;
            int newBoxY = origRectLoc.y + dy;

            if (draggingInside) {

                // make sure that the view box does not leave the thumb
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

            } else {

                // initial point will always be in viewbox, else that will be an impossible drag star.

                // making sure the drag wont exceed the available space.
                if (mouseNow.x > thumbWidth) {
                    mouseNow.x = thumbWidth;
                }
                if (mouseNow.y > thumbHeight) {
                    mouseNow.y = thumbHeight;
                }

                int x = Math.min(dragStartPoint.x, mouseNow.x);
                int y = Math.min(dragStartPoint.y, mouseNow.y);
                int w = Math.max(dragStartPoint.x, mouseNow.x) - x;
                int h = Math.max(dragStartPoint.y, mouseNow.y) - y;

                updateRatioViewBox(dragStartPoint.x, dragStartPoint.y, mouseNow.x, mouseNow.y);
                updateViewBox(x, y, w, h);
            }
        }
    }

    private void updateViewBox(int newBoxX, int newBoxY, int newWidth, int newHeight) {
        if (newBoxX != viewBoxRect.x || newBoxY != viewBoxRect.y || newWidth != viewBoxRect.width || newHeight != viewBoxRect.height) {
            viewBoxRect.setBounds(newBoxX, newBoxY, newWidth, newHeight);
            repaint();
        }
    }

    private void updateRatioViewBox(int xi, int yi, int xn, int yn) {

        int h = Math.abs(yn - yi);
        int w = (int) (view.getVisiblePart().width * 1d * h / view.getVisiblePart().height);

        if (xn < xi) xi -= w;
        if (yn < yi) yi -= h;

        viewRatioRect.setBounds(xi, yi, w, h);
    }

    private void updateViewBoxLocation(int newBoxX, int newBoxY) {
        if (newBoxX != viewBoxRect.x || newBoxY != viewBoxRect.y) {
            viewBoxRect.setLocation(newBoxX, newBoxY);
            repaint();
            scrollView();
        }
    }

    private void zoomFrom(Rectangle rect) {
        viewBoxRect.setBounds(rect);
        repaint();

        double scaleX = (double) view.getWidth() / thumbWidth;

        int x = (int) (viewBoxRect.x * scaleX);
        int y = (int) (viewBoxRect.y * scaleX);
        int width = (int) (viewBoxRect.width * scaleX);
        int height = (int) (viewBoxRect.height * scaleX);

        view.zoomToRect(PRectangle.fromCo(new Rectangle(x, y, width, height), view));
    }

    private void recalculateScaling(View view, int width, int height) {
        Canvas canvas = view.getCanvas();
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        if (exactZoom != null) {
            imgScalingRatio = exactZoom.getViewScale();

            exactZoom = null; // was set only temporarily
        } else {
            double xScaling = width / (double) canvasWidth;
            double yScaling = height / (double) canvasHeight;

            imgScalingRatio = Math.min(xScaling, yScaling);
        }

        thumbWidth = (int) (canvasWidth * imgScalingRatio);
        thumbHeight = (int) (canvasHeight * imgScalingRatio);
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
        releaseImage();
        repaint();
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        recalculateSize(newView, true, true, false);
    }

    private void dispose() {
        OpenImages.removeActivationListener(this);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(preferredWidth, preferredHeight);
    }
}
