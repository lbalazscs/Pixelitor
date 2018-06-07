/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import com.bric.swing.ColorPicker;
import org.jdesktop.swingx.painter.CheckerboardPainter;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.menus.view.ZoomMenu;
import pixelitor.utils.ActiveImageChangeListener;
import pixelitor.utils.Cursors;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;

/**
 * The navigator component that allows the user to pan a zoomed-in image.
 */
public class Navigator extends JComponent implements MouseListener, MouseMotionListener, ActiveImageChangeListener {
    private static final BasicStroke VIEW_BOX_STROKE = new BasicStroke(3);
    private static final CheckerboardPainter checkerBoardPainter = ImageUtils.createCheckerboardPainter();
    private static final int DEFAULT_NAVIGATOR_SIZE = 300;

    private ImageComponent ic; // can be null if all images are closed

    private boolean dragging = false;
    private double imgScalingRatio;

    private Rectangle viewBoxRect;
    private Point dragStartPoint;
    private Point origRectLoc; // the view box rectangle location before starting the drag
    private int thumbWidth;
    private int thumbHeight;
    private int viewWidth;
    private int viewHeight;
    private JScrollPane scrollPane;
    private final AdjustmentListener adjListener;
    private static JDialog dialog;
    private JPopupMenu popup;
    private Color viewBoxColor = Color.RED;

    private int preferredWidth;
    private int preferredHeight;

    // it not null, the scaling factor should be calculated
    // based on this instead of the navigator size
    private ZoomLevel exactZoom = null;

    private Navigator(ImageComponent ic) {
        adjListener = e ->
                SwingUtilities.invokeLater(this::updateViewBoxPosition);

        recalculateSize(ic, true, true, true);

        addMouseListener(this);
        addMouseMotionListener(this);
        ImageComponents.addActiveImageChangeListener(this);

        addNavigatorResizedListener();
        addMouseWheelZoomingSupport();

        ZoomMenu.setupZoomKeys(this);
        addPopupMenu();
    }

    private void addPopupMenu() {
        popup = new JPopupMenu();
        ZoomLevel[] levels = {ZoomLevel.Z100, ZoomLevel.Z50, ZoomLevel.Z25, ZoomLevel.Z12};
        for (ZoomLevel level : levels) {
            popup.add(new AbstractAction("Navigator Zoom: " + level.toString()) {
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
                Window owner = SwingUtilities.getWindowAncestor(Navigator.this);
                Color newColor = ColorPicker.showDialog(owner, "View Box Color", viewBoxColor, true);
                if (newColor != null) { // something was chosen
                    viewBoxColor = newColor;
                    repaint();
                }
            }
        });
    }

    private void setNavigatorSizeFromZoom(ZoomLevel zoom) {
        Canvas canvas = ic.getCanvas();
        double scale = zoom.getViewScale();
        preferredWidth = (int) (scale * canvas.getWidth());
        preferredHeight = (int) (scale * canvas.getHeight());

        JDialog dialog = GUIUtils.getDialogAncestor(this);
        dialog.setTitle("Navigator - " + zoom.toString());

        exactZoom = zoom; // set the the exact zoom only temporarily

        // force pack to use the newest preferred
        // size instead of some cached value
        invalidate();

        dialog.pack();
    }

    private void showPopup(MouseEvent e) {
        popup.show(this, e.getX(), e.getY());
    }

