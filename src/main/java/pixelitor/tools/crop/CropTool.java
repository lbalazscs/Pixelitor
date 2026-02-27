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

package pixelitor.tools.crop;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.compactions.Crop;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.GUIText;
import pixelitor.gui.View;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.guides.GuidesRenderer;
import pixelitor.history.History;
import pixelitor.history.PixelitorEdit;
import pixelitor.tools.DragTool;
import pixelitor.tools.DragToolState;
import pixelitor.tools.ToolIcons;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PRectangle;
import pixelitor.utils.Cursors;
import pixelitor.utils.Messages;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static java.awt.AlphaComposite.SRC_OVER;
import static java.awt.Color.BLACK;
import static pixelitor.gui.utils.SliderSpinner.LabelPosition.WEST;
import static pixelitor.tools.DragToolState.IDLE;
import static pixelitor.tools.DragToolState.INITIAL_DRAG;
import static pixelitor.tools.DragToolState.TRANSFORM;

/**
 * The crop tool.
 */
public class CropTool extends DragTool {
    private CropBox cropBox;

    // stored state before a drag is started (for undo)
    private Rectangle2D rectBefore;
    private boolean allowGrowingBefore;

    private final RangeParam maskOpacity = new RangeParam(
        "Mask Opacity (%)", 0, 75, 100, false, WEST);
    private Composite maskComposite;

    private final JButton cancelButton = new JButton(GUIText.CANCEL);
    private JButton cropButton;

    private JComboBox<CompositionGuideType> guidesCB;
    private final CompositionGuide compositionGuide;

    private final JLabel widthLabel = new JLabel("Width:");
    private final JLabel heightLabel = new JLabel("Height:");
    private JSpinner widthSpinner;
    private JSpinner heightSpinner;
    private boolean userChangedSpinner = true;

    private JCheckBox deleteCroppedCB;
    private JCheckBox allowGrowingCB;
    private static final String DELETE_CROPPED_TEXT = "Delete Cropped Pixels";
    private static final String ALLOW_GROWING_TEXT = "Allow Growing";

    // the result of adjusting the crop box to fit within
    // the canvas bounds if "allow growing" is not enabled
    private enum BoxAdjustmentResult {
        NO_CHANGE,
        ADJUSTED,
        RESET
    }

    public CropTool() {
        super("Crop", 'C',
            "<b>drag</b> to start or <b>Alt-drag</b> to start form the center. " +
                "After handles appear: " +
                "<b>Shift-drag</b> to keep aspect ratio, " +
                "<b>Double-click</b> to crop, <b>Esc</b> to cancel.",
            Cursors.DEFAULT, false);

        repositionOnSpace = true; // allow moving the initial rectangle with spacebar
        pixelSnapping = true; // always snaps to pixels

        compositionGuide = new CompositionGuide(GuidesRenderer.CROP_GUIDES_INSTANCE.get());
        updateMaskOpacity(false);
    }

    @Override
    public void initSettingsPanel(ResourceBundle resources) {
        addMaskOpacitySlider();

        settingsPanel.addSeparator();
        addGuidesSelector();

        settingsPanel.addSeparator();
        addCropSizeControls();

        settingsPanel.addSeparator();
        addCropButton();
        addCancelButton();

        settingsPanel.addSeparator();
        addCropOptionsCheckBoxes();

        setCropEnabled(false); // there's no crop box initially
    }

    private void addMaskOpacitySlider() {
        // use a change listener so that the mask is
        // continuously updated while the slider is dragged
        maskOpacity.addChangeListener(e -> updateMaskOpacity(true));
        settingsPanel.add(maskOpacity.createGUI());
    }

    private void updateMaskOpacity(boolean repaint) {
        float alpha = (float) maskOpacity.getPercentage();
        maskComposite = AlphaComposite.getInstance(SRC_OVER, alpha);
        if (repaint) {
            Views.repaintActive();
        }
    }

    private void addGuidesSelector() {
        guidesCB = GUIUtils.createComboBox(CompositionGuideType.values(),
            e -> guidesChanged());
        guidesCB.setToolTipText("<html>Composition guides." +
            "<br><br>Press <b>O</b> to select the next guide." +
            "<br>Press <b>Shift-O</b> to change the orientation.");
        settingsPanel.addComboBox("Guides:", guidesCB, "guidesCB");
    }

    private void guidesChanged() {
        compositionGuide.setType(getSelectedGuides());
        Views.repaintActive();
    }

