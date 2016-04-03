/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

package pixelitor.tools.shapestool;

import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import org.jdesktop.swingx.painter.effects.AreaEffect;
import pixelitor.Composition;
import pixelitor.filters.gui.StrokeParam;
import pixelitor.filters.painters.AreaEffects;
import pixelitor.filters.painters.EffectsPanel;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.OKCancelDialog;
import pixelitor.history.History;
import pixelitor.history.NewSelectionEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.history.SelectionChangeEdit;
import pixelitor.layers.ImageLayer;
import pixelitor.selection.Selection;
import pixelitor.tools.ClipStrategy;
import pixelitor.tools.ShapeType;
import pixelitor.tools.ShapesAction;
import pixelitor.tools.StrokeType;
import pixelitor.tools.Tool;
import pixelitor.tools.ToolAffectedArea;
import pixelitor.tools.UserDrag;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.Composition.ImageChangeActions.FULL;
import static pixelitor.Composition.ImageChangeActions.REPAINT;

/**
 * The Shapes Tool
 */
public class ShapesTool extends Tool {
    private final EnumComboBoxModel<ShapesAction> actionModel = new EnumComboBoxModel<>(ShapesAction.class);
    private final EnumComboBoxModel<ShapeType> typeModel = new EnumComboBoxModel<>(ShapeType.class);
    private final EnumComboBoxModel<TwoPointBasedPaint> fillModel = new EnumComboBoxModel<>(TwoPointBasedPaint.class);
    private final EnumComboBoxModel<TwoPointBasedPaint> strokeFillModel = new EnumComboBoxModel<>(TwoPointBasedPaint.class);

    private final StrokeParam strokeParam = new StrokeParam("");

    private JButton strokeSettingsButton;
    private BasicStroke basicStrokeForOpenShapes;
    private final JComboBox<TwoPointBasedPaint> strokeFillCombo;
    private final JComboBox<TwoPointBasedPaint> fillCombo = new JComboBox<>(fillModel);
    private JButton effectsButton;
    private OKCancelDialog effectsDialog;
    private EffectsPanel effectsPanel;

    private Shape backupSelectionShape = null;
    private boolean drawing = false;

    private Stroke stroke;

    public ShapesTool() {
        super('u', "Shapes", "shapes_tool_icon.png",
                "Click and drag to draw a shape. Hold SPACE down while drawing to move the shape. ",
                Cursor.getDefaultCursor(), true, true, false, ClipStrategy.IMAGE_ONLY);

        strokeFillModel.setSelectedItem(TwoPointBasedPaint.BACKGROUND);
        strokeFillCombo = new JComboBox<>(strokeFillModel);

        spaceDragBehavior = true;
    }

    @Override
    public void initSettingsPanel() {
        JComboBox<ShapeType> shapeTypeCB = new JComboBox<>(typeModel);
        settingsPanel.addWithLabel("Shape:", shapeTypeCB, "shapeTypeCB");
        // make sure all values are visible without a scrollbar
        shapeTypeCB.setMaximumRowCount(ShapeType.values().length);

        JComboBox<ShapesAction> actionCB = new JComboBox<>(actionModel);
        settingsPanel.addWithLabel("Action:", actionCB, "actionCB");
        actionCB.addActionListener(e -> updateWhichSettingsAreEnabled());

        settingsPanel.addWithLabel("Fill:", fillCombo);

        settingsPanel.addWithLabel("Stroke:", strokeFillCombo);

        strokeSettingsButton = settingsPanel.addButton("Stroke Settings...",
                e -> initAndShowStrokeSettingsDialog());

        effectsButton = settingsPanel.addButton("Effects...",
                e -> showEffectsDialog());

        updateWhichSettingsAreEnabled();
    }

    private void showEffectsDialog() {
        if (effectsPanel == null) {
            effectsPanel = new EffectsPanel(null, null);
        }

        effectsDialog = new OKCancelDialog(effectsPanel, "Effects") {
            @Override
            protected void dialogAccepted() {
                effectsDialog.close();
                effectsPanel.updateEffectsFromGUI();
            }

            @Override
            protected void dialogCanceled() {
                super.dialogCanceled();
                effectsDialog.close();
            }
        };
        effectsDialog.setVisible(true);
    }

    @Override
    public void mousePressed(MouseEvent e, ImageComponent ic) {
        Composition comp = ic.getComp();
        backupSelectionShape = comp.getSelectionShape();
    }

