/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.menus.help;

import com.bric.util.JVM;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.utils.MemoryInfo;

import javax.swing.*;
import java.awt.GridBagLayout;
import java.awt.Toolkit;

import static java.lang.String.format;

class SystemInfoPanel extends JPanel {
    private final GridBagHelper gbh;

    public SystemInfoPanel() {
        super(new GridBagLayout());
        gbh = new GridBagHelper(this);

        addSystemProperties();
        addMemoryProperties();
    }

    private void addSystemProperties() {
        addProperty("Java Version:", "java.version");
        addProperty("Java VM:", "java.vm.name");
        addProperty("Java vendor:", "java.vendor");
        addProperty("OS:", "os.name");

        // rendering pipeline / acceleration
        addProperty("OpenGL:", "sun.java2d.opengl");
        addProperty("XRender:", "sun.java2d.xrender");
        if (JVM.isWindows) {
            addProperty("Direct3D:", "sun.java2d.d3d");
            addProperty("No DirectDraw:", "sun.java2d.noddraw");
        } else if (JVM.isMac) {
            addProperty("Metal:", "sun.java2d.metal");
        }

        gbh.addTwoLabels("AWT Toolkit:",
            Toolkit.getDefaultToolkit().getClass().getName());

        gbh.addTwoLabels("UI scaling:", format("%.2f",
            PixelitorWindow.get().getHiDPIScaling().getScaleY()));
    }

    private void addProperty(String label, String propName) {
        String property = System.getProperty(propName);
        if (property != null) {
            gbh.addTwoLabels(label, property);
        }
    }

    private void addMemoryProperties() {
        MemoryInfo mi = new MemoryInfo();

        gbh.addTwoLabels("Allocated Memory:", mi.getTotalMemory());
        gbh.addTwoLabels("Used Memory:", mi.getUsedMemory());
        gbh.addTwoLabels("Free Memory:", mi.getFreeMemory());
        gbh.addTwoLabels("Max Memory:", mi.getMaxMemory());
    }
}
