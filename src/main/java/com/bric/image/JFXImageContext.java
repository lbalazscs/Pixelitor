/*
 * @(#)JFXImageContext.java
 *
 * $Date: 2014-11-27 07:50:51 +0100 (Cs, 27 nov. 2014) $
 *
 * Copyright (c) 2014 by Jeremy Wood.
 * All rights reserved.
 *
 * The copyright of this software is owned by Jeremy Wood. 
 * You may not use, copy or modify this software, except in  
 * accordance with the license agreement you entered into with  
 * Jeremy Wood. For details see accompanying license terms.
 * 
 * This software is probably, but not necessarily, discussed here:
 * https://javagraphics.java.net/
 * 
 * That site should also contain the most recent official version
 * of this software.  (See the SVN repository for more details.)
 */
package com.bric.image;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/** This is an <code>ImageContext</code> that uses JavaFX.
 */
public class JFXImageContext extends ImageContext {

    private boolean realized = false;
    private BufferedImage buf;
    private Canvas canvas;
    private GraphicsContext context;
    private WritableImage currentImage;
    private WritableImage tmpOut;
    private SnapshotParameters snapshotParameters = new SnapshotParameters();
    private boolean autoFlush = true;

    private static boolean isInitJavaFX;

    public JFXImageContext(BufferedImage bi) {

        initJavaFX();

        snapshotParameters.setFill(new Color(1, 1, 1, 0));

        buf = bi;
        canvas = new Canvas(bi.getWidth(), bi.getHeight());
        context = canvas.getGraphicsContext2D();
        realized = true;

    }

    private static void initJavaFX() {
        if (!isInitJavaFX) {
            isInitJavaFX = true;
            new JFXPanel(); // initializes JavaFX environment 
        }
    }

    private void ensureRealized() {
        if (!realized) {
            throw new IllegalStateException("This context has been disposed.");
        }
    }

    /** Changes auto flush behavior. 
     * <p>
     * If you set auto flush to false, you need to invoke {@link #flush} in 
     * order to copy the image data into the buffered image.
     * <p>
     * Setting auto flush to false greatly improves performance, because
     * the image data does not need be converted from JavaFX to AWT
     * after each drawing call.
     * 
     * <p>Note: autoflush is active by default. When I turn it off and render
     * two unique images, then only one of those images is eventually rendered
     * to the target BufferedImage.
     * 
     * @param b the new auto flush state.
     */
    public void setAutoFlush(boolean b) {
        this.autoFlush = b;
    }

    public boolean isAutoFlush() {
        return autoFlush;
    }

    /** Draw an image to this Graphics3D.
     * <p>This respects the interpolation rendering hints. When the
     * interpolation hint is missing, this will also consult the antialiasing
     * hint or the render hint. The bilinear hint is used by default.
     * <p>This uses a source over composite.
     * <p>This sets the current image.
     *
     * <p>Note: if you want to draw the same image multiple times, it is faster
     * to use the following approach:
     * <pre>
     * GraphicsFX g = ...;
     * g.setAutoFlush(false);
     * g.setImage(img);
     * for (...) {
     *    g.drawImage(topLeft,topRight,bottomRight,bottomLeft);
     * }
     * g.flush();
     * </pre>
     * instead.
     * 
     * 
     * @param img the image to draw.
     * @param topLeft where the top-left corner of this image will be painted.
     * @param topRight where the top-right corner of this image will be painted.
     * @param bottomRight where the bottom-right corner of this image will be painted.
     * @param bottomLeft where the bottom-left corner of this image will be painted.
     */
    public void drawImage(BufferedImage img, Point2D topLeft, Point2D topRight, Point2D bottomRight, Point2D bottomLeft) {
        setImage(img);
        drawImage(topLeft, topRight, bottomRight, bottomLeft);
    }

