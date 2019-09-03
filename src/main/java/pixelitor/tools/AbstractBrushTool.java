/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.Build;
import pixelitor.Composition;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.View;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.history.History;
import pixelitor.layers.Drawable;
import pixelitor.tools.brushes.AffectedArea;
import pixelitor.tools.brushes.Brush;
import pixelitor.tools.brushes.LazyMouseBrush;
import pixelitor.tools.brushes.SymmetryBrush;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.VisibleForTesting;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.Composition.ImageChangeActions.HISTOGRAM;
import static pixelitor.filters.gui.FilterSetting.EnabledReason.APP_LOGIC;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.WEST;

/**
 * Abstract base class for all brush-like tools.
 */
public abstract class AbstractBrushTool extends Tool {
    private static final int MIN_BRUSH_RADIUS = 1;
    public static final int MAX_BRUSH_RADIUS = 100;
    public static final int DEFAULT_BRUSH_RADIUS = 10;

    private boolean respectSelection = true; // false while tracing a selection

    private JComboBox<BrushType> typeSelector;
    protected JCheckBox lazyMouseCB;
    private JDialog lazyMouseDialog;

    protected Graphics2D graphics;
    private final RangeParam brushRadiusParam = new RangeParam("Radius",
            MIN_BRUSH_RADIUS, DEFAULT_BRUSH_RADIUS, MAX_BRUSH_RADIUS, false, WEST);

    private final EnumComboBoxModel<Symmetry> symmetryModel
            = new EnumComboBoxModel<>(Symmetry.class);

    protected Brush brush;
    private SymmetryBrush symmetryBrush;
    protected AffectedArea affectedArea;

    // for the first click it shouldn't draw lines even if it is a shift-click
    // TODO this shouldn't be necessary, but there are problems
    // (exception after a first shift-click) in the smudge brush without it
    private boolean firstMouseDown = true;

    private JButton brushSettingsButton;

    private JDialog settingsDialog;

    DrawDestination drawDestination;

    AbstractBrushTool(String name, char activationKeyChar,
                      String iconFileName, String toolMessage, Cursor cursor) {
        super(name, activationKeyChar, iconFileName, toolMessage,
                cursor, true, true, ClipStrategy.CANVAS);
        initBrushVariables();
    }

    protected void initBrushVariables() {
        symmetryBrush = new SymmetryBrush(
                this, BrushType.values()[0], getSymmetry(), getRadius());
        brush = symmetryBrush;
        affectedArea = symmetryBrush.getAffectedArea();
    }

    // if initBrushVariables() is overridden,
    // then this must also be overridden
    protected void setLazyBrush() {
        if (lazyMouseCB.isSelected()) {
            // set the decorated brush
            brush = new LazyMouseBrush(symmetryBrush);
        } else {
            // set the normal brush
            brush = symmetryBrush;
        }
    }

    protected void addTypeSelector() {
        BrushType[] brushTypes = BrushType.values();
        typeSelector = new JComboBox<>(brushTypes);
        settingsPanel.addWithLabel("Brush:", typeSelector, "brushTypeSelector");
        typeSelector.addActionListener(e -> brushTypeChanged());

        // make sure all values are visible without a scrollbar
        typeSelector.setMaximumRowCount(brushTypes.length);
    }

    private void brushTypeChanged() {
        closeBrushSettingsDialog();

        BrushType brushType = getBrushType();
        symmetryBrush.brushTypeChanged(brushType, getRadius());
        brushRadiusParam.setEnabled(brushType.sizeCanBeSet(), APP_LOGIC);
        brushSettingsButton.setEnabled(brushType.hasSettings());
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
        brushSettingsButton = settingsPanel.addButton(
                "Brush Settings",
                e -> brushSettingsButtonPressed());

        brushSettingsButton.setEnabled(false);
    }

    private void brushSettingsButtonPressed() {
        BrushType brushType = getBrushType();
        JPanel p = brushType.getConfigPanel(this);
        settingsDialog = new DialogBuilder()
                .content(p)
                .title("Settings for the " + brushType.toString() + " Brush")
                .notModal()
                .withScrollbars()
                .okText("Close")
                .noCancelButton()
                .show();
    }

    protected void addLazyMouseDialogButton() {
        JButton button = new JButton("Lazy Mouse...");
        button.addActionListener(e -> showLazyMouseDialog());

        settingsPanel.add(button);
    }

