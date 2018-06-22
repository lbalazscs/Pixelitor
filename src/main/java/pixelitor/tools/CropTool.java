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

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.tools.guidelines.RectGuideline;
import pixelitor.tools.guidelines.RectGuidelineType;
import pixelitor.transform.TransformSupport;
import pixelitor.utils.Cursors;
import pixelitor.utils.Messages;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

import static java.awt.AlphaComposite.SRC_OVER;
import static java.awt.Color.BLACK;
import static pixelitor.Composition.ImageChangeActions.FULL;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.WEST;
import static pixelitor.tools.CropToolState.INITIAL;
import static pixelitor.tools.CropToolState.TRANSFORM;
import static pixelitor.tools.CropToolState.USER_DRAG;

/**
 * The crop tool
 */
public class CropTool extends DragTool {
    private CropToolState state = INITIAL;

    private TransformSupport transformSupport;

    private final RangeParam maskOpacity = new RangeParam("Mask Opacity (%)", 0, 75, 100);

    private Composite hideComposite = AlphaComposite.getInstance(SRC_OVER, maskOpacity.getValueAsPercentage());

    private final JButton cancelButton = new JButton("Cancel");
    private JButton cropButton;

    private final JLabel widthLabel = new JLabel("Width:");
    private final JLabel heightLabel = new JLabel("Height:");
    private JSpinner wSizeSpinner;
    private JSpinner hSizeSpinner;
    private JComboBox guidelinesSelector;

    // The crop rectangle in image space.
    // This variable is used only while the image component is resized
    private Rectangle2D lastCropRect;

    private JCheckBox allowGrowingCB;

    private final RectGuideline rectGuideline = new RectGuideline();

    CropTool() {
        super('c', "Crop", "crop_tool_icon.png",
                "<b>drag</b> to start, hold down <b>SPACE</b> to move the entire region. After the handles appear: <b>Shift-drag</b> the handles to keep the aspect ratio. <b>Double-click</b> to crop, or press <b>Esc</b> to cancel.",
                Cursors.DEFAULT, false, true, false, ClipStrategy.CANVAS);
        spaceDragBehavior = true;

        maskOpacity.addChangeListener(e -> maskOpacityChanged());
    }

    private void maskOpacityChanged() {
        float alpha = maskOpacity.getValueAsPercentage();
        // because of a swing bug, the slider can get out of range
        if (alpha < 0.0f) {
            alpha = 0.0f;
            maskOpacity.setValue(0);
        } else if (alpha > 1.0f) {
            alpha = 1.0f;
            maskOpacity.setValue(100);
        }
        hideComposite = AlphaComposite.getInstance(SRC_OVER, alpha);
        ImageComponents.repaintActive();
    }

