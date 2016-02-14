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
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.OKDialog;
import pixelitor.utils.OpenInBrowserAction;

import javax.swing.*;
import java.awt.Component;
import java.awt.Dimension;
import java.net.URL;

public class AboutDialog extends OKDialog {
    private static Box box;
    public static final String HOME_PAGE = "http://pixelitor.sourceforge.net";

    private AboutDialog(JFrame owner, JComponent form) {
        super(owner, form, "About Pixelitor");
    }

    public static void showDialog(PixelitorWindow pixelitorWindow) {
        createAboutBox();

        JTabbedPane tabbedPane = new JTabbedPane();
//        tabbedPane.add("About", new JScrollPane(box));
//        tabbedPane.add("Credits", new JScrollPane(createCreditsPanel()));
//        tabbedPane.add("System Info", new JScrollPane(new SystemInfoPanel()));

        tabbedPane.add("About", box);
        tabbedPane.add("Credits", createCreditsPanel());
        tabbedPane.add("System Info", new SystemInfoPanel());

        new AboutDialog(pixelitorWindow, tabbedPane);
    }

    private static JPanel createCreditsPanel() {
        JPanel p = new JPanel();
        p.add(new JLabel("<html>Pixelitor was written by <b>L\u00e1szl\u00f3 Bal\u00e1zs-Cs\u00edki</b>." +
                "<br><br>The Sepia filter was contributed by <b>Daniel Wreczycki</b>." +
                "<br><br>Pixelitor uses <ul><li>the image filter library by <b>Jerry Huxtable</b> " +
                "<li>many components by <b>Jeremy Wood</b>" +
                "<li>the fast math library by <b>Jeff Hain</b>" +
                "<li>the animated GIF encoder by <b>Kevin Weiner</b>" +
                "<li>the Canny Edge Detector by <b>Tom Gibara</b>" +
                "<li>the SwingX library"));

        return p;
    }

    private static void createAboutBox() {
        box = Box.createVerticalBox();

        addLabel(AboutDialog.class.getResource("/images/pixelitor_icon48.png"));

        addLabel("<html><b><font size=+1>Pixelitor</font></b></html>");
        addLabel("Version " + Build.VERSION_NUMBER);
        box.add(Box.createRigidArea(new Dimension(10, 20)));
        addLabel("<html><center> Copyright \u00A9 2009-2016 L\u00E1szl\u00F3 Bal\u00E1zs-Cs\u00EDki <br>and Contributors<br><br>");
        addLabel("lbalazscs\u0040gmail.com");

        JButton linkButton = createLinkButton();
        box.add(linkButton);

        box.add(Box.createGlue());
    }

    private static JButton createLinkButton() {
        JButton linkButton = new JButton("<HTML><FONT color=\"#000099\"><U>" + HOME_PAGE + "</U></FONT></HTML>");

        linkButton.setHorizontalAlignment(SwingConstants.CENTER);
        linkButton.setBorderPainted(false);
        linkButton.setFocusPainted(false);
        linkButton.setOpaque(false);
        linkButton.setBackground(box.getBackground());
        linkButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        linkButton.addActionListener(new OpenInBrowserAction(null, HOME_PAGE));
        return linkButton;
    }

    private static void addLabel(URL url) {
        ImageIcon imageIcon = new ImageIcon(url);
        JLabel label = new JLabel(imageIcon, JLabel.CENTER);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(label);
    }

    private static void addLabel(String text) {
        JLabel label = new JLabel(text, JLabel.CENTER);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(label);
    }
}