    private void showLazyMouseDialog() {
        if (lazyMouseDialog != null) {
            GUIUtils.showDialog(lazyMouseDialog);
            return;
        }
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        GridBagHelper gbh = new GridBagHelper(p);

        boolean lazyMouseEnabledByDefault = false;
        lazyMouseCB = new JCheckBox("", lazyMouseEnabledByDefault);
        lazyMouseCB.addActionListener(e -> setLazyBrush());
        gbh.addLabelWithControlNoStretch("Enabled:", lazyMouseCB);

        RangeParam lazyMouseDist = LazyMouseBrush.createDistParam();
        SliderSpinner distSlider = SliderSpinner.simpleFrom(lazyMouseDist);
        distSlider.setName("distSlider");
        distSlider.setEnabled(lazyMouseEnabledByDefault);
        gbh.addLabelWithControl(lazyMouseDist.getName() + ":", distSlider);

        RangeParam lazyMouseSpacing = LazyMouseBrush.createSpacingParam();
        SliderSpinner spacingSlider = SliderSpinner.simpleFrom(lazyMouseSpacing);
        spacingSlider.setEnabled(lazyMouseEnabledByDefault);
        spacingSlider.setName("spacingSlider");
        gbh.addLabelWithControl(lazyMouseSpacing.getName() + ":", spacingSlider);

        lazyMouseCB.addActionListener(e -> {
            boolean enable = lazyMouseCB.isSelected();
            distSlider.setEnabled(enable);
            spacingSlider.setEnabled(enable);
        });

        lazyMouseDialog = new DialogBuilder()
                .content(p)
                .title("Lazy Mouse")
                .notModal()
                .willBeShownAgain()
                .okText("Close")
                .noCancelButton()
                .show();
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        boolean withLine = withLine(e);
        firstMouseDown = false;

        newMousePoint(e.getComp().getActiveDrawableOrThrow(), e, withLine);

        if (withLine) {
            affectedArea.updateWith(e);
        } else {
            affectedArea.initAt(e);
        }
    }

    protected boolean withLine(PMouseEvent e) {
        // the first mousePressed event is not a line-connecting
        // one, even if the shift key is down
        return !firstMouseDown && e.isShiftDown();
//        return e.isShiftDown();
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        newMousePoint(e.getComp().getActiveDrawableOrThrow(), e, false);
    }

    @Override
    public void mouseReleased(PMouseEvent e) {
        if (graphics == null) {
            // we can get here if the mousePressed was an Alt-press, therefore
            // consumed by the color picker. Nothing was drawn, therefore
            // there is no need to save a backup, we can just return
            return;
        }

        brush.finish();

        Composition comp = e.getComp();
        finishBrushStroke(comp.getActiveDrawableOrThrow());
    }

    private void finishBrushStroke(Drawable dr) {
        BufferedImage originalImage = drawDestination.getOriginalImage(dr, this);

        double brushRadius = brush.getActualRadius();
        Rectangle rect = affectedArea.asRectangle(brushRadius);
        assert !rect.isEmpty() : "brush radius = " + brushRadius + ", affected area = " + affectedArea;

        History.addToolArea(rect,
                originalImage, dr,
                false, getName());

        if (graphics != null) {
            graphics.dispose();
        }
        graphics = null;

        drawDestination.finishBrushStroke(dr);

        dr.updateIconImage();

        dr.getComp().imageChanged(HISTOGRAM);
    }

    public void drawBrushStrokeProgrammatically(Drawable dr, PPoint start, PPoint end) {
        prepareProgrammaticBrushStroke(dr, start);

        brush.startAt(start);
        brush.continueTo(end);

        finishBrushStroke(dr);
    }

    protected void prepareProgrammaticBrushStroke(Drawable dr, PPoint start) {
        drawDestination.prepareBrushStroke(dr);
        graphics = createGraphicsForNewBrushStroke(dr);
    }

