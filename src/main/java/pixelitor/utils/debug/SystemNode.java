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
package pixelitor.utils.debug;

import pixelitor.gui.PixelitorWindow;
import pixelitor.utils.Utils;

import java.awt.DisplayMode;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.image.ColorModel;

/**
 * A debugging node for the OS settings
 */
public class SystemNode extends DebugNode {


    public SystemNode(GraphicsDevice device) {
        super("System", device);

        addStringChild("Java version", System.getProperty("java.version"));
        addStringChild("Java vendor", System.getProperty("java.vendor"));
        addStringChild("OS name", System.getProperty("os.name"));

        DisplayMode displayMode = device.getDisplayMode();

        int width = displayMode.getWidth();
        int height = displayMode.getHeight();
        int bitDepth = displayMode.getBitDepth();
        addIntChild("display width", width);
        addIntChild("display height", height);
        addIntChild("display bit depth", bitDepth);

        PixelitorWindow pw = PixelitorWindow.getInstance();
        addIntChild("app window width", pw.getWidth());
        addIntChild("app window height", pw.getHeight());

        addStringChild("max memory", Utils.getMaxHeapInMegabytes() + " Mb");
        addStringChild("used memory", Utils.getUsedMemoryInMegabytes() + " Mb");

        GraphicsConfiguration configuration = device.getDefaultConfiguration();
        ColorModel defaultColorModel = configuration.getColorModel();

        ColorModelNode colorModelNode = new ColorModelNode("Default Color Model", defaultColorModel);
        add(colorModelNode);
    }

}
