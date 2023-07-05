/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.GUIMode;
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
import pixelitor.tools.Tool;
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

import static java.awt.AlphaComposite.SRC_OVER;
import static java.awt.Color.BLACK;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.WEST;
import static pixelitor.tools.DragToolState.*;

/**
 * The crop tool
 */
public class CropTool extends DragTool {
    private DragToolState state = NO_INTERACTION;

    private CropBox cropBox;

    private final RangeParam maskOpacity = new RangeParam(
        "Mask Opacity (%)", 0, 75, 100, false, WEST);
    private Composite maskComposite = AlphaComposite.getInstance(
        SRC_OVER, (float) maskOpacity.getPercentage());

    private final JButton cancelButton = new JButton(GUIText.CANCEL);
    private JButton cropButton;

    private JComboBox<CompositionGuideType> guidesCB;
    private final CompositionGuide compositionGuide;

    private final JLabel widthLabel = new JLabel("Width:");
    private final JLabel heightLabel = new JLabel("Height:");
    private JSpinner widthSpinner;
    private JSpinner heightSpinner;

    private JCheckBox deleteCroppedPixelsCB;
    private JCheckBox allowGrowingCB;
    private static final String DELETE_CROPPED_TEXT = "Delete Cropped Pixels";
    private static final String ALLOW_GROWING_TEXT = "Allow Growing";

    public CropTool() {
        super("Crop", 'C',
            "<b>drag</b> to start or <b>Alt-drag</b> to start form the center. " +
                "After the handles appear: " +
                "<b>Shift-drag</b> keeps the aspect ratio, " +
                "<b>Double-click</b> crops, <b>Esc</b> cancels.",
            Cursors.DEFAULT, false);
        spaceDragStartPoint = true;

        GuidesRenderer renderer = GuidesRenderer.CROP_GUIDES_INSTANCE.get();
        compositionGuide = new CompositionGuide(renderer);
    }

    /**
     * Initialize settings panel controls
     */
    @Override
    public void initSettingsPanel() {
        addMaskOpacitySelector();

        settingsPanel.addSeparator();
        addGuidesSelector();

        settingsPanel.addSeparator();
        addCropSizeControls();

        settingsPanel.addSeparator();
        addCropButton();
        addCancelButton();

        settingsPanel.addSeparator();
        addCropControlCheckboxes();

        enableCropActions(false);
    }

    private void addMaskOpacitySelector() {
        // use a change listener so that the mask is
        // continuously updated while the slider is dragged
        maskOpacity.addChangeListener(e -> maskOpacityChanged());
        settingsPanel.add(maskOpacity.createGUI());
    }

    private void maskOpacityChanged() {
        float alpha = (float) maskOpacity.getPercentage();
        // can the slider get out of range?
        if (alpha < 0.0f) {
            if (GUIMode.isDevelopment()) {
                throw new IllegalStateException("alpha = " + alpha);
            }
            alpha = 0.0f;
            maskOpacity.setValue(0);
        } else if (alpha > 1.0f) {
            if (GUIMode.isDevelopment()) {
                throw new IllegalStateException("alpha = " + alpha);
            }
            alpha = 1.0f;
            maskOpacity.setValue(100);
        }
        maskComposite = AlphaComposite.getInstance(SRC_OVER, alpha);
        Views.repaintActive();
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
        ChangeListener whChangeListener = e -> {
            if (state == TRANSFORM && !cropBox.isAdjusting()) {
                cropBox.setImSize(
                    (int) widthSpinner.getValue(),
                    (int) heightSpinner.getValue(),
                    Views.getActive()
                );
            }
        };

        // add crop width spinner
        widthSpinner = createSpinner(whChangeListener, Canvas.MAX_WIDTH,
            "Width of the cropped image (px)");
        settingsPanel.add(widthLabel);
        settingsPanel.add(widthSpinner);

        // add crop height spinner
        heightSpinner = createSpinner(whChangeListener, Canvas.MAX_HEIGHT,
            "Height of the cropped image (px)");
        settingsPanel.add(heightLabel);
        settingsPanel.add(heightSpinner);
    }

