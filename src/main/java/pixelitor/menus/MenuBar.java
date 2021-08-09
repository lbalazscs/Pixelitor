/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import com.jhlabs.image.LaplaceFilter;
import com.jhlabs.image.MedianFilter;
import com.jhlabs.image.ReduceNoiseFilter;
import pixelitor.*;
import pixelitor.automate.AutoPaint;
import pixelitor.automate.BatchFilterWizard;
import pixelitor.automate.BatchResize;
import pixelitor.colors.palette.ColorSwatchClickHandler;
import pixelitor.colors.palette.FullPalette;
import pixelitor.colors.palette.PalettePanel;
import pixelitor.compactions.EnlargeCanvas;
import pixelitor.compactions.Flip;
import pixelitor.compactions.ResizePanel;
import pixelitor.compactions.Rotate;
import pixelitor.filters.Mirror;
import pixelitor.filters.*;
import pixelitor.filters.animation.TweenWizard;
import pixelitor.filters.convolve.Convolve;
import pixelitor.filters.curves.ToneCurvesFilter;
import pixelitor.filters.jhlabsproxies.*;
import pixelitor.filters.levels.Levels;
import pixelitor.filters.lookup.ColorBalance;
import pixelitor.filters.lookup.Luminosity;
import pixelitor.filters.painters.TextFilter;
import pixelitor.filters.util.FilterSearchPanel;
import pixelitor.filters.util.FilterUtils;
import pixelitor.gui.*;
import pixelitor.gui.utils.OpenImageEnabledAction;
import pixelitor.gui.utils.PAction;
import pixelitor.gui.utils.RestrictedLayerAction;
import pixelitor.gui.utils.Themes;
import pixelitor.guides.Guides;
import pixelitor.history.History;
import pixelitor.history.RedoAction;
import pixelitor.history.UndoAction;
import pixelitor.io.FileChoosers;
import pixelitor.io.IO;
import pixelitor.io.OptimizedJpegSavePanel;
import pixelitor.io.magick.ExportSettings;
import pixelitor.io.magick.ImageMagick;
import pixelitor.layers.*;
import pixelitor.menus.edit.CopyAction;
import pixelitor.menus.edit.FadeAction;
import pixelitor.menus.edit.PasteAction;
import pixelitor.menus.edit.PasteDestination;
import pixelitor.menus.file.*;
import pixelitor.menus.help.AboutDialog;
import pixelitor.menus.help.UpdatesCheck;
import pixelitor.menus.view.*;
import pixelitor.selection.SelectionActions;
import pixelitor.tools.brushes.CopyBrush;
import pixelitor.utils.*;
import pixelitor.utils.debug.Debug;
import pixelitor.utils.test.Events;
import pixelitor.utils.test.RandomGUITest;
import pixelitor.utils.test.SplashImageCreator;

import javax.swing.*;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ResourceBundle;

import static pixelitor.Composition.UpdateActions.FULL;
import static pixelitor.OpenImages.*;
import static pixelitor.colors.FillType.*;
import static pixelitor.compactions.Flip.Direction.HORIZONTAL;
import static pixelitor.compactions.Flip.Direction.VERTICAL;
import static pixelitor.filters.jhlabsproxies.JHMotionBlur.Mode.MOTION_BLUR;
import static pixelitor.filters.jhlabsproxies.JHMotionBlur.Mode.SPIN_ZOOM_BLUR;
import static pixelitor.gui.ImageArea.Mode.FRAMES;
import static pixelitor.gui.utils.RestrictedLayerAction.Condition.*;
import static pixelitor.layers.LayerMaskAddType.*;
import static pixelitor.layers.LayerMoveAction.*;
import static pixelitor.utils.Keys.*;
import static pixelitor.utils.QuadrantAngle.*;

/**
 * The menu bar of the app
 */
public class MenuBar extends JMenuBar {
    public MenuBar(PixelitorWindow pw) {
        ResourceBundle texts = Texts.getResources();

        add(createFileMenu(pw, texts));
        add(createEditMenu(texts));
        add(createLayerMenu(pw, texts));
        add(createSelectMenu(texts));
        add(createImageMenu(texts));
        add(createColorMenu(texts));
        add(createFilterMenu(texts));
        add(createViewMenu(pw, texts));

        if (AppContext.isDevelopment()) {
            add(createDevelopMenu(pw));
        }

        add(createHelpMenu(pw, texts));
    }

    private static JMenu createFileMenu(PixelitorWindow pw, ResourceBundle texts) {
        // TODO adapt the mnemonic
        PMenu fileMenu = new PMenu(texts.getString("file"), 'F');

        // new image
        fileMenu.add(NewImage.getAction(), CTRL_N);

        fileMenu.add(new PAction(texts.getString("open") + "...") {
            @Override
            public void onClick() {
                FileChoosers.openAsync();
            }
        }, CTRL_O);

        fileMenu.add(RecentFilesMenu.INSTANCE);

        fileMenu.addSeparator();

        fileMenu.add(new OpenImageEnabledAction(texts.getString("save")) {
            @Override
            public void onClick() {
                IO.save(false);
            }
        }, CTRL_S);

        fileMenu.add(new OpenImageEnabledAction(texts.getString("save_as") + "...") {
            @Override
            public void onClick() {
                IO.save(true);
            }
        }, CTRL_SHIFT_S);

        fileMenu.add(new OpenImageEnabledAction(texts.getString("export_optimized_jpeg") + "...") {
            @Override
            public void onClick() {
                BufferedImage image = getActiveCompositeImage();
                OptimizedJpegSavePanel.showInDialog(image);
            }
        });

        fileMenu.add(createImageMagickSubmenu());

        fileMenu.addSeparator();

        fileMenu.add(new OpenImageEnabledAction(texts.getString("export_layer_animation") + "...") {
            @Override
            public void onClick() {
                LayerAnimExport.start();
            }
        });

        fileMenu.add(new DrawableAction(texts.getString("export_tweening_animation")) {
            @Override
            protected void process(Drawable dr) {
                new TweenWizard(dr).showDialog(pw);
            }
        });

        fileMenu.addSeparator();

        // reload
        fileMenu.add(new OpenImageEnabledAction(texts.getString("reload")) {
            @Override
            public void onClick() {
                reloadActiveFromFileAsync();
            }
        }, F12);

        fileMenu.add(new OpenImageEnabledAction(texts.getString("show_metadata") + "...") {
            @Override
            public void onClick() {
                MetaDataPanel.showInDialog();
            }
        });

        fileMenu.addSeparator();

        // close
        fileMenu.add(CLOSE_ACTIVE_ACTION, CTRL_W);

        // close all
        fileMenu.add(CLOSE_ALL_ACTION, CTRL_ALT_W);

        fileMenu.addSeparator();

        fileMenu.add(new PrintAction(), CTRL_P);

        fileMenu.add(createAutomateSubmenu(pw, texts));

        if (!JVM.isMac) {
            fileMenu.add(new ScreenCaptureAction());
        }

        fileMenu.addSeparator();

        // exit
        String exitName = JVM.isMac ?
            texts.getString("exit_mac") : texts.getString("exit");
        fileMenu.add(new PAction(exitName) {
            @Override
            public void onClick() {
                Pixelitor.exitApp(pw);
            }
        });

        return fileMenu;
    }

