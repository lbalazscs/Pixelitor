/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.AppMode;
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.GUIText;
import pixelitor.gui.View;
import pixelitor.gui.utils.*;
import pixelitor.history.History;
import pixelitor.history.MultiEdit;
import pixelitor.history.PartialImageEdit;
import pixelitor.layers.Drawable;
import pixelitor.tools.brushes.*;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.Shapes;
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
import static pixelitor.gui.GUIText.CLOSE_DIALOG;
import static pixelitor.gui.utils.SliderSpinner.LabelPosition.WEST;

/**
 * Abstract base class for tools that draw using {@link Brush} objects.
 */
public abstract class AbstractBrushTool extends Tool {
    private static final int MIN_BRUSH_RADIUS = 1;
    public static final int MAX_BRUSH_RADIUS = 500;
    public static final int DEFAULT_BRUSH_RADIUS = 10;

    // margin around the outline's repaint region, because repaint() is
    // asynchronous and the outline may have moved by the time it paints
    private static final int OUTLINE_REPAINT_MARGIN = 20;

    private static final String UNICODE_MOUSE_SYMBOL = Character.toString(0x1F42D);

    private JComboBox<BrushType> typeCB;

    private final RangeParam brushRadiusParam = new RangeParam(GUIText.RADIUS,
        MIN_BRUSH_RADIUS, DEFAULT_BRUSH_RADIUS, MAX_BRUSH_RADIUS, false, WEST);

    private final boolean supportsSymmetry;
    private EnumComboBoxModel<Symmetry> symmetryModel;

    private BrushContext brushContext; // null between strokes

    // the innermost brush instance
    private Brush coreBrush;

    // the active brush instance (decorated with symmetry, lazy mouse, and affected area tracking)
    private Brush brush;

    // the brush responsible for symmetry
    // (null when the tool doesn't support symmetry)
    private SymmetryBrush symmetryBrush;

    // non-null only while lazy mouse is active
    private LazyMouseBrush lazyMouseBrush;

    private final AffectedArea affectedArea = new AffectedArea();
    private JButton brushSettingsDialogButton;
    private Action brushSettingsAction;
    private JDialog brushSettingsDialog;

    // defines how drawing occurs (directly or via temporary layer)
    protected DrawTarget drawTarget;

    // the parameter controlling whether the lazy mouse is enabled;
    // the param's name is used only as a preset key
    protected final BooleanParam lazyMouseEnabled = new BooleanParam("Lazy.Enabled");

    // the parameter controlling the lazy mouse distance
    private final RangeParam lazyMouseDist = LazyMouseBrush.createDistParam();

    private JDialog lazyMouseDialog;
    private JButton lazyMouseDialogButton;

    // current brush outline coordinates in component space
    // (lags behind the mouse if lazy mouse is enabled)
    private int outlineCoX;
    private int outlineCoY;

    private final BrushOutlinePainter brushPainter = new BrushOutlinePainter(DEFAULT_BRUSH_RADIUS);

    private boolean outlineVisible = false;

    AbstractBrushTool(String name, char hotkey, String statusBarMessage,
                      Cursor cursor, boolean supportsSymmetry) {
        super(name, hotkey, statusBarMessage, cursor);
        this.supportsSymmetry = supportsSymmetry;
        if (supportsSymmetry) {
            symmetryModel = new EnumComboBoxModel<>(Symmetry.class);
        }
        coreBrush = createCoreBrush();
        updateActiveBrush();

        assert (symmetryBrush != null) == supportsSymmetry;
    }

    /**
     * Creates the core brush to be decorated with lazy mouse and affected-area tracking.
     * <p>
     * Note: this method is called from the {@link AbstractBrushTool} constructor,
     * so subclass fields assigned here must not have field initializers (otherwise the
     * initializers will overwrite the values assigned during the super constructor).
     */
    protected Brush createCoreBrush() {
        symmetryBrush = new SymmetryBrush(
            this, BrushType.values()[0], getSymmetry(), getRadius(), affectedArea);
        return symmetryBrush;
    }

