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

package pixelitor.gui;

import com.bric.util.JVM;
import pixelitor.AppMode;
import pixelitor.Composition;
import pixelitor.Pixelitor;
import pixelitor.Views;
import pixelitor.menus.MenuBar;
import pixelitor.menus.help.AboutDialog;
import pixelitor.tools.Tools;
import pixelitor.utils.AppPreferences;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.net.URL;
import java.util.List;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.EAST;
import static java.awt.Desktop.Action.*;
import static java.awt.Taskbar.Feature.ICON_IMAGE;
import static pixelitor.utils.ImageUtils.findImageURL;
import static pixelitor.utils.Texts.i18n;
import static pixelitor.utils.Threads.callInfo;
import static pixelitor.utils.Threads.calledOnEDT;

/**
 * The main application window.
 */
public class PixelitorWindow extends JFrame {
    private static final String BASE_TITLE = calcBaseTitle();

    private JPanel sidePanel; // layers and histograms
    private final WorkSpace workSpace;

    // normal bounds: the window bounds when it is not maximized
    private Rectangle lastNormalBounds; // the last one before maximization
    private Rectangle savedNormalBounds; // the saved one

    private PixelitorWindow() {
        super(BASE_TITLE);

        workSpace = new WorkSpace(this);

        AppPreferences.loadFramePreferences(this);

        addMenuBar();
        addImageArea();
        initPanels();
        Tools.setDefaultTool();

        initIcons();

        GlobalEvents.init();

        if (JVM.isWindows) {
            // this is tricky code that had problems on Linux
            setupRememberingLastBounds();
            setupFirstUnMaximization();
        }
        configureWindowEvents();
        workSpace.setFrameInitialized(true);
        setVisible(true);
    }

    public void resetDefaultWorkspace() {
        workSpace.restoreDefaults();
    }

    private void configureWindowEvents() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(
            new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent we) {
                    Pixelitor.requestExit(PixelitorWindow.this);
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
            desktop.setAboutHandler(_ -> AboutDialog.showDialog(i18n("about")));
        }
        if (desktop.isSupported(APP_PREFERENCES)) {
            desktop.setPreferencesHandler(_ -> PreferencesPanel.showInDialog());
        }
        if (desktop.isSupported(APP_QUIT_HANDLER)) {
            desktop.setQuitHandler((_, _) -> Pixelitor.requestExit(this));
        }
    }

    public void addImageArea() {
        add(ImageArea.getUI(), CENTER);
    }

    public void removeImageArea() {
        remove(ImageArea.getUI());
    }

    private void initIcons() {
        URL imgURL32 = findImageURL("pixelitor_icon32.png");
        URL imgURL48 = findImageURL("pixelitor_icon48.png");
        URL imgURL256 = findImageURL("pixelitor_icon256.png");

        Image img256 = new ImageIcon(imgURL256).getImage();
        setTaskbarIcon(img256);

        setIconImages(List.of(
            new ImageIcon(imgURL32).getImage(),
            new ImageIcon(imgURL48).getImage(),
            img256
        ));
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
        return PixelitorWindowHolder.INSTANCE;
    }

    /**
     * Singleton holder for the main window instance.
     * Uses initialization-on-demand holder idiom for thread-safe lazy initialization.
     */
    private static class PixelitorWindowHolder {
        static final PixelitorWindow INSTANCE = new PixelitorWindow();
    }

    private void initPanels() {
        sidePanel = new JPanel(new BorderLayout());

        for (AppPanel panel : AppPanel.PANELS) {
            if (workSpace.isVisible(panel)) {
                setPanelVisible(panel, true, false);
            } else {
                // initialize the tools panel even if it is hidden at startup
                if (panel == AppPanel.TOOLS) {
                    panel.getComponent();
                }
            }
        }

        add(sidePanel, EAST);
        getContentPane().revalidate();
    }

    public void setPanelVisible(AppPanel panel, boolean visible, boolean revalidate) {
        assert calledOnEDT() : callInfo();

        JComponent comp = panel.getComponent();
        Container target = panel.getTarget().resolveContainer(this);

        if (visible) {
            assert comp.getParent() == null : "Panel " + panel + " is already attached";
            target.add(comp, panel.getLayoutConstraint());
        } else {
            assert comp.getParent() == target : "Panel " + panel + " is not attached to its expected target";
            target.remove(comp);
        }

        if (revalidate) {
            target.revalidate();
            target.repaint();
        }
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
     * Iconifies the frame without affecting the maximized state.
     */
    public void iconify() {
        int state = getExtendedState();
        // set the iconified bit
        state |= Frame.ICONIFIED;
        setExtendedState(state);
    }

    /**
     * De-iconifies the frame without affecting the maximized state.
     */
    public void deiconify() {
        int state = getExtendedState();
        // clear the iconified bit
        state &= ~Frame.ICONIFIED;
        setExtendedState(state);
    }

    public void maximize() {
        setExtendedState(getExtendedState() | Frame.MAXIMIZED_BOTH);
    }

    public boolean isMaximized() {
        return isStateMaximized(getExtendedState());
    }

    private static boolean isStateMaximized(int extState) {
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
            boolean wasMaximized = isStateMaximized(e.getOldState());
            boolean isMaximized = isStateMaximized(e.getNewState());

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

    public JPanel getSidePanel() {
        return sidePanel;
    }
}