    private static JMenu createImageMagickSubmenu() {
        PMenu imMenu = new PMenu("ImageMagick");

        imMenu.add(new OpenImageEnabledAction("Export...") {
            @Override
            public void onClick() {
                ImageMagick.exportActiveComp();
            }
        });

        imMenu.add(new PAction("Import...") {
            @Override
            public void onClick() {
                ImageMagick.importComposition();
            }
        });

        return imMenu;
    }

    private static JMenu createAutomateSubmenu(PixelitorWindow pw, ResourceBundle texts) {
        PMenu automateMenu = new PMenu(texts.getString("automate"));

        automateMenu.add(new PAction(texts.getString("batch_resize") + "...") {
            @Override
            public void onClick() {
                BatchResize.showDialog();
            }
        });

        automateMenu.add(new DrawableAction(texts.getString("batch_filter")) {
            @Override
            protected void process(Drawable dr) {
                new BatchFilterWizard(dr).showDialog(pw);
            }
        });

        automateMenu.add(new OpenImageEnabledAction(texts.getString("export_layers_to_png") + "...") {
            @Override
            public void onClick() {
                IO.exportLayersToPNGAsync();
            }
        });

        automateMenu.add(new DrawableAction(texts.getString("auto_paint")) {
            @Override
            protected void process(Drawable dr) {
                AutoPaint.showDialog(dr);
            }
        });

        return automateMenu;
    }

    private static JMenu createEditMenu(ResourceBundle texts) {
        String editMenuName = texts.getString("edit");
        PMenu editMenu = new PMenu(editMenuName, 'E');

        // undo
        editMenu.add(UndoAction.INSTANCE, CTRL_Z);

        // redo
        editMenu.add(RedoAction.INSTANCE, CTRL_SHIFT_Z);

        // fade
        editMenu.add(FadeAction.INSTANCE, CTRL_SHIFT_F);

        editMenu.addSeparator();

        // copy
        editMenu.add(CopyAction.COPY_LAYER, CTRL_C);
        editMenu.add(CopyAction.COPY_COMPOSITE, CTRL_SHIFT_C);

        // paste
        var pasteAsNewImage = new PasteAction(PasteDestination.NEW_IMAGE);
        editMenu.add(pasteAsNewImage, CTRL_V);

        var pasteAsNewLayer = new PasteAction(PasteDestination.NEW_LAYER);
        editMenu.add(pasteAsNewLayer, CTRL_SHIFT_V);

        var pasteAsMask = new PasteAction(PasteDestination.MASK);
        editMenu.add(pasteAsMask, CTRL_ALT_V);

        editMenu.addSeparator();

        // preferences
        String prefsMenuName = texts.getString("preferences") + "...";
        editMenu.add(new PAction(prefsMenuName) {
            @Override
            public void onClick() {
                PreferencesPanel.showInDialog();
            }
        });

        return editMenu;
    }

    private static JMenu createLayerMenu(PixelitorWindow pw, ResourceBundle texts) {
        var layersMenu = new PMenu(texts.getString("layer"), 'L');

        layersMenu.add(AddNewLayerAction.INSTANCE);
        layersMenu.add(DeleteActiveLayerAction.INSTANCE);
        layersMenu.add(DuplicateLayerAction.INSTANCE, CTRL_J);

        layersMenu.addSeparator();

        // merge down
        var mergeDown = new OpenImageEnabledAction(GUIText.MERGE_DOWN) {
            @Override
            public void onClick() {
                getActiveComp().mergeActiveLayerDown();
            }
        };
        mergeDown.setToolTip(GUIText.MERGE_DOWN_TT);
        layersMenu.add(mergeDown, CTRL_E);

        // flatten image
        var flattenImage = new OpenImageEnabledAction(texts.getString("flatten_image")) {
            @Override
            public void onClick() {
                getActiveComp().flattenImage();
            }
        };
        flattenImage.setToolTip(texts.getString("flatten_image_tt"));
        layersMenu.add(flattenImage);

        // new layer from visible
        var newFromVisible = new OpenImageEnabledAction(texts.getString("new_from_visible")) {
            @Override
            public void onClick() {
                getActiveComp().addNewLayerFromComposite();
            }
        };
        newFromVisible.setToolTip(texts.getString("new_from_visible_tt"));
        layersMenu.add(newFromVisible, CTRL_SHIFT_ALT_E);

        layersMenu.add(createLayerStackSubmenu(texts));
        layersMenu.add(createLayerMaskSubmenu(texts));
        layersMenu.add(createTextLayerSubmenu(pw, texts));

        if (AppContext.enableAdjLayers) {
            layersMenu.add(createAdjustmentLayersSubmenu());
        }

        return layersMenu;
    }