    /**
     * Updates the active brush based on the lazy mouse enabled state.
     */
    private void updateActiveBrush() {
        if (lazyMouseEnabled.isChecked()) {
            lazyMouseBrush = createLazyMouseBrush(coreBrush);
            brush = new AffectedAreaTracker(lazyMouseBrush, affectedArea);
        } else {
            brush = new AffectedAreaTracker(coreBrush, affectedArea);
            lazyMouseBrush = null;
        }
    }

    private LazyMouseBrush createLazyMouseBrush(Brush core) {
        return new LazyMouseBrush(core, lazyMouseDist::getValue);
    }

    protected void addTypeSelector() {
        typeCB = GUIUtils.createComboBox(BrushType.values(), _ -> brushTypeChanged());
        settingsPanel.addComboBox(GUIText.BRUSH + ":", typeCB, "typeCB");
    }

    private void brushTypeChanged() {
        closeBrushSettingsDialog();

        BrushType newBrushType = getBrushType();
        symmetryBrush.updateBrushType(newBrushType, getRadius());
        brushRadiusParam.setEnabled(newBrushType.hasRadius());
        brushSettingsAction.setEnabled(newBrushType.hasSettings());
    }

    private boolean hasBrushType() {
        return typeCB != null;
    }

    protected void addRadiusSelector() {
        settingsPanel.add(brushRadiusParam.createGUI());
        brushRadiusParam.setAdjustmentListener(this::updateDrawingRadius);
        updateDrawingRadius();
    }

    protected void addSymmetrySelector() {
        assert supportsSymmetry;

        @SuppressWarnings("unchecked")
        var symmetryCB = new JComboBox<Symmetry>(symmetryModel);

        settingsPanel.addComboBox(GUIText.MIRROR + ":", symmetryCB, "symmetrySelector");
        symmetryCB.addActionListener(_ ->
            symmetryBrush.updateSymmetry(getSymmetry(), getRadius()));
    }

    protected void addBrushSettingsButton() {
        brushSettingsAction = new TaskAction("Settings...",
            this::showBrushSettingsDialog);
        brushSettingsDialogButton = settingsPanel.addButton(brushSettingsAction,
            "brushSettingsDialogButton", "Configure the selected brush");

        brushSettingsAction.setEnabled(false);
    }

    private void showBrushSettingsDialog() {
        BrushType brushType = getBrushType();
        assert brushType.hasSettings();

        brushSettingsDialog = new DialogBuilder()
            .content(brushType.getSettings(this).getConfigPanel())
            .title("Settings for the " + brushType + " Brush")
            .modeless()
            .withScrollbars()
            .okText(CLOSE_DIALOG)
            .noCancelButton()
            .parentComponent(brushSettingsDialogButton)
            .show()
            .getDialog();
    }

    protected void addLazyMouseDialogButton() {
        lazyMouseDialogButton = settingsPanel.addButton(
            "Lazy Mouse...", _ -> showLazyMouseDialog(),
            "lazyMouseDialogButton", "Configure brush smoothing");
    }

    private void showLazyMouseDialog() {
        if (lazyMouseDialog != null) {
            GUIUtils.showDialog(lazyMouseDialog, lazyMouseDialogButton);
            return;
        }

        JPanel configPanel = createLazyMouseConfigPanel();
        lazyMouseDialog = new DialogBuilder()
            .content(configPanel)
            .title("Lazy Mouse Settings")
            .modeless()
            .reusable()
            .okText(CLOSE_DIALOG)
            .noCancelButton()
            .parentComponent(lazyMouseDialogButton)
            .show()
            .getDialog();
    }

    private JPanel createLazyMouseConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(createEmptyBorder(5, 5, 5, 5));
        var gbh = new GridBagHelper(panel);

        lazyMouseEnabled.setAdjustmentListener(this::updateActiveBrush);
        gbh.addLabelAndControlNoStretch("Enabled:", lazyMouseEnabled.createGUI());

        var distSlider = lazyMouseDist.createGUI("distSlider");
        gbh.addLabelAndControl(lazyMouseDist.getName() + ":", distSlider);

