/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.colors.ColorUtils;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.menus.view.ZoomMenu;
import pixelitor.utils.Cursors;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.ViewActivationListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

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
    private boolean dragging = false;

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
            popup.add(new AbstractAction("Navigator Zoom: " + level) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setNavigatorSizeFromZoom(level);
                }
            });
        }
        popup.addSeparator();
        popup.add(new AbstractAction("View Box Color...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ColorUtils.selectColorWithDialog(Navigator.this,
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
        addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                if (e.getWheelRotation() < 0) { // up, away from the user
                    // this.view will be always the active image...
                    if (view != null) { // ...and it is null if all images are closed
                        view.increaseZoom();
                    }
                } else {  // down, towards the user
                    if (view != null) {
                        view.decreaseZoom();
                    }
                }
            }
        });
    }

    public static void showInDialog() {
        if (dialog != null && dialog.isVisible()) {
            dialog.setVisible(false);
            dialog.dispose();
        }

        View view = OpenImages.getActiveView();
        var navigator = new Navigator(view);

        dialog = new DialogBuilder()
            .title("Navigator")
            .content(navigator)
            .notModal()
            .noOKButton()
            .noCancelButton()
            .noGlobalKeyChange()
            .cancelAction(navigator::dispose) // when it is closed with X
            .show();
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
        repaint();
    }

    // scrolls the main composition view based on the view box position
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
        g2.drawImage(view.getComp().getCompositeImage(), 0, 0, null);
        g2.setTransform(origTransform);

        g2.setStroke(VIEW_BOX_STROKE);
        g2.setColor(viewBoxColor);
        g2.draw(viewBoxRect);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            showPopup(e);
        } else {
            Point point = e.getPoint();
            if (viewBoxRect.contains(point) && view != null) {
                dragStartPoint = point;
                origRectLoc = viewBoxRect.getLocation();
                dragging = true;
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
        }
    }

    private void updateViewBoxLocation(int newBoxX, int newBoxY) {
        if (newBoxX != viewBoxRect.x || newBoxY != viewBoxRect.y) {
            viewBoxRect.setLocation(newBoxX, newBoxY);
            repaint();
            scrollView();
        }
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
