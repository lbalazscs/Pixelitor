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
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.Desktop;
import pixelitor.FgBgColors;
import pixelitor.ImageComponent;
import pixelitor.ImageComponents;
import pixelitor.ImageDisplay;
import pixelitor.NewImage;
import pixelitor.PixelitorWindow;
import pixelitor.TipsOfTheDay;
import pixelitor.automate.AutoPaint;
import pixelitor.automate.BatchFilterWizard;
import pixelitor.automate.BatchResize;
import pixelitor.filters.*;
import pixelitor.filters.animation.TweenWizard;
import pixelitor.filters.comp.EnlargeCanvas;
import pixelitor.filters.comp.Flip;
import pixelitor.filters.comp.Rotate;
import pixelitor.filters.convolve.Convolve;
import pixelitor.filters.gui.ResizePanel;
import pixelitor.filters.jhlabsproxies.*;
import pixelitor.filters.levels.Levels;
import pixelitor.filters.levels.Levels2;
import pixelitor.filters.lookup.ColorBalance;
import pixelitor.filters.lookup.Luminosity;
import pixelitor.filters.painters.TextFilter;
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
import pixelitor.tools.brushes.CopyBrush;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.FilterCreator;
import pixelitor.utils.HistogramsPanel;
import pixelitor.utils.Messages;
import pixelitor.utils.PerformanceTestingDialog;
import pixelitor.utils.Tests3x3;
import pixelitor.utils.UpdateGUI;
import pixelitor.utils.Utils;
import pixelitor.utils.test.DebugEventQueue;
import pixelitor.utils.test.ImageTests;
import pixelitor.utils.test.OpTests;
import pixelitor.utils.test.RobotTest;
import pixelitor.utils.test.ToolTests;

import javax.swing.*;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementFactory;
import java.util.Optional;

import static pixelitor.Composition.ImageChangeActions.FULL;
import static pixelitor.FillType.BACKGROUND;
import static pixelitor.FillType.FOREGROUND;
import static pixelitor.FillType.TRANSPARENT;
import static pixelitor.filters.comp.Flip.Direction.HORIZONTAL;
import static pixelitor.filters.comp.Flip.Direction.VERTICAL;
import static pixelitor.filters.comp.Rotate.SpecialAngle.ANGLE_180;
import static pixelitor.filters.comp.Rotate.SpecialAngle.ANGLE_270;
import static pixelitor.filters.comp.Rotate.SpecialAngle.ANGLE_90;
import static pixelitor.filters.jhlabsproxies.JHMotionBlur.Mode.MOTION_BLUR;
import static pixelitor.filters.jhlabsproxies.JHMotionBlur.Mode.SPIN_ZOOM_BLUR;
import static pixelitor.menus.MenuAction.AllowedLayerType.HAS_LAYER_MASK;
import static pixelitor.menus.MenuAction.AllowedLayerType.IS_TEXT_LAYER;

/**
 * The menu bar of the app
 */
public class MenuBar extends JMenuBar {