    /** Draw an image to this Graphics3D.
     * <p>This respects the interpolation rendering hints. When the
     * interpolation hint is missing, this will also consult the antialiasing
     * hint or the render hint. The bilinear hint is used by default.
     * <p>This uses a source over composite.
     * <p>This sets the current image.
     *
     * <p>Note: if you want to draw the same image multiple times, it is faster
     * to use the following approach:
     * <pre>
     * GraphicsFX g = ...;
     * g.setAutoFlush(false);
     * g.setImage(img);
     * for (...) {
     *    g.drawImage(topLeft,topRight,bottomRight,bottomLeft);
     * }
     * g.flush();
     * </pre>
     * instead.
     * 
     * 
     * @param img the image to draw.
     * @param topLeft where the top-left corner of this image will be painted.
     * @param topRight where the top-right corner of this image will be painted.
     * @param bottomRight where the bottom-right corner of this image will be painted.
     * @param bottomLeft where the bottom-left corner of this image will be painted.
     */
    public void drawImage(WritableImage img, Point2D topLeft, Point2D topRight, Point2D bottomRight, Point2D bottomLeft) {
        setImage(img);
        drawImage(topLeft, topRight, bottomRight, bottomLeft);
    }

    /** Sets the current image for consecutively drawing the same image. 
     * 
     * @param img the current image to paint.
     */
    public void setImage(BufferedImage img) {
        currentImage = toImage(img, currentImage);
    }

    /** Sets the current image for consecutively drawing the same image. 
     * 
     * @param img the current image to paint.
     */
    public void setImage(WritableImage img) {
        currentImage = img;
    }

    /** Converts the provided BufferedImage into a JavaFX Image.
     * @param img The BufferedImage
     * @param reuseMe Attempts to reuse this image. Specify null to force creation
     *            of a new image.
     * @return The converted image. May be the same instance as {@code reuseMe}.
     */
    public WritableImage toImage(BufferedImage img, WritableImage reuseMe) {
        return SwingFXUtils.toFXImage(img, reuseMe);
    }

    /** Draws the current image. 
     * @param topLeft where the top-left corner of the current image should be rendered
     * @param topRight where the top-right corner of the current image should be rendered
     * @param bottomRight where the bottom-right corner of the current image should be rendered
     * @param bottomLeft where the bottom-left corner of the current image should be rendered
     */
    public void drawImage(Point2D topLeft, Point2D topRight, Point2D bottomRight, Point2D bottomLeft) {
        ensureRealized();

        PerspectiveTransform t = new PerspectiveTransform();
        t.setUlx(topLeft.getX());
        t.setUly(topLeft.getY());
        t.setUrx(topRight.getX());
        t.setUry(topRight.getY());
        t.setLrx(bottomRight.getX());
        t.setLry(bottomRight.getY());
        t.setLlx(bottomLeft.getX());
        t.setLly(bottomLeft.getY());
        context.setEffect(t);
        context.drawImage(currentImage, 0, 0);

        if (autoFlush) {
            flush();
        }
    }

    /** Renders the current graphics state into the BufferedImage. */
    public void flush() {
        FutureTask<Void> r = new FutureTask<>(new Runnable() {
            @Override
            public void run() {
                tmpOut = canvas.snapshot(snapshotParameters, tmpOut);

                BufferedImage newBuf = SwingFXUtils.fromFXImage(tmpOut, buf);
                if (newBuf != buf) {
                	//Graphics2D g = buf.createGraphics();
                	//g.drawImage(newBuf, 0, 0, null);
                	//g.dispose();
//                	System.err.println(newBuf.getWidth()+"x"+newBuf.getHeight()+", "+Reflection.nameStaticField(BufferedImage.class, newBuf.getType()));
//                	System.err.println(buf.getWidth()+"x"+buf.getHeight()+", "+Reflection.nameStaticField(BufferedImage.class, buf.getType()));
                    throw new InternalError("Allocated a new buffer instead of reusing the existing one.");
                }
            }

        }, null);
        Platform.runLater(r);
        try {
            r.get();
        } catch (InterruptedException | ExecutionException ex) {
        	InternalError e = new InternalError();
        	e.initCause(ex);
        	throw e;
        }
    }

    /** Commit all changes back to the BufferedImage this context paints to.
     */
    public void dispose() {
        if (realized) {
        	flush();
            realized = false;
            canvas = null;
            context = null;
            buf = null;
            currentImage = null;
        }
    }

    public void setRenderingHint(RenderingHints.Key KEY_INTERPOLATION, Object VALUE_INTERPOLATION_NEAREST_NEIGHBOR) {
        /// ignore this for now
    }

}
