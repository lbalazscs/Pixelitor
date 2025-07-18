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

import pixelitor.gui.utils.VectorIcon;
import pixelitor.selection.SelectionType;
import pixelitor.tools.util.OverlayType;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;

import java.awt.Graphics2D;
import java.awt.geom.Path2D;

/**
 * A tool that creates freehand selections by dragging.
 */
public class LassoSelectionTool extends AbstractSelectionTool {
    public LassoSelectionTool() {
        super("Freehand Selection", 'L',
            "simply drag around the area that you want to select.",
            Cursors.DEFAULT, false);
        repositionOnSpace = false;
        pixelSnapping = true;
    }

    @Override
    protected void dragStarted(PMouseEvent e) {
        initCombinatorAndBuilder(e, SelectionType.LASSO);

        // add the starting point immediately?
        // selectionBuilder.updateDraftSelection(drag, e.getComp(), e);
    }

    @Override
    protected void ongoingDrag(PMouseEvent e) {
        if (selectionBuilder == null) {
            // can happen if the image changed mid-drag; restart the drag
            dragStarted(e);
        }

        altDown = e.isAltDown();
        // if Alt is released mid-drag, it no longer means subtract for this drag
        if (!altDown) {
            altMeansSubtract = false;
        }

        // add the current point to the lasso path
        selectionBuilder.updateDraftSelection(drag);
    }

    @Override
    protected void dragFinished(PMouseEvent e) {
        // common logic in the base class
        finalizeDragBasedSelection(e);
    }

    @Override
    protected OverlayType getOverlayType() {
        // no measurement overlay for freehand drawing
        return OverlayType.NONE;
    }

    @Override
    public boolean hasSharedHotkey() {
        return true;
    }

    @Override
    public VectorIcon createIcon() {
        return new LassoToolIcon();
    }

    private static class LassoToolIcon extends ToolIcon {
        @Override
        public void paintIcon(Graphics2D g) {
            // based on lasso_tool.svg
            Path2D path = new Path2D.Double(Path2D.WIND_EVEN_ODD);

            path.moveTo(14.975346, 2.4473651);
            path.curveTo(10.833825, 2.4917351, 6.6417778, 3.5243885, 4.13823, 7.7701889);
            path.curveTo(2.5103706, 10.530897, 3.5668295, 13.121411, 6.1484487, 15.462234);
            path.curveTo(5.441171, 17.407727, 6.8027102, 20.385766, 11.455085, 18.79228);
            path.curveTo(11.180367, 19.575245, 9.1532087, 20.355129, 8.4512542, 20.699642);
            path.curveTo(6.9299497, 21.283743, 5.6160939, 21.932861, 5.36689, 23.697969);
            path.curveTo(5.2350935, 24.631483, 5.7478461, 26.234294, 6.6964167, 25.593747);
            path.curveTo(7.1059144, 24.837846, 6.4940809, 23.524083, 7.8683296, 22.662188);
            path.curveTo(10.178743, 21.213153, 13.150935, 20.749483, 13.524148, 17.652914);
            path.curveTo(17.185698, 17.003803, 21.162153, 16.08333, 23.731157, 13.248202);
            path.curveTo(25.913043, 10.599099, 24.792459, 6.7877244, 22.12508, 4.8042194);
            path.curveTo(20.14463, 3.3315254, 17.409278, 2.4212897, 14.975346, 2.4473651);

            path.moveTo(15.112912, 3.9003906);
            path.curveTo(21.110775, 4.1077395, 24.760267, 8.6954564, 23.006194, 11.527174);
            path.curveTo(20.896045, 14.474877, 16.894225, 15.482169, 13.394205, 15.863054);
            path.curveTo(12.035263, 13.160494, 9.931844, 13.17073, 7.4016223, 14.33738);
            path.curveTo(4.054675, 11.216044, 4.1825418, 8.9120531, 7.6983987, 6.0466615);
            path.curveTo(9.8134805, 4.3228889, 12.883084, 3.8233043, 15.112912, 3.9003906);

            path.moveTo(11.46885, 16.915383);
            path.curveTo(10.047759, 19.071274, 6.3925118, 17.76767, 7.7550969, 15.923511);
            path.curveTo(8.9939951, 14.471869, 12.469638, 15.019517, 11.46885, 16.915383);

            path.closePath();

            g.fill(path);
        }
    }
}
