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
import pixelitor.gui.utils.VectorIcon;
import pixelitor.guides.GuidesRenderer;
import pixelitor.tools.DragTool;
import pixelitor.tools.DragToolState;
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
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ResourceBundle;

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

    private JCheckBox deleteCroppedCB;
    private JCheckBox allowGrowingCB;
    private static final String DELETE_CROPPED_TEXT = "Delete Cropped Pixels";
    private static final String ALLOW_GROWING_TEXT = "Allow Growing";

    public CropTool() {
        super("Crop", 'C',
            "<b>drag</b> to start or <b>Alt-drag</b> to start form the center. " +
                "After handles appear: " +
                "<b>Shift-drag</b> to keep aspect ratio, " +
                "<b>Double-click</b> to crop, <b>Esc</b> to cancel.",
            Cursors.DEFAULT, false);

        spaceDragStartPoint = true; // allow moving the initial rectangle with spacebar
        pixelSnapping = true; // always snaps to pixels

        compositionGuide = new CompositionGuide(GuidesRenderer.CROP_GUIDES_INSTANCE.get());
        updateMaskopacity(false);
    }

    /**
     * Initialize settings panel controls
     */
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

        setCropEnabled(false); // initially, no crop box exists
    }

    private void addMaskOpacitySlider() {
        // use a change listener so that the mask is
        // continuously updated while the slider is dragged
        maskOpacity.addChangeListener(e -> updateMaskopacity(true));
        settingsPanel.add(maskOpacity.createGUI());
    }

    private void updateMaskopacity(boolean repaint) {
        float alpha = (float) maskOpacity.getPercentage();
        maskComposite = AlphaComposite.getInstance(SRC_OVER, alpha);
        if (repaint) {
            Views.repaintActive();
        }
    }

    private void addGuidesSelector() {
        guidesCB = GUIUtils.createComboBox(CompositionGuideType.values());
        guidesCB.setToolTipText("<html>Composition guides." +
            "<br><br>Press <b>O</b> to select the next guide." +
            "<br>Press <b>Shift-O</b> to change the orientation.");
        guidesCB.addActionListener(e -> Views.repaintActive());
        settingsPanel.addComboBox("Guides:", guidesCB, "guidesCB");
    }

    private void addCropSizeControls() {
        ChangeListener sizeChangeListener = e -> {
            // update the crop box only when handles are shown and not currently being dragged
            if (state == TRANSFORM && !cropBox.isAdjusting()) {
                View view = Views.getActive();
                assert view != null; // the spinners must be disabled if there is no view

                int newImWidth = (int) widthSpinner.getValue();
                int newImHeight = (int) heightSpinner.getValue();

                cropBox.setImSize(newImWidth, newImHeight, view);

                if (!allowGrowingCB.isSelected()) {
                    // this will modify the spinners, preventing UI
                    // values that would lead to constraint violation
                    adjustCropBoxToCanvasConstraints(view);
                }
            }
        };

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

    /**
     * Adds the "Delete Cropped Pixels" and "Allow Growing" checkboxes.
     */
    private void addCropOptionsCheckBoxes() {
        deleteCroppedCB = settingsPanel.addCheckBox(
            DELETE_CROPPED_TEXT, true, "deleteCroppedCB",
            "<html>If checked, pixels outside the crop area are permanently removed." +
                "<br>If unchecked, only the canvas size is reduced, preserving the pixels outside the canvas.");

        allowGrowingCB = settingsPanel.addCheckBox(
            ALLOW_GROWING_TEXT, false, "allowGrowingCB",
            "Enables enlarging the canvas.");
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
        assert state.isOK(this);

        if (state == IDLE) {
            // start defining a new crop area
            setState(INITIAL_DRAG);
            setCropEnabled(true);
        } else if (state == TRANSFORM) {
            // interact with the existing crop box (move or resize)
            assert isCropEnabled();

            cropBox.mousePressed(e);
        }
    }

    @Override
    protected void ongoingDrag(PMouseEvent e) {
        assert state.isOK(this);

        if (state == TRANSFORM) {
            // adjust the existing crop box
            cropBox.mouseDragged(e);
        } else if (state == INITIAL_DRAG) {
            // define the initial crop rectangle
            drag.setExpandFromCenter(e.isAltDown());
            drag.setEnforceEqualDimensions(e.isShiftDown());
        }

        // update the size spinners continuously
        updateSizeSettings(getEffectiveImCropRect(e.getView()));

        // in the INITIAL_DRAG state this will also
        // cause the painting of the darkening overlay
        e.repaint();
    }

    /**
     * Handles the end of a mouse drag gesture, finalizing the crop box or canceling if empty.
     */
    @Override
    protected void dragFinished(PMouseEvent e) {
        assert state.isOK(this);

        if (state == INITIAL_DRAG && drag.isEmptyRect()) {
            // if the user just clicked without dragging, cancel the operation
            reset();
            return;
        }

        View view = e.getView();
        boolean needsRepaint = false;

        switch (state) {
            case IDLE, AFTER_FIRST_MOUSE_PRESS:
                return;
            case INITIAL_DRAG:
                PRectangle rect = PRectangle.positiveFromCo(drag.toCoRect(), view);
                assert !rect.isEmpty();

                cropBox = new CropBox(rect, view);
                setState(TRANSFORM);
                needsRepaint = true; // show the crop box
                break;
            case TRANSFORM:
                cropBox.mouseReleased(e);
                break;
        }

        assert cropBox != null;
        assert state == TRANSFORM;

        boolean wasAdjustedOrReset = adjustCropBoxToCanvasConstraints(view);
        needsRepaint = needsRepaint || wasAdjustedOrReset;
        if (needsRepaint) {
            view.repaint();
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

        // create an area covering the whole canvas
        Area darkAreaShape = new Area(coCanvasBounds);
        // subtract the crop rectangle area to create the mask shape
        darkAreaShape.subtract(new Area(cropRect.getCo()));

        g2.setColor(BLACK);
        g2.setComposite(maskComposite);
        g2.fill(darkAreaShape);

        // restore original graphics settings
        g2.setColor(origColor);
        g2.setComposite(origComposite);
    }

    /**
     * Paints the crop box handles and the selected composition guides.
     */
    private void paintBoxAndGuides(Graphics2D g2, PRectangle cropRect) {
        compositionGuide.setType((CompositionGuideType) guidesCB.getSelectedItem());
        compositionGuide.draw(cropRect.getCo(), g2);

        cropBox.paint(g2);
    }

    /**
     * Enables or disables the crop-specific UI controls.
     */
    private void setCropEnabled(boolean enabled) {
        widthLabel.setEnabled(enabled);
        heightSpinner.setEnabled(enabled);

        heightLabel.setEnabled(enabled);
        widthSpinner.setEnabled(enabled);

        cropButton.setEnabled(enabled);
        cancelButton.setEnabled(enabled);
    }

    private boolean isCropEnabled() {
        return cropButton.isEnabled();
    }

    /**
     * Updates the width and height spinners from the given effective crop rectangle.
     */
    private void updateSizeSettings(Rectangle2D effectiveImRect) {
        if (effectiveImRect == null) {
            return;
        }
        int width = (int) Math.round(effectiveImRect.getWidth());
        int height = (int) Math.round(effectiveImRect.getHeight());

        widthSpinner.setValue(width);
        heightSpinner.setValue(height);
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
            Canvas canvas = view.getComp().getCanvas();
            return imRect.createIntersection(canvas.getBounds());
        }
    }

    private boolean adjustCropBoxToCanvasConstraints(View view) {
        if (cropBox == null || state != TRANSFORM) {
            return false;
        }
        if (allowGrowingCB.isSelected()) {
            return false; // there are no constraints
        }

        Canvas canvas = view.getComp().getCanvas();
        Rectangle canvasBoundsIm = canvas.getBounds();

        PRectangle currentPRect = cropBox.getCropRect();
        Rectangle2D currentRect = currentPRect.getIm();
        Rectangle2D intersection = currentRect.createIntersection(canvasBoundsIm);

        if (intersection.isEmpty()) {
            // crop box is entirely outside the canvas => cancel the crop
            reset();
            return true; // reset occurred
        }
        boolean needsAdjustment = !intersection.equals(currentRect);
        if (needsAdjustment) {
            cropBox.setImSize(intersection, view);
            updateSizeSettings(intersection);
            return true; // adjustment made
        } else {
            return false;
        }
    }

    @Override
    protected void toolDeactivated(View view) {
        super.toolDeactivated(view);
        reset();
    }

    @Override
    public void reset() {
        cropBox = null;
        setState(IDLE);

        setCropEnabled(false);

        heightSpinner.setValue(0);
        widthSpinner.setValue(0);

        Views.repaintActive();
        Views.setCursorForAll(Cursors.DEFAULT);
    }

    private void setState(DragToolState newState) {
        state = newState;
    }

    public boolean hasCropBox() {
        return cropBox != null;
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
        View view = Views.getActive();
        if (view == null || state != TRANSFORM) {
            return false;
        }
        cropBox.arrowKeyPressed(key, view);
        if (!allowGrowingCB.isSelected()) {
            // the crop area could shrink if the crop box
            // is pushed out and growing is not allowed
            adjustCropBoxToCanvasConstraints(view);
        }
        // if canvas growing is allowed, then the crop area doesn't change

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

        Crop.toolCrop(view.getComp(), cropRect,
            allowGrowingCB.isSelected(), deleteCroppedCB.isSelected());
        reset();
        return true;
    }

    private void cancel() {
        if (state == IDLE) {
            return; // nothing to cancel
        }

        reset();
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
                // within the current composition guide family
                if (state == TRANSFORM) {
                    compositionGuide.setNextOrientation();
                    Views.repaintActive();
                    e.consume();
                }
            } else {
                // O: advance to the next composition guide
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
        int nextIndex = (index + 1) % numGuideTypes; // wrap around using modulo
        guidesCB.setSelectedIndex(nextIndex);
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        maskOpacity.saveStateTo(preset);
        preset.put("Guides", guidesCB.getSelectedItem().toString());

        preset.putBoolean(DELETE_CROPPED_TEXT, deleteCroppedCB.isSelected());
        preset.putBoolean(ALLOW_GROWING_TEXT, allowGrowingCB.isSelected());
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        maskOpacity.loadStateFrom(preset);

        CompositionGuideType guideType = preset.getEnum("Guides", CompositionGuideType.class);
        guidesCB.setSelectedItem(guideType);

        deleteCroppedCB.setSelected(preset.getBoolean(DELETE_CROPPED_TEXT));
        allowGrowingCB.setSelected(preset.getBoolean(ALLOW_GROWING_TEXT));
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.addDouble("mask opacity", maskOpacity.getPercentage());
        node.addBoolean("delete cropped", deleteCroppedCB.isSelected());
        node.addBoolean("allow growing", allowGrowingCB.isSelected());
        node.addAsString("guide type", guidesCB.getSelectedItem());
        node.addAsString("state", state);

        return node;
    }

    @Override
    public VectorIcon createIcon() {
        return new CropToolIcon();
    }

    private static class CropToolIcon extends ToolIcon {
        @Override
        public void paintIcon(Graphics2D g) {
            // the shape is based on crop_tool.svg
            Path2D shape = new Path2D.Double();

            // top-left little square
            shape.moveTo(5, 1);
            shape.lineTo(8, 1);
            shape.lineTo(8, 4);
            shape.lineTo(5, 4);
            shape.closePath();

            // top, bigger shape
            shape.moveTo(1, 5);
            shape.lineTo(23, 5);
            shape.lineTo(23, 27);
            shape.lineTo(20, 27);
            shape.lineTo(20, 8);
            shape.lineTo(1, 8);
            shape.closePath();

            // bottom, smaller shape
            shape.moveTo(5, 9);
            shape.lineTo(8, 9);
            shape.lineTo(8, 20);
            shape.lineTo(19, 20);
            shape.lineTo(19, 23);
            shape.lineTo(5, 23);
            shape.closePath();

            // bottom-right little square
            shape.moveTo(24, 20);
            shape.lineTo(27, 20);
            shape.lineTo(27, 23);
            shape.lineTo(24, 23);
            shape.closePath();

            g.fill(shape);
        }
    }
}