    private static final KeyStroke CTRL_B = KeyStroke.getKeyStroke('B', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_C = KeyStroke.getKeyStroke('C', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_D = KeyStroke.getKeyStroke('D', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_E = KeyStroke.getKeyStroke('E', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_F = KeyStroke.getKeyStroke('F', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_G = KeyStroke.getKeyStroke('G', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_H = KeyStroke.getKeyStroke('H', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_I = KeyStroke.getKeyStroke('I', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_J = KeyStroke.getKeyStroke('J', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_K = KeyStroke.getKeyStroke('K', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_L = KeyStroke.getKeyStroke('L', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_N = KeyStroke.getKeyStroke('N', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_O = KeyStroke.getKeyStroke('O', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_R = KeyStroke.getKeyStroke('R', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_S = KeyStroke.getKeyStroke('S', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_T = KeyStroke.getKeyStroke('T', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_U = KeyStroke.getKeyStroke('U', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_V = KeyStroke.getKeyStroke('V', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_Z = KeyStroke.getKeyStroke('Z', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_W = KeyStroke.getKeyStroke('W', InputEvent.CTRL_MASK);

    private static final KeyStroke CTRL_1 = KeyStroke.getKeyStroke('1', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_2 = KeyStroke.getKeyStroke('2', InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_3 = KeyStroke.getKeyStroke('3', InputEvent.CTRL_MASK);

    private static final KeyStroke CTRL_SHIFT_S = KeyStroke.getKeyStroke('S', InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK);
    private static final KeyStroke CTRL_ALT_W = KeyStroke.getKeyStroke('W', InputEvent.CTRL_MASK | InputEvent.ALT_MASK);
    private static final KeyStroke CTRL_SHIFT_Z = KeyStroke.getKeyStroke('Z', InputEvent.SHIFT_MASK + InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_SHIFT_F = KeyStroke.getKeyStroke('F', InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK);
    private static final KeyStroke CTRL_SHIFT_C = KeyStroke.getKeyStroke('C', InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK);
    private static final KeyStroke CTRL_SHIFT_V = KeyStroke.getKeyStroke('V', InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK);
    private static final KeyStroke CTRL_ALT_I = KeyStroke.getKeyStroke('I', InputEvent.CTRL_MASK | InputEvent.ALT_MASK);
    private static final KeyStroke ALT_BACKSPACE = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, InputEvent.ALT_MASK);
    private static final KeyStroke CTRL_BACKSPACE = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_SHIFT_I = KeyStroke.getKeyStroke('I', InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK);
    private static final KeyStroke T = KeyStroke.getKeyStroke('T');
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
    private static final KeyStroke CTRL_ALT_R = KeyStroke.getKeyStroke('R', InputEvent.CTRL_MASK + InputEvent.ALT_MASK);



    public MenuBar(PixelitorWindow pw) {
        initFileMenu(pw);
        initEditMenu();
        initLayerMenu(pw);
        initSelectionMenu();
        initColorsMenu();
        initFilterMenu();
        initViewMenu();

        if (Build.CURRENT != Build.FINAL) {
            initDevelopMenu(pw);
        }

        initHelpMenu(pw);
    }

    private void initFileMenu(PixelitorWindow pixelitorWindow) {
        JMenu fileMenu = createMenu("File", 'F');

        // new image
        createMenuItem(NewImage.getAction(), fileMenu, EnabledIf.ACTION_ENABLED, CTRL_N);

        // open
        createMenuItem(new MenuAction("Open...") {
            @Override
            public void onClick() {
                FileChoosers.open();
            }
        }, fileMenu, EnabledIf.ACTION_ENABLED, CTRL_O);

        // recent files
        JMenu recentFiles = RecentFilesMenu.getInstance();
        fileMenu.add(recentFiles);

        fileMenu.addSeparator();

        // save
        createMenuItem(new MenuAction("Save") {
            @Override
            public void onClick() {
                OpenSaveManager.save(false);
            }
        }, fileMenu, CTRL_S);

        // save as
        createMenuItem(new MenuAction("Save As...") {
            @Override
            public void onClick() {
                OpenSaveManager.save(true);
            }
        }, fileMenu, CTRL_SHIFT_S);

        createMenuItem(new MenuAction("Export Optimized JPEG...") {
            @Override
            public void onClick() {
                Composition comp = ImageComponents.getActiveComp().get();
                BufferedImage image = comp.getCompositeImage();
                OptimizedJpegSavePanel.showInDialog(image, pixelitorWindow);
            }
        }, fileMenu);

        createMenuItem(new MenuAction("Export OpenRaster...") {
            @Override
            public void onClick() {
                OpenRasterExportPanel.showInDialog(pixelitorWindow);
            }
        }, fileMenu);

        fileMenu.addSeparator();

        createMenuItem(new MenuAction("Export Layer Animation...") {
            @Override
            public void onClick() {
                AnimGifExport.start(pixelitorWindow);
            }
        }, fileMenu);

        createMenuItem(new MenuAction("Export Tweening Animation...") {
            @Override
            public void onClick() {
                new TweenWizard().start(pixelitorWindow);
            }
        }, fileMenu);

        fileMenu.addSeparator();

        // close
        createMenuItem(new MenuAction("Close") {
            @Override
            public void onClick() {
                OpenSaveManager.warnAndCloseImage(ImageComponents.getActiveIC());
            }
        }, fileMenu, CTRL_W);

        // close all
        createMenuItem(new MenuAction("Close All") {
            @Override
            public void onClick() {
                OpenSaveManager.warnAndCloseAllImages();
            }
        }, fileMenu, CTRL_ALT_W);

        initAutomateSubmenu(fileMenu, pixelitorWindow);

        if (!JVM.isMac) {
            createMenuItem(new ScreenCaptureAction(),
                    fileMenu, EnabledIf.ACTION_ENABLED);
        }

        fileMenu.addSeparator();

        // exit
        String exitName = JVM.isMac ? "Quit" : "Exit";
        createMenuItem(new MenuAction(exitName) {
            @Override
            public void onClick() {
                AppLogic.exitApp(pixelitorWindow);
            }
        }, fileMenu, EnabledIf.ACTION_ENABLED);

        this.add(fileMenu);
    }

    private static void initAutomateSubmenu(JMenu fileMenu, PixelitorWindow pixelitorWindow) {
        JMenu batchSubmenu = new JMenu("Automate");
        fileMenu.add(batchSubmenu);

        createMenuItem(new MenuAction("Batch Resize...") {
            @Override
            public void onClick() {
                BatchResize.start();
            }
        }, batchSubmenu, EnabledIf.ACTION_ENABLED);

        createMenuItem(new MenuAction("Batch Filter...") {
            @Override
            public void onClick() {
                new BatchFilterWizard().start(pixelitorWindow);
            }
        }, batchSubmenu, EnabledIf.ACTION_ENABLED);

        createMenuItem(new MenuAction("Export Layers to PNG...") {
            @Override
            public void onClick() {
                OpenSaveManager.exportLayersToPNG();
            }
        }, batchSubmenu, EnabledIf.THERE_IS_OPEN_IMAGE);

        createMenuItem(new MenuAction("Auto Paint...") {
            @Override
            public void onClick() {
                AutoPaint.showDialog();
            }
        }, batchSubmenu);

    }

    private void initEditMenu() {
        JMenu editMenu = createMenu("Edit", 'E');

        // last op
        createMenuItem(RepeatLast.INSTANCE, editMenu, EnabledIf.CAN_REPEAT_OPERATION, CTRL_F);
        editMenu.addSeparator();

        // undo
        createMenuItem(History.UNDO_ACTION, editMenu, EnabledIf.UNDO_POSSIBLE, CTRL_Z);

        // redo
        createMenuItem(History.REDO_ACTION, editMenu, EnabledIf.REDO_POSSIBLE, CTRL_SHIFT_Z);

        // fade
        createMenuItem(new Fade(), editMenu, EnabledIf.FADING_POSSIBLE, CTRL_SHIFT_F);

        editMenu.addSeparator();

        // copy
        createMenuItem(new CopyAction(CopySource.LAYER), editMenu, CTRL_C);
        createMenuItem(new CopyAction(CopySource.COMPOSITE), editMenu, CTRL_SHIFT_C);
        // paste
        createMenuItem(new PasteAction(PasteDestination.NEW_IMAGE), editMenu, EnabledIf.ACTION_ENABLED, CTRL_V);
        createMenuItem(new PasteAction(PasteDestination.NEW_LAYER), editMenu, CTRL_SHIFT_V);

        editMenu.addSeparator();

        // resize
        createMenuItem(new MenuAction("Resize...") {
            @Override
            public void onClick() {
                ResizePanel.resizeActiveImage();
            }
        }, editMenu, CTRL_ALT_I);

        createMenuItem(SelectionActions.getCropAction(), editMenu, EnabledIf.ACTION_ENABLED);
        createMenuItem(EnlargeCanvas.getAction(), editMenu);
        createRotateFlipSubmenu(editMenu);

        editMenu.addSeparator();

        // preferences
        Action preferencesAction = new MenuAction("Preferences...") {
            @Override
            public void onClick() {
                AppPreferences.Panel.showInDialog();
            }
        };
        editMenu.add(preferencesAction);

        this.add(editMenu);
    }

    private void createRotateFlipSubmenu(JMenu editMenu) {
        JMenu rotateSubmenu = new JMenu("Rotate/Flip");
        editMenu.add(rotateSubmenu);
        // rotate
        createMenuItem(new Rotate(ANGLE_90), rotateSubmenu);
        createMenuItem(new Rotate(ANGLE_180), rotateSubmenu);
        createMenuItem(new Rotate(ANGLE_270), rotateSubmenu);
        rotateSubmenu.addSeparator();
        // flip
        createMenuItem(new Flip(HORIZONTAL), rotateSubmenu);
        createMenuItem(new Flip(VERTICAL), rotateSubmenu);
    }

    private void initColorsMenu() {
        JMenu colorsMenu = createMenu("Colors", 'C');

        createMenuItem(new ColorBalance(), colorsMenu, CTRL_B);
        createMenuItem(new HueSat(), colorsMenu, CTRL_U);
        createMenuItem(new Colorize(), colorsMenu);
        createMenuItem(new Levels(), colorsMenu, CTRL_L);
        if (Build.advancedLayersEnabled()) {
            createMenuItem(new Levels2(), colorsMenu);
        }
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

    private void initSelectionMenu() {
        JMenu selectMenu = createMenu("Selection", 'S');

        createMenuItem(SelectionActions.getDeselectAction(), selectMenu, EnabledIf.ACTION_ENABLED, CTRL_D);

        createMenuItem(SelectionActions.getInvertSelectionAction(), selectMenu, EnabledIf.ACTION_ENABLED, CTRL_SHIFT_I);
        createMenuItem(SelectionActions.getModifyAction(), selectMenu, EnabledIf.ACTION_ENABLED);

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

        KeyStroke keyStroke = T;
        if (Build.advancedLayersEnabled()) {
            keyStroke = null;
        }
        createMenuItem(TextFilter.INSTANCE, filterMenu, keyStroke);

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
        createMenuItem(new Voronoi(), renderSubmenu);
        createMenuItem(new FractalTree(), renderSubmenu);

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
        createMenuItem(new RandomFilter(), otherFiltersSubmenu);
        createMenuItem(new TransformLayer(), otherFiltersSubmenu);

        createMenuItem(new Convolve(3), otherFiltersSubmenu);
        createMenuItem(new Convolve(5), otherFiltersSubmenu);

        createMenuItem(new JHDropShadow(), otherFiltersSubmenu);
        createMenuItem(new Transition2D(), otherFiltersSubmenu);

        createMenuItem(new ChannelToTransparency(), otherFiltersSubmenu);

        filterMenu.add(otherFiltersSubmenu);
    }

    private static void initArtisticSubmenu(JMenu filterMenu) {
        JMenu artisticFiltersSubmenu = new JMenu("Artistic");
        createMenuItem(new JHCrystallize(), artisticFiltersSubmenu);
        createMenuItem(new JHPointillize(), artisticFiltersSubmenu);
        createMenuItem(new JHStamp(), artisticFiltersSubmenu);
        createMenuItem(new JHOilPainting(), artisticFiltersSubmenu);
        createMenuItem(new RandomSpheres(), artisticFiltersSubmenu);
        createMenuItem(new JHSmear(), artisticFiltersSubmenu);
        createMenuItem(new JHEmboss(), artisticFiltersSubmenu);
        createMenuItem(new Orton(), artisticFiltersSubmenu);
        createMenuItem(new PhotoCollage(), artisticFiltersSubmenu);
        createMenuItem(new JHWeave(), artisticFiltersSubmenu);

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
        createMenuItem(new DrunkVision(), dislocateSubmenu);
        createMenuItem(new JHVideoFeedback(), dislocateSubmenu);
        createMenuItem(new JHOffset(), dislocateSubmenu);
        createMenuItem(new Slice(), dislocateSubmenu);
        createMenuItem(new Mirror(), dislocateSubmenu);
    }

    private void initLayerMenu(PixelitorWindow pw) {
        JMenu layersMenu = createMenu("Layer", 'L');

        layersMenu.add(AddNewLayerAction.INSTANCE);
        layersMenu.add(DeleteActiveLayerAction.INSTANCE);

        createMenuItem(new MenuAction("Flatten Image") {
            @Override
            public void onClick() {
                Composition comp = ImageComponents.getActiveComp().get();
                comp.flattenImage(UpdateGUI.YES);
            }
        }, layersMenu);

        createMenuItem(new MenuAction("Merge Down") {
            @Override
            public void onClick() {
                Composition comp = ImageComponents.getActiveComp().get();
                comp.mergeDown(UpdateGUI.YES);
            }
        }, layersMenu, CTRL_E);

        createMenuItem(DuplicateLayerAction.INSTANCE, layersMenu, CTRL_J);

        createMenuItem(new MenuAction("New Layer from Composite") {
            @Override
            public void onClick() {
                Composition comp = ImageComponents.getActiveComp().get();
                comp.addNewLayerFromComposite("Composite");
            }
        }, layersMenu, CTRL_SHIFT_ALT_E);

        createMenuItem(new MenuAction("Layer to Canvas Size") {
            @Override
            public void onClick() {
                Composition comp = ImageComponents.getActiveComp().get();
                comp.activeLayerToCanvasSize();
            }
        }, layersMenu);

        initLayerStackSubmenu(layersMenu);

        if (Build.advancedLayersEnabled()) {
            initLayerMaskSubmenu(layersMenu);
            initTextLayerSubmenu(layersMenu, pw);
            initAdjustmentLayersSubmenu(layersMenu);

            createMenuItem(new MenuAction("Show Pixelitor Internal State...") {
                @Override
                public void onClick() {
                    Messages.showDebugAppDialog();
                }
            }, layersMenu, EnabledIf.ACTION_ENABLED);
        }

        this.add(layersMenu);
    }

    private void initAdjustmentLayersSubmenu(JMenu parentMenu) {
        JMenu adjustmentLayersSubMenu = new JMenu("New Adjustment Layer");

        createMenuItem(new MenuAction("Invert Adjustment") { // TODO not "Invert" because of assertj test lookup confusion
            @Override
            public void onClick() {
                AddAdjLayerAction.INSTANCE.actionPerformed(null);
            }
        }, adjustmentLayersSubMenu);

        parentMenu.add(adjustmentLayersSubMenu);
    }

    private static void initLayerMaskSubmenu(JMenu layerMenu) {
        JMenu layerMaskSubMenu = new JMenu("Layer Mask");

        createMenuItem(new MenuAction("Add White (Reveal All)") {
            @Override
            public void onClick() {
                Layer layer = ImageComponents.getActiveLayer().get();
                layer.addMask(LayerMaskAddType.REVEAL_ALL);
            }
        }, layerMaskSubMenu, CTRL_G);

        createMenuItem(new MenuAction("Add Black (Hide All)") {
            @Override
            public void onClick() {
                Layer layer = ImageComponents.getActiveLayer().get();
                layer.addMask(LayerMaskAddType.HIDE_ALL);
            }
        }, layerMaskSubMenu);

        createMenuItem(new MenuAction("Add from Selection") {
            @Override
            public void onClick() {
                Layer layer = ImageComponents.getActiveLayer().get();
                layer.addMask(LayerMaskAddType.REVEAL_SELECTION);
            }
        }, layerMaskSubMenu);

        createMenuItem(new MenuAction("Delete", HAS_LAYER_MASK) {
            @Override
            public void onClick() {
                ImageDisplay ic = ImageComponents.getActiveIC();
                Layer layer = ic.getComp().getActiveLayer();

                layer.deleteMask(AddToHistory.YES, true);

                layer.getComp().imageChanged(FULL);
            }
        }, layerMaskSubMenu);

        createMenuItem(new MenuAction("Apply", HAS_LAYER_MASK) {
            @Override
            public void onClick() {
                ImageDisplay ic = ImageComponents.getActiveIC();
                Layer layer = ic.getComp().getActiveLayer();

                if (!(layer instanceof ImageLayer)) {
                    Messages.showNotImageLayerError();
                    return;
                }

                ((ImageLayer) layer).applyLayerMask(AddToHistory.YES);

                ic.setShowLayerMask(false);
                FgBgColors.setLayerMaskEditing(false);

                layer.getComp().imageChanged(FULL);
            }
        }, layerMaskSubMenu);

        layerMaskSubMenu.addSeparator();

        createMenuItem(new MenuAction("Show and Edit Composition") {
            @Override
            public void onClick() {
                ImageDisplay ic = ImageComponents.getActiveIC();
                Layer activeLayer = ic.getComp().getActiveLayer();
                ic.setShowLayerMask(false);
                FgBgColors.setLayerMaskEditing(false);
                activeLayer.setMaskEditing(false);
            }
        }, layerMaskSubMenu, CTRL_1);

        createMenuItem(new MenuAction("Show and Edit Mask", HAS_LAYER_MASK) {
            @Override
            public void onClick() {
                ImageDisplay ic = ImageComponents.getActiveIC();
                Layer activeLayer = ic.getComp().getActiveLayer();
                ic.setShowLayerMask(true);
                FgBgColors.setLayerMaskEditing(true);
                activeLayer.setMaskEditing(true);
            }
        }, layerMaskSubMenu, CTRL_2);

        createMenuItem(new MenuAction("Show Composition, but Edit Mask", HAS_LAYER_MASK) {
            @Override
            public void onClick() {
                ImageDisplay ic = ImageComponents.getActiveIC();
                Layer activeLayer = ic.getComp().getActiveLayer();
                ic.setShowLayerMask(false);
                FgBgColors.setLayerMaskEditing(true);
                activeLayer.setMaskEditing(true);
            }
        }, layerMaskSubMenu, CTRL_3);

        layerMenu.add(layerMaskSubMenu);
    }

    private static void initLayerStackSubmenu(JMenu layersMenu) {
        JMenu layerStackSubmenu = new JMenu("Layer Stack");

        createMenuItem(LayerMoveAction.INSTANCE_UP, layerStackSubmenu, CTRL_CLOSE_BRACKET);

        createMenuItem(LayerMoveAction.INSTANCE_DOWN, layerStackSubmenu, CTRL_OPEN_BRACKET);

        createMenuItem(new MenuAction("Layer to Top") {
            @Override
            public void onClick() {
                Composition comp = ImageComponents.getActiveComp().get();
                comp.moveActiveLayerToTop();
            }
        }, layerStackSubmenu, CTRL_SHIFT_CLOSE_BRACKET);

        createMenuItem(new MenuAction("Layer to Bottom") {
            @Override
            public void onClick() {
                Composition comp = ImageComponents.getActiveComp().get();
                comp.moveActiveLayerToBottom();
            }
        }, layerStackSubmenu, CTRL_SHIFT_OPEN_BRACKET);

        layerStackSubmenu.addSeparator();

        createMenuItem(new MenuAction("Raise Layer Selection") {
            @Override
            public void onClick() {
                Composition comp = ImageComponents.getActiveComp().get();
                comp.moveLayerSelectionUp();
            }
        }, layerStackSubmenu, ALT_CLOSE_BRACKET);

        createMenuItem(new MenuAction("Lower Layer Selection") {
            @Override
            public void onClick() {
                Composition comp = ImageComponents.getActiveComp().get();
                comp.moveLayerSelectionDown();
            }
        }, layerStackSubmenu, ALT_OPEN_BRACKET);

        layersMenu.add(layerStackSubmenu);
    }

    private void initViewMenu() {
        JMenu viewMenu = createMenu("View", 'V');

        viewMenu.add(ZoomMenu.INSTANCE);

        viewMenu.addSeparator();

        createMenuItem(new MenuAction("Show History...") {
            @Override
            public void onClick() {
                History.showHistory();
            }
        }, viewMenu, CTRL_H);

        viewMenu.add(new ShowHideStatusBarAction());
        createMenuItem(new ShowHideHistogramsAction(), viewMenu, EnabledIf.ACTION_ENABLED, F6);
        createMenuItem(new ShowHideLayersAction(), viewMenu, EnabledIf.ACTION_ENABLED, F7);
        viewMenu.add(new ShowHideToolsAction());
        createMenuItem(ShowHideAllAction.INSTANCE, viewMenu, EnabledIf.ACTION_ENABLED, TAB);

        createMenuItem(new MenuAction("Set Default Workspace") {
            @Override
            public void onClick() {
                AppPreferences.WorkSpace.setDefault();
            }
        }, viewMenu, EnabledIf.ACTION_ENABLED);

        JCheckBoxMenuItem showPixelGridMI = new JCheckBoxMenuItem("Show Pixel Grid");
        showPixelGridMI.addActionListener(e -> {
            ImageComponent.showPixelGrid = showPixelGridMI.getState();
            ImageComponents.repaintAll();
        });
        viewMenu.add(showPixelGridMI);

        viewMenu.addSeparator();

        initArrangeWindowsSubmenu(viewMenu);

        this.add(viewMenu);
    }

    private static void initArrangeWindowsSubmenu(JMenu viewMenu) {
        JMenu arrangeWindowsSubmenu = new JMenu("Arrange Windows");

        createMenuItem(new MenuAction("Cascade") {
            @Override
            public void onClick() {
                Desktop.INSTANCE.cascadeWindows();
            }
        }, arrangeWindowsSubmenu);

        createMenuItem(new MenuAction("Tile") {
            @Override
            public void onClick() {
                Desktop.INSTANCE.tileWindows();
            }
        }, arrangeWindowsSubmenu);

        viewMenu.add(arrangeWindowsSubmenu);
    }

    private void initDevelopMenu(PixelitorWindow pw) {
        JMenu developMenu = createMenu("Develop", 'D');

        initDebugSubmenu(developMenu, pw);
        initTestSubmenu(developMenu, pw);
        initSplashSubmenu(developMenu);
        initExperimentalSubmenu(developMenu);

        createMenuItem(new MenuAction("Filter Creator...") {
            @Override
            public void onClick() {
                FilterCreator.showInDialog(pw);
            }
        }, developMenu, EnabledIf.ACTION_ENABLED);

        createMenuItem(new MenuAction("Debug Special") {
            @Override
            public void onClick() {
                BufferedImage img = ImageComponents.getActiveCompositeImage().get();

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
        }, developMenu, EnabledIf.ACTION_ENABLED);

        createMenuItem(new MenuAction("Dump Event Queue") {
            @Override
            public void onClick() {
                DebugEventQueue.dump();
            }
        }, developMenu);

        createMenuItem(new MenuAction("Debug PID") {
            @Override
            public void onClick() {
                String vmRuntimeInfo = ManagementFactory.getRuntimeMXBean().getName();
                System.out.println(String.format("MenuBar::onClick: vmRuntimeInfo = '%s'", vmRuntimeInfo));
            }
        }, developMenu);

        createMenuItem(new MenuAction("Debug Layer Mask") {
            @Override
            public void onClick() {
                ImageLayer imageLayer = (ImageLayer) ImageComponents.getActiveLayer().get();
                Utils.debugImage(imageLayer.getImage(), "layer image");

                if (imageLayer.hasMask()) {
                    LayerMask layerMask = imageLayer.getMask();
                    BufferedImage maskImage = layerMask.getImage();
                    Utils.debugImage(maskImage, "mask image");

                    BufferedImage transparencyImage = layerMask.getTransparencyImage();
                    Utils.debugImage(transparencyImage, "transparency image");
                }
            }
        }, developMenu);

        createMenuItem(new MenuAction("Update from Mask") {
            @Override
            public void onClick() {
                ImageLayer imageLayer = (ImageLayer) ImageComponents.getActiveLayer().get();
                if (imageLayer.hasMask()) {
                    imageLayer.getMask().updateFromBWImage();
                } else {
                    Messages.showInfo("No Mask in Current image", "Error");
                }
            }
        }, developMenu);

        createMenuItem(new MenuAction("imageChanged(FULL) on the active image") {
            @Override
            public void onClick() {
                ImageComponents.getActiveComp().get().imageChanged(FULL);
            }
        }, developMenu);

        createMenuItem(new MenuAction("Debug getCanvasSizedSubImage") {
            @Override
            public void onClick() {
                BufferedImage bi = ImageComponents.getActiveImageLayerOrMask().get().getCanvasSizedSubImage();
                Utils.debugImage(bi);
            }
        }, developMenu);

        createMenuItem(new MenuAction("Tests3x3.dumpCompositeOfActive()") {
            @Override
            public void onClick() {
                Tests3x3.dumpCompositeOfActive();
            }
        }, developMenu);

        createMenuItem(new MenuAction("Debug translation and canvas") {
            @Override
            public void onClick() {
                Composition comp = ImageComponents.getActiveIC().getComp();
                Layer layer = comp.getActiveLayer();
                if (layer instanceof ImageLayer) {
                    ImageLayer imageLayer = (ImageLayer) layer;
                    String s = imageLayer.toDebugCanvasString();
                    System.out.println(s);
                }
            }
        }, developMenu);

        createMenuItem(new MenuAction("Debug Copy Brush") {
            @Override
            public void onClick() {
                CopyBrush.setDebugBrushImage(true);
            }
        }, developMenu);

        this.add(developMenu);
    }

    private void initTextLayerSubmenu(JMenu parentMenu, PixelitorWindow pw) {
        JMenu textLayerMenu = new JMenu("Text Layer");

        createMenuItem(new MenuAction("New...") {
            @Override
            public void onClick() {
                TextLayer.createNew(pw);
            }
        }, textLayerMenu, T);

        createMenuItem(new MenuAction("Edit...", IS_TEXT_LAYER) {
            @Override
            public void onClick() {
                Composition comp = ImageComponents.getActiveIC().getComp();
                Layer layer = comp.getActiveLayer();
                TextLayer textLayer = (TextLayer) layer;
                textLayer.edit(pw);
            }
        }, textLayerMenu, CTRL_T);

        createMenuItem(new MenuAction("Rasterize", IS_TEXT_LAYER) {
            @Override
            public void onClick() {
                Composition comp = ImageComponents.getActiveComp().get();
                TextLayer.replaceWithRasterized(comp);
            }
        }, textLayerMenu);

        parentMenu.add(textLayerMenu);
    }

    private static void initSplashSubmenu(JMenu developMenu) {
        JMenu splashMenu = new JMenu("Splash");

        createMenuItem(new MenuAction("Create Splash Image") {
            @Override
            public void onClick() {
                ImageTests.createSplashImage();
            }
        }, splashMenu, EnabledIf.ACTION_ENABLED);

        createMenuItem(new MenuAction("Save Many Splash Images...") {
            @Override
            public void onClick() {
                ImageTests.saveManySplashImages();
            }
        }, splashMenu, EnabledIf.ACTION_ENABLED);

        developMenu.add(splashMenu);
    }

    private static void initExperimentalSubmenu(JMenu developMenu) {
        JMenu experimentalSubmenu = new JMenu("Experimental");
        developMenu.add(experimentalSubmenu);

        createMenuItem(new MysticRose(), experimentalSubmenu);

        createMenuItem(new Droste(), experimentalSubmenu);

        createMenuItem(new Sphere3D(), experimentalSubmenu);

        createMenuItem(new Brick(), experimentalSubmenu);

        createMenuItem(new RenderGrid(), experimentalSubmenu);
        createMenuItem(new Lightning(), experimentalSubmenu);

        createMenuItem(new EmptyPolar(), experimentalSubmenu);
        createMenuItem(new JHCheckerFilter(), experimentalSubmenu);
    }

    private static void initTestSubmenu(JMenu developMenu, PixelitorWindow pixelitorWindow) {
        JMenu testSubmenu = new JMenu("Test");

        createMenuItem(new ParamTest(), testSubmenu);

        createMenuItem(new MenuAction("Random Resize") {
            @Override
            public void onClick() {
                OpTests.randomResize();
            }
        }, testSubmenu, CTRL_ALT_R);

        createMenuItem(new MenuAction("Robot Test") {
            @Override
            public void onClick() {
                RobotTest.runRobot();
            }
        }, testSubmenu, EnabledIf.ACTION_ENABLED, CTRL_R);

        createMenuItem(new MenuAction("Filter Performance Test...") {
            @Override
            public void onClick() {
                new PerformanceTestingDialog(pixelitorWindow);
            }
        }, testSubmenu);

        createMenuItem(new MenuAction("Find Slowest Filter") {
            @Override
            public void onClick() {
                OpTests.findSlowestFilter();
            }
        }, testSubmenu);

        createMenuItem(new MenuAction("getCompositeImage() Performance Test...") {
            @Override
            public void onClick() {
                OpTests.getCompositeImagePerformanceTest();
            }
        }, testSubmenu);

        testSubmenu.addSeparator();

        createMenuItem(new MenuAction("Run All Filters on Current Layer") {
            @Override
            public void onClick() {
                OpTests.runAllFiltersOnCurrentLayer();
            }
        }, testSubmenu);

        createMenuItem(new MenuAction("Save the Result of Each Filter...") {
            @Override
            public void onClick() {
                OpTests.saveTheResultOfEachFilter();
            }
        }, testSubmenu);

        createMenuItem(new MenuAction("Save Current Image in All Formats...") {
            @Override
            public void onClick() {
                OpenSaveManager.saveCurrentImageInAllFormats();
            }
        }, testSubmenu);

        testSubmenu.addSeparator();

        createMenuItem(new MenuAction("Test Layer Operations") {
            @Override
            public void onClick() {
                ImageTests.testLayers();
            }
        }, testSubmenu, EnabledIf.ACTION_ENABLED);

        createMenuItem(new MenuAction("Test Tools") {
            @Override
            public void onClick() {
                ToolTests.testTools();
            }
        }, testSubmenu, EnabledIf.ACTION_ENABLED);

        createMenuItem(new MenuAction("IO Overlay Blur...") {
            @Override
            public void onClick() {
                ImageTests.ioOverlayBlur();
            }
        }, testSubmenu, EnabledIf.ACTION_ENABLED);

        developMenu.add(testSubmenu);
    }

    private static void initDebugSubmenu(JMenu develMenu, PixelitorWindow pixelitorWindow) {
        JMenu debugSubmenu = new JMenu("Debug");

        createMenuItem(new MenuAction("repaint() on the active image") {
            @Override
            public void onClick() {
                ImageComponents.repaintActive();
            }
        }, debugSubmenu);

        createMenuItem(new MenuAction("revalidate() the main window") {
            @Override
            public void onClick() {
                pixelitorWindow.getContentPane().revalidate();
            }
        }, debugSubmenu, EnabledIf.ACTION_ENABLED);

        createMenuItem(new MenuAction("reset the translation of current layer") {
            @Override
            public void onClick() {
                Composition comp = ImageComponents.getActiveComp().get();
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
        }, debugSubmenu);

        createMenuItem(new MenuAction("Debug Translation") {
            @Override
            public void onClick() {
                ImageLayer layer = ImageComponents.getActiveImageLayerOrMaskOrNull();
                int tx = layer.getTranslationX();
                int ty = layer.getTranslationY();
                System.out.println("MenuBar::onClick: tx = " + tx + ", ty = " + ty);
                Canvas canvas = layer.getComp().getCanvas();
                int canvasWidth = canvas.getWidth();
                int canvasHeight = canvas.getHeight();
                System.out.println("MenuBar::onClick: canvasWidth = " + canvasWidth + ", canvasHeight = " + canvasHeight);
                BufferedImage image = layer.getImage();
                int imageWidth = image.getWidth();
                int imageHeight = image.getHeight();
                System.out.println("MenuBar::onClick: imageWidth = " + imageWidth + ", imageHeight = " + imageHeight);
            }
        }, debugSubmenu, CTRL_K);

        createMenuItem(new MenuAction("Update Histograms") {
            @Override
            public void onClick() {
                Composition comp = ImageComponents.getActiveComp().get();
                HistogramsPanel.INSTANCE.updateFromCompIfShown(comp);
            }
        }, debugSubmenu);

        createMenuItem(new MenuAction("Save All Images to Folder...") {
            @Override
            public void onClick() {
                OpenSaveManager.saveAllImagesToDir();
            }
        }, debugSubmenu);

        createMenuItem(new MenuAction("Debug ImageLayer Images") {
            @Override
            public void onClick() {
                Optional<ImageLayer> layer = ImageComponents.getActiveImageLayerOrMask();
                layer.get().debugImages();
            }
        }, debugSubmenu);

        develMenu.add(debugSubmenu);
    }

    private void initHelpMenu(PixelitorWindow pixelitorWindow) {
        JMenu helpMenu = createMenu("Help", 'H');

        createMenuItem(new MenuAction("Tip of the Day") {
            @Override
            public void onClick() {
                TipsOfTheDay.showTips(pixelitorWindow, true);
            }
        }, helpMenu, EnabledIf.ACTION_ENABLED);

//        JMenu webSubMenu = new JMenu("Web");
//        MenuFactory.createMenuItem(new OpenInBrowserAction("Ask for Help", "https://sourceforge.net/projects/pixelitor/forums/forum/1034234"), null, webSubMenu, MenuEnableCondition.ACTION_ENABLED);
//        MenuFactory.createMenuItem(new OpenInBrowserAction("Discuss Pixelitor", "https://sourceforge.net/projects/pixelitor/forums/forum/1034233"), null, webSubMenu, MenuEnableCondition.ACTION_ENABLED);
//        MenuFactory.createMenuItem(new OpenInBrowserAction("Report a Bug", "https://sourceforge.net/tracker/?group_id=285935&atid=1211793"), null, webSubMenu, MenuEnableCondition.ACTION_ENABLED);
//        helpMenu.add(webSubMenu);

        helpMenu.addSeparator();

        helpMenu.add(new MenuAction("Check for Update...") {
            @Override
            public void onClick() {
                UpdatesCheck.checkForUpdates();
            }
        });

        createMenuItem(new MenuAction("About") {
            @Override
            public void onClick() {
                AboutDialog.showDialog(pixelitorWindow);
            }
        }, helpMenu, EnabledIf.ACTION_ENABLED);

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

}
