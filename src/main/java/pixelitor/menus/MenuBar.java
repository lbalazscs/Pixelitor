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

package pixelitor.menus;

import com.bric.util.JVM;
import com.jhlabs.composite.MultiplyComposite;
import pixelitor.AppLogic;
import pixelitor.Build;
import pixelitor.Composition;
import pixelitor.NewImage;
import pixelitor.TipsOfTheDay;
import pixelitor.automate.AutoPaint;
import pixelitor.automate.BatchFilterWizard;
import pixelitor.automate.BatchResize;
import pixelitor.colors.FgBgColors;
import pixelitor.colors.palette.ColorSwatchClickHandler;
import pixelitor.colors.palette.FullPalette;
import pixelitor.colors.palette.VariationsPanel;
import pixelitor.filters.*;
import pixelitor.filters.animation.TweenWizard;
import pixelitor.filters.comp.EnlargeCanvas;
import pixelitor.filters.comp.Flip;
import pixelitor.filters.comp.Rotate;
import pixelitor.filters.convolve.Convolve;
import pixelitor.filters.gui.ResizePanel;
import pixelitor.filters.jhlabsproxies.*;
import pixelitor.filters.levels.Levels;
import pixelitor.filters.lookup.ColorBalance;
import pixelitor.filters.lookup.Luminosity;
import pixelitor.filters.painters.TextFilter;
import pixelitor.gui.Desktop;
import pixelitor.gui.HistogramsPanel;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.Navigator;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.PerformanceTestingDialog;
import pixelitor.history.AddToHistory;
import pixelitor.history.History;
import pixelitor.io.FileChoosers;
import pixelitor.io.OpenSaveManager;
import pixelitor.io.OptimizedJpegSavePanel;
import pixelitor.layers.AddAdjLayerAction;
import pixelitor.layers.AddNewLayerAction;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.DeleteActiveLayerAction;
import pixelitor.layers.DuplicateLayerAction;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMask;
import pixelitor.layers.LayerMaskAddType;
import pixelitor.layers.LayerMoveAction;
import pixelitor.layers.MaskFromColorRangePanel;
import pixelitor.layers.MaskViewMode;
import pixelitor.layers.TextLayer;
import pixelitor.menus.edit.CopyAction;
import pixelitor.menus.edit.CopySource;
import pixelitor.menus.edit.FadeMenuItem;
import pixelitor.menus.edit.PasteAction;
import pixelitor.menus.edit.PasteDestination;
import pixelitor.menus.file.AnimGifExport;
import pixelitor.menus.file.OpenRasterExportPanel;
import pixelitor.menus.file.RecentFilesMenu;
import pixelitor.menus.file.ScreenCaptureAction;
import pixelitor.menus.help.AboutDialog;
import pixelitor.menus.help.UpdatesCheck;
import pixelitor.menus.view.ShowHideAllAction;
import pixelitor.menus.view.ShowHideHistogramsAction;
import pixelitor.menus.view.ShowHideLayersAction;
import pixelitor.menus.view.ShowHideStatusBarAction;
import pixelitor.menus.view.ShowHideToolsAction;
import pixelitor.menus.view.ZoomMenu;
import pixelitor.selection.SelectionActions;
import pixelitor.tools.brushes.CopyBrush;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.FilterCreator;
import pixelitor.utils.Messages;
import pixelitor.utils.Tests3x3;
import pixelitor.utils.UpdateGUI;
import pixelitor.utils.Utils;
import pixelitor.utils.debug.AppNode;
import pixelitor.utils.test.Events;
import pixelitor.utils.test.FilterTests;
import pixelitor.utils.test.RandomGUITest;
import pixelitor.utils.test.SplashImageCreator;
import pixelitor.utils.test.ToolTests;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementFactory;

import static pixelitor.Composition.ImageChangeActions.FULL;
import static pixelitor.colors.FillType.BACKGROUND;
import static pixelitor.colors.FillType.FOREGROUND;
import static pixelitor.colors.FillType.TRANSPARENT;
import static pixelitor.filters.comp.Flip.Direction.HORIZONTAL;
import static pixelitor.filters.comp.Flip.Direction.VERTICAL;
import static pixelitor.filters.comp.Rotate.SpecialAngle.ANGLE_180;
import static pixelitor.filters.comp.Rotate.SpecialAngle.ANGLE_270;
import static pixelitor.filters.comp.Rotate.SpecialAngle.ANGLE_90;
import static pixelitor.filters.jhlabsproxies.JHMotionBlur.Mode.MOTION_BLUR;
import static pixelitor.filters.jhlabsproxies.JHMotionBlur.Mode.SPIN_ZOOM_BLUR;
import static pixelitor.gui.ImageComponents.getActiveCompOrNull;
import static pixelitor.gui.ImageComponents.getActiveLayerOrNull;
import static pixelitor.menus.EnabledIf.ACTION_ENABLED;
import static pixelitor.menus.EnabledIf.CAN_REPEAT_OPERATION;
import static pixelitor.menus.EnabledIf.REDO_POSSIBLE;
import static pixelitor.menus.EnabledIf.UNDO_POSSIBLE;
import static pixelitor.menus.MenuAction.AllowedLayerType.HAS_LAYER_MASK;
import static pixelitor.menus.MenuAction.AllowedLayerType.IS_TEXT_LAYER;

/**
 * The menu bar of the app
 */
public class MenuBar extends JMenuBar {
    public static final int MENU_SHORTCUT_KEY_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    public static final KeyStroke CTRL_1 = KeyStroke.getKeyStroke('1', MENU_SHORTCUT_KEY_MASK);
    public static final KeyStroke CTRL_2 = KeyStroke.getKeyStroke('2', MENU_SHORTCUT_KEY_MASK);
    public static final KeyStroke CTRL_3 = KeyStroke.getKeyStroke('3', MENU_SHORTCUT_KEY_MASK);
    public static final KeyStroke CTRL_4 = KeyStroke.getKeyStroke('4', MENU_SHORTCUT_KEY_MASK);

