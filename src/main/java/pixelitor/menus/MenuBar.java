/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.compactions.*;
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
import pixelitor.filters.transitions.BlindsTransition;
import pixelitor.filters.transitions.CheckerboardTransition;
import pixelitor.filters.transitions.GooTransition;
import pixelitor.filters.transitions.ShapesGridTransition;
import pixelitor.filters.util.FilterAction;
import pixelitor.filters.util.FilterSearchPanel;
import pixelitor.filters.util.Filters;
import pixelitor.gui.*;
import pixelitor.gui.utils.OpenViewEnabledAction;
import pixelitor.gui.utils.PAction;
import pixelitor.gui.utils.RestrictedLayerAction;
import pixelitor.gui.utils.RestrictedLayerAction.Condition;
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
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ResourceBundle;

import static pixelitor.Composition.LayerAdder.Position.ABOVE_ACTIVE;
import static pixelitor.Views.*;
import static pixelitor.colors.FillType.*;
import static pixelitor.compactions.Flip.Direction.HORIZONTAL;
import static pixelitor.compactions.Flip.Direction.VERTICAL;
import static pixelitor.gui.ImageArea.Mode.FRAMES;
import static pixelitor.gui.utils.RestrictedLayerAction.Condition.*;
import static pixelitor.layers.LayerMaskAddType.*;
import static pixelitor.layers.LayerMoveAction.*;
import static pixelitor.utils.Keys.*;
import static pixelitor.utils.QuadrantAngle.*;

/**
 * The main menu bar of the app window.
 */
public class MenuBar extends JMenuBar {
    public MenuBar(PixelitorWindow pw) {
        ResourceBundle texts = Texts.getResources();

        add(createFileMenu(pw, texts));
        add(createEditMenu(texts));
        add(createLayerMenu(texts));
        add(createSelectMenu(texts));
        add(createImageMenu(texts));
        add(createColorMenu());
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

        fileMenu.add(new PAction(texts.getString("open") + "...",
            FileChoosers::openAsync), CTRL_O);

        fileMenu.add(RecentFilesMenu.INSTANCE);

        fileMenu.addSeparator();

        fileMenu.add(new OpenViewEnabledAction(texts.getString("save"),
            comp -> IO.save(comp, false)), CTRL_S);

        fileMenu.add(new OpenViewEnabledAction(texts.getString("save_as") + "...",
            comp -> IO.save(comp, true)), CTRL_SHIFT_S);

        fileMenu.add(new OpenViewEnabledAction(
            texts.getString("export_optimized_jpeg") + "...",
            comp -> OptimizedJpegSavePanel.showInDialog(comp.getCompositeImage())));

        fileMenu.add(createImageMagickSubmenu());

        fileMenu.addSeparator();

        fileMenu.add(new OpenViewEnabledAction(
            texts.getString("export_layer_animation") + "...",
            LayerAnimExport::start));

        fileMenu.add(new DrawableAction(texts.getString("export_tweening_animation")) {
            @Override
            protected void process(Drawable dr) {
                new TweenWizard(dr).showDialog(pw);
            }
        });

        fileMenu.addSeparator();

        // reload
        fileMenu.add(new OpenViewEnabledAction(
            texts.getString("reload"),
            comp -> comp.getView().reloadAsync()), F12);

        fileMenu.add(new OpenViewEnabledAction(
            texts.getString("show_metadata") + "...",
            MetaDataPanel::showInDialog));

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
        fileMenu.add(new PAction(exitName, () -> Pixelitor.exitApp(pw)));

        return fileMenu;
    }

    private static JMenu createImageMagickSubmenu() {
        PMenu imMenu = new PMenu("ImageMagick");

        imMenu.add(new OpenViewEnabledAction("Export...", ImageMagick::export));
        imMenu.add(new PAction("Import...", ImageMagick::importComposition));

        return imMenu;
    }

    private static JMenu createAutomateSubmenu(PixelitorWindow pw, ResourceBundle texts) {
        PMenu automateMenu = new PMenu(texts.getString("automate"));

        automateMenu.add(new PAction(
            texts.getString("batch_resize") + "...",
            BatchResize::showDialog));

        automateMenu.add(new DrawableAction(texts.getString("batch_filter")) {
            @Override
            protected void process(Drawable dr) {
                new BatchFilterWizard(dr).showDialog(pw);
            }
        });

        automateMenu.add(new OpenViewEnabledAction(
            texts.getString("export_layers_to_png") + "...",
            IO::exportLayersToPNGAsync));

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
        editMenu.add(new PAction(prefsMenuName, PreferencesPanel::showInDialog));

        return editMenu;
    }

