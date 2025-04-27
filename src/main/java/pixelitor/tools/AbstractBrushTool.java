/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import static java.awt.geom.PathIterator.SEG_CLOSE;
import static java.awt.geom.PathIterator.SEG_LINETO;
import static java.awt.geom.PathIterator.SEG_MOVETO;
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

    // extra space added to the repaint region for the
    // brush outline because repaint() is asynchronous
    private static final int REPAINT_EXTRA_SPACE = 20;

    private static final String UNICODE_MOUSE_SYMBOL = new String(Character.toChars(0x1F42D));

    private JComboBox<BrushType> typeCB;

    private final RangeParam brushRadiusParam = new RangeParam(GUIText.RADIUS,
        MIN_BRUSH_RADIUS, DEFAULT_BRUSH_RADIUS, MAX_BRUSH_RADIUS, false, WEST);

    private final boolean supportsSymmetry;
    private EnumComboBoxModel<Symmetry> symmetryModel;

    protected BrushContext brushContext;

    // the active brush instance (might be decorated with symmetry or lazy mouse)
    protected Brush brush;

    // the brush responsible for symmetry
    private SymmetryBrush symmetryBrush;

    protected LazyMouseBrush lazyMouseBrush;

    protected AffectedArea affectedArea;

    private JButton brushSettingsDialogButton;
    private Action brushSettingsAction;
    private JDialog settingsDialog;

    // defines how drawing occurs (directly or via temporary layer)
    protected DrawTarget drawTarget;

    // true if lazy mouse smoothing is active
    protected boolean lazyMouse;

    // the parameter controlling the lazy mouse enabled state
    // the name of the param is used only as the preset key
    protected final BooleanParam lazyMouseEnabled = new BooleanParam("Lazy.Enabled");

    // the parameter controlling the lazy mouse distance
    private final RangeParam lazyMouseDist = LazyMouseBrush.createDistParam();

    private JDialog lazyMouseDialog;
    private JButton showLazyMouseDialogButton;

    // current brush outline coordinates in component space
    // (lags behind the mouse if lazy mouse is enabled)
    private int outlineCoX;
    private int outlineCoY;

    private final BrushOutlinePainter brushPainter = new BrushOutlinePainter(DEFAULT_BRUSH_RADIUS);

    // tracks whether the brush outline should be painted
    private boolean paintBrushOutline = false;

    AbstractBrushTool(String name, char hotkey, String toolMessage,
                      Cursor cursor, boolean supportsSymmetry) {
        super(name, hotkey, toolMessage, cursor);
        this.supportsSymmetry = supportsSymmetry;
        if (supportsSymmetry) {
            symmetryModel = new EnumComboBoxModel<>(Symmetry.class);
        }
        initBrushVariables();

        assert (symmetryBrush != null) == supportsSymmetry;
    }

    /**
     * Initializes the core brush instances.
     * Subclasses might override this to use different core brushes.
     */
    protected void initBrushVariables() {
        symmetryBrush = new SymmetryBrush(
            this, BrushType.values()[0], getSymmetry(), getRadius());
        // by default, the active brush is the symmetry brush
        brush = symmetryBrush;
        affectedArea = symmetryBrush.getAffectedArea();
    }

    /**
     * Updates the active brush based on the lazy mouse enabled state.
     * This method must be overridden if {@link #initBrushVariables()} is overridden.
     */
    protected void updateLazyMouseEnabledState() {
        if (lazyMouseEnabled.isChecked()) {
            // decorate the symmetry brush with lazy mouse functionality
            lazyMouseBrush = new LazyMouseBrush(symmetryBrush);
            brush = lazyMouseBrush;
            lazyMouse = true;
        } else {
            // use the undecorated symmetry brush
            brush = symmetryBrush;
            lazyMouseBrush = null;
            lazyMouse = false;
        }
    }

    protected void addTypeSelector() {
        typeCB = GUIUtils.createComboBox(BrushType.values());
        settingsPanel.addComboBox(GUIText.BRUSH + ":", typeCB, "typeCB");
        typeCB.addActionListener(e -> brushTypeChanged());
    }

    private void brushTypeChanged() {
        closeBrushSettingsDialog();

        BrushType newBrushType = getBrushType();
        symmetryBrush.brushTypeChanged(newBrushType, getRadius());
        brushRadiusParam.setEnabled(newBrushType.hasRadius());
        brushSettingsAction.setEnabled(newBrushType.hasSettings());
    }

    private boolean hasBrushType() {
        return typeCB != null;
    }

    protected void addSizeSelector() {
        settingsPanel.add(brushRadiusParam.createGUI());
        brushRadiusParam.setAdjustmentListener(this::updateDrawingRadius);
        updateDrawingRadius();
    }

    protected void addSymmetrySelector() {
        assert supportsSymmetry;

        @SuppressWarnings("unchecked")
        var symmetryCB = new JComboBox<Symmetry>(symmetryModel);

        settingsPanel.addComboBox(GUIText.MIRROR + ":", symmetryCB, "symmetrySelector");
        symmetryCB.addActionListener(e ->
            symmetryBrush.symmetryChanged(getSymmetry(), getRadius()));
    }

    protected void addBrushSettingsButton() {
        brushSettingsAction = new TaskAction("Settings...",
            this::showBrushSettingsDialog);
        brushSettingsDialogButton = settingsPanel.addButton(brushSettingsAction,
            "brushSettingsDialogButton", "Configure the selected brush");

        brushSettingsAction.setEnabled(false);
    }

    private void showBrushSettingsDialog() {
        var brushType = getBrushType();
        assert brushType.hasSettings();
        
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
        showLazyMouseDialogButton = settingsPanel.addButton(
            "Lazy Mouse...", e -> showLazyMouseDialog(),
            "lazyMouseDialogButton", "Configure brush smoothing");
    }

    private void showLazyMouseDialog() {
        if (lazyMouseDialog != null) {
            GUIUtils.showDialog(lazyMouseDialog, showLazyMouseDialogButton);
            return;
        }

        JPanel configPanel = createLazyMouseConfigPanel();
        lazyMouseDialog = new DialogBuilder()
            .content(configPanel)
            .title("Lazy Mouse Settings")
            .notModal()
            .reusable()
            .okText(CLOSE_DIALOG)
            .noCancelButton()
            .parentComponent(showLazyMouseDialogButton)
            .show()
            .getDialog();
    }

    private JPanel createLazyMouseConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(createEmptyBorder(5, 5, 5, 5));
        var gbh = new GridBagHelper(panel);

        lazyMouseEnabled.setAdjustmentListener(this::updateLazyMouseEnabledState);
        gbh.addLabelAndControlNoStretch("Enabled:", lazyMouseEnabled.createGUI());

        var distSlider = lazyMouseDist.createGUI("distSlider");
        gbh.addLabelAndControl(lazyMouseDist.getName() + ":", distSlider);

        lazyMouseEnabled.setupEnableOtherIfChecked(lazyMouseDist);
        return panel;
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        // start a new brush stroke or continue with line connect (Shift)
        boolean lineConnect = e.isShiftDown() && brush.hasPrevious();
        newMousePoint(e, lineConnect);

        // if it can have symmetry, then the symmetry brush does
        // the tracking of the affected area
        if (!supportsSymmetry) {
            if (lineConnect) {
                assert brush.hasPrevious();
                affectedArea.updateWith(e);
            } else {
                affectedArea.initAt(e);
            }
        }
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        newMousePoint(e, false); // continue the stroke

        if (lazyMouse) {
            PPoint drawLoc = lazyMouseBrush.getDrawLoc();
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

        // Whether or not it is a lazy mouse, reset
        // the outline back to the mouse coordinates
        outlineCoX = (int) e.getCoX();
        outlineCoY = (int) e.getCoY();

        Composition comp = e.getComp();
        Drawable dr = comp.getActiveDrawableOrThrow();
        finishBrushStroke(dr);

        // repaint needed if lazy mouse caused drawing lag
        if (lazyMouse) {
            comp.repaint();
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
        updateOutlinePosition(e.getX(), e.getY(), view);
    }

    private void updateOutlinePosition(int x, int y, View view) {
        int prevX = outlineCoX;
        int prevY = outlineCoY;

        outlineCoX = x;
        outlineCoY = y;

        // calculate the rectangle encompassing both old and new positions
        var repaintRect = Shapes.toPositiveRect(prevX, outlineCoX, prevY, outlineCoY);

        // add padding to account for brush radius and repaint delay
        int growth = brushPainter.getCoRadius() + REPAINT_EXTRA_SPACE;
        repaintRect.grow(growth, growth);
        view.repaint(repaintRect);
    }

    // repaints the area currently occupied by the brush outline
    private void repaintOutline(View view) {
        int growth = brushPainter.getCoRadius() + REPAINT_EXTRA_SPACE;

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

    private void showOutlineAt(int x, int y, View view) {
        paintBrushOutline = typeCB == null || getBrushType() != BrushType.ONE_PIXEL;
        outlineCoX = x;
        outlineCoY = y;
        repaintOutline(view);
    }

    private void hideOutline(View view) {
        hideOutlineAt(outlineCoX, outlineCoY, view);
    }

    private void hideOutlineAt(int x, int y, View view) {
        paintBrushOutline = false;
        updateOutlinePosition(x, y, view);
    }

    private void finishBrushStroke(Drawable dr) {
        brush.finishBrushStroke();
        addBrushStrokeToHistory(dr);

        assert brushContext != null;
        if (brushContext != null) {
            brushContext.finish(dr);
        }
        brushContext = null;
    }

    private void addBrushStrokeToHistory(Drawable dr) {
        var originalImage = drawTarget.getOriginalImage(dr, this);

        double maxBrushRadius = brush.getMaxEffectiveRadius();
        var affectedRect = affectedArea.toRectangle(maxBrushRadius);
        assert !affectedRect.isEmpty() : "brush radius = " + maxBrushRadius
            + ", affected area = " + affectedArea;

        var imageEdit = PartialImageEdit.create(
            affectedRect, originalImage, dr, false, getName());
        if (imageEdit != null) {
            if (hasBrushType() && getBrushType() == BrushType.CONNECT) {
                Composition comp = dr.getComp();
                History.add(new MultiEdit(imageEdit.getName(), comp,
                    imageEdit, new ConnectBrushHistory.Edit(comp)));
            } else {
                History.add(imageEdit);
            }
        }
    }

    protected void prepareProgrammaticBrushStroke(Drawable dr, PPoint start) {
        createBrushStroke(dr);
    }

    private void createBrushStroke(Drawable dr) {
        brushContext = new BrushContext(dr, drawTarget, brush, getComposite());
        initBrushStroke();
    }

    /**
     * Hook for subclasses to perform tool-specific initialization on the {@link BrushContext}.
     */
    protected void initBrushStroke() {
        // default implementation does nothing
    }

    // overridden in brush tools with blending mode
    protected Composite getComposite() {
        return null;
    }

    /**
     * Processes a new mouse point during a drawing operation (press or drag).
     */
    private void newMousePoint(PMouseEvent p, boolean lineConnect) {
        Drawable dr = p.getComp().getActiveDrawableOrThrow();
        if (brushContext == null) { // start of a new stroke
            createBrushStroke(dr);

            if (lineConnect) {
                brush.lineConnectTo(p);
            } else {
                brush.startAt(p);
            }
        } else if (brush.hasPrevious()) { // continuation of an existing stroke
            brush.continueTo(p);
        } else {
            // there is a brush stroke, but the brush has no previous
            // TODO why does this happen sometimes in random tests?
            //   Perhaps after programmatic changes?
            brush.startAt(p);
        }
    }

    private void updateDrawingRadius() {
        int newRadius = getRadius();
        brush.setRadius(newRadius);

        brushPainter.setRadius(newRadius);
        if (paintBrushOutline) {
            repaintOutline(Views.getActive());
        }
    }

    @Override
    protected void toolActivated(View view) {
        super.toolActivated(view);
        reset();

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
    public void allViewsClosed() {

    }

    @Override
    public void viewActivated(View oldView, View newView) {
        reset();
        brushPainter.setView(newView);

        // get rid of the outline on the old view
        // (important in "Internal Windows" mode)
        if (oldView != null) {
            oldView.repaint();
        }

        // make sure that the mouse coordinates are correct relative to the new view
        paintOutlineOnChangedView(newView);
    }

    @Override
    public void coCoordsChanged(View view) {
        // use invokeLater to ensure coordinates are calculated after the UI changes
        EventQueue.invokeLater(() -> paintOutlineOnChangedView(view));
    }

    private void paintOutlineOnChangedView(View view) {
        Point mousePos = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(mousePos, view);
        brushPainter.setView(view);
        updateOutlinePosition(mousePos.x, mousePos.y, view);
    }

    @Override
    public void firstModalDialogShown() {
        // the outline has to be hidden, because there is no mouseExited event
        View view = Views.getActive();
        if (view != null) {
            hideOutline(view);
        }
    }

    @Override
    public void firstModalDialogHidden() {
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
    public void trace(Drawable dr, Shape shape) {
        // temporarily disable the lazy mouse, because otherwise
        // the mouse would cut corners instead of following the shape
        boolean wasLazy = lazyMouse;
        try {
            if (wasLazy) {
                lazyMouseEnabled.setValue(false, false, false);
            }
            doTrace(dr, shape);
            finishBrushStroke(dr);
        } finally {
            if (wasLazy) {
                lazyMouseEnabled.setValue(true, false, false);
            }
        }
    }

    // performs the actual shape tracing by iterating path segments
    private void doTrace(Drawable dr, Shape shape) {
        View view = dr.getComp().getView();

        // the current tracing state
        PPoint subPathStart = null;
        boolean isFirstPoint = true;
        boolean brushStrokePrepared = false;
        int subPathIndex = -1; // tracks subpaths within the shape

        float[] coords = new float[2];
        var pathIterator = new FlatteningPathIterator(shape.getPathIterator(null), 1.0);
        while (!pathIterator.isDone()) {
            int segmentType = pathIterator.currentSegment(coords);
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
                    subPathStart = pathPoint;

                    if (!brushStrokePrepared) {
                        prepareProgrammaticBrushStroke(dr, pathPoint);
                        brushStrokePrepared = true;
                    }

                    if (subPathIndex > 0) {
                        // finish the previous brush stroke before starting a new subpath
                        brush.finishBrushStroke();
                    }
                    brush.startAt(pathPoint);
                }
                case SEG_LINETO -> brush.continueTo(pathPoint);
                case SEG_CLOSE -> brush.continueTo(subPathStart);
                default -> throw new IllegalArgumentException("segmentType = " + segmentType);
            }

            pathIterator.next();
        }
    }

    public void increaseBrushSize() {
        brushRadiusParam.increaseValue();
        // the attached listener handles updates
    }

    public void decreaseBrushSize() {
        brushRadiusParam.decreaseValue();
        // the attached listener handles updates
    }

    protected Symmetry getSymmetry() {
        assert supportsSymmetry;

        return symmetryModel.getSelectedItem();
    }

    /**
     * Returns the current brush radius in pixels.
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
    public void paintOverCanvas(Graphics2D g2, Composition comp) {
        if (paintBrushOutline) {
            brushPainter.paint(g2, outlineCoX, outlineCoY);
        }
    }

    @Override
    protected void closeAllDialogs() {
        closeBrushSettingsDialog();
        GUIUtils.closeDialog(lazyMouseDialog, false);
    }

    private void closeBrushSettingsDialog() {
        GUIUtils.closeDialog(settingsDialog, true);
    }

    @Override
    public boolean allowOnlyDrawables() {
        return true; // brush tools operate on drawable layers or on masks
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        if (hasBrushType()) {
            BrushType brushType = getBrushType();
            preset.put("Brush Type", brushType.toString());
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
            preset.put("Mirror", symmetryModel.getSelectedItem().toString());
        }

        lazyMouseEnabled.saveStateTo(preset);
        lazyMouseDist.saveStateTo(preset);
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        if (hasBrushType()) {
            BrushType type = preset.getEnum("Brush Type", BrushType.class);
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
            symmetryModel.setSelectedItem(preset.getEnum("Mirror", Symmetry.class));
        }

        lazyMouseEnabled.loadStateFrom(preset);
        lazyMouseDist.loadStateFrom(preset);
        updateLazyMouseEnabledState();
        LazyMouseBrush.setDist(lazyMouseDist.getValue());
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        if (hasBrushType()) {
            node.addAsString("brush type", getBrushType());
        }
        node.addInt("radius", getRadius());
        node.add(brush.createDebugNode("brush"));

        if (symmetryBrush != null) { // can be null, for example in Clone
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
        if (lazyMouse) {
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
     * made via custom brush images. See java.awt.Toolkit.getBestCursorSize.
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
