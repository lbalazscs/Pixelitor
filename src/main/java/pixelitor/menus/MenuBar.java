/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
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
import pixelitor.automate.BatchResize;
import pixelitor.filters.*;
import pixelitor.filters.comp.Flip;
import pixelitor.filters.comp.Rotate;
import pixelitor.filters.convolve.Convolve;
import pixelitor.filters.jhlabsproxies.*;
import pixelitor.filters.lookup.ColorBalance;
import pixelitor.filters.lookup.Levels;
import pixelitor.filters.lookup.Luminosity;
import pixelitor.filters.painters.TextFilter;
import pixelitor.history.History;
import pixelitor.io.FileChooser;
import pixelitor.io.OpenSaveManager;
import pixelitor.io.OptimizedJpegSavePanel;
import pixelitor.layers.AddNewLayerAction;
import pixelitor.layers.AdjustmentLayer;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.DeleteActiveLayerAction;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerDownAction;
import pixelitor.layers.LayerUpAction;
import pixelitor.layers.TextLayer;
import pixelitor.menus.edit.CopyAction;
import pixelitor.menus.edit.CopyType;
import pixelitor.menus.edit.PasteAction;
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

/**
 * The menu bar of the app
 */
public class MenuBar extends JMenuBar {

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


    private void initFileMenu(final PixelitorWindow pixelitorWindow) {
        JMenu fileMenu = createMenu("File", 'F');

        // new image
        MenuFactory.createMenuItem(NewImage.getAction(), KeyStroke.getKeyStroke('N', InputEvent.CTRL_MASK), fileMenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);

        // open
        Action openAction = new AbstractAction("Open...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    FileChooser.open();
                } catch (Exception ex) {
                    Dialogs.showExceptionDialog(ex);
                }
            }
        };
        MenuFactory.createMenuItem(openAction, KeyStroke.getKeyStroke('O', InputEvent.CTRL_MASK), fileMenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);

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
        MenuFactory.createMenuItem(saveAction, KeyStroke.getKeyStroke('S', InputEvent.CTRL_MASK), fileMenu);

        // save as
        Action saveAsAction = new AbstractAction("Save As...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpenSaveManager.save(true);
            }
        };
        MenuFactory.createMenuItem(saveAsAction, KeyStroke.getKeyStroke('S', InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK), fileMenu);

        Action optimizedSaveAction = new AbstractAction("Save Optimized JPEG...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Composition comp = ImageComponents.getActiveComp();
                BufferedImage image = comp.getCompositeImage();
                OptimizedJpegSavePanel.showInDialog(image, pixelitorWindow);
            }
        };
        MenuFactory.createMenuItem(optimizedSaveAction, null, fileMenu);

        Action exportAnimGIFAction = new AbstractAction("Export Animated GIF...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                AnimGifExportPanel.showInDialog(pixelitorWindow);
            }
        };
        MenuFactory.createMenuItem(exportAnimGIFAction, null, fileMenu);

        AbstractAction exportORA = new AbstractAction("Export OpenRaster...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpenRasterExportPanel.showInDialog(pixelitorWindow);
            }
        };
        MenuFactory.createMenuItem(exportORA, null, fileMenu);

        fileMenu.addSeparator();

        // close
        Action closeAction = new AbstractAction("Close") {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpenSaveManager.warnAndCloseImage(ImageComponents.getActiveImageComponent());
            }
        };
        MenuFactory.createMenuItem(closeAction, KeyStroke.getKeyStroke('W', InputEvent.CTRL_MASK), fileMenu, MenuEnableCondition.THERE_IS_OPEN_IMAGE);

        // close all
        Action closeAllAction = new AbstractAction("Close All") {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpenSaveManager.warnAndCloseAllImages();
            }
        };
        MenuFactory.createMenuItem(closeAllAction, KeyStroke.getKeyStroke('W', InputEvent.CTRL_MASK | InputEvent.ALT_MASK), fileMenu, MenuEnableCondition.THERE_IS_OPEN_IMAGE);

        initAutomateSubmenu(fileMenu);

        if (!JVM.isMac) {
            Action newFromScreenCapture = new ScreenCaptureAction();
            MenuFactory.createMenuItem(newFromScreenCapture, null, fileMenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);
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
        MenuFactory.createMenuItem(exitAction, null, fileMenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);

        this.add(fileMenu);
    }

    private static void initAutomateSubmenu(JMenu fileMenu) {
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
        MenuFactory.createMenuItem(batchResizeAction, null, batchSubmenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);

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
        MenuFactory.createMenuItem(exportLayersAction, null, batchSubmenu, MenuEnableCondition.THERE_IS_OPEN_IMAGE);
    }

    private void initEditMenu() {
        JMenu editMenu = createMenu("Edit", 'E');

        // last op
        MenuFactory.createMenuItem(RepeatLastOp.INSTANCE, KeyStroke.getKeyStroke('F', InputEvent.CTRL_MASK), editMenu, MenuEnableCondition.CAN_REPEAT_OPERATION);
        editMenu.addSeparator();

        // undo
        Action undoAction = new AbstractAction("Undo") {
            @Override
            public void actionPerformed(ActionEvent e) {
                History.undo();
            }
        };
        MenuFactory.createMenuItem(undoAction, KeyStroke.getKeyStroke('Z', InputEvent.CTRL_MASK), editMenu, MenuEnableCondition.UNDO_POSSIBLE);

        // undo
        Action redoAction = new AbstractAction("Redo") {
            @Override
            public void actionPerformed(ActionEvent e) {
                History.redo();
            }
        };
        MenuFactory.createMenuItem(redoAction, KeyStroke.getKeyStroke('Z', InputEvent.SHIFT_MASK + InputEvent.CTRL_MASK), editMenu, MenuEnableCondition.REDO_POSSIBLE);

        // fade
        MenuFactory.createMenuItem(new Fade(), KeyStroke.getKeyStroke('F', InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK), editMenu, MenuEnableCondition.FADING_POSSIBLE);

        // crop
        MenuFactory.createMenuItem(SelectionActions.getCropAction(), null, editMenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);

        editMenu.addSeparator();

        // copy
        MenuFactory.createMenuItem(new CopyAction(CopyType.COPY_LAYER), KeyStroke.getKeyStroke('C', InputEvent.CTRL_MASK), editMenu, MenuEnableCondition.THERE_IS_OPEN_IMAGE);
        MenuFactory.createMenuItem(new CopyAction(CopyType.COPY_COMPOSITE), KeyStroke.getKeyStroke('C', InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK), editMenu, MenuEnableCondition.THERE_IS_OPEN_IMAGE);
        // paste
        MenuFactory.createMenuItem(new PasteAction(false), KeyStroke.getKeyStroke('V', InputEvent.CTRL_MASK), editMenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);
        MenuFactory.createMenuItem(new PasteAction(true), KeyStroke.getKeyStroke('V', InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK), editMenu);


        editMenu.addSeparator();

        // resize
        Action resizeAction = new AbstractAction("Resize...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImageComponents.resizeActiveImage();
            }
        };
        MenuFactory.createMenuItem(resizeAction, KeyStroke.getKeyStroke('I', InputEvent.CTRL_MASK | InputEvent.ALT_MASK), editMenu, MenuEnableCondition.THERE_IS_OPEN_IMAGE);

        JMenu rotateSubmenu = new JMenu("Rotate/Flip");
        editMenu.add(rotateSubmenu);
        // rotate
        MenuFactory.createMenuItem(new Rotate(90, "Rotate 90\u00B0 CW"), null, rotateSubmenu, MenuEnableCondition.THERE_IS_OPEN_IMAGE);
        MenuFactory.createMenuItem(new Rotate(180, "Rotate 180\u00B0"), null, rotateSubmenu, MenuEnableCondition.THERE_IS_OPEN_IMAGE);
        MenuFactory.createMenuItem(new Rotate(270, "Rotate 90\u00B0 CCW"), null, rotateSubmenu, MenuEnableCondition.THERE_IS_OPEN_IMAGE);
        rotateSubmenu.addSeparator();
        // flip
        MenuFactory.createMenuItem(Flip.createFlipOp(Flip.Direction.HORIZONTAL), null, rotateSubmenu, MenuEnableCondition.THERE_IS_OPEN_IMAGE);
        MenuFactory.createMenuItem(Flip.createFlipOp(Flip.Direction.VERTICAL), null, rotateSubmenu, MenuEnableCondition.THERE_IS_OPEN_IMAGE);

        MenuFactory.createMenuItem(new TransformLayer(), null, editMenu, MenuEnableCondition.THERE_IS_OPEN_IMAGE);

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

        MenuFactory.createMenuItem(new ColorBalance(), KeyStroke.getKeyStroke('B', InputEvent.CTRL_MASK), colorsMenu);
        MenuFactory.createMenuItem(new HueSat(), KeyStroke.getKeyStroke('U', InputEvent.CTRL_MASK), colorsMenu);
        MenuFactory.createMenuItem(new Colorize(), null, colorsMenu);
        MenuFactory.createMenuItem(new Levels(), KeyStroke.getKeyStroke('L', InputEvent.CTRL_MASK), colorsMenu);
        MenuFactory.createMenuItem(new Brightness(), null, colorsMenu);
        MenuFactory.createMenuItem(new Solarize(), null, colorsMenu);
        MenuFactory.createMenuItem(new Sepia(), null, colorsMenu);
        MenuFactory.createMenuItem(new Invert(), KeyStroke.getKeyStroke('I', InputEvent.CTRL_MASK), colorsMenu);
        MenuFactory.createMenuItem(new ChannelInvert(), null, colorsMenu);
        MenuFactory.createMenuItem(new ChannelMixer(), null, colorsMenu);

        initExtractChannelsSubmenu(colorsMenu);

        initReduceColorsSubmenu(colorsMenu);

        initFillSubmenu(colorsMenu);

        this.add(colorsMenu);
    }

    private static void initFillSubmenu(JMenu colorsMenu) {
        JMenu fillSubmenu = new JMenu("Fill with");
        MenuFactory.createMenuItem(new Fill(FillType.FOREGROUND), KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, InputEvent.ALT_MASK), fillSubmenu);
        MenuFactory.createMenuItem(new Fill(FillType.BACKGROUND), KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, InputEvent.CTRL_MASK), fillSubmenu);
        MenuFactory.createMenuItem(new Fill(FillType.TRANSPARENT), null, fillSubmenu);
        MenuFactory.createMenuItem(new FillWithColorWheel(), null, fillSubmenu);
        MenuFactory.createMenuItem(new JHFourColorGradient(), null, fillSubmenu);
        MenuFactory.createMenuItem(new Starburst(), null, fillSubmenu);

        colorsMenu.add(fillSubmenu);
    }

    private static void initExtractChannelsSubmenu(JMenu colorsMenu) {
        JMenu channelsSubmenu = new JMenu("Extract Channels");
        colorsMenu.add(channelsSubmenu);

        MenuFactory.createMenuItem(new ExtractChannel(), null, channelsSubmenu);

        channelsSubmenu.addSeparator();
        MenuFactory.createMenuItem(new Luminosity(), null, channelsSubmenu);
        MenuFactory.createMenuItem(NoDialogPixelOpFactory.getValueChannelOp(), null, channelsSubmenu);
        MenuFactory.createMenuItem(NoDialogPixelOpFactory.getDesaturateChannelOp(), null, channelsSubmenu);
        channelsSubmenu.addSeparator();
        MenuFactory.createMenuItem(NoDialogPixelOpFactory.getHueChannelOp(), null, channelsSubmenu);
        MenuFactory.createMenuItem(NoDialogPixelOpFactory.getHueInColorsChannelOp(), null, channelsSubmenu);
        MenuFactory.createMenuItem(NoDialogPixelOpFactory.getSaturationChannelOp(), null, channelsSubmenu);
    }

    private static void initReduceColorsSubmenu(JMenu colorsMenu) {
        JMenu reduceColorsSubmenu = new JMenu("Reduce Colors");
        colorsMenu.add(reduceColorsSubmenu);

        MenuFactory.createMenuItem(new JHQuantize(), null, reduceColorsSubmenu);
        MenuFactory.createMenuItem(new Posterize(), null, reduceColorsSubmenu);
        MenuFactory.createMenuItem(new Threshold(), null, reduceColorsSubmenu);
        reduceColorsSubmenu.addSeparator();
        MenuFactory.createMenuItem(new JHTriTone(), null, reduceColorsSubmenu);
        MenuFactory.createMenuItem(new GradientMap(), null, reduceColorsSubmenu);
        reduceColorsSubmenu.addSeparator();
        MenuFactory.createMenuItem(new JHColorHalftone(), null, reduceColorsSubmenu);
        MenuFactory.createMenuItem(new JHDither(), null, reduceColorsSubmenu);
    }

    private void initSelectMenu() {
        JMenu selectMenu = createMenu("Selection", 'S');

        MenuFactory.createMenuItem(SelectionActions.getDeselectAction(), KeyStroke.getKeyStroke('D', InputEvent.CTRL_MASK), selectMenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);

        MenuFactory.createMenuItem(SelectionActions.getInvertSelectionAction(), KeyStroke.getKeyStroke('I', InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK), selectMenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);

        selectMenu.addSeparator();
        MenuFactory.createMenuItem(SelectionActions.getTraceWithBrush(), null, selectMenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);
        MenuFactory.createMenuItem(SelectionActions.getTraceWithEraser(), null, selectMenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);

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

        MenuFactory.createMenuItem(TextFilter.INSTANCE, KeyStroke.getKeyStroke('T'), filterMenu);

        this.add(filterMenu);
    }

    private static void initRenderSubmenu(JMenu filterMenu) {
        JMenu renderSubmenu = new JMenu("Render");
        MenuFactory.createMenuItem(new Clouds(), null, renderSubmenu);
        MenuFactory.createMenuItem(new ValueNoise(), null, renderSubmenu);
        MenuFactory.createMenuItem(new JHCaustics(), null, renderSubmenu);
        MenuFactory.createMenuItem(new JHPlasma(), null, renderSubmenu);
        MenuFactory.createMenuItem(new JHWood(), null, renderSubmenu);
        MenuFactory.createMenuItem(new JHCells(), null, renderSubmenu);
        MenuFactory.createMenuItem(new JHBrushedMetal(), null, renderSubmenu);

        filterMenu.add(renderSubmenu);
    }

    private static void initFindEdgesSubmenu(JMenu filterMenu) {
        JMenu findEdgesSubmenu = new JMenu("Find Edges");
        MenuFactory.createMenuItem(new JHConvolutionEdge(), null, findEdgesSubmenu);
        MenuFactory.createMenuItem(new JHLaplacian(), null, findEdgesSubmenu);
        MenuFactory.createMenuItem(new JHDifferenceOfGaussians(), null, findEdgesSubmenu);
        MenuFactory.createMenuItem(new Canny(), null, findEdgesSubmenu);
        filterMenu.add(findEdgesSubmenu);
    }

    private static void initOtherSubmenu(JMenu filterMenu) {
        JMenu otherFiltersSubmenu = new JMenu("Other");
        MenuFactory.createMenuItem(new Convolve(3), null, otherFiltersSubmenu);
        MenuFactory.createMenuItem(new Convolve(5), null, otherFiltersSubmenu);

        MenuFactory.createMenuItem(new JHDropShadow(), null, otherFiltersSubmenu);
        MenuFactory.createMenuItem(new Transition2D(), null, otherFiltersSubmenu);


        filterMenu.add(otherFiltersSubmenu);
    }

    private static void initArtisticSubmenu(JMenu filterMenu) {
        JMenu artisticFiltersSubmenu = new JMenu("Artistic");
        MenuFactory.createMenuItem(new JHCrystallize(), null, artisticFiltersSubmenu);
        MenuFactory.createMenuItem(new JHPointillize(), null, artisticFiltersSubmenu);
        MenuFactory.createMenuItem(new JHStamp(), null, artisticFiltersSubmenu);
        MenuFactory.createMenuItem(new JHDryBrush(), null, artisticFiltersSubmenu);

        MenuFactory.createMenuItem(new RandomSpheres(), null, artisticFiltersSubmenu);
        MenuFactory.createMenuItem(new JHSmear(), null, artisticFiltersSubmenu);
        MenuFactory.createMenuItem(new JHEmboss(), null, artisticFiltersSubmenu);

        MenuFactory.createMenuItem(new Orton(), null, artisticFiltersSubmenu);
        MenuFactory.createMenuItem(new PhotoCollage(), null, artisticFiltersSubmenu);

        filterMenu.add(artisticFiltersSubmenu);
    }

    private static void initBlurSharpenSubmenu(JMenu filterMenu) {
        JMenu bsSubmenu = new JMenu("Blur/Sharpen");
        MenuFactory.createMenuItem(new JHGaussianBlur(), null, bsSubmenu);
        MenuFactory.createMenuItem(new JHSmartBlur(), null, bsSubmenu);
        MenuFactory.createMenuItem(new JHBoxBlur(), null, bsSubmenu);
        MenuFactory.createMenuItem(new FastBlur(), null, bsSubmenu);
        MenuFactory.createMenuItem(new JHLensBlur(), null, bsSubmenu);
        MenuFactory.createMenuItem(new JHMotionBlur(JHMotionBlur.Mode.MOTION_BLUR), null, bsSubmenu);
        MenuFactory.createMenuItem(new JHMotionBlur(JHMotionBlur.Mode.SPIN_ZOOM_BLUR), null, bsSubmenu);
        bsSubmenu.addSeparator();
        MenuFactory.createMenuItem(new JHUnsharpMask(), null, bsSubmenu);
        filterMenu.add(bsSubmenu);
    }

    private static void initNoiseSubmenu(JMenu filterMenu) {
        JMenu noiseSubmenu = new JMenu("Noise");
        MenuFactory.createMenuItem(new JHReduceNoise(), null, noiseSubmenu);
        MenuFactory.createMenuItem(new JHMedian(), null, noiseSubmenu);

        noiseSubmenu.addSeparator();
        MenuFactory.createMenuItem(new AddNoise(), null, noiseSubmenu);
        MenuFactory.createMenuItem(new JHPixelate(), null, noiseSubmenu);

        filterMenu.add(noiseSubmenu);
    }

    private static void initLightSubmenu(JMenu filterMenu) {
        JMenu lightSubmenu = new JMenu("Light");
        filterMenu.add(lightSubmenu);
        MenuFactory.createMenuItem(new JHGlow(), null, lightSubmenu);
        MenuFactory.createMenuItem(new JHSparkle(), null, lightSubmenu);
        MenuFactory.createMenuItem(new JHRays(), null, lightSubmenu);
        MenuFactory.createMenuItem(new JHGlint(), null, lightSubmenu);
    }

    private static void initDistortSubmenu(JMenu filterMenu) {
        JMenu distortMenu = new JMenu("Distort");
        filterMenu.add(distortMenu);
//        MenuFactory.createMenuItem(new JHPinch(), null, distortMenu);
//        MenuFactory.createMenuItem(new Swirl(), null, distortMenu);
        MenuFactory.createMenuItem(new UnifiedSwirl(), null, distortMenu);

        MenuFactory.createMenuItem(new CircleToSquare(), null, distortMenu);
        MenuFactory.createMenuItem(new JHPerspective(), null, distortMenu);
        distortMenu.addSeparator();
        MenuFactory.createMenuItem(new JHLensOverImage(), null, distortMenu);
        MenuFactory.createMenuItem(new Magnify(), null, distortMenu);
        distortMenu.addSeparator();
        MenuFactory.createMenuItem(new JHTurbulentDistortion(), null, distortMenu);
        MenuFactory.createMenuItem(new JHUnderWater(), null, distortMenu);
        MenuFactory.createMenuItem(new JHWaterRipple(), null, distortMenu);
        MenuFactory.createMenuItem(new JHWaves(), null, distortMenu);
        MenuFactory.createMenuItem(new AngularWaves(), null, distortMenu);
        MenuFactory.createMenuItem(new RadialWaves(), null, distortMenu);
        distortMenu.addSeparator();
        MenuFactory.createMenuItem(new GlassTiles(), null, distortMenu);
        MenuFactory.createMenuItem(new PolarTiles(), null, distortMenu);
        MenuFactory.createMenuItem(new JHFrostedGlass(), null, distortMenu);
        distortMenu.addSeparator();
        MenuFactory.createMenuItem(new LittlePlanet(), null, distortMenu);
        MenuFactory.createMenuItem(new JHPolarCoordinates(), null, distortMenu);
        MenuFactory.createMenuItem(new JHWrapAroundArc(), null, distortMenu);
    }

    private static void initDislocateSubmenu(JMenu filterMenu) {
        JMenu dislocateSubmenu = new JMenu("Dislocate");
        filterMenu.add(dislocateSubmenu);

        MenuFactory.createMenuItem(new JHKaleidoscope(), null, dislocateSubmenu);
        MenuFactory.createMenuItem(new JHVideoFeedback(), null, dislocateSubmenu);
        MenuFactory.createMenuItem(new JHOffset(), null, dislocateSubmenu);
        MenuFactory.createMenuItem(new Slice(), null, dislocateSubmenu);
        MenuFactory.createMenuItem(new Mirror(), null, dislocateSubmenu);
    }


    private void initLayerMenu() {
        JMenu layersMenu = createMenu("Layer", 'L');

        layersMenu.add(AddNewLayerAction.INSTANCE);
        layersMenu.add(DeleteActiveLayerAction.INSTANCE);

        AbstractAction flattenImageAction = new AbstractAction("Flatten Image") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Composition comp = ImageComponents.getActiveComp();
                comp.flattenImage();
            }
        };
        MenuFactory.createMenuItem(flattenImageAction, null, layersMenu);

        AbstractAction mergeDownAction = new AbstractAction("Merge Down") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Composition comp = ImageComponents.getActiveComp();
                comp.mergeDown();
            }
        };
        MenuFactory.createMenuItem(mergeDownAction, KeyStroke.getKeyStroke('E', InputEvent.CTRL_MASK), layersMenu);


        AbstractAction duplicateLayerAction = new AbstractAction("Duplicate Layer") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Composition comp = ImageComponents.getActiveComp();
                comp.duplicateLayer();
            }
        };
        MenuFactory.createMenuItem(duplicateLayerAction, KeyStroke.getKeyStroke('J', InputEvent.CTRL_MASK), layersMenu);

        AbstractAction newLayerFromCompositeAction = new AbstractAction("New Layer from Composite") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Composition comp = ImageComponents.getActiveComp();
                comp.addNewLayerFromComposite("Composite");
            }
        };
        MenuFactory.createMenuItem(newLayerFromCompositeAction, KeyStroke.getKeyStroke('E', InputEvent.CTRL_MASK + InputEvent.ALT_MASK + InputEvent.SHIFT_MASK), layersMenu);

        AbstractAction layerToCanvasSizeAction = new AbstractAction("Layer to Canvas Size") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Composition comp = ImageComponents.getActiveComp();
                comp.layerToCanvasSize();
            }
        };
        MenuFactory.createMenuItem(layerToCanvasSizeAction, null, layersMenu);


        initLayerStackSubmenu(layersMenu);
        this.add(layersMenu);
    }

    private static void initLayerStackSubmenu(JMenu layersMenu) {
        JMenu layerStackSubmenu = new JMenu("Layer Stack");

        MenuFactory.createMenuItem(LayerUpAction.INSTANCE, KeyStroke.getKeyStroke(']', InputEvent.CTRL_MASK), layerStackSubmenu);

        MenuFactory.createMenuItem(LayerDownAction.INSTANCE, KeyStroke.getKeyStroke('[', InputEvent.CTRL_MASK), layerStackSubmenu);

        AbstractAction moveToLast = new AbstractAction("Layer to Top") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Composition comp = ImageComponents.getActiveComp();
                comp.moveActiveLayerToTop();
            }
        };
        MenuFactory.createMenuItem(moveToLast, KeyStroke.getKeyStroke(']', InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK), layerStackSubmenu);

        AbstractAction moveToFirstAction = new AbstractAction("Layer to Bottom") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Composition comp = ImageComponents.getActiveComp();
                comp.moveActiveLayerToBottom();
            }
        };
        MenuFactory.createMenuItem(moveToFirstAction, KeyStroke.getKeyStroke('[', InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK), layerStackSubmenu);

        layerStackSubmenu.addSeparator();

        AbstractAction moveSelectionUpAction = new AbstractAction("Raise Layer Selection") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Composition comp = ImageComponents.getActiveComp();
                comp.moveLayerSelectionUp();
            }
        };
        MenuFactory.createMenuItem(moveSelectionUpAction, KeyStroke.getKeyStroke(']', InputEvent.ALT_MASK), layerStackSubmenu);

        AbstractAction moveDownSelectionAction = new AbstractAction("Lower Layer Selection") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Composition comp = ImageComponents.getActiveComp();
                comp.moveLayerSelectionDown();
            }
        };
        MenuFactory.createMenuItem(moveDownSelectionAction, KeyStroke.getKeyStroke('[', InputEvent.ALT_MASK), layerStackSubmenu);


        layersMenu.add(layerStackSubmenu);
    }


    private void initViewMenu(PixelitorWindow pixelitorWindow) {
        JMenu viewMenu = createMenu("View", 'V');
//        JMenu lfSubmenu = new LookAndFeelMenu("Skin", parent);
//        viewMenu.add(lfSubmenu);

        viewMenu.add(ZoomMenu.INSTANCE);

        viewMenu.addSeparator();

        viewMenu.add(new ShowHideStatusBarAction());
        MenuFactory.createMenuItem(new ShowHideHistogramsAction(), KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), viewMenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);
        MenuFactory.createMenuItem(new ShowHideLayersAction(), KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), viewMenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);
        viewMenu.add(new ShowHideToolsAction());
        MenuFactory.createMenuItem(ShowHideAllAction.INSTANCE, KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), viewMenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);

        AbstractAction defaultWorkspaceAction = new AbstractAction("Set Default Workspace") {
            @Override
            public void actionPerformed(ActionEvent e) {
                AppPreferences.WorkSpace.setDefault();
            }
        };
        MenuFactory.createMenuItem(defaultWorkspaceAction, null, viewMenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);

