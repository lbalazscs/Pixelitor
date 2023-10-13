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

package pixelitor.menus.help;

import pixelitor.Pixelitor;
import pixelitor.gui.utils.Dialogs;
import pixelitor.utils.Messages;
import pixelitor.utils.OpenInBrowserAction;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static pixelitor.gui.GUIText.CLOSE_DIALOG;
import static pixelitor.menus.help.AboutDialog.HOME_PAGE;
import static pixelitor.utils.Utils.getJavaMainVersion;

/**
 * Checking for new Pixelitor versions
 */
public class UpdatesCheck {

    private UpdatesCheck() {
    }

    public static void checkForUpdates() {
        Properties versionInfo;
        try {
            versionInfo = downloadVersionInfo();
        } catch (IOException e) {
            showCouldNotCheckForUpdatesDialog();
            return;
        } catch (URISyntaxException e) {
            Messages.showException(e);
            return;
        }
        if (versionInfo == null) {
            showCouldNotCheckForUpdatesDialog();
            return;
        }
        String lastVersion = versionInfo.getProperty("last_version");
        if (lastVersion == null) {
            showCouldNotCheckForUpdatesDialog();
            return;
        }
        lastVersion = lastVersion.trim();

        if (Pixelitor.VERSION_NUMBER.equals(lastVersion)) {
            String msg = format(
                "You already have the latest version (%s) of Pixelitor installed",
                lastVersion);
            Messages.showInfo("Pixelitor is up to date", msg);
        } else {
            String requiredJavaVersion = versionInfo.getProperty(
                "required_java_version").trim(); // like "7"
            newVersionAlert(lastVersion, requiredJavaVersion);
        }
    }

    private static void showCouldNotCheckForUpdatesDialog() {
        String msg = "Could not check for updates on the Pixelitor website.";
        String title = "Could not check for updates";
        Dialogs.showErrorDialog(title, msg);
    }

    private static void newVersionAlert(String lastVersion, String requiredJavaVersion) {
        // see http://stackoverflow.com/questions/2591083/getting-version-of-java-in-runtime

        String msg = format("There is a newer version (%s) available.", lastVersion);

        if (needsJavaUpdate(requiredJavaVersion)) {
            msg += format("%nAlso note that the newest Pixelitor requires Java %s" +
                    "%n(It is currently running on Java %d)",
                requiredJavaVersion, getJavaMainVersion());
        }
        String title = "Pixelitor is not up to date";
        Object[] options = {"Go to the Pixelitor homepage", CLOSE_DIALOG};
        if (Dialogs.showOKCancelWarningDialog(msg, title, options, 0)) {
            new OpenInBrowserAction(null, HOME_PAGE).actionPerformed(null);
        }
    }

    private static boolean needsJavaUpdate(String requiredJavaVersion) {
        if (requiredJavaVersion.equals("8")) {
            // we always have at least Java 8, otherwise this code would not run
            return false;
        }
        int currentMainJavaVersion = getJavaMainVersion();
        return parseInt(requiredJavaVersion) > currentMainJavaVersion;
    }

    private static Properties downloadVersionInfo() throws IOException, URISyntaxException {
        URL lastVersionURL = new URI(
            HOME_PAGE + "/version_info.txt").toURL();
        URLConnection conn = lastVersionURL.openConnection();
        Properties properties = new Properties();
        properties.load(conn.getInputStream());
        return properties;
    }
}
