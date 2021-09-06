/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.AppContext;
import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.GUIText;
import pixelitor.gui.View;
import pixelitor.gui.utils.*;
import pixelitor.history.History;
import pixelitor.history.MultiEdit;
import pixelitor.layers.Drawable;
import pixelitor.layers.LayerMask;
import pixelitor.tools.brushes.*;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.Shapes;
import pixelitor.utils.VisibleForTesting;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.FlatteningPathIterator;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.geom.PathIterator.*;
import static javax.swing.BorderFactory.createEmptyBorder;
import static pixelitor.Composition.UpdateActions.HISTOGRAM;
import static pixelitor.filters.gui.FilterSetting.EnabledReason.APP_LOGIC;
import static pixelitor.gui.GUIText.CLOSE_DIALOG;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.WEST;

/**
 * Abstract base class for the tools working with {@link Brush} objects.
 */
public abstract class AbstractBrushTool extends Tool {
    private static final int MIN_BRUSH_RADIUS = 1;
    public static final int MAX_BRUSH_RADIUS = 100;
    public static final int DEFAULT_BRUSH_RADIUS = 10;

    private JComboBox<BrushType> typeCB;

    protected Graphics2D graphics;
    private final RangeParam brushRadiusParam = new RangeParam(GUIText.RADIUS,
        MIN_BRUSH_RADIUS, DEFAULT_BRUSH_RADIUS, MAX_BRUSH_RADIUS, false, WEST);

    private final boolean canHaveSymmetry;
    private EnumComboBoxModel<Symmetry> symmetryModel;

    protected Brush brush;
    private SymmetryBrush symmetryBrush;
    protected AffectedArea affectedArea;

    private JButton brushSettingsDialogButton;
    private Action brushSettingsAction;
    private JDialog settingsDialog;

    protected DrawDestination drawDestination;

    protected boolean lazyMouse;
    protected JCheckBox lazyMouseCB;
    private JDialog lazyMouseDialog;
    private RangeParam lazyMouseDist;
    protected LazyMouseBrush lazyMouseBrush;
    private JButton showLazyMouseDialogButton;

    private int outlineCoX;
    private int outlineCoY;
    private final BrushOutlinePainter outlinePainter = new BrushOutlinePainter(DEFAULT_BRUSH_RADIUS);
    private boolean paintBrushOutline = false;

    // some extra space is added to the repaint region
    // since repaint() is asynchronous
    private static final int REPAINT_EXTRA_SPACE = 20;

    private static final String UNICODE_MOUSE_SYMBOL = new String(Character.toChars(0x1F42D));

