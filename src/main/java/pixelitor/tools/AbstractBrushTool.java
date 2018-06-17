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

package pixelitor.tools;

import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.filters.gui.FilterSetting;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.OKDialog;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.layers.Drawable;
import pixelitor.tools.brushes.Brush;
import pixelitor.tools.brushes.BrushAffectedArea;
import pixelitor.tools.brushes.SymmetryBrush;
import pixelitor.utils.ActiveImageChangeListener;
import pixelitor.utils.VisibleForTesting;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.Composition.ImageChangeActions.HISTOGRAM;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.WEST;

/**
 * Abstract superclass for all the brush-like tools.
 */
public abstract class AbstractBrushTool extends Tool implements ActiveImageChangeListener {
    private static final int MIN_BRUSH_RADIUS = 1;
    public static final int MAX_BRUSH_RADIUS = 100;
    public static final int DEFAULT_BRUSH_RADIUS = 10;

    private boolean respectSelection = true; // false while tracing a selection

    private JComboBox<BrushType> typeSelector;

    protected Graphics2D graphics;
    private final RangeParam brushRadiusParam = new RangeParam("Radius", MIN_BRUSH_RADIUS, DEFAULT_BRUSH_RADIUS, MAX_BRUSH_RADIUS, false, WEST);

    private final EnumComboBoxModel<Symmetry> symmetryModel = new EnumComboBoxModel<>(Symmetry.class);

    protected Brush brush;
    private SymmetryBrush symmetryBrush;
    protected BrushAffectedArea brushAffectedArea;

    private boolean firstMouseDown = true; // for the first click don't draw lines even if it is a shift-click
    private JButton brushSettingsButton;

    private JDialog settingsDialog;

    DrawStrategy drawStrategy;

