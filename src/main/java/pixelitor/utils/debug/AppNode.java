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

import pixelitor.Build;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.PixelitorWindow;
import pixelitor.history.History;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.List;

/**
 * A debugging node for the application as a whole, the root of the debug tree
 */
public class AppNode extends DebugNode {
    public AppNode() {
        super("Pixelitor", PixelitorWindow.getInstance());

        addStringChild("Pixelitor Version", Build.VERSION_NUMBER);
        addSystemNode();
        addActiveToolNode();
        addHistoryNode();

        addImageNodes();

//        addQuotedStringChild("Opening Folder", FileChoosers.getLastOpenDir().getAbsolutePath());
//        addQuotedStringChild("Saving Folder", FileChoosers.getLastSaveDir().getAbsolutePath());

        addBooleanChild("Can Undo", History.canUndo());
        addBooleanChild("Can Redo", History.canRedo());
        addBooleanChild("Can Fade", History.canFade());
        addBooleanChild("Can Repeat", History.canRepeatOperation());
    }

    private void addActiveToolNode() {
        Tool tool = Tools.getCurrentTool();
        DebugNode toolNode = tool.getDebugNode();
        add(toolNode);
    }

    private void addHistoryNode() {
        DebugNode toolNode = History.getDebugNode();
        add(toolNode);
    }

    private void addSystemNode() {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        add(new SystemNode(device));
    }

    private void addImageNodes() {
        List<ImageComponent> images = ImageComponents.getICList();

        int nrOpenImages = images.size();
        addIntChild("Number of Open Images", nrOpenImages);

        ImageComponent activeIC = ImageComponents.getActiveIC();
        for (ImageComponent ic : images) {
            ImageComponentNode node;
            if (ic == activeIC) {
                node = new ImageComponentNode("ACTIVE Image - " + ic.getComp().getName(), ic);
            } else {
                node = new ImageComponentNode("Image - " + ic.getComp().getName(), ic);
            }
            add(node);
        }
    }
}