    /**
     * Initialize settings panel controls
     */
    @Override
    public void initSettingsPanel() {
        SliderSpinner maskOpacitySpinner = new SliderSpinner(maskOpacity, WEST, false);
        settingsPanel.add(maskOpacitySpinner);

        ChangeListener whChangeListener = e -> {
            if (state == TRANSFORM && !transformSupport.isAdjusting()) {
                transformSupport.setSize(
                    (int) wSizeSpinner.getValue(),
                    (int) hSizeSpinner.getValue(),
                    ImageComponents.getActiveIC()
                );
            }
        };
        settingsPanel.addSeparator();

        // add crop guidelines type selector
        guidelinesSelector = new JComboBox<>(RectGuidelineType.values());
        guidelinesSelector.setToolTipText("Composition guides");
        guidelinesSelector.setMaximumRowCount(guidelinesSelector.getItemCount());
//        guidelinesSelector.setSelectedItem(RectGuidelineType.RULE_OF_THIRDS);
        guidelinesSelector.addActionListener(e -> ImageComponents.repaintActive());
        settingsPanel.addWithLabel("Guides:", guidelinesSelector);

        settingsPanel.addSeparator();

        // add crop width spinner
        wSizeSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Canvas.MAX_WIDTH, 1));
        wSizeSpinner.addChangeListener(whChangeListener);
        wSizeSpinner.setToolTipText("Width of the cropped image (px)");
        settingsPanel.add(widthLabel);
        settingsPanel.add(wSizeSpinner);

        // add crop height spinner
        hSizeSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Canvas.MAX_HEIGHT, 1));
        hSizeSpinner.addChangeListener(whChangeListener);
        hSizeSpinner.setToolTipText("Height of the cropped image (px)");
        settingsPanel.add(heightLabel);
        settingsPanel.add(hSizeSpinner);

        settingsPanel.addSeparator();

        // add allow growing check box
        allowGrowingCB = new JCheckBox("Allow Growing", false);
        allowGrowingCB.setToolTipText("Enables the enlargement of the canvas");
        settingsPanel.add(allowGrowingCB);

        settingsPanel.addSeparator();

        // add crop button
        cropButton = new JButton("Crop");
        cropButton.addActionListener(e -> executeCropCommand());
        settingsPanel.add(cropButton);

        // add cancel button
        cancelButton.addActionListener(e -> executeCloseCommand());
        settingsPanel.add(cancelButton);

        enableCropActions(false);
    }

    @Override
    public void mouseClicked(PMouseEvent e) {
        if (e.getClickCount() == 2 && !e.isConsumed()) {
            mouseDoubleClicked(e);
        }
    }

    private void mouseDoubleClicked(PMouseEvent e) {
        // if user double clicked inside selection then accept cropping

        if (state != TRANSFORM) {
            return;
        }

        if (!transformSupport.getComponentSpaceRect().contains(e.getPoint())) {
            return;
        }

        e.consume();
        executeCropCommand();
    }

    @Override
    public void dragStarted(PMouseEvent e) {
        // in case of crop/image change the ended is set to true even if the tool is not ended
        // if a new drag is started, then reset it
        ended = false;

        state = state.getNextAfterMousePressed();

        if (state == TRANSFORM) {
            assert transformSupport != null;
            transformSupport.mousePressed(e);
            enableCropActions(true);
        } else if (state == USER_DRAG) {
            enableCropActions(true);
        }
    }

    @Override
    public void ongoingDrag(PMouseEvent e) {
        if (state == TRANSFORM) {
            transformSupport.mouseDragged(e);
        }
        e.repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e, ImageComponent ic) {
        super.mouseMoved(e, ic);
        if (state == TRANSFORM) {
            transformSupport.mouseMoved(e, ic);
        }
    }

    @Override
    public void dragFinished(PMouseEvent e) {
        Composition comp = e.getComp();
        comp.imageChanged(FULL);

        switch (state) {
            case INITIAL:
                break;
            case USER_DRAG:
                if (transformSupport != null) {
                    throw new IllegalStateException();
                }
                Rectangle2D imageSpaceRect = userDrag.toImDrag().createPositiveRect();

                // TODO all the info is in the userDrag, calculate directly,
                // without rounding errors
                Rectangle compSpaceRect = e.getIC().fromImageToComponentSpace(imageSpaceRect);

                transformSupport = new TransformSupport(compSpaceRect, imageSpaceRect);

                state = TRANSFORM;
                break;
            case TRANSFORM:
                if (transformSupport == null) {
                    throw new IllegalStateException();
                }
                transformSupport.mouseReleased(e);
                break;
        }
    }

    @Override
    public void paintOverImage(Graphics2D g2, Canvas canvas, ImageComponent ic,
                               AffineTransform componentTransform,
                               AffineTransform imageTransform) {
        if (ended) {
            return;
        }
        if (ic != ImageComponents.getActiveIC()) {
            return;
        }
        Rectangle2D cropRect = getCropRect();
        if (cropRect == null) {
            return;
        }

        // TODO done for compatibility. The whole code should be re-evaluated
        g2.setTransform(imageTransform);

        // here we have the cropping rectangle in image space, therefore
        // this is a good opportunity to update the width/height info
        // even if it has nothing to do with painting
        updateSettingsPanel(cropRect);

        // paint the semi-transparent dark area outside the crop rectangle
        Shape origClip = g2.getClip();  // save for later use

        Rectangle canvasBounds = canvas.getImBounds();

        // Similar to ClipStrategy.INTERNAL_FRAME, but we need some intermediary variables

        Rectangle componentSpaceVisiblePart = ic.getVisiblePart();
        // ...but first get this to image space...
        Rectangle2D imageSpaceVisiblePart = ic.fromComponentToImageSpace(componentSpaceVisiblePart);
        // ... and now we can intersect
        Rectangle2D canvasImgIntersection = canvasBounds.createIntersection(imageSpaceVisiblePart);
        Path2D darkAreaClip = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        darkAreaClip.append(canvasImgIntersection, false);
        darkAreaClip.append(cropRect, false);
        g2.setClip(darkAreaClip);

        Color origColor = g2.getColor();
        g2.setColor(BLACK);

        Composite origComposite = g2.getComposite();
        g2.setComposite(hideComposite);

        g2.fill(canvasImgIntersection);

        g2.setColor(origColor);
        g2.setComposite(origComposite);

        g2.setTransform(componentTransform);
        if (state == TRANSFORM) {
            // Paint the handles.
            // The zooming is temporarily reset because the transformSupport works in component space

            // prevents drawing outside the InternalImageFrame/ImageComponent
            // it is important to call this AFTER setting the unscaled transform
            g2.setClip(componentSpaceVisiblePart);

            // draw guidelines
            RectGuidelineType guidelineType = (RectGuidelineType) guidelinesSelector.getSelectedItem();
            rectGuideline.draw(ic.fromImageToComponentSpace(cropRect), guidelineType, g2);

            transformSupport.paintHandles(g2);
        }

        g2.setClip(origClip);
    }

    private void enableCropActions(boolean b) {
        widthLabel.setEnabled(b);
        hSizeSpinner.setEnabled(b);

        heightLabel.setEnabled(b);
        wSizeSpinner.setEnabled(b);

        cropButton.setEnabled(b);
        cancelButton.setEnabled(b);
    }

    /**
     * Update settings panel after crop dimension change
     */
    private void updateSettingsPanel(Rectangle2D cropRect)
    {
        int width = (int) cropRect.getWidth();
        int height = (int) cropRect.getHeight();

        wSizeSpinner.setValue(width);
        hSizeSpinner.setValue(height);
    }

    /**
     * Returns the crop rectangle in image space
     */
    public Rectangle2D getCropRect() {
        switch (state) {
            case INITIAL:
                lastCropRect = null;
                break;
            case USER_DRAG:
                lastCropRect = userDrag.toImDrag().createPositiveRect();
                break;
            case TRANSFORM:
                lastCropRect = transformSupport.getImageSpaceRect();
                break;
        }

        return lastCropRect;
    }

    @Override
    protected void toolEnded() {
        super.toolEnded();
        resetStateToInitial();
    }

    @Override
    public void resetStateToInitial() {
        ended = true;
        transformSupport = null;
        state = INITIAL;

        enableCropActions(false);

        hSizeSpinner.setValue(0);
        wSizeSpinner.setValue(0);

        ImageComponents.repaintActive();
        ImageComponents.setCursorForAll(Cursors.DEFAULT);
    }

    public void icSizeChanged(ImageComponent ic) {
        if (transformSupport != null && lastCropRect != null && state == TRANSFORM) {
            transformSupport.setComponentSpaceRect(ic.fromImageToComponentSpace(lastCropRect));
        }
    }

    @Override
    public boolean arrowKeyPressed(ArrowKey key) {
        if (state == TRANSFORM) {
            ImageComponent ic = ImageComponents.getActiveIC();
            if (ic != null) {
                transformSupport.arrowKeyPressed(key, ic);
                return true;
            }
        }
        return false;
    }

    @Override
    public void escPressed() {
        executeCloseCommand();
    }

    private void executeCropCommand() {
        if (state != TRANSFORM) {
            return;
        }

        ImageComponents.toolCropActiveImage(allowGrowingCB.isSelected());
        ImageComponents.repaintActive();
        resetStateToInitial();
    }

    private void executeCloseCommand() {
        if (state != TRANSFORM) {
            return;
        }

        resetStateToInitial();
        Messages.showInStatusBar("Crop canceled.");
    }

    @Override
    public void shiftPressed() {
        if (state != TRANSFORM) {
            return;
        }

        transformSupport.setUseAspectRatio(true);
    }

    @Override
    public void shiftReleased() {
        if (state != TRANSFORM) {
            return;
        }

        transformSupport.setUseAspectRatio(false);
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = super.getDebugNode();

        node.addFloat("Mask Opacity", maskOpacity.getValueAsPercentage());
        node.addBoolean("Allow Growing", allowGrowingCB.isSelected());
        node.addString("State", state.toString());

        return node;
    }
}
