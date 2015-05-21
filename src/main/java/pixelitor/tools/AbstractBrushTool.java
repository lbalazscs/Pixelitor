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
import pixelitor.PixelitorWindow;
import pixelitor.filters.gui.FilterGUIComponent;
import pixelitor.filters.gui.RangeParam;
import pixelitor.layers.ImageLayer;
import pixelitor.tools.brushes.Brush;
import pixelitor.tools.brushes.BrushAffectedArea;
import pixelitor.tools.brushes.SymmetryBrush;
import pixelitor.utils.ImageSwitchListener;
import pixelitor.utils.OKDialog;
import pixelitor.utils.SliderSpinner;

import javax.swing.*;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.Composition.ImageChangeActions.HISTOGRAM;
import static pixelitor.utils.SliderSpinner.TextPosition.WEST;

/**
 * Abstract superclass for tools like brush, erase, clone.
 */
public abstract class AbstractBrushTool extends Tool implements ImageSwitchListener {
    private static final int MIN_BRUSH_RADIUS = 1;
    public static final int MAX_BRUSH_RADIUS = 100;
    public static final int DEFAULT_BRUSH_RADIUS = 10;

    boolean respectSelection = true; // false while tracing a selection

    private JComboBox<BrushType> typeSelector;

    protected Graphics2D graphics;
    private final RangeParam brushRadiusParam = new RangeParam("Radius", MIN_BRUSH_RADIUS, MAX_BRUSH_RADIUS, DEFAULT_BRUSH_RADIUS, false, WEST);

    private final EnumComboBoxModel<Symmetry> symmetryModel = new EnumComboBoxModel<>(Symmetry.class);

    protected Brush brush;
    private SymmetryBrush symmetryBrush;
    protected BrushAffectedArea brushAffectedArea;

    private boolean firstMouseDown = true; // for the first click don't draw lines even if it is a shift-click
    private JButton brushSettingsButton;

    AbstractBrushTool(char activationKeyChar, String name, String iconFileName, String toolMessage, Cursor cursor) {
        super(activationKeyChar, name, iconFileName, toolMessage,
                cursor, true, true, false, ClipStrategy.IMAGE_ONLY);
        ImageComponents.addImageSwitchListener(this);
        initBrushVariables();
    }

    protected void initBrushVariables() {
        symmetryBrush = new SymmetryBrush(
                this, BrushType.values()[0], getSymmetry(), getRadius());
        brush = symmetryBrush;
        brushAffectedArea = symmetryBrush.getAffectedArea();
    }

    protected void addTypeSelector() {
        typeSelector = new JComboBox<>(BrushType.values());
        settingsPanel.addWithLabel("Type:", typeSelector, "brushTypeSelector");
        typeSelector.addActionListener(e -> {
            BrushType brushType = (BrushType) typeSelector.getSelectedItem();
            symmetryBrush.brushTypeChanged(brushType, getRadius());

            brushRadiusParam.setEnabled(brushType.sizeCanBeSet(), FilterGUIComponent.EnabledReason.APP_LOGIC);

            brushSettingsButton.setEnabled(brushType.hasSettings());
        });

        // make sure all values are visible without a scrollbar
        typeSelector.setMaximumRowCount(BrushType.values().length);
    }

    protected void addSizeSelector() {
        SliderSpinner brushSizeSelector = (SliderSpinner) brushRadiusParam.createGUI();
        settingsPanel.add(brushSizeSelector);
        brushRadiusParam.setAdjustmentListener(this::setupDrawingRadius);
        setupDrawingRadius();
    }

    protected void addSymmetryCombo() {
        JComboBox<Symmetry> symmetryCombo = new JComboBox<>(symmetryModel);
        settingsPanel.addWithLabel("Mirror:", symmetryCombo, "symmetrySelector");
        symmetryCombo.addActionListener(e -> symmetryBrush.symmetryChanged(
                getSymmetry(), getRadius()));
    }

