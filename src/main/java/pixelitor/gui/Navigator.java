/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.Composition;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.menus.view.ZoomMenu;
import pixelitor.utils.ImageSwitchListener;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;

public class Navigator extends JComponent implements MouseListener, MouseMotionListener, ImageSwitchListener {
    private static final BasicStroke STROKE = new BasicStroke(3);
    private static final CheckerboardPainter checkerBoardPainter = ImageUtils.createCheckerboardPainter();
    private static final int DEFAULT_SIZE = 300;

    private ImageComponent ic; // can be null if all images are closed

    private boolean dragging = false;
    private double scaling;

    private Rectangle redRect;
    private Point dragStartPoint;
    private Point origRectLoc; // the red rectangle location before starting the drag
    private int thumbWidth;
    private int thumbHeight;
    private int viewWidth;
    private int viewHeight;
    private JScrollPane scrollPane;
    private final AdjustmentListener adjListener;
    private static JDialog dialog;

    private Navigator(ImageComponent ic) {
        adjListener = e ->
                SwingUtilities.invokeLater(this::updateRedRectanglePosition);

        refreshSizeCalc(ic, true, true, true);

        addMouseListener(this);
        addMouseMotionListener(this);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ImageComponents.addImageSwitchListener(this);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (Navigator.this.ic != null) { // it is null if all images are closed
                    refreshSizeCalc(Navigator.this.ic, false, false, true);
                }
            }
        });

        addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                if (e.getWheelRotation() < 0) { // up, away from the user
                    this.ic.increaseZoom();
                } else {  // down, towards the user
                    this.ic.decreaseZoom();
                }
            }
        });

        ZoomMenu.setupZoomKeys(this);
    }

    public static void showInDialog(PixelitorWindow pw) {
        ImageComponent ic = ImageComponents.getActiveIC();
        Navigator navigator = new Navigator(ic);

        if(dialog != null && dialog.isVisible()) {
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
                .show();
    }

    public void refreshSizeCalc(ImageComponent ic,
                                boolean newIC, boolean newICSize, boolean newOwnSize) {
        if (this.ic != null && newIC) {
            releaseImage();
        }

        this.ic = ic;
        scrollPane = ic.getInternalFrame().getScrollPane();

        if (newIC) {
            reCalcScaling(ic, DEFAULT_SIZE, DEFAULT_SIZE);
        } else if (newICSize || newOwnSize) {
            reCalcScaling(ic, getWidth(), getHeight());
        }

        setPreferredSize(new Dimension(thumbWidth, thumbHeight));

        updateRedRectanglePosition();

        if (newIC) {
            ic.setNavigator(this);
            scrollPane.getHorizontalScrollBar().addAdjustmentListener(adjListener);
            scrollPane.getVerticalScrollBar().addAdjustmentListener(adjListener);
        }

        if (newICSize) {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.pack();
            }
        }
    }

    private void releaseImage() {
        ic.setNavigator(null);
        scrollPane.getHorizontalScrollBar().removeAdjustmentListener(adjListener);
        scrollPane.getVerticalScrollBar().removeAdjustmentListener(adjListener);

        ic = null;
    }

    // updates the red rectangle position based on the ic
    private void updateRedRectanglePosition() {
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

        int redX = (int) (viewRect.x * scaleX);
        int redY = (int) (viewRect.y * scaleY);
        int redWidth = (int) (viewRect.width * scaleX);
        int redHeight = (int) (viewRect.height * scaleY);

        redRect = new Rectangle(redX, redY, redWidth, redHeight);
        repaint();
    }

    // scrolls the image component based on the red rectangle position
    private void scrollIC() {
        double scaleX = (double) viewWidth / thumbWidth;
        double scaleY = (double) viewHeight / thumbHeight;

        int bigX = (int) (redRect.x * scaleX);
        int bigY = (int) (redRect.y * scaleY);
        int bigWidth = (int) (redRect.width * scaleX);
        int bigHeight = (int) (redRect.height * scaleY);

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
        g2.scale(scaling, scaling);
        g2.drawImage(ic.getComp().getCompositeImage(), 0, 0, null);
        g2.setTransform(origTX);

        g2.setStroke(STROKE);
        g2.setColor(Color.RED);
        g2.draw(redRect);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Point point = e.getPoint();
        if (redRect.contains(point) && ic != null) {
            dragStartPoint = point;
            origRectLoc = redRect.getLocation();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragStartPoint = null;
        dragging = false;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (dragStartPoint != null) {
            dragging = true;

            Point eventPoint = e.getPoint();
            int relX = eventPoint.x - dragStartPoint.x;
            int relY = eventPoint.y - dragStartPoint.y;


            int newRedX = origRectLoc.x + relX;
            int newRedY = origRectLoc.y + relY;

            // make sure that the red rectangle does not leave the thumb
            if (newRedX < 0) {
                newRedX = 0;
            }
            if (newRedY < 0) {
                newRedY = 0;
            }
            if (newRedX + redRect.width > thumbWidth) {
                newRedX = thumbWidth - redRect.width;
            }
            if (newRedY + redRect.height > thumbHeight) {
                newRedY = thumbHeight - redRect.height;
            }

            updateRedRectLocation(newRedX, newRedY);
        }
    }

    private void updateRedRectLocation(int newRedX, int newRedY) {
        if (newRedX != redRect.x || newRedY != redRect.y) {
            redRect.setLocation(newRedX, newRedY);
            repaint();
            scrollIC();
        }
    }

    private void reCalcScaling(ImageComponent ic, int width, int height) {
        Canvas canvas = ic.getCanvas();
        int imgWidth = canvas.getWidth();
        int imgHeight = canvas.getHeight();

        double xScaling = width / (double) imgWidth;
        double yScaling = height / (double) imgHeight;

        scaling = Math.min(xScaling, yScaling);
        thumbWidth = (int) (imgWidth * scaling);
        thumbHeight = (int) (imgHeight * scaling);
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
        // not interested
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
        refreshSizeCalc(newIC, true, true, false);
    }

    // called when this navigator instance is no longer needed
    private void dispose() {
        ImageComponents.removeImageSwitchListener(this);
    }
}


