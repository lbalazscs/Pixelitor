/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import static pixelitor.utils.ImageUtils.findImageURL;
import static pixelitor.utils.Texts.i18n;

/**
 * The main application window.
 */
public class PixelitorWindow extends JFrame {
    private static final String BASE_TITLE = calcBaseTitle();

    private JPanel sidePanel; // layers and histograms
    private ToolsPanel toolsPanel;
    private final WorkSpace workSpace;

    // normal bounds: the window bounds when it is not maximized
    private Rectangle lastNormalBounds; // the last one before maximization
    private Rectangle savedNormalBounds; // the saved one

    private PixelitorWindow() {
        super(BASE_TITLE);

        workSpace = new WorkSpace();

        Dimension screenSize = Screens.getMaxWindowSize();

        addMenuBar();
        addImageArea();
        addSidePanel();
        addToolsPanel(screenSize);
        Tools.setDefaultTool();
        addStatusBar();

        initIcons();

        GlobalEvents.init();

        initWindow(screenSize);
    }

    private void initWindow(Dimension screenSize) {
        AppPreferences.loadFramePreferences(this, screenSize);
        if (JVM.isWindows) {
            // this is tricky code, and had problems on Linux
            setupRememberingLastBounds();
            setupFirstUnMaximization();
        }
        configureWindowEvents();
        setVisible(true);
    }

    public void resetDefaultWorkspace() {
        workSpace.restoreDefaults(this);
    }

    private void configureWindowEvents() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(
            new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent we) {
                    Pixelitor.exitApp(PixelitorWindow.this);
                }

                @Override
                public void windowActivated(WindowEvent e) {
                    // ignore activation events from closed dialogs
                    if (e.getOppositeWindow() == null) {
                        Views.appWindowActivated();
                    }
                }
            }
        );
    }

    private void addMenuBar() {
        setJMenuBar(new MenuBar(this));

        setupMacHandlers();
    }

    private void setupMacHandlers() {
        if (!Desktop.isDesktopSupported()) {
            return;
        }
        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(APP_ABOUT)) {
            desktop.setAboutHandler(e -> AboutDialog.showDialog(i18n("about")));
        }
        if (desktop.isSupported(APP_PREFERENCES)) {
            desktop.setPreferencesHandler(e -> PreferencesPanel.showInDialog());
        }
        if (desktop.isSupported(APP_QUIT_HANDLER)) {
            desktop.setQuitHandler((e, r) -> Pixelitor.exitApp(this));
        }
    }

    public void addImageArea() {
        add(ImageArea.getUI(), CENTER);
    }

    public void removeImageArea(JComponent c) {
        remove(c);
    }

    private void addSidePanel() {
        sidePanel = new JPanel(new BorderLayout());
        HistogramsPanel histogramsPanel = HistogramsPanel.getInstance();
        Views.addActivationListener(histogramsPanel);

        if (workSpace.areHistogramsVisible()) {
            sidePanel.add(histogramsPanel, NORTH);
        }
        if (workSpace.areLayersVisible()) {
            sidePanel.add(LayersContainer.get(), CENTER);
        }

        add(sidePanel, EAST);
    }

    private void addToolsPanel(Dimension screenSize) {
        toolsPanel = new ToolsPanel(this, screenSize);

        if (workSpace.areToolsVisible()) {
            add(ToolSettingsPanelContainer.get(), NORTH);
            add(toolsPanel, WEST);
        }
    }

    private void addStatusBar() {
        if (workSpace.isStatusBarVisible()) {
            add(StatusBar.get(), SOUTH);
        }
    }

    private void initIcons() {
        URL imgURL32 = findImageURL("pixelitor_icon32.png");
        URL imgURL48 = findImageURL("pixelitor_icon48.png");
        URL imgURL256 = findImageURL("pixelitor_icon256.png");

        List<Image> icons = new ArrayList<>(2);
        icons.add(new ImageIcon(imgURL32).getImage());
        icons.add(new ImageIcon(imgURL48).getImage());
        Image img256 = new ImageIcon(imgURL256).getImage();
        icons.add(img256);
        setIconImages(icons);

        setTaskbarIcon(img256);
    }

    private static void setTaskbarIcon(Image image) {
        if (Taskbar.isTaskbarSupported()) {
            Taskbar taskBar = Taskbar.getTaskbar();
            if (taskBar.isSupported(ICON_IMAGE)) {
                taskBar.setIconImage(image);
            }
        }
    }

    /**
     * Returns the single instance of the main window.
     */
    public static PixelitorWindow get() {
        return PixelitorWindowHolder.field;
    }

    /**
     * Singleton holder for the main window instance.
     * Uses initialization-on-demand holder idiom for thread-safe lazy initialization.
     */
    private static class PixelitorWindowHolder {
        static final PixelitorWindow field = new PixelitorWindow();
    }

    public void setStatusBarVisible(boolean visible, boolean revalidate) {
        if (visible) {
            add(StatusBar.get(), SOUTH);
        } else {
            remove(StatusBar.get());
        }

        if (revalidate) {
            getContentPane().revalidate();
        }
    }

    public void setHistogramsVisible(boolean visible, boolean revalidate) {
        HistogramsPanel histogramsPanel = HistogramsPanel.getInstance();
        if (visible) {
            assert !HistogramsPanel.isShown();
            sidePanel.add(histogramsPanel, NORTH);
            HistogramsPanel.updateFromActiveComp();
        } else {
            assert histogramsPanel.getParent() == sidePanel;
            sidePanel.remove(histogramsPanel);
        }

        if (revalidate) {
            sidePanel.revalidate();
        }
    }

    public void setLayersVisible(boolean visible, boolean revalidate) {
        if (visible) {
            assert LayersContainer.parentIs(null);
            sidePanel.add(LayersContainer.get(), CENTER);
        } else {
            assert LayersContainer.parentIs(sidePanel);
            sidePanel.remove(LayersContainer.get());
        }

        if (revalidate) {
            sidePanel.revalidate();
        }
    }

    public void setToolsVisible(boolean visible, boolean revalidate) {
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

    /**
     * Calculates the base title of the app, which is appended to
     * composition names when files are open.
     */
    private static String calcBaseTitle() {
        String baseTitle = "Pixelitor " + Pixelitor.VERSION;
        if (AppMode.isDevelopment()) {
            baseTitle += " DEVELOPMENT " + System.getProperty("java.version");
        }
        return baseTitle;
    }

    /**
     * Updates the app title with the name of the given {@link Composition}
     */
    public void updateTitle(Composition comp) {
        String title;
        if (comp != null) {
            title = comp.calcWindowTitle() + " - " + BASE_TITLE;
        } else {
            title = BASE_TITLE;
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

    public WorkSpace getWorkSpace() {
        return workSpace;
    }
}

