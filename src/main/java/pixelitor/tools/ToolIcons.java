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

import pixelitor.gui.utils.Themes;
import pixelitor.tools.gui.ToolButton;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.CAP_ROUND;
import static java.awt.BasicStroke.JOIN_MITER;
import static java.awt.BasicStroke.JOIN_ROUND;

public class ToolIcons {
    private ToolIcons() {
    }

    public static void paintMoveIcon(Graphics2D g) {
        // the shape is based on move_tool.svg
        Path2D shape = new Path2D.Double();
        // start at the top
        shape.moveTo(14, 0);
        shape.lineTo(18, 5);
        shape.lineTo(15, 5);
        shape.lineTo(15, 13);

        // east arrow
        shape.lineTo(23, 13);
        shape.lineTo(23, 10);
        shape.lineTo(28, 14);
        shape.lineTo(23, 18);
        shape.lineTo(23, 15);
        shape.lineTo(15, 15);

        // south arrow
        shape.lineTo(15, 23);
        shape.lineTo(18, 23);
        shape.lineTo(14, 28);
        shape.lineTo(10, 23);
        shape.lineTo(13, 23);
        shape.lineTo(13, 15);

        // west arrow
        shape.lineTo(5, 15);
        shape.lineTo(5, 18);
        shape.lineTo(0, 14);
        shape.lineTo(5, 10);
        shape.lineTo(5, 13);
        shape.lineTo(13, 13);

        // finish north arrow
        shape.lineTo(13, 5);
        shape.lineTo(10, 5);
        shape.closePath();

        g.fill(shape);
    }