    private static JMenu createLayerStackSubmenu(ResourceBundle texts) {
        var sub = new PMenu(texts.getString("layer_stack"));

        sub.add(MOVE_LAYER_UP, CTRL_ALT_R);

        sub.add(MOVE_LAYER_DOWN, CTRL_ALT_L);

        // layer to top
        var layerToTop = new OpenImageEnabledAction(LAYER_TO_TOP) {
            @Override
            public void onClick() {
                getActiveComp().moveActiveLayerToTop();
            }
        };
        layerToTop.setToolTip(texts.getString("layer_to_top_tt"));
        sub.add(layerToTop, CTRL_SHIFT_ALT_R);

        // layer_to_bottom
        var layerToBottom = new OpenImageEnabledAction(LAYER_TO_BOTTOM) {
            @Override
            public void onClick() {
                getActiveComp().moveActiveLayerToBottom();
            }
        };
        layerToBottom.setToolTip(texts.getString("layer_to_bottom_tt"));
        sub.add(layerToBottom, CTRL_SHIFT_ALT_L);

        sub.addSeparator();

        // raise layer selection
        var raiseLayerSelection = new OpenImageEnabledAction(RAISE_LAYER_SELECTION) {
            @Override
            public void onClick() {
                var comp = getActiveComp();
                comp.raiseLayerSelection();
            }
        };
        raiseLayerSelection.setToolTip(texts.getString("raise_layer_selection_tt"));
        sub.add(raiseLayerSelection, CTRL_SHIFT_R);

        var lowerLayerSelection = new OpenImageEnabledAction(LOWER_LAYER_SELECTION) {
            @Override
            public void onClick() {
                getActiveComp().lowerLayerSelection();
            }
        };
        lowerLayerSelection.setToolTip(texts.getString("lower_layer_selection_tt"));
        sub.add(lowerLayerSelection, CTRL_SHIFT_L);

        return sub;
    }

