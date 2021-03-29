/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

import static java.awt.BasicStroke.CAP_ROUND;
import static java.awt.BasicStroke.JOIN_ROUND;

/**
 * The hand tool.
 */
public class HandTool extends Tool {
    private final HandToolSupport handToolSupport = new HandToolSupport();

    HandTool() {
        super("Hand", 'H', "hand_tool.png",
            "<b>drag</b> to move the view (if there are scrollbars).",
            Cursors.HAND);
    }

    @Override
    public void initSettingsPanel() {
        settingsPanel.addAutoZoomButtons();
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        handToolSupport.mousePressed(e.getOrigEvent(), e.getViewport());
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        handToolSupport.mouseDragged(e.getOrigEvent(), e.getViewport());
    }

    @Override
    public void mouseReleased(PMouseEvent e) {
    }

    @Override
    public boolean hasHandToolForwarding() {
        return false;
    }

    @Override
    public Icon createIcon() {
        return new HandToolIcon();
    }

    private static class HandToolIcon extends Tool.ToolIcon {
        @Override
        public void paintIcon(Graphics2D g) {
            // based on hand_tool.svg
            Path2D shape = new Path2D.Float();

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
    }

}
