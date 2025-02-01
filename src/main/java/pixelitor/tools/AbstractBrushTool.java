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
 * Abstract base class for tools that work with {@link Brush} objects.
 */
public abstract class AbstractBrushTool extends Tool {
    private static final int MIN_BRUSH_RADIUS = 1;
    public static final int MAX_BRUSH_RADIUS = 500;
    public static final int DEFAULT_BRUSH_RADIUS = 10;

    // some extra space is added to the repaint region
    // since repaint() is asynchronous
    private static final int REPAINT_EXTRA_SPACE = 20;

    private static final String UNICODE_MOUSE_SYMBOL = new String(Character.toChars(0x1F42D));

    private JComboBox<BrushType> typeCB;

    private final RangeParam brushRadiusParam = new RangeParam(GUIText.RADIUS,
        MIN_BRUSH_RADIUS, DEFAULT_BRUSH_RADIUS, MAX_BRUSH_RADIUS, false, WEST);

    private final boolean supportsSymmetry;
    private EnumComboBoxModel<Symmetry> symmetryModel;

    protected BrushStroke brushStroke;
    protected Brush brush;
    private SymmetryBrush symmetryBrush;
    protected AffectedArea affectedArea;
    protected LazyMouseBrush lazyMouseBrush;

    private JButton brushSettingsDialogButton;
    private Action brushSettingsAction;
    private JDialog settingsDialog;

    protected DrawTarget drawTarget;

    protected boolean lazyMouse;
    // the name of the param is used only as the preset key
    protected final BooleanParam lazyMouseEnabled = new BooleanParam("Lazy.Enabled");
    private final RangeParam lazyMouseDist = LazyMouseBrush.createDistParam();
    private JDialog lazyMouseDialog;
    private JButton showLazyMouseDialogButton;

    // Current brush outline coordinates in component space.
    // If lazy mouse is enabled, then they are lagging behind the mouse.
    private int outlineCoX;
    private int outlineCoY;

    private final BrushOutlinePainter brushPainter = new BrushOutlinePainter(DEFAULT_BRUSH_RADIUS);
    private boolean brushPainted = false;

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

    protected void initBrushVariables() {
        symmetryBrush = new SymmetryBrush(
            this, BrushType.values()[0], getSymmetry(), getRadius());
        brush = symmetryBrush;
        affectedArea = symmetryBrush.getAffectedArea();
    }

    // Called when the laziness is either enabled or disabled.
    // if initBrushVariables() is overridden,
    // then this must also be overridden
    protected void updateLazyMouseEnabledState() {
        if (lazyMouseEnabled.isChecked()) {
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

    protected void addSymmetryCombo() {
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
        newMousePoint(e, false);

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
        if (brushStroke == null) {
            // we can get here if the mousePressed was an Alt-press, therefore
            // consumed by the color picker. Nothing was drawn, therefore
            // there is no need to save a backup, we can just return
            return;
        }

        // Whether or not it is a lazy mouse, reset
        // the outline back to the mouse coordinates
        outlineCoX = (int) e.getCoX();
        outlineCoY = (int) e.getCoY();

        Composition comp = e.getComp();
        Drawable dr = comp.getActiveDrawableOrThrow();
        finishBrushStroke(dr);

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

        // calculates the rectangle that encompasses both the old and new positions
        var repaintRect = Shapes.toPositiveRect(prevX, outlineCoX, prevY, outlineCoY);

        // add padding to account for brush radius and repaint delay
        int growth = brushPainter.getCoRadius() + REPAINT_EXTRA_SPACE;
        repaintRect.grow(growth, growth);
        view.repaint(repaintRect);
    }

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
        brushPainted = typeCB == null || getBrushType() != BrushType.ONE_PIXEL;
        outlineCoX = x;
        outlineCoY = y;
        repaintOutline(view);
    }

    private void hideOutline(View view) {
        hideOutlineAt(outlineCoX, outlineCoY, view);
    }

    private void hideOutlineAt(int x, int y, View view) {
        brushPainted = false;
        updateOutlinePosition(x, y, view);
    }

    private void finishBrushStroke(Drawable dr) {
        brush.finishBrushStroke();
        addBrushStrokeToHistory(dr);

        assert brushStroke != null;
        if (brushStroke != null) {
            brushStroke.finish(dr);
        }
        brushStroke = null;
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
        brushStroke = new BrushStroke(dr, drawTarget, brush, getComposite());
        initBrushStroke();
    }

    /**
     * An opportunity to do extra tool-specific
     * initializations in the subclasses
     */
    protected void initBrushStroke() {
    }

    // overridden in brush tools with blending mode
    protected Composite getComposite() {
        return null;
    }

    /**
     * Called from mousePressed, mouseDragged
     */
    private void newMousePoint(PMouseEvent p, boolean lineConnect) {
        Drawable dr = p.getComp().getActiveDrawableOrThrow();
        if (brushStroke == null) { // a new brush stroke has to be initialized
            createBrushStroke(dr);

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

    private void updateDrawingRadius() {
        int newRadius = getRadius();
        brush.setRadius(newRadius);

        brushPainter.setRadius(newRadius);
        if (brushPainted) {
            // changing the brush via keyboard shortcut
            repaintOutline(Views.getActive());
        }
    }

    @Override
    protected void toolActivated() {
        super.toolActivated();
        reset();

        View view = Views.getActive();
        if (view != null) {
            brushPainter.setView(view);

            // If the tool is started with the hotkey, then there is
            // no mouseEntered event to start the outline painting
            showOutline(view);
        }
    }

    @Override
    protected void toolDeactivated() {
        super.toolDeactivated();

        View view = Views.getActive();
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
        // the tracing is done with temporarily disabled lazy mouse
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

    // Does the actual shape tracing by following the shape's path segments.
    // Handles multiple subpaths within the shape.
    private void doTrace(Drawable dr, Shape shape) {
        View view = dr.getComp().getView();

        // the current tracing state
        PPoint subPathStart = null;
        boolean isFirstPoint = true;
        boolean brushStrokePrepared = false;
        int subPathIndex = -1;

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
    }

    public void decreaseBrushSize() {
        brushRadiusParam.decreaseValue();
    }

    protected Symmetry getSymmetry() {
        assert supportsSymmetry;

        return symmetryModel.getSelectedItem();
    }

    protected int getRadius() {
        return brushRadiusParam.getValue();
    }

    @Override
    public boolean hasColorPickerForwarding() {
        return true;
    }

    protected Brush getBrush() {
        return brush;
    }

    protected void setBrush(Brush brush) {
        this.brush = brush;
    }

    public BrushType getBrushType() {
        return (BrushType) typeCB.getSelectedItem();
    }

    @Override
    public void paintOverView(Graphics2D g2, Composition comp) {
        if (brushPainted) {
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
        return true;
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        if (hasBrushType()) {
            BrushType brushType = getBrushType();
            preset.put("Brush Type", brushType.toString());
            if (brushType.hasSettings()) {
                BrushSettings settings = brushType.getSettings(this);
                settings.saveStateTo(preset);
            }
            if (brushType.hasRadius()) {
                brushRadiusParam.saveStateTo(preset);
            }
        } else {
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
