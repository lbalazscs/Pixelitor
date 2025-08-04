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

import pixelitor.Composition;
import pixelitor.ConsistencyChecks;
import pixelitor.gui.View;
import pixelitor.selection.SelectionBuilder;
import pixelitor.selection.SelectionType;
import pixelitor.tools.util.OverlayType;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;

import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.function.Consumer;

/**
 * A tool that creates polygonal selections by clicking points.
 */
public class PolygonalSelectionTool extends AbstractSelectionTool {
    public PolygonalSelectionTool() {
        super("Polygonal Selection", 'L',
            "<b>click</b> to add points, " +
                "<b>double-click</b> or <b>right-click</b> to close the selection.",
            Cursors.DEFAULT, false);
        repositionOnSpace = false;
        pixelSnapping = true;
    }

    @Override
    protected void toolDeactivated(View view) {
        super.toolDeactivated(view);

        // ensure unfinished selections are cancelled
        // and don't remain visible after switching tools
        cancelSelectionBuilder();
    }

    @Override
    protected void dragStarted(PMouseEvent e) {
        // ignored, polygon points are added on mouse release/click
    }

    @Override
    protected void ongoingDrag(PMouseEvent e) {
        // ignored, dragging doesn't modify the polygon being built
    }

    @Override
    protected void dragFinished(PMouseEvent e) {
        Composition comp = e.getComp();
        if (selectionBuilder == null) {
            // first click: start building the polygon
            setupCombinatorWithKeyModifiers(e);
            selectionBuilder = new SelectionBuilder(
                SelectionType.POLYGONAL_LASSO, getCombinator(), comp);
            selectionBuilder.updateDraftSelection(e);
            resetCombinator();
        } else {
            // subsequent click: add another point
            selectionBuilder.updateDraftSelection(e);
            if (e.isRight()) {
                // right-click finishes the polygon
                selectionBuilder.combineShapes();
                cancelSelectionBuilder();
            }
        }

        assert ConsistencyChecks.selectionShapeIsNotEmpty(comp) : "selection is empty";
        assert ConsistencyChecks.selectionIsInsideCanvas(comp) : "selection is outside";
    }

    @Override
    public void mouseClicked(PMouseEvent e) {
        Composition comp = e.getComp();
        // handle double-click to finish the polygon
        if (selectionBuilder != null && e.getClickCount() > 1) {
            // update with the final point (same as the double-clicked point)
            selectionBuilder.updateDraftSelection(e);
            selectionBuilder.combineShapes();
            cancelSelectionBuilder();
        }
        // single clicks are handled in dragFinished
    }

    @Override
    protected OverlayType getOverlayType() {
        // no drag overlay as we build point by point
        return OverlayType.NONE;
    }

    @Override
    public Consumer<Graphics2D> createIconPainter() {
        return g -> {
            // based on poly_tool.svg
            Path2D path = new Path2D.Double(Path2D.WIND_EVEN_ODD);

            path.moveTo(6.1484487, 15.462234);
            path.curveTo(5.6141719, 17.426949, 6.5335977, 20.270432, 11.440085, 18.79228);
            path.curveTo(11.165367, 19.575245, 9.138209, 20.355129, 8.4362545, 20.699641);
            path.curveTo(6.9299497, 21.283743, 5.6160939, 21.932861, 5.36689, 23.697969);
            path.curveTo(5.2350935, 24.631483, 5.7478461, 26.234294, 6.6964167, 25.593747);
            path.curveTo(7.1059144, 24.837846, 6.4940809, 23.524083, 7.8683306, 22.662188);
            path.curveTo(10.178744, 21.213153, 13.189381, 20.634149, 13.562594, 17.53758);
            path.lineTo(21.104014, 15.10914);
            path.lineTo(24.97093, 1.9650698);
            path.lineTo(13.005194, 8.5186106);
            path.lineTo(2.6531691, 3.8095857);
            path.closePath();

            path.moveTo(13.380613, 15.9582);
            path.curveTo(11.925559, 13.25564, 10.007567, 13.111697, 7.381234, 14.201458);
            path.lineTo(4.6956935, 6.0863419);
            path.lineTo(13.012216, 9.9973027);
            path.lineTo(23.185362, 4.218993);
            path.lineTo(19.96356, 13.997892);
            path.closePath();

            path.moveTo(11.46885, 16.915383);
            path.curveTo(10.047759, 19.071274, 6.3925118, 17.76767, 7.7550969, 15.923511);
            path.curveTo(8.9939951, 14.471869, 12.469638, 15.019517, 11.46885, 16.915383);
            path.closePath();

            g.fill(path);
        };
    }
}

