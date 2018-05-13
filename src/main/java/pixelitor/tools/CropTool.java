/*
 * Copyright 2018 Laszlo Balazs-Csiki
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
import pixelitor.utils.ImageSwitchListener;
import pixelitor.utils.Messages;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
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
public class CropTool extends Tool implements ImageSwitchListener {
    private CropToolState state = INITIAL;

    private TransformSupport transformSupport;

    private final RangeParam maskOpacity = new RangeParam("Mask Opacity (%)", 0, 75, 100);

    private Composite hideComposite = AlphaComposite.getInstance(SRC_OVER, maskOpacity.getValueAsPercentage());

    private final JButton cancelButton = new JButton("Cancel");
    private JButton cropButton;

    private JSpinner wSizeSpinner;
    private JSpinner hSizeSpinner;
    private JComboBox guidelinesSelector;

    // The crop rectangle in image space.
    // This variable is used only while the image component is resized
    private Rectangle2D lastCropRect;

    private JCheckBox allowGrowingCB;

    private RectGuideline rectGuideline = new RectGuideline();

    CropTool() {
        super('c', "Crop", "crop_tool_icon.png",
                "Click and drag to define the crop area. Hold SPACE down to move the entire region.",
                Cursor.getDefaultCursor(), false, true, true, ClipStrategy.IMAGE_ONLY);
        spaceDragBehavior = true;
        maskOpacity.addChangeListener(e -> {
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
        });
        ImageComponents.addImageSwitchListener(this);
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
        guidelinesSelector.setMaximumRowCount(guidelinesSelector.getItemCount());
        guidelinesSelector.setSelectedItem(RectGuidelineType.RULE_OF_THIRDS);
        guidelinesSelector.addActionListener(e -> ImageComponents.repaintActive());

        settingsPanel.add(new JLabel("Guidelines:"));
        settingsPanel.add(guidelinesSelector);

        settingsPanel.addSeparator();

        // add crop width spinner
        wSizeSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Canvas.MAX_WIDTH, 1));
        wSizeSpinner.setEnabled(false);
        wSizeSpinner.addChangeListener(whChangeListener);
        wSizeSpinner.setToolTipText("Width of the cropped image (px)");
        settingsPanel.add(new JLabel("Width:"));
        settingsPanel.add(wSizeSpinner);

        // add crop height spinner
        hSizeSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Canvas.MAX_HEIGHT, 1));
        hSizeSpinner.setEnabled(false);
        hSizeSpinner.addChangeListener(whChangeListener);
        hSizeSpinner.setToolTipText("Height of the cropped image (px)");
        settingsPanel.add(new JLabel("Height:"));
        settingsPanel.add(hSizeSpinner);

        settingsPanel.addSeparator();

        // add growing check box
        allowGrowingCB = new JCheckBox("Allow Growing", false);
        allowGrowingCB.setToolTipText("Enables the enlargement of the canvas");
        settingsPanel.add(allowGrowingCB);

        settingsPanel.addSeparator();

        // add crop button
        cropButton = new JButton("Crop");
        cropButton.addActionListener(e -> executeCropCommand());
        cropButton.setEnabled(false);
        settingsPanel.add(cropButton);

        // add cancel button
        cancelButton.addActionListener(e -> executeCloseCommand());
        cancelButton.setEnabled(false);
        settingsPanel.add(cancelButton);
    }

    @Override
    public boolean dispatchMouseClicked(MouseEvent e, ImageComponent ic) {
        if (e.getClickCount() == 2 && !e.isConsumed()) {
            return mouseDoubleClicked(e);
        }

        return false;
    }

    private boolean mouseDoubleClicked(MouseEvent e) {
        // if user double clicked inside selection then accept cropping

        if (state != TRANSFORM) {
            return false;
        }

        if (!transformSupport.getComponentSpaceRect().contains(e.getPoint())) {
            return false;
        }

        e.consume();
        executeCropCommand();
        return true;
    }

    @Override
    public void mousePressed(MouseEvent e, ImageComponent ic) {
        // in case of crop/image change the ended is set to true even if the tool is not ended
        // if a new drag is started, then reset it
        ended = false;

        state = state.getNextAfterMousePressed();

        if (state == TRANSFORM) {
            assert transformSupport != null;
            transformSupport.mousePressed(e, ic);
            cropButton.setEnabled(true);
            cancelButton.setEnabled(true);
            hSizeSpinner.setEnabled(true);
            wSizeSpinner.setEnabled(true);
        } else if (state == USER_DRAG) {
            cropButton.setEnabled(true);
            cancelButton.setEnabled(true);
            hSizeSpinner.setEnabled(true);
            wSizeSpinner.setEnabled(true);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e, ImageComponent ic) {
        if (state == TRANSFORM) {
            transformSupport.mouseDragged(e, ic);
        }
        ic.repaint();
    }

    // TODO: this is done directly with the dispatch mechanism
    @Override
    public void dispatchMouseMoved(MouseEvent e, ImageComponent ic) {
        super.dispatchMouseMoved(e, ic);
        if (state == TRANSFORM) {
            transformSupport.mouseMoved(e, ic);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e, ImageComponent ic) {
        Composition comp = ic.getComp();
        comp.imageChanged(FULL);

        switch (state) {
            case INITIAL:
                break;
            case USER_DRAG:
                if (transformSupport != null) {
                    throw new IllegalStateException();
                }
                Rectangle2D imageSpaceRect = userDrag.createPositiveRect();
                Rectangle compSpaceRect = ic.fromImageToComponentSpace(imageSpaceRect);

                transformSupport = new TransformSupport(compSpaceRect, imageSpaceRect);

                state = TRANSFORM;
                break;
            case TRANSFORM:
                if (transformSupport == null) {
                    throw new IllegalStateException();
                }
                transformSupport.mouseReleased(e, ic);
                break;
        }
    }

    @Override
    public void paintOverImage(Graphics2D g2, Canvas canvas, ImageComponent callingIC, AffineTransform unscaledTransform) {
        if (ended) {
            return;
        }
        if (callingIC != ImageComponents.getActiveIC()) {
            return;
        }
        Rectangle2D cropRect = getCropRect();
        if (cropRect == null) {
            return;
        }

        // here we have the cropping rectangle in image space, therefore
        // this is a good opportunity to update the status bar message
        // even if it has nothing to do with painting
        updateSettingsPanel(cropRect);

        // paint the semi-transparent dark area outside the crop rectangle
        Shape previousClip = g2.getClip();  // save for later use

        Rectangle canvasBounds = canvas.getBounds();

        // Similar to ClipStrategy.INTERNAL_FRAME, but we need intermediary some variables

        Rectangle componentSpaceViewRect = callingIC.getViewRect();
        // ...but first get this to image space...
        Rectangle2D imageSpaceViewRect = callingIC.fromComponentToImageSpace(componentSpaceViewRect);
        // ... and now we can intersect
        Rectangle2D canvasImgIntersection = canvasBounds.createIntersection(imageSpaceViewRect);
        Path2D darkAreaClip = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        darkAreaClip.append(canvasImgIntersection, false);
        darkAreaClip.append(cropRect, false);
        g2.setClip(darkAreaClip);

        Color previousColor = g2.getColor();
        g2.setColor(BLACK);

        Composite previousComposite = g2.getComposite();
        g2.setComposite(hideComposite);

        g2.fill(canvasImgIntersection);

        g2.setColor(previousColor);
        g2.setComposite(previousComposite);

        if (state == TRANSFORM) {
            // Paint the handles.
            // The zooming is temporarily reset because the transformSupport works in component space
            AffineTransform scaledTransform = g2.getTransform();
            g2.setTransform(unscaledTransform);
            // prevents drawing outside the InternalImageFrame/ImageComponent
            // it is important to call this AFTER setting the unscaled transform
            g2.setClip(componentSpaceViewRect);

            // draw guidelines
            RectGuidelineType guidelineType = (RectGuidelineType) guidelinesSelector.getSelectedItem();
            rectGuideline.draw(callingIC.fromImageToComponentSpace(cropRect), guidelineType, g2);

            transformSupport.paintHandles(g2);

            g2.setTransform(scaledTransform);
        }

        g2.setClip(previousClip);
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
                lastCropRect = userDrag.createPositiveRect();
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
    public void noOpenImageAnymore() {
        resetStateToInitial();
    }

    @Override
    public void newImageOpened(Composition comp) {
        resetStateToInitial();
    }

    @Override
    public void activeImageHasChanged(ImageComponent oldIC, ImageComponent newIC) {
        oldIC.repaint();
        resetStateToInitial();
    }

    private void resetStateToInitial() {
        ended = true;
        transformSupport = null;
        state = INITIAL;
        cancelButton.setEnabled(false);
        cropButton.setEnabled(false);
        hSizeSpinner.setEnabled(false);
        hSizeSpinner.setValue(0);
        wSizeSpinner.setEnabled(false);
        wSizeSpinner.setValue(0);

        ImageComponents.repaintActive();
        ImageComponents.setCursorForAll(Cursor.getDefaultCursor());
    }

    public void icResized(ImageComponent ic) {
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
        Messages.showStatusMessage("Crop canceled.");
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

        node.addFloatChild("Mask Opacity", maskOpacity.getValueAsPercentage());
        node.addBooleanChild("Allow Growing", allowGrowingCB.isSelected());
        node.addStringChild("State", state.toString());

        return node;
    }
}