    private static JMenu createLayerMenu(ResourceBundle texts) {
        var layersMenu = new PMenu(texts.getString("layer"), 'L');

        layersMenu.add(AddNewLayerAction.INSTANCE);
        layersMenu.add(DeleteActiveLayerAction.INSTANCE, DELETE);
        layersMenu.add(DuplicateLayerAction.INSTANCE, CTRL_J);

        layersMenu.addSeparator();

        // merge down
        var mergeDown = new OpenViewEnabledAction(GUIText.MERGE_DOWN,
            Composition::mergeActiveLayerDown);
        mergeDown.setToolTip(GUIText.MERGE_DOWN_TT);
        layersMenu.add(mergeDown, CTRL_E);

        // flatten image
        var flattenImage = new OpenViewEnabledAction(
            texts.getString("flatten_image"),
            Composition::flattenImage);
        flattenImage.setToolTip(texts.getString("flatten_image_tt"));
        layersMenu.add(flattenImage);

        // new layer from visible
        var newFromVisible = new OpenViewEnabledAction(
            texts.getString("new_from_visible"),
            Composition::addNewLayerFromComposite);
        newFromVisible.setToolTip(texts.getString("new_from_visible_tt"));
        layersMenu.add(newFromVisible, CTRL_SHIFT_ALT_E);

        layersMenu.add(createLayerStackSubmenu(texts));
        layersMenu.add(createLayerMaskSubmenu(texts));

        if (AppContext.enableExperimentalFeatures) {
            layersMenu.addSeparator();

            layersMenu.add(createAdjustmentLayersSubmenu());
            layersMenu.add(createColorFillLayerSubmenu());
            layersMenu.add(createGradientFillLayerSubmenu());
            layersMenu.add(createLayerGroupsSubmenu());
            layersMenu.add(createShapeLayerSubmenu());
            layersMenu.add(createSmartObjectSubmenu());
        }

        layersMenu.add(createTextLayerSubmenu(texts));

        return layersMenu;
    }

