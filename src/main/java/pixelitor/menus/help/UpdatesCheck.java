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

package pixelitor.menus.help;

import pixelitor.Pixelitor;
import pixelitor.gui.utils.Dialogs;
import pixelitor.utils.Messages;
import pixelitor.utils.OpenInBrowserAction;
import pixelitor.utils.Result;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static pixelitor.gui.GUIText.CLOSE_DIALOG;
import static pixelitor.menus.help.AboutDialog.WEBSITE_URL;
import static pixelitor.utils.Utils.getJavaMainVersion;

/**
 * Checking for new Pixelitor versions
 */
public class UpdatesCheck {

    private UpdatesCheck() {
    }

    public static void checkForUpdates() {
        Result<Properties, String> result = fetchLatestVersionInfo();
        if (!result.isSuccess()) {
            showUpdateCheckErrorDialog(result.errorDetails());
            return;
        }

        Properties versionInfo = result.get();
        String latestVersion = versionInfo.getProperty("last_version");
        if (latestVersion == null) {
            showUpdateCheckErrorDialog("missing last_version");
            return;
        }

        latestVersion = latestVersion.trim();
        if (Pixelitor.VERSION.equals(latestVersion)) {
            showUpToDateDialog(latestVersion);
        } else {
            String requiredJavaVersion = versionInfo.getProperty(
                "required_java_version").trim();
            showNewVersionAlert(latestVersion, requiredJavaVersion);
        }
    }

    private static void showUpToDateDialog(String latestVersion) {
        String msg = format(
            "You already have the latest version (%s) of Pixelitor installed",
            latestVersion);
        Messages.showInfo("Pixelitor is up to date", msg);
    }

    private static void showNewVersionAlert(String lastVersion, String requiredJavaVersion) {
        String msg = format("A newer version (%s) of Pixelitor is available.", lastVersion);

        if (isJavaUpdateRequired(requiredJavaVersion)) {
            msg += format("%nNote: The latest version requires Java %s.%n" +
                    "(Currently running on Java %d).",
                requiredJavaVersion, getJavaMainVersion());
        }
        String title = "New Version Available";
        Object[] options = {"Visit Pixelitor Homepage", CLOSE_DIALOG};
        if (Dialogs.showOKCancelWarningDialog(msg, title, options, 0)) {
            new OpenInBrowserAction(null, WEBSITE_URL).actionPerformed(null);
        }
    }

    private static boolean isJavaUpdateRequired(String requiredJavaVersion) {
        if (requiredJavaVersion.equals("21")) {
            // we already have at least Java 21, otherwise this code would not run
            return false;
        }
        int currentJavaVersion = getJavaMainVersion();
        return parseInt(requiredJavaVersion) > currentJavaVersion;
    }

    private static Result<Properties, String> fetchLatestVersionInfo() {
        Properties versionInfo;
        try {
            versionInfo = downloadVersionInfo();
        } catch (IOException | URISyntaxException e) {
            return Result.error(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return Result.ofNullable(versionInfo);
    }

    private static Properties downloadVersionInfo() throws IOException, URISyntaxException {
        URL versionInfoURL = new URI(
            WEBSITE_URL + "/version_info.txt").toURL();
        URLConnection connection = versionInfoURL.openConnection();
        Properties properties = new Properties();

        try (InputStream inputStream = connection.getInputStream()) {
            properties.load(inputStream);
        }
        
        return properties;
    }

    private static void showUpdateCheckErrorDialog(String errorDetails) {
        String title = "Update Check Failed";
        StringBuilder message = new StringBuilder("Unable to check for updates on the Pixelitor website.");

        if (errorDetails != null && !errorDetails.isEmpty()) {
            message.append("\nError details: ").append(errorDetails);
        }

        Dialogs.showErrorDialog(title, message.toString());
    }
}
