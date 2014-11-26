package pixelitor.menus;

import javax.swing.*;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class ShortcutTest {
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                buildGUI();
            }
        });
    }

    private static void buildGUI() {
        JFrame f = new JFrame("Test");
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JMenuBar menuBar = new JMenuBar();
        f.setJMenuBar(menuBar);
        JMenu menu = new JMenu("Test");
        menuBar.add(menu);

        setupMenuItem(menu, "Ctrl-C", KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
        setupMenuItem(menu, "Ctrl-D", KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK));
        setupMenuItem(menu, "Ctrl-E", KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));

        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    private static void setupMenuItem(JMenu menu, final String text, KeyStroke keyStroke) {
        JMenuItem menuItem = new JMenuItem(text);
        menu.add(menuItem);
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println(text + " menu activated");
            }
        });
        menuItem.setAccelerator(keyStroke);
    }
}
