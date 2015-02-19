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

package pixelitor.tools;

import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.Composition;
import pixelitor.ImageComponent;
import pixelitor.ImageComponents;
import pixelitor.ImageDisplay;
import pixelitor.filters.gui.RangeParam;
import pixelitor.layers.ImageLayer;
import pixelitor.utils.ImageSwitchListener;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.SliderSpinner;

import javax.swing.*;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;

import static pixelitor.Composition.ImageChangeActions.HISTOGRAM;

/**
 * Abstract superclass for tools like brush or erase.
 */
public abstract class AbstractBrushTool extends Tool implements ImageSwitchListener {
    private static final int MIN_BRUSH_RADIUS = 1;
    public static final int MAX_BRUSH_RADIUS = 100;
    public static final int DEFAULT_BRUSH_RADIUS = 10;

    boolean respectSelection = true; // false while tracing a selection

    private JComboBox<BrushType> typeSelector;

    Graphics2D drawingGraphics;
    private final RangeParam brushRadiusParam = new RangeParam("Radius", MIN_BRUSH_RADIUS, MAX_BRUSH_RADIUS, DEFAULT_BRUSH_RADIUS);

//    private Composition comp;


    private final EnumComboBoxModel<Symmetry> symmetryModel = new EnumComboBoxModel<>(Symmetry.class);

//    private Brush brush = BrushType.IDEAL.getBrush();
    Brushes brushes = new Brushes(BrushType.values()[0]);

    private int previousMouseX = 0;
    private int previousMouseY = 0;

    private boolean firstMouseDown = true; // for the first click don't draw lines even if it is a shift-click

    AbstractBrushTool(char activationKeyChar, String name, String iconFileName, String toolMessage) {
        super(activationKeyChar, name, iconFileName, toolMessage,
                Cursor.getDefaultCursor(), true, true, false, ClipStrategy.IMAGE_ONLY);
        ImageComponents.addImageSwitchListener(this);
    }

    @Override
    public void initSettingsPanel() {
        toolSettingsPanel.add(new JLabel("Type:"));
        typeSelector = new JComboBox<>(BrushType.values());
        toolSettingsPanel.add(typeSelector);
        typeSelector.addActionListener(e -> {
            BrushType brushType = (BrushType) typeSelector.getSelectedItem();
            brushes.brushTypeChanged(brushType);
        });

        // make sure all values are visible without a scrollbar
        typeSelector.setMaximumRowCount(BrushType.values().length);

        SliderSpinner brushSizeSpinner = new SliderSpinner(brushRadiusParam, SliderSpinner.TextPosition.WEST, false);
        toolSettingsPanel.add(brushSizeSpinner);

        toolSettingsPanel.add(new JLabel("Mirror:"));

        @SuppressWarnings("unchecked")
        JComboBox<Symmetry> symmetryCombo = new JComboBox<>(symmetryModel);

        toolSettingsPanel.add(symmetryCombo);
    }

    @Override
    public void toolMousePressed(MouseEvent e, ImageDisplay ic) {
        boolean withLine = !firstMouseDown && e.isShiftDown();

        Paint p;
        int button = e.getButton();

        if (button == MouseEvent.BUTTON3) {
            p = FgBgColorSelector.getBG();
        } else if (button == MouseEvent.BUTTON2) {
            // we never get here because isAltDown is always true for middle-button events, even if Alt is not pressed
            Color fg = FgBgColorSelector.getFG();
            Color bg = FgBgColorSelector.getBG();
            if (e.isControlDown()) {
                p = ImageUtils.getHSBAverageColor(fg, bg);
            } else {
                p = ImageUtils.getRGBAverageColor(fg, bg);
            }
        } else {
            p = FgBgColorSelector.getFG();
        }

        int x = userDrag.getStartX();
        int y = userDrag.getStartY();
        drawTo(ic.getComp(), p, x, y, withLine);
        firstMouseDown = false;

        if (withLine) {
            brushes.updateAffectedCoordinates(x, y);
        } else {
            brushes.initAffectedCoordinates(x, y);
        }
    }

    @Override
    public void toolMouseDragged(MouseEvent e, ImageDisplay ic) {
        int x = userDrag.getEndX();
        int y = userDrag.getEndY();

        // at this point x and y are already scaled according to the zoom level
        // (unlike e.getX(), e.getY())

        drawTo(ic.getComp(), null, x, y, false);
    }

    @Override
    public void toolMouseReleased(MouseEvent e, ImageDisplay ic) {
        finishBrushStroke(ic.getComp());
    }

    abstract BufferedImage getFullUntouchedImage(Composition comp);

    abstract void mergeTmpLayer(Composition comp);

