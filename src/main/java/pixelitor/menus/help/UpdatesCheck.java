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

import pixelitor.Build;
import pixelitor.gui.utils.Dialogs;
import pixelitor.utils.Messages;
import pixelitor.utils.OpenInBrowserAction;

import javax.swing.*;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

public class UpdatesCheck {

    private UpdatesCheck() {
    }

    public static void checkForUpdates() {
        try {
            Properties versionInfo = getVersionInfo();
            String lastVersion = versionInfo.getProperty("last_version");

            if(Build.VERSION_NUMBER.equals(lastVersion)) {
                String msg = String.format("You already have the latest version (%s) of Pixelitor installed", lastVersion);
                Messages.showInfo("Pixelitor is up to date", msg);
            } else {
                String requiredJavaVersion = versionInfo.getProperty("required_java_version"); // like "7"
                newVersionAlert(lastVersion, requiredJavaVersion);
            }
        } catch (IOException e) {
            String msg = "Could not check for updates on the Pixelitor website.";
            String title = "Could not check for updates";
            Object[] options = {"See Details", "Close"};
            if(Dialogs.showOKCancelDialog(msg, title, options, 1, JOptionPane.ERROR_MESSAGE)) {
                Messages.showException(e);
            }
        }
    }

    private static void newVersionAlert(String lastVersion, String requiredJavaVersion) {
        // see http://stackoverflow.com/questions/2591083/getting-version-of-java-in-runtime

        String msg = String.format("There is a newer version (%s) available.", lastVersion);

        if(needsJavaUpdate(requiredJavaVersion)) {
            msg += String.format("\nAlso note that the newest Pixelitor requires Java %s" +
                            "\n(It is currently running on Java %d)",
                    requiredJavaVersion, getCurrentMainJavaVersion());
        }
        String title = "Pixelitor is not up to date";
        Object[] options = {"Go to the Pixelitor homepage", "Close"};
        if(Dialogs.showOKCancelWarningDialog(msg, title, options, 0)) {
            new OpenInBrowserAction(null, AboutDialog.HOME_PAGE).actionPerformed(null);
        }
    }

    private static boolean needsJavaUpdate(String requiredJavaVersion) {
        if(requiredJavaVersion.equals("7")) {
            return false; // we always have at least Java 7, otherwise this code would not run
        }
        int currentMainJavaVersion = getCurrentMainJavaVersion();
        return Integer.parseInt(requiredJavaVersion) > currentMainJavaVersion;
    }

    private static int getCurrentMainJavaVersion() {
        int version = Integer.parseInt(System.getProperty("java.version").substring(2,3));
        if(version != 1) {
            return version;
        } else {
            // OMG, are we already running on Java 10+?
            return Integer.parseInt(System.getProperty("java.version").substring(2,4));
        }
    }

    private static Properties getVersionInfo() throws IOException {
        URL lastVersionURL = new URL("http://pixelitor.sourceforge.net/version_info.txt");
        URLConnection conn = lastVersionURL.openConnection();
        Properties properties = new Properties();
        properties.load(conn.getInputStream());
        return properties;
    }
}
