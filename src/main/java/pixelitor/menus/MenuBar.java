/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
import pixelitor.Desktop;
import pixelitor.EnlargeCanvas;
import pixelitor.ImageComponent;
import pixelitor.ImageComponents;
import pixelitor.NewImage;
import pixelitor.PixelitorWindow;
import pixelitor.TipsOfTheDay;
import pixelitor.automate.BatchFilterWizard;
import pixelitor.automate.BatchResize;
import pixelitor.filters.*;
import pixelitor.filters.animation.TweenWizard;
import pixelitor.filters.comp.Flip;
import pixelitor.filters.comp.Rotate;
import pixelitor.filters.convolve.Convolve;
import pixelitor.filters.gui.ResizePanel;
import pixelitor.filters.jhlabsproxies.*;
import pixelitor.filters.lookup.ColorBalance;
import pixelitor.filters.lookup.Levels;
import pixelitor.filters.lookup.Luminosity;
import pixelitor.filters.painters.TextFilter;
import pixelitor.history.AddToHistory;
import pixelitor.history.History;
import pixelitor.io.FileChoosers;
import pixelitor.io.OpenSaveManager;
import pixelitor.io.OptimizedJpegSavePanel;
import pixelitor.layers.AddNewLayerAction;
import pixelitor.layers.AdjustmentLayer;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.DeleteActiveLayerAction;
import pixelitor.layers.DuplicateLayerAction;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMoveAction;
import pixelitor.layers.TextLayer;
import pixelitor.menus.edit.CopyAction;
import pixelitor.menus.edit.CopySource;
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
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Dialogs;
import pixelitor.utils.FilterCreator;
import pixelitor.utils.HistogramsPanel;
import pixelitor.utils.PerformanceTestingDialog;
import pixelitor.utils.test.DebugEventQueue;
import pixelitor.utils.test.ImageTests;
import pixelitor.utils.test.OpTests;
import pixelitor.utils.test.RobotTest;
import pixelitor.utils.test.ToolTests;

import javax.swing.*;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Optional;

import static pixelitor.Composition.ImageChangeActions.FULL;
import static pixelitor.FillType.BACKGROUND;
import static pixelitor.FillType.FOREGROUND;
import static pixelitor.FillType.TRANSPARENT;
import static pixelitor.filters.comp.Flip.Direction.HORIZONTAL;
import static pixelitor.filters.comp.Flip.Direction.VERTICAL;
import static pixelitor.filters.jhlabsproxies.JHMotionBlur.Mode.MOTION_BLUR;
import static pixelitor.filters.jhlabsproxies.JHMotionBlur.Mode.SPIN_ZOOM_BLUR;

/**
 * The menu bar of the app
 */
