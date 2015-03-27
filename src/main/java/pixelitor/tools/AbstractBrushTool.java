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
import pixelitor.tools.brushes.Brush;
import pixelitor.tools.brushes.BrushAffectedArea;
import pixelitor.tools.brushes.SymmetryBrush;
import pixelitor.utils.ImageSwitchListener;
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

    private final EnumComboBoxModel<Symmetry> symmetryModel = new EnumComboBoxModel<>(Symmetry.class);

    //    Brushes brushes;
    protected Brush brush;
    private final BrushAffectedArea brushAffectedArea = new BrushAffectedArea();

    private boolean firstMouseDown = true; // for the first click don't draw lines even if it is a shift-click

    AbstractBrushTool(char activationKeyChar, String name, String iconFileName, String toolMessage) {
        super(activationKeyChar, name, iconFileName, toolMessage,
                Cursor.getDefaultCursor(), true, true, false, ClipStrategy.IMAGE_ONLY);
        ImageComponents.addImageSwitchListener(this);
        initBrush();
    }

    protected void initBrush() {
//        brushes = new Brushes(BrushType.values()[0], getCurrentSymmetry());
        brush = new SymmetryBrush(BrushType.values()[0], getCurrentSymmetry(), brushAffectedArea);
    }

    Symmetry getCurrentSymmetry() {
        return symmetryModel.getSelectedItem();
    }

    @Override
    public void initSettingsPanel() {
        toolSettingsPanel.add(new JLabel("Type:"));
        typeSelector = new JComboBox<>(BrushType.values());
        typeSelector.setName("brushTypeSelector");
        toolSettingsPanel.add(typeSelector);
        typeSelector.addActionListener(e -> {
            BrushType brushType = (BrushType) typeSelector.getSelectedItem();
            ((SymmetryBrush) brush).brushTypeChanged(brushType);
        });

        // make sure all values are visible without a scrollbar
        typeSelector.setMaximumRowCount(BrushType.values().length);

        addSizeSelector();

        toolSettingsPanel.add(new JLabel("Mirror:"));

        @SuppressWarnings("unchecked")
        JComboBox<Symmetry> symmetryCombo = new JComboBox<>(symmetryModel);
        symmetryCombo.setName("symmetrySelector");
        symmetryCombo.addActionListener(e -> ((SymmetryBrush) brush).symmetryChanged(getCurrentSymmetry()));

        toolSettingsPanel.add(symmetryCombo);
    }

    protected void addSizeSelector() {
        SliderSpinner brushSizeSelector = new SliderSpinner(brushRadiusParam, SliderSpinner.TextPosition.WEST, false);
        toolSettingsPanel.add(brushSizeSelector);
    }

    @Override
    public void toolMousePressed(MouseEvent e, ImageDisplay ic) {
        Paint p = getPaint(e);

        boolean withLine = withLine(e);
        int x = userDrag.getStartX();
        int y = userDrag.getStartY();
        drawTo(ic.getComp(), p, x, y, withLine);
        firstMouseDown = false;

        if (withLine) {
            brushAffectedArea.updateAffectedCoordinates(x, y);
        } else {
            brushAffectedArea.initAffectedCoordinates(x, y);
        }
    }

    protected boolean withLine(MouseEvent e) {
        return !firstMouseDown && e.isShiftDown();
    }

    // only the Brush Tool returns non-null here
    protected abstract Paint getPaint(MouseEvent e);

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
        ToolAffectedArea affectedArea = new ToolAffectedArea(comp, brushAffectedArea.getRectangleAffectedByBrush(brushRadiusParam.getValue()), false);
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
    abstract void initDrawingGraphics(Composition comp, ImageLayer layer);

    /**
     * Called from mousePressed, mouseDragged, and drawBrushStroke
     */
    private void drawTo(Composition comp, Paint p, int x, int y, boolean connectClickWithLine) {
        setupDrawingRadius();
//        Symmetry currentSymmetry = getCurrentSymmetry();

        if(drawingGraphics == null) { // a new brush stroke has to be initialized
//            if(!connectClickWithLine) {
//                brushes.reset();
//            }

            ImageLayer imageLayer = (ImageLayer) comp.getActiveLayer();
            initDrawingGraphics(comp, imageLayer);
            setupGraphics(drawingGraphics, p);
            drawingGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (connectClickWithLine) {
                brush.onNewMousePoint(x, y);
            } else {
                brush.onDragStart(x, y);
            }

        } else {
            brush.onNewMousePoint(x, y);
        }
    }

    private void setupDrawingRadius() {
        int value = brushRadiusParam.getValue();

        // because of a JDK bug, sometimes it is possible to drag the slider to negative values
        if (value < MIN_BRUSH_RADIUS) {
            value = MIN_BRUSH_RADIUS;
            brushRadiusParam.setValue(MIN_BRUSH_RADIUS);
        }

        brush.setRadius(value);
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
        setupDrawingRadius();
        try {
            respectSelection = false;

            ImageLayer imageLayer = (ImageLayer) comp.getActiveLayer();
            initDrawingGraphics(comp, imageLayer);
            setupGraphics(drawingGraphics, FgBgColorSelector.getFG());

            doTraceAfterSetup(shape);

            finishBrushStroke(comp);
        } finally {
            resetState();
        }
    }

    private void doTraceAfterSetup(Shape shape) {
        int startingX = 0;
        int startingY = 0;

        FlatteningPathIterator fpi = new FlatteningPathIterator(shape.getPathIterator(null), 1.0);
        float[] coords = new float[2];
        while (!fpi.isDone()) {
            int type = fpi.currentSegment(coords);
            int x = (int) coords[0];
            int y = (int) coords[1];
            brushAffectedArea.updateAffectedCoordinates(x, y);

            switch (type) {
                case PathIterator.SEG_MOVETO:
                    startingX = x;
                    startingY = y;

                    brush.onDragStart(x, y);

                    break;
                case PathIterator.SEG_LINETO:
                    brush.onNewMousePoint(x, y);

                    break;
                case PathIterator.SEG_CLOSE:
                    brush.onNewMousePoint(startingX, startingY);
                    break;
                default:
                    throw new IllegalArgumentException("type = " + type);
            }

            fpi.next();
        }
    }

    public void increaseBrushSize() {
        brushRadiusParam.increaseValue();
    }

    public void decreaseBrushSize() {
        brushRadiusParam.decreaseValue();
    }

    @Override
    protected boolean doColorPickerForwarding() {
        return true;
    }
}