    protected void addBrushSettingsButton() {
        brushSettingsButton = settingsPanel.addButton("Brush Settings",
                e -> {
                    BrushType brushType = (BrushType) typeSelector.getSelectedItem();
                    JPanel p = brushType.getSettingsPanel(this);
                    new OKDialog(PixelitorWindow.getInstance(), "Brush Settings", p);
                });

        brushSettingsButton.setEnabled(false);
    }

    @Override
    public void mousePressed(MouseEvent e, ImageDisplay ic) {
        boolean withLine = withLine(e);
        int x = userDrag.getStartX();
        int y = userDrag.getStartY();

        drawTo(ic.getComp(), x, y, withLine);
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

    @Override
    public void mouseDragged(MouseEvent e, ImageDisplay ic) {
        int x = userDrag.getEndX();
        int y = userDrag.getEndY();

        // at this point x and y are already scaled according to the zoom level
        // (unlike e.getX(), e.getY())

        drawTo(ic.getComp(), x, y, false);
    }

    @Override
    public void mouseReleased(MouseEvent e, ImageDisplay ic) {
        finishBrushStroke(ic.getComp());
    }

    /**
     * Returns the original (untouched) image for undo
     */
    abstract BufferedImage getOriginalImage(Composition comp);

    abstract void mergeTmpLayer(Composition comp);

    private void finishBrushStroke(Composition comp) {
        int radius = getRadius();
        ToolAffectedArea affectedArea = new ToolAffectedArea(comp,
                brushAffectedArea.getRectangleAffectedByBrush(radius), false);
        saveSubImageForUndo(getOriginalImage(comp), affectedArea);

        mergeTmpLayer(comp);

        if (graphics != null) {
            graphics.dispose();
        }
        graphics = null;

        comp.imageChanged(HISTOGRAM);
    }

    public void drawBrushStrokeProgrammatically(Composition comp, Point startingPoint, Point endPoint) {
        int startX = startingPoint.x;
        int startY = startingPoint.y;
        int endX = endPoint.x;
        int endY = endPoint.y;

//        setupDrawingRadius();
        drawTo(comp, startX, startY, false);
        drawTo(comp, endX, endY, false);
        finishBrushStroke(comp);
    }

    /**
     * Creates the global Graphics2D object graphics.
     */
    abstract void createGraphicsForNewBrushStroke(Composition comp, ImageLayer layer);

    /**
     * Called from mousePressed, mouseDragged, and drawBrushStroke
     */
    private void drawTo(Composition comp, int x, int y, boolean connectClickWithLine) {
        if (graphics == null) { // a new brush stroke has to be initialized
            ImageLayer imageLayer = (ImageLayer) comp.getActiveLayer();
            createGraphicsForNewBrushStroke(comp, imageLayer);
            graphics.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

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
//        System.out.println("AbstractBrushTool::setupDrawingRadius: CALLED " +
//                "for " + this.getClass().getName());

        brush.setRadius(getRadius());
    }

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
     * Traces the given shape with the current brush tool
     */
    public void trace(Composition comp, Shape shape) {
//        setupDrawingRadius();
        try {
            respectSelection = false;

            ImageLayer imageLayer = (ImageLayer) comp.getActiveLayer();
            createGraphicsForNewBrushStroke(comp, imageLayer);

            doTraceAfterSetup(shape);

            finishBrushStroke(comp);
        } finally {
            resetState();
        }
    }

    private void doTraceAfterSetup(Shape shape) {
        int startingX = 0;
        int startingY = 0;

        PathIterator fpi = new FlatteningPathIterator(shape.getPathIterator(null), 1.0);
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

    protected Symmetry getSymmetry() {
        return symmetryModel.getSelectedItem();
    }

    protected int getRadius() {
        int value = brushRadiusParam.getValue();

        // because of a JDK bug, sometimes it is possible to drag the slider to negative values
        if (value < MIN_BRUSH_RADIUS) {
            value = MIN_BRUSH_RADIUS;
            brushRadiusParam.setValue(MIN_BRUSH_RADIUS);
        }

        return value;
    }

    @Override
    protected boolean doColorPickerForwarding() {
        return true;
    }
}
