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

package pixelitor.gui;

import com.bric.util.JVM;
import pixelitor.AppMode;
import pixelitor.Composition;
import pixelitor.Pixelitor;
import pixelitor.Views;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.Screens;
import pixelitor.layers.LayersContainer;
import pixelitor.menus.MenuBar;
import pixelitor.menus.help.AboutDialog;
import pixelitor.tools.Tools;
import pixelitor.tools.gui.ToolSettingsPanelContainer;
import pixelitor.tools.gui.ToolsPanel;
import pixelitor.utils.AppPreferences;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
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
import static pixelitor.utils.Texts.i18n;

/**
 * The main application window.
 */
public class PixelitorWindow extends JFrame {
    private static final String FIX_TITLE = calcFixTitle();

    private Box eastPanel;
    private ToolsPanel toolsPanel;

    // normal bounds: the window bounds when it is not maximized
    private Rectangle lastNormalBounds; // the last one before maximization
    private Rectangle savedNormalBounds; // the saved one

    private PixelitorWindow() {
        super(FIX_TITLE);

        Dimension screenSize = Screens.getMaxWindowSize();

        setupWindowEvents();

        addMenus();
        addImageArea();
        addLayersAndHistograms();
        addToolsPanel(screenSize);
        Tools.setDefaultTool();
        addStatusBar();

        setupIcons();

        GlobalEvents.init();

        AppPreferences.loadFramePosition(this, screenSize);

        if (JVM.isWindows) {
            // this is tricky code, and had problems on Linux
            setupRememberingLastBounds();
            setupFirstUnMaximization();
        }

        setVisible(true);
    }

    private void setupWindowEvents() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(
            new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent we) {
                    Pixelitor.warnAndExit(PixelitorWindow.this);
                }

                @Override
                public void windowActivated(WindowEvent e) {
                    // ignore activation events from closed dialogs
                    if (e.getOppositeWindow() == null) {
                        Views.appActivated();
                    }
                }
            }
        );
    }

    private void addMenus() {
        setJMenuBar(new MenuBar(this));

        setupMacHandlers();
    }

    private void setupMacHandlers() {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(APP_ABOUT)) {
                desktop.setAboutHandler(e -> AboutDialog.showDialog(i18n("about")));
            }
            if (desktop.isSupported(APP_PREFERENCES)) {
                desktop.setPreferencesHandler(e -> PreferencesPanel.showInDialog());
            }
            if (desktop.isSupported(APP_QUIT_HANDLER)) {
                desktop.setQuitHandler((e, r) -> Pixelitor.warnAndExit(this));
            }
        }
    }

    public void addImageArea() {
        add(ImageArea.getUI(), CENTER);
    }

    public void removeImageArea(JComponent c) {
        remove(c);
    }

    private void addLayersAndHistograms() {
        eastPanel = Box.createVerticalBox();
        HistogramsPanel histogramsPanel = HistogramsPanel.get();
        Views.addActivationListener(histogramsPanel);

        if (WorkSpace.getHistogramsVisibility()) {
            eastPanel.add(histogramsPanel);
        }
        if (WorkSpace.getLayersVisibility()) {
            eastPanel.add(LayersContainer.get());
        }

        add(eastPanel, EAST);
    }

    private void addToolsPanel(Dimension screenSize) {
        toolsPanel = new ToolsPanel(this, screenSize);

        if (WorkSpace.getToolsVisibility()) {
            add(ToolSettingsPanelContainer.get(), NORTH);
            add(toolsPanel, WEST);
        }
    }

    private void addStatusBar() {
        if (WorkSpace.getStatusBarVisibility()) {
            add(StatusBar.get(), SOUTH);
        }
    }

    private void setupIcons() {
        var clazz = getClass();
        URL imgURL32 = clazz.getResource("/images/pixelitor_icon32.png");
        URL imgURL48 = clazz.getResource("/images/pixelitor_icon48.png");
        URL imgURL256 = clazz.getResource("/images/pixelitor_icon256.png");

        if (imgURL32 != null && imgURL48 != null && imgURL256 != null) {
            List<Image> icons = new ArrayList<>(2);
            icons.add(new ImageIcon(imgURL32).getImage());
            icons.add(new ImageIcon(imgURL48).getImage());
            Image img256 = new ImageIcon(imgURL256).getImage();
            icons.add(img256);
            setIconImages(icons);

            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskBar = Taskbar.getTaskbar();
                if (taskBar.isSupported(ICON_IMAGE)) {
                    taskBar.setIconImage(img256);
                }
            }
        } else {
            Dialogs.showErrorDialog(this, "Error",
                "icon imgURL is null");
        }
    }

    /**
     * Returns the single instance of the main window.
     */
    public static PixelitorWindow get() {
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
            add(StatusBar.get(), SOUTH);
        } else {
            remove(StatusBar.get());
        }

        if (revalidate) {
            getContentPane().revalidate();
        }
    }

    public void setHistogramsVisibility(boolean visible, boolean revalidate) {
        HistogramsPanel histogramsPanel = HistogramsPanel.get();
        if (visible) {
            assert !HistogramsPanel.isShown();
            eastPanel.add(histogramsPanel);
            HistogramsPanel.updateFromActiveComp();
        } else {
            assert histogramsPanel.getParent() == eastPanel;
            eastPanel.remove(histogramsPanel);
        }

        if (revalidate) {
            eastPanel.revalidate();
        }
    }

    public void setLayersVisibility(boolean visible, boolean revalidate) {
        if (visible) {
            assert LayersContainer.parentIs(null);
            eastPanel.add(LayersContainer.get());
        } else {
            assert LayersContainer.parentIs(eastPanel);
            eastPanel.remove(LayersContainer.get());
        }

        if (revalidate) {
            eastPanel.revalidate();
        }
    }

    public void setToolsVisibility(boolean visible, boolean revalidate) {
        var toolSettingsPanel = ToolSettingsPanelContainer.get();
        if (visible) {
            assert toolsPanel.getParent() == null;
            assert toolSettingsPanel.getParent() == null;
            add(toolsPanel, WEST);
            add(toolSettingsPanel, NORTH);
        } else {
            assert toolsPanel.getParent() == getContentPane();
            assert toolSettingsPanel.getParent() == getContentPane();
            remove(toolsPanel);
            remove(toolSettingsPanel);
        }

        if (revalidate) {
            getContentPane().revalidate();
        }
    }

    public boolean areToolsShown() {
        return toolsPanel.getParent() != null;
    }

    private static String calcFixTitle() {
        String fixTitle = "Pixelitor " + Pixelitor.VERSION_NUMBER;
        if (AppMode.isDevelopment()) {
            fixTitle += " DEVELOPMENT " + System.getProperty("java.version");
        }
        return fixTitle;
    }

    /**
     * Updates the app title with the name of the given {@link Composition}
     */
    public void updateTitle(Composition comp) {
        String title;
        if (comp != null) {
            title = comp.calcTitle() + " - " + FIX_TITLE;
        } else {
            title = FIX_TITLE;
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
        return stateMaximized(getExtendedState());
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

    public AffineTransform getHiDPIScaling() {
        return getGraphicsConfiguration().getDefaultTransform();
    }
}