    @Override
    public void mouseDragged(MouseEvent e, ImageComponent ic) {
        // hack to prevent AssertionError when dragging started
        // from negative coordinates bug
        // TODO investigate
        ShapesAction action = actionModel.getSelectedItem();
        if(action.drawEffects() && effectsPanel != null) {
            AreaEffects effects = effectsPanel.getEffects();
            if(effects.hasAny()) {
                if (userDrag.getStartX() < 0) {
                    return;
                }
                if (userDrag.getStartY() < 0) {
                    return;
                }
            }
        }
        // end hack

        drawing = true;
        userDrag.setStartFromCenter(e.isAltDown());

        Composition comp = ic.getComp();

        // this will trigger paintOverLayer, therefore the continuous drawing of the shape
        comp.imageChanged(REPAINT); // TODO optimize, the whole image should not be repainted
    }

    @Override
    public void mouseReleased(MouseEvent e, ImageComponent ic) {
        userDrag.setStartFromCenter(e.isAltDown());

        Composition comp = ic.getComp();
        ImageLayer layer = comp.getActiveMaskOrImageLayer();

        ShapesAction action = actionModel.getSelectedItem();
        boolean selectionMode = action.createSelection();
        if (!selectionMode) {
//            saveImageForUndo(comp);

            int thickness = 0;
            int extraStrokeThickness = 0;
            if (action.enableStrokePaintSelection()) {
                thickness = strokeParam.getStrokeWidth();

                StrokeType strokeType = strokeParam.getStrokeType();
                extraStrokeThickness = strokeType.getExtraWidth(thickness);
                thickness += extraStrokeThickness;
            }

            int effectThickness = 0;
            if (effectsPanel != null) {
                effectThickness = effectsPanel.getMaxEffectThickness();

                // the extra stroke thickness must be added to this because the effect can be on the stroke
                effectThickness += extraStrokeThickness;
            }

            if (effectThickness > thickness) {
                thickness = effectThickness;
            }

            ShapeType shapeType = typeModel.getSelectedItem();
            Shape currentShape = shapeType.getShape(userDrag);
            Rectangle shapeBounds = currentShape.getBounds();
            shapeBounds.grow(thickness, thickness);

            if (!shapeBounds.isEmpty()) {
                ToolAffectedArea affectedArea = new ToolAffectedArea(layer, shapeBounds, false);
                saveSubImageForUndo(layer.getImage(), affectedArea);
            }
            paintShapeOnIC(layer, userDrag);

            comp.imageChanged(FULL);
            layer.updateIconImage();
        } else { // selection mode
            comp.onSelection(selection -> {
                selection.clipToCompSize(comp); // the selection can be too big

                PixelitorEdit edit;
                if (backupSelectionShape != null) {
                    edit = new SelectionChangeEdit(comp, backupSelectionShape, "Selection Change");
                } else {
                    edit = new NewSelectionEdit(comp, selection.getShape());
                }
                History.addEdit(edit);
            });
        }

        drawing = false;
        stroke = null;
    }

    private void updateWhichSettingsAreEnabled() {
        ShapesAction action = actionModel.getSelectedItem();
        enableEffectSettings(action.drawEffects());
        enableStrokeSettings(action.enableStrokeSettings());
        enableFillPaintSelection(action.enableFillPaintSelection());
        enableStrokePaintSelection(action.enableStrokePaintSelection());
    }

    private void initAndShowStrokeSettingsDialog() {
        if (toolDialog == null) {
            toolDialog = strokeParam.createSettingsDialogForShapesTool();
        }

        GUIUtils.centerOnScreen(toolDialog);
        toolDialog.setVisible(true);
    }

    private void closeEffectsDialog() {
        if (effectsDialog != null) {
            effectsDialog.setVisible(false);
            effectsDialog.dispose();
        }
    }

    @Override
    protected void toolEnded() {
        super.toolEnded();
        closeEffectsDialog();
    }

    @Override
    public void paintOverLayer(Graphics2D g, Composition comp) {
        if (drawing) {
            // updates continuously the shape while drawing
            paintShape(g, userDrag, comp);
        }
    }

    /**
     * Paint a shape on the given image layer. Can be used programmatically.
     * The start and end point points are given relative to the Composition (not Layer)
     */
    public void paintShapeOnIC(ImageLayer layer, UserDrag userDrag) {
        int tx = -layer.getTX();
        int ty = -layer.getTY();

        BufferedImage bi = layer.getImage();
        Graphics2D g2 = bi.createGraphics();
        g2.translate(tx, ty);

        Composition comp = layer.getComp();

        comp.applySelectionClipping(g2, null);

        paintShape(g2, userDrag, comp);
        g2.dispose();
    }