    private static final KeyStroke CTRL_B = KeyStroke.getKeyStroke('B', MENU_SHORTCUT_KEY_MASK);
    private static final KeyStroke CTRL_C = KeyStroke.getKeyStroke('C', MENU_SHORTCUT_KEY_MASK);
    private static final KeyStroke CTRL_D = KeyStroke.getKeyStroke('D', MENU_SHORTCUT_KEY_MASK);
    private static final KeyStroke CTRL_E = KeyStroke.getKeyStroke('E', MENU_SHORTCUT_KEY_MASK);
    private static final KeyStroke CTRL_F = KeyStroke.getKeyStroke('F', MENU_SHORTCUT_KEY_MASK);
//    private static final KeyStroke CTRL_G = KeyStroke.getKeyStroke('G', MENU_SHORTCUT_KEY_MASK);
    private static final KeyStroke CTRL_H = KeyStroke.getKeyStroke('H', MENU_SHORTCUT_KEY_MASK);
    private static final KeyStroke CTRL_I = KeyStroke.getKeyStroke('I', MENU_SHORTCUT_KEY_MASK);
    private static final KeyStroke CTRL_J = KeyStroke.getKeyStroke('J', MENU_SHORTCUT_KEY_MASK);
    //    private static final KeyStroke CTRL_K = KeyStroke.getKeyStroke('K', MENU_SHORTCUT_KEY_MASK);
    private static final KeyStroke CTRL_L = KeyStroke.getKeyStroke('L', MENU_SHORTCUT_KEY_MASK);
    private static final KeyStroke CTRL_M = KeyStroke.getKeyStroke('M', MENU_SHORTCUT_KEY_MASK);
    private static final KeyStroke CTRL_N = KeyStroke.getKeyStroke('N', MENU_SHORTCUT_KEY_MASK);
    private static final KeyStroke CTRL_O = KeyStroke.getKeyStroke('O', MENU_SHORTCUT_KEY_MASK);
    private static final KeyStroke CTRL_R = KeyStroke.getKeyStroke('R', MENU_SHORTCUT_KEY_MASK);
    private static final KeyStroke CTRL_S = KeyStroke.getKeyStroke('S', MENU_SHORTCUT_KEY_MASK);
    private static final KeyStroke CTRL_T = KeyStroke.getKeyStroke('T', MENU_SHORTCUT_KEY_MASK);
    private static final KeyStroke CTRL_U = KeyStroke.getKeyStroke('U', MENU_SHORTCUT_KEY_MASK);
    private static final KeyStroke CTRL_V = KeyStroke.getKeyStroke('V', MENU_SHORTCUT_KEY_MASK);
    private static final KeyStroke CTRL_Z = KeyStroke.getKeyStroke('Z', MENU_SHORTCUT_KEY_MASK);
    private static final KeyStroke CTRL_W = KeyStroke.getKeyStroke('W', MENU_SHORTCUT_KEY_MASK);