    private CompositionGuideType getSelectedGuides() {
        return (CompositionGuideType) guidesCB.getSelectedItem();
    }

    private void addCropSizeControls() {
        // shared change listener for the two size spinners
        ChangeListener sizeChangeListener = e -> sizeSpinnerAdjusted();

        // add crop width spinner
        widthSpinner = createSpinner(sizeChangeListener, Canvas.MAX_WIDTH,
            "Width of the cropped image (px)");
        settingsPanel.add(widthLabel);
        settingsPanel.add(widthSpinner);

        // add crop height spinner
        heightSpinner = createSpinner(sizeChangeListener, Canvas.MAX_HEIGHT,
            "Height of the cropped image (px)");
        settingsPanel.add(heightLabel);
        settingsPanel.add(heightSpinner);
    }

    // called when a size spinner's value is changed by the user
    private void sizeSpinnerAdjusted() {
        if (!userChangedSpinner) {
            // if a spinner was programmatically updated, then
            // there's no need to update the crop box
            return;
        }
        // update the crop box only when handles are shown and not currently being dragged
        if (state != TRANSFORM || cropBox.isAdjusting()) {
            return;
        }

        View view = Views.getActive();
        assert view != null; // the spinners must be disabled if there is no active view
        assert cropBox != null; // we are in TRANSFORM mode

        int newCropWidth = (int) widthSpinner.getValue();
        int newCropHeight = (int) heightSpinner.getValue();

        Rectangle2D rectBeforeSizeChange = cropBox.getImCropRect();
        // doesn't change due to spinner input,
        // so "before" and "after" are the same for this flag
        boolean allowGrowing = allowGrowingCB.isSelected();

        cropBox.setImSize(newCropWidth, newCropHeight, view);

        BoxAdjustmentResult adjustmentResult = BoxAdjustmentResult.NO_CHANGE;
        if (!allowGrowing) {
            // this will also modify the spinners, preventing UI
            // values that would lead to constraint violation
            adjustmentResult = adjustCropBoxToCanvas(view,
                rectBeforeSizeChange, allowGrowing);
        }
        if (adjustmentResult != BoxAdjustmentResult.RESET) {
            // if it was reset, history was already handled
            assert cropBox != null;
            History.add(new CropBoxChangedEdit("Adjust Crop Size", view.getComp(),
                rectBeforeSizeChange, cropBox.getImCropRect(),
                allowGrowing, allowGrowing));
        }
    }