    private static JMenu createLayerMaskSubmenu(ResourceBundle texts) {
        PMenu sub = new PMenu(texts.getString("layer_mask"));

        sub.add(new RestrictedLayerAction("Add White (Reveal All)", NO_LAYER_MASK) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.addMask(REVEAL_ALL);
            }
        });

        sub.add(new RestrictedLayerAction("Add Black (Hide All)", NO_LAYER_MASK) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.addMask(HIDE_ALL);
            }
        });

        sub.add(new RestrictedLayerAction("Add from Selection", NO_LAYER_MASK) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.addMask(REVEAL_SELECTION);
            }
        });

        sub.add(new RestrictedLayerAction("Add from Transparency", NO_LAYER_MASK) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.addMask(FROM_TRANSPARENCY);
            }
        });

        sub.add(new RestrictedLayerAction("Add from Layer", NO_LAYER_MASK) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.addMask(FROM_LAYER);
            }
        });

        sub.add(new OpenImageEnabledAction("Add/Replace from Color Range...") {
            @Override
            public void onClick() {
                MaskFromColorRangePanel.showInDialog();
            }
        });

        sub.addSeparator();

        sub.add(new RestrictedLayerAction("Delete", HAS_LAYER_MASK) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.deleteMask(true);
            }
        });

        sub.add(new RestrictedLayerAction("Apply", HAS_LAYER_MASK) {
            @Override
            public void onActiveLayer(Layer layer) {
                if (!(layer instanceof ImageLayer)) {
                    Messages.showNotImageLayerError(layer);
                    return;
                }

                ((ImageLayer) layer).applyLayerMask(true);

                // not necessary, as the result looks the same, but still
                // useful because eventual problems would be spotted early
                layer.getComp().update();
            }
        });

        sub.addSeparator();

        MaskViewMode.NORMAL.addToMenuBar(sub);
        MaskViewMode.SHOW_MASK.addToMenuBar(sub);
        MaskViewMode.EDIT_MASK.addToMenuBar(sub);
        MaskViewMode.RUBYLITH.addToMenuBar(sub);

        return sub;
    }

    private static JMenu createTextLayerSubmenu(PixelitorWindow pw, ResourceBundle texts) {
        PMenu sub = new PMenu(texts.getString("text_layer"));

        sub.add(new OpenImageEnabledAction("New...") {
            @Override
            public void onClick() {
                TextLayer.createNew();
            }
        }, T);

        sub.add(new RestrictedLayerAction("Edit...", IS_TEXT_LAYER) {
            @Override
            public void onActiveLayer(Layer layer) {
                ((TextLayer) layer).edit(pw);
            }
        }, CTRL_T);

        sub.add(new RestrictedLayerAction("Rasterize", IS_TEXT_LAYER) {
            @Override
            public void onActiveLayer(Layer layer) {
                ((TextLayer) layer).replaceWithRasterized();
            }
        });

        sub.add(new RestrictedLayerAction("Selection from Text", IS_TEXT_LAYER) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.getComp().createSelectionFromText();
            }
        });

        return sub;
    }

    private static JMenu createAdjustmentLayersSubmenu() {
        PMenu sub = new PMenu("New Adjustment Layer");

        // not called "Invert" because of assertj test lookup confusion
        sub.add(new OpenImageEnabledAction("Invert Adjustment") {
            @Override
            public void onClick() {
                AddAdjLayerAction.INSTANCE.actionPerformed(null);
            }
        });

        return sub;
    }

    private static JMenu createSelectMenu(ResourceBundle texts) {
        PMenu selectMenu = new PMenu(texts.getString("select"), 'S');

        selectMenu.add(SelectionActions.getDeselect(), CTRL_D);
        selectMenu.add(SelectionActions.getShowHide(), CTRL_H);

        selectMenu.addSeparator();

        selectMenu.add(SelectionActions.getInvert(), CTRL_SHIFT_I);
        selectMenu.add(SelectionActions.getModify());

        selectMenu.addSeparator();

        selectMenu.add(SelectionActions.getCopy());
        selectMenu.add(SelectionActions.getPaste());

        return selectMenu;
    }

    private static JMenu createImageMenu(ResourceBundle texts) {
        PMenu imageMenu = new PMenu(texts.getString("image"), 'I');

        // crop
        imageMenu.add(SelectionActions.getCrop());

        // resize
        imageMenu.add(new OpenImageEnabledAction("Resize...") {
            @Override
            public void onClick() {
                ResizePanel.resizeActiveImage();
            }
        }, CTRL_ALT_I);

        imageMenu.add(new OpenImageEnabledAction("Duplicate") {
            @Override
            public void onClick() {
                duplicateActiveComp();
            }
        });

        if (AppContext.enableImageMode) {
            imageMenu.add(createModeSubmenu());
        }

        imageMenu.addSeparator();

        imageMenu.add(EnlargeCanvas.getAction());

        var fitCanvasToLayers = new OpenImageEnabledAction(texts.getString("fit_canvas_to_layers")) {
            @Override
            public void onClick() {
                getActiveComp().fitCanvasToLayers();
            }
        };
        fitCanvasToLayers.setToolTip(texts.getString("fit_canvas_to_layers_tt"));
        imageMenu.add(fitCanvasToLayers);

        imageMenu.add(new OpenImageEnabledAction("Layer to Canvas Size") {
            @Override
            public void onClick() {
                getActiveComp().activeLayerToCanvasSize();
            }
        });

        imageMenu.addSeparator();

        // rotate
        imageMenu.add(new Rotate(ANGLE_90));
        imageMenu.add(new Rotate(ANGLE_180));
        imageMenu.add(new Rotate(ANGLE_270));

        imageMenu.addSeparator();

        // flip
        imageMenu.add(new Flip(HORIZONTAL));
        imageMenu.add(new Flip(VERTICAL));

        return imageMenu;
    }

    private static JMenu createModeSubmenu() {
        PMenu sub = new PMenu("Mode");

        ImageMode[] modes = ImageMode.values();
        var radioGroup = new ButtonGroup();
        for (ImageMode mode : modes) {
            var menuItem = mode.getMenuItem();
            sub.add(menuItem);
            radioGroup.add(menuItem);
        }

        addActivationListener(new ViewActivationListener() {
            @Override
            public void viewActivated(View oldView, View newView) {
                newView.getComp().getMode().getMenuItem().setSelected(true);
            }

            @Override
            public void allViewsClosed() {
            }
        });

        return sub;
    }

    private static JMenu createColorMenu(ResourceBundle texts) {
        PMenu colorsMenu = new PMenu(GUIText.COLOR, 'C');

        colorsMenu.addFilter(ColorBalance.NAME, ColorBalance::new, CTRL_B);
        colorsMenu.addFilter(HueSat.NAME, HueSat::new, CTRL_U);
        colorsMenu.addFilter(Colorize.NAME, Colorize::new);
        colorsMenu.addFilter(Levels.NAME, Levels::new, CTRL_L);
        colorsMenu.addFilter(ToneCurvesFilter.NAME, ToneCurvesFilter::new, CTRL_M);
        colorsMenu.addFilter(BrightnessContrast.NAME, BrightnessContrast::new);
        colorsMenu.addFilter(Solarize.NAME, Solarize::new);
        colorsMenu.addFilter(Sepia.NAME, Sepia::new);
        colorsMenu.addFilterWithoutGUI(Invert.NAME, Invert::new, CTRL_I);
        colorsMenu.addFilter(ChannelInvert.NAME, ChannelInvert::new);
        colorsMenu.addFilter(ChannelMixer.NAME, ChannelMixer::new);

        colorsMenu.add(createExtractChannelsSubmenu());
        colorsMenu.add(createReduceColorsSubmenu());
        colorsMenu.add(createFillSubmenu());

        return colorsMenu;
    }

    private static JMenu createExtractChannelsSubmenu() {
        PMenu sub = new PMenu("Extract Channels");

        sub.addFilter("Extract Channel", ExtractChannel::new);

        sub.addSeparator();

        sub.addFilterWithoutGUI(Luminosity.NAME, Luminosity::new);
        sub.add(ExtractChannelFilter.getValueChannelFA());
        sub.add(ExtractChannelFilter.getDesaturateChannelFA());

        sub.addSeparator();

        sub.add(ExtractChannelFilter.getHueChannelFA());
        sub.add(ExtractChannelFilter.getHueInColorsChannelFA());
        sub.add(ExtractChannelFilter.getSaturationChannelFA());

        return sub;
    }

    private static JMenu createReduceColorsSubmenu() {
        PMenu sub = new PMenu("Reduce Colors");

        sub.addFilter(JHQuantize.NAME, JHQuantize::new);
        sub.addFilter(Posterize.NAME, Posterize::new);
        sub.addFilter(Threshold.NAME, Threshold::new);
        sub.addFilter(ColorThreshold.NAME, ColorThreshold::new);

        sub.addSeparator();

        sub.addFilter(JHTriTone.NAME, JHTriTone::new);
        sub.addFilter(GradientMap.NAME, GradientMap::new);

        sub.addSeparator();

        sub.addFilter(JHDither.NAME, JHDither::new);

        return sub;
    }

    private static JMenu createFillSubmenu() {
        PMenu sub = new PMenu(GUIText.FILL_WITH);

        sub.add(FOREGROUND.asFillFilterAction(), ALT_BACKSPACE);
        sub.add(BACKGROUND.asFillFilterAction(), CTRL_BACKSPACE);
        sub.add(TRANSPARENT.asFillFilterAction());

        sub.addFilter(ColorWheel.NAME, ColorWheel::new);
        sub.addFilter(JHFourColorGradient.NAME, JHFourColorGradient::new);

        return sub;
    }

    private static JMenu createFilterMenu(ResourceBundle texts) {
        PMenu filterMenu = new PMenu(texts.getString("filter"), 'T');

        filterMenu.add(new OpenImageEnabledAction("Filter Search...") {
            @Override
            public void onClick() {
                FilterSearchPanel.showInDialog();
            }
        }, F3);

        filterMenu.add(RepeatLast.REPEAT_LAST_ACTION, CTRL_F);
        filterMenu.add(RepeatLast.SHOW_LAST_ACTION, CTRL_ALT_F);

        filterMenu.addSeparator();

        filterMenu.add(createBlurSharpenSubmenu(texts));
        filterMenu.add(createDistortSubmenu(texts));
        filterMenu.add(createDisplaceSubmenu(texts));
        filterMenu.add(createLightSubmenu(texts));
        filterMenu.add(createNoiseSubmenu(texts));
        filterMenu.add(createRenderSubmenu(texts));
        filterMenu.add(createArtisticSubmenu(texts));
        filterMenu.add(createFindEdgesSubmenu(texts));
        filterMenu.add(createOtherSubmenu());

        // the text as filter is still useful for batch operations
        filterMenu.addFilter("Text", TextFilter::new);

        return filterMenu;
    }

    private static JMenu createBlurSharpenSubmenu(ResourceBundle texts) {
        PMenu sub = new PMenu(texts.getString("blur")
            + "/" + texts.getString("sharpen"));

        sub.addFilter(JHBoxBlur.NAME, JHBoxBlur::new);
        sub.addFilter(JHFocus.NAME, JHFocus::new);
        sub.addFilter(JHGaussianBlur.NAME, JHGaussianBlur::new);
        sub.addFilter(JHLensBlur.NAME, JHLensBlur::new);
        sub.add(MOTION_BLUR.createFilterAction());
        sub.addFilter(JHSmartBlur.NAME, JHSmartBlur::new);
        sub.add(SPIN_ZOOM_BLUR.createFilterAction());
        sub.addSeparator();
        sub.addFilter(JHUnsharpMask.NAME, JHUnsharpMask::new);

        return sub;
    }

    private static JMenu createDistortSubmenu(ResourceBundle texts) {
        PMenu sub = new PMenu(texts.getString("distort"));

        sub.addFilter(JHSwirlPinchBulge.NAME, JHSwirlPinchBulge::new);
        sub.addFilter(CircleToSquare.NAME, CircleToSquare::new);
        sub.addFilter(JHPerspective.NAME, JHPerspective::new);

        sub.addSeparator();

        sub.addFilter(JHLensOverImage.NAME, JHLensOverImage::new);
        sub.addFilter(Magnify.NAME, Magnify::new);

        sub.addSeparator();

        sub.addFilter(JHTurbulentDistortion.NAME, JHTurbulentDistortion::new);
        sub.addFilter(JHUnderWater.NAME, JHUnderWater::new);
        sub.addFilter(JHWaterRipple.NAME, JHWaterRipple::new);
        sub.addFilter(JHWaves.NAME, JHWaves::new);
        sub.addFilter(AngularWaves.NAME, AngularWaves::new);
        sub.addFilter(RadialWaves.NAME, RadialWaves::new);

        sub.addSeparator();

        sub.addFilter(GlassTiles.NAME, GlassTiles::new);
        sub.addFilter(PolarTiles.NAME, PolarTiles::new);
        sub.addFilter(JHFrostedGlass.NAME, JHFrostedGlass::new);

        sub.addSeparator();

        sub.addFilter(LittlePlanet.NAME, LittlePlanet::new);
        sub.addFilter(JHPolarCoordinates.NAME, JHPolarCoordinates::new);
        sub.addFilter(JHWrapAroundArc.NAME, JHWrapAroundArc::new);

        return sub;
    }

    private static JMenu createDisplaceSubmenu(ResourceBundle texts) {
        PMenu sub = new PMenu(texts.getString("displace"));

        sub.addFilter(DisplacementMap.NAME, DisplacementMap::new);
        sub.addFilter(DrunkVision.NAME, DrunkVision::new);
        sub.addFilter(JHKaleidoscope.NAME, JHKaleidoscope::new);
        sub.addFilter(JHOffset.NAME, JHOffset::new);
        sub.addFilter(Mirror.NAME, Mirror::new);
        sub.addFilter(Slice.NAME, Slice::new);
        sub.addFilter(JHVideoFeedback.NAME, JHVideoFeedback::new);

        return sub;
    }

    private static JMenu createLightSubmenu(ResourceBundle texts) {
        PMenu sub = new PMenu(texts.getString("light"));

        sub.addFilter(BumpMap.NAME, BumpMap::new);
        sub.addFilter(Flashlight.NAME, Flashlight::new);
        sub.addFilter(JHGlint.NAME, JHGlint::new);
        sub.addFilter(JHGlow.NAME, JHGlow::new);
        sub.addFilter(JHRays.NAME, JHRays::new);
        sub.addFilter(JHSparkle.NAME, JHSparkle::new);

        return sub;
    }

    private static JMenu createNoiseSubmenu(ResourceBundle texts) {
        PMenu sub = new PMenu(texts.getString("noise"));

        String reduceNoiseFilterName = "Reduce Single Pixel Noise";
        sub.addForwardingFilter(reduceNoiseFilterName,
            () -> new ReduceNoiseFilter(reduceNoiseFilterName));

        String medianFilterName = "3x3 Median Filter";
        sub.addForwardingFilter(medianFilterName,
            () -> new MedianFilter(medianFilterName));

        sub.addSeparator();

        sub.addFilter(AddNoise.NAME, AddNoise::new);
        sub.addFilter(JHPixelate.NAME, JHPixelate::new);

        return sub;
    }

    private static JMenu createRenderSubmenu(ResourceBundle texts) {
        PMenu sub = new PMenu(texts.getString("render"));

        sub.addFilter(Clouds.NAME, Clouds::new);
        sub.addFilter(JHPlasma.NAME, JHPlasma::new);
        sub.addFilter(ValueNoise.NAME, ValueNoise::new);

        sub.addSeparator();

        sub.addFilter(JHBrushedMetal.NAME, JHBrushedMetal::new);
        sub.addFilter(JHCaustics.NAME, JHCaustics::new);
        sub.addFilter(JHCells.NAME, JHCells::new);
        sub.addFilter(FlowFieldNew.NAME, FlowFieldNew::new);
        sub.addFilter(Marble.NAME, Marble::new);
        sub.addFilter(Voronoi.NAME, Voronoi::new);
        sub.addFilter(JHWood.NAME, JHWood::new);

        sub.addFilter(NMLFilter_test.NAME, NMLFilter_test::new);

        sub.addSeparator();

        sub.add(createRenderFractalsSubmenu(texts));
        sub.add(createRenderGeometrySubmenu());
        sub.add(createRenderShapesSubmenu());

        return sub;
    }

    private static JMenu createRenderShapesSubmenu() {
        PMenu sub = new PMenu("Shapes");

        sub.addFilter("Flower of Life", FlowerOfLife::new);
        sub.addFilter("Grid", Grid::new);
        sub.addFilter(Lissajous.NAME, Lissajous::new);
        sub.addFilter(MysticRose.NAME, MysticRose::new);
        sub.addFilter(SpiderWeb.NAME, SpiderWeb::new);
        sub.addFilter(Spiral.NAME, Spiral::new);
        sub.addFilter("Spirograph", Spirograph::new);

        return sub;
    }

    private static JMenu createRenderFractalsSubmenu(ResourceBundle texts) {
        PMenu sub = new PMenu(texts.getString("fractals"));

        sub.addFilter(ChaosGame.NAME, ChaosGame::new);
        sub.addFilter(FractalTree.NAME, FractalTree::new);
        sub.addFilter(JuliaSet.NAME, JuliaSet::new);
        sub.addFilter(MandelbrotSet.NAME, MandelbrotSet::new);

        return sub;
    }

    private static JMenu createRenderGeometrySubmenu() {
        PMenu sub = new PMenu("Geometry");

        sub.addFilter(JHCheckerFilter.NAME, JHCheckerFilter::new);
        sub.addFilter(Starburst.NAME, Starburst::new);
        sub.addFilter(Truchet.NAME, Truchet::new);

        return sub;
    }

    private static JMenu createArtisticSubmenu(ResourceBundle texts) {
        PMenu sub = new PMenu(texts.getString("artistic"));

        sub.addFilter(JHCrystallize.NAME, JHCrystallize::new);
        sub.addFilter(JHEmboss.NAME, JHEmboss::new);
        sub.addFilter(JHOilPainting.NAME, JHOilPainting::new);
        sub.addFilter(Orton.NAME, Orton::new);
        sub.addFilter(PhotoCollage.NAME, PhotoCollage::new);
        sub.addFilter(JHPointillize.NAME, JHPointillize::new);
        sub.addFilter(RandomSpheres.NAME, RandomSpheres::new);
        sub.addFilter(JHSmear.NAME, JHSmear::new);
        sub.addFilter(JHStamp.NAME, JHStamp::new);
        sub.addFilter(JHWeave.NAME, JHWeave::new);

        sub.add(createHalftoneSubmenu());

        return sub;
    }

    private static JMenu createHalftoneSubmenu() {
        PMenu sub = new PMenu("Halftone");

        sub.addFilter(JHStripedHalftone.NAME, JHStripedHalftone::new);
        sub.addFilter(JHConcentricHalftone.NAME, JHConcentricHalftone::new);
        sub.addFilter(JHColorHalftone.NAME, JHColorHalftone::new);

        return sub;
    }

    private static JMenu createFindEdgesSubmenu(ResourceBundle texts) {
        PMenu sub = new PMenu(texts.getString("find_edges"));

        sub.addFilter(JHConvolutionEdge.NAME, JHConvolutionEdge::new);

        String laplacianFilterName = "Laplacian";
        sub.addNoGrayForwardingFilter(laplacianFilterName,
            () -> new LaplaceFilter(laplacianFilterName));

        sub.addFilter(JHDifferenceOfGaussians.NAME, JHDifferenceOfGaussians::new);
        sub.addFilter("Canny", Canny::new);

        return sub;
    }

    private static JMenu createOtherSubmenu() {
        PMenu sub = new PMenu("Other");

        sub.addFilter(JHDropShadow.NAME, JHDropShadow::new);
        sub.addFilter(Morphology.NAME, Morphology::new);
        sub.addFilter("Random Filter", RandomFilter::new);
        sub.addFilter("Transform Layer", TransformLayer::new);
        sub.addFilter(Transition2D.NAME, Transition2D::new);

        sub.addSeparator();

        sub.add(Convolve.createFilterAction(3));
        sub.add(Convolve.createFilterAction(5));

        sub.addSeparator();

        sub.addFilter(ChannelToTransparency.NAME, ChannelToTransparency::new);
        sub.addFilterWithoutGUI(JHInvertTransparency.NAME, JHInvertTransparency::new);

        return sub;
    }

    private static JMenu createViewMenu(PixelitorWindow pw, ResourceBundle texts) {
        PMenu viewMenu = new PMenu(texts.getString("view"), 'V');

        viewMenu.add(ZoomMenu.INSTANCE);

        viewMenu.addSeparator();

        viewMenu.add(new OpenImageEnabledAction("Show History...") {
            @Override
            public void onClick() {
                History.showHistory();
            }
        });
        viewMenu.add(new OpenImageEnabledAction("Show Navigator...") {
            @Override
            public void onClick() {
                Navigator.showInDialog();
            }
        });

        viewMenu.addSeparator();

        viewMenu.add(createColorVariationsSubmenu(pw));
        viewMenu.add(new PAction("Color Palette...") {
            @Override
            public void onClick() {
                PalettePanel.showDialog(pw, new FullPalette(),
                    ColorSwatchClickHandler.STANDARD);
            }
        });

        viewMenu.addSeparator();

        viewMenu.add(ShowHideStatusBarAction.INSTANCE);
        viewMenu.add(ShowHideHistogramsAction.INSTANCE, F6);
        viewMenu.add(ShowHideLayersAction.INSTANCE, F7);
        viewMenu.add(ShowHideToolsAction.INSTANCE);
        viewMenu.add(ShowHideAllAction.INSTANCE, F8);

        viewMenu.add(new PAction("Set Default Workspace") {
            @Override
            public void onClick() {
                WorkSpace.resetDefaults(pw);
            }
        });

//        if (!JVM.isLinux) { // see https://github.com/lbalazscs/Pixelitor/issues/140
        var showPixelGridMI = new OpenImageEnabledCheckBoxMenuItem("Show Pixel Grid");
        showPixelGridMI.addActionListener(e ->
            View.setShowPixelGrid(showPixelGridMI.getState()));
        viewMenu.add(showPixelGridMI);
//        }

        viewMenu.addSeparator();

        viewMenu.add(new OpenImageEnabledAction("Add Horizontal Guide...") {
            @Override
            public void onClick() {
                View view = getActiveView();
                Guides.showAddSingleGuideDialog(view, true);
            }
        });

        viewMenu.add(new OpenImageEnabledAction("Add Vertical Guide...") {
            @Override
            public void onClick() {
                View view = getActiveView();
                Guides.showAddSingleGuideDialog(view, false);
            }
        });

        viewMenu.add(new OpenImageEnabledAction("Add Grid Guides...") {
            @Override
            public void onClick() {
                View view = getActiveView();
                Guides.showAddGridDialog(view);
            }
        });

        viewMenu.add(new OpenImageEnabledAction("Clear Guides") {
            @Override
            public void onClick() {
                var comp = getActiveComp();
                comp.clearGuides();
            }
        });

        viewMenu.addSeparator();

        viewMenu.add(createArrangeWindowsSubmenu());

        return viewMenu;
    }

    private static JMenu createColorVariationsSubmenu(PixelitorWindow pw) {
        PMenu variations = new PMenu("Color Variations");
        variations.add(new PAction("Foreground...") {
            @Override
            public void onClick() {
                PalettePanel.showFGVariationsDialog(pw);
            }
        });
        variations.add(new PAction(
            "HSB Mix Foreground with Background...") {
            @Override
            public void onClick() {
                PalettePanel.showHSBMixDialog(pw, true);
            }
        });
        variations.add(new PAction(
            "RGB Mix Foreground with Background...") {
            @Override
            public void onClick() {
                PalettePanel.showRGBMixDialog(pw, true);
            }
        });

        variations.addSeparator();

        variations.add(new PAction("Background...") {
            @Override
            public void onClick() {
                PalettePanel.showBGVariationsDialog(pw);
            }
        });
        variations.add(new PAction(
            "HSB Mix Background with Foreground...") {
            @Override
            public void onClick() {
                PalettePanel.showHSBMixDialog(pw, false);
            }
        });
        variations.add(new PAction(
            "RGB Mix Background with Foreground...") {
            @Override
            public void onClick() {
                PalettePanel.showRGBMixDialog(pw, false);
            }
        });
        return variations;
    }

    private static JMenu createArrangeWindowsSubmenu() {
        PMenu sub = new PMenu("Arrange Windows");

        var cascadeAction = new PAction("Cascade") {
            @Override
            public void onClick() {
                ImageArea.cascadeWindows();
            }
        };
        cascadeAction.setEnabled(ImageArea.currentModeIs(FRAMES));
        sub.add(cascadeAction);

        var tileAction = new PAction("Tile") {
            @Override
            public void onClick() {
                ImageArea.tileWindows();
            }
        };
        tileAction.setEnabled(ImageArea.currentModeIs(FRAMES));
        sub.add(tileAction);

        // make sure that "Cascade" and "Tile" are grayed out in TABS mode
        ImageArea.addUIChangeListener(newMode -> {
            cascadeAction.setEnabled(newMode == FRAMES);
            tileAction.setEnabled(newMode == FRAMES);
        });

        return sub;
    }

    private static JMenu createDevelopMenu(PixelitorWindow pw) {
        PMenu developMenu = new PMenu("Develop", 'D');

        developMenu.add(createDebugSubmenu(pw));
        developMenu.add(createTestSubmenu());
        developMenu.add(createSplashSubmenu());
        developMenu.add(createExperimentalSubmenu());

        developMenu.addFilter(PorterDuff.NAME, PorterDuff::new);

        developMenu.add(new PAction("Filter Creator...") {
            @Override
            public void onClick() {
                FilterCreator.showInDialog();
            }
        });

        developMenu.add(new OpenImageEnabledAction("Dump Event Queue") {
            @Override
            public void onClick() {
                Events.dumpAll();
            }
        });

        developMenu.add(new RestrictedLayerAction("Debug Layer Mask", HAS_LAYER_MASK) {
            @Override
            public void onActiveLayer(Layer layer) {
                ImageLayer imageLayer = (ImageLayer) layer;
                Debug.debugImage(imageLayer.getImage(), "layer image");

                if (imageLayer.hasMask()) {
                    LayerMask layerMask = imageLayer.getMask();
                    BufferedImage maskImage = layerMask.getImage();
                    Debug.debugImage(maskImage, "mask image");

                    BufferedImage transparencyImage = layerMask.getTransparencyImage();
                    Debug.debugImage(transparencyImage, "transparency image");
                }
            }
        });

        developMenu.add(new RestrictedLayerAction("Mask update transparency from BW", HAS_LAYER_MASK) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.getMask().updateFromBWImage();
                layer.getComp().update();
            }
        });

        developMenu.add(new DrawableAction("Debug getCanvasSizedSubImage") {
            @Override
            protected void process(Drawable dr) {
                Debug.debugImage(dr.getCanvasSizedSubImage());
            }
        });

        developMenu.add(new OpenImageEnabledAction("Debug Copy Brush") {
            @Override
            public void onClick() {
                CopyBrush.setDebugBrushImage(true);
            }
        });

        developMenu.add(new PAction("Create All Filters") {
            @Override
            public void onClick() {
                FilterUtils.createAllFilters();
            }
        });

        developMenu.add(new PAction("Change UI") {
            @Override
            public void onClick() {
                ImageArea.changeUI();
            }
        });

        developMenu.add(new PAction("frame size 1366x728") {
            @Override
            public void onClick() {
                PixelitorWindow.get().setSize(1366, 728);
            }
        });

        developMenu.add(new OpenImageEnabledAction("Export with ImageMagick") {
            @Override
            public void onClick() {
                BufferedImage img = getActiveCompositeImage();
                ImageMagick.exportImage(img, new File("out.webp"), ExportSettings.DEFAULTS);
            }
        }, CTRL_ALT_E);

        return developMenu;
    }

    private static JMenu createDebugSubmenu(PixelitorWindow pw) {
        PMenu sub = new PMenu("Debug");

        sub.add(new OpenImageEnabledAction("repaint() on the active image") {
            @Override
            public void onClick() {
                repaintActive();
            }
        });

        sub.add(new OpenImageEnabledAction("update(FULL) on the active image") {
            @Override
            public void onClick() {
                getActiveComp().update(FULL, true);
            }
        });

        sub.add(new PAction("revalidate() the main window") {
            @Override
            public void onClick() {
                pw.getContentPane().revalidate();
            }
        });

        sub.add(new PAction("update all UI") {
            @Override
            public void onClick() {
                Themes.updateAllUI();
            }
        });

        sub.add(new OpenImageEnabledAction("reset the translation of current layer") {
            @Override
            public void onClick() {
                var comp = getActiveComp();
                Layer layer = comp.getActiveLayer();
                if (layer instanceof ContentLayer contentLayer) {
                    contentLayer.setTranslation(0, 0);
                }
                if (layer.hasMask()) {
                    layer.getMask().setTranslation(0, 0);
                }
                comp.update();
            }
        });

        sub.add(new OpenImageEnabledAction("Update Histograms") {
            @Override
            public void onClick() {
                var comp = getActiveComp();
                HistogramsPanel.updateFrom(comp);
            }
        });

        sub.add(new DrawableAction("Debug ImageLayer Images") {
            @Override
            protected void process(Drawable dr) {
                dr.debugImages();
            }
        });

        sub.add(new OpenImageEnabledAction("debug mouse to sys.out") {
            @Override
            public void onClick() {
                GlobalEvents.registerDebugMouseWatching(false);
            }
        });

        sub.add(new PAction("Debug Screen Location") {
            @Override
            public void onClick() {
                Point locationOnScreen = pw.getLocationOnScreen();
                System.out.println("locationOnScreen = " + locationOnScreen);
                Rectangle bounds = pw.getBounds();
                System.out.println("bounds = " + bounds);
                GraphicsConfiguration gc = pw.getGraphicsConfiguration();
                Rectangle gcBounds = gc.getBounds();
                System.out.println("gcBounds = " + gcBounds);
            }
        });

        return sub;
    }

    private static JMenu createTestSubmenu() {
        PMenu sub = new PMenu("Test");

        sub.addFilter("ParamTest", ParamTest::new);

        sub.add(new PAction("Random GUI Test") {
            @Override
            public void onClick() {
                RandomGUITest.start();
            }
        }, CTRL_R);

        sub.add(new OpenImageEnabledAction("Save Current Image in All Formats...") {
            @Override
            public void onClick() {
                IO.saveCurrentImageInAllFormats();
            }
        });

        return sub;
    }

    private static JMenu createSplashSubmenu() {
        PMenu sub = new PMenu("Splash");

        sub.add(new PAction("Create Splash Image") {
            @Override
            public void onClick() {
                SplashImageCreator.createSplashComp();
            }
        }, CTRL_K);

        sub.add(new PAction("Save Many Splash Images...") {
            @Override
            public void onClick() {
                SplashImageCreator.saveManySplashImages(64);
            }
        });

        return sub;
    }

    private static JMenu createExperimentalSubmenu() {
        PMenu sub = new PMenu("Experimental");

        sub.addFilter(Contours.NAME, Contours::new);
        sub.addFilter(JHCustomHalftone.NAME, JHCustomHalftone::new);

        sub.addSeparator();

        sub.addFilter(BlurredShapeTester.NAME, BlurredShapeTester::new);
        sub.addFilter(XYZTest.NAME, XYZTest::new);
        sub.addFilter(Sphere3D.NAME, Sphere3D::new);

        return sub;
    }

    private static JMenu createHelpMenu(PixelitorWindow pw, ResourceBundle texts) {
        PMenu helpMenu = new PMenu(GUIText.HELP, 'H');

        String tipOfTheDayText = texts.getString("tip_of_the_day");
        helpMenu.add(new PAction(tipOfTheDayText) {
            @Override
            public void onClick() {
                TipsOfTheDay.showTips(pw, true);
            }
        });

        helpMenu.addSeparator();

        helpMenu.add(new OpenInBrowserAction("Report an Issue...",
            "https://github.com/lbalazscs/Pixelitor/issues"));

        helpMenu.add(new PAction("Internal State...") {
            @Override
            public void onClick() {
                Debug.showInternalState();
            }
        });

        helpMenu.add(new PAction("Check for Update...") {
            @Override
            public void onClick() {
                UpdatesCheck.checkForUpdates();
            }
        });

        String aboutText = texts.getString("about");
        helpMenu.add(new PAction(aboutText) {
            @Override
            public void onClick() {
                AboutDialog.showDialog(aboutText);
            }
        });

        return helpMenu;
    }
}
