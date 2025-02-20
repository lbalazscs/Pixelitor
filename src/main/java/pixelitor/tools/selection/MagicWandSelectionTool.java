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

package pixelitor.tools.selection;

import pixelitor.AppMode;
import pixelitor.Composition;
import pixelitor.ConsistencyChecks;
import pixelitor.Views;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.gui.utils.VectorIcon;
import pixelitor.selection.SelectionBuilder;
import pixelitor.selection.SelectionType;
import pixelitor.tools.util.OverlayType;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.ResourceBundle;

import static pixelitor.gui.utils.SliderSpinner.LabelPosition.WEST;

/**
 * A selection tool that creates selections based on color similarity.
 */
public class MagicWandSelectionTool extends AbstractSelectionTool {
    private static final RangeParam magicWandToleranceParam = new RangeParam("Tolerance", 0, 20, 255);
    private static final SliderSpinner magicWandToleranceSlider = new SliderSpinner(magicWandToleranceParam, WEST, false);

    public MagicWandSelectionTool() {
        super("Magic Wand Selection", 'W', "MagicWand selection: " +
            "<b>click</b> on the area you want to select. " +
            "<b>right-click</b> to cancel the selection." +
            "<b>Shift</b> adds to an existing selection, " +
            "<b>Alt</b> removes from it, <b>Shift-Alt</b> intersects.", Cursors.DEFAULT, false);
        spaceDragStartPoint = true;
        pixelSnapping = true;
    }

    @Override
    public void initSettingsPanel(ResourceBundle resources) {
        super.initSettingsPanel(resources);

        magicWandToleranceSlider.setEnabled(true);
        settingsPanel.add(magicWandToleranceSlider);
    }

    @Override
    protected void dragStarted(PMouseEvent e) {
        //ignore
    }

    @Override
    protected void ongoingDrag(PMouseEvent e) {
        //ignore
    }


    @Override
    protected void dragFinished(PMouseEvent e) {
        //ignore
    }

    @Override
    public void mouseClicked(PMouseEvent e) {
        Composition comp = e.getComp();
        setupCombinatorWithKeyModifiers(e);
        selectionBuilder = new SelectionBuilder(
            SelectionType.SELECTION_MAGIC_WAND, getCombinator(), e.getComp());

        if (e.isRight()) {
            cancelSelection(comp);
        } else if (selectionBuilder != null && e.getClickCount() == 1) {
            SelectionBuilder sb = selectionBuilder;
            SwingWorker<Void, Void> swingWorker = new SwingWorker<>() {
                @Override
                public Void doInBackground() {
                    try {
                        sb.updateDraftSelection(e, comp);
                        sb.combineShapes(comp);
                        stopBuildingSelection();
                    } catch (Exception e) {
                        cancelSelection(comp);
                    }
                    return null;
                }
            };
            swingWorker.execute();
        }
        super.mouseClicked(e);
    }

    private void cancelSelection(Composition comp) {
        if (comp.hasSelection() || comp.hasDraftSelection()) {
            comp.deselect(true);
        }
        assert !comp.hasDraftSelection() : "draft selection is = " + comp.getDraftSelection();
        assert !comp.hasSelection() : "selection is = " + comp.getSelection();

        altMeansSubtract = false;

        if (AppMode.isDevelopment()) {
            ConsistencyChecks.selectionActionsEnabledCheck(comp);
        }
    }

    @Override
    public void escPressed() {
        // pressing Esc should work the same as clicking outside the selection
        Views.onActiveComp(this::cancelSelection);
    }

    @Override
    protected OverlayType getDragDisplayType() {
        return OverlayType.NONE;
    }

    @Override
    protected void toolDeactivated() {
        super.toolDeactivated();

        // otherwise in polygonal mode unfinished selections
        // remain visible after switching to another tool
        stopBuildingSelection();
    }

    public static int getTolerance() {
        return magicWandToleranceParam.getValue();
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        super.saveStateTo(preset);
        // TODO
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        super.loadUserPreset(preset);
        // TODO
    }

    @Override
    public VectorIcon createIcon() {
        return new MagicWandToolIcon();
    }

    private static class MagicWandToolIcon extends ToolIcon {
        @Override
        public void paintIcon(Graphics2D g) {
            // based on magic_wand_tool.svg
            Path2D path = new Path2D.Double(Path2D.WIND_EVEN_ODD);

            path.moveTo(21.034894, 11.558253);
            path.lineTo(26.578, 11.905282);
            path.moveTo(11.604685, 8.2918986);
            path.lineTo(6.9357983, 5.2839172);
            path.moveTo(15.408841, 6.0200348);
            path.lineTo(14.797582, 0.49981555);
            path.moveTo(19.095951, 15.196649);
            path.lineTo(22.612622, 19.495426);
            path.moveTo(19.568835, 7.5360495);
            path.lineTo(23.463737, 3.5767324);
            path.moveTo(16.236448, 14.396477);
            path.lineTo(12.75574, 11.520686);
            path.moveTo(16.978189, 9.131115);
            path.lineTo(17.756734, 9.836974);
            path.curveTo(18.497482, 10.508564, 18.553158, 11.645571, 17.881568, 12.386318);
            path.lineTo(6.6333647, 24.792818);
            path.curveTo(5.9617748, 25.533566, 4.8247674, 25.589242, 4.1531775, 24.917652);
            path.lineTo(3.3054743, 24.211793);
            path.curveTo(2.5647274, 23.540204, 2.5090517, 22.403196, 3.1806416, 21.662449);
            path.lineTo(14.428845, 9.2559484);
            path.curveTo(15.100435, 8.5152013, 16.237442, 8.4595257, 16.978189, 9.131115);
            path.closePath();

            g.setStroke(new BasicStroke(1.5f));

            g.draw(path);
        }
    }
}