    /**
     * Paints the selected shape on the given Graphics2D within the bounds of the given UserDrag
     * Called by paintOnImage while dragging, and by paintShapeOnIC on mouse release
     */
    private void paintShape(Graphics2D g, UserDrag userDrag, Composition comp) {
        if (userDrag.isClick()) {
            return;
        }

        if (basicStrokeForOpenShapes == null) {
            basicStrokeForOpenShapes = new BasicStroke(1);
        }

        ShapeType shapeType = typeModel.getSelectedItem();
        Shape currentShape = shapeType.getShape(userDrag);

        ShapesAction action = actionModel.getSelectedItem();

        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        if (action.hasFill()) {
            TwoPointBasedPaint fillType = fillModel.getSelectedItem();
            if (shapeType.isClosed()) {
                //g.setPaint(fillType.getPaint(userDrag));
                fillType.setupPaint(g, userDrag);
                g.fill(currentShape);
                fillType.restorePaint(g);
            } else if (!action.hasStroke()) {
                // special case: a shape that is not closed can be only stroked, even if stroke is disabled
                // stroke it with the basic stroke
                g.setStroke(basicStrokeForOpenShapes);
//                g.setPaint(fillType.getPaint(userDrag));
                fillType.setupPaint(g, userDrag);
                g.draw(currentShape);
                fillType.restorePaint(g);
            }
        }

        if (action.hasStroke()) {
            TwoPointBasedPaint strokeFill = strokeFillModel.getSelectedItem();
            if (stroke == null) {
                // During a single mouse drag, only one stroke should be created
                // This is particularly important for "random shape"
                stroke = strokeParam.createStroke();
            }
            g.setStroke(stroke);
//            g.setPaint(strokeFill.getPaint(userDrag));
            strokeFill.setupPaint(g, userDrag);
            g.draw(currentShape);
            strokeFill.restorePaint(g);
        }

        if (action.drawEffects()) {
            if (effectsPanel != null) {
                AreaEffect[] areaEffects = effectsPanel.getEffects().asArray();
                for (AreaEffect effect : areaEffects) {
                    if (action.hasFill()) {
                        effect.apply(g, currentShape, 0, 0);
                    } else if (action.hasStroke()) { // special case if there is only stroke
                        if (stroke == null) {
                            stroke = strokeParam.createStroke();
                        }
                        effect.apply(g, stroke.createStrokedShape(currentShape), 0, 0);
                    } else { // "effects only"
                        effect.apply(g, currentShape, 0, 0);
                    }
                }
            }
        }

        if (action.createSelection()) {
            Shape selectionShape;
            if (action.enableStrokeSettings()) {
                if (stroke == null) {
                    stroke = strokeParam.createStroke();
                }
                selectionShape = stroke.createStrokedShape(currentShape);
            } else if (!shapeType.isClosed()) {
                if (basicStrokeForOpenShapes == null) {
                    throw new IllegalStateException("action = " + action + ", shapeType = " + shapeType);
                }
                selectionShape = basicStrokeForOpenShapes.createStrokedShape(currentShape);
            } else {
                selectionShape = currentShape;
            }

            Selection selection = comp.getSelection();

            if (selection != null) {
                selection.setShape(selectionShape);
            } else {
                comp.createSelectionFromShape(selectionShape);
            }
        }
    }

    public boolean isDrawing() {
        return drawing;
    }

    private void enableStrokeSettings(boolean b) {
        strokeSettingsButton.setEnabled(b);

        if (!b) {
            closeToolDialog();
        }
    }

    private void enableEffectSettings(boolean b) {
        effectsButton.setEnabled(b);

        if (!b) {
            closeEffectsDialog();
        }
    }

    private void enableStrokePaintSelection(boolean b) {
        strokeFillCombo.setEnabled(b);
    }

    private void enableFillPaintSelection(boolean b) {
        fillCombo.setEnabled(b);
    }

    /**
     * Used for testing
     */
    public void setShapeType(ShapeType newType) {
        typeModel.setSelectedItem(newType);
    }

    /**
     * Can be used for debugging
     */
    public void setAction(ShapesAction action) {
        actionModel.setSelectedItem(action);
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = super.getDebugNode();

        node.addStringChild("Type", typeModel.getSelectedItem().toString());
        node.addStringChild("Action", actionModel.getSelectedItem().toString());
        node.addStringChild("Fill", fillModel.getSelectedItem().toString());
        node.addStringChild("Stroke", strokeFillModel.getSelectedItem().toString());
        strokeParam.addDebugNodeInfo(node);

        return node;
    }
}

