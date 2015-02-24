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
import pixelitor.EnlargeCanvas;
import pixelitor.FillType;
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
import pixelitor.filters.jhlabsproxies.*;
import pixelitor.filters.lookup.ColorBalance;
import pixelitor.filters.lookup.Levels;
import pixelitor.filters.lookup.Luminosity;
import pixelitor.filters.painters.TextFilter;
import pixelitor.history.History;
import pixelitor.io.FileChoosers;
import pixelitor.io.OpenSaveManager;
import pixelitor.io.OptimizedJpegSavePanel;
import pixelitor.layers.AddNewLayerAction;
import pixelitor.layers.AdjustmentLayer;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.DeleteActiveLayerAction;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMoveAction;
import pixelitor.layers.TextLayer;
import pixelitor.menus.edit.CopyAction;
import pixelitor.menus.edit.CopySource;
import pixelitor.menus.edit.PasteAction;
import pixelitor.menus.edit.PasteDestination;
import pixelitor.menus.file.AnimGifExportPanel;
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
import pixelitor.utils.Utils;
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
        createMenuItem(NewImage.getAction(), fileMenu, EnabledIf.ACTION_ENABLED, CTRL_N, "new");

        // open
        Action openAction = new AbstractAction("Open...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    FileChoosers.open();
                } catch (Exception ex) {
                    Dialogs.showExceptionDialog(ex);
                }
            }
        };
        createMenuItem(openAction, fileMenu, EnabledIf.ACTION_ENABLED, CTRL_O, "open");

        // recent files
        JMenu recentFiles = RecentFilesMenu.getInstance();
        fileMenu.add(recentFiles);

        fileMenu.addSeparator();

        // save
        Action saveAction = new AbstractAction("Save") {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpenSaveManager.save(false);
            }
        };
        createMenuItem(saveAction, fileMenu, CTRL_S);

        // save as
        Action saveAsAction = new AbstractAction("Save As...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpenSaveManager.save(true);
            }
        };
        createMenuItem(saveAsAction, fileMenu, CTRL_SHIFT_S);

        Action optimizedSaveAction = new AbstractAction("Export Optimized JPEG...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Composition comp = ImageComponents.getActiveComp().get();
                BufferedImage image = comp.getCompositeImage();
                OptimizedJpegSavePanel.showInDialog(image, pixelitorWindow);
            }
        };
        createMenuItem(optimizedSaveAction, fileMenu);

        AbstractAction exportORA = new AbstractAction("Export OpenRaster...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpenRasterExportPanel.showInDialog(pixelitorWindow);
            }
        };
        createMenuItem(exportORA, fileMenu);

        fileMenu.addSeparator();

        Action exportLayerAnim = new AbstractAction("Export Layer Animation...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                AnimGifExportPanel.showInDialog(pixelitorWindow);
            }
        };
        createMenuItem(exportLayerAnim, fileMenu);

        AbstractAction exportTweeningAnim = new AbstractAction("Export Tweening Animation...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                new TweenWizard().start(pixelitorWindow);
            }
        };
        createMenuItem(exportTweeningAnim, fileMenu);

        fileMenu.addSeparator();

        // close
        Action closeAction = new AbstractAction("Close") {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpenSaveManager.warnAndCloseImage(ImageComponents.getActiveImageComponent());
            }
        };
        createMenuItem(closeAction, fileMenu, CTRL_W);

        // close all
        Action closeAllAction = new AbstractAction("Close All") {
            @Override
            public void actionPerformed(ActionEvent e) {
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

        String exitName = JVM.isMac ? "Quit" : "Exit";
        // exit
        Action exitAction = new AbstractAction(exitName) {
            @Override
            public void actionPerformed(ActionEvent e) {
                AppPreferences.exitApp();
            }
        };
        createMenuItem(exitAction, fileMenu, EnabledIf.ACTION_ENABLED);

        this.add(fileMenu);
    }

    private static void initAutomateSubmenu(JMenu fileMenu, PixelitorWindow pixelitorWindow) {
        JMenu batchSubmenu = new JMenu("Automate");
        fileMenu.add(batchSubmenu);

        Action batchResizeAction = new AbstractAction("Batch Resize...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    BatchResize.runBatchResize();
                } catch (Exception ex) {
                    Dialogs.showExceptionDialog(ex);
                }
            }
        };
        createMenuItem(batchResizeAction, batchSubmenu, EnabledIf.ACTION_ENABLED);

        Action batchFilterAction = new AbstractAction("Batch Filter...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    new BatchFilterWizard().start(pixelitorWindow);
                } catch (Exception ex) {
                    Dialogs.showExceptionDialog(ex);
                }
            }
        };
        createMenuItem(batchFilterAction, batchSubmenu, EnabledIf.ACTION_ENABLED);

        Action exportLayersAction = new AbstractAction("Export Layers to PNG...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    OpenSaveManager.exportLayersToPNG();
                } catch (Exception ex) {
                    Dialogs.showExceptionDialog(ex);
                }
            }
        };
        createMenuItem(exportLayersAction, batchSubmenu, EnabledIf.THERE_IS_OPEN_IMAGE);
    }

    private void initEditMenu() {
        JMenu editMenu = createMenu("Edit", 'E');

        // last op
        createMenuItem(RepeatLastOp.INSTANCE, editMenu, EnabledIf.CAN_REPEAT_OPERATION, CTRL_F);
        editMenu.addSeparator();

        // undo
        Action undoAction = new AbstractAction("Undo") {
            @Override
            public void actionPerformed(ActionEvent e) {
                History.undo();
            }
        };
        createMenuItem(undoAction, editMenu, EnabledIf.UNDO_POSSIBLE, CTRL_Z);

        // undo
        Action redoAction = new AbstractAction("Redo") {
            @Override
            public void actionPerformed(ActionEvent e) {
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
        Action resizeAction = new AbstractAction("Resize...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImageComponents.resizeActiveImage();
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
        Action preferencesAction = new AbstractAction("Preferences...") {
            @Override
            public void actionPerformed(ActionEvent e) {
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
        createMenuItem(new Fill(FillType.FOREGROUND), fillSubmenu, ALT_BACKSPACE);
        createMenuItem(new Fill(FillType.BACKGROUND), fillSubmenu, CTRL_BACKSPACE);
        createMenuItem(new Fill(FillType.TRANSPARENT), fillSubmenu);
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

        AbstractAction flattenImageAction = new AbstractAction("Flatten Image") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Composition comp = ImageComponents.getActiveComp().get();
                comp.flattenImage(true);
            }
        };
        createMenuItem(flattenImageAction, layersMenu);

        AbstractAction mergeDownAction = new AbstractAction("Merge Down") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Composition comp = ImageComponents.getActiveComp().get();
                comp.mergeDown();
            }
        };
        createMenuItem(mergeDownAction, layersMenu, CTRL_E);

        AbstractAction duplicateLayerAction = new AbstractAction("Duplicate Layer") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Composition comp = ImageComponents.getActiveComp().get();
                comp.duplicateLayer();
            }
        };
        createMenuItem(duplicateLayerAction, layersMenu, CTRL_J);

        AbstractAction newLayerFromCompositeAction = new AbstractAction("New Layer from Composite") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Composition comp = ImageComponents.getActiveComp().get();
                comp.addNewLayerFromComposite("Composite");
            }
        };
        createMenuItem(newLayerFromCompositeAction, layersMenu, CTRL_SHIFT_ALT_E);

        AbstractAction layerToCanvasSizeAction = new AbstractAction("Layer to Canvas Size") {
            @Override
            public void actionPerformed(ActionEvent e) {
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

        AbstractAction moveToLast = new AbstractAction("Layer to Top") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Composition comp = ImageComponents.getActiveComp().get();
                comp.moveActiveLayerToTop();
            }
        };
        createMenuItem(moveToLast, layerStackSubmenu, CTRL_SHIFT_CLOSE_BRACKET);

        AbstractAction moveToFirstAction = new AbstractAction("Layer to Bottom") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Composition comp = ImageComponents.getActiveComp().get();
                comp.moveActiveLayerToBottom();
            }
        };
        createMenuItem(moveToFirstAction, layerStackSubmenu, CTRL_SHIFT_OPEN_BRACKET);

        layerStackSubmenu.addSeparator();

        AbstractAction moveSelectionUpAction = new AbstractAction("Raise Layer Selection") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Composition comp = ImageComponents.getActiveComp().get();
                comp.moveLayerSelectionUp();
            }
        };
        createMenuItem(moveSelectionUpAction, layerStackSubmenu, ALT_CLOSE_BRACKET);

        AbstractAction moveDownSelectionAction = new AbstractAction("Lower Layer Selection") {
            @Override
            public void actionPerformed(ActionEvent e) {
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

        AbstractAction defaultWorkspaceAction = new AbstractAction("Set Default Workspace") {
            @Override
            public void actionPerformed(ActionEvent e) {
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

        AbstractAction cascadeWindowsAction = new AbstractAction("Cascade") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    pixelitorWindow.cascadeWindows();
                } catch (Exception ex) {
                    Dialogs.showExceptionDialog(ex);
                }
            }
        };
        createMenuItem(cascadeWindowsAction, arrangeWindowsSubmenu);

        AbstractAction tileWindowsAction = new AbstractAction("Tile") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    pixelitorWindow.tileWindows();
                } catch (Exception ex) {
                    Dialogs.showExceptionDialog(ex);
                }
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

        AbstractAction newTextLayer = new AbstractAction("New Text Layer...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                String s = JOptionPane.showInputDialog(pixelitorWindow, "Text:", "Text Layer Text", JOptionPane.QUESTION_MESSAGE);
                Composition comp = ImageComponents.getActiveComp().get();
                TextLayer textLayer = new TextLayer(comp, "text layer", s);

                comp.addLayer(textLayer, true, true, false);
            }
        };
        createMenuItem(newTextLayer, developMenu);

        AbstractAction newAdjustmentLayer = new AbstractAction("New Global Adjustment Layer...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Composition comp = ImageComponents.getActiveComp().get();
                AdjustmentLayer adjustmentLayer = new AdjustmentLayer(comp, "invert adjustment", new Invert());

                comp.addLayer(adjustmentLayer, true, true, false);
            }
        };
        createMenuItem(newAdjustmentLayer, developMenu);


        AbstractAction filterCreatorAction = new AbstractAction("Filter Creator...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                FilterCreator.showInDialog(pixelitorWindow);
            }
        };
        createMenuItem(filterCreatorAction, developMenu, EnabledIf.ACTION_ENABLED);

        AbstractAction debugSpecialAction = new AbstractAction("Debug Special") {
            @Override
            public void actionPerformed(ActionEvent e) {

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

        AbstractAction addLayerMask = new AbstractAction("Add Layer Mask") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Layer layer = ImageComponents.getActiveLayer().get();
                layer.addTestLayerMask();
            }
        };
        createMenuItem(addLayerMask, developMenu);

        AbstractAction dumpEvents = new AbstractAction("Dump Event Queue") {
            @Override
            public void actionPerformed(ActionEvent e) {
                DebugEventQueue.dump();
            }
        };
        createMenuItem(dumpEvents, developMenu);

        initLayerMaskSubmenu(developMenu);

        this.add(developMenu);
    }

    private static void initSplashSubmenu(JMenu developMenu) {
        JMenu splashMenu = new JMenu("Splash");

        AbstractAction splashScreenAction = new AbstractAction("Create Splash Image") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImageTests.createSplashImage();
            }
        };
        createMenuItem(splashScreenAction, splashMenu, EnabledIf.ACTION_ENABLED);

        AbstractAction manySplashScreensAction = new AbstractAction("Save Many Splash Images...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImageTests.saveManySplashImages();
            }
        };
        createMenuItem(manySplashScreensAction, splashMenu, EnabledIf.ACTION_ENABLED);
        developMenu.add(splashMenu);
    }

    private static void initLayerMaskSubmenu(JMenu developMenu) {
        JMenu layerMaskSubMenu = new JMenu("Layer Mask");

        AbstractAction editLayerMask = new AbstractAction("Edit Layer Mask") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImageComponent ic = ImageComponents.getActiveImageComponent();
                if (ic.getComp().getActiveLayer().hasLayerMask()) {
                    ic.setLayerMaskEditing(true);
                } else {
                    Dialogs.showInfoDialog("No Layer mask", "The active layer has no layer mask");
                }
            }
        };
        createMenuItem(editLayerMask, layerMaskSubMenu);

        AbstractAction editComposition = new AbstractAction("Edit Composition") {
            @Override
            public void actionPerformed(ActionEvent e) {
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

        AbstractAction randomResizeAction = new AbstractAction("Random Resize") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    OpTests.randomResize();
                } catch (Exception ex) {
                    Dialogs.showExceptionDialog(ex);
                }
            }
        };
        createMenuItem(randomResizeAction, testSubmenu, CTRL_ALT_R);

        AbstractAction randomToolAction = new AbstractAction("1001 Brush & Shape Actions") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    ToolTests.randomToolActions(1001, false);
                } catch (Exception ex) {
                    Dialogs.showExceptionDialog(ex);
                }
            }
        };
        createMenuItem(randomToolAction, testSubmenu);

        AbstractAction randomBrushAction = new AbstractAction("1001 Brush Only Actions") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    ToolTests.randomToolActions(1001, true);
                } catch (Exception ex) {
                    Dialogs.showExceptionDialog(ex);
                }
            }
        };
        createMenuItem(randomBrushAction, testSubmenu);


        AbstractAction robotTestAction = new AbstractAction("Robot Test") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    RobotTest.runRobot();
                } catch (Exception ex) {
                    Dialogs.showExceptionDialog(ex);
                }
            }
        };
        createMenuItem(robotTestAction, testSubmenu, EnabledIf.ACTION_ENABLED, CTRL_R);

        AbstractAction opPerformanceTestAction = new AbstractAction("Operation Performance Test...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                new PerformanceTestingDialog(pixelitorWindow);
            }
        };
        createMenuItem(opPerformanceTestAction, testSubmenu);

        AbstractAction findSlowestFilter = new AbstractAction("Find Slowest Filter") {
            @Override
            public void actionPerformed(ActionEvent e) {

                Utils.findSlowestFilter();
            }
        };
        createMenuItem(findSlowestFilter, testSubmenu);

        AbstractAction ciPerformanceTestAction = new AbstractAction("getCompositeImage() Performance Test...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpTests.getCompositeImagePerformanceTest();
            }
        };
        createMenuItem(ciPerformanceTestAction, testSubmenu);

        testSubmenu.addSeparator();

        AbstractAction runAllOps = new AbstractAction("Run All Operations on Current Layer") {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpTests.runAllOpsOnCurrentLayer();
            }
        };
        createMenuItem(runAllOps, testSubmenu);

        AbstractAction saveAllOps = new AbstractAction("Save the Result of Each Operation...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpTests.saveTheResultOfEachOp();
            }
        };
        createMenuItem(saveAllOps, testSubmenu);

        AbstractAction saveInAllFormats = new AbstractAction("Save Current Image in All Formats...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpenSaveManager.saveCurrentImageInAllFormats();
            }
        };
        createMenuItem(saveInAllFormats, testSubmenu);

        testSubmenu.addSeparator();

        AbstractAction testAllOnNewImg = new AbstractAction("Test Layer Operations") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImageTests.testLayers();
            }
        };
        createMenuItem(testAllOnNewImg, testSubmenu, EnabledIf.ACTION_ENABLED);

        AbstractAction testTools = new AbstractAction("Test Tools") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ToolTests.testTools();
            }
        };
        createMenuItem(testTools, testSubmenu, EnabledIf.ACTION_ENABLED);

        AbstractAction testIOOverlayBlur = new AbstractAction("IO Overlay Blur...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImageTests.ioOverlayBlur();
            }
        };
        createMenuItem(testIOOverlayBlur, testSubmenu, EnabledIf.ACTION_ENABLED);

        developMenu.add(testSubmenu);
    }

    private static void initDebugSubmenu(JMenu develMenu, PixelitorWindow pixelitorWindow) {
        JMenu debugSubmenu = new JMenu("Debug");

        AbstractAction debugAppAction = new AbstractAction("Debug App...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                AppLogic.showDebugAppDialog();
            }
        };
        createMenuItem(debugAppAction, debugSubmenu, EnabledIf.ACTION_ENABLED);

        AbstractAction debugHistoryAction = new AbstractAction("Debug History...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                History.showHistory();
            }
        };
        createMenuItem(debugHistoryAction, debugSubmenu);

        AbstractAction imageInfo = new AbstractAction("Image Info...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Composition comp = ImageComponents.getActiveComp().get();
                int canvasWidth = comp.getCanvasWidth();
                int canvasHeight = comp.getCanvasHeight();
                long pixels = canvasWidth * canvasHeight * 4;

                float sizeMBytes = pixels / 1048576.0f;
                String msg = String.format("Canvas Width = %d pixels\nCanvas Height = %d pixels\nSize in Memory = %.2f Mbytes/layer", canvasWidth, canvasHeight, sizeMBytes);
                Dialogs.showInfoDialog("Image Info - " + comp.getName(), msg);
            }
        };
        createMenuItem(imageInfo, debugSubmenu);

        AbstractAction repaintActive = new AbstractAction("repaint() on the active image") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImageComponents.repaintActive();
            }
        };
        createMenuItem(repaintActive, debugSubmenu);

        AbstractAction imageChangedActive = new AbstractAction("imageChanged(true, true) on the active image") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImageComponents.getActiveComp().get().imageChanged(FULL);
            }
        };
        createMenuItem(imageChangedActive, debugSubmenu);

        AbstractAction revalidateActive = new AbstractAction("revalidate() the main window") {
            @Override
            public void actionPerformed(ActionEvent e) {
                pixelitorWindow.getContentPane().revalidate();
            }
        };
        createMenuItem(revalidateActive, debugSubmenu, EnabledIf.ACTION_ENABLED);

        AbstractAction resetLayerTranslation = new AbstractAction("reset the translation of current layer") {
            @Override
            public void actionPerformed(ActionEvent e) {
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

        AbstractAction updateHistogram = new AbstractAction("Update Histograms") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Composition comp = ImageComponents.getActiveComp().get();
                HistogramsPanel.INSTANCE.updateFromCompIfShown(comp);
            }
        };
        createMenuItem(updateHistogram, debugSubmenu);

        AbstractAction saveAllImagesToDir = new AbstractAction("Save All Images to Folder...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpenSaveManager.saveAllImagesToDir();
            }
        };
        createMenuItem(saveAllImagesToDir, debugSubmenu);

        AbstractAction debugImageLayerImages = new AbstractAction("Debug ImageLayer Images") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Optional<ImageLayer> layer = ImageComponents.getActiveImageLayer();
                layer.get().debugImages();
            }
        };
        createMenuItem(debugImageLayerImages, debugSubmenu);

        develMenu.add(debugSubmenu);
    }

    private void initHelpMenu(PixelitorWindow pixelitorWindow) {
        JMenu helpMenu = createMenu("Help", 'H');

        AbstractAction tipOfTheDayAction = new AbstractAction("Tip of the Day") {
            @Override
            public void actionPerformed(ActionEvent e) {
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

        AbstractAction checkForUpdateAction = new AbstractAction("Check for Update...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                UpdatesCheck.checkForUpdates();
            }
        };
        helpMenu.add(checkForUpdateAction);

        AbstractAction aboutAction = new AbstractAction("About") {
            @Override
            public void actionPerformed(ActionEvent e) {
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

    private static void createMenuItem(Action a, JMenu parent, EnabledIf whenToEnable, KeyStroke keyStroke, String name) {
        JMenuItem menuItem = whenToEnable.getMenuItem(a);
        if(name != null) {
            menuItem.setName(name);
        }
        parent.add(menuItem);
        if (keyStroke != null) {
            menuItem.setAccelerator(keyStroke);
        }
    }

    private static void createMenuItem(Action action, JMenu parent, EnabledIf whenToEnable, KeyStroke keyStroke) {
        createMenuItem(action, parent, whenToEnable, keyStroke, (String) action.getValue(Action.NAME));
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
