/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.OpenComps;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.View;
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

        addString("Pixelitor Version", Build.VERSION_NUMBER);
        addSystemNode();
        addActiveToolNode();
        addHistoryNode();

        addImageNodes();

//        addQuotedStringChild("Opening Folder", FileChoosers.getLastOpenDir().getAbsolutePath());
//        addQuotedStringChild("Saving Folder", FileChoosers.getLastSaveDir().getAbsolutePath());
    }

    private void addActiveToolNode() {
        Tool tool = Tools.getCurrent();
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
        List<View> views = OpenComps.getViews();

        int nrOpenImages = views.size();
        addInt("Number of Open Images", nrOpenImages);

        View activeView = OpenComps.getActiveView();
        for (View view : views) {
            ViewNode node;
            if (view == activeView) {
                node = new ViewNode("ACTIVE Image - " + view.getComp().getName(), view);
            } else {
                node = new ViewNode("Image - " + view.getComp().getName(), view);
            }
            add(node);
        }
    }
}