    private static final KeyStroke CTRL_SHIFT_S = KeyStroke.getKeyStroke('S', MENU_SHORTCUT_KEY_MASK + InputEvent.SHIFT_MASK);
    private static final KeyStroke CTRL_ALT_W = KeyStroke.getKeyStroke('W', MENU_SHORTCUT_KEY_MASK | InputEvent.ALT_MASK);
    private static final KeyStroke CTRL_SHIFT_Z = KeyStroke.getKeyStroke('Z', InputEvent.SHIFT_MASK + MENU_SHORTCUT_KEY_MASK);
    private static final KeyStroke CTRL_SHIFT_F = KeyStroke.getKeyStroke('F', MENU_SHORTCUT_KEY_MASK + InputEvent.SHIFT_MASK);
    private static final KeyStroke CTRL_SHIFT_C = KeyStroke.getKeyStroke('C', MENU_SHORTCUT_KEY_MASK + InputEvent.SHIFT_MASK);
    private static final KeyStroke CTRL_SHIFT_V = KeyStroke.getKeyStroke('V', MENU_SHORTCUT_KEY_MASK + InputEvent.SHIFT_MASK);
    private static final KeyStroke CTRL_ALT_V = KeyStroke.getKeyStroke('V', MENU_SHORTCUT_KEY_MASK + InputEvent.ALT_MASK);
    private static final KeyStroke CTRL_ALT_I = KeyStroke.getKeyStroke('I', MENU_SHORTCUT_KEY_MASK | InputEvent.ALT_MASK);
    private static final KeyStroke ALT_BACKSPACE = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, InputEvent.ALT_MASK);
    private static final KeyStroke CTRL_BACKSPACE = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, MENU_SHORTCUT_KEY_MASK);
    private static final KeyStroke CTRL_SHIFT_I = KeyStroke.getKeyStroke('I', MENU_SHORTCUT_KEY_MASK + InputEvent.SHIFT_MASK);
    private static final KeyStroke T = KeyStroke.getKeyStroke('T');
    private static final KeyStroke CTRL_SHIFT_ALT_E = KeyStroke.getKeyStroke('E', MENU_SHORTCUT_KEY_MASK + InputEvent.ALT_MASK + InputEvent.SHIFT_MASK);
    private static final KeyStroke CTRL_CLOSE_BRACKET = KeyStroke.getKeyStroke(']', MENU_SHORTCUT_KEY_MASK);
    private static final KeyStroke CTRL_OPEN_BRACKET = KeyStroke.getKeyStroke('[', MENU_SHORTCUT_KEY_MASK);
    private static final KeyStroke CTRL_SHIFT_CLOSE_BRACKET = KeyStroke.getKeyStroke(']', MENU_SHORTCUT_KEY_MASK + InputEvent.SHIFT_MASK);
    private static final KeyStroke CTRL_SHIFT_OPEN_BRACKET = KeyStroke.getKeyStroke('[', MENU_SHORTCUT_KEY_MASK + InputEvent.SHIFT_MASK);
    private static final KeyStroke ALT_CLOSE_BRACKET = KeyStroke.getKeyStroke(']', InputEvent.ALT_MASK);
    private static final KeyStroke ALT_OPEN_BRACKET = KeyStroke.getKeyStroke('[', InputEvent.ALT_MASK);
    private static final KeyStroke F5 = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);
    private static final KeyStroke F6 = KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0);
    private static final KeyStroke F7 = KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0);
    private static final KeyStroke TAB = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
    private static final KeyStroke CTRL_ALT_R = KeyStroke.getKeyStroke('R', MENU_SHORTCUT_KEY_MASK + InputEvent.ALT_MASK);

    public MenuBar(PixelitorWindow pw) {
        this.add(createFileMenu(pw));
        this.add(createEditMenu());
        this.add(createLayerMenu(pw));
        this.add(createSelectMenu());
        this.add(createImageMenu());
        this.add(createColorMenu());
        this.add(createFilterMenu());
        this.add(createViewMenu(pw));

        if (Build.CURRENT != Build.FINAL) {
            this.add(createDevelopMenu(pw));
        }

        this.add(createHelpMenu(pw));
    }

    private static JMenu createFileMenu(PixelitorWindow pw) {
        PMenu fileMenu = new PMenu("File", 'F');

        // new image
        fileMenu.buildAction(NewImage.getAction())
                .alwaysEnabled()
                .withKey(CTRL_N)
                .add();

        fileMenu.buildAction(new MenuAction("Open...") {
            @Override
            public void onClick() {
                FileChoosers.open();
            }
        }).alwaysEnabled().withKey(CTRL_O).add();

        // recent files
        JMenu recentFiles = RecentFilesMenu.getInstance();
        fileMenu.add(recentFiles);

        fileMenu.addSeparator();

        fileMenu.addActionWithKey(new MenuAction("Save") {
            @Override
            public void onClick() {
                OpenSaveManager.save(false);
            }
        }, CTRL_S);

        fileMenu.addActionWithKey(new MenuAction("Save As...") {
            @Override
            public void onClick() {
                OpenSaveManager.save(true);
            }
        }, CTRL_SHIFT_S);

        fileMenu.addAction(new MenuAction("Export Optimized JPEG...") {
            @Override
            public void onClick() {
                BufferedImage image = ImageComponents.getActiveCompositeImageOrNull();
                OptimizedJpegSavePanel.showInDialog(image, pw);
            }
        });

        fileMenu.addAction(new MenuAction("Export OpenRaster...") {
            @Override
            public void onClick() {
                OpenRasterExportPanel.showInDialog(pw);
            }
        });

        fileMenu.addSeparator();

        fileMenu.addAction(new MenuAction("Export Layer Animation...") {
            @Override
            public void onClick() {
                AnimGifExport.start(pw);
            }
        });

        fileMenu.addAction(new ImageLayerAction("Export Tweening Animation") {
            @Override
            protected void process(ImageLayer layer) {
                new TweenWizard(layer).start(pw);
            }
        });

        fileMenu.addSeparator();

        // close
        fileMenu.addActionWithKey(new MenuAction("Close") {
            @Override
            public void onClick() {
                OpenSaveManager.warnAndCloseImage(ImageComponents.getActiveIC());
            }
        }, CTRL_W);

        // close all
        fileMenu.addActionWithKey(new MenuAction("Close All") {
            @Override
            public void onClick() {
                OpenSaveManager.warnAndCloseAllImages();
            }
        }, CTRL_ALT_W);

        // reload
        fileMenu.addActionWithKey(new MenuAction("Reload") {
            @Override
            public void onClick() {
                ImageComponents.reloadActiveFromFile();
            }
        }, F5);

        fileMenu.addSeparator();

        fileMenu.add(createAutomateSubmenu(pw));

        if (!JVM.isMac) {
            fileMenu.addAlwaysEnabledAction(new ScreenCaptureAction());
        }

        fileMenu.addSeparator();

        // exit
        String exitName = JVM.isMac ? "Quit" : "Exit";
        fileMenu.addAlwaysEnabledAction(new MenuAction(exitName) {
            @Override
            public void onClick() {
                AppLogic.exitApp(pw);
            }
        });

        return fileMenu;
    }

    private static JMenu createAutomateSubmenu(PixelitorWindow pw) {
        PMenu sub = new PMenu("Automate");

        sub.addAlwaysEnabledAction(new MenuAction("Batch Resize...") {
            @Override
            public void onClick() {
                BatchResize.start();
            }
        });

        sub.buildAction(new ImageLayerAction("Batch Filter") {
            @Override
            protected void process(ImageLayer layer) {
                new BatchFilterWizard(layer).start(pw);
            }
        }).add();

        sub.addAction(new MenuAction("Export Layers to PNG...") {
            @Override
            public void onClick() {
                OpenSaveManager.exportLayersToPNG();
            }
        });

        sub.addAction(new ImageLayerAction("Auto Paint") {
            @Override
            protected void process(ImageLayer layer) {
                AutoPaint.showDialog(layer);
            }
        });

        return sub;
    }

    private static JMenu createEditMenu() {
        PMenu editMenu = new PMenu("Edit", 'E');

        // last op
        editMenu.buildAction(RepeatLast.INSTANCE)
                .enableIf(CAN_REPEAT_OPERATION)
                .withKey(CTRL_F)
                .add();

        editMenu.addSeparator();

        // undo
        editMenu.buildAction(History.UNDO_ACTION)
                .enableIf(UNDO_POSSIBLE)
                .withKey(CTRL_Z)
                .add();

        // redo
        editMenu.buildAction(History.REDO_ACTION)
                .enableIf(REDO_POSSIBLE)
                .withKey(CTRL_SHIFT_Z)
                .add();

        // fade
        FadeMenuItem.INSTANCE.setAccelerator(CTRL_SHIFT_F);
        editMenu.add(FadeMenuItem.INSTANCE);

        editMenu.addSeparator();

        // copy
        editMenu.addActionWithKey(new CopyAction(CopySource.LAYER), CTRL_C);
        editMenu.addActionWithKey(new CopyAction(CopySource.COMPOSITE), CTRL_SHIFT_C);
        // paste
        editMenu.buildAction(new PasteAction(PasteDestination.NEW_IMAGE)).alwaysEnabled().withKey(CTRL_V).add();
        editMenu.addActionWithKey(new PasteAction(PasteDestination.NEW_LAYER), CTRL_SHIFT_V);
        editMenu.addActionWithKey(new PasteAction(PasteDestination.MASK), CTRL_ALT_V);

        editMenu.addSeparator();

        // preferences
        Action preferencesAction = new MenuAction("Preferences...") {
            @Override
            public void onClick() {
                AppPreferences.Panel.showInDialog();
            }
        };
        editMenu.add(preferencesAction);

        return editMenu;
    }

    private static JMenu createLayerMenu(PixelitorWindow pw) {
        PMenu layersMenu = new PMenu("Layer", 'L');

        layersMenu.add(AddNewLayerAction.INSTANCE);
        layersMenu.add(DeleteActiveLayerAction.INSTANCE);
        layersMenu.addActionWithKey(DuplicateLayerAction.INSTANCE, CTRL_J);

        layersMenu.addSeparator();

        layersMenu.addActionWithKey(new MenuAction("Merge Down") {
            @Override
            public void onClick() {
                getActiveCompOrNull().mergeDown(UpdateGUI.YES);
            }
        }, CTRL_E);

        layersMenu.addAction(new MenuAction("Flatten Image") {
            @Override
            public void onClick() {
                getActiveCompOrNull().flattenImage(UpdateGUI.YES);
            }
        });

        layersMenu.addActionWithKey(new MenuAction("New Layer from Composite") {
            @Override
            public void onClick() {
                getActiveCompOrNull().addNewLayerFromComposite("Composite");
            }
        }, CTRL_SHIFT_ALT_E);

        layersMenu.add(createLayerStackSubmenu());
        layersMenu.add(createLayerMaskSubmenu());
        layersMenu.add(createTextLayerSubmenu(pw));

        if (Build.enableAdjLayers) {
            layersMenu.add(createAdjustmentLayersSubmenu());
        }

        return layersMenu;
    }

    private static JMenu createLayerStackSubmenu() {
        PMenu sub = new PMenu("Layer Stack");

        sub.addActionWithKey(LayerMoveAction.INSTANCE_UP, CTRL_CLOSE_BRACKET);

        sub.addActionWithKey(LayerMoveAction.INSTANCE_DOWN, CTRL_OPEN_BRACKET);

        sub.addActionWithKey(new MenuAction("Layer to Top") {
            @Override
            public void onClick() {
                getActiveCompOrNull().moveActiveLayerToTop();
            }
        }, CTRL_SHIFT_CLOSE_BRACKET);

        sub.addActionWithKey(new MenuAction("Layer to Bottom") {
            @Override
            public void onClick() {
                getActiveCompOrNull().moveActiveLayerToBottom();
            }
        }, CTRL_SHIFT_OPEN_BRACKET);

        sub.addSeparator();

        sub.addActionWithKey(new MenuAction("Raise Layer Selection") {
            @Override
            public void onClick() {
                Composition comp = getActiveCompOrNull();
                comp.moveLayerSelectionUp();
            }
        }, ALT_CLOSE_BRACKET);

        sub.addActionWithKey(new MenuAction("Lower Layer Selection") {
            @Override
            public void onClick() {
                getActiveCompOrNull().moveLayerSelectionDown();
            }
        }, ALT_OPEN_BRACKET);

        return sub;
    }

    private static JMenu createLayerMaskSubmenu() {
        PMenu sub = new PMenu("Layer Mask");

        sub.addAction(new MenuAction("Add White (Reveal All)") {
            @Override
            public void onClick() {
                getActiveLayerOrNull().addMask(LayerMaskAddType.REVEAL_ALL);
            }
        });

        sub.addAction(new MenuAction("Add Black (Hide All)") {
            @Override
            public void onClick() {
                getActiveLayerOrNull().addMask(LayerMaskAddType.HIDE_ALL);
            }
        });

        sub.addAction(new MenuAction("Add from Selection") {
            @Override
            public void onClick() {
                getActiveLayerOrNull().addMask(LayerMaskAddType.REVEAL_SELECTION);
            }
        });

        sub.addActionWithKey(new GetImageAction(MaskFromColorRangePanel.NAME, true, false) {
            @Override
            protected void process(Layer layer, BufferedImage image) {
                MaskFromColorRangePanel.showInDialog(layer, image);
            }
        }, CTRL_M);

        sub.addAction(new MenuAction("Delete", HAS_LAYER_MASK) {
            @Override
            public void onClick() {
                ImageComponent ic = ImageComponents.getActiveIC();
                Composition comp = ic.getComp();
                Layer layer = comp.getActiveLayer();

                layer.deleteMask(AddToHistory.YES);

                comp.imageChanged(FULL);
            }
        });

        sub.addAction(new MenuAction("Apply", HAS_LAYER_MASK) {
            @Override
            public void onClick() {
                ImageComponent ic = ImageComponents.getActiveIC();
                Layer layer = ic.getComp().getActiveLayer();

                if (!(layer instanceof ImageLayer)) {
                    Messages.showNotImageLayerError();
                    return;
                }

                ((ImageLayer) layer).applyLayerMask(AddToHistory.YES);

                // TODO actually this should not be necessary
                layer.getComp().imageChanged(FULL);
            }
        });

        sub.addSeparator();

        MaskViewMode.NORMAL.addToMenu(sub);
        MaskViewMode.SHOW_MASK.addToMenu(sub);
        MaskViewMode.EDIT_MASK.addToMenu(sub);
        MaskViewMode.RUBYLITH.addToMenu(sub);

        return sub;
    }

    private static JMenu createTextLayerSubmenu(PixelitorWindow pw) {
        PMenu sub = new PMenu("Text Layer");

        sub.addActionWithKey(new MenuAction("New...") {
            @Override
            public void onClick() {
                TextLayer.createNew(pw);
            }
        }, T);

        sub.addActionWithKey(new MenuAction("Edit...", IS_TEXT_LAYER) {
            @Override
            public void onClick() {
                ImageComponents.onActiveTextLayer(textLayer -> textLayer.edit(pw));
            }
        }, CTRL_T);

        sub.addAction(new MenuAction("Rasterize", IS_TEXT_LAYER) {
            @Override
            public void onClick() {
                ImageComponents.onActiveTextLayer(TextLayer::replaceWithRasterized);
            }
        });

        return sub;
    }

    private static JMenu createAdjustmentLayersSubmenu() {
        PMenu sub = new PMenu("New Adjustment Layer");

        sub.addAction(new MenuAction("Invert Adjustment") { // TODO not "Invert" because of assertj test lookup confusion
            @Override
            public void onClick() {
                AddAdjLayerAction.INSTANCE.actionPerformed(null);
            }
        });

        return sub;
    }

    private static JMenu createSelectMenu() {
        PMenu selectMenu = new PMenu("Select", 'S');

        selectMenu.buildAction(SelectionActions.getDeselectAction()).enableIf(ACTION_ENABLED).withKey(CTRL_D).add();
        selectMenu.buildAction(SelectionActions.getShowHideSelectionAction()).enableIf(ACTION_ENABLED).withKey(CTRL_H).add();

        selectMenu.buildAction(SelectionActions.getInvertSelectionAction()).enableIf(ACTION_ENABLED).withKey(CTRL_SHIFT_I).add();
        selectMenu.buildAction(SelectionActions.getModifyAction()).enableIf(ACTION_ENABLED).add();

        selectMenu.addSeparator();
        selectMenu.buildAction(SelectionActions.getTraceWithBrush()).enableIf(ACTION_ENABLED).add();
        selectMenu.buildAction(SelectionActions.getTraceWithEraser()).enableIf(ACTION_ENABLED).add();

        return selectMenu;
    }

    private static JMenu createImageMenu() {
        PMenu imageMenu = new PMenu("Image", 'I');

        // crop
        imageMenu.buildAction(SelectionActions.getCropAction())
                .enableIf(ACTION_ENABLED).add();

        // resize
        imageMenu.addActionWithKey(new MenuAction("Resize...") {
            @Override
            public void onClick() {
                ResizePanel.resizeActiveImage();
            }
        }, CTRL_ALT_I);

        imageMenu.addAction(new MenuAction("Duplicate") {
            @Override
            public void onClick() {
                ImageComponents.duplicateActive();
            }
        });

        imageMenu.addSeparator();

        imageMenu.addAction(EnlargeCanvas.getAction());

        imageMenu.addAction(new MenuAction("Layer to Canvas Size") {
            @Override
            public void onClick() {
                getActiveCompOrNull().activeLayerToCanvasSize();
            }
        });

        imageMenu.addSeparator();

        // rotate
        imageMenu.addAction(new Rotate(ANGLE_90));
        imageMenu.addAction(new Rotate(ANGLE_180));
        imageMenu.addAction(new Rotate(ANGLE_270));

        imageMenu.addSeparator();

        // flip
        imageMenu.addAction(new Flip(HORIZONTAL));
        imageMenu.addAction(new Flip(VERTICAL));

        return imageMenu;
    }

    private static JMenu createColorMenu() {
        PMenu colorsMenu = new PMenu("Color", 'C');

        colorsMenu.buildFA("Color Balance", ColorBalance::new).withKey(CTRL_B).add();
        colorsMenu.buildFA(HueSat.NAME, HueSat::new).withKey(CTRL_U).add();
        colorsMenu.buildFA(Colorize.NAME, Colorize::new).add();
        colorsMenu.buildFA("Levels", Levels::new).withKey(CTRL_L).add();
//        colorsMenu.buildFA("Levels 2", Levels2::new).add();
        colorsMenu.buildFA(Brightness.NAME, Brightness::new).add();
        colorsMenu.buildFA(Solarize.NAME, Solarize::new).add();
        colorsMenu.buildFA(Sepia.NAME, Sepia::new).add();
        colorsMenu.buildFA("Invert", Invert::new).noGUI().withKey(CTRL_I).add();
        colorsMenu.buildFA(ChannelInvert.NAME, ChannelInvert::new).add();
        colorsMenu.buildFA("Channel Mixer", ChannelMixer::new).add();

        colorsMenu.add(createExtractChannelsSubmenu());

        colorsMenu.add(createReduceColorsSubmenu());

        colorsMenu.add(createFillSubmenu());

        return colorsMenu;
    }

    private static JMenu createExtractChannelsSubmenu() {
        PMenu sub = new PMenu("Extract Channels");

        sub.addFA("Extract Channel", ExtractChannel::new);

        sub.addSeparator();

        sub.buildFA(Luminosity.NAME, Luminosity::new).noGUI().extract().add();

        FilterAction extractValueChannel = ExtractChannelFilter.getValueChannelFA();
        sub.addFA(extractValueChannel);

        FilterAction desaturateChannel = ExtractChannelFilter.getDesaturateChannelFA();
        sub.addFA(desaturateChannel);

        sub.addSeparator();

        FilterAction getHue = ExtractChannelFilter.getHueChannelFA();
        sub.addFA(getHue);

        FilterAction getHuiInColors = ExtractChannelFilter.getHueInColorsChannelFA();
        sub.addFA(getHuiInColors);

        FilterAction getSat = ExtractChannelFilter.getSaturationChannelFA();
        sub.addFA(getSat);

        return sub;
    }

    private static JMenu createReduceColorsSubmenu() {
        PMenu sub = new PMenu("Reduce Colors");

        sub.addFA(JHQuantize.NAME, JHQuantize::new);
        sub.addFA("Posterize", Posterize::new);
        sub.addFA(Threshold.NAME, Threshold::new);

        sub.addSeparator();

        sub.addFA(JHTriTone.NAME, JHTriTone::new);
        sub.addFA(GradientMap.NAME, GradientMap::new);

        sub.addSeparator();

        sub.addFA(JHColorHalftone.NAME, JHColorHalftone::new);
        sub.addFA(JHDither.NAME, JHDither::new);

        return sub;
    }

    private static JMenu createFillSubmenu() {
        PMenu sub = new PMenu("Fill with");

        sub.buildFA(FOREGROUND.createFillFilterAction()).withKey(ALT_BACKSPACE).add();
        sub.buildFA(BACKGROUND.createFillFilterAction()).withKey(CTRL_BACKSPACE).add();
        sub.addFA(TRANSPARENT.createFillFilterAction());

        sub.buildFA(ColorWheel.NAME, ColorWheel::new).withFillListName().add();
        sub.buildFA(JHFourColorGradient.NAME, JHFourColorGradient::new)
                .withFillListName().add();
        sub.buildFA("Starburst", Starburst::new).withFillListName().add();

        return sub;
    }

    private static JMenu createFilterMenu() {
        PMenu filterMenu = new PMenu("Filter", 'T');

        filterMenu.add(createBlurSharpenSubmenu());
        filterMenu.add(createDistortSubmenu());
        filterMenu.add(createDislocateSubmenu());
        filterMenu.add(createLightSubmenu());
        filterMenu.add(createNoiseSubmenu());
        filterMenu.add(createRenderSubmenu());
        filterMenu.add(createArtisticSubmenu());
        filterMenu.add(createFindEdgesSubmenu());
        filterMenu.add(createOtherSubmenu());

        // TODO does it still make sense to add the old text filter?
        filterMenu.buildFA(TextFilter.createFilterAction()).add();

        return filterMenu;
    }

    private static JMenu createBlurSharpenSubmenu() {
        PMenu sub = new PMenu("Blur/Sharpen");
        sub.addFA(JHBoxBlur.NAME, JHBoxBlur::new);
//        sub.addFA(FastBlur.NAME, FastBlur::new);
        sub.addFA(JHFocus.NAME, JHFocus::new);
        sub.addFA(JHGaussianBlur.NAME, JHGaussianBlur::new);
        sub.addFA(JHLensBlur.NAME, JHLensBlur::new);
        sub.addFA(MOTION_BLUR.createFilterAction());
        sub.addFA(JHSmartBlur.NAME, JHSmartBlur::new);
        sub.addFA(SPIN_ZOOM_BLUR.createFilterAction());
        sub.addSeparator();
        sub.addFA(JHUnsharpMask.NAME, JHUnsharpMask::new);
        return sub;
    }

    private static JMenu createDistortSubmenu() {
        PMenu sub = new PMenu("Distort");

        sub.addFA(SwirlPinchBulge.NAME, SwirlPinchBulge::new);
        sub.addFA(CircleToSquare.NAME, CircleToSquare::new);
        sub.addFA(JHPerspective.NAME, JHPerspective::new);

        sub.addSeparator();

        sub.addFA(JHLensOverImage.NAME, JHLensOverImage::new);
        sub.addFA(Magnify.NAME, Magnify::new);

        sub.addSeparator();

        sub.addFA(JHTurbulentDistortion.NAME, JHTurbulentDistortion::new);
        sub.addFA(JHUnderWater.NAME, JHUnderWater::new);
        sub.addFA(JHWaterRipple.NAME, JHWaterRipple::new);
        sub.addFA(JHWaves.NAME, JHWaves::new);
        sub.addFA(AngularWaves.NAME, AngularWaves::new);
        sub.addFA(RadialWaves.NAME, RadialWaves::new);

        sub.addSeparator();

        sub.addFA(GlassTiles.NAME, GlassTiles::new);
        sub.addFA(PolarTiles.NAME, PolarTiles::new);
        sub.addFA(JHFrostedGlass.NAME, JHFrostedGlass::new);

        sub.addSeparator();

        sub.addFA(LittlePlanet.NAME, LittlePlanet::new);
        sub.addFA(JHPolarCoordinates.NAME, JHPolarCoordinates::new);
        sub.addFA(JHWrapAroundArc.NAME, JHWrapAroundArc::new);

        return sub;
    }

    private static JMenu createDislocateSubmenu() {
        PMenu sub = new PMenu("Dislocate");

        sub.addFA(DrunkVision.NAME, DrunkVision::new);
        sub.addFA(JHKaleidoscope.NAME, JHKaleidoscope::new);
        sub.addFA(JHOffset.NAME, JHOffset::new);
        sub.addFA(Mirror.NAME, Mirror::new);
        sub.addFA(Slice.NAME, Slice::new);
        sub.addFA(JHVideoFeedback.NAME, JHVideoFeedback::new);

        return sub;
    }

    private static JMenu createLightSubmenu() {
        PMenu sub = new PMenu("Light");

        sub.addFA(Flashlight.NAME, Flashlight::new);
        sub.addFA(JHGlint.NAME, JHGlint::new);
        sub.addFA(JHGlow.NAME, JHGlow::new);
        sub.addFA(JHRays.NAME, JHRays::new);
        sub.addFA(JHSparkle.NAME, JHSparkle::new);

        return sub;
    }

    private static JMenu createNoiseSubmenu() {
        PMenu sub = new PMenu("Noise");

        sub.buildFA(JHReduceNoise.NAME, JHReduceNoise::new).noGUI().add();
        sub.buildFA(JHMedian.NAME, JHMedian::new).noGUI().add();

        sub.addSeparator();

        sub.addFA(AddNoise.NAME, AddNoise::new);
        sub.addFA(JHPixelate.NAME, JHPixelate::new);

        return sub;
    }

    private static JMenu createRenderSubmenu() {
        PMenu sub = new PMenu("Render");

        sub.addFA(Clouds.NAME, Clouds::new);
        sub.addFA(JHPlasma.NAME, JHPlasma::new);
        sub.addFA(ValueNoise.NAME, ValueNoise::new);

        sub.addSeparator();

        sub.addFA(JHBrushedMetal.NAME, JHBrushedMetal::new);
        sub.addFA(JHCaustics.NAME, JHCaustics::new);
        sub.addFA(JHCells.NAME, JHCells::new);
        sub.addFA(FractalTree.NAME, FractalTree::new);
        sub.addFA(Marble.NAME, Marble::new);
        sub.addFA(Voronoi.NAME, Voronoi::new);
        sub.addFA(JHWood.NAME, JHWood::new);

        sub.addSeparator();

        sub.add(createRenderShapesSubmenu());

        return sub;
    }

    private static JMenu createRenderShapesSubmenu() {
        PMenu sub = new PMenu("Shapes");
        sub.addFA("Flower of Life", FlowerOfLife::new);
        sub.addFA("Lissajous Curve", Lissajous::new);
        sub.addFA("Mystic Rose", MysticRose::new);
        sub.addFA("Spirograph", Spirograph::new);
        return sub;
    }

    private static JMenu createArtisticSubmenu() {
        PMenu sub = new PMenu("Artistic");

        sub.addFA(JHCrystallize.NAME, JHCrystallize::new);
        sub.addFA(JHEmboss.NAME, JHEmboss::new);
        sub.addFA(JHOilPainting.NAME, JHOilPainting::new);
        sub.addFA(Orton.NAME, Orton::new);
        sub.addFA(PhotoCollage.NAME, PhotoCollage::new);
        sub.addFA(JHPointillize.NAME, JHPointillize::new);
        sub.addFA(RandomSpheres.NAME, RandomSpheres::new);
        sub.addFA(JHSmear.NAME, JHSmear::new);
        sub.addFA(JHStamp.NAME, JHStamp::new);
        sub.addFA(JHWeave.NAME, JHWeave::new);

        return sub;
    }

    private static JMenu createFindEdgesSubmenu() {
        PMenu sub = new PMenu("Find Edges");

        sub.addFA(JHConvolutionEdge.NAME, JHConvolutionEdge::new);

        sub.addAction(new FilterAction(JHLaplacian.NAME, JHLaplacian::new)
                .withoutGUI());

        sub.addFA(JHDifferenceOfGaussians.NAME, JHDifferenceOfGaussians::new);
        sub.addFA("Canny", Canny::new);

        return sub;
    }

    private static JMenu createOtherSubmenu() {
        PMenu sub = new PMenu("Other");

        sub.addFA(JHDropShadow.NAME, JHDropShadow::new);
        sub.addFA(Morphology.NAME, Morphology::new);
        sub.addFA("Random Filter", RandomFilter::new);
        sub.addFA("Transform Layer", TransformLayer::new);
        sub.addFA(Transition2D.NAME, Transition2D::new);

        sub.addSeparator();

        sub.addFA(Convolve.createFilterAction(3));
        sub.addFA(Convolve.createFilterAction(5));

        sub.addSeparator();

        sub.addFA(ChannelToTransparency.NAME, ChannelToTransparency::new);
        sub.buildFA(InvertTransparency.NAME, InvertTransparency::new).noGUI().add();

        return sub;
    }

    private static JMenu createViewMenu(PixelitorWindow pw) {
        PMenu viewMenu = new PMenu("View", 'V');

        viewMenu.add(ZoomMenu.INSTANCE);

        viewMenu.addSeparator();

        viewMenu.addAction(new MenuAction("Show History...") {
            @Override
            public void onClick() {
                History.showHistory();
            }
        });

        viewMenu.addAction(new MenuAction("Show Navigator...") {
            @Override
            public void onClick() {
                Navigator.showInDialog(pw);
            }
        });

        viewMenu.addSeparator();

        viewMenu.add(createColorVariationsSubmenu(pw));

        viewMenu.addAlwaysEnabledAction(new MenuAction("Color Palette...") {
            @Override
            public void onClick() {
                FullPalette palette = new FullPalette();
                VariationsPanel.showDialog(pw, palette, ColorSwatchClickHandler.STANDARD);
            }
        });

        viewMenu.addSeparator();

        viewMenu.add(new ShowHideStatusBarAction());
        viewMenu.buildAction(new ShowHideHistogramsAction()).alwaysEnabled().withKey(F6).add();
        viewMenu.buildAction(new ShowHideLayersAction()).alwaysEnabled().withKey(F7).add();
        viewMenu.add(new ShowHideToolsAction());
        viewMenu.buildAction(ShowHideAllAction.INSTANCE).alwaysEnabled().withKey(TAB).add();

        viewMenu.addAlwaysEnabledAction(new MenuAction("Set Default Workspace") {
            @Override
            public void onClick() {
                AppPreferences.WorkSpace.setDefault(pw);
            }
        });

        JCheckBoxMenuItem showPixelGridMI = new JCheckBoxMenuItem("Show Pixel Grid");
        showPixelGridMI.addActionListener(e -> {
            ImageComponent.showPixelGrid = showPixelGridMI.getState();
            ImageComponents.repaintAll();
        });
        viewMenu.add(showPixelGridMI);

        viewMenu.addSeparator();

        viewMenu.add(createArrangeWindowsSubmenu());

        return viewMenu;
    }

    private static JMenu createColorVariationsSubmenu(PixelitorWindow pw) {
        PMenu variations = new PMenu("Color Variations");
        variations.addAlwaysEnabledAction(new MenuAction("Foreground...") {
            @Override
            public void onClick() {
                VariationsPanel.showFGVariationsDialog(pw);
            }
        });
        variations.addAlwaysEnabledAction(new MenuAction("HSB Mix Foreground with Background...") {
            @Override
            public void onClick() {
                VariationsPanel.showHSBMixDialog(pw, true);
            }
        });
        variations.addAlwaysEnabledAction(new MenuAction("RGB Mix Foreground with Background...") {
            @Override
            public void onClick() {
                VariationsPanel.showRGBMixDialog(pw, true);
            }
        });

        variations.addSeparator();

        variations.addAlwaysEnabledAction(new MenuAction("Background...") {
            @Override
            public void onClick() {
                VariationsPanel.showBGVariationsDialog(pw);
            }
        });
        variations.addAlwaysEnabledAction(new MenuAction("HSB Mix Background with Foreground...") {
            @Override
            public void onClick() {
                VariationsPanel.showHSBMixDialog(pw, false);
            }
        });
        variations.addAlwaysEnabledAction(new MenuAction("RGB Mix Background with Foreground...") {
            @Override
            public void onClick() {
                VariationsPanel.showRGBMixDialog(pw, false);
            }
        });
        return variations;
    }

    private static JMenu createArrangeWindowsSubmenu() {
        PMenu sub = new PMenu("Arrange Windows");

        sub.addAction(new MenuAction("Cascade") {
            @Override
            public void onClick() {
                Desktop.INSTANCE.cascadeWindows();
            }
        });

        sub.addAction(new MenuAction("Tile") {
            @Override
            public void onClick() {
                Desktop.INSTANCE.tileWindows();
            }
        });

        return sub;
    }

    private static JMenu createDevelopMenu(PixelitorWindow pw) {
        PMenu developMenu = new PMenu("Develop", 'D');

        developMenu.add(createDebugSubmenu(pw));
        developMenu.add(createTestSubmenu(pw));
        developMenu.add(createSplashSubmenu());
        developMenu.add(createExperimentalSubmenu());

        developMenu.addAlwaysEnabledAction(new MenuAction("Filter Creator...") {
            @Override
            public void onClick() {
                FilterCreator.showInDialog(pw);
            }
        });

        developMenu.addAlwaysEnabledAction(new MenuAction("Debug Special") {
            @Override
            public void onClick() {
                BufferedImage img = ImageComponents.getActiveCompositeImageOrNull();

                Composite composite = new MultiplyComposite(1.0f);

                long startTime = System.nanoTime();

                int testsRun = 100;
                for (int i = 0; i < testsRun; i++) {
                    Graphics2D g = img.createGraphics();
                    g.setComposite(composite);
                    g.drawImage(img, 0, 0, null);
                    g.dispose();
                }

                long totalTime = (System.nanoTime() - startTime) / 1000000;
                System.out.println("MenuBar.actionPerformed: it took " + totalTime + " ms, average time = " + totalTime / testsRun);

            }
        });

        developMenu.addAction(new MenuAction("Dump Event Queue") {
            @Override
            public void onClick() {
                Events.dumpAll();
            }
        });

        developMenu.addAction(new MenuAction("Debug PID") {
            @Override
            public void onClick() {
                String vmRuntimeInfo = ManagementFactory.getRuntimeMXBean().getName();
                System.out.println(String.format("MenuBar::onClick: vmRuntimeInfo = '%s'", vmRuntimeInfo));
            }
        });

        developMenu.addAction(new MenuAction("Debug Layer Mask") {
            @Override
            public void onClick() {
                ImageLayer imageLayer = (ImageLayer) getActiveLayerOrNull();
                Utils.debugImage(imageLayer.getImage(), "layer image");

                if (imageLayer.hasMask()) {
                    LayerMask layerMask = imageLayer.getMask();
                    BufferedImage maskImage = layerMask.getImage();
                    Utils.debugImage(maskImage, "mask image");

                    BufferedImage transparencyImage = layerMask.getTransparencyImage();
                    Utils.debugImage(transparencyImage, "transparency image");
                }
            }
        });

        developMenu.addAction(new MenuAction("Mask update transparency from BW") {
            @Override
            public void onClick() {
                ImageLayer imageLayer = (ImageLayer) getActiveLayerOrNull();
                if (imageLayer.hasMask()) {
                    imageLayer.getMask().updateFromBWImage();
                    imageLayer.getComp().imageChanged(FULL);
                } else {
                    Messages.showInfo("No Mask in Current image", "Error");
                }
            }
        });

        developMenu.addAction(new MenuAction("imageChanged(FULL) on the active image") {
            @Override
            public void onClick() {
                getActiveCompOrNull().imageChanged(FULL, true);
            }
        });

        developMenu.addAction(new MenuAction("Debug getCanvasSizedSubImage") {
            @Override
            public void onClick() {
                ImageLayer imageLayer = ImageComponents.getActiveImageLayerOrMaskOrNull();
                BufferedImage bi = imageLayer.getCanvasSizedSubImage();
                Utils.debugImage(bi);
            }
        });

        developMenu.addAction(new MenuAction("Tests3x3.dumpCompositeOfActive()") {
            @Override
            public void onClick() {
                Tests3x3.dumpCompositeOfActive();
            }
        });

        developMenu.addAction(new MenuAction("Debug translation and canvas") {
            @Override
            public void onClick() {
                ImageComponents.onActiveImageLayer(layer -> {
                    String s = layer.toDebugCanvasString();
                    System.out.println(s);
                });
            }
        });

        developMenu.addAction(new MenuAction("Debug Copy Brush") {
            @Override
            public void onClick() {
                CopyBrush.setDebugBrushImage(true);
            }
        });

        developMenu.addAction(new MenuAction("Create All Filters") {
            @Override
            public void onClick() {
                FilterUtils.createAllFilters();
            }
        });

        developMenu.addAction(new MenuAction("debug FgBgColorSelector") {
            @Override
            public void onClick() {
                FgBgColors.getGUI().dumpState();
            }
        });

        return developMenu;
    }

    private static JMenu createDebugSubmenu(PixelitorWindow pw) {
        PMenu sub = new PMenu("Debug");

        sub.addAction(new MenuAction("repaint() on the active image") {
            @Override
            public void onClick() {
                ImageComponents.repaintActive();
            }
        });

        sub.addAlwaysEnabledAction(new MenuAction("revalidate() the main window") {
            @Override
            public void onClick() {
                pw.getContentPane().revalidate();
            }
        });

        sub.addAction(new MenuAction("reset the translation of current layer") {
            @Override
            public void onClick() {
                Composition comp = getActiveCompOrNull();
                Layer layer = comp.getActiveLayer();
                if (layer instanceof ContentLayer) {
                    ContentLayer contentLayer = (ContentLayer) layer;
                    contentLayer.setTranslation(0, 0);
                }
                if (layer.hasMask()) {
                    layer.getMask().setTranslation(0, 0);
                }
                comp.imageChanged(FULL);
            }
        });

        sub.addAction(new MenuAction("Update Histograms") {
            @Override
            public void onClick() {
                Composition comp = getActiveCompOrNull();
                HistogramsPanel.INSTANCE.updateFromCompIfShown(comp);
            }
        });

        sub.addAction(new MenuAction("Save All Images to Folder...") {
            @Override
            public void onClick() {
                OpenSaveManager.saveAllImagesToDir();
            }
        });

        sub.addAction(new MenuAction("Debug ImageLayer Images") {
            @Override
            public void onClick() {
                ImageLayer layer = ImageComponents.getActiveImageLayerOrMaskOrNull();
                layer.debugImages();
            }
        });

        return sub;
    }

    private static JMenu createTestSubmenu(PixelitorWindow pw) {
        PMenu sub = new PMenu("Test");

        sub.addFA("ParamTest", ParamTest::new);

        sub.addActionWithKey(new MenuAction("Random Resize") {
            @Override
            public void onClick() {
                FilterTests.randomResize();
            }
        }, CTRL_ALT_R);

        sub.buildAction(new MenuAction("Random GUI Test") {
            @Override
            public void onClick() {
                RandomGUITest.runTest();
            }
        }).alwaysEnabled().withKey(CTRL_R).add();

        sub.addAction(new ImageLayerAction("Filter Performance Test") {
            @Override
            protected void process(ImageLayer layer) {
                new PerformanceTestingDialog(pw, layer);
            }
        });

        sub.addAction(new ImageLayerAction("Find Slowest Filter", false) {
            @Override
            protected void process(ImageLayer layer) {
                FilterTests.findSlowestFilter(layer);
            }
        });

        sub.addAction(new MenuAction("getCompositeImage() Performance Test...") {
            @Override
            public void onClick() {
                FilterTests.getCompositeImagePerformanceTest();
            }
        });

        sub.addSeparator();

        sub.addAction(new ImageLayerAction("Run All Filters on Current Layer/Mask", false) {
            @Override
            protected void process(ImageLayer layer) {
                FilterTests.runAllFiltersOn(layer);
            }
        });

        sub.addAction(new ImageLayerAction("Save the Result of Each Filter...") {
            @Override
            protected void process(ImageLayer layer) {
                FilterTests.saveTheResultOfEachFilter(layer);
            }
        });

        sub.addAction(new MenuAction("Save Current Image in All Formats...") {
            @Override
            public void onClick() {
                OpenSaveManager.saveCurrentImageInAllFormats();
            }
        });

        sub.addSeparator();

        sub.addAlwaysEnabledAction(new ImageLayerAction("Test Tools", false) {
            @Override
            protected void process(ImageLayer layer) {
                ToolTests.testTools(layer);
            }
        });

        return sub;
    }

    private static JMenu createSplashSubmenu() {
        PMenu sub = new PMenu("Splash");

        sub.addAlwaysEnabledAction(new MenuAction("Create Splash Image") {
            @Override
            public void onClick() {
                SplashImageCreator.createSplashImage();
            }
        });

        sub.addAlwaysEnabledAction(new MenuAction("Save Many Splash Images...") {
            @Override
            public void onClick() {
                SplashImageCreator.saveManySplashImages();
            }
        });

        return sub;
    }

    private static JMenu createExperimentalSubmenu() {
        PMenu sub = new PMenu("Experimental");

        sub.addFA(Contours.NAME, Contours::new);
        sub.addSeparator();

        sub.addFA(Droste.NAME, Droste::new);
        sub.addFA(Sphere3D.NAME, Sphere3D::new);
        sub.addFA("Grid", RenderGrid::new);
        sub.addFA(EmptyPolar.NAME, EmptyPolar::new);
        sub.addFA(JHCheckerFilter.NAME, JHCheckerFilter::new);

        return sub;
    }

    private static JMenu createHelpMenu(PixelitorWindow pw) {
        PMenu helpMenu = new PMenu("Help", 'H');

        helpMenu.addAlwaysEnabledAction(new MenuAction("Tip of the Day") {
            @Override
            public void onClick() {
                TipsOfTheDay.showTips(pw, true);
            }
        });

//        JMenu sub = new JMenu("Web");
//        MenuFactory.createMenuItem(new OpenInBrowserAction("Ask for Help", "https://sourceforge.net/projects/pixelitor/forums/forum/1034234"), null, sub, MenuEnableCondition.ACTION_ENABLED);
//        MenuFactory.createMenuItem(new OpenInBrowserAction("Discuss Pixelitor", "https://sourceforge.net/projects/pixelitor/forums/forum/1034233"), null, sub, MenuEnableCondition.ACTION_ENABLED);
//        MenuFactory.createMenuItem(new OpenInBrowserAction("Report a Bug", "https://sourceforge.net/tracker/?group_id=285935&atid=1211793"), null, sub, MenuEnableCondition.ACTION_ENABLED);
//        helpMenu.add(sub);

        helpMenu.addSeparator();

        helpMenu.add(new MenuAction("Internal State...") {
            @Override
            public void onClick() {
                AppNode node = new AppNode();
                String title = "Internal State";

                JTree tree = new JTree(node);

                JLabel explainLabel = new JLabel("<html>If you are reporting a bug that cannot be reproduced," +
                        "<br>please include the following information:");
                explainLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

                JPanel form = new JPanel(new BorderLayout());
                form.add(explainLabel, BorderLayout.NORTH);
                form.add(new JScrollPane(tree), BorderLayout.CENTER);

                String text = node.toDetailedString();

                GUIUtils.showClipboardTextDialog(form, title, text);
            }
        });

        helpMenu.add(new MenuAction("Check for Update...") {
            @Override
            public void onClick() {
                UpdatesCheck.checkForUpdates();
            }
        });

        helpMenu.addAlwaysEnabledAction(new MenuAction("About") {
            @Override
            public void onClick() {
                AboutDialog.showDialog(pw);
            }
        });

        return helpMenu;
    }
}
