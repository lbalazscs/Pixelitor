/*
 * Copyright (c) 2015 Laszlo Balazs-Csiki
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

import pixelitor.Build;
import pixelitor.ImageComponent;
import pixelitor.ImageComponents;
import pixelitor.PixelitorWindow;
import pixelitor.history.History;
import pixelitor.io.FileChoosers;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.List;

/**
 * A debugging node for the whole application
 */
public class AppNode extends DebugNode {
    public AppNode() {
        super("Pixelitor", PixelitorWindow.getInstance());

        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        SystemNode systemNode = new SystemNode(device);
        add(systemNode);

        List<ImageComponent> images = ImageComponents.getImageComponents();

        int nrOpenImages = images.size();
        addIntChild("Number of Open Images", nrOpenImages);

        ImageComponent activeIC = ImageComponents.getActiveImageComponent();
        for (ImageComponent ic : images) {
            ImageComponentNode node;
            if (ic == activeIC) {
                node = new ImageComponentNode("ACTIVE Image - " + ic.getComp().getName(), ic);
            } else {
                node = new ImageComponentNode("Image - " + ic.getComp().getName(), ic);
            }
            add(node);
        }

        addStringChild("Pixelitor Version", Build.VERSION_NUMBER);
        addQuotedStringChild("Opening Folder", FileChoosers.getLastOpenDir().getAbsolutePath());
        addQuotedStringChild("Saving Folder", FileChoosers.getLastSaveDir().getAbsolutePath());

        addBooleanChild("Can Undo", History.canUndo());
        addBooleanChild("Can Redo", History.canRedo());
        addBooleanChild("Can Fade", History.canFade());
        addBooleanChild("Can Repeat", History.canRepeatOperation());
    }
}
