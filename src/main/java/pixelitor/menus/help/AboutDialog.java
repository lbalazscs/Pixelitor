/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.Themes;
import pixelitor.utils.OpenInBrowserAction;

import javax.swing.*;
import java.awt.Component;
import java.awt.Dimension;
import java.net.URL;

/**
 * The "About" dialog of the app.
 */
public class AboutDialog {
    public static final String WEBSITE_URL = "https://pixelitor.sourceforge.io";
    private static final String APP_ICON_PATH = "/images/pixelitor_icon48.png";

    private AboutDialog() {
        // should not be instantiated
    }

    public static void showDialog(String dialogTitle) {
        var tabbedPane = new JTabbedPane();

        tabbedPane.add("About", createAboutPanel());
        tabbedPane.add("Credits", createCreditsPanel());
        tabbedPane.add("System Info", new SystemInfoPanel());

        new DialogBuilder()
            .title(dialogTitle)
            .content(tabbedPane)
            .withScrollbars()
            .noCancelButton()
            .show();
    }

    private static JPanel createCreditsPanel() {
        JPanel p = new JPanel();
        String text = "<html>Pixelitor was written by <b>László Balázs-Csíki</b>." +
            "<br><br><b>Anirudh Sharma</b> and <b>Łukasz Kurzaj</b> contributed many" +
            "<br>improvements, see the release notes." +
            "<br>The Sepia filter was contributed by <b>Daniel Wreczycki</b>." +
            "<br><br>Pixelitor includes <ul><li>the image filter library by <b>Jerry Huxtable</b> " +
            "<li>many components by <b>Jeremy Wood</b>" +
            "<li>the fast math library by <b>Jeff Hain</b>" +
            "<li>the TwelveMonkeys library by <b>Harald Kuhr</b>" +
            "<li>the metadata-extractor library by <b>Drew Noakes</b>" +
            "<li>the animated GIF encoder by <b>Kevin Weiner</b>" +
            "<li>the GIF decoder by <b>Dhyan Blum</b>" +
            "<li>the Canny Edge Detector by <b>Tom Gibara</b>" +
            "<li>the SwingX library";
        p.add(new JLabel(text));

        return p;
    }

    private static JComponent createAboutPanel() {
        JComponent p = Box.createVerticalBox();

        addLabel(p, AboutDialog.class.getResource(APP_ICON_PATH));

        addLabel(p, "<html><b><font size=+1>Pixelitor</font></b></html>");
        addLabel(p, "Version " + Pixelitor.VERSION);
        p.add(Box.createRigidArea(new Dimension(10, 20)));
        addLabel(p, "<html><center> Copyright © 2009-2026 László Balázs-Csíki " +
            "<br>and Contributors<br><br>");
        addLabel(p, "lbalazscs@gmail.com");

        p.add(createLinkButton(p));
        p.add(Box.createGlue());
        return p;
    }

    private static JButton createLinkButton(JComponent aboutPanel) {
        String fontColor = Themes.getActive().isDark() ? "#77ABD4" : "#000099";
        String linkButtonText = "<html><font color=\"" + fontColor + "\"><u>" + WEBSITE_URL + "</u></font>";
        var linkButton = new JButton(linkButtonText);

        linkButton.setHorizontalAlignment(SwingConstants.CENTER);
        linkButton.setBorderPainted(false);
        linkButton.setFocusPainted(false);
        linkButton.setOpaque(false);
        linkButton.setBackground(aboutPanel.getBackground());
        linkButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        linkButton.addActionListener(new OpenInBrowserAction(null, WEBSITE_URL));

        return linkButton;
    }

    private static void addLabel(JComponent p, URL imageURL) {
        var imageIcon = new ImageIcon(imageURL);
        var label = new JLabel(imageIcon, SwingConstants.CENTER);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(label);
    }

    private static void addLabel(JComponent p, String text) {
        var label = new JLabel(text, SwingConstants.CENTER);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(label);
    }
}
