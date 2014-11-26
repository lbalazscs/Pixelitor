/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.menus.help;

import pixelitor.Build;
import pixelitor.PixelitorWindow;
import pixelitor.utils.GridBagHelper;
import pixelitor.utils.MemoryInfo;
import pixelitor.utils.OKDialog;
import pixelitor.utils.OpenInBrowserAction;

import javax.swing.*;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.net.URL;

public class AboutDialog extends OKDialog {
    private static Box box;
    public static final String HOME_PAGE = "http://pixelitor.sourceforge.net";

    public AboutDialog(JFrame owner, JComponent form) {
        super(owner, "About Pixelitor", form);
    }

    public static void showDialog(PixelitorWindow pixelitorWindow) {
        createAboutBox();

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("About", new JScrollPane(box));
        tabbedPane.add("Credits", new JScrollPane(createCreditsPanel()));
        tabbedPane.add("System Info", new JScrollPane(createSysInfoPanel()));

        new AboutDialog(pixelitorWindow, tabbedPane);
    }

    private static JPanel createCreditsPanel() {
        JPanel p = new JPanel();
        p.add(new JLabel("<html>Pixelitor was written by <b>László Balázs-Csíki</b>." +
                "<br><br>The Sepia filter was contributed by <b>Daniel Wreczycki</b>." +
                "<br><br>Pixelitor uses <ul><li>the image filter library by <b>Jerry Huxtable</b> " +
                "<li>some components by <b>Jeremy Wood</b>" +
                "<li>the fast math library by <b>Jeff Hain</b>" +
                "<li>the animated GIF encoder by <b>Kevin Weiner</b>" +
                "<li>the Canny Edge Detector by <b>Tom Gibara</b>" +
                "<li>the SwingX library"));

        return p;
    }

    private static JPanel createSysInfoPanel() {
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());

        GridBagHelper.addLabel(p, "Java Version:", 0, 0);
        GridBagHelper.addControl(p, new JLabel(System.getProperty("java.version")));

        GridBagHelper.addLabel(p, "Java VM:", 0, 1);
        GridBagHelper.addControl(p, new JLabel(System.getProperty("java.vm.name")));

        GridBagHelper.addLabel(p, "OS:", 0, 2);
        GridBagHelper.addControl(p, new JLabel(System.getProperty("os.name")));

        MemoryInfo memoryInfo = new MemoryInfo();
        long freeMemoryMB = memoryInfo.getFreeMemoryMB();
        long maxMemoryMB = memoryInfo.getMaxMemoryMB();
        long totalMemoryMB = memoryInfo.getTotalMemoryMB();
        long usedMemoryMB = memoryInfo.getUsedMemoryMB();

        GridBagHelper.addLabel(p, "Allocated Memory:", 0, 3);
        GridBagHelper.addControl(p, new JLabel(totalMemoryMB + " megabytes"));

        GridBagHelper.addLabel(p, "Used Memory:", 0, 4);
        GridBagHelper.addControl(p, new JLabel(usedMemoryMB + " megabytes"));

        GridBagHelper.addLabel(p, "Free Memory:", 0, 5);
        GridBagHelper.addControl(p, new JLabel(freeMemoryMB + " megabytes"));

        GridBagHelper.addLabel(p, "Max Memory:", 0, 6);
        GridBagHelper.addControl(p, new JLabel(maxMemoryMB + " megabytes"));

        return p;
    }

    private static void createAboutBox() {
        box = Box.createVerticalBox();

        addLabel(AboutDialog.class.getResource("/images/pixelitor_icon48.png"));

        addLabel("<html><b><font size=+1>Pixelitor</font></b></html>");
        addLabel("Version " + Build.VERSION_NUMBER);
        box.add(Box.createRigidArea(new Dimension(10, 20)));
        addLabel("<html><center> Copyright \u00A9 2009-2014 L\u00E1szl\u00F3 Bal\u00E1zs-Cs\u00EDki <br>and Contributors<br><br>");
        addLabel("lbalazscs\u0040gmail.com");

        OpenInBrowserAction browserAction = new OpenInBrowserAction(null, HOME_PAGE);

        JButton linkButton = new JButton("<HTML><FONT color=\"#000099\"><U>" + HOME_PAGE + "</U></FONT></HTML>");

        linkButton.setHorizontalAlignment(SwingConstants.CENTER);
        linkButton.setBorderPainted(false);
        linkButton.setFocusPainted(false);
        linkButton.setOpaque(false);
        linkButton.setBackground(box.getBackground());
        linkButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        linkButton.addActionListener(browserAction);

        box.add(linkButton);
        box.add(Box.createGlue());
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


