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

import pixelitor.colors.FgBgColors;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.GUIText;
import pixelitor.gui.utils.VectorIcon;
import pixelitor.layers.Drawable;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.Cursors;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.util.ResourceBundle;

import static pixelitor.colors.FgBgColors.getBGColor;
import static pixelitor.colors.FgBgColors.getFGColor;

/**
 * The brush tool.
 */
public class BrushTool extends BlendingModeBrushTool {
    public static final String NAME = GUIText.BRUSH;
    private Color drawingColor;

    public BrushTool() {
        super(NAME, 'B',
            "<b>click</b> or <b>drag</b> to draw with the active brush, " +
                "<b>Shift-click</b> to draw lines, " +
                "<b>right-click</b> or <b>right-drag</b> to draw with the background color.",
            Cursors.CROSSHAIR, true
        );
    }

    @Override
    public void initSettingsPanel(ResourceBundle resources) {
        addTypeSelector();
        addBrushSettingsButton();

        settingsPanel.addSeparator();
        addSizeSelector();
        addSymmetrySelector();

        settingsPanel.addSeparator();
        addBlendingModePanel();
        addLazyMouseDialogButton();
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        initDrawingColor(e);
        super.mousePressed(e);
    }

    @Override
    protected void initBrushStroke() {
        // reinitialize the color for each stroke
        brushContext.setColor(drawingColor);
    }

    @Override
    protected void prepareProgrammaticBrushStroke(Drawable dr, PPoint start) {
        super.prepareProgrammaticBrushStroke(dr, start);
        brushContext.setColor(getFGColor());
    }

    private void initDrawingColor(PMouseEvent e) {
        drawingColor = e.isRight() ? getBGColor() : getFGColor();
    }

    @Override
    public void trace(Drawable dr, Shape shape) {
        drawingColor = getFGColor();
        super.trace(dr, shape);
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        super.saveStateTo(preset);

        FgBgColors.saveStateTo(preset);
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        super.loadUserPreset(preset);

        FgBgColors.loadStateFrom(preset);
    }

    @Override
    public VectorIcon createIcon() {
        return new BrushToolIcon();
    }

    private static class BrushToolIcon extends Tool.ToolIcon {
        @Override
        protected void paintIcon(Graphics2D g) {
            // based on brush_tool.svg
            Path2D shape = new Path2D.Double();

            shape.moveTo(26.9375, 0.037109375);
            shape.curveTo(26.145912, 0.28008816, 25.539442, 0.9161635, 24.828125, 1.3320312);
            shape.curveTo(23.814398, 2.069558, 22.805168, 2.8130207, 21.826977, 3.597441);
            shape.curveTo(19.863256, 5.167382, 17.95563, 6.8091216, 16.121742, 8.529259);
            shape.curveTo(14.577, 9.985679, 13.077322, 11.491393, 11.667434, 13.079378);
            shape.curveTo(10.985824, 13.897236, 10.233374, 14.671378, 9.713719, 15.608811);
            shape.curveTo(9.185345, 16.515099, 8.805246, 17.498367, 8.496313, 18.498697);
            shape.curveTo(8.395515, 18.626312, 8.476824, 19.039421, 8.187745, 18.871857);
            shape.curveTo(7.1921477, 18.738533, 6.134835, 18.996895, 5.3448443, 19.62644);
            shape.curveTo(4.531526, 20.230675, 4.035764, 21.1389, 3.4824219, 21.964844);
            shape.curveTo(2.9489794, 22.784832, 2.5256033, 23.716242, 1.7421875, 24.33789);
            shape.curveTo(1.1186419, 24.75492, 0.11944403, 25.169472, 0.29492188, 26.09375);
            shape.curveTo(0.45163387, 26.842884, 1.2562873, 27.125015, 1.8970894, 27.33005);
            shape.curveTo(2.8067741, 27.53693, 3.7520325, 27.594017, 4.681838, 27.607592);
            shape.curveTo(5.8432894, 27.576454, 7.0062246, 27.447554, 8.134766, 27.166016);
            shape.curveTo(9.196509, 26.851646, 10.079067, 26.077631, 10.635744, 25.132713);
            shape.curveTo(11.111429, 24.385746, 11.357354, 23.523472, 11.517578, 22.660156);
            shape.curveTo(11.43723, 22.30564, 11.850601, 22.36445, 12.046448, 22.228456);
            shape.curveTo(12.921212, 21.871424, 13.754507, 21.39437, 14.472656, 20.777344);
            shape.curveTo(15.781392, 19.639624, 16.878502, 18.287962, 17.971893, 16.94902);
            shape.curveTo(19.159994, 15.486305, 20.265392, 13.959362, 21.342113, 12.413531);
            shape.curveTo(21.978598, 11.50273, 22.583694, 10.569926, 23.194439, 9.641789);
            shape.curveTo(23.88814, 8.537527, 24.580744, 7.432601, 25.224754, 6.298321);
            shape.curveTo(26.19168, 4.6604705, 27.061821, 2.966444, 27.855469, 1.2382812);
            shape.curveTo(28.162935, 0.62783056, 27.576368, -0.085242756, 26.9375, 0.037109375);
            shape.closePath();

            shape.moveTo(23.59961, 5.6132812);
            shape.curveTo(22.339964, 7.732425, 21.019167, 9.816201, 19.601068, 11.833224);
            shape.curveTo(18.427746, 13.504073, 17.199286, 15.137625, 15.892474, 16.706614);
            shape.curveTo(14.99784, 17.760288, 14.125215, 18.85511, 13.015115, 19.694159);
            shape.curveTo(12.398359, 20.11887, 11.735267, 20.50584, 11.017578, 20.734375);
            shape.curveTo(10.682943, 20.395832, 10.348307, 20.057293, 10.013672, 19.71875);
            shape.curveTo(10.383976, 18.431059, 10.804265, 17.133957, 11.548852, 16.007347);
            shape.curveTo(12.226638, 15.082865, 12.999885, 14.230518, 13.771484, 13.382812);
            shape.curveTo(14.950818, 12.117063, 16.170456, 10.889238, 17.43914, 9.71288);
            shape.curveTo(18.52587, 8.690398, 19.644182, 7.702363, 20.781528, 6.7366104);
            shape.curveTo(21.655563, 5.992977, 22.5486, 5.270964, 23.448025, 4.558366);
            shape.curveTo(23.881365, 4.225108, 24.314707, 3.891851, 24.748047, 3.5585938);
            shape.curveTo(24.365234, 4.2434897, 23.982422, 4.9283853, 23.59961, 5.6132812);
            shape.closePath();

            shape.moveTo(8.064453, 20.578125);
            shape.curveTo(8.697029, 20.575447, 8.981803, 21.224638, 9.428168, 21.570967);
            shape.curveTo(9.555709, 21.801142, 10.022954, 21.929466, 9.884301, 22.23945);
            shape.curveTo(9.716332, 23.140547, 9.464694, 24.08691, 8.800781, 24.757812);
            shape.curveTo(8.132112, 25.4087, 7.1796045, 25.59955, 6.287619, 25.71645);
            shape.curveTo(5.1412277, 25.891186, 3.9700973, 25.908194, 2.8222656, 25.736328);
            shape.curveTo(3.6933331, 25.003983, 4.301696, 24.026129, 4.915642, 23.082129);
            shape.curveTo(5.4617577, 22.29021, 5.9230003, 21.402695, 6.7109375, 20.816406);
            shape.curveTo(7.1046047, 20.533644, 7.601538, 20.513977, 8.064453, 20.578125);
            shape.closePath();

            g.fill(shape);
        }
    }
}