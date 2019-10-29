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
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.OpenComps;
import pixelitor.gui.View;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.SimpleCachedPainter;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.history.History;
import pixelitor.layers.Drawable;
import pixelitor.tools.brushes.AffectedArea;
import pixelitor.tools.brushes.Brush;
import pixelitor.tools.brushes.LazyMouseBrush;
import pixelitor.tools.brushes.SymmetryBrush;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.Shapes;
import pixelitor.utils.VisibleForTesting;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Transparency;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
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

    // some extra space is added to the repaint region
    // since repaint() is asynchronous
    private static final int REPAINT_EXTRA_SPACE = 20;

    private final boolean canHaveSymmetry;

    private JComboBox<BrushType> typeCB;
    protected JCheckBox lazyMouseCB;
    private JDialog lazyMouseDialog;

    protected Graphics2D graphics;
    private final RangeParam brushRadiusParam = new RangeParam("Radius",
            MIN_BRUSH_RADIUS, DEFAULT_BRUSH_RADIUS, MAX_BRUSH_RADIUS, false, WEST);

    private EnumComboBoxModel<Symmetry> symmetryModel;

    protected Brush brush;
    private SymmetryBrush symmetryBrush;
    protected AffectedArea affectedArea;

    private JButton brushSettingsButton;

    private JDialog settingsDialog;

    DrawDestination drawDestination;
    private RangeParam lazyMouseDist;
    private RangeParam lazyMouseSpacing;
    private static final String UNICODE_MOUSE_SYMBOL = new String(Character.toChars(0x1F42D));

    private int lastCoX;
    private int lastCoY;
    private final BrushOutlinePainter outlinePainter = new BrushOutlinePainter(DEFAULT_BRUSH_RADIUS);
    private boolean paintBrushOutline = false;

    AbstractBrushTool(String name, char activationKey, String iconFileName,
                      String toolMessage, Cursor cursor, boolean canHaveSymmetry) {
        super(name, activationKey, iconFileName, toolMessage,
                cursor, true, true, ClipStrategy.CANVAS);
        this.canHaveSymmetry = canHaveSymmetry;
        if (canHaveSymmetry) {
            symmetryModel = new EnumComboBoxModel<>(Symmetry.class);
        }
        initBrushVariables();

        assert (symmetryBrush != null) == canHaveSymmetry;
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
        typeCB = new JComboBox<>(brushTypes);
        settingsPanel.addComboBox("Brush:", typeCB, "typeCB");
        typeCB.addActionListener(e -> brushTypeChanged());

        // make sure all values are visible without a scrollbar
        typeCB.setMaximumRowCount(brushTypes.length);
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
        assert canHaveSymmetry;

        JComboBox<Symmetry> symmetryCB = new JComboBox<>(symmetryModel);
        settingsPanel.addComboBox("Mirror:", symmetryCB, "symmetrySelector");
        symmetryCB.addActionListener(e -> symmetryBrush.symmetryChanged(
                getSymmetry(), getRadius()));
    }

    protected void addBrushSettingsButton() {
        brushSettingsButton = settingsPanel.addButton(
                "Settings...", e -> brushSettingsButtonPressed(),
                "brushSettingsDialogButton", "Configure the selected brush");

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
        settingsPanel.addButton("Lazy Mouse...",
                e -> showLazyMouseDialog(),
                "lazyMouseDialogButton", "Configure brush smoothing");
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

        lazyMouseDist = LazyMouseBrush.createDistParam();
        SliderSpinner distSlider = SliderSpinner.simpleFrom(lazyMouseDist);
        distSlider.setName("distSlider");
        distSlider.setEnabled(lazyMouseEnabledByDefault);
        gbh.addLabelWithControl(lazyMouseDist.getName() + ":", distSlider);

        lazyMouseSpacing = LazyMouseBrush.createSpacingParam();
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
                .title("Lazy Mouse Settings")
                .notModal()
                .willBeShownAgain()
                .okText("Close")
                .noCancelButton()
                .show();
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        boolean lineConnect = e.isShiftDown() && brush.hasPrevious();

        Drawable dr = e.getComp().getActiveDrawableOrThrow();
        newMousePoint(dr, e, lineConnect);

        // it it can have symmetry, then the symmetry brush does
        // the tracking of the affected area
        if (!canHaveSymmetry) {
            if (lineConnect) {
                affectedArea.updateWith(e);
            } else {
                affectedArea.initAt(e);
            }
        }
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        lastCoX = (int) e.getCoX();
        lastCoY = (int) e.getCoY();

        newMousePoint(e.getComp().getActiveDrawableOrThrow(), e, false);
    }

    @Override
    public void mouseMoved(MouseEvent e, View view) {
        repaintOutlineSinceLast(e.getX(), e.getY(), view);
    }

    private void repaintOutlineSinceLast(int x, int y, View view) {
        int prevX = lastCoX;
        int prevY = lastCoY;

        lastCoX = x;
        lastCoY = y;

        Rectangle r = Shapes.toPositiveRect(prevX, lastCoX, prevY, lastCoY);

        int growth = outlinePainter.getCoRadius() + REPAINT_EXTRA_SPACE;
        r.grow(growth, growth);
        view.repaint(r);
    }

    private void repaintOutline(View view) {
        int growth = outlinePainter.getCoRadius() + REPAINT_EXTRA_SPACE;

        view.repaint(lastCoX - growth, lastCoY - growth, 2 * growth, 2 * growth);
    }

    @Override
    public void mouseEntered(MouseEvent e, View view) {
        if (typeCB == null) {
            paintBrushOutline = true;
        } else if (getBrushType() != BrushType.ONE_PIXEL) {
            paintBrushOutline = true;
        } else {
            // it should be false already, but set it for safety
            paintBrushOutline = false;
        }
        lastCoX = e.getX();
        lastCoY = e.getY();
        repaintOutline(view);
    }

    @Override
    public void mouseExited(MouseEvent e, View view) {
        paintBrushOutline = false;
        repaintOutlineSinceLast(e.getX(), e.getY(), view);
    }

    @Override
    public void mouseReleased(PMouseEvent e) {
        if (graphics == null) {
            // we can get here if the mousePressed was an Alt-press, therefore
            // consumed by the color picker. Nothing was drawn, therefore
            // there is no need to save a backup, we can just return
            return;
        }

        Composition comp = e.getComp();
        Drawable dr = comp.getActiveDrawableOrThrow();
        finishBrushStroke(dr);
    }

    private void finishBrushStroke(Drawable dr) {
        brush.finish();

        BufferedImage originalImage = drawDestination.getOriginalImage(dr, this);

        double brushRadius = brush.getEffectiveRadius();
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
        comp.applySelectionClipping(g);

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
        } else if (brush.hasPrevious()) {
            brush.continueTo(p);
        } else {
            // there is a graphics, but the brush has no previous
            // TODO why does this happen sometimes in random tests?
            brush.startAt(p);
        }
    }

    private void setupDrawingRadius() {
        int newRadius = getRadius();
        brush.setRadius(newRadius);

        outlinePainter.setRadius(newRadius);
        if (paintBrushOutline) {
            // changing the brush via keyboard shortcut
            repaintOutline(OpenComps.getActiveView());
        }
    }

    @Override
    protected void toolStarted() {
        super.toolStarted();
        resetInitialState();

        View view = OpenComps.getActiveView();
        if (view != null) {
            outlinePainter.setView(view);
        }
    }

    @Override
    public void allCompsClosed() {

    }

    @Override
    public void compActivated(View oldCV, View newCV) {
        resetInitialState();
        outlinePainter.setView(newCV);
    }

    @Override
    public void coCoordsChanged(View view) {
        EventQueue.invokeLater(() -> {
            Point location = MouseInfo.getPointerInfo().getLocation();
            SwingUtilities.convertPointFromScreen(location, view);
            outlinePainter.setView(view);
            repaintOutlineSinceLast(location.x, location.y, view);
        });
    }

    /**
     * Traces the given shape with the current brush tool
     */
    public void trace(Drawable dr, Shape shape) {
        try {
            affectedArea = new AffectedArea();
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
        int subPathIndex = -1;
        while (!fpi.isDone()) {
            int type = fpi.currentSegment(coords);
            double x = coords[0];
            double y = coords[1];
            PPoint p = PPoint.lazyFromIm(x, y, view);
            affectedArea.updateWith(p);

            switch (type) {
                case PathIterator.SEG_MOVETO:
                    // we can get here more than once if there are multiple subpaths!
                    subPathIndex++;
                    startingPoint = p;
                    if (!brushStrokePrepared) {
                        // TODO this should not be here, and it should not need
                        // a point argument, but it is here because some hacks
                        // in the clone and smudge tools need that point
                        prepareProgrammaticBrushStroke(dr, p);
                        brushStrokePrepared = true;
                    }
                    if (subPathIndex != 0) {
                        brush.finish();
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
        assert canHaveSymmetry;

        return symmetryModel.getSelectedItem();
    }

    protected int getRadius() {
        int value = brushRadiusParam.getValue();

        assert value >= MIN_BRUSH_RADIUS : "value = " + value;

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
        return (BrushType) typeCB.getSelectedItem();
    }

    @Override
    public void paintOverImage(Graphics2D g2, Canvas canvas,
                               View view,
                               AffineTransform componentTransform,
                               AffineTransform imageTransform) {
        if (paintBrushOutline) {
            outlinePainter.paint(g2, lastCoX, lastCoY);
        }
    }

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

        if (typeCB != null) { // can be null, for example in Clone
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

    @Override
    public String getStateInfo() {
        StringBuilder sb = new StringBuilder(20);
        if (typeCB != null) { // not all subclasses have a type selector
            sb.append(getBrushType()).append(", ");
        }
        sb.append("r=").append(getRadius());
        if (canHaveSymmetry) {
            sb.append(", sym=").append(getSymmetry());
        }
        boolean lazyMouse = false;
        if (lazyMouseCB != null) {
            if (lazyMouseCB.isSelected()) {
                lazyMouse = true;
                sb.append(", (lazy " + UNICODE_MOUSE_SYMBOL + " d=")
                        .append(lazyMouseDist.getValue())
                        .append(", sp=")
                        .append(lazyMouseSpacing.getValue())
                        .append(")");
            }
        }
        if (!lazyMouse) {
            sb.append(", eager ");
            sb.append(UNICODE_MOUSE_SYMBOL);
        }

        return sb.toString();
    }

    /**
     * Paints the brush outline.
     *
     * This is necessary because (at least on Windows) it looks like
     * cursors can't have an arbitrary size, so the outline cannot be
     * made via custom brush images. See java.awt.Toolkit.getBestCursorSize.
     */
    static class BrushOutlinePainter extends SimpleCachedPainter {
        private static final Stroke stroke3 = new BasicStroke(3);
        private static final Stroke stroke1 = new BasicStroke(1);

        private int imRadius;
        private double coRadius;
        private View view;
        private double coDiameter;

        public BrushOutlinePainter(int radius) {
            super(Transparency.TRANSLUCENT);
            this.imRadius = radius;
        }

        public void setView(View newView) {
            view = newView;
            calcCoRadius();
        }

        public void setRadius(int imRadius) {
            this.imRadius = imRadius;
            calcCoRadius();
        }

        private void calcCoRadius() {
            if (view == null) {
                return;
            }
            double radiusBefore = coRadius;
            coRadius = view.getScaling() * imRadius;
            if (radiusBefore != coRadius) {
                coDiameter = 2 * coRadius;
                invalidateCache();
            }
        }

        @Override
        public void doPaint(Graphics2D g, int width, int height) {
            g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

            // start at 1, 1 so that the full stroke width fits in the image
            Shape shape = new Ellipse2D.Double(1, 1, coDiameter, coDiameter);

            g.setStroke(stroke3);
            g.setColor(Color.BLACK);
            g.draw(shape);

            g.setStroke(stroke1);
            g.setColor(Color.WHITE);
            g.draw(shape);
        }

        public void paint(Graphics2D g2, double x, double y) {
            if (view == null) {
                throw new IllegalStateException("brush outline not initialized");
            }
            AffineTransform origTX = g2.getTransform();

            g2.translate(x - coRadius - 1, y - coRadius - 1);
            super.paint(g2, null, 3 + (int) coDiameter, 3 + (int) coDiameter);

            g2.setTransform(origTX);
        }

        public int getCoRadius() {
            return (int) coRadius;
        }
    }
}
