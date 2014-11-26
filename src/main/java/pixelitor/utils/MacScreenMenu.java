/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
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
package pixelitor.utils;

import javax.swing.*;

/**
 * See http://www.pushing-pixels.org/?p=366
 */
public class MacScreenMenu {
    private static Object menuBarUI;
    private static Object menuUI;
    private static Object menuItemUI;
    private static Object checkBoxMenuItemUI;
    private static Object radioButtonMenuItemUI;
    private static Object popupMenuUI;

    private MacScreenMenu() {
    }

    public static void saveTrickyUISettings() {
        long startTime = System.nanoTime();

//        try {
//            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        } catch (InstantiationException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (UnsupportedLookAndFeelException e) {
//            e.printStackTrace();
//        }

        menuBarUI = UIManager.get("MenuBarUI");
        menuUI = UIManager.get("MenuUI");
        menuItemUI = UIManager.get("MenuItemUI");
        checkBoxMenuItemUI = UIManager.get("CheckBoxMenuItemUI");
        radioButtonMenuItemUI = UIManager.get("RadioButtonMenuItemUI");
        popupMenuUI = UIManager.get("PopupMenuUI");

        long totalTime = (System.nanoTime() - startTime) / 1000000;
        System.out.println("MacScreenMenu.saveTrickyUISettings: it took " + totalTime + " ms");
    }

    public static void restoreTrickyUISettings() {
        long startTime = System.nanoTime();

        UIManager.put("MenuBarUI", menuBarUI);
        UIManager.put("MenuUI", menuUI);
        UIManager.put("MenuItemUI", menuItemUI);
        UIManager.put("CheckBoxMenuItemUI", checkBoxMenuItemUI);
        UIManager.put("RadioButtonMenuItemUI", radioButtonMenuItemUI);
        UIManager.put("PopupMenuUI", popupMenuUI);

        long totalTime = (System.nanoTime() - startTime) / 1000000;
        System.out.println("MacScreenMenu.restoreTrickyUISettings: it took " + totalTime + " ms");
    }
}