        lazyMouseEnabled.enableOtherWhenChecked(lazyMouseDist);
        return panel;
    }

    // returns true if lazy mouse smoothing is active
    public boolean isLazyMouseActive() {
        return lazyMouseBrush != null;
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        // starts a new stroke, or connects to the previous
        // point with a straight line if Shift is held
        boolean lineConnect = e.isShiftDown() && brush.hasPrevPos();

        // the affected area is tracked by the brush (and reset in createBrushContext)
        processStrokePoint(e, lineConnect);
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        processStrokePoint(e, false); // continue the stroke

        if (isLazyMouseActive()) {
            PPoint drawLoc = lazyMouseBrush.getDrawLocation();
            outlineCoX = (int) drawLoc.getCoX();
            outlineCoY = (int) drawLoc.getCoY();
        } else {
            outlineCoX = (int) e.getCoX();
            outlineCoY = (int) e.getCoY();
        }
    }

    @Override
    public void mouseReleased(PMouseEvent e) {
        if (brushContext == null) {
            // can happen if Alt-press was consumed by color picker; nothing was drawn
            return;
        }

        // regardless of whether lazy mouse is enabled, reset
        // the outline back to the actual mouse coordinates
        outlineCoX = (int) e.getCoX();
        outlineCoY = (int) e.getCoY();

        finishBrushStroke();

        // repaint needed if lazy mouse caused drawing lag
        if (isLazyMouseActive()) {
            e.getView().repaint();
        }
    }

    @Override
    public void mouseEntered(MouseEvent e, View view) {
        showOutlineAt(e.getX(), e.getY(), view);
    }

    @Override
    public void mouseExited(MouseEvent e, View view) {
        hideOutlineAt(e.getX(), e.getY(), view);
    }

    @Override
    public void mouseMoved(MouseEvent e, View view) {
        moveOutlineTo(e.getX(), e.getY(), view);
    }

    @Override
    public void escPressed() {
        // do nothing
    }

    private void moveOutlineTo(int coX, int coY, View view) {
        int prevX = outlineCoX;
        int prevY = outlineCoY;

        outlineCoX = coX;
        outlineCoY = coY;

        // calculate the rectangle encompassing both old and new positions
        var repaintRect = Shapes.posRectFromCorners(prevX, prevY, outlineCoX, outlineCoY);

        // add padding to account for brush radius and repaint delay
        int growth = brushPainter.getCoRadius() + OUTLINE_REPAINT_MARGIN;
        repaintRect.grow(growth, growth);
        view.repaint(repaintRect);
    }

    // repaints the area currently occupied by the brush outline
    private void repaintOutline(View view) {
        int growth = brushPainter.getCoRadius() + OUTLINE_REPAINT_MARGIN;

        view.repaint(outlineCoX - growth, outlineCoY - growth, 2 * growth, 2 * growth);
    }

    private void showOutline(View view) {
        Point mousePos = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(mousePos, view);
        Rectangle visibleRegion = view.getVisibleRegion();
        if (visibleRegion != null) {
            if (visibleRegion.contains(mousePos)) {
                showOutlineAt(mousePos.x, mousePos.y, view);
            }
        } else if (!AppMode.isUnitTesting()) {
            throw new IllegalStateException();
        }
    }

    private void showOutlineAt(int coX, int coY, View view) {
        outlineVisible = typeCB == null || getBrushType() != BrushType.ONE_PIXEL;
        outlineCoX = coX;
        outlineCoY = coY;
        repaintOutline(view);
    }

    private void hideOutline(View view) {
        hideOutlineAt(outlineCoX, outlineCoY, view);
    }

    private void hideOutlineAt(int x, int y, View view) {
        outlineVisible = false;
        moveOutlineTo(x, y, view);
    }

    private void finishBrushStroke() {
        assert brushContext != null;

        Drawable dr = brushContext.getDrawable();

        brush.finishBrushStroke();
        addBrushStrokeToHistory(dr);

        brushContext.finish();
        brushContext = null;
    }

    private void addBrushStrokeToHistory(Drawable dr) {
        var originalImage = brushContext.getOriginalImage();

        double maxBrushRadius = brush.getMaxEffectiveRadius();
        var affectedRect = affectedArea.toRectangle(maxBrushRadius);
        assert !affectedRect.isEmpty() : "brush radius = " + maxBrushRadius
            + ", affected area = " + affectedArea;

        PartialImageEdit imageEdit = PartialImageEdit.create(
            affectedRect, originalImage, dr, false, getName());
        if (imageEdit != null) { // there was a change
            if (hasBrushType() && getBrushType() == BrushType.CONNECT) {
                Composition comp = dr.getComp();
                History.add(new MultiEdit(imageEdit.getName(), comp,
                    imageEdit, new ConnectBrushHistory.Edit(comp)));
            } else {
                History.add(imageEdit);
            }
        }
    }

    // called before the first point of a programmatic stroke, e.g. tracing
    protected void prepareProgrammaticBrushStroke(Drawable dr, PPoint start) {
        createBrushContext(dr, start, false);
    }

    /**
     * Creates the context for a new undoable brush edit. This is the only
     * place where the affected area is reset.
     *
     * @param lineConnect true for a Shift-click line, which keeps the
     *                    old area because it contains the line's start
     */
    private void createBrushContext(Drawable dr, PPoint start, boolean lineConnect) {
        if (!lineConnect) {
            affectedArea.reset();
        }
        brushContext = new BrushContext(dr, drawTarget, brush, getComposite());
        initBrushContext(brushContext);
        strokeStarting(dr, start, lineConnect);
    }

    /**
     * Called once per undoable stroke, after the {@link BrushContext} exists,
     * before the first brush call.
     */
    protected void strokeStarting(Drawable dr, PPoint start, boolean lineConnect) {
        // default implementation does nothing
    }

    /**
     * Hook for subclasses to perform tool-specific initialization on the {@link BrushContext}.
     */
    protected void initBrushContext(BrushContext ctx) {
        // default implementation does nothing
    }

    // overridden in brush tools with blending mode
    protected Composite getComposite() {
        return null;
    }

    /**
     * Processes a new mouse point during a drawing operation (press or drag).
     */
    private void processStrokePoint(PMouseEvent p, boolean lineConnect) {
        if (brushContext == null) { // start of a new stroke
            Drawable dr = p.getComp().getActiveDrawableOrThrow();
            createBrushContext(dr, p, lineConnect);

            if (lineConnect) {
                brush.connectWithLineTo(p);
            } else {
                brush.startStrokeAt(p);
            }
        } else if (brush.hasPrevPos()) { // continuation of an existing stroke
            brush.continueTo(p);
        } else {
            // there is a brush stroke, but the brush has no previous position
            // TODO why does this happen sometimes in random tests?
            //   Perhaps after programmatic changes?
            brush.startStrokeAt(p);
        }
    }

    private void updateDrawingRadius() {
        int newRadius = getRadius();
        brush.setRadius(newRadius);

        brushPainter.setRadius(newRadius);
        if (outlineVisible) {
            repaintOutline(Views.getActive());
        }
    }

    @Override
    protected void toolActivated(View view) {
        super.toolActivated(view);

        if (view != null) {
            brushPainter.setView(view);

            // show the outline if activated via hotkey when the mouse
            // is already over the view (no mouseEntered event)
            showOutline(view);
        }
    }

    @Override
    protected void toolDeactivated(View view) {
        super.toolDeactivated(view);

        if (view != null) {
            hideOutline(view);
        }
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        brushPainter.setView(newView);

        // get rid of the outline on the old view
        // (important in "Internal Windows" mode)
        if (oldView != null) {
            oldView.repaint();
        }

        // make sure that the mouse coordinates are correct relative to the new view
        updateOutlineForView(newView);
    }

    @Override
    public void coCoordsChanged(View view) {
        // use invokeLater to ensure coordinates are calculated after the UI changes
        EventQueue.invokeLater(() -> updateOutlineForView(view));
    }

    private void updateOutlineForView(View view) {
        Point mousePos = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(mousePos, view);
        brushPainter.setView(view);
        moveOutlineTo(mousePos.x, mousePos.y, view);
    }

    @Override
    public void modalDialogShown() {
        // the outline has to be hidden, because there is no mouseExited event
        View view = Views.getActive();
        if (view != null) {
            hideOutline(view);
        }
    }

    @Override
    public void modalDialogHidden() {
        // the outline has to be shown again, because there is no mouseEntered event
        View view = Views.getActive();
        if (view != null) {
            showOutline(view);
        }
    }

    /**
     * Traces the given shape with the current brush tool.
     * The given shape must be in image coordinates.
     */
    public void trace(Shape shape, Drawable dr) {
        assert brushContext == null;

        // temporarily disable the lazy mouse, because otherwise
        // the brush would "cut corners" instead of following the shape
        Brush savedBrush = brush;
        LazyMouseBrush savedLazy = lazyMouseBrush;
        brush = new AffectedAreaTracker(coreBrush, affectedArea); // no lazy mouse
        lazyMouseBrush = null;

        try {
            traceShape(shape, dr);
            // only finish if traceShape() actually started a context (i.e. shape isn't empty)
            if (brushContext != null) {
                finishBrushStroke();
            }
        } finally {
            brush = savedBrush;
            lazyMouseBrush = savedLazy;
        }
    }

    // performs the actual shape tracing by iterating path segments
    private void traceShape(Shape shape, Drawable dr) {
        View view = dr.getComp().getView();

        // the current tracing state
        PPoint subPathStart = null;
        boolean brushStrokePrepared = false;
        int subPathIndex = -1; // the index of the current subpath (-1 before the first)

        float[] coords = new float[2];
        var pathIterator = new FlatteningPathIterator(shape.getPathIterator(null), 1.0);
        while (!pathIterator.isDone()) {
            int segmentType = pathIterator.currentSegment(coords);
            PPoint pathPoint = PPoint.lazyFromIm(coords[0], coords[1], view);

            // the affected area is fed by the brush calls below; it was reset
            // once in prepareProgrammaticBrushStroke and accumulates all subpaths
            switch (segmentType) {
                case SEG_MOVETO -> {
                    // we can get here more than once if there are multiple subpaths!
                    subPathIndex++;
                    subPathStart = pathPoint;

                    if (!brushStrokePrepared) {
                        prepareProgrammaticBrushStroke(dr, pathPoint);
                        brushStrokePrepared = true;
                    }

                    if (subPathIndex > 0) {
                        // finish the previous brush stroke before starting a new subpath
                        brush.finishBrushStroke();
                    }
                    brush.startStrokeAt(pathPoint);
                }
                case SEG_LINETO -> brush.continueTo(pathPoint);
                case SEG_CLOSE -> brush.continueTo(subPathStart);
                default -> throw new IllegalArgumentException("segmentType = " + segmentType);
            }

            pathIterator.next();
        }
    }

    public void increaseBrushRadius() {
        brushRadiusParam.increaseValue();
        // the attached listener handles updates
    }

    public void decreaseBrushRadius() {
        brushRadiusParam.decreaseValue();
        // the attached listener handles updates
    }

    protected Symmetry getSymmetry() {
        assert supportsSymmetry;

        return symmetryModel.getSelectedItem();
    }

    /**
     * Returns the current brush radius in image-space pixels.
     */
    protected int getRadius() {
        return brushRadiusParam.getValue();
    }

    @Override
    public boolean hasColorPickerForwarding() {
        return true; // by default allow Alt-click for color picking
    }

    protected Brush getBrush() {
        return brush;
    }

    protected void setBrush(Brush brush) {
        assert AppMode.isUnitTesting();

        this.brush = brush;
    }

    public BrushType getBrushType() {
        assert hasBrushType();

        return (BrushType) typeCB.getSelectedItem();
    }

    @Override
    public void paintOverCanvas(Graphics2D g, Composition comp) {
        if (outlineVisible) {
            brushPainter.paint(g, outlineCoX, outlineCoY);
        }
    }

    @Override
    protected void closeAllDialogs() {
        closeBrushSettingsDialog();
        GUIUtils.closeDialog(lazyMouseDialog, false);
    }

    private void closeBrushSettingsDialog() {
        GUIUtils.closeDialog(brushSettingsDialog, true);
    }

    @Override
    public boolean requiresDrawables() {
        return true; // brush tools operate on drawable layers or on masks
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        if (hasBrushType()) {
            BrushType brushType = getBrushType();
            preset.put(BrushType.PRESET_KEY, brushType.name());
            if (brushType.hasRadius()) {
                brushRadiusParam.saveStateTo(preset);
            }
            if (brushType.hasSettings()) {
                BrushSettings settings = brushType.getSettings(this);
                settings.saveStateTo(preset);
            }
        } else {
            // tools without a brush type always have a radius selector
            brushRadiusParam.saveStateTo(preset);
        }

        if (supportsSymmetry) {
            preset.put(Symmetry.PRESET_KEY, symmetryModel.getSelectedItem().name());
        }

        lazyMouseEnabled.saveStateTo(preset);
        lazyMouseDist.saveStateTo(preset);
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        if (hasBrushType()) {
            BrushType type = preset.getEnum(BrushType.PRESET_KEY, BrushType.class);
            typeCB.setSelectedItem(type);
            if (type.hasSettings()) {
                BrushSettings settings = type.getSettings(this);
                settings.loadStateFrom(preset);
            }
            if (type.hasRadius()) {
                brushRadiusParam.loadStateFrom(preset);
                updateDrawingRadius();
            }
        } else {
            brushRadiusParam.loadStateFrom(preset);
            updateDrawingRadius();
        }

        if (supportsSymmetry) {
            symmetryModel.setSelectedItem(preset.getEnum(Symmetry.PRESET_KEY, Symmetry.class));
        }

        lazyMouseEnabled.loadStateFrom(preset);
        lazyMouseDist.loadStateFrom(preset);
        updateActiveBrush();
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        if (hasBrushType()) {
            node.addAsString("brush type", getBrushType());
        }
        node.addInt("radius", getRadius());
        node.add(brush.createDebugNode("brush"));

        if (symmetryBrush != null) { // can be null in tools without symmetry
            node.addAsString("symmetry", getSymmetry());
            if (symmetryBrush != brush) {
                node.add(symmetryBrush.createDebugNode("symmetryBrush"));
            }
        }

        return node;
    }

    @Override
    public String getStateInfo() {
        StringBuilder sb = new StringBuilder(20);
        if (hasBrushType()) {
            sb.append(getBrushType()).append(", ");
        }
        sb.append("r=").append(getRadius());
        if (supportsSymmetry) {
            sb.append(", sym=").append(getSymmetry());
        }
        if (isLazyMouseActive()) {
            sb.append(", (lazy ")
                .append(UNICODE_MOUSE_SYMBOL)
                .append(" d=")
                .append(lazyMouseDist.getValue())
                .append(")");
        }

        return sb.toString();
    }

    /**
     * Paints the brush outline.
     *
     * This is necessary because (at least on Windows) it looks like
     * cursors can't have an arbitrary size, so the outline cannot be
     * implemented via custom cursor images. See {@link java.awt.Toolkit#getBestCursorSize}.
     */
    static class BrushOutlinePainter extends SimpleCachedPainter {
        private static final Stroke OUTER_STROKE = new BasicStroke(3);
        private static final Stroke INNER_STROKE = new BasicStroke(1);

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
            updateCoRadius();
        }

        public void setRadius(int imRadius) {
            this.imRadius = imRadius;
            updateCoRadius();
        }

        private void updateCoRadius() {
            if (view == null) {
                return;
            }
            double radiusBefore = coRadius;
            coRadius = view.getZoomScale() * imRadius;
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

            g.setStroke(OUTER_STROKE);
            g.setColor(Color.BLACK);
            g.draw(shape);

            g.setStroke(INNER_STROKE);
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
