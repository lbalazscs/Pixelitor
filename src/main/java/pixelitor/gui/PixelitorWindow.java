/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Build;
import pixelitor.Pixelitor;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.LayersContainer;
import pixelitor.menus.MenuBar;
import pixelitor.tools.Tools;
import pixelitor.tools.gui.ToolSettingsPanelContainer;
import pixelitor.tools.gui.ToolsPanel;
import pixelitor.utils.AppPreferences;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * The main application window.
 */
public class PixelitorWindow extends JFrame {
    private HistogramsPanel histogramsPanel;
    private Box verticalBoxEast;
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

        setupFrameIcons();

        GlobalEventWatch.init();
        GlobalEventWatch.addBrushSizeActions();
        GlobalEventWatch.registerKeysOnAlwaysVisibleComponent();

        AppPreferences.loadFramePosition(this, screenSize);

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

        // the purpose of this is to prevent the "visual resize" problem described here:
        // https://stackoverflow.com/questions/13912692/can-i-set-jframes-normal-size-while-it-is-maximized
        // actually (with Java 8) there would be no window-resize with setSize(savedNormalBounds),
        // but a repeated content-layout would still be annoying
        addWindowStateListener(e -> {
            boolean wasMaximized = stateMaximized(e.getOldState());
            boolean isMaximized = stateMaximized(e.getNewState());

            // the first time the window is un-maximized, use the saved bounds
            if (wasMaximized && !isMaximized) {
                if (savedNormalBounds != null) {
                    setBounds(savedNormalBounds);
                    // now the saved bounds is realized, we can forget about it
                    savedNormalBounds = null;
                }
            }
        });

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
    }

    public void addImagesArea() {
        add(ImageArea.getUI(), BorderLayout.CENTER);
    }

    public void removeImagesArea(JComponent c) {
        remove(c);
    }

    private void addLayersAndHistograms() {
        verticalBoxEast = Box.createVerticalBox();
        histogramsPanel = HistogramsPanel.INSTANCE;
        OpenComps.addActivationListener(histogramsPanel);

        if (AppPreferences.WorkSpace.getHistogramsVisibility()) {
            verticalBoxEast.add(histogramsPanel);
        }
        if (AppPreferences.WorkSpace.getLayersVisibility()) {
            verticalBoxEast.add(LayersContainer.INSTANCE);
        }

        add(verticalBoxEast, BorderLayout.EAST);
    }

    private void addToolsPanel(Dimension screenSize) {
        toolsPanel = new ToolsPanel(this, screenSize);

        if (AppPreferences.WorkSpace.getToolsVisibility()) {
            add(ToolSettingsPanelContainer.INSTANCE, BorderLayout.NORTH);
            add(toolsPanel, BorderLayout.WEST);
        }
    }

    private void addStatusBar() {
        if (AppPreferences.WorkSpace.getStatusBarVisibility()) {
            add(StatusBar.INSTANCE, BorderLayout.SOUTH);
        }
    }

    private void setupFrameIcons() {
        URL imgURL32 = getClass().getResource("/images/pixelitor_icon32.png");
        URL imgURL48 = getClass().getResource("/images/pixelitor_icon48.png");
        URL imgURL256 = getClass().getResource("/images/pixelitor_icon256.png");

        if (imgURL32 != null && imgURL48 != null && imgURL256 != null) {
            List<Image> icons = new ArrayList<>(2);
            icons.add(new ImageIcon(imgURL32).getImage());
            icons.add(new ImageIcon(imgURL48).getImage());
            icons.add(new ImageIcon(imgURL256).getImage());
            setIconImages(icons);
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

    public void setStatusBarVisibility(boolean v, boolean revalidate) {
        if (v) {
            add(StatusBar.INSTANCE, BorderLayout.SOUTH);
        } else {
            remove(StatusBar.INSTANCE);
        }
        if (revalidate) {
            getContentPane().revalidate();
        }
    }

    public void setHistogramsVisibility(boolean v, boolean revalidate) {
        if (v) {
            verticalBoxEast.add(histogramsPanel);

            OpenComps.onActiveComp(histogramsPanel::updateFromCompIfShown);
        } else {
            verticalBoxEast.remove(histogramsPanel);
        }
        if (revalidate) {
            verticalBoxEast.revalidate();
        }
    }

    public boolean areHistogramsShown() {
        return histogramsPanel.isShown();
    }

    public void setLayersVisibility(boolean v, boolean revalidate) {
        if (v) {
            verticalBoxEast.add(LayersContainer.INSTANCE);
        } else {
            verticalBoxEast.remove(LayersContainer.INSTANCE);
        }
        if (revalidate) {
            verticalBoxEast.revalidate();
        }
    }

    public void setToolsVisibility(boolean v, boolean revalidate) {
        if (v) {
            add(toolsPanel, BorderLayout.WEST);
            add(ToolSettingsPanelContainer.INSTANCE, BorderLayout.NORTH);
        } else {
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

    private static boolean stateIconified(int extState) {
        return (extState & Frame.ICONIFIED) == Frame.ICONIFIED;
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
        this.lastNormalBounds = normalBounds;
    }

    public void setSavedNormalBounds(Rectangle normalBounds) {
        this.savedNormalBounds = normalBounds;
    }
}