    private static JMenu createLayerStackSubmenu(ResourceBundle texts) {
        var sub = new PMenu(texts.getString("layer_stack"));

        sub.add(MOVE_LAYER_UP, CTRL_PAGE_UP);

        sub.add(MOVE_LAYER_DOWN, CTRL_PAGE_DOWN);

        // layer to top
        var layerToTop = new OpenViewEnabledAction(LAYER_TO_TOP,
            comp -> comp.getActiveHolder().moveActiveLayerToTop());
        layerToTop.setToolTip(texts.getString("layer_to_top_tt"));
        sub.add(layerToTop, CTRL_ALT_PAGE_UP);

        // layer_to_bottom
        var layerToBottom = new OpenViewEnabledAction(LAYER_TO_BOTTOM,
            comp -> comp.getActiveHolder().moveActiveLayerToBottom());
        layerToBottom.setToolTip(texts.getString("layer_to_bottom_tt"));
        sub.add(layerToBottom, CTRL_ALT_PAGE_DOWN);

        sub.addSeparator();

        // raise layer selection
        var raiseLayerSelection = new OpenViewEnabledAction(RAISE_LAYER_SELECTION,
            comp -> comp.getActiveHolder().raiseLayerSelection());
        raiseLayerSelection.setToolTip(texts.getString("raise_layer_selection_tt"));
        sub.add(raiseLayerSelection, PAGE_UP);

        var lowerLayerSelection = new OpenViewEnabledAction(LOWER_LAYER_SELECTION,
            comp -> comp.getActiveHolder().lowerLayerSelection());
        lowerLayerSelection.setToolTip(texts.getString("lower_layer_selection_tt"));
        sub.add(lowerLayerSelection, PAGE_DOWN);

        sub.addSeparator();

        sub.add(new OpenViewEnabledAction("Isolate",
            Composition::isolateRoot));

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

        sub.add(new OpenViewEnabledAction(
            MaskFromColorRangePanel.NAME + "...",
            MaskFromColorRangePanel::showInDialog));

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
                // useful because bugs would be spotted early
                layer.update();
            }
        });

        sub.addSeparator();

        MaskViewMode.NORMAL.addToMenuBar(sub);
        MaskViewMode.SHOW_MASK.addToMenuBar(sub);
        MaskViewMode.EDIT_MASK.addToMenuBar(sub);
        MaskViewMode.RUBYLITH.addToMenuBar(sub);

        return sub;
    }

    private static JMenu createTextLayerSubmenu(ResourceBundle texts) {
        PMenu sub = new PMenu(texts.getString("text_layer"));

        sub.add(new OpenViewEnabledAction("New Text Layer...",
            TextLayer::createNew), T);

        Condition isTextLayer = new ClassCondition(TextLayer.class, "text layer");

        sub.add(new RestrictedLayerAction("Edit Text Layer...", isTextLayer) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.edit();
            }
        }, CTRL_T);

        sub.add(new RestrictedLayerAction("Rasterize Text Layer", isTextLayer) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.replaceWithRasterized();
            }
        });

        sub.add(new RestrictedLayerAction("Selection from Text", isTextLayer) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.getComp().createSelectionFromText();
            }
        });

        return sub;
    }

    private static JMenu createLayerGroupsSubmenu() {
        PMenu sub = new PMenu("Layer Groups");

        sub.add(new OpenViewEnabledAction("New Empty Group",
            comp -> comp.getHolderForNewLayers().addEmptyGroup()), CTRL_G);

        sub.add(new OpenViewEnabledAction("Convert Visible to Group",
            comp -> comp.getHolderForGrouping().convertVisibleLayersToGroup()), CTRL_SHIFT_G);

//        Condition isLayerGroup = new ClassCondition(LayerGroup.class, "layer group");

        sub.add(new OpenViewEnabledAction("Ungroup",
            comp -> comp.getActiveLayer().unGroup()), CTRL_U);

        return sub;
    }

    private static JMenu createAdjustmentLayersSubmenu() {
        PMenu sub = new PMenu("Adjustment Layer");

        for (Action action : AddAdjLayerAction.actions) {
            sub.add(action);
        }

        return sub;
    }

    private static JMenu createSmartObjectSubmenu() {
        PMenu sub = new PMenu("Smart Object");

        sub.add(new OpenViewEnabledAction("Convert Layer to Smart Object",
            comp -> comp.getActiveLayer().replaceWithSmartObject()));

        sub.add(new OpenViewEnabledAction("Convert All to Smart Object",
            Composition::replaceWithSmartObject));

        sub.add(new OpenViewEnabledAction("Convert Visible to Smart Object",
            Composition::convertVisibleLayersToSmartObject));
        sub.addSeparator();

        Condition isSmartObject = new ClassCondition(SmartObject.class, "smart object");

        sub.add(new RestrictedLayerAction("Rasterize Smart Object", isSmartObject) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.replaceWithRasterized();
            }
        });

        sub.add(new RestrictedLayerAction("Edit Contents", isSmartObject) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.edit();
            }
        });

        sub.add(new OpenViewEnabledAction("Edit All Nested Contents",
            comp -> comp.forAllNestedSmartObjects(SmartObject::edit)), CTRL_ALT_O);

        sub.add(new RestrictedLayerAction("Edit Smart Filter", isSmartObject) {
            @Override
            public void onActiveLayer(Layer layer) {
                ((SmartObject) layer).editSelectedSmartFilter();
            }
        }, CTRL_SHIFT_E);

        sub.add(new OpenViewEnabledAction("Add Linked...", comp -> {
            File file = FileChoosers.getSupportedOpenFile();
            if (file == null) {
                return; // the user cancelled the dialog
            }

            IO.loadCompAsync(file)
                .thenAcceptAsync(content -> {
                    SmartObject so = new SmartObject(file, comp, content);
                    new Composition.LayerAdder(comp).atPosition(ABOVE_ACTIVE).add(so);
                }, Threads.onEDT)
                .exceptionally(Messages::showExceptionOnEDT);
        }));

        sub.add(new RestrictedLayerAction("Clone", isSmartObject) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.getComp().shallowDuplicate((SmartObject) layer);
            }
        });

        return sub;
    }

    private static JMenu createColorFillLayerSubmenu() {
        PMenu sub = new PMenu("Color Fill Layer");

        sub.add(new OpenViewEnabledAction("New Color Fill Layer...",
            ColorFillLayer::createNew));

        Condition isColorFillLayer = new ClassCondition(ColorFillLayer.class, "color fill layer");

        sub.add(new RestrictedLayerAction("Edit Color Fill Layer...", isColorFillLayer) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.edit();
            }
        });

        sub.add(new RestrictedLayerAction("Rasterize Color Fill Layer", isColorFillLayer) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.replaceWithRasterized();
            }
        });

        return sub;
    }

    private static JMenu createGradientFillLayerSubmenu() {
        PMenu sub = new PMenu("Gradient Fill Layer");

        sub.add(new OpenViewEnabledAction("New Gradient Fill Layer...",
            GradientFillLayer::createNew), CTRL_ALT_G);

        Condition isGradientFillLayer = new ClassCondition(GradientFillLayer.class, "gradient fill layer");

        sub.add(new RestrictedLayerAction("Rasterize Gradient Fill Layer", isGradientFillLayer) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.replaceWithRasterized();
            }
        });

        return sub;
    }

    private static JMenu createShapeLayerSubmenu() {
        PMenu sub = new PMenu("Shape Layer");

        sub.add(new OpenViewEnabledAction("New Shape Layer...",
            ShapesLayer::createNew), CTRL_ALT_S);

        Condition isShapeLayer = new ClassCondition(ShapesLayer.class, "shape layer");

        sub.add(new RestrictedLayerAction("Rasterize Shape Layer", isShapeLayer) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.replaceWithRasterized();
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

        // selection crop
        imageMenu.add(SelectionActions.getCrop());

        imageMenu.add(new OpenViewEnabledAction("Crop to Content", Crop::contentCrop));

        imageMenu.addSeparator();

        // resize
        imageMenu.add(new OpenViewEnabledAction("Resize...",
            ResizePanel::showInDialog), CTRL_ALT_I);

        imageMenu.add(new OpenViewEnabledAction("Duplicate",
                comp -> addNew(comp.copy(CopyType.DUPLICATE_COMP, true))));

        if (AppContext.enableImageMode) {
            imageMenu.add(createModeSubmenu());
        }

        imageMenu.addSeparator();

        imageMenu.add(EnlargeCanvas.getAction());

        var fitCanvasToLayers = new OpenViewEnabledAction(
            texts.getString("fit_canvas_to_layers"),
            Composition::fitCanvasToLayers);
        fitCanvasToLayers.setToolTip(texts.getString("fit_canvas_to_layers_tt"));
        imageMenu.add(fitCanvasToLayers);

        imageMenu.add(new OpenViewEnabledAction(
            "Layer to Canvas Size",
            Composition::activeLayerToCanvasSize));

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

    private static JMenu createColorMenu() {
        PMenu colorsMenu = new PMenu(GUIText.COLOR, 'C');

        colorsMenu.addFilter(ColorBalance.NAME, ColorBalance::new, CTRL_B);
        colorsMenu.addFilter(HueSat.NAME, HueSat::new);
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

        filterMenu.add(new OpenViewEnabledAction("Find Filter...",
            comp -> {
                FilterAction action = FilterSearchPanel.showInDialog("Find Filter");
                if (action != null) {
                    action.actionPerformed(null);
                }
            }), F3);

        filterMenu.add(RepeatLast.REPEAT_LAST_ACTION, CTRL_F);
        filterMenu.add(RepeatLast.SHOW_LAST_ACTION, CTRL_ALT_F);

        filterMenu.addSeparator();

        filterMenu.add(createArtisticSubmenu(texts));
        filterMenu.add(createBlurSharpenSubmenu(texts));
        filterMenu.add(createDisplaceSubmenu(texts));
        filterMenu.add(createDistortSubmenu(texts));
        filterMenu.add(createFindEdgesSubmenu(texts));
        filterMenu.add(createLightSubmenu(texts));
        filterMenu.add(createNoiseSubmenu(texts));
        filterMenu.add(createOtherSubmenu());
        filterMenu.add(createRenderSubmenu(texts));
        filterMenu.add(createTransitionsSubmenu());

        return filterMenu;
    }

    private static JMenu createArtisticSubmenu(ResourceBundle texts) {
        PMenu sub = new PMenu(texts.getString("artistic"));

        sub.addFilter(ComicBook.NAME, ComicBook::new);
        sub.addFilter(JHCrystallize.NAME, JHCrystallize::new);
        sub.addFilter(JHEmboss.NAME, JHEmboss::new);
        sub.addFilter(JHOilPainting.NAME, JHOilPainting::new);
        sub.addFilter(Orton.NAME, Orton::new);
        sub.addFilter(PhotoCollage.NAME, PhotoCollage::new);
        sub.addFilter(JHPointillize.NAME, JHPointillize::new);
        sub.addFilter(JHSmear.NAME, JHSmear::new);
        sub.addFilter(Spheres.NAME, Spheres::new);
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

    private static JMenu createBlurSharpenSubmenu(ResourceBundle texts) {
        PMenu sub = new PMenu(texts.getString("blur")
                              + "/" + texts.getString("sharpen"));

        sub.addFilter(JHBoxBlur.NAME, JHBoxBlur::new);
        sub.addFilter(JHFocus.NAME, JHFocus::new);
        sub.addFilter(JHGaussianBlur.NAME, JHGaussianBlur::new);
        sub.addFilter(JHLensBlur.NAME, JHLensBlur::new);
        sub.addFilter(JHMotionBlur.NAME, JHMotionBlur::new);
        sub.addFilter(JHSmartBlur.NAME, JHSmartBlur::new);
        sub.addFilter(JHSpinZoomBlur.NAME, JHSpinZoomBlur::new);
        sub.addSeparator();
        sub.addFilter(JHUnsharpMask.NAME, JHUnsharpMask::new);

        return sub;
    }

    private static JMenu createDisplaceSubmenu(ResourceBundle texts) {
        PMenu sub = new PMenu(texts.getString("displace"));

        sub.addFilter(DisplacementMap.NAME, DisplacementMap::new);
        sub.addFilter(DrunkVision.NAME, DrunkVision::new);
        sub.addFilter(JHKaleidoscope.NAME, JHKaleidoscope::new);
        sub.addFilter(Mirror.NAME, Mirror::new);
        sub.addFilter(JHOffset.NAME, JHOffset::new);
        sub.addFilter(Slice.NAME, Slice::new);
//        sub.addFilter(SlippingTiles.NAME, SlippingTiles::new);
        sub.addFilter(TileSeamless.NAME, TileSeamless::new);
        sub.addFilter(JHVideoFeedback.NAME, JHVideoFeedback::new);

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

        sub.addFilter(JHFrostedGlass.NAME, JHFrostedGlass::new);
        sub.addFilter(GlassTiles.NAME, GlassTiles::new);
        sub.addFilter(PolarTiles.NAME, PolarTiles::new);

        sub.addSeparator();

        sub.addFilter(LittlePlanet.NAME, LittlePlanet::new);
        sub.addFilter(JHPolarCoordinates.NAME, JHPolarCoordinates::new);
        sub.addFilter(JHWrapAroundArc.NAME, JHWrapAroundArc::new);

        return sub;
    }

    private static JMenu createFindEdgesSubmenu(ResourceBundle texts) {
        PMenu sub = new PMenu(texts.getString("find_edges"));

        sub.addFilter(Canny.NAME, Canny::new);
        sub.addFilter(JHConvolutionEdge.NAME, JHConvolutionEdge::new);
        sub.addFilter(JHDifferenceOfGaussians.NAME, JHDifferenceOfGaussians::new);

        String laplacianFilterName = "Laplacian";
        sub.addNoGrayForwardingFilter(laplacianFilterName,
            () -> new LaplaceFilter(laplacianFilterName));

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

    private static JMenu createOtherSubmenu() {
        PMenu sub = new PMenu("Other");

        sub.addFilter(JHDropShadow.NAME, JHDropShadow::new);
        sub.addFilter(Morphology.NAME, Morphology::new);
        sub.addFilter("Random Filter", RandomFilter::new);

        // the text as filter is still useful for batch operations
        sub.addFilter("Text", TextFilter::new);

        sub.addFilter("Transform Layer", TransformLayer::new);

        sub.addSeparator();

        sub.add(Convolve.createFilterAction(3));
        sub.add(Convolve.createFilterAction(5));

        sub.addSeparator();

        sub.addFilter(ChannelToTransparency.NAME, ChannelToTransparency::new);
        sub.addFilterWithoutGUI(JHInvertTransparency.NAME, JHInvertTransparency::new);

        return sub;
    }

    private static JMenu createRenderSubmenu(ResourceBundle texts) {
        PMenu sub = new PMenu(texts.getString("render"));

        sub.addFilter(Clouds.NAME, Clouds::new);
        sub.addFilter(JHPlasma.NAME, JHPlasma::new);
        sub.addFilter(ValueNoise.NAME, ValueNoise::new);

        sub.addSeparator();

        sub.addFilter(AbstractLights.NAME, AbstractLights::new);
        sub.addFilter(JHBrushedMetal.NAME, JHBrushedMetal::new);
        sub.addFilter(JHCaustics.NAME, JHCaustics::new);
        sub.addFilter(JHCells.NAME, JHCells::new);
        sub.addFilter(FlowField.NAME, FlowField::new);
        sub.addFilter(Marble.NAME, Marble::new);
        sub.addFilter(Voronoi.NAME, Voronoi::new);
        sub.addFilter(JHWood.NAME, JHWood::new);

        sub.addSeparator();

        sub.add(createRenderFractalsSubmenu(texts));
        sub.add(createRenderGeometrySubmenu());
        sub.add(createRenderShapesSubmenu());

        return sub;
    }

    private static JMenu createRenderShapesSubmenu() {
        PMenu sub = new PMenu("Shapes");

        sub.addFilter(FlowerOfLife.NAME, FlowerOfLife::new);
        sub.addFilter(Grid.NAME, Grid::new);
        sub.addFilter(Lissajous.NAME, Lissajous::new);
        sub.addFilter(MysticRose.NAME, MysticRose::new);
        sub.addFilter(SpiderWeb.NAME, SpiderWeb::new);
        sub.addFilter(Spiral.NAME, Spiral::new);
        sub.addFilter(Spirograph.NAME, Spirograph::new);

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

        sub.addFilter(BorderMask.NAME, BorderMask::new);
        sub.addFilter(ConcentricShapes.NAME, ConcentricShapes::new);
        sub.addFilter(JHCheckerFilter.NAME, JHCheckerFilter::new);
        sub.addFilter(Rose.NAME, Rose::new);
        sub.addFilter(Starburst.NAME, Starburst::new);
        sub.addFilter(Truchet.NAME, Truchet::new);

        return sub;
    }

    private static JMenu createTransitionsSubmenu() {
        PMenu sub = new PMenu("Transitions");

        sub.addFilter(Transition2D.NAME, Transition2D::new);
        sub.addSeparator();
        sub.addFilter(BlindsTransition.NAME, BlindsTransition::new);
        sub.addFilter(CheckerboardTransition.NAME, CheckerboardTransition::new);
        sub.addFilter(GooTransition.NAME, GooTransition::new);
        sub.addFilter(ShapesGridTransition.NAME, ShapesGridTransition::new);

        return sub;
    }

    private static JMenu createViewMenu(PixelitorWindow pw, ResourceBundle texts) {
        PMenu viewMenu = new PMenu(texts.getString("view"), 'V');

        viewMenu.add(ZoomMenu.INSTANCE);

        viewMenu.addSeparator();

        viewMenu.add(new OpenViewEnabledAction("Show History...",
            comp -> History.showHistory()));
        viewMenu.add(new OpenViewEnabledAction("Show Navigator...",
            comp -> Navigator.showInDialog(comp.getView())));

        viewMenu.addSeparator();

        viewMenu.add(createColorVariationsSubmenu(pw));
        viewMenu.add(new PAction("Color Palette...", () ->
            PalettePanel.showDialog(pw, new FullPalette(),
                ColorSwatchClickHandler.STANDARD)));

        viewMenu.addSeparator();

        viewMenu.add(ShowHideStatusBarAction.INSTANCE);
        viewMenu.add(ShowHideHistogramsAction.INSTANCE, F6);
        viewMenu.add(ShowHideLayersAction.INSTANCE, F7);
        viewMenu.add(ShowHideToolsAction.INSTANCE);
        viewMenu.add(ShowHideAllAction.INSTANCE, F8);

        viewMenu.add(new PAction("Set Default Workspace", () ->
            WorkSpace.resetDefaults(pw)));

//        if (!JVM.isLinux) { // see https://github.com/lbalazscs/Pixelitor/issues/140
        var showPixelGridMI = new OpenViewEnabledCheckBoxMenuItem("Show Pixel Grid");
        showPixelGridMI.addActionListener(e ->
            View.setShowPixelGrid(showPixelGridMI.getState()));
        viewMenu.add(showPixelGridMI);
//        }

        viewMenu.addSeparator();

        viewMenu.add(new OpenViewEnabledAction("Add Horizontal Guide...",
            comp -> Guides.showAddSingleGuideDialog(comp.getView(), true)));

        viewMenu.add(new OpenViewEnabledAction("Add Vertical Guide...",
            comp -> Guides.showAddSingleGuideDialog(comp.getView(), false)));

        viewMenu.add(new OpenViewEnabledAction("Add Grid Guides...",
            comp -> Guides.showAddGridDialog(comp.getView())));

        viewMenu.add(new OpenViewEnabledAction("Clear Guides",
            Composition::clearGuides));

        viewMenu.addSeparator();

        viewMenu.add(createArrangeWindowsSubmenu());

        return viewMenu;
    }

    private static JMenu createColorVariationsSubmenu(PixelitorWindow pw) {
        PMenu variations = new PMenu("Color Variations");
        variations.add(new PAction("Foreground...", () ->
            PalettePanel.showFGVariationsDialog(pw)));
        variations.add(new PAction(
            "HSB Mix Foreground with Background...", () ->
            PalettePanel.showHSBMixDialog(pw, true)));
        variations.add(new PAction(
            "RGB Mix Foreground with Background...", () ->
            PalettePanel.showRGBMixDialog(pw, true)));

        variations.addSeparator();

        variations.add(new PAction("Background...", () ->
            PalettePanel.showBGVariationsDialog(pw)));
        variations.add(new PAction(
            "HSB Mix Background with Foreground...", () ->
            PalettePanel.showHSBMixDialog(pw, false)));
        variations.add(new PAction(
            "RGB Mix Background with Foreground...", () ->
            PalettePanel.showRGBMixDialog(pw, false)));
        return variations;
    }

    private static JMenu createArrangeWindowsSubmenu() {
        PMenu sub = new PMenu("Arrange Windows");

        var cascadeAction = new PAction("Cascade", ImageArea::cascadeWindows);
        cascadeAction.setEnabled(ImageArea.currentModeIs(FRAMES));
        sub.add(cascadeAction);

        var tileAction = new PAction("Tile", ImageArea::tileWindows);
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

        developMenu.add(createDebugSubmenu());
        developMenu.add(createExperimentalSubmenu());
        developMenu.add(createManualSubmenu(pw));
        developMenu.add(createSplashSubmenu());
        developMenu.add(createTestSubmenu());

        Condition isSmartObject = new ClassCondition(SmartObject.class, "smart object");
        abstract class ActiveSmartObjectAction extends RestrictedLayerAction {
            protected ActiveSmartObjectAction(String name) {
                super(name, isSmartObject);
            }

            @Override
            protected void onActiveLayer(Layer layer) {
                onActiveSO((SmartObject) layer);
            }

            protected abstract void onActiveSO(SmartObject so);
        }

        developMenu.add(new ActiveSmartObjectAction("Edit 0") {
            @Override
            protected void onActiveSO(SmartObject so) {
                so.getSmartFilter(0).edit();
            }
        });
        developMenu.add(new ActiveSmartObjectAction("Edit 1") {
            @Override
            protected void onActiveSO(SmartObject so) {
                so.getSmartFilter(1).edit();
            }
        });
        developMenu.add(new ActiveSmartObjectAction("Edit 2") {
            @Override
            protected void onActiveSO(SmartObject so) {
                so.getSmartFilter(2).edit();
            }
        });

        developMenu.add(new ActiveSmartObjectAction("Print Smart Filters") {
            @Override
            protected void onActiveSO(SmartObject so) {
                String s = so.debugSmartFilters();
                System.out.printf("%s%n", s);
            }
        });

        return developMenu;
    }

    private static JMenu createDebugSubmenu() {
        PMenu sub = new PMenu("Debug");

        sub.add(new PAction("Copy Internal State to Clipboard",
                Debug::copyInternalState), CTRL_ALT_D);

        sub.add(new OpenViewEnabledAction("Debug Active Composite Image",
                comp -> Debug.debugImage(comp.getCompositeImage(), "Composite of " + comp.getDebugName())));

        sub.add(new DrawableAction("Debug ImageLayer Images") {
            @Override
            protected void process(Drawable dr) {
                dr.debugImages();
            }
        });

        sub.add(new OpenViewEnabledAction("Debug Mouse to System.out",
            comp -> GlobalEvents.registerDebugMouseWatching(false)));

        sub.add(new OpenViewEnabledAction("Dump Event Queue",
            comp -> Events.dumpAll()));

        sub.add(new RestrictedLayerAction("Debug Layer Mask", HAS_LAYER_MASK) {
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

        sub.add(new DrawableAction("Debug getCanvasSizedSubImage()") {
            @Override
            protected void process(Drawable dr) {
                Debug.debugImage(dr.getCanvasSizedSubImage());
            }
        });

        sub.add(new PAction("Debug Copy Brush",
            () -> CopyBrush.setDebugBrushImage(true)));

        sub.addFilter(PorterDuff.NAME, PorterDuff::new);

        sub.add(new PAction("Debug All Comp Names",
            Debug::debugAllDebugNames));

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
        sub.addFilter(PoissonDiskTester.NAME, PoissonDiskTester::new);

        return sub;
    }

    private static JMenu createManualSubmenu(PixelitorWindow pw) {
        PMenu sub = new PMenu("Manual");

        sub.add(new PAction("repaint() the main window", pw::repaint));

        sub.add(new OpenViewEnabledAction("repaint() on the active image",
            comp -> repaintActive()));

        sub.add(new PAction("revalidate() the main window", () ->
            pw.getContentPane().revalidate()));

        sub.add(new PAction("Themes.updateAllUI()", Themes::updateAllUI));

        sub.add(new OpenViewEnabledAction("update() on the active image",
            Composition::update));

        sub.add(new OpenViewEnabledAction("update() on the active holder",
            comp -> comp.getActiveHolder().update()));

        sub.add(new OpenViewEnabledAction("update() on the active layer",
            comp -> comp.getActiveLayer().update()));

        sub.addSeparator();

        sub.add(new PAction("Change UI", ImageArea::changeUI));

        sub.add(new OpenViewEnabledAction("Export with ImageMagick",
            comp -> {
                BufferedImage img = comp.getCompositeImage();
                ImageMagick.exportImage(img, new File("out.webp"), ExportSettings.DEFAULTS);
            }), CTRL_ALT_E);

        sub.add(new OpenViewEnabledAction(
            "Reset the translation of current layer",
            comp -> {
                Layer layer = comp.getActiveLayer();
                if (layer instanceof ContentLayer contentLayer) {
                    contentLayer.setTranslation(0, 0);
                }
                if (layer.hasMask()) {
                    layer.getMask().setTranslation(0, 0);
                }
                comp.update();
            }));

        sub.add(new OpenViewEnabledAction("Update Histograms",
            HistogramsPanel::updateFrom));

        sub.add(new RestrictedLayerAction("Update Mask Transparency from BW", HAS_LAYER_MASK) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.getMask().updateTransparencyImage();
                layer.update();
            }
        });

        return sub;
    }

    private static JMenu createSplashSubmenu() {
        PMenu sub = new PMenu("Splash");

        sub.add(new PAction("Create Splash Image",
            SplashImageCreator::createSplashComp), CTRL_K);

        sub.add(new PAction("Save Many Splash Images...", () ->
            SplashImageCreator.saveManySplashImages(64)));

        return sub;
    }

    private static JMenu createTestSubmenu() {
        PMenu sub = new PMenu("Test");

        sub.add(new PAction("Create All Filters", Filters::createAllFilters));

        sub.addFilter("ParamTest", ParamTest::new);

        sub.add(new PAction("Random GUI Test", RandomGUITest::start), CTRL_R);

        sub.add(new OpenViewEnabledAction(
            "Save in All Formats...",
            IO::saveInAllFormats));

        sub.addSeparator();

        sub.add(new OpenViewEnabledAction("Add All Smart Filters",
            Debug::addAllSmartFilters));

        sub.add(new PAction("Test Filter Constructors",
            Filters::testFilterConstructors));

        sub.add(new PAction("Serialize All Filters",
            Debug::serializeAllFilters));
        sub.add(new PAction("Deserialize All Filters",
            Debug::deserializeAllFilters));

        return sub;
    }

    private static JMenu createHelpMenu(PixelitorWindow pw, ResourceBundle texts) {
        PMenu helpMenu = new PMenu(GUIText.HELP, 'H');

        String tipOfTheDayText = texts.getString("tip_of_the_day");
        helpMenu.add(new PAction(tipOfTheDayText, () ->
            TipsOfTheDay.showTips(pw, true)));

        helpMenu.addSeparator();

        helpMenu.add(new OpenInBrowserAction("Report an Issue...",
            "https://github.com/lbalazscs/Pixelitor/issues"));

        helpMenu.add(new PAction("Internal State...",
            Debug::showInternalState));

        helpMenu.add(new PAction("Check for Update...",
            UpdatesCheck::checkForUpdates));

        String aboutText = texts.getString("about");
        helpMenu.add(new PAction(aboutText, () ->
            AboutDialog.showDialog(aboutText)));

        return helpMenu;
    }
}