    private void finishBrushStroke(Composition comp) {
        ToolAffectedArea affectedArea = new ToolAffectedArea(comp, brushes.getRectangleAffectedByBrush(), false);
        saveSubImageForUndo(getFullUntouchedImage(comp), affectedArea);
        mergeTmpLayer(comp);
        if(drawingGraphics != null) {
            drawingGraphics.dispose();
        }
        drawingGraphics = null;

        comp.imageChanged(HISTOGRAM);
    }

    public void drawBrushStrokeProgrammatically(Composition comp, Point startingPoint, Point endPoint) {
        int startX = startingPoint.x;
        int startY = startingPoint.y;
        int endX = endPoint.x;
        int endY = endPoint.y;

        Color c = FgBgColorSelector.getFG();
        drawTo(comp, c, startX, startY, false);
        drawTo(comp, c, endX, endY, false);
        finishBrushStroke(comp);
    }

    /**
     * Creates the global Graphics2D object drawingGraphics.
     */
    abstract void initDrawingGraphics(ImageLayer layer);

    /**
     * Called from mousePressed, mouseDragged, and drawBrushStroke
     */
    private void drawTo(Composition comp, Paint p, int x, int y, boolean connectClickWithLine) {
        // TODO these two variables could be initialized outside this function
        setupDrawingRadius();
        Symmetry currentSymmetry = symmetryModel.getSelectedItem();

        if(drawingGraphics == null) {
            if(!connectClickWithLine) {
                brushes.reset();
            }
            brushes.setComp(comp);

            ImageLayer imageLayer = (ImageLayer) comp.getActiveLayer();
            initDrawingGraphics(imageLayer);
            setupGraphics(drawingGraphics, p);
            drawingGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (connectClickWithLine) {
                currentSymmetry.onNewMousePoint(brushes, previousMouseX, previousMouseY, x, y);
            } else {
                // TODO this is not drag start
                currentSymmetry.onDragStart(brushes, x, y);
            }

        } else {
            currentSymmetry.onNewMousePoint(brushes, previousMouseX, previousMouseY, x, y);
        }

        previousMouseX = x;
        previousMouseY = y;
    }

    private void setupDrawingRadius() {
        int value = brushRadiusParam.getValue();

        // because of a JDK bug, sometimes it is possible to drag the slider to negative values
        if (value < MIN_BRUSH_RADIUS) {
            value = MIN_BRUSH_RADIUS;
            brushRadiusParam.setValue(MIN_BRUSH_RADIUS);
        }

        brushes.setRadius(value);
    }

    protected abstract void setupGraphics(Graphics2D g, Paint p);

    @Override
    protected void toolStarted() {
        super.toolStarted();
        resetState();
    }

    @Override
    public void noOpenImageAnymore() {

    }

    @Override
    public void newImageOpened() {
        resetState();
    }

    @Override
    public void activeImageHasChanged(ImageComponent oldIC, ImageComponent newIC) {
        resetState();

    }

    private void resetState() {
        firstMouseDown = true;
        respectSelection = true;
    }

    /**
     * Traces the given shape and paint with the current brush tool
     */
    public void trace(Composition comp, Shape shape) {
        brushes.setComp(comp); // just to be sure

        setupDrawingRadius();
        try {
            respectSelection = false;

            ImageLayer imageLayer = (ImageLayer) comp.getActiveLayer();
            initDrawingGraphics(imageLayer);
            setupGraphics(drawingGraphics, FgBgColorSelector.getFG());

            int startingX = 0;
            int startingY = 0;

            FlatteningPathIterator fpi = new FlatteningPathIterator(shape.getPathIterator(null), 1.0);
            float[] coords = new float[2];
            while (!fpi.isDone()) {
                int type = fpi.currentSegment(coords);
                int x = (int) coords[0];
                int y = (int) coords[1];
                brushes.updateAffectedCoordinates(x, y);

                switch(type) {
                    case PathIterator.SEG_MOVETO:
                        startingX = x;
                        startingY = y;

                        previousMouseX = x;
                        previousMouseY = y;

                        break;
                    case PathIterator.SEG_LINETO:
                        brushes.onNewMousePoint(0, previousMouseX, previousMouseY, x, y);

                        previousMouseX = x;
                        previousMouseY = y;

                        break;
                    case PathIterator.SEG_CLOSE:
                        brushes.onNewMousePoint(0, previousMouseX, previousMouseY, startingX, startingY);
                        break;
                    default:
                        throw new IllegalArgumentException("type = " + type);
                }

                fpi.next();
            }
            finishBrushStroke(comp);
        } finally {
            resetState();
        }
    }

    public void increaseBrushSize() {
        brushRadiusParam.increaseValue();
    }

    public void decreaseBrushSize() {
        brushRadiusParam.decreaseValue();
    }
}