    AbstractBrushTool(String name, char activationKey, String iconFileName,
                      String toolMessage, Cursor cursor, boolean canHaveSymmetry) {
        super(name, activationKey, iconFileName, toolMessage, cursor);
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
            lazyMouseBrush = new LazyMouseBrush(symmetryBrush);
            brush = lazyMouseBrush;
            lazyMouse = true;
        } else {
            // set the normal brush
            brush = symmetryBrush;
            lazyMouseBrush = null;
            lazyMouse = false;
        }
    }

    protected void addTypeSelector() {
        var brushTypes = BrushType.values();
        typeCB = GUIUtils.createComboBox(brushTypes);
        settingsPanel.addComboBox(GUIText.BRUSH + ":", typeCB, "typeCB");
        typeCB.addActionListener(e -> brushTypeChanged());
    }

    private void brushTypeChanged() {
        closeBrushSettingsDialog();

        var brushType = getBrushType();
        symmetryBrush.brushTypeChanged(brushType, getRadius());
        brushRadiusParam.setEnabled(brushType.sizeCanBeSet(), APP_LOGIC);
        brushSettingsAction.setEnabled(brushType.hasSettings());
    }

    protected void addSizeSelector() {
        settingsPanel.add(brushRadiusParam.createGUI());
        brushRadiusParam.setAdjustmentListener(this::setupDrawingRadius);
        setupDrawingRadius();
    }

    protected void addSymmetryCombo() {
        assert canHaveSymmetry;

        @SuppressWarnings("unchecked")
        var symmetryCB = new JComboBox<Symmetry>(symmetryModel);

        settingsPanel.addComboBox(GUIText.MIRROR + ":", symmetryCB, "symmetrySelector");
        symmetryCB.addActionListener(e ->
            symmetryBrush.symmetryChanged(getSymmetry(), getRadius()));
    }

    protected void addBrushSettingsButton() {
        brushSettingsAction = new PAction("Settings...") {
            @Override
            public void onClick() {
                brushSettingsButtonPressed();
            }
        };
        brushSettingsDialogButton = settingsPanel.addButton(brushSettingsAction,
            "brushSettingsDialogButton", "Configure the selected brush");

        brushSettingsAction.setEnabled(false);
    }

    private void brushSettingsButtonPressed() {
        var brushType = getBrushType();
        settingsDialog = new DialogBuilder()
            .content(brushType.getConfigPanel(this))
            .title("Settings for the " + brushType + " Brush")
            .notModal()
            .withScrollbars()
            .okText(CLOSE_DIALOG)
            .noCancelButton()
            .parentComponent(brushSettingsDialogButton)
            .show()
            .getDialog();
    }

    protected void addLazyMouseDialogButton() {
        showLazyMouseDialogButton = settingsPanel.addButton("Lazy Mouse...",
            e -> showLazyMouseDialog(),
            "lazyMouseDialogButton", "Configure brush smoothing");
    }

    private void showLazyMouseDialog() {
        if (lazyMouseDialog != null) {
            GUIUtils.showDialog(lazyMouseDialog, showLazyMouseDialogButton);
            return;
        }
        var p = new JPanel(new GridBagLayout());
        p.setBorder(createEmptyBorder(5, 5, 5, 5));
        var gbh = new GridBagHelper(p);

        lazyMouseCB = new JCheckBox("", false);
        // actionListener doesn't react to programmatic setSelected calls
        lazyMouseCB.addItemListener(e -> setLazyBrush());
        gbh.addLabelAndControlNoStretch("Enabled:", lazyMouseCB);

        lazyMouseDist = LazyMouseBrush.createDistParam();
        var distSlider = SliderSpinner.from(lazyMouseDist);
        distSlider.setName("distSlider");
        distSlider.setEnabled(false);
        gbh.addLabelAndControl(lazyMouseDist.getName() + ":", distSlider);

        lazyMouseCB.addActionListener(e ->
            distSlider.setEnabled(lazyMouseCB.isSelected()));

        lazyMouseDialog = new DialogBuilder()
            .content(p)
            .title("Lazy Mouse Settings")
            .notModal()
            .willBeShownAgain()
            .okText(CLOSE_DIALOG)
            .noCancelButton()
            .parentComponent(showLazyMouseDialogButton)
            .show()
            .getDialog();
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        boolean lineConnect = e.isShiftDown() && brush.hasPrevious();

        Drawable dr = e.getComp().getActiveDrawableOrThrow();
        newMousePoint(dr, e, lineConnect);

        // if it can have symmetry, then the symmetry brush does
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
        newMousePoint(e.getComp().getActiveDrawableOrThrow(), e, false);

        if (lazyMouse) {
            PPoint drawPoint = lazyMouseBrush.getDrawPoint();
            outlineCoX = (int) drawPoint.getCoX();
            outlineCoY = (int) drawPoint.getCoY();
        } else {
            outlineCoX = (int) e.getCoX();
            outlineCoY = (int) e.getCoY();
        }
    }

    @Override
    public void mouseReleased(PMouseEvent e) {
        if (graphics == null) {
            // we can get here if the mousePressed was an Alt-press, therefore
            // consumed by the color picker. Nothing was drawn, therefore
            // there is no need to save a backup, we can just return
            return;
        }

        // whether or not it is lazy mouse, set
        // the outline back to the mouse coordinates
        outlineCoX = (int) e.getCoX();
        outlineCoY = (int) e.getCoY();

        var comp = e.getComp();
        var dr = comp.getActiveDrawableOrThrow();
        finishBrushStroke(dr);

        if (lazyMouse) {
            comp.repaint();
        }
    }

    @Override
    public void mouseEntered(MouseEvent e, View view) {
        startOutlinePaintingAt(e.getX(), e.getY(), view);
    }

    @Override
    public void mouseExited(MouseEvent e, View view) {
        stopOutlinePaintingAt(e.getX(), e.getY(), view);
    }

    @Override
    public void mouseMoved(MouseEvent e, View view) {
        repaintOutlineSinceLast(e.getX(), e.getY(), view);
    }

    private void repaintOutlineSinceLast(int x, int y, View view) {
        int prevX = outlineCoX;
        int prevY = outlineCoY;

        outlineCoX = x;
        outlineCoY = y;

        var repaintRect = Shapes.toPositiveRect(prevX, outlineCoX, prevY, outlineCoY);

        int growth = outlinePainter.getCoRadius() + REPAINT_EXTRA_SPACE;
        repaintRect.grow(growth, growth);
        view.repaint(repaintRect);
    }

    private void repaintOutline(View view) {
        int growth = outlinePainter.getCoRadius() + REPAINT_EXTRA_SPACE;

        view.repaint(outlineCoX - growth, outlineCoY - growth, 2 * growth, 2 * growth);
    }

    private void startOutlinePainting(View view) {
        Point mousePos = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(mousePos, view);
        Rectangle visiblePart = view.getVisiblePart();
        if (visiblePart != null) {
            if (visiblePart.contains(mousePos)) {
                startOutlinePaintingAt(mousePos.x, mousePos.y, view);
            }
        } else if (!AppContext.isUnitTesting()) {
            throw new IllegalStateException();
        }
    }

    private void startOutlinePaintingAt(int x, int y, View view) {
        if (typeCB == null) {
            paintBrushOutline = true;
        } else if (getBrushType() != BrushType.ONE_PIXEL) {
            paintBrushOutline = true;
        } else {
            // it should be false already, but set it for safety
            paintBrushOutline = false;
        }
        outlineCoX = x;
        outlineCoY = y;
        repaintOutline(view);
    }

    private void stopOutlinePainting(View view) {
        stopOutlinePaintingAt(outlineCoX, outlineCoY, view);
    }

    private void stopOutlinePaintingAt(int x, int y, View view) {
        paintBrushOutline = false;
        repaintOutlineSinceLast(x, y, view);
    }

    private void finishBrushStroke(Drawable dr) {
        brush.finishBrushStroke();
        addBrushStrokeToHistory(dr);

        if (graphics != null) {
            graphics.dispose();
        }
        graphics = null;
        drawDestination.finishBrushStroke(dr);

        dr.updateIconImage();
        dr.getComp().update(HISTOGRAM);
    }

    private void addBrushStrokeToHistory(Drawable dr) {
        var originalImage = drawDestination.getOriginalImage(dr, this);

        double maxBrushRadius = brush.getMaxEffectiveRadius();
        var affectedRect = affectedArea.asRectangle(maxBrushRadius);
        assert !affectedRect.isEmpty() : "brush radius = " + maxBrushRadius
            + ", affected area = " + affectedArea;

        var imageEdit = History.createPartialImageEdit(
            affectedRect, originalImage, dr, false, getName());
        if (imageEdit != null) {
            if (typeCB != null && getBrushType() == BrushType.CONNECT) {
                var comp = dr.getComp();
                var connectEdit = new ConnectBrushHistory.Edit(comp);
                History.add(new MultiEdit(imageEdit.getName(), comp, imageEdit, connectEdit));
            } else {
                History.add(imageEdit);
            }
        }
    }

    protected void prepareProgrammaticBrushStroke(Drawable dr, PPoint start) {
        drawDestination.prepareBrushStroke(dr);
        graphics = createGraphicsForNewBrushStroke(dr);
    }

    /**
     * Creates the global Graphics2D object graphics.
     */
    private Graphics2D createGraphicsForNewBrushStroke(Drawable dr) {
        var comp = dr.getComp();
        Composite composite = getComposite();

        // when editing masks, no tmp drawing layer should be used
        assert !(dr instanceof LayerMask)
            || drawDestination == DrawDestination.DIRECT :
            "dr is " + dr.getClass().getSimpleName()
                + ", comp = " + comp.getName()
                + ", tool = " + getClass().getSimpleName()
                + ", drawDestination = " + drawDestination;

        var g = drawDestination.createGraphics(dr, composite);
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        initGraphics(g);
        comp.applySelectionClipping(g);

        brush.setTarget(comp, g);
        return g;
    }

    /**
     * An opportunity to do extra tool-specific
     * initializations in the subclasses
     */
    protected void initGraphics(Graphics2D g) {
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
            repaintOutline(OpenImages.getActiveView());
        }
    }

    @Override
    protected void toolStarted() {
        super.toolStarted();
        resetInitialState();

        View view = OpenImages.getActiveView();
        if (view != null) {
            outlinePainter.setView(view);

            // If the tool is started with the hotkey, then there is
            // no mouseEntered event to start the outline painting
            startOutlinePainting(view);
        }
    }

    @Override
    protected void toolEnded() {
        super.toolEnded();

        View view = OpenImages.getActiveView();
        if (view != null) {
            stopOutlinePainting(view);
        }
    }

    @Override
    public void allViewsClosed() {

    }

    @Override
    public void viewActivated(View oldCV, View newCV) {
        resetInitialState();
        outlinePainter.setView(newCV);

        // get rid of the outline on the old view
        // (important in "Internal Windows" mode)
        if (oldCV != null) {
            oldCV.repaint();
        }

        // make sure that the mouse coordinates are correct relative to the new view
        paintOutlineOnChangedView(newCV);
    }

    @Override
    public void coCoordsChanged(View view) {
        EventQueue.invokeLater(() -> paintOutlineOnChangedView(view));
    }

    private void paintOutlineOnChangedView(View view) {
        Point mousePos = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(mousePos, view);
        outlinePainter.setView(view);
        repaintOutlineSinceLast(mousePos.x, mousePos.y, view);
    }

    @Override
    public void firstModalDialogShown() {
        // the outline has to be hidden, because there is no mouseExited event
        View view = OpenImages.getActiveView();
        if (view != null) {
            stopOutlinePainting(view);
        }
    }

    @Override
    public void firstModalDialogHidden() {
        // the outline has to be shown again, because there is no mouseEntered event
        View view = OpenImages.getActiveView();
        if (view != null) {
            startOutlinePainting(view);
        }
    }

    /**
     * Traces the given shape with the current brush tool.
     * The given shape must be in image coordinates.
     */
    public void trace(Drawable dr, Shape shape) {
        boolean wasLazy = lazyMouse;
        try {
            if (wasLazy) {
                lazyMouseCB.setSelected(false);
            }
            doTrace(dr, shape);
            finishBrushStroke(dr);
        } finally {
            if (wasLazy) {
                lazyMouseCB.setSelected(true);
            }
        }
    }

    private void doTrace(Drawable dr, Shape shape) {
        View view = dr.getComp().getView();
        PPoint subPathStartingPoint = null;
        boolean isFirstPoint = true;
        boolean brushStrokePrepared = false;
        float[] coords = new float[2];
        int subPathIndex = -1;

        var fpi = new FlatteningPathIterator(shape.getPathIterator(null), 1.0);
        while (!fpi.isDone()) {
            int segmentType = fpi.currentSegment(coords);
            PPoint pathPoint = PPoint.lazyFromIm(coords[0], coords[1], view);

            if (isFirstPoint) {
                affectedArea.initAt(pathPoint);
                isFirstPoint = false;
            } else {
                affectedArea.updateWith(pathPoint);
            }

            switch (segmentType) {
                case SEG_MOVETO -> {
                    // we can get here more than once if there are multiple subpaths!
                    subPathIndex++;
                    subPathStartingPoint = pathPoint;
                    if (!brushStrokePrepared) {
                        prepareProgrammaticBrushStroke(dr, pathPoint);
                        brushStrokePrepared = true;
                    }
                    if (subPathIndex != 0) {
                        brush.finishBrushStroke();
                    }
                    brush.startAt(pathPoint);
                }
                case SEG_LINETO -> brush.continueTo(pathPoint);
                case SEG_CLOSE -> brush.continueTo(subPathStartingPoint);
                default -> throw new IllegalArgumentException("segmentType = " + segmentType);
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
        return brushRadiusParam.getValue();
    }

    @Override
    public boolean hasColorPickerForwarding() {
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

    public BrushType getBrushType() {
        return (BrushType) typeCB.getSelectedItem();
    }

    @Override
    public void paintOverImage(Graphics2D g2, Composition comp) {
        if (paintBrushOutline) {
            outlinePainter.paint(g2, outlineCoX, outlineCoY);
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
    public boolean allowOnlyDrawables() {
        return true;
    }

    @Override
    public DebugNode createDebugNode() {
        var node = super.createDebugNode();

        if (typeCB != null) { // can be null, for example in Clone
            node.addString("brush type", getBrushType().toString());
        }
        node.addInt("radius", getRadius());
        node.add(brush.createDebugNode());

        if (symmetryBrush != null) { // can be null, for example in Clone
            node.addString("symmetry", getSymmetry().toString());
            if (symmetryBrush != brush) {
                node.add(symmetryBrush.createDebugNode());
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
        if (lazyMouse) {
            sb.append(", (lazy " + UNICODE_MOUSE_SYMBOL + " d=")
                .append(lazyMouseDist.getValue())
                .append(")");
        } else {
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
            imRadius = radius;
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
            var origTransform = g2.getTransform();

            g2.translate(x - coRadius - 1, y - coRadius - 1);
            super.paint(g2, null, 3 + (int) coDiameter, 3 + (int) coDiameter);

            g2.setTransform(origTransform);
        }

        public int getCoRadius() {
            return (int) coRadius;
        }
    }
}