    private static JSpinner createSpinner(ChangeListener listener, int max, String toolTip) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(
            0, 0, max, 1));
        spinner.addChangeListener(listener);
        spinner.setToolTipText(toolTip);
        // setting it to 3 columns seems enough
        // for the range 1-9999, but leave it as 4 for safety
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(4);
        return spinner;
    }

    private void addCropOptionsCheckBoxes() {
        deleteCroppedCB = settingsPanel.addCheckBox(
            DELETE_CROPPED_TEXT, true, "deleteCroppedCB",
            "<html>If checked, pixels outside the crop area are permanently removed." +
                "<br>If unchecked, only the canvas size is reduced, preserving the pixels outside the canvas.");

        allowGrowingCB = settingsPanel.addCheckBox(
            ALLOW_GROWING_TEXT, false, "allowGrowingCB",
            "Enables enlarging the canvas.");

        allowGrowingCB.addActionListener(e -> allowGrowingToggled());
    }

    private void allowGrowingToggled() {
        if (state != TRANSFORM) {
            return;
        }

        View view = Views.getActive();
        assert view != null;

        Rectangle2D rectBeforeToggle = cropBox.getImCropRect();
        boolean isAllowingGrowing = allowGrowingCB.isSelected();
        boolean wasAllowingGrowing = !isAllowingGrowing;

        BoxAdjustmentResult adjustmentResult = BoxAdjustmentResult.NO_CHANGE;
        if (wasAllowingGrowing) {
            // if allow growing is unchecked, then adjust the crop box
            adjustmentResult = adjustCropBoxToCanvas(view,
                rectBeforeToggle, wasAllowingGrowing);
        }

        if (adjustmentResult == BoxAdjustmentResult.ADJUSTED) {
            assert cropBox != null;
            updateSizeSpinners(getEffectiveImCropRect(view));
            view.repaint();

            History.add(new CropBoxChangedEdit("Toggle Allow Growing", view.getComp(),
                rectBeforeToggle, cropBox.getImCropRect(),
                wasAllowingGrowing, isAllowingGrowing));
        }
    }

    private void addCropButton() {
        cropButton = new JButton("Crop");
        cropButton.addActionListener(e -> crop());
        settingsPanel.add(cropButton);
    }

    private void addCancelButton() {
        cancelButton.addActionListener(e -> cancel());
        settingsPanel.add(cancelButton);
    }

    @Override
    public void mouseClicked(PMouseEvent e) {
        if (e.getClickCount() == 2 && !e.isConsumed()) {
            mouseDoubleClicked(e);
        }
    }

    private void mouseDoubleClicked(PMouseEvent e) {
        if (state != TRANSFORM) {
            return;
        }

        if (!cropBox.getCropRect().containsCo(e.getPoint())) {
            // only double-clicking inside the crop box will execute the crop
            return;
        }

        if (crop()) {
            e.consume(); // consume the event if the crop was successful
        }
    }

    @Override
    protected void dragStarted(PMouseEvent e) {
        assert state != INITIAL_DRAG;
        assert state.checkInvariants(this);

        if (state == IDLE) {
            rectBefore = null;

            // start defining a new crop area
            setState(INITIAL_DRAG);
            setCropEnabled(true);
        } else if (state == TRANSFORM) {
            // interact with the existing crop box (move or resize)
            rectBefore = cropBox.getImCropRect();
            allowGrowingBefore = allowGrowingCB.isSelected();

            cropBox.mousePressed(e);
        }
    }

    @Override
    protected void ongoingDrag(PMouseEvent e) {
        assert state.checkInvariants(this);

        if (state == TRANSFORM) {
            // adjust the existing crop box
            cropBox.mouseDragged(e);
        } else if (state == INITIAL_DRAG) {
            // define the initial crop rectangle
            drag.setExpandFromCenter(e.isAltDown());
            drag.setEnforceEqualDimensions(e.isShiftDown());
        }

        // update the size spinners continuously
        updateSizeSpinners(getEffectiveImCropRect(e.getView()));

        // in the INITIAL_DRAG state this will also
        // cause the painting of the darkening overlay
        e.repaint();
    }

    /**
     * Handles the end of a mouse drag gesture, finalizing the crop box or canceling if empty.
     */
    @Override
    protected void dragFinished(PMouseEvent e) {
        assert state.checkInvariants(this);

        if (drag.isClick()) {
            if (state == INITIAL_DRAG) {
                reset(); // can't create a crop box from a click
            }
            // else ignore clicks - double clicks are handled separately
            return;
        }
        if (drag.isEmptyRect() && state == INITIAL_DRAG) {
            reset(); // can't create a crop box from an empty rectangle
            return;
        }

        View view = e.getView();
        boolean needsRepaint = false;
        boolean boxJustCreated = false;

        switch (state) {
            case IDLE, AFTER_FIRST_MOUSE_PRESS:
                return;
            case INITIAL_DRAG:
                PRectangle rect = PRectangle.positiveFromCo(drag.toCoRect(), view);
                assert !rect.isEmpty();

                cropBox = new CropBox(rect, view);
                boxJustCreated = true;
                setState(TRANSFORM);
                needsRepaint = true; // show the crop box
                break;
            case TRANSFORM:
                cropBox.mouseReleased(e);
                boxJustCreated = false;
                break;
        }

        assert cropBox != null;
        assert state == TRANSFORM;

        BoxAdjustmentResult adjustmentResult = adjustCropBoxToCanvas(view,
            this.rectBefore, this.allowGrowingBefore);
        needsRepaint = needsRepaint || (adjustmentResult != BoxAdjustmentResult.NO_CHANGE);
        if (needsRepaint) {
            view.repaint();
        }

        if (adjustmentResult != BoxAdjustmentResult.RESET) {
            String editName = boxJustCreated ? "Create Crop Box" : "Modify Crop Box";
            History.add(new CropBoxChangedEdit(editName, view.getComp(),
                rectBefore, cropBox.getImCropRect(),
                allowGrowingBefore, allowGrowingCB.isSelected()));
        }
    }

    @Override
    public void mouseMoved(MouseEvent e, View view) {
        if (state == TRANSFORM) {
            // set handle-specific cursors
            cropBox.mouseMoved(e, view);
        }
    }

    /**
     * Paints the crop overlay, including the dark mask and handles.
     */
    @Override
    public void paintOverCanvas(Graphics2D g2, Composition comp) {
        if (state == IDLE) {
            return;
        }
        PRectangle cropRect = getCropRect(comp.getView());
        if (cropRect == null) {
            return;
        }

        paintDarkMask(g2, comp, cropRect);

        if (state == TRANSFORM) {
            paintBoxAndGuides(g2, cropRect);
        }
    }

    /**
     * Paints the semi-transparent dark area outside the crop rectangle.
     */
    private void paintDarkMask(Graphics2D g2, Composition comp, PRectangle cropRect) {
        Color origColor = g2.getColor();
        Composite origComposite = g2.getComposite();
        View view = comp.getView();

        // all calculations are in component space
        Rectangle coCanvasBounds = comp.getCanvas().getCoBounds(view);
        Rectangle coCropRect = cropRect.getCo();

        g2.setColor(BLACK);
        g2.setComposite(maskComposite);

        // avoids using slow Area objects and Area.subtract, and
        // constructs the dark mask shape (rectangle with a hole) manually
        Rectangle hole = coCanvasBounds.intersection(coCropRect);
        if (hole.isEmpty()) {
            // if the crop rectangle is entirely outside the canvas,
            // the whole canvas should be dark
            g2.fill(coCanvasBounds);
        } else if (hole.equals(coCanvasBounds)) {
            // if the crop rect completely covers the canvas
            // (or is larger), nothing should be darkened (do nothing)
        } else {
            // standard crop: WIND_EVEN_ODD ensures that
            // the inner rectangle is treated as a hole
            Path2D darkAreaShape = new Path2D.Double(Path2D.WIND_EVEN_ODD);
            darkAreaShape.append(coCanvasBounds, false);
            darkAreaShape.append(hole, false);
            g2.fill(darkAreaShape);
        }

        // restore original graphics settings
        g2.setColor(origColor);
        g2.setComposite(origComposite);
    }

    private void paintBoxAndGuides(Graphics2D g2, PRectangle cropRect) {
        compositionGuide.draw(cropRect.getCo(), g2);

        cropBox.paint(g2);
    }

    private void setCropEnabled(boolean enabled) {
        widthLabel.setEnabled(enabled);
        widthSpinner.setEnabled(enabled);

        heightLabel.setEnabled(enabled);
        heightSpinner.setEnabled(enabled);

        cropButton.setEnabled(enabled);
        cancelButton.setEnabled(enabled);
    }

    public boolean isCropEnabled() {
        return cropButton.isEnabled();
    }

    /**
     * Updates the width and height spinners from the given crop rectangle.
     */
    private void updateSizeSpinners(Rectangle2D rect) {
        if (rect == null) {
            return;
        }
        int newWidth = (int) Math.round(rect.getWidth());
        int newHeight = (int) Math.round(rect.getHeight());
        updateSizeSpinners(newWidth, newHeight);
    }

    /**
     * Programmatically updates the width and height spinners.
     */
    private void updateSizeSpinners(int newWidth, int newHeight) {
        userChangedSpinner = false;
        widthSpinner.setValue(newWidth);
        heightSpinner.setValue(newHeight);
        userChangedSpinner = true;
    }

    /**
     * Returns the current crop rectangle based on the tool's state.
     */
    private PRectangle getCropRect(View view) {
        return switch (state) {
            case INITIAL_DRAG -> drag.toPosPRect(view);
            case TRANSFORM -> cropBox.getCropRect();
            case IDLE, AFTER_FIRST_MOUSE_PRESS -> null;
        };
    }

    /**
     * Calculates the effective image-space crop rectangle
     * reflecting the final crop dimensions (for UI preview).
     */
    private Rectangle2D getEffectiveImCropRect(View view) {
        PRectangle cropBoxRect = getCropRect(view);
        if (cropBoxRect == null) {
            return null; // no crop box active or defined
        }

        Rectangle2D imRect = cropBoxRect.getIm();
        if (allowGrowingCB.isSelected()) {
            // the effective rectangle is the rectangle from the crop box
            return imRect;
        } else {
            // intersect with canvas bounds if growing is not allowed
            return view.getCanvas().intersect(imRect);
        }
    }

    /**
     * Adjusts the crop box to fit within canvas limits if growing is not allowed.
     */
    private BoxAdjustmentResult adjustCropBoxToCanvas(View view,
                                                      Rectangle2D origRect,
                                                      boolean origAllowGrowing) {
        if (cropBox == null || state != TRANSFORM) {
            return BoxAdjustmentResult.NO_CHANGE;
        }
        if (allowGrowingCB.isSelected()) {
            // there are no constraints if growing is allowed
            return BoxAdjustmentResult.NO_CHANGE;
        }

        PRectangle currentPRect = cropBox.getCropRect();
        Rectangle2D currentImRect = currentPRect.getIm();
        Rectangle2D intersection = view.getCanvas().intersect(currentImRect);

        if (intersection.isEmpty()) {
            // true if a box was moved out, false if the initial drag was off-canvas
            boolean addHistory = origRect != null;

            // crop box is entirely outside the canvas => cancel the crop
            reset(addHistory, origRect, origAllowGrowing);
            return BoxAdjustmentResult.RESET;
        }
        boolean needsAdjustment = !intersection.equals(currentImRect);
        if (needsAdjustment) {
            cropBox.setImSize(intersection, view);
            updateSizeSpinners(intersection);
            return BoxAdjustmentResult.ADJUSTED;
        } else {
            return BoxAdjustmentResult.NO_CHANGE;
        }
    }

    @Override
    protected void toolDeactivated(View view) {
        super.toolDeactivated(view);
        reset();
    }

    private void reset(boolean addHistory,
                       Rectangle2D rectBeforeDismissal,
                       boolean allowGrowingBeforeDismissal) {
        View activeView = Views.getActive();
        Composition comp = (activeView != null) ? activeView.getComp() : null;

        if (addHistory && comp != null) {
            assert rectBeforeDismissal != null;
            History.add(new CropBoxChangedEdit("Dismiss Crop Box", comp,
                rectBeforeDismissal, null,
                allowGrowingBeforeDismissal, allowGrowingBeforeDismissal
            ));
        }

        cropBox = null;
        setState(IDLE);
        setCropEnabled(false);
        updateSizeSpinners(0, 0);
        Views.repaintActive();
        Views.setCursorForAll(Cursors.DEFAULT);
        this.rectBefore = null;
    }

    @Override
    public void reset() {
        reset(false, null, allowGrowingCB.isSelected());
    }

    private void setState(DragToolState newState) {
        state = newState;
    }

    public boolean hasCropBox() {
        return cropBox != null;
    }

    /**
     * Sets the allowGrowingCB state without triggering listeners, for undo/redo.
     */
    public void setAllowGrowingUndoRedo(boolean allow) {
        allowGrowingCB.setSelected(allow);
    }

    /**
     * Clears the crop box as part of an undo/redo operation.
     */
    public void clearBoxForUndoRedo() {
        this.cropBox = null;
        setState(IDLE);
    }

    public void setBoxForUndoRedo(Rectangle2D imRect, View view) {
        if (cropBox == null) {
            // restores the dismissed crop box
            PRectangle pRect = PRectangle.fromIm(imRect, view);
            this.cropBox = new CropBox(pRect, view);
        } else {
            // changes the existing crop box
            cropBox.setImSize(imRect, view);
        }
        setState(TRANSFORM);
    }

    /**
     * Updates crop tool UI elements based on the current cropBox and tool state.
     */
    public void updateUIFromState(View view) {
        boolean hasBox = (this.cropBox != null);
        setCropEnabled(hasBox);

        if (hasBox) {
            updateSizeSpinners(getEffectiveImCropRect(view));
        } else {
            updateSizeSpinners(0, 0);
        }

        if (state == IDLE) {
            Views.setCursorForAll(Cursors.DEFAULT);
        }
    }

    @Override
    public void compReplaced(Composition newComp, boolean reloaded) {
        if (reloaded) {
            reset();
        }
    }

    @Override
    public void coCoordsChanged(View view) {
        if (cropBox != null && state == TRANSFORM) {
            cropBox.coCoordsChanged(view);
        }
    }

    @Override
    public void imCoordsChanged(AffineTransform at, View view) {
        if (cropBox != null && state == TRANSFORM) {
            cropBox.imCoordsChanged(at, view);
        }
    }

    /**
     * Nudges the crop box when an arrow key is pressed.
     */
    @Override
    public boolean arrowKeyPressed(ArrowKey key) {
        assert state.checkInvariants(this);

        View view = Views.getActive();
        if (view == null || state != TRANSFORM) {
            return false;
        }

        Rectangle2D rectBeforeNudge = cropBox.getImCropRect();
        boolean allowGrowingBeforeNudge = allowGrowingCB.isSelected();

        cropBox.arrowKeyPressed(key, view);

        BoxAdjustmentResult adjustmentResult = BoxAdjustmentResult.NO_CHANGE;
        if (!allowGrowingCB.isSelected()) {
            // the crop area could shrink if the crop box
            // is pushed out and growing is not allowed
            adjustmentResult = adjustCropBoxToCanvas(view,
                rectBeforeNudge, allowGrowingBeforeNudge);
        }
        // if canvas growing is allowed, then the crop area doesn't change

        if (adjustmentResult != BoxAdjustmentResult.RESET) {
            assert state == TRANSFORM;
            History.add(new CropBoxChangedEdit("Nudge Crop Box", view.getComp(),
                rectBeforeNudge, cropBox.getImCropRect(),
                allowGrowingBeforeNudge, allowGrowingCB.isSelected()));
        }

        return true;
    }

    @Override
    public void escPressed() {
        cancel();
    }

    /**
     * Performs the crop operation and returns true if successful.
     */
    private boolean crop() {
        if (state != TRANSFORM) {
            return false;
        }

        View view = Views.getActive();
        assert view != null; // the tool should be reset if there is no view

        Rectangle2D cropRect = getCropRect(view).getIm();
        if (cropRect.isEmpty()) {
            Messages.showInfo("Empty Crop Rectangle",
                "Can't crop to %dx%d image.".formatted(
                    (int) cropRect.getWidth(),
                    (int) cropRect.getHeight()));
            return false;
        }

        // create an edit that handles restoring the crop box state
        boolean allowGrowing = allowGrowingCB.isSelected();
        PixelitorEdit cropBoxRestorationEdit = new CropBoxChangedEdit(
            "Box Restoration", // internal name
            view.getComp(),
            cropRect,
            null,
            allowGrowing,
            allowGrowing // the same (reset doesn't change this)
        );

        Crop.toolCrop(view.getComp(), cropRect,
            allowGrowingCB.isSelected(), deleteCroppedCB.isSelected(),
            cropBoxRestorationEdit);
        reset();
        return true;
    }

    private void cancel() {
        if (state != TRANSFORM) {
            return;
        }
        reset(true,
            cropBox.getImCropRect(), allowGrowingCB.isSelected());
        Messages.showPlainStatusMessage("Crop canceled.");
    }

    @Override
    public void otherKeyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (crop()) {
                e.consume();
            }
            // otherwise the "Enter" key event might be used elsewhere,
            // for example by the layer name editor
        } else if (e.getKeyCode() == KeyEvent.VK_O) {
            if (e.isControlDown()) {
                // ignore Ctrl-O which means "Open File" (see issue #81)
                return;
            }
            if (e.isShiftDown()) {
                // Shift-O: change the orientation
                // of the current composition guide type
                if (state == TRANSFORM) {
                    compositionGuide.setNextOrientation();
                    Views.repaintActive();
                    e.consume();
                }
            } else {
                // O: advance to the next composition guide type
                selectNextCompositionGuide();
                e.consume();
            }
        }
    }

    /**
     * Selects the next available composition guide type in the combo box.
     */
    private void selectNextCompositionGuide() {
        int index = guidesCB.getSelectedIndex();
        int numGuideTypes = guidesCB.getItemCount();
        int nextIndex = (index + 1) % numGuideTypes; // wrap around
        guidesCB.setSelectedIndex(nextIndex);
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        maskOpacity.saveStateTo(preset);
        preset.put(CompositionGuideType.PRESET_KEY, getSelectedGuides().name());

        preset.putBoolean(DELETE_CROPPED_TEXT, deleteCroppedCB.isSelected());
        preset.putBoolean(ALLOW_GROWING_TEXT, allowGrowingCB.isSelected());
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        maskOpacity.loadStateFrom(preset);
        guidesCB.setSelectedItem(preset.getEnum(
            CompositionGuideType.PRESET_KEY, CompositionGuideType.class));
        deleteCroppedCB.setSelected(preset.getBoolean(DELETE_CROPPED_TEXT));
        allowGrowingCB.setSelected(preset.getBoolean(ALLOW_GROWING_TEXT));
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.addDouble("mask opacity", maskOpacity.getPercentage());
        node.addBoolean("delete cropped", deleteCroppedCB.isSelected());
        node.addBoolean("allow growing", allowGrowingCB.isSelected());
        node.addAsString("guide type", getSelectedGuides());
        node.addNullableDebuggable("cropBox", cropBox);

        return node;
    }

    @Override
    public Consumer<Graphics2D> createIconPainter() {
        return ToolIcons::paintCropIcon;
    }
}