//        viewMenu.addSeparator();
//
//        NewWindowForAction newWindowForAction = new NewWindowForAction();
//        MenuFactory.createMenuItem(newWindowForAction, null, viewMenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);

        viewMenu.addSeparator();

        initArrangeWindowsSubmenu(viewMenu, pixelitorWindow);

        this.add(viewMenu);
    }

    private static void initArrangeWindowsSubmenu(JMenu viewMenu, final PixelitorWindow pixelitorWindow) {
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
        MenuFactory.createMenuItem(cascadeWindowsAction, null, arrangeWindowsSubmenu);

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
        MenuFactory.createMenuItem(tileWindowsAction, null, arrangeWindowsSubmenu);

        viewMenu.add(arrangeWindowsSubmenu);
    }

    private void initDevelopMenu(final PixelitorWindow pixelitorWindow) {
        JMenu developMenu = createMenu("Develop", 'D');

        initDebugSubmenu(developMenu, pixelitorWindow);
        initTestSubmenu(developMenu, pixelitorWindow);
        initSplashSubmenu(developMenu);
        initExperimentalSubmenu(developMenu);

        AbstractAction newTextLayer = new AbstractAction("New Text Layer...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                String s = JOptionPane.showInputDialog(pixelitorWindow, "Text:", "Text Layer Text", JOptionPane.QUESTION_MESSAGE);
                Composition comp = ImageComponents.getActiveComp();
                TextLayer textLayer = new TextLayer(comp, "text layer", s);

                comp.addLayer(textLayer, true, true, false);
            }
        };
        MenuFactory.createMenuItem(newTextLayer, null, developMenu);

        AbstractAction newAdjustmentLayer = new AbstractAction("New Global Adjustment Layer...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Composition comp = ImageComponents.getActiveComp();
                AdjustmentLayer adjustmentLayer = new AdjustmentLayer(comp, "invert adjustment", new Invert());

                comp.addLayer(adjustmentLayer, true, true, false);
            }
        };
        MenuFactory.createMenuItem(newAdjustmentLayer, null, developMenu);


        AbstractAction filterCreatorAction = new AbstractAction("Filter Creator...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                FilterCreator.showInDialog(pixelitorWindow);
            }
        };
        MenuFactory.createMenuItem(filterCreatorAction, null, developMenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);

        AbstractAction debugSpecialAction = new AbstractAction("Debug Special") {
            @Override
            public void actionPerformed(ActionEvent e) {

                BufferedImage img = ImageComponents.getActiveCompositeImage();

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
        MenuFactory.createMenuItem(debugSpecialAction, null, developMenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);

        AbstractAction addLayerMask = new AbstractAction("Add Layer Mask") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Layer layer = ImageComponents.getActiveComp().getActiveLayer();
                layer.addTestLayerMask();
            }
        };
        MenuFactory.createMenuItem(addLayerMask, null, developMenu);

        AbstractAction dumpEvents = new AbstractAction("Dump Event Queue") {
            @Override
            public void actionPerformed(ActionEvent e) {
                DebugEventQueue.dump();
            }
        };
        MenuFactory.createMenuItem(dumpEvents, null, developMenu);

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
        MenuFactory.createMenuItem(splashScreenAction, null, splashMenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);

        AbstractAction manySplashScreensAction = new AbstractAction("Save Many Splash Images...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImageTests.saveManySplashImages();
            }
        };
        MenuFactory.createMenuItem(manySplashScreensAction, null, splashMenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);
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
        MenuFactory.createMenuItem(editLayerMask, null, layerMaskSubMenu);

        AbstractAction editComposition = new AbstractAction("Edit Composition") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImageComponent ic = ImageComponents.getActiveImageComponent();
                if (ic != null) {
                    ic.setLayerMaskEditing(false);
                }
            }
        };

        MenuFactory.createMenuItem(editComposition, null, layerMaskSubMenu);

        developMenu.add(layerMaskSubMenu);
    }

    private static void initExperimentalSubmenu(JMenu developMenu) {
        JMenu experimentalSubmenu = new JMenu("Experimental");
        developMenu.add(experimentalSubmenu);

        MenuFactory.createMenuItem(new MysticRose(), null, experimentalSubmenu);


        MenuFactory.createMenuItem(new Sphere3D(), null, experimentalSubmenu);

        MenuFactory.createMenuItem(EnlargeCanvas.getAction(), null, experimentalSubmenu);


        MenuFactory.createMenuItem(new Brick(), null, experimentalSubmenu);

        MenuFactory.createMenuItem(new RenderGrid(), null, experimentalSubmenu);
        MenuFactory.createMenuItem(new Lightning(), null, experimentalSubmenu);

        MenuFactory.createMenuItem(new EmptyPolar(), null, experimentalSubmenu);

    }

    private static void initTestSubmenu(JMenu developMenu, final PixelitorWindow pixelitorWindow) {
        JMenu testSubmenu = new JMenu("Test");

        MenuFactory.createMenuItem(new ParamTest(), KeyStroke.getKeyStroke('P', InputEvent.CTRL_MASK), testSubmenu);

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
        MenuFactory.createMenuItem(randomResizeAction, KeyStroke.getKeyStroke('M', InputEvent.CTRL_MASK), testSubmenu);

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
        MenuFactory.createMenuItem(randomToolAction, null, testSubmenu);

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
        MenuFactory.createMenuItem(randomBrushAction, null, testSubmenu);


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
//        MenuFactory.createMenuItem(robotTestAction, KeyStroke.getKeyStroke('R', InputEvent.CTRL_MASK), testSubmenu, MenuEnableCondition.ALWAYS);
        MenuFactory.createMenuItem(robotTestAction, KeyStroke.getKeyStroke('R', InputEvent.CTRL_MASK), testSubmenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);

        AbstractAction opPerformanceTestAction = new AbstractAction("Operation Performance Test...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                new PerformanceTestingDialog(pixelitorWindow);
            }
        };
        MenuFactory.createMenuItem(opPerformanceTestAction, null, testSubmenu);


        AbstractAction findSlowestFilter = new AbstractAction("Find Slowest Filter") {
            @Override
            public void actionPerformed(ActionEvent e) {

                Utils.findSlowestFilter();
            }
        };
        MenuFactory.createMenuItem(findSlowestFilter, KeyStroke.getKeyStroke('K', InputEvent.CTRL_MASK), testSubmenu);

        AbstractAction ciPerformanceTestAction = new AbstractAction("getCompositeImage() Performance Test...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpTests.getCompositeImagePerformanceTest();
            }
        };
        MenuFactory.createMenuItem(ciPerformanceTestAction, null, testSubmenu);

        testSubmenu.addSeparator();

        AbstractAction runAllOps = new AbstractAction("Run All Operations on Current Layer") {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpTests.runAllOpsOnCurrentLayer();
            }
        };
        MenuFactory.createMenuItem(runAllOps, null, testSubmenu);

        AbstractAction saveAllOps = new AbstractAction("Save the Result of Each Operation...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpTests.saveTheResultOfEachOp();
            }
        };
        MenuFactory.createMenuItem(saveAllOps, null, testSubmenu);

        AbstractAction saveInAllFormats = new AbstractAction("Save Current Image in All Formats...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpenSaveManager.saveCurrentImageInAllFormats();
            }
        };
        MenuFactory.createMenuItem(saveInAllFormats, null, testSubmenu);

        testSubmenu.addSeparator();

        AbstractAction testAllOnNewImg = new AbstractAction("Test Layer Operations") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImageTests.testLayers();
            }
        };
        MenuFactory.createMenuItem(testAllOnNewImg, null, testSubmenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);

        AbstractAction testTools = new AbstractAction("Test Tools") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ToolTests.testTools();
            }
        };
        MenuFactory.createMenuItem(testTools, null, testSubmenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);

        AbstractAction testIOOverlayBlur = new AbstractAction("IO Overlay Blur...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImageTests.ioOverlayBlur();
            }
        };
        MenuFactory.createMenuItem(testIOOverlayBlur, null, testSubmenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);


        developMenu.add(testSubmenu);
    }

    private static void initDebugSubmenu(JMenu develMenu, final PixelitorWindow pixelitorWindow) {
        JMenu debugSubmenu = new JMenu("Debug");

        AbstractAction debugAppAction = new AbstractAction("Debug App...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                AppLogic.showDebugAppDialog();
            }
        };
        MenuFactory.createMenuItem(debugAppAction, null, debugSubmenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);

        AbstractAction debugHistoryAction = new AbstractAction("Debug History...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                History.showHistory();
            }
        };
        MenuFactory.createMenuItem(debugHistoryAction, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), debugSubmenu);

        AbstractAction imageInfo = new AbstractAction("Image Info...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Composition comp = ImageComponents.getActiveComp();
                int canvasWidth = comp.getCanvasWidth();
                int canvasHeight = comp.getCanvasHeight();
                long pixels = canvasWidth * canvasHeight * 4;

                float sizeMBytes = pixels / 1048576.0f;
                String msg = String.format("Canvas Width = %d pixels\nCanvas Height = %d pixels\nSize in Memory = %.2f Mbytes/layer", canvasWidth, canvasHeight, sizeMBytes);
                Dialogs.showInfoDialog("Image Info - " + comp.getName(), msg);
            }
        };
        MenuFactory.createMenuItem(imageInfo, null, debugSubmenu);

        AbstractAction repaintActive = new AbstractAction("repaint() on the active image") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImageComponents.repaintActive();
            }
        };
        MenuFactory.createMenuItem(repaintActive, null, debugSubmenu);

        AbstractAction imageChangedActive = new AbstractAction("imageChanged(true, true) on the active image") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImageComponents.getActiveComp().imageChanged(true, true);
            }
        };
        MenuFactory.createMenuItem(imageChangedActive, null, debugSubmenu);


        AbstractAction revalidateActive = new AbstractAction("revalidate() the main window") {
            @Override
            public void actionPerformed(ActionEvent e) {
                pixelitorWindow.getContentPane().revalidate();
            }
        };
        MenuFactory.createMenuItem(revalidateActive, null, debugSubmenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);

        AbstractAction resetLayerTranslation = new AbstractAction("reset the translation of current layer") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Composition comp = ImageComponents.getActiveComp();
                Layer layer = comp.getActiveLayer();
                if (layer instanceof ContentLayer) {
                    ContentLayer contentLayer = (ContentLayer) layer;
                    contentLayer.setTranslationX(0);
                    contentLayer.setTranslationY(0);
                    comp.imageChanged(true, true);
                }
            }
        };
        MenuFactory.createMenuItem(resetLayerTranslation, null, debugSubmenu);


        AbstractAction updateHistogram = new AbstractAction("Update Histograms") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Composition comp = ImageComponents.getActiveComp();
                HistogramsPanel.INSTANCE.updateFromCompIfShown(comp);
            }
        };
        MenuFactory.createMenuItem(updateHistogram, null, debugSubmenu);

        AbstractAction saveAllImagesToDir = new AbstractAction("Save All Images to Directory...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpenSaveManager.saveAllImagesToDir();
            }
        };
        MenuFactory.createMenuItem(saveAllImagesToDir, null, debugSubmenu);


        develMenu.add(debugSubmenu);
    }

    private void initHelpMenu(final PixelitorWindow pixelitorWindow) {
        JMenu helpMenu = createMenu("Help", 'H');

        AbstractAction tipOfTheDayAction = new AbstractAction("Tip of the Day") {
            @Override
            public void actionPerformed(ActionEvent e) {
                TipsOfTheDay.showTips(pixelitorWindow, true);
            }
        };
        MenuFactory.createMenuItem(tipOfTheDayAction, null, helpMenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);

//        JMenu webSubMenu = new JMenu("Web");
//        MenuFactory.createMenuItem(new OpenInBrowserAction("Ask for Help", "https://sourceforge.net/projects/pixelitor/forums/forum/1034234"), null, webSubMenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);
//        MenuFactory.createMenuItem(new OpenInBrowserAction("Discuss Pixelitor", "https://sourceforge.net/projects/pixelitor/forums/forum/1034233"), null, webSubMenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);
//        MenuFactory.createMenuItem(new OpenInBrowserAction("Report a Bug", "https://sourceforge.net/tracker/?group_id=285935&atid=1211793"), null, webSubMenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);
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
        MenuFactory.createMenuItem(aboutAction, null, helpMenu, MenuEnableCondition.ALWAYS_UNLESS_ACTION_DISABLED);

        this.add(helpMenu);
    }

    private static JMenu createMenu(String s, char c) {
        JMenu menu = new JMenu(s);
        menu.setMnemonic(c);
        return menu;
    }

}
