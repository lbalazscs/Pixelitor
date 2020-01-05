/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.gui;

import com.bric.util.JVM;
import pixelitor.Build;
import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.Pixelitor;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.LayersContainer;
import pixelitor.menus.MenuBar;
import pixelitor.menus.help.AboutDialog;
import pixelitor.tools.Tools;
import pixelitor.tools.gui.ToolSettingsPanelContainer;
import pixelitor.tools.gui.ToolsPanel;
import pixelitor.utils.AppPreferences;

import javax.swing.*;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Taskbar;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.EAST;
import static java.awt.BorderLayout.NORTH;
import static java.awt.BorderLayout.SOUTH;
import static java.awt.BorderLayout.WEST;
import static java.awt.Desktop.Action.APP_ABOUT;
import static java.awt.Desktop.Action.APP_PREFERENCES;
import static java.awt.Desktop.Action.APP_QUIT_HANDLER;
import static java.awt.Taskbar.Feature.ICON_IMAGE;

/**
 * The main application window.
 */
public class PixelitorWindow extends JFrame {
    private Box eastPanel;
    private ToolsPanel toolsPanel;

    // normal bounds: the window bounds when it is not maximized
    private Rectangle lastNormalBounds; // the last one before maximization
    private Rectangle savedNormalBounds; // the saved one

    private PixelitorWindow() {
        super(Build.getPixelitorWindowFixTitle());

        Dimension screenSize = GUIUtils.getMaxWindowSize();

        setupWindowClosing();

        addMenus();
        addImagesArea();
        addLayersAndHistograms();
        addToolsPanel(screenSize);
        Tools.setDefaultTool();
        addStatusBar();

        setupIcons();

        GlobalEvents.init();
        GlobalEvents.addBrushSizeActions();

        AppPreferences.loadFramePosition(this, screenSize);

        if (JVM.isWindows) {
            // this is tricky code, and had problems on Linux
            setupRememberingLastBounds();
            setupFirstUnMaximization();
        }

        setVisible(true);
    }