    /**
     * Creates the global Graphics2D object graphics.
     */
    private Graphics2D createGraphicsForNewBrushStroke(Drawable dr) {
        Composition comp = dr.getComp();
        Composite composite = getComposite();
        Graphics2D g = drawDestination.createGraphics(dr, composite);
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        initializeGraphics(g);
        if (respectSelection) {
            comp.applySelectionClipping(g);
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
    private void newMousePoint(Drawable dr, PPoint p, boolean lineConnect) {
        if (graphics == null) { // a new brush stroke has to be initialized
            drawDestination.prepareBrushStroke(dr);
            graphics = createGraphicsForNewBrushStroke(dr);
            graphics.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

            if (lineConnect) {
                brush.lineConnectTo(p);
            } else {
                brush.startAt(p);
            }
        } else {
            brush.continueTo(p);
        }
    }

    private void setupDrawingRadius() {
        int newRadius = getRadius();
        brush.setRadius(newRadius);

//        int desiredImgSize = 2 * newRadius;
//        Dimension cursorSize = Toolkit.getDefaultToolkit().getBestCursorSize(
//              desiredImgSize, desiredImgSize);
//
//        BufferedImage cursorImage = ImageUtils.createSysCompatibleImage(
//              cursorSize.width, cursorSize.height);
//        Graphics2D g = cursorImage.createGraphics();
//        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
//        g.setColor(Color.RED);
//        g.drawOval(0, 0, cursorSize.width, cursorSize.height);
//        g.dispose();
//
//        cursor = Toolkit.getDefaultToolkit().createCustomCursor(
//                cursorImage,
//                new Point(cursorSize.width / 2, cursorSize.height / 2), "brush");
//        ImageComponents.onAllImages(view -> view.setCursor(cursor));
    }

    @Override
    protected void toolStarted() {
        super.toolStarted();
        resetInitialState();
    }

    @Override
    public void allCompsClosed() {

    }

    @Override
    public void compActivated(View oldCV, View newCV) {
        resetInitialState();
    }

    @Override
    public void resetInitialState() {
        firstMouseDown = true;
        respectSelection = true;
    }

    /**
     * Traces the given shape with the current brush tool
     */
    public void trace(Drawable dr, Shape shape) {
        try {
            respectSelection = false;

            doTrace(dr, shape);

            finishBrushStroke(dr);
        } finally {
            resetInitialState();
        }
    }

    private void doTrace(Drawable dr, Shape shape) {
        View view = dr.getComp().getView();
        PPoint startingPoint = null;

        PathIterator fpi = new FlatteningPathIterator(
                shape.getPathIterator(null), 1.0);

//        MeasuredShape[] subpaths = MeasuredShape.getSubpaths(shape, 3.0f);
//        GeneralPath gp = new GeneralPath();
//        subpaths[0].writeShape(new GeneralPathWriter(gp));
//        PathIterator fpi = gp.getPathIterator(null);

        boolean brushStrokePrepared = false;
        float[] coords = new float[2];
        while (!fpi.isDone()) {
            int type = fpi.currentSegment(coords);
            double x = coords[0];
            double y = coords[1];
            PPoint p = PPoint.lazyFromIm(x, y, view);
            affectedArea.updateWith(p);

            switch (type) {
                case PathIterator.SEG_MOVETO:
                    // we can get here more than once if there are multiple subpaths!
                    startingPoint = p;
                    if (!brushStrokePrepared) {
                        // TODO this should not be here, and it should not need
                        // a point argument, but it is here because some hacks
                        // in the clone and smudge tools need that point
                        prepareProgrammaticBrushStroke(dr, p);
                        brushStrokePrepared = true;
                    }
                    brush.startAt(p);
                    break;
                case PathIterator.SEG_LINETO:
                    brush.continueTo(p);
                    break;
                case PathIterator.SEG_CLOSE:
                    brush.continueTo(startingPoint);
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

        // because of a JDK bug (?), sometimes it is possible
        // to drag the slider to negative values
        if (value < MIN_BRUSH_RADIUS) {
            if (Build.isDevelopment()) {
                Thread.dumpStack();
            }
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
//    @Override
//    public void paintOverImage(Graphics2D g2, Canvas canvas,
//                               View view,
//                               AffineTransform componentTransform,
//                               AffineTransform imageTransform) {
//        if(userDrag != null) {
//            int x = userDrag.getCoEndX();
//            int y = userDrag.getCoEndY();
//            double radius = getRadius() * view.getScaling();
//            double diameter = 2 * radius;
//            Ellipse2D.Double shape = new Ellipse2D.Double(x - radius, y - radius,
//                  diameter, diameter);
//            g2.setStroke(stroke3);
//            g2.setColor(Color.BLACK);
//            g2.draw(shape);
//            g2.setStroke(stroke1);
//            g2.setColor(Color.WHITE);
//            g2.draw(shape);
//        }
//    }
//    private static final Stroke stroke3 = new BasicStroke(3);
//    private static final Stroke stroke1 = new BasicStroke(1);

    @Override
    protected void closeToolDialogs() {
        closeBrushSettingsDialog();
        GUIUtils.closeDialog(lazyMouseDialog, false);
    }

    private void closeBrushSettingsDialog() {
        GUIUtils.closeDialog(settingsDialog, true);
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
