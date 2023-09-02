/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
package pixelitor.utils;

import javax.swing.*;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static java.awt.Desktop.Action.BROWSE;

/**
 * An action that opens a URL in an external browser
 */
public class OpenInBrowserAction extends AbstractAction {
    public static final boolean CAN_BROWSE = Desktop.isDesktopSupported()
        && Desktop.getDesktop().isSupported(BROWSE);

    private URI uri;

    public OpenInBrowserAction(String name, String url) {
        super(name);
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            Messages.showException(e);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        openURI(uri);
    }

    private static void openURI(URI uri) {
        if (!CAN_BROWSE) {
            return;
        }
        try {
            Desktop.getDesktop().browse(uri);
        } catch (IOException e) {
            Messages.showException(e);
        }
    }
}