    private void setupWindowClosing() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent we) {
                        Pixelitor.exitApp(PixelitorWindow.this);
                    }
                }
        );
    }

    private void addMenus() {
        MenuBar menuBar = new MenuBar(this);
        setJMenuBar(menuBar);

        if (JVM.isMac) {
            setupMacHandlers();
        }
    }

    private void setupMacHandlers() {
        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(APP_ABOUT)) {
            desktop.setAboutHandler(e -> AboutDialog.showDialog(this));
        }
        if (desktop.isSupported(APP_PREFERENCES)) {
            desktop.setPreferencesHandler(e -> PreferencesPanel.showInDialog());
        }
        if (desktop.isSupported(APP_QUIT_HANDLER)) {
            desktop.setQuitHandler((e, r) -> Pixelitor.exitApp(this));
        }
    }

    public void addImagesArea() {
        add(ImageArea.getUI(), CENTER);
    }

    public void removeImagesArea(JComponent c) {
        remove(c);
    }

    private void addLayersAndHistograms() {
        eastPanel = Box.createVerticalBox();
        OpenImages.addActivationListener(HistogramsPanel.INSTANCE);

        if (WorkSpace.getHistogramsVisibility()) {
            eastPanel.add(HistogramsPanel.INSTANCE);
        }
        if (WorkSpace.getLayersVisibility()) {
            eastPanel.add(LayersContainer.INSTANCE);
        }

        add(eastPanel, EAST);
    }

    private void addToolsPanel(Dimension screenSize) {
        toolsPanel = new ToolsPanel(this, screenSize);

        if (WorkSpace.getToolsVisibility()) {
            add(ToolSettingsPanelContainer.INSTANCE, NORTH);
            add(toolsPanel, WEST);
        }
    }

    private void addStatusBar() {
        if (WorkSpace.getStatusBarVisibility()) {
            add(StatusBar.INSTANCE, SOUTH);
        }
    }

    private void setupIcons() {
        URL imgURL32 = getClass().getResource("/images/pixelitor_icon32.png");
        URL imgURL48 = getClass().getResource("/images/pixelitor_icon48.png");
        URL imgURL256 = getClass().getResource("/images/pixelitor_icon256.png");

        if (imgURL32 != null && imgURL48 != null && imgURL256 != null) {
            List<Image> icons = new ArrayList<>(2);
            Image img32 = new ImageIcon(imgURL32).getImage();
            Image img48 = new ImageIcon(imgURL48).getImage();
            Image img256 = new ImageIcon(imgURL256).getImage();
            icons.add(img32);
            icons.add(img48);
            icons.add(img256);
            setIconImages(icons);

            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskBar = Taskbar.getTaskbar();
                if (taskBar.isSupported(ICON_IMAGE)) {
                    taskBar.setIconImage(img256);
                }
            }
        } else {
            String msg = "icon imgURL is null";
            Dialogs.showErrorDialog(this, "Error", msg);
        }
    }

    public static PixelitorWindow getInstance() {
        return PixelitorWindowHolder.field;
    }

    /**
     * See "Effective Java" 2nd edition, Item 71
     */
    private static class PixelitorWindowHolder {
        static final PixelitorWindow field = new PixelitorWindow();
    }

    public void setStatusBarVisibility(boolean visible, boolean revalidate) {
        if (visible) {
            add(StatusBar.INSTANCE, SOUTH);
        } else {
            remove(StatusBar.INSTANCE);
        }

        if (revalidate) {
            getContentPane().revalidate();
        }
    }

    public void setHistogramsVisibility(boolean visible, boolean revalidate) {
        if (visible) {
            assert HistogramsPanel.INSTANCE.getParent() == null;
            eastPanel.add(HistogramsPanel.INSTANCE);
            OpenImages.onActiveComp(HistogramsPanel.INSTANCE::updateFrom);
        } else {
            assert HistogramsPanel.INSTANCE.getParent() == eastPanel;
            eastPanel.remove(HistogramsPanel.INSTANCE);
        }

        if (revalidate) {
            eastPanel.revalidate();
        }
    }

    public void setLayersVisibility(boolean visible, boolean revalidate) {
        if (visible) {
            assert LayersContainer.INSTANCE.getParent() == null;
            eastPanel.add(LayersContainer.INSTANCE);
        } else {
            assert LayersContainer.INSTANCE.getParent() == eastPanel;
            eastPanel.remove(LayersContainer.INSTANCE);
        }

        if (revalidate) {
            eastPanel.revalidate();
        }
    }

    public void setToolsVisibility(boolean visible, boolean revalidate) {
        if (visible) {
            assert toolsPanel.getParent() == null;
            assert ToolSettingsPanelContainer.INSTANCE.getParent() == null;
            add(toolsPanel, WEST);
            add(ToolSettingsPanelContainer.INSTANCE, NORTH);
        } else {
            assert toolsPanel.getParent() == getContentPane();
            assert ToolSettingsPanelContainer.INSTANCE.getParent() == getContentPane();
            remove(toolsPanel);
            remove(ToolSettingsPanelContainer.INSTANCE);
        }

        if (revalidate) {
            getContentPane().revalidate();
        }
    }

    public boolean areToolsShown() {
        return toolsPanel.getParent() != null;
    }

    /**
     * Updates the app title with the name of the given {@link Composition}
     */
    public void updateTitle(Composition comp) {
        String title;
        if (comp != null) {
            title = comp.getName() + " - " + Build.getPixelitorWindowFixTitle();
        } else {
            title = Build.getPixelitorWindowFixTitle();
        }
        setTitle(title);
    }

    /**
     * Iconifies a frame; the maximized bits are not affected.
     */
    public void iconify() {
        int state = getExtendedState();

        // Set the iconified bit
        state |= Frame.ICONIFIED;

        // Iconify the frame
        setExtendedState(state);
    }

    /**
     * Deiconifies a frame; the maximized bits are not affected.
     */
    public void deiconify() {
        int state = getExtendedState();

        // Clear the iconified bit
        state &= ~Frame.ICONIFIED;

        // Deiconify the frame
        setExtendedState(state);
    }

    public void maximize() {
        setExtendedState(getExtendedState() | Frame.MAXIMIZED_BOTH);
    }

    public boolean isMaximized() {
        int extState = getExtendedState();
        return stateMaximized(extState);
    }

    private static boolean stateMaximized(int extState) {
        return (extState & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
    }

    public Rectangle getNormalBounds() {
        if (savedNormalBounds != null) {
            // this session was started and finished in maximized mode,
            // but there is a saved normal size from a previous one
            return savedNormalBounds;
        }
        return lastNormalBounds;
    }

    private void setLastNormalBounds(Rectangle normalBounds) {
        lastNormalBounds = normalBounds;
    }

    public void setSavedNormalBounds(Rectangle normalBounds) {
        savedNormalBounds = normalBounds;
    }

    private void setupRememberingLastBounds() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (!isMaximized()) {
                    setLastNormalBounds(getBounds());
                }
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                if (!isMaximized()) {
                    setLastNormalBounds(getBounds());
                }
            }
        });
    }

    private void setupFirstUnMaximization() {
        // the purpose of this is to prevent the "visual resize" problem described here:
        // https://stackoverflow.com/questions/13912692/can-i-set-jframes-normal-size-while-it-is-maximized
        // actually (with Java 8) there would be no window-resize with setSize(savedNormalBounds),
        // but a repeated content-layout would still be annoying
        addWindowStateListener(e -> {
            if (savedNormalBounds == null) {
                return;
            }
            boolean wasMaximized = stateMaximized(e.getOldState());
            boolean isMaximized = stateMaximized(e.getNewState());

            // the first time the window is un-maximized, use the saved bounds
            if (wasMaximized && !isMaximized) {
                setBounds(savedNormalBounds);
                // now the saved bounds is realized, we can forget about it
                savedNormalBounds = null;
            }
        });
    }
}