    public static void paintCropIcon(Graphics2D g) {
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

    public static void paintRectangleSelectionIcon(Graphics2D g) {
        // based on selection_tool.svg

        // north
        g.fillRect(1, 1, 4, 2);
        g.fillRect(9, 1, 4, 2);
        g.fillRect(17, 1, 4, 2);
        // east
        g.fillRect(25, 1, 2, 4);
        g.fillRect(25, 9, 2, 4);
        g.fillRect(25, 17, 2, 4);
        // south
        g.fillRect(7, 25, 4, 2);
        g.fillRect(15, 25, 4, 2);
        g.fillRect(23, 25, 4, 2);
        // west
        g.fillRect(1, 7, 2, 4);
        g.fillRect(1, 15, 2, 4);
        g.fillRect(1, 23, 2, 4);
    }

    public static void paintEllipseSelectionIcon(Graphics2D g) {
        // draws a dashed ellipse
        g.setStroke(new BasicStroke(2,
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
            0, new float[]{4.1888f, 4.1888f}, 0));
        g.drawOval(2, 2, 24, 24);
    }

    public static void paintLassoSelectionIcon(Graphics2D g) {
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

    public static void paintPolygonalSelectionIcon(Graphics2D g) {
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
    }

    public static void paintMagicWandSelectionIcon(Graphics2D g) {
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

    static void paintBrushIcon(Graphics2D g) {
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

    static void paintCloneIcon(Graphics2D g) {
        // based on clone_tool.svg

        // body
        Path2D shape = new Path2D.Double();
        shape.moveTo(0.9556584, 15.885808);
        shape.lineTo(4.6826134, 26.81291);
        shape.lineTo(23.317389, 26.81291);
        shape.lineTo(27.044344, 15.885808);
        shape.lineTo(14.029414, 15.885808);
        shape.lineTo(5.157098, 15.885808);
        shape.closePath();

        g.setStroke(new BasicStroke(1.4f, CAP_BUTT, JOIN_ROUND, 4));
        g.draw(shape);

        // triangle
        shape = new Path2D.Double();
        shape.moveTo(10.273045, 19.449001);
        shape.lineTo(14.0, 25.150095);
        shape.lineTo(17.726954, 19.449001);
        shape.closePath();

        g.fill(shape);
        g.draw(shape);

        // handle
        shape = new Path2D.Double();
        shape.moveTo(12.602392, 15.648266);
        shape.curveTo(12.602392, 15.648266, 8.388771, 10.379845, 8.438116, 6.145397);
        shape.curveTo(8.471819, 3.2532463, 10.364073, 1.4242454, 14.003829, 1.4242454);
        shape.curveTo(17.643581, 1.4242454, 19.590433, 3.2958915, 19.574783, 6.2119613);
        shape.curveTo(19.552193, 10.422222, 15.397608, 15.648266, 15.397608, 15.648266);

        g.draw(shape);
    }

    static void paintEraserIcon(Graphics2D g) {
        // based on eraser_tool.svg
        Path2D shape = new Path2D.Double();

        shape.moveTo(16.78125, 1.6210938);
        shape.curveTo(15.8157015, 1.6210741, 14.889705, 2.004681, 14.207031, 2.6875);
        shape.lineTo(14.205078, 2.6875);
        shape.lineTo(1.6855469, 15.207031);
        shape.curveTo(0.2648747, 16.628397, 0.2648747, 18.93215, 1.6855469, 20.353516);
        shape.lineTo(6.236328, 24.904297);
        shape.curveTo(6.9190016, 25.587116, 7.8449984, 25.970722, 8.810547, 25.970703);
        shape.lineTo(13.830078, 25.970703);
        shape.curveTo(13.835288, 25.970064, 13.840496, 25.969412, 13.845703, 25.96875);
        shape.lineTo(22.435547, 25.96875);
        shape.curveTo(22.924255, 25.96875, 23.316406, 25.574646, 23.316406, 25.085938);
        shape.lineTo(23.316406, 25.064453);
        shape.curveTo(23.316406, 24.575745, 22.924255, 24.18164, 22.435547, 24.18164);
        shape.lineTo(17.126953, 24.18164);
        shape.lineTo(26.414062, 14.894531);
        shape.curveTo(27.096909, 14.21187, 27.480543, 13.2858715, 27.480543, 12.3203125);
        shape.curveTo(27.480543, 11.3547535, 27.096909, 10.428755, 26.414062, 9.746094);
        shape.lineTo(19.355469, 2.6875);
        shape.curveTo(18.672796, 2.004681, 17.7468, 1.6210741, 16.78125, 1.6210938);
        shape.closePath();

        shape.moveTo(16.78125, 3.4414062);
        shape.curveTo(17.264025, 3.4413965, 17.727022, 3.6332, 18.06836, 3.9746094);
        shape.lineTo(25.126953, 11.033203);
        shape.curveTo(25.468376, 11.374534, 25.660192, 11.837533, 25.660192, 12.3203125);
        shape.curveTo(25.660192, 12.803092, 25.468376, 13.266091, 25.126953, 13.607422);
        shape.lineTo(16.695312, 22.039062);
        shape.lineTo(7.0625, 12.40625);
        shape.lineTo(15.494141, 3.9746094);
        shape.curveTo(15.835477, 3.6332, 16.298475, 3.4413965, 16.78125, 3.4414062);
        shape.closePath();

        shape.moveTo(5.7753906, 13.693359);
        shape.lineTo(15.408203, 23.326172);
        shape.lineTo(15.119141, 23.617188);
        shape.curveTo(14.777804, 23.958597, 14.314806, 24.1504, 13.832031, 24.15039);
        shape.lineTo(8.810547, 24.15039);
        shape.curveTo(8.328449, 24.149883, 7.8662486, 23.958118, 7.5253906, 23.617188);
        shape.lineTo(2.9746094, 19.066406);
        shape.curveTo(2.2649238, 18.355835, 2.2649238, 17.204712, 2.9746094, 16.49414);
        shape.lineTo(5.7753906, 13.693359);
        shape.closePath();

        g.fill(shape);
    }

    static void paintSmudgeIcon(Graphics2D g) {
        // based on smudge_tool.svg

        Path2D shape = new Path2D.Double();
        shape.moveTo(22.147558, 2.4032667);
        shape.curveTo(24.571192, 8.312117, 24.440912, 17.305197, 23.313412, 17.941687);
        shape.curveTo(21.999336, 18.683477, 19.62295, 19.883547, 18.796078, 20.238096);
        shape.curveTo(17.900263, 20.622177, 17.936176, 20.847305, 13.613189, 20.898235);
        shape.curveTo(11.353691, 20.926235, 13.273945, 18.784855, 13.844296, 18.054346);
        shape.curveTo(14.628173, 17.050365, 16.27149, 18.165026, 17.198284, 17.238327);
        shape.curveTo(18.125061, 16.311558, 18.14161, 11.826557, 17.214813, 12.753326);
        shape.curveTo(16.28802, 13.680137, 10.402279, 23.827217, 7.847664, 25.839296);
        shape.curveTo(5.9332438, 27.347157, 4.7676945, 27.763706, 3.3247247, 27.160036);
        shape.curveTo(2.032953, 26.619627, 2.6445398, 25.255596, 3.4592745, 24.813887);
        shape.curveTo(6.5001836, 23.165287, 7.438078, 20.790596, 9.101483, 18.542076);
        shape.curveTo(9.880999, 17.488396, 12.282015, 13.668896, 13.881689, 11.873806);
        shape.curveTo(11.842229, 13.5384865, 9.519419, 18.817917, 8.774448, 18.702995);
        shape.curveTo(8.342622, 18.636395, 7.3511715, 17.627275, 7.3511715, 16.700466);
        shape.curveTo(7.3511715, 15.773696, 10.068112, 12.470516, 11.375208, 10.529186);
        shape.curveTo(9.828844, 12.499756, 8.350622, 14.858656, 7.6307526, 15.349896);
        shape.curveTo(6.6277995, 16.034336, 5.596125, 14.886256, 6.0640936, 13.575437);
        shape.curveTo(6.297478, 12.921747, 8.215015, 10.376117, 9.820339, 8.357027);
        shape.curveTo(8.008162, 10.500257, 7.4930644, 11.035727, 6.7325964, 11.983277);
        shape.curveTo(6.1860065, 12.664357, 4.3971505, 12.153507, 4.756982, 10.893757);
        shape.curveTo(5.0504823, 9.866137, 13.408393, 1.0940566, 13.408393, 1.0940566);
        shape.lineTo(13.408393, 1.0940566);

        g.setStroke(new BasicStroke(1.1584814f, CAP_ROUND, JOIN_ROUND, 4));
        g.draw(shape);
    }

    public static void paintGradientIcon(Graphics2D g) {
        Color startColor = Color.BLACK;
        Color endColor = Themes.getActive().isDark() ? g.getColor() : Color.WHITE;
        Paint gradient = new GradientPaint(0, 0, startColor,
            ToolButton.ICON_SIZE, 0, endColor);
        g.setPaint(gradient);
        g.fillRect(0, 0, ToolButton.ICON_SIZE, ToolButton.ICON_SIZE);
    }

    static void paintPaintBucketIcon(Graphics2D g) {
        // the shape is based on paint_bucket_tool.svg
        Path2D shape = new Path2D.Double();

        // bucket
        shape.moveTo(5.4289136, 12.759313);
        shape.lineTo(14.020062, 25.454193);
        shape.lineTo(26.406328, 18.948254);
        shape.lineTo(20.734503, 4.768684);
        shape.closePath();

        g.setStroke(new BasicStroke(1.3f, CAP_BUTT, JOIN_MITER, 4));
        g.draw(shape);

        // handle
        shape = new Path2D.Double();
        shape.moveTo(14.87057, 12.192133);
        shape.curveTo(14.87057, 12.192133, 11.7013235, 3.5592537, 13.550051, 2.5583534);
        shape.curveTo(15.398779, 1.5574434, 16.939384, 6.8122234, 16.939384, 6.8122234);
        shape.lineTo(16.939384, 6.8122234);

        g.draw(shape);

        // paint
        shape = new Path2D.Double();
        shape.moveTo(8.19497, 10.853959);
        shape.curveTo(5.256423, 10.799759, 0.59281015, 13.51276, 0.6504288, 15.789537);
        shape.curveTo(0.70804787, 18.066315, 1.8028003, 18.152325, 2.8399348, 18.206568);
        shape.curveTo(3.9284532, 18.238678, 4.7648406, 17.252796, 4.862978, 16.437378);
        shape.curveTo(4.978212, 15.298976, 5.03873, 14.855405, 5.635888, 13.452499);
        shape.curveTo(5.828665, 12.999517, 8.19497, 10.853959, 8.19497, 10.853959);
        shape.closePath();

        g.fill(shape);
    }

    static void paintColorPickerIcon(Graphics2D g) {
        Color GLASS_COLOR = new Color(0x68_00_00_00, true);

        // based on color_picker_tool.svg
        Path2D glassPath = new Path2D.Double();
        Color color = g.getColor();

        glassPath.moveTo(15.487128, 10.694453);
        glassPath.lineTo(1.8488811, 24.332703);
        glassPath.curveTo(1.8488811, 24.332703, 0.9396646, 25.241873, 1.8488811, 26.151114);
        glassPath.curveTo(2.7580976, 27.060343, 3.667314, 26.151114, 3.667314, 26.151114);
        glassPath.lineTo(17.305561, 12.512863);
        glassPath.closePath();

        g.setColor(GLASS_COLOR);
        g.fill(glassPath);

        g.setColor(color);

        g.setStroke(new BasicStroke(0.9106483f, CAP_BUTT, JOIN_MITER, 4));
        g.draw(glassPath);

        Path2D handlePath = new Path2D.Double();
        handlePath.moveTo(13.668696, 7.966804);
        handlePath.lineTo(16.396345, 5.239154);
        handlePath.lineTo(18.214779, 7.057564);
        handlePath.curveTo(18.214779, 7.057564, 21.90847, 1.6428838, 22.76086, 1.6022639);
        handlePath.curveTo(23.694431, 1.642764, 26.438318, 4.549124, 26.397728, 5.239154);
        handlePath.curveTo(26.397728, 6.132134, 20.942429, 9.785213, 20.942429, 9.785213);
        handlePath.lineTo(22.76086, 11.603653);
        handlePath.lineTo(20.03321, 14.331303);
        handlePath.closePath();

        g.setColor(color);

        g.fill(handlePath);
        g.setStroke(new BasicStroke(0.90921646f, CAP_BUTT, JOIN_MITER, 4));
        g.draw(handlePath);
    }

    public static void paintPenIcon(Graphics2D g) {
        // based on pen_tool.svg
        g.setStroke(new BasicStroke(1.5f, CAP_BUTT, JOIN_MITER, 4));

        Path2D.Double body = new Path2D.Double();
        body.moveTo(20.182807, -0.58412839);
        body.lineTo(15.885317, 4.1900467);
        body.lineTo(24.480293, 13.738471);
        body.lineTo(28.77778, 8.9642464);
        g.draw(body);

        Path2D.Double head = new Path2D.Double();
        head.moveTo(18.504954, 7.3429552);
        head.curveTo(18.504954, 7.3429552, 13.526367, 11.562249, 5.7244174, 11.725182);
        head.lineTo(3.5014621, 24.583714);
        head.lineTo(17, 22.5);
        head.curveTo(17, 22.5, 17.107143, 15.553571, 21.218165, 10.083241);
        head.closePath();
        g.draw(head);

        Line2D line = new Line2D.Double(3.95, 24.0, 10.09609, 18);
        g.draw(line);

        Ellipse2D circle = new Ellipse2D.Double(9, 14.375, 4.8, 4.8);
        g.draw(circle);
    }

    public static void paintNodeIcon(Graphics2D g) {
        // based on path_edit_tool.svg

        g.setStroke(new BasicStroke(1.5f, CAP_BUTT, JOIN_MITER, 4));

        g.draw(new Rectangle2D.Double(2.75, 21.75, 3.5, 3.5)); // left
        g.draw(new Rectangle2D.Double(21.75, 21.75, 3.5, 3.5)); // right
        g.draw(new Rectangle2D.Double(12.25, 2.75, 3.5, 3.5)); // top

        // Circles
        g.draw(new Ellipse2D.Double(4.5 - 1.75, 4.5 - 1.75, 1.75 * 2, 1.75 * 2)); // left
        g.draw(new Ellipse2D.Double(23.5 - 1.75, 4.5 - 1.75, 1.75 * 2, 1.75 * 2)); // right

        g.draw(new Line2D.Double(6.5, 4.5, 12, 4.5)); // left
        g.draw(new Line2D.Double(16, 4.5, 21.5, 4.5)); // right

        Path2D.Double leftPath = new Path2D.Double();
        leftPath.moveTo(4.5, 21.5);
        leftPath.curveTo(4.5, 21.5, 4.5, 4.5, 12.0, 4.5);
        g.draw(leftPath);

        Path2D.Double rightPath = new Path2D.Double();
        rightPath.moveTo(23.5, 21.5);
        rightPath.curveTo(23.5, 21.5, 23.5, 4.5, 16.25, 4.500000);
        g.draw(rightPath);
    }

    public static void paintTransformPathIcon(Graphics2D g) {
        // based on transfrom_tool.svg
        Path2D shape = new Path2D.Double();

        // top left rectangle
        shape.append(new Rectangle2D.Double(2.75, 2.75, 5.5, 5.5), false);
        // bottom left rectangle
        shape.append(new Rectangle2D.Double(2.75, 19.75, 5.5, 5.5), false);
        // top right rectangle
        shape.append(new Rectangle2D.Double(19.75, 2.75, 5.5, 5.5), false);
        // bottom right rectangle
        shape.append(new Rectangle2D.Double(19.75, 19.75, 5.5, 5.5), false);

        // left line segment
        shape.append(new Line2D.Double(5.5, 8.5, 5.5, 19.5), false);
        // top line segment
        shape.append(new Line2D.Double(8.5, 5.5, 19.5, 5.5), false);
        // right line segment
        shape.append(new Line2D.Double(22.5, 8.5, 22.5, 19.5), false);
        // bottom line segment
        shape.append(new Line2D.Double(8.5, 22.5, 19.5, 22.5), false);

        g.setStroke(new BasicStroke(1.5f, CAP_BUTT, JOIN_MITER, 4));
        g.draw(shape);
    }

    public static void paintShapesIcon(Graphics2D g) {
        // the shape is based on shapes_tool.svg
        Path2D shape = new Path2D.Double();

        shape.moveTo(14.0, 5.134844);
        shape.curveTo(14, 5.134844, 15.964081, 1.4467101, 20.675045, 1.4467101);
        shape.curveTo(24.443815, 1.4467101, 26.260193, 5.2239814, 26.286139, 9.187077);
        shape.curveTo(26.328203, 15.612079, 16.0, 21.0, 14, 26.0);
        shape.curveTo(12.0, 21.0, 1.8311971, 15.612079, 1.8311971, 9.001549);
        shape.curveTo(1.8311971, 5.224129, 3.7155826, 1.44671, 7.484354, 1.44671);
        shape.curveTo(12.195311, 1.44671, 14, 5.134844, 14, 5.134844);
        shape.closePath();

        g.setStroke(new BasicStroke(1.5f));
        g.draw(shape);
    }

    static void paintHandIcon(Graphics2D g) {
        // based on hand_tool.svg
        Path2D shape = new Path2D.Double();

        shape.moveTo(12.343156, 28.583105);
        shape.curveTo(9.511524, 26.499704, 7.455484, 25.649765, 6.0110054, 24.676895);
        shape.curveTo(3.610977, 23.060486, 0.71735954, 18.394806, 0.6245651, 16.124395);
        shape.curveTo(0.5752961, 14.918925, 2.3428454, 14.583045, 4.695234, 18.303455);
        shape.curveTo(8.162258, 22.297945, 8.388426, 20.332075, 8.519196, 16.453156);
        shape.curveTo(8.603289, 13.958816, 7.0998454, 7.413025, 6.8744807, 4.6523046);
        shape.curveTo(6.710009, 2.6375546, 9.053727, 1.8152146, 9.670496, 3.8710446);
        shape.curveTo(10.287264, 5.9269447, 11.004557, 9.481285, 11.388325, 11.625285);
        shape.curveTo(11.849692, 14.202866, 12.906976, 13.664685, 12.896788, 12.312215);
        shape.curveTo(12.874758, 9.387884, 12.384279, 3.048664, 12.9188175, 1.7329245);
        shape.curveTo(13.453357, 0.41713417, 15.180315, 0.088204265, 15.591479, 1.8561544);
        shape.curveTo(16.002672, 3.6242244, 16.113848, 8.561304, 16.35362, 11.395505);
        shape.curveTo(16.553474, 13.757845, 17.765564, 13.042934, 17.896145, 11.854835);
        shape.curveTo(18.19308, 9.153114, 18.140781, 3.089674, 18.963142, 2.1028538);
        shape.curveTo(19.785501, 1.1160336, 21.00314, 1.0588036, 21.306862, 2.3496637);
        shape.curveTo(21.635801, 3.7476737, 21.278942, 9.100004, 21.060158, 12.382435);
        shape.curveTo(20.924759, 14.413965, 21.917635, 14.855615, 22.49928, 12.793755);
        shape.curveTo(23.260397, 10.095695, 23.430496, 8.042543, 24.18718, 6.197914);
        shape.curveTo(24.585037, 5.228044, 26.303837, 5.144144, 26.158772, 6.9139442);
        shape.curveTo(25.95318, 9.422104, 25.390371, 11.784515, 25.089708, 14.027325);
        shape.curveTo(24.55175, 18.040184, 24.072752, 20.420914, 25.048588, 24.101194);
        shape.curveTo(25.673756, 26.458975, 29.489317, 29.158705, 29.489317, 29.158705);

        g.setStroke(new BasicStroke(1.1522429f, CAP_ROUND, JOIN_ROUND, 4));
        g.draw(shape);
    }

    static void paintZoomIcon(Graphics2D g) {
        // based on zoom_tool.svg
        g.setStroke(new BasicStroke(2.0f, CAP_BUTT, JOIN_MITER, 4));

        Ellipse2D circle = new Ellipse2D.Double(9, 1, 18, 18);
        g.draw(circle);

        Line2D hor = new Line2D.Double(12.0, 10.0, 24.0, 10.0);
        g.draw(hor);

        Line2D ver = new Line2D.Double(18.0, 16.0, 18.0, 4.0);
        g.draw(ver);

        Path2D shape = new Path2D.Double();
        shape.moveTo(13.447782, 17.801485);
        shape.lineTo(4.73615, 26.041084);
        shape.curveTo(4.73615, 26.041084, 2.9090462, 26.923565, 1.9954941, 26.041084);
        shape.curveTo(1.0819423, 25.158604, 1.9954941, 23.393635, 1.9954941, 23.393635);
        shape.lineTo(11.043547, 14.977204);

        g.setStroke(new BasicStroke(1.7017335f, CAP_BUTT, JOIN_MITER, 4));
        g.draw(shape);
    }
}