    private void addNavigatorResizedListener() {
        // if the navigator is resized, then the size calculations
        // have to be refreshed
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (Navigator.this.ic != null) { // it is null if all images are closed
                    recalculateSize(Navigator.this.ic, false, false, true);
                }
            }
        });
    }

    private void addMouseWheelZoomingSupport() {
        addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                if (e.getWheelRotation() < 0) { // up, away from the user
                    // this.ic will be always the active image
                    this.ic.increaseZoom();
                } else {  // down, towards the user
                    this.ic.decreaseZoom();
                }
            }
        });
    }

    public static void showInDialog(PixelitorWindow pw) {
        ImageComponent ic = ImageComponents.getActiveIC();
        Navigator navigator = new Navigator(ic);

        if (dialog != null && dialog.isVisible()) {
            dialog.setVisible(false);
            dialog.dispose();
        }

        dialog = new DialogBuilder()
                .title("Navigator")
                .parent(pw)
                .form(navigator)
                .notModal()
                .noOKButton()
                .noCancelButton()
                .noGlobalKeyChange()
                .okAction(navigator::dispose)
//                .dialogFactory(dialogFactory)
                .show();
    }

    public void recalculateSize(ImageComponent ic,
                                boolean newIC,
                                boolean icSizeChanged,
                                boolean navigatorResized) {
        assert (newIC || icSizeChanged || navigatorResized) : "why did you call me?";

        if (newIC) {
            if (this.ic != null) {
                releaseImage();
            }

            this.ic = ic;
            scrollPane = ic.getFrame().getScrollPane();
        }

        if (exactZoom == null) {
            JDialog dialog = GUIUtils.getDialogAncestor(this);
            if (dialog != null) { // is null during the initial construction
                dialog.setTitle("Navigator");
            }
        }

        if (newIC) {
            reCalcScaling(ic, DEFAULT_NAVIGATOR_SIZE, DEFAULT_NAVIGATOR_SIZE);
        } else if (icSizeChanged || navigatorResized) {
            reCalcScaling(ic, getWidth(), getHeight());
        }

        preferredWidth = thumbWidth;
        preferredHeight = thumbHeight;

        updateViewBoxPosition();

        if (newIC) {
            ic.setNavigator(this);
            scrollPane.getHorizontalScrollBar().addAdjustmentListener(adjListener);
            scrollPane.getVerticalScrollBar().addAdjustmentListener(adjListener);
        }

        if (icSizeChanged) {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.pack();
            }
        }
        repaint();
    }

    private void releaseImage() {
        ic.setNavigator(null);
        scrollPane.getHorizontalScrollBar().removeAdjustmentListener(adjListener);
        scrollPane.getVerticalScrollBar().removeAdjustmentListener(adjListener);

        ic = null;
    }

    // updates the view box rectangle position based on the ic
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

    // scrolls the image component based on the view box position
    private void scrollIC() {
        double scaleX = (double) viewWidth / thumbWidth;
        double scaleY = (double) viewHeight / thumbHeight;

        int bigX = (int) (viewBoxRect.x * scaleX);
        int bigY = (int) (viewBoxRect.y * scaleY);
        int bigWidth = (int) (viewBoxRect.width * scaleX);
        int bigHeight = (int) (viewBoxRect.height * scaleY);

        ic.scrollRectToVisible(new Rectangle(bigX, bigY, bigWidth, bigHeight));
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (ic == null) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;

        checkerBoardPainter.paint(g2, null, thumbWidth, thumbHeight);

        AffineTransform origTX = g2.getTransform();

        g2.scale(imgScalingRatio, imgScalingRatio);
        g2.drawImage(ic.getComp().getCompositeImage(), 0, 0, null);
        g2.setTransform(origTX);

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
            if (viewBoxRect.contains(point) && ic != null) {
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

            Point eventPoint = e.getPoint();
            int relX = eventPoint.x - dragStartPoint.x;
            int relY = eventPoint.y - dragStartPoint.y;

            int newBoxX = origRectLoc.x + relX;
            int newBoxY = origRectLoc.y + relY;

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
            scrollIC();
        }
    }

    private void reCalcScaling(ImageComponent ic, int width, int height) {
        Canvas canvas = ic.getCanvas();
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
        resetCursor(e.getX(), e.getY());
    }

    private void resetCursor(int x, int y) {
        if (viewBoxRect.contains(x, y)) {
            setCursor(Cursors.HAND);
        } else {
            setCursor(Cursors.DEFAULT);
        }
    }

    @Override
    public void noOpenImageAnymore() {
        releaseImage();
        repaint();
    }

    @Override
    public void newImageOpened(Composition comp) {
        // not necessary to implement, since activeImageHasChanged is also called
    }

    @Override
    public void activeImageHasChanged(ImageComponent oldIC, ImageComponent newIC) {
        recalculateSize(newIC, true, true, false);
    }

    // called when this navigator instance is no longer needed
    private void dispose() {
        ImageComponents.removeActiveImageChangeListener(this);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(preferredWidth, preferredHeight);
    }
}


