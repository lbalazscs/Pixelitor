/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

import pixelitor.utils.GridBagHelper;
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

    private void addMemoryProperties() {
        MemoryInfo memoryInfo = new MemoryInfo();
        long freeMemoryMB = memoryInfo.getFreeMemoryMB();
        long maxMemoryMB = memoryInfo.getMaxMemoryMB();
        long totalMemoryMB = memoryInfo.getTotalMemoryMB();
        long usedMemoryMB = memoryInfo.getUsedMemoryMB();

        gridBagHelper.addLabel("Allocated Memory:", 0, 3);
        gridBagHelper.addControl(new JLabel(totalMemoryMB + " megabytes"));

        gridBagHelper.addLabel("Used Memory:", 0, 4);
        gridBagHelper.addControl(new JLabel(usedMemoryMB + " megabytes"));

        gridBagHelper.addLabel("Free Memory:", 0, 5);
        gridBagHelper.addControl(new JLabel(freeMemoryMB + " megabytes"));

        gridBagHelper.addLabel("Max Memory:", 0, 6);
        gridBagHelper.addControl(new JLabel(maxMemoryMB + " megabytes"));
    }

    private void addSystemProperties() {
        gridBagHelper.addLabel("Java Version:", 0, 0);
        gridBagHelper.addControl(new JLabel(System.getProperty("java.version")));

        gridBagHelper.addLabel("Java VM:", 0, 1);
        gridBagHelper.addControl(new JLabel(System.getProperty("java.vm.name")));

        gridBagHelper.addLabel("OS:", 0, 2);
        gridBagHelper.addControl(new JLabel(System.getProperty("os.name")));
    }
}