public class MenuBar extends JMenuBar {
    private static final KeyStroke CTRL_N = KeyStroke.getKeyStroke('N', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_O = KeyStroke.getKeyStroke('O', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_S = KeyStroke.getKeyStroke('S', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_SHIFT_S = KeyStroke.getKeyStroke('S', InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK);
    private static final KeyStroke CTRL_W = KeyStroke.getKeyStroke('W', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_ALT_W = KeyStroke.getKeyStroke('W', InputEvent.CTRL_MASK | InputEvent.ALT_MASK);
    private static final KeyStroke CTRL_F = KeyStroke.getKeyStroke('F', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_Z = KeyStroke.getKeyStroke('Z', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_SHIFT_Z = KeyStroke.getKeyStroke('Z', InputEvent.SHIFT_MASK + InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_SHIFT_F = KeyStroke.getKeyStroke('F', InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK);
    private static final KeyStroke CTRL_C = KeyStroke.getKeyStroke('C', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_SHIFT_C = KeyStroke.getKeyStroke('C', InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK);
    private static final KeyStroke CTRL_V = KeyStroke.getKeyStroke('V', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_SHIFT_V = KeyStroke.getKeyStroke('V', InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK);
    private static final KeyStroke CTRL_ALT_I = KeyStroke.getKeyStroke('I', InputEvent.CTRL_MASK | InputEvent.ALT_MASK);
    private static final KeyStroke CTRL_B = KeyStroke.getKeyStroke('B', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_U = KeyStroke.getKeyStroke('U', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_L = KeyStroke.getKeyStroke('L', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_I = KeyStroke.getKeyStroke('I', InputEvent.CTRL_MASK);
    private static final KeyStroke ALT_BACKSPACE = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, InputEvent.ALT_MASK);
    private static final KeyStroke CTRL_BACKSPACE = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_D = KeyStroke.getKeyStroke('D', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_SHIFT_I = KeyStroke.getKeyStroke('I', InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK);
    private static final KeyStroke T = KeyStroke.getKeyStroke('T');
    private static final KeyStroke CTRL_E = KeyStroke.getKeyStroke('E', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_J = KeyStroke.getKeyStroke('J', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_SHIFT_ALT_E = KeyStroke.getKeyStroke('E', InputEvent.CTRL_MASK + InputEvent.ALT_MASK + InputEvent.SHIFT_MASK);
    private static final KeyStroke CTRL_CLOSE_BRACKET = KeyStroke.getKeyStroke(']', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_OPEN_BRACKET = KeyStroke.getKeyStroke('[', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_SHIFT_CLOSE_BRACKET = KeyStroke.getKeyStroke(']', InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK);
    private static final KeyStroke CTRL_SHIFT_OPEN_BRACKET = KeyStroke.getKeyStroke('[', InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK);
    private static final KeyStroke ALT_CLOSE_BRACKET = KeyStroke.getKeyStroke(']', InputEvent.ALT_MASK);
    private static final KeyStroke ALT_OPEN_BRACKET = KeyStroke.getKeyStroke('[', InputEvent.ALT_MASK);
    private static final KeyStroke F6 = KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0);
    private static final KeyStroke F7 = KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0);
    private static final KeyStroke TAB = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
    private static final KeyStroke CTRL_R = KeyStroke.getKeyStroke('R', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_ALT_R = KeyStroke.getKeyStroke('R', InputEvent.CTRL_MASK + InputEvent.ALT_MASK);

    public MenuBar(PixelitorWindow pixelitorWindow) {
        initFileMenu(pixelitorWindow);
        initEditMenu();
        initLayerMenu();
        initSelectMenu();
        initColorsMenu();
        initFilterMenu();
        initViewMenu(pixelitorWindow);

        if (Build.CURRENT != Build.FINAL) {
            initDevelopMenu(pixelitorWindow);
        }

        initHelpMenu(pixelitorWindow);
    }

    private void initFileMenu(PixelitorWindow pixelitorWindow) {
        JMenu fileMenu = createMenu("File", 'F');

        // new image
        createMenuItem(NewImage.getAction(), fileMenu, EnabledIf.ACTION_ENABLED, CTRL_N);

        // open
        Action openAction = new MenuAction("Open...") {
            @Override
            void onClick() {
                FileChoosers.open();
            }
        };
        createMenuItem(openAction, fileMenu, EnabledIf.ACTION_ENABLED, CTRL_O);

        // recent files
        JMenu recentFiles = RecentFilesMenu.getInstance();
        fileMenu.add(recentFiles);

        fileMenu.addSeparator();

        // save
        Action saveAction = new MenuAction("Save") {
            @Override
            void onClick() {
                OpenSaveManager.save(false);
            }
        };
        createMenuItem(saveAction, fileMenu, CTRL_S);

        // save as
        Action saveAsAction = new MenuAction("Save As...") {
            @Override
            void onClick() {
                OpenSaveManager.save(true);
            }
        };
        createMenuItem(saveAsAction, fileMenu, CTRL_SHIFT_S);

        Action optimizedSaveAction = new MenuAction("Export Optimized JPEG...") {
            @Override
            void onClick() {
                Composition comp = ImageComponents.getActiveComp().get();
                BufferedImage image = comp.getCompositeImage();
                OptimizedJpegSavePanel.showInDialog(image, pixelitorWindow);
            }
        };
        createMenuItem(optimizedSaveAction, fileMenu);

        Action exportORA = new MenuAction("Export OpenRaster...") {
            @Override
            void onClick() {
                OpenRasterExportPanel.showInDialog(pixelitorWindow);
            }
        };
        createMenuItem(exportORA, fileMenu);

        fileMenu.addSeparator();

        Action exportLayerAnim = new MenuAction("Export Layer Animation...") {
            @Override
            void onClick() {
                AnimGifExport.start(pixelitorWindow);
            }
        };
        createMenuItem(exportLayerAnim, fileMenu);

        Action exportTweeningAnim = new MenuAction("Export Tweening Animation...") {
            @Override
            void onClick() {
                new TweenWizard().start(pixelitorWindow);
            }
        };
        createMenuItem(exportTweeningAnim, fileMenu);

        fileMenu.addSeparator();

        // close
        Action closeAction = new MenuAction("Close") {
            @Override
            void onClick() {
                OpenSaveManager.warnAndCloseImage(ImageComponents.getActiveImageComponent());
            }
        };
        createMenuItem(closeAction, fileMenu, CTRL_W);

        // close all
        Action closeAllAction = new MenuAction("Close All") {
            @Override
            void onClick() {
                OpenSaveManager.warnAndCloseAllImages();
            }
        };
        createMenuItem(closeAllAction, fileMenu, CTRL_ALT_W);

        initAutomateSubmenu(fileMenu, pixelitorWindow);

        if (!JVM.isMac) {
            Action newFromScreenCapture = new ScreenCaptureAction();
            createMenuItem(newFromScreenCapture, fileMenu, EnabledIf.ACTION_ENABLED);
        }

        fileMenu.addSeparator();

        // exit
        String exitName = JVM.isMac ? "Quit" : "Exit";
        Action exitAction = new MenuAction(exitName) {
            @Override
            void onClick() {
                AppLogic.exitApp(pixelitorWindow);
            }
        };
        createMenuItem(exitAction, fileMenu, EnabledIf.ACTION_ENABLED);

        this.add(fileMenu);
    }

    private static void initAutomateSubmenu(JMenu fileMenu, PixelitorWindow pixelitorWindow) {
        JMenu batchSubmenu = new JMenu("Automate");
        fileMenu.add(batchSubmenu);

        Action batchResizeAction = new MenuAction("Batch Resize...") {
            @Override
            void onClick() {
                BatchResize.start();
            }
        };
        createMenuItem(batchResizeAction, batchSubmenu, EnabledIf.ACTION_ENABLED);

        Action batchFilterAction = new MenuAction("Batch Filter...") {
            @Override
            void onClick() {
                new BatchFilterWizard().start(pixelitorWindow);
            }
        };
        createMenuItem(batchFilterAction, batchSubmenu, EnabledIf.ACTION_ENABLED);

        Action exportLayersAction = new MenuAction("Export Layers to PNG...") {
            @Override
            void onClick() {
                OpenSaveManager.exportLayersToPNG();
            }
        };
        createMenuItem(exportLayersAction, batchSubmenu, EnabledIf.THERE_IS_OPEN_IMAGE);
    }

    private void initEditMenu() {
        JMenu editMenu = createMenu("Edit", 'E');

        // last op
        createMenuItem(RepeatLast.INSTANCE, editMenu, EnabledIf.CAN_REPEAT_OPERATION, CTRL_F);
        editMenu.addSeparator();

        // undo
        Action undoAction = new MenuAction("Undo") {
            @Override
            void onClick() {
                History.undo();
            }
        };
        createMenuItem(undoAction, editMenu, EnabledIf.UNDO_POSSIBLE, CTRL_Z);

        // undo
        Action redoAction = new MenuAction("Redo") {
            @Override
            void onClick() {
                History.redo();
            }
        };
        createMenuItem(redoAction, editMenu, EnabledIf.REDO_POSSIBLE, CTRL_SHIFT_Z);

        // fade
        createMenuItem(new Fade(), editMenu, EnabledIf.FADING_POSSIBLE, CTRL_SHIFT_F);

        // crop
        createMenuItem(SelectionActions.getCropAction(), editMenu, EnabledIf.ACTION_ENABLED);

        editMenu.addSeparator();

        // copy
        createMenuItem(new CopyAction(CopySource.LAYER), editMenu, CTRL_C);
        createMenuItem(new CopyAction(CopySource.COMPOSITE), editMenu, CTRL_SHIFT_C);
        // paste
        createMenuItem(new PasteAction(PasteDestination.NEW_IMAGE), editMenu, EnabledIf.ACTION_ENABLED, CTRL_V);
        createMenuItem(new PasteAction(PasteDestination.NEW_LAYER), editMenu, CTRL_SHIFT_V);

        editMenu.addSeparator();

        // resize
        Action resizeAction = new MenuAction("Resize...") {
            @Override
            void onClick() {
                ResizePanel.resizeActiveImage();
            }
        };
        createMenuItem(resizeAction, editMenu, CTRL_ALT_I);

        JMenu rotateSubmenu = new JMenu("Rotate/Flip");
        editMenu.add(rotateSubmenu);
        // rotate
        createMenuItem(new Rotate(90, "Rotate 90\u00B0 CW"), rotateSubmenu);
        createMenuItem(new Rotate(180, "Rotate 180\u00B0"), rotateSubmenu);
        createMenuItem(new Rotate(270, "Rotate 90\u00B0 CCW"), rotateSubmenu);
        rotateSubmenu.addSeparator();
        // flip
        createMenuItem(Flip.createFlip(HORIZONTAL), rotateSubmenu);
        createMenuItem(Flip.createFlip(VERTICAL), rotateSubmenu);

        createMenuItem(new TransformLayer(), editMenu);

        editMenu.addSeparator();
        // preferences
        Action preferencesAction = new MenuAction("Preferences...") {
            @Override
            void onClick() {
                AppPreferences.Panel.showInDialog();
            }
        };
        editMenu.add(preferencesAction);

        this.add(editMenu);
    }

    private void initColorsMenu() {
        JMenu colorsMenu = createMenu("Colors", 'C');

        createMenuItem(new ColorBalance(), colorsMenu, CTRL_B);
        createMenuItem(new HueSat(), colorsMenu, CTRL_U);
        createMenuItem(new Colorize(), colorsMenu);
        createMenuItem(new Levels(), colorsMenu, CTRL_L);
        createMenuItem(new Brightness(), colorsMenu);
        createMenuItem(new Solarize(), colorsMenu);
        createMenuItem(new Sepia(), colorsMenu);
        createMenuItem(new Invert(), colorsMenu, CTRL_I);
        createMenuItem(new ChannelInvert(), colorsMenu);
        createMenuItem(new ChannelMixer(), colorsMenu);

        initExtractChannelsSubmenu(colorsMenu);

        initReduceColorsSubmenu(colorsMenu);

        initFillSubmenu(colorsMenu);

        this.add(colorsMenu);
    }

    private static void initFillSubmenu(JMenu colorsMenu) {
        JMenu fillSubmenu = new JMenu("Fill with");
        createMenuItem(new Fill(FOREGROUND), fillSubmenu, ALT_BACKSPACE);
        createMenuItem(new Fill(BACKGROUND), fillSubmenu, CTRL_BACKSPACE);
        createMenuItem(new Fill(TRANSPARENT), fillSubmenu);
        createMenuItem(new ColorWheel(), fillSubmenu);
        createMenuItem(new JHFourColorGradient(), fillSubmenu);
        createMenuItem(new Starburst(), fillSubmenu);

        colorsMenu.add(fillSubmenu);
    }

    private static void initExtractChannelsSubmenu(JMenu colorsMenu) {
        JMenu channelsSubmenu = new JMenu("Extract Channels");
        colorsMenu.add(channelsSubmenu);

        createMenuItem(new ExtractChannel(), channelsSubmenu);

        channelsSubmenu.addSeparator();
        createMenuItem(new Luminosity(), channelsSubmenu);
        createMenuItem(NoDialogPixelOpFactory.getValueChannelOp(), channelsSubmenu);
        createMenuItem(NoDialogPixelOpFactory.getDesaturateChannelOp(), channelsSubmenu);
        channelsSubmenu.addSeparator();
        createMenuItem(NoDialogPixelOpFactory.getHueChannelOp(), channelsSubmenu);
        createMenuItem(NoDialogPixelOpFactory.getHueInColorsChannelOp(), channelsSubmenu);
        createMenuItem(NoDialogPixelOpFactory.getSaturationChannelOp(), channelsSubmenu);
    }

    private static void initReduceColorsSubmenu(JMenu colorsMenu) {
        JMenu reduceColorsSubmenu = new JMenu("Reduce Colors");
        colorsMenu.add(reduceColorsSubmenu);

        createMenuItem(new JHQuantize(), reduceColorsSubmenu);
        createMenuItem(new Posterize(), reduceColorsSubmenu);
        createMenuItem(new Threshold(), reduceColorsSubmenu);
        reduceColorsSubmenu.addSeparator();
        createMenuItem(new JHTriTone(), reduceColorsSubmenu);
        createMenuItem(new GradientMap(), reduceColorsSubmenu);
        reduceColorsSubmenu.addSeparator();
        createMenuItem(new JHColorHalftone(), reduceColorsSubmenu);
        createMenuItem(new JHDither(), reduceColorsSubmenu);
    }

    private void initSelectMenu() {
        JMenu selectMenu = createMenu("Selection", 'S');

        createMenuItem(SelectionActions.getDeselectAction(), selectMenu, EnabledIf.ACTION_ENABLED, CTRL_D);

        createMenuItem(SelectionActions.getInvertSelectionAction(), selectMenu, EnabledIf.ACTION_ENABLED, CTRL_SHIFT_I);

        selectMenu.addSeparator();
        createMenuItem(SelectionActions.getTraceWithBrush(), selectMenu, EnabledIf.ACTION_ENABLED);
        createMenuItem(SelectionActions.getTraceWithEraser(), selectMenu, EnabledIf.ACTION_ENABLED);

        this.add(selectMenu);
    }

    private void initFilterMenu() {
        JMenu filterMenu = createMenu("Filter", 'T');

        initBlurSharpenSubmenu(filterMenu);
        initDistortSubmenu(filterMenu);
        initDislocateSubmenu(filterMenu);
        initLightSubmenu(filterMenu);
        initNoiseSubmenu(filterMenu);
        initRenderSubmenu(filterMenu);
        initArtisticSubmenu(filterMenu);
        initFindEdgesSubmenu(filterMenu);
        initOtherSubmenu(filterMenu);

        createMenuItem(TextFilter.INSTANCE, filterMenu, T);

        this.add(filterMenu);
    }

    private static void initRenderSubmenu(JMenu filterMenu) {
        JMenu renderSubmenu = new JMenu("Render");
        createMenuItem(new Clouds(), renderSubmenu);
        createMenuItem(new ValueNoise(), renderSubmenu);
        createMenuItem(new JHCaustics(), renderSubmenu);
        createMenuItem(new JHPlasma(), renderSubmenu);
        createMenuItem(new JHWood(), renderSubmenu);
        createMenuItem(new JHCells(), renderSubmenu);
        createMenuItem(new JHBrushedMetal(), renderSubmenu);

        filterMenu.add(renderSubmenu);
    }

    private static void initFindEdgesSubmenu(JMenu filterMenu) {
        JMenu findEdgesSubmenu = new JMenu("Find Edges");
        createMenuItem(new JHConvolutionEdge(), findEdgesSubmenu);
        createMenuItem(new JHLaplacian(), findEdgesSubmenu);
        createMenuItem(new JHDifferenceOfGaussians(), findEdgesSubmenu);
        createMenuItem(new Canny(), findEdgesSubmenu);
        filterMenu.add(findEdgesSubmenu);
    }

    private static void initOtherSubmenu(JMenu filterMenu) {
        JMenu otherFiltersSubmenu = new JMenu("Other");
        createMenuItem(new Convolve(3), otherFiltersSubmenu);
        createMenuItem(new Convolve(5), otherFiltersSubmenu);

        createMenuItem(new JHDropShadow(), otherFiltersSubmenu);
        createMenuItem(new Transition2D(), otherFiltersSubmenu);

        createMenuItem(new RandomFilter(), otherFiltersSubmenu);

        filterMenu.add(otherFiltersSubmenu);
    }

    private static void initArtisticSubmenu(JMenu filterMenu) {
        JMenu artisticFiltersSubmenu = new JMenu("Artistic");
        createMenuItem(new JHCrystallize(), artisticFiltersSubmenu);
        createMenuItem(new JHPointillize(), artisticFiltersSubmenu);
        createMenuItem(new JHStamp(), artisticFiltersSubmenu);
        createMenuItem(new JHDryBrush(), artisticFiltersSubmenu);

        createMenuItem(new RandomSpheres(), artisticFiltersSubmenu);
        createMenuItem(new JHSmear(), artisticFiltersSubmenu);
        createMenuItem(new JHEmboss(), artisticFiltersSubmenu);

        createMenuItem(new Orton(), artisticFiltersSubmenu);
        createMenuItem(new PhotoCollage(), artisticFiltersSubmenu);

        filterMenu.add(artisticFiltersSubmenu);
    }

    private static void initBlurSharpenSubmenu(JMenu filterMenu) {
        JMenu bsSubmenu = new JMenu("Blur/Sharpen");
        createMenuItem(new JHGaussianBlur(), bsSubmenu);
        createMenuItem(new JHSmartBlur(), bsSubmenu);
        createMenuItem(new JHBoxBlur(), bsSubmenu);
        createMenuItem(new FastBlur(), bsSubmenu);
        createMenuItem(new JHLensBlur(), bsSubmenu);
        createMenuItem(new JHMotionBlur(MOTION_BLUR), bsSubmenu);
        createMenuItem(new JHMotionBlur(SPIN_ZOOM_BLUR), bsSubmenu);
        bsSubmenu.addSeparator();
        createMenuItem(new JHUnsharpMask(), bsSubmenu);
        filterMenu.add(bsSubmenu);
    }

    private static void initNoiseSubmenu(JMenu filterMenu) {
        JMenu noiseSubmenu = new JMenu("Noise");
        createMenuItem(new JHReduceNoise(), noiseSubmenu);
        createMenuItem(new JHMedian(), noiseSubmenu);

        noiseSubmenu.addSeparator();
        createMenuItem(new AddNoise(), noiseSubmenu);
        createMenuItem(new JHPixelate(), noiseSubmenu);

        filterMenu.add(noiseSubmenu);
    }

    private static void initLightSubmenu(JMenu filterMenu) {
        JMenu lightSubmenu = new JMenu("Light");
        filterMenu.add(lightSubmenu);
        createMenuItem(new JHGlow(), lightSubmenu);
        createMenuItem(new JHSparkle(), lightSubmenu);
        createMenuItem(new JHRays(), lightSubmenu);
        createMenuItem(new JHGlint(), lightSubmenu);
    }

    private static void initDistortSubmenu(JMenu filterMenu) {
        JMenu distortMenu = new JMenu("Distort");
        filterMenu.add(distortMenu);
//        MenuFactory.createMenuItem(new JHPinch(), null, distortMenu);
//        MenuFactory.createMenuItem(new Swirl(), null, distortMenu);
        createMenuItem(new UnifiedSwirl(), distortMenu);

        createMenuItem(new CircleToSquare(), distortMenu);
        createMenuItem(new JHPerspective(), distortMenu);
        distortMenu.addSeparator();
        createMenuItem(new JHLensOverImage(), distortMenu);
        createMenuItem(new Magnify(), distortMenu);
        distortMenu.addSeparator();
        createMenuItem(new JHTurbulentDistortion(), distortMenu);
        createMenuItem(new JHUnderWater(), distortMenu);
        createMenuItem(new JHWaterRipple(), distortMenu);
        createMenuItem(new JHWaves(), distortMenu);
        createMenuItem(new AngularWaves(), distortMenu);
        createMenuItem(new RadialWaves(), distortMenu);
        distortMenu.addSeparator();
        createMenuItem(new GlassTiles(), distortMenu);
        createMenuItem(new PolarTiles(), distortMenu);
        createMenuItem(new JHFrostedGlass(), distortMenu);
        distortMenu.addSeparator();
        createMenuItem(new LittlePlanet(), distortMenu);
        createMenuItem(new JHPolarCoordinates(), distortMenu);
        createMenuItem(new JHWrapAroundArc(), distortMenu);
    }

    private static void initDislocateSubmenu(JMenu filterMenu) {
        JMenu dislocateSubmenu = new JMenu("Dislocate");
        filterMenu.add(dislocateSubmenu);

        createMenuItem(new JHKaleidoscope(), dislocateSubmenu);
        createMenuItem(new JHVideoFeedback(), dislocateSubmenu);
        createMenuItem(new JHOffset(), dislocateSubmenu);
        createMenuItem(new Slice(), dislocateSubmenu);
        createMenuItem(new Mirror(), dislocateSubmenu);
    }

    private void initLayerMenu() {
        JMenu layersMenu = createMenu("Layer", 'L');

        layersMenu.add(AddNewLayerAction.INSTANCE);
        layersMenu.add(DeleteActiveLayerAction.INSTANCE);

        Action flattenImageAction = new MenuAction("Flatten Image") {
            @Override
            void onClick() {
                Composition comp = ImageComponents.getActiveComp().get();
                comp.flattenImage(true);
            }
        };
        createMenuItem(flattenImageAction, layersMenu);

        Action mergeDownAction = new MenuAction("Merge Down") {
            @Override
            void onClick() {
                Composition comp = ImageComponents.getActiveComp().get();
                comp.mergeDown();
            }
        };
        createMenuItem(mergeDownAction, layersMenu, CTRL_E);

//        Action duplicateLayerAction = new MenuAction("Duplicate Layer") {
//            @Override
//            void onClick() {
//                Composition comp = ImageComponents.getActiveComp().get();
//                comp.duplicateLayer();
//            }
//        };
        createMenuItem(DuplicateLayerAction.INSTANCE, layersMenu, CTRL_J);

        Action newLayerFromCompositeAction = new MenuAction("New Layer from Composite") {
            @Override
            void onClick() {
                Composition comp = ImageComponents.getActiveComp().get();
                comp.addNewLayerFromComposite("Composite");
            }
        };
        createMenuItem(newLayerFromCompositeAction, layersMenu, CTRL_SHIFT_ALT_E);

        Action layerToCanvasSizeAction = new MenuAction("Layer to Canvas Size") {
            @Override
            void onClick() {
                Composition comp = ImageComponents.getActiveComp().get();
                comp.layerToCanvasSize();
            }
        };
        createMenuItem(layerToCanvasSizeAction, layersMenu);

        initLayerStackSubmenu(layersMenu);
        this.add(layersMenu);
    }

    private static void initLayerStackSubmenu(JMenu layersMenu) {
        JMenu layerStackSubmenu = new JMenu("Layer Stack");

        createMenuItem(LayerMoveAction.INSTANCE_UP, layerStackSubmenu, CTRL_CLOSE_BRACKET);

        createMenuItem(LayerMoveAction.INSTANCE_DOWN, layerStackSubmenu, CTRL_OPEN_BRACKET);

        Action moveToLast = new MenuAction("Layer to Top") {
            @Override
            void onClick() {
                Composition comp = ImageComponents.getActiveComp().get();
                comp.moveActiveLayerToTop();
            }
        };
        createMenuItem(moveToLast, layerStackSubmenu, CTRL_SHIFT_CLOSE_BRACKET);

        Action moveToFirstAction = new MenuAction("Layer to Bottom") {
            @Override
            void onClick() {
                Composition comp = ImageComponents.getActiveComp().get();
                comp.moveActiveLayerToBottom();
            }
        };
        createMenuItem(moveToFirstAction, layerStackSubmenu, CTRL_SHIFT_OPEN_BRACKET);

        layerStackSubmenu.addSeparator();

        Action moveSelectionUpAction = new MenuAction("Raise Layer Selection") {
            @Override
            void onClick() {
                Composition comp = ImageComponents.getActiveComp().get();
                comp.moveLayerSelectionUp();
            }
        };
        createMenuItem(moveSelectionUpAction, layerStackSubmenu, ALT_CLOSE_BRACKET);

        Action moveDownSelectionAction = new MenuAction("Lower Layer Selection") {
            @Override
            void onClick() {
                Composition comp = ImageComponents.getActiveComp().get();
                comp.moveLayerSelectionDown();
            }
        };
        createMenuItem(moveDownSelectionAction, layerStackSubmenu, ALT_OPEN_BRACKET);

        layersMenu.add(layerStackSubmenu);
    }

    private void initViewMenu(PixelitorWindow pixelitorWindow) {
        JMenu viewMenu = createMenu("View", 'V');

        viewMenu.add(ZoomMenu.INSTANCE);

        viewMenu.addSeparator();

        viewMenu.add(new ShowHideStatusBarAction());
        createMenuItem(new ShowHideHistogramsAction(), viewMenu, EnabledIf.ACTION_ENABLED, F6);
        createMenuItem(new ShowHideLayersAction(), viewMenu, EnabledIf.ACTION_ENABLED, F7);
        viewMenu.add(new ShowHideToolsAction());
        createMenuItem(ShowHideAllAction.INSTANCE, viewMenu, EnabledIf.ACTION_ENABLED, TAB);

        Action defaultWorkspaceAction = new MenuAction("Set Default Workspace") {
            @Override
            void onClick() {
                AppPreferences.WorkSpace.setDefault();
            }
        };
        createMenuItem(defaultWorkspaceAction, viewMenu, EnabledIf.ACTION_ENABLED);

        viewMenu.addSeparator();

        initArrangeWindowsSubmenu(viewMenu, pixelitorWindow);

        this.add(viewMenu);
    }

    private static void initArrangeWindowsSubmenu(JMenu viewMenu, PixelitorWindow pixelitorWindow) {
        JMenu arrangeWindowsSubmenu = new JMenu("Arrange Windows");

        Action cascadeWindowsAction = new MenuAction("Cascade") {
            @Override
            void onClick() {
                Desktop.INSTANCE.cascadeWindows();
            }
        };
        createMenuItem(cascadeWindowsAction, arrangeWindowsSubmenu);

        Action tileWindowsAction = new MenuAction("Tile") {
            @Override
            void onClick() {
                Desktop.INSTANCE.tileWindows();
            }
        };
        createMenuItem(tileWindowsAction, arrangeWindowsSubmenu);

        viewMenu.add(arrangeWindowsSubmenu);
    }

    private void initDevelopMenu(PixelitorWindow pixelitorWindow) {
        JMenu developMenu = createMenu("Develop", 'D');

        initDebugSubmenu(developMenu, pixelitorWindow);
        initTestSubmenu(developMenu, pixelitorWindow);
        initSplashSubmenu(developMenu);
        initExperimentalSubmenu(developMenu);

        Action newTextLayer = new MenuAction("New Text Layer...") {
            @Override
            void onClick() {
                String s = JOptionPane.showInputDialog(pixelitorWindow, "Text:", "Text Layer Text", JOptionPane.QUESTION_MESSAGE);
                Composition comp = ImageComponents.getActiveComp().get();
                TextLayer textLayer = new TextLayer(comp, "text layer", s);

                comp.addLayer(textLayer, AddToHistory.YES, true, false);
            }
        };
        createMenuItem(newTextLayer, developMenu);

        Action newAdjustmentLayer = new MenuAction("New Global Adjustment Layer...") {
            @Override
            void onClick() {
                Composition comp = ImageComponents.getActiveComp().get();
                AdjustmentLayer adjustmentLayer = new AdjustmentLayer(comp, "invert adjustment", new Invert());

                comp.addLayer(adjustmentLayer, AddToHistory.YES, true, false);
            }
        };
        createMenuItem(newAdjustmentLayer, developMenu);


        Action filterCreatorAction = new MenuAction("Filter Creator...") {
            @Override
            void onClick() {
                FilterCreator.showInDialog(pixelitorWindow);
            }
        };
        createMenuItem(filterCreatorAction, developMenu, EnabledIf.ACTION_ENABLED);

        Action debugSpecialAction = new MenuAction("Debug Special") {
            @Override
            void onClick() {
                BufferedImage img = ImageComponents.getActiveCompositeImage().get();

                Composite composite = new MultiplyComposite(1.0f);
//                Composite composite =  BlendComposite.Multiply;
//                Composite composite =  new TestComposite();

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
        };
        createMenuItem(debugSpecialAction, developMenu, EnabledIf.ACTION_ENABLED);

        Action addLayerMask = new MenuAction("Add Layer Mask") {
            @Override
            void onClick() {
                Layer layer = ImageComponents.getActiveLayer().get();
                layer.addTestLayerMask();
            }
        };
        createMenuItem(addLayerMask, developMenu);

        Action dumpEvents = new MenuAction("Dump Event Queue") {
            @Override
            void onClick() {
                DebugEventQueue.dump();
            }
        };
        createMenuItem(dumpEvents, developMenu);

        initLayerMaskSubmenu(developMenu);

        this.add(developMenu);
    }

    private static void initSplashSubmenu(JMenu developMenu) {
        JMenu splashMenu = new JMenu("Splash");

        Action splashScreenAction = new MenuAction("Create Splash Image") {
            @Override
            void onClick() {
                ImageTests.createSplashImage();
            }
        };
        createMenuItem(splashScreenAction, splashMenu, EnabledIf.ACTION_ENABLED);

        Action manySplashScreensAction = new MenuAction("Save Many Splash Images...") {
            @Override
            void onClick() {
                ImageTests.saveManySplashImages();
            }
        };
        createMenuItem(manySplashScreensAction, splashMenu, EnabledIf.ACTION_ENABLED);
        developMenu.add(splashMenu);
    }

    private static void initLayerMaskSubmenu(JMenu developMenu) {
        JMenu layerMaskSubMenu = new JMenu("Layer Mask");

        Action editLayerMask = new MenuAction("Edit Layer Mask") {
            @Override
            void onClick() {
                ImageComponent ic = ImageComponents.getActiveImageComponent();
                if (ic.getComp().getActiveLayer().hasLayerMask()) {
                    ic.setLayerMaskEditing(true);
                } else {
                    Dialogs.showInfoDialog("No Layer mask", "The active layer has no layer mask");
                }
            }
        };
        createMenuItem(editLayerMask, layerMaskSubMenu);

        Action editComposition = new MenuAction("Edit Composition") {
            @Override
            void onClick() {
                ImageComponent ic = ImageComponents.getActiveImageComponent();
                if (ic != null) {
                    ic.setLayerMaskEditing(false);
                }
            }
        };

        createMenuItem(editComposition, layerMaskSubMenu);

        developMenu.add(layerMaskSubMenu);
    }

    private static void initExperimentalSubmenu(JMenu developMenu) {
        JMenu experimentalSubmenu = new JMenu("Experimental");
        developMenu.add(experimentalSubmenu);

        createMenuItem(new MysticRose(), experimentalSubmenu);

        createMenuItem(new Droste(), experimentalSubmenu);

        createMenuItem(new Sphere3D(), experimentalSubmenu);

        createMenuItem(EnlargeCanvas.getAction(), experimentalSubmenu);


        createMenuItem(new Brick(), experimentalSubmenu);

        createMenuItem(new RenderGrid(), experimentalSubmenu);
        createMenuItem(new Lightning(), experimentalSubmenu);

        createMenuItem(new EmptyPolar(), experimentalSubmenu);

    }

    private static void initTestSubmenu(JMenu developMenu, PixelitorWindow pixelitorWindow) {
        JMenu testSubmenu = new JMenu("Test");

        createMenuItem(new ParamTest(), testSubmenu);

        Action randomResizeAction = new MenuAction("Random Resize") {
            @Override
            void onClick() {
                OpTests.randomResize();
            }
        };
        createMenuItem(randomResizeAction, testSubmenu, CTRL_ALT_R);

        Action randomToolAction = new MenuAction("1001 Brush & Shape Actions") {
            @Override
            void onClick() {
                ToolTests.randomToolActions(1001, false);
            }
        };
        createMenuItem(randomToolAction, testSubmenu);

        Action randomBrushAction = new MenuAction("1001 Brush Only Actions") {
            @Override
            void onClick() {
                ToolTests.randomToolActions(1001, true);
            }
        };
        createMenuItem(randomBrushAction, testSubmenu);


        Action robotTestAction = new MenuAction("Robot Test") {
            @Override
            void onClick() {
                RobotTest.runRobot();
            }
        };
        createMenuItem(robotTestAction, testSubmenu, EnabledIf.ACTION_ENABLED, CTRL_R);

        Action opPerformanceTestAction = new MenuAction("Filter Performance Test...") {
            @Override
            void onClick() {
                new PerformanceTestingDialog(pixelitorWindow);
            }
        };
        createMenuItem(opPerformanceTestAction, testSubmenu);

        Action findSlowestFilter = new MenuAction("Find Slowest Filter") {
            @Override
            void onClick() {
                OpTests.findSlowestFilter();
            }
        };
        createMenuItem(findSlowestFilter, testSubmenu);

        Action ciPerformanceTestAction = new MenuAction("getCompositeImage() Performance Test...") {
            @Override
            void onClick() {
                OpTests.getCompositeImagePerformanceTest();
            }
        };
        createMenuItem(ciPerformanceTestAction, testSubmenu);

        testSubmenu.addSeparator();

        Action runAllOps = new MenuAction("Run All Filters on Current Layer") {
            @Override
            void onClick() {
                OpTests.runAllFiltersOnCurrentLayer();
            }
        };
        createMenuItem(runAllOps, testSubmenu);

        Action saveAllOps = new MenuAction("Save the Result of Each Filter...") {
            @Override
            void onClick() {
                OpTests.saveTheResultOfEachFilter();
            }
        };
        createMenuItem(saveAllOps, testSubmenu);

        Action saveInAllFormats = new MenuAction("Save Current Image in All Formats...") {
            @Override
            void onClick() {
                OpenSaveManager.saveCurrentImageInAllFormats();
            }
        };
        createMenuItem(saveInAllFormats, testSubmenu);

        testSubmenu.addSeparator();

        Action testAllOnNewImg = new MenuAction("Test Layer Operations") {
            @Override
            void onClick() {
                ImageTests.testLayers();
            }
        };
        createMenuItem(testAllOnNewImg, testSubmenu, EnabledIf.ACTION_ENABLED);

        Action testTools = new MenuAction("Test Tools") {
            @Override
            void onClick() {
                ToolTests.testTools();
            }
        };
        createMenuItem(testTools, testSubmenu, EnabledIf.ACTION_ENABLED);

        Action testIOOverlayBlur = new MenuAction("IO Overlay Blur...") {
            @Override
            void onClick() {
                ImageTests.ioOverlayBlur();
            }
        };
        createMenuItem(testIOOverlayBlur, testSubmenu, EnabledIf.ACTION_ENABLED);

        developMenu.add(testSubmenu);
    }

    private static void initDebugSubmenu(JMenu develMenu, PixelitorWindow pixelitorWindow) {
        JMenu debugSubmenu = new JMenu("Debug");

        Action debugAppAction = new MenuAction("Debug App...") {
            @Override
            void onClick() {
                AppLogic.showDebugAppDialog();
            }
        };
        createMenuItem(debugAppAction, debugSubmenu, EnabledIf.ACTION_ENABLED);

        Action debugHistoryAction = new MenuAction("Debug History...") {
            @Override
            void onClick() {
                History.showHistory();
            }
        };
        createMenuItem(debugHistoryAction, debugSubmenu);

        Action repaintActive = new MenuAction("repaint() on the active image") {
            @Override
            void onClick() {
                ImageComponents.repaintActive();
            }
        };
        createMenuItem(repaintActive, debugSubmenu);

        Action imageChangedActive = new MenuAction("imageChanged(FULL) on the active image") {
            @Override
            void onClick() {
                ImageComponents.getActiveComp().get().imageChanged(FULL);
            }
        };
        createMenuItem(imageChangedActive, debugSubmenu);

        Action revalidateActive = new MenuAction("revalidate() the main window") {
            @Override
            void onClick() {
                pixelitorWindow.getContentPane().revalidate();
            }
        };
        createMenuItem(revalidateActive, debugSubmenu, EnabledIf.ACTION_ENABLED);

        Action resetLayerTranslation = new MenuAction("reset the translation of current layer") {
            @Override
            void onClick() {
                Composition comp = ImageComponents.getActiveComp().get();
                Layer layer = comp.getActiveLayer();
                if (layer instanceof ContentLayer) {
                    ContentLayer contentLayer = (ContentLayer) layer;
                    contentLayer.setTranslationX(0);
                    contentLayer.setTranslationY(0);
                    comp.imageChanged(FULL);
                }
            }
        };
        createMenuItem(resetLayerTranslation, debugSubmenu);

        Action updateHistogram = new MenuAction("Update Histograms") {
            @Override
            void onClick() {
                Composition comp = ImageComponents.getActiveComp().get();
                HistogramsPanel.INSTANCE.updateFromCompIfShown(comp);
            }
        };
        createMenuItem(updateHistogram, debugSubmenu);

        Action saveAllImagesToDir = new MenuAction("Save All Images to Folder...") {
            @Override
            void onClick() {
                OpenSaveManager.saveAllImagesToDir();
            }
        };
        createMenuItem(saveAllImagesToDir, debugSubmenu);

        Action debugImageLayerImages = new MenuAction("Debug ImageLayer Images") {
            @Override
            void onClick() {
                Optional<ImageLayer> layer = ImageComponents.getActiveImageLayer();
                layer.get().debugImages();
            }
        };
        createMenuItem(debugImageLayerImages, debugSubmenu);

        develMenu.add(debugSubmenu);
    }

    private void initHelpMenu(PixelitorWindow pixelitorWindow) {
        JMenu helpMenu = createMenu("Help", 'H');

        Action tipOfTheDayAction = new MenuAction("Tip of the Day") {
            @Override
            void onClick() {
                TipsOfTheDay.showTips(pixelitorWindow, true);
            }
        };
        createMenuItem(tipOfTheDayAction, helpMenu, EnabledIf.ACTION_ENABLED);

//        JMenu webSubMenu = new JMenu("Web");
//        MenuFactory.createMenuItem(new OpenInBrowserAction("Ask for Help", "https://sourceforge.net/projects/pixelitor/forums/forum/1034234"), null, webSubMenu, MenuEnableCondition.ACTION_ENABLED);
//        MenuFactory.createMenuItem(new OpenInBrowserAction("Discuss Pixelitor", "https://sourceforge.net/projects/pixelitor/forums/forum/1034233"), null, webSubMenu, MenuEnableCondition.ACTION_ENABLED);
//        MenuFactory.createMenuItem(new OpenInBrowserAction("Report a Bug", "https://sourceforge.net/tracker/?group_id=285935&atid=1211793"), null, webSubMenu, MenuEnableCondition.ACTION_ENABLED);
//        helpMenu.add(webSubMenu);

        helpMenu.addSeparator();

        Action checkForUpdateAction = new MenuAction("Check for Update...") {
            @Override
            void onClick() {
                UpdatesCheck.checkForUpdates();
            }
        };
        helpMenu.add(checkForUpdateAction);

        Action aboutAction = new MenuAction("About") {
            @Override
            void onClick() {
                AboutDialog.showDialog(pixelitorWindow);
            }
        };
        createMenuItem(aboutAction, helpMenu, EnabledIf.ACTION_ENABLED);

        this.add(helpMenu);
    }

    private static JMenu createMenu(String s, char c) {
        JMenu menu = new JMenu(s);
        menu.setMnemonic(c);
        return menu;
    }

    private static void createMenuItem(Action a, JMenu parent, EnabledIf whenToEnable, KeyStroke keyStroke) {
        JMenuItem menuItem = whenToEnable.getMenuItem(a);
        parent.add(menuItem);
        if (keyStroke != null) {
            menuItem.setAccelerator(keyStroke);
        }
    }

    private static void createMenuItem(Action action, JMenu parent, EnabledIf whenToEnable) {
        createMenuItem(action, parent, whenToEnable, null);
    }

    public static void createMenuItem(Action action, JMenu parent, KeyStroke keyStroke) {
        createMenuItem(action, parent, EnabledIf.THERE_IS_OPEN_IMAGE, keyStroke);
    }

    public static void createMenuItem(Action action, JMenu parent) {
        createMenuItem(action, parent, EnabledIf.THERE_IS_OPEN_IMAGE, null);
    }

    private static abstract class MenuAction extends AbstractAction {
        public MenuAction(String name) {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                onClick();
            } catch (Exception ex) {
                Dialogs.showExceptionDialog(ex);
            }
        }

        abstract void onClick();
    }
}