    private static JSpinner createSpinner(ChangeListener whChangeListener, int max, String toolTip) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(
            0, 0, max, 1));
        spinner.addChangeListener(whChangeListener);
        spinner.setToolTipText(toolTip);
        // In fact setting it to 3 columns seems enough
        // for the range 1-9999, but leave it as 4 for safety
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(4);
        return spinner;
    }

    private void addCropControlCheckboxes() {
        deleteCroppedPixelsCB = settingsPanel.addCheckBox(
            DELETE_CROPPED_TEXT, true, "deleteCroppedPixelsCB",
            "If not checked, only the canvas gets smaller");

        allowGrowingCB = settingsPanel.addCheckBox(
            ALLOW_GROWING_TEXT, false, "allowGrowingCB",
            "Enables the enlargement of the canvas");
    }

    private void addCropButton() {
        cropButton = new JButton("Crop");
        cropButton.addActionListener(e -> executeCropCommand());
        settingsPanel.add(cropButton);
    }

    private void addCancelButton() {
        cancelButton.addActionListener(e -> executeCancelCommand());
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

        if (!cropBox.getRect().containsCo(e.getPoint())) {
            // only double-clicking inside the crop box will execute the crop
            return;
        }

        if (executeCropCommand()) {
            e.consume();
        }
    }

    @Override
    protected void dragStarted(PMouseEvent e) {
        assert state != INITIAL_DRAG;
        if (state == NO_INTERACTION) {
            setState(INITIAL_DRAG);
            enableCropActions(true);
        } else if (state == TRANSFORM) {
            assert cropBox != null;
            cropBox.mousePressed(e);
            enableCropActions(true);
        }
    }

    @Override
    protected void ongoingDrag(PMouseEvent e) {
        if (state == TRANSFORM) {
            cropBox.mouseDragged(e);
        } else if (drag != null) {
            drag.setStartFromCenter(e.isAltDown());
            drag.setEquallySized(e.isShiftDown());
        }

        PRectangle cropRect = getCropRect(e.getView());
        if (cropRect != null) {
            updateSizeSettings(cropRect);
        }

        // in the INITIAL_DRAG state this will also
        // cause the painting of the darkening overlay
        e.repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e, View view) {
        super.mouseMoved(e, view);
        if (state == TRANSFORM) {
            cropBox.mouseMoved(e, view);
        }
    }

    @Override
    protected void dragFinished(PMouseEvent e) {
        e.getComp().update(); // TODO is an update needed?

        switch (state) {
            case NO_INTERACTION, AFTER_FIRST_MOUSE_PRESS:
                break;
            case INITIAL_DRAG:
                if (cropBox != null) {
                    throw new IllegalStateException();
                }

                PRectangle rect = PRectangle.positiveFromCo(
                    drag.toCoRect(), e.getView());

                cropBox = new CropBox(rect, e.getView());

                setState(TRANSFORM);
                break;
            case TRANSFORM:
                if (cropBox == null) {
                    throw new IllegalStateException();
                }
                cropBox.mouseReleased(e);
                break;
        }
    }

    @Override
    public void paintOverImage(Graphics2D g2, Composition comp) {
        if (state == NO_INTERACTION) {
            return;
        }
        PRectangle cropRect = getCropRect(comp.getView());
        if (cropRect == null) {
            return;
        }

        paintDarkMask(g2, comp, cropRect);

        if (state == TRANSFORM) {
            paintBox(g2, cropRect);
        }
    }

    // Paint the semi-transparent dark area outside the crop rectangle.
    // All calculations are in component space.
    private void paintDarkMask(Graphics2D g2, Composition comp, PRectangle cropRect) {
        Color origColor = g2.getColor();
        Composite origComposite = g2.getComposite();

        View view = comp.getView();
        Rectangle coCanvasBounds = comp.getCanvas().getCoBounds(view);

        Area darkAreaShape = new Area(coCanvasBounds);
        darkAreaShape.subtract(new Area(cropRect.getCo2D()));

        g2.setColor(BLACK);
        g2.setComposite(maskComposite);
        g2.fill(darkAreaShape);

        g2.setColor(origColor);
        g2.setComposite(origComposite);
    }

    // Paint the handles and the guides.
    private void paintBox(Graphics2D g2, PRectangle cropRect) {
        compositionGuide.setType((CompositionGuideType) guidesCB.getSelectedItem());
        compositionGuide.draw(cropRect.getCo2D(), g2);

        cropBox.paint(g2);
    }

    private void enableCropActions(boolean b) {
        widthLabel.setEnabled(b);
        heightSpinner.setEnabled(b);

        heightLabel.setEnabled(b);
        widthSpinner.setEnabled(b);

        cropButton.setEnabled(b);
        cancelButton.setEnabled(b);
    }

    /**
     * Update the settings panel after the crop size changes
     */
    private void updateSizeSettings(PRectangle cropRect) {
        Rectangle2D imRect = cropRect.getIm();
        int width = (int) imRect.getWidth();
        int height = (int) imRect.getHeight();

        widthSpinner.setValue(width);
        heightSpinner.setValue(height);
    }

    /**
     * Returns the crop rectangle
     */
    public PRectangle getCropRect(View view) {
        if (state == INITIAL_DRAG) {
            return drag.toPosPRect(view);
        } else if (state == TRANSFORM) {
            return cropBox.getRect();
        }
        // initial state
        return null;
    }

    @Override
    protected void toolEnded() {
        super.toolEnded();
        resetInitialState();
    }

    @Override
    public void resetInitialState() {
        cropBox = null;
        setState(NO_INTERACTION);

        enableCropActions(false);

        heightSpinner.setValue(0);
        widthSpinner.setValue(0);

        Views.repaintActive();
        Views.setCursorForAll(Cursors.DEFAULT);
    }

    private void setState(DragToolState newState) {
        state = newState;
    }

    @Override
    public void compReplaced(Composition newComp, boolean reloaded) {
        if (reloaded) {
            resetInitialState();
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

    @Override
    public boolean arrowKeyPressed(ArrowKey key) {
        if (state == TRANSFORM) {
            View view = Views.getActive();
            if (view != null) {
                cropBox.arrowKeyPressed(key, view);
                return true;
            }
        }
        return false;
    }

    @Override
    public void escPressed() {
        executeCancelCommand();
    }

    // return true if a crop was executed
    private boolean executeCropCommand() {
        if (state != TRANSFORM) {
            return false;
        }

        Rectangle2D cropRect = getCropRect(Views.getActive()).getIm();
        if (cropRect.isEmpty()) {
            return false;
        }

        Crop.toolCropActiveImage(cropRect,
            allowGrowingCB.isSelected(), deleteCroppedPixelsCB.isSelected());
        resetInitialState();
        return true;
    }

    private void executeCancelCommand() {
        if (state != TRANSFORM) {
            return;
        }

        resetInitialState();
        Messages.showPlainInStatusBar("Crop canceled.");
    }

    @Override
    public void otherKeyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (executeCropCommand()) {
                e.consume();
            }
            // otherwise the "Enter" key event might be used elsewhere,
            // for example by the layer name editor
        } else if (e.getKeyCode() == KeyEvent.VK_O) {
            if (e.isControlDown()) {
                // ignore Ctrl-O see issue #81
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

    private void selectNextCompositionGuide() {
        int index = guidesCB.getSelectedIndex();
        int numGuideTypes = guidesCB.getItemCount();
        int nextIndex;
        if (index == numGuideTypes - 1) {
            nextIndex = 0;
        } else {
            nextIndex = index + 1;
        }
        guidesCB.setSelectedIndex(nextIndex);
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        maskOpacity.saveStateTo(preset);
        preset.put("Guides", guidesCB.getSelectedItem().toString());

        preset.putBoolean(DELETE_CROPPED_TEXT, deleteCroppedPixelsCB.isSelected());
        preset.putBoolean(ALLOW_GROWING_TEXT, allowGrowingCB.isSelected());
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        maskOpacity.loadStateFrom(preset);

        CompositionGuideType guideType = preset.getEnum("Guides", CompositionGuideType.class);
        guidesCB.setSelectedItem(guideType);

        deleteCroppedPixelsCB.setSelected(preset.getBoolean(DELETE_CROPPED_TEXT));
        allowGrowingCB.setSelected(preset.getBoolean(ALLOW_GROWING_TEXT));
    }

    @Override
    public DebugNode createDebugNode(String key) {
        var node = super.createDebugNode(key);

        node.addDouble("mask opacity", maskOpacity.getPercentage());
        node.addBoolean("allow growing", allowGrowingCB.isSelected());
        node.addAsString("state", state);

        return node;
    }

    @Override
    public VectorIcon createIcon() {
        return new CropToolIcon();
    }

    private static class CropToolIcon extends Tool.ToolIcon {
        @Override
        public void paintIcon(Graphics2D g) {
            // the shape is based on crop_tool.svg
            Path2D shape = new Path2D.Float();

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