    AbstractBrushTool(char activationKeyChar, String name, String iconFileName, String toolMessage, Cursor cursor) {
        super(activationKeyChar, name, iconFileName, toolMessage,
                cursor, true, true, ClipStrategy.CANVAS);
        ImageComponents.addActiveImageChangeListener(this);
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
            closeToolDialogs();

            BrushType brushType = getBrushType();
            symmetryBrush.brushTypeChanged(brushType, getRadius());

            brushRadiusParam.setEnabled(brushType.sizeCanBeSet(), FilterSetting.EnabledReason.APP_LOGIC);

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
                    BrushType brushType = getBrushType();
                    JPanel p = brushType.getConfigPanel(this);
                    settingsDialog = new OKDialog(PixelitorWindow.getInstance(), p, "Brush Settings");
                });

        brushSettingsButton.setEnabled(false);
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        boolean withLine = withLine(e);
        double x = e.getImX();
        double y = e.getImY();

        newMousePoint(e.getComp().getActiveDrawable(), x, y, withLine);
        firstMouseDown = false;

        if (withLine) {
            brushAffectedArea.updateAffectedCoordinates(x, y);
        } else {
            brushAffectedArea.initAffectedCoordinates(x, y);
        }
    }

    protected boolean withLine(PMouseEvent e) {
        return !firstMouseDown && e.isShiftDown();
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        double x = e.getImX();
        double y = e.getImY();

        // at this point x and y are already scaled according to the zoom level
        // (unlike e.getX(), e.getY())

        newMousePoint(e.getComp().getActiveDrawable(), x, y, false);
    }

    @Override
    public void mouseReleased(PMouseEvent e) {
        if (graphics == null) {
            // we can get here if the mousePressed was an Alt-press, therefore
            // consumed by the color picker. Nothing was drawn, therefore
            // there is no need to save a backup, we can just return
            // TODO is this true after all the refactorings?
            return;
        }

        Composition comp = e.getComp();
        finishBrushStroke(comp.getActiveDrawable());
    }

    private void finishBrushStroke(Drawable dr) {
        int radius = getRadius();
        BufferedImage originalImage = drawStrategy.getOriginalImage(dr, this);
        ToolAffectedArea affectedArea = new ToolAffectedArea(
                brushAffectedArea.getRectangleAffectedByBrush(radius),
                originalImage, dr,
                false, getName());
        affectedArea.addToHistory();

        if (graphics != null) {
            graphics.dispose();
        }
        graphics = null;

        drawStrategy.finishBrushStroke(dr);

        dr.updateIconImage();

        dr.getComp().imageChanged(HISTOGRAM);
    }

    public void drawBrushStrokeProgrammatically(Drawable dr, Point start, Point end) {
        prepareProgrammaticBrushStroke(dr, start);

        brush.onStrokeStart(start.x, start.y);
        brush.onNewStrokePoint(end.x, end.y);

        finishBrushStroke(dr);
    }

    protected void prepareProgrammaticBrushStroke(Drawable dr, Point start) {
        drawStrategy.prepareBrushStroke(dr);
        graphics = createGraphicsForNewBrushStroke(dr);
    }

    /**
     * Creates the global Graphics2D object graphics.
     */
    private Graphics2D createGraphicsForNewBrushStroke(Drawable dr) {
        Composition comp = dr.getComp();
        Composite composite = getComposite();
        Graphics2D g = drawStrategy.createDrawGraphics(dr, composite);
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        initializeGraphics(g);
        if (respectSelection) {
            comp.applySelectionClipping(g, null);
        }

        brush.setTarget(comp, g);
        return g;
    }

    /**
     * An opportunity to do extra tool-specific
     * initializations in the subclasses
     */
    protected void initializeGraphics(Graphics2D g) {
    }

    // overridden in brush tools with blending mode
    protected Composite getComposite() {
        return null;
    }

    /**
     * Called from mousePressed, mouseDragged
     */
    private void newMousePoint(Drawable dr, double x, double y, boolean connectClickWithLine) {
        if (graphics == null) { // a new brush stroke has to be initialized
            drawStrategy.prepareBrushStroke(dr);
            graphics = createGraphicsForNewBrushStroke(dr);
            graphics.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

            if (connectClickWithLine) {
                brush.onNewStrokePoint(x, y);
            } else {
                brush.onStrokeStart(x, y);
            }
        } else {
            brush.onNewStrokePoint(x, y);
        }
    }

    private void setupDrawingRadius() {
        int newRadius = getRadius();
        brush.setRadius(newRadius);

//        int desiredImgSize = 2 * newRadius;
//        Dimension cursorSize = Toolkit.getDefaultToolkit().getBestCursorSize(desiredImgSize, desiredImgSize);
//
//        BufferedImage cursorImage = ImageUtils.createSysCompatibleImage(cursorSize.width, cursorSize.height);
//        Graphics2D g = cursorImage.createGraphics();
//        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
//        g.setColor(Color.RED);
//        g.drawOval(0, 0, cursorSize.width, cursorSize.height);
//        g.dispose();
//
//        cursor = Toolkit.getDefaultToolkit().createCustomCursor(
//                cursorImage,
//                new Point(cursorSize.width / 2, cursorSize.height / 2), "brush");
//        ImageComponents.onAllImages(ic -> ic.setCursor(cursor));
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
    public void newImageOpened(Composition comp) {
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
    public void trace(Drawable dr, Shape shape) {
        try {
            respectSelection = false;

            drawStrategy.prepareBrushStroke(dr);

            graphics = createGraphicsForNewBrushStroke(dr);

            doTraceAfterSetup(shape);

            finishBrushStroke(dr);
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

                    brush.onStrokeStart(x, y);

                    break;
                case PathIterator.SEG_LINETO:
                    brush.onNewStrokePoint(x, y);

                    break;
                case PathIterator.SEG_CLOSE:
                    brush.onNewStrokePoint(startingX, startingY);
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
    public boolean doColorPickerForwarding() {
        return true;
    }

    @VisibleForTesting
    protected Brush getBrush() {
        return brush;
    }

    @VisibleForTesting
    protected void setBrush(Brush brush) {
        this.brush = brush;
    }

    private BrushType getBrushType() {
        return (BrushType) typeSelector.getSelectedItem();
    }

    // TODO indicate the size of the brush
    @Override
    public void paintOverImage(Graphics2D g2, Canvas canvas, ImageComponent ic, AffineTransform componentTransform, AffineTransform imageTransform) {
//        if(userDrag != null) {
//            int x = userDrag.getCoEndX();
//            int y = userDrag.getCoEndY();
//            double radius = getRadius() * ic.getViewScale();
//            double diameter = 2 * radius;
//            Ellipse2D.Double shape = new Ellipse2D.Double(x - radius, y - radius, diameter, diameter);
//            g2.setStroke(stroke3);
//            g2.setColor(Color.BLACK);
//            g2.draw(shape);
//            g2.setStroke(stroke1);
//            g2.setColor(Color.WHITE);
//            g2.draw(shape);
//        }
    }
//    private static final Stroke stroke3 = new BasicStroke(3);
//    private static final Stroke stroke1 = new BasicStroke(1);

    @Override
    protected void closeToolDialogs() {
        GUIUtils.closeDialog(settingsDialog);
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = super.getDebugNode();

        if (typeSelector != null) { // can be null, for example in Clone
            node.addString("Brush Type", getBrushType().toString());
        }
        node.addInt("Radius", getRadius());

        node.add(brush.getDebugNode());

        if (symmetryBrush != null) { // can be null, for example in Clone
            node.addString("Symmetry", getSymmetry().toString());
            if (symmetryBrush != brush) {
                node.add(symmetryBrush.getDebugNode());
            }
        }

        return node;
    }
}
