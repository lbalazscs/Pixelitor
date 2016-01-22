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

package pixelitor.menus.help;

import pixelitor.gui.utils.GridBagHelper;
import pixelitor.utils.MemoryInfo;

import javax.swing.*;
import java.awt.GridBagLayout;

class SystemInfoPanel extends JPanel {
    private final GridBagHelper gridBagHelper;

    public SystemInfoPanel() {
        super(new GridBagLayout());
        gridBagHelper = new GridBagHelper(this);

        addSystemProperties();
        addMemoryProperties();
    }

    private void addSystemProperties() {
        gridBagHelper.addTwoLabels("Java Version:", System.getProperty("java.version"));
        gridBagHelper.addTwoLabels("Java VM:", System.getProperty("java.vm.name"));
        gridBagHelper.addTwoLabels("OS:", System.getProperty("os.name"));
    }

    private void addMemoryProperties() {
        MemoryInfo memoryInfo = new MemoryInfo();
        long freeMemoryMB = memoryInfo.getFreeMemoryMB();
        long maxMemoryMB = memoryInfo.getMaxMemoryMB();
        long totalMemoryMB = memoryInfo.getTotalMemoryMB();
        long usedMemoryMB = memoryInfo.getUsedMemoryMB();

        gridBagHelper.addTwoLabels("Allocated Memory:", totalMemoryMB + " megabytes");
        gridBagHelper.addTwoLabels("Used Memory:", usedMemoryMB + " megabytes");
        gridBagHelper.addTwoLabels("Free Memory:", freeMemoryMB + " megabytes");
        gridBagHelper.addTwoLabels("Max Memory:", maxMemoryMB + " megabytes");
    }
}
