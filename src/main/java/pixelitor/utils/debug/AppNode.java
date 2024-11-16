/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Pixelitor;
import pixelitor.Views;
import pixelitor.gui.PixelitorWindow;
import pixelitor.history.History;
import pixelitor.tools.Tools;

/**
 * A debugging node for the application as a whole, the root of the debug tree
 */
public class AppNode extends DebugNode {
    public AppNode() {
        super("Pixelitor", PixelitorWindow.get());

        addQuotedString("version", Pixelitor.VERSION);
        add(DebugNodes.createSystemNode());
        add(Tools.getActive().createDebugNode("active tool"));
        add(History.createDebugNode());

        addViewNodes();
    }

    private void addViewNodes() {
        addInt("# open images", Views.getNumViews());

        Views.forEachView(view -> {
            String prefix = view.isActive() ? "active view - " : "view - ";
            add(view.createDebugNode(prefix + view.getName()));
        });
    }
}
