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
import pixelitor.filters.gmic.*;
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
import pixelitor.gui.utils.RestrictedLayerAction;
import pixelitor.gui.utils.TaskAction;
import pixelitor.gui.utils.Themes;
import pixelitor.gui.utils.ViewEnabledAction;
import pixelitor.guides.AddGridGuidesPanel;
import pixelitor.guides.AddSingleGuidePanel;
import pixelitor.history.History;
import pixelitor.history.RedoAction;
import pixelitor.history.UndoAction;
import pixelitor.io.FileChoosers;
import pixelitor.io.FileIO;
import pixelitor.io.LayerAnimation;
import pixelitor.io.OptimizedJpegExportPanel;
import pixelitor.io.magick.ExportSettings;
import pixelitor.io.magick.ImageMagick;
import pixelitor.layers.*;
import pixelitor.menus.edit.CopyAction;
import pixelitor.menus.edit.FadeAction;
import pixelitor.menus.edit.PasteAction;
import pixelitor.menus.edit.PasteTarget;
import pixelitor.menus.file.MetaDataPanel;
import pixelitor.menus.file.PrintAction;
import pixelitor.menus.file.RecentFilesMenu;
import pixelitor.menus.file.ScreenCaptureAction;
import pixelitor.menus.help.AboutDialog;
import pixelitor.menus.help.UpdatesCheck;
import pixelitor.menus.view.ZoomMenu;
import pixelitor.selection.SelectionActions;
import pixelitor.tools.brushes.CopyBrush;
import pixelitor.utils.*;
import pixelitor.utils.debug.Debug;
import pixelitor.utils.test.RandomGUITest;
import pixelitor.utils.test.SplashImageCreator;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ResourceBundle;

import static pixelitor.Views.CLOSE_ACTIVE_ACTION;
import static pixelitor.Views.CLOSE_ALL_ACTION;
import static pixelitor.Views.addActivationListener;
import static pixelitor.Views.addNew;
import static pixelitor.Views.repaintActive;
import static pixelitor.colors.FillType.BACKGROUND;
import static pixelitor.colors.FillType.FOREGROUND;
import static pixelitor.colors.FillType.TRANSPARENT;
import static pixelitor.compactions.FlipDirection.HORIZONTAL;
import static pixelitor.compactions.FlipDirection.VERTICAL;
import static pixelitor.compactions.QuadrantAngle.ANGLE_180;
import static pixelitor.compactions.QuadrantAngle.ANGLE_270;
import static pixelitor.compactions.QuadrantAngle.ANGLE_90;
import static pixelitor.gui.ImageArea.Mode.FRAMES;
import static pixelitor.gui.utils.RestrictedLayerAction.LayerRestriction.HAS_LAYER_MASK;
import static pixelitor.gui.utils.RestrictedLayerAction.LayerRestriction.LayerClassRestriction;
import static pixelitor.gui.utils.RestrictedLayerAction.LayerRestriction.NO_LAYER_MASK;
import static pixelitor.layers.LayerMaskAddType.FROM_LAYER;
import static pixelitor.layers.LayerMaskAddType.FROM_TRANSPARENCY;
import static pixelitor.layers.LayerMaskAddType.HIDE_ALL;
import static pixelitor.layers.LayerMaskAddType.REVEAL_ALL;
import static pixelitor.layers.LayerMaskAddType.REVEAL_SELECTION;
import static pixelitor.layers.LayerMoveAction.LAYER_TO_BOTTOM;
import static pixelitor.layers.LayerMoveAction.LAYER_TO_TOP;
import static pixelitor.layers.LayerMoveAction.LOWER_LAYER_SELECTION;
import static pixelitor.layers.LayerMoveAction.MOVE_LAYER_DOWN;
import static pixelitor.layers.LayerMoveAction.MOVE_LAYER_UP;
import static pixelitor.layers.LayerMoveAction.RAISE_LAYER_SELECTION;
import static pixelitor.utils.Keys.*;

/**
 * The main menu bar of the app window.
 */
public class MenuBar extends JMenuBar {
    public MenuBar(PixelitorWindow pw) {
        ResourceBundle i18n = Texts.getResources();

        add(createFileMenu(pw, i18n));
        add(createEditMenu(i18n));
        add(createLayerMenu(i18n));
        add(createSelectMenu(i18n));
        add(createImageMenu(i18n));
        add(createColorMenu());
        add(createFilterMenu(i18n));
        add(createViewMenu(pw, i18n));

        if (AppMode.isDevelopment()) {
            add(createDevelopMenu(pw));
        }

        add(createHelpMenu(pw, i18n));

        Filters.finishedRegistering();
    }

    private static JMenu createFileMenu(PixelitorWindow pw, ResourceBundle i18n) {
        // TODO localize the mnemonic
        PMenu fileMenu = new PMenu(i18n.getString("file"), 'F');

        // new image
        fileMenu.add(NewImage.getAction(), CTRL_N);

        fileMenu.add(new TaskAction(i18n.getString("open") + "...",
            FileChoosers::openAsync), CTRL_O);

        fileMenu.add(RecentFilesMenu.INSTANCE);

        fileMenu.addSeparator();

        fileMenu.add(new ViewEnabledAction(i18n.getString("save"),
            comp -> FileIO.save(comp, false)), CTRL_S);

        fileMenu.add(new ViewEnabledAction(i18n.getString("save_as") + "...",
            comp -> FileIO.save(comp, true)), CTRL_SHIFT_S);

        String exportOptimizedText = i18n.getString("export_optimized_jpeg");
        fileMenu.add(new ViewEnabledAction(
            exportOptimizedText + "...",
            comp -> OptimizedJpegExportPanel.showInDialog(comp, exportOptimizedText)));

        fileMenu.add(createImageMagickSubmenu(i18n));

        fileMenu.addSeparator();

        fileMenu.add(new ViewEnabledAction(
            i18n.getString("export_layer_animation") + "...",
            LayerAnimation::showExportDialog));

        fileMenu.add(new DrawableAction(i18n.getString("export_tweening_animation")) {
            @Override
            protected void process(Drawable dr) {
                new TweenWizard(dr).start(pw);
            }
        });

        fileMenu.addSeparator();

        // reload
        fileMenu.add(new ViewEnabledAction(
            i18n.getString("reload"),
            comp -> comp.getView().reloadCompAsync()), F12);

        String showMetaDataName = i18n.getString("show_metadata");
        fileMenu.add(new ViewEnabledAction(
            showMetaDataName + "...",
            MetaDataPanel::showInDialog));

        fileMenu.addSeparator();

        // close
        fileMenu.add(CLOSE_ACTIVE_ACTION, CTRL_W);

        // close all
        fileMenu.add(CLOSE_ALL_ACTION, CTRL_ALT_W);

        fileMenu.addSeparator();

        fileMenu.add(new PrintAction(i18n), CTRL_P);

        fileMenu.add(createAutomateSubmenu(pw, i18n));

        if (!JVM.isMac) {
            fileMenu.add(new ScreenCaptureAction());
        }

        fileMenu.addSeparator();

        // exit
        String exitText = JVM.isMac ?
            i18n.getString("exit_mac") : i18n.getString("exit");
        fileMenu.add(new TaskAction(exitText, () -> Pixelitor.exitApp(pw)));

        return fileMenu;
    }

    private static JMenu createImageMagickSubmenu(ResourceBundle i18n) {
        PMenu imMenu = new PMenu("ImageMagick");

        imMenu.add(new ViewEnabledAction(i18n.getString("im_export") + "...", ImageMagick::export));
        imMenu.add(new TaskAction(i18n.getString("im_import") + "...", ImageMagick::importComposition));

        return imMenu;
    }

    private static JMenu createAutomateSubmenu(PixelitorWindow pw, ResourceBundle i18n) {
        PMenu automateMenu = new PMenu(i18n.getString("automate"));

        automateMenu.add(new DrawableAction(i18n.getString("auto_paint")) {
            @Override
            protected void process(Drawable dr) {
                AutoPaint.showDialog(dr);
            }
        });

        String batchFilterText = i18n.getString("batch_filter");
        automateMenu.add(new DrawableAction(batchFilterText) {
            @Override
            protected void process(Drawable dr) {
                new BatchFilterWizard(dr, batchFilterText).start(pw);
            }
        });

        String batchResizeText = i18n.getString("batch_resize");
        automateMenu.add(new TaskAction(
            batchResizeText + "...",
            () -> BatchResize.showDialog(batchResizeText)));

        automateMenu.add(new ViewEnabledAction(
            i18n.getString("export_layers_to_png") + "...",
            FileIO::exportLayersToPNGAsync));

        return automateMenu;
    }

    private static JMenu createEditMenu(ResourceBundle i18n) {
        String editMenuText = i18n.getString("edit");
        PMenu editMenu = new PMenu(editMenuText, 'E');

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
        var pasteAsNewImage = new PasteAction(PasteTarget.NEW_IMAGE);
        editMenu.add(pasteAsNewImage, CTRL_V);

        var pasteAsNewLayer = new PasteAction(PasteTarget.NEW_LAYER);
        editMenu.add(pasteAsNewLayer, CTRL_SHIFT_V);

        var pasteAsMask = new PasteAction(PasteTarget.MASK);
        editMenu.add(pasteAsMask, CTRL_ALT_V);

        editMenu.addSeparator();

        // preferences
        String prefsText = i18n.getString("preferences");
        editMenu.add(new TaskAction(prefsText + "...",
            PreferencesPanel::showInDialog));

        return editMenu;
    }

    private static JMenu createLayerMenu(ResourceBundle i18n) {
        var layersMenu = new PMenu(i18n.getString("layer"), 'L');

        layersMenu.add(AddNewLayerAction.INSTANCE);
        layersMenu.add(DeleteActiveLayerAction.INSTANCE, DELETE);
        layersMenu.add(DuplicateLayerAction.INSTANCE, CTRL_J);

        layersMenu.addSeparator();

        // merge down
        var mergeDown = new ViewEnabledAction(GUIText.MERGE_DOWN,
            Composition::mergeActiveLayerDown);
        mergeDown.setToolTip(GUIText.MERGE_DOWN_TT);
        layersMenu.add(mergeDown, CTRL_E);

        // flatten image
        var flattenImage = new ViewEnabledAction(
            i18n.getString("flatten_image"),
            Composition::flattenImage);
        flattenImage.setToolTip(i18n.getString("flatten_image_tt"));
        layersMenu.add(flattenImage);

        // new layer from visible
        var newFromVisible = new ViewEnabledAction(
            i18n.getString("new_from_visible"),
            Composition::addNewLayerFromVisible);
        newFromVisible.setToolTip(i18n.getString("new_from_visible_tt"));
        layersMenu.add(newFromVisible, CTRL_SHIFT_ALT_E);

        layersMenu.add(createLayerStackSubmenu(i18n));
        layersMenu.add(createLayerTransformSubmenu());
        layersMenu.add(createLayerMaskSubmenu(i18n));

        if (Features.enableExperimental) {
            layersMenu.addSeparator();

            layersMenu.add(createAdjustmentLayersSubmenu());
            layersMenu.add(createColorFillLayerSubmenu());
            layersMenu.add(createGradientFillLayerSubmenu());
            layersMenu.add(createLayerGroupsSubmenu());
            layersMenu.add(createShapeLayerSubmenu());
            layersMenu.add(createSmartObjectSubmenu());
        }

        layersMenu.add(createTextLayerSubmenu(i18n));

        return layersMenu;
    }

    private static JMenu createLayerStackSubmenu(ResourceBundle i18n) {
        var sub = new PMenu(i18n.getString("layer_stack"));

        sub.add(MOVE_LAYER_UP, CTRL_PAGE_UP);

        sub.add(MOVE_LAYER_DOWN, CTRL_PAGE_DOWN);

        // layer to top
        var layerToTop = new ViewEnabledAction(LAYER_TO_TOP,
            comp -> comp.getActiveHolder().moveActiveLayerToTop());
        layerToTop.setToolTip(i18n.getString("layer_to_top_tt"));
        sub.add(layerToTop, CTRL_ALT_PAGE_UP);

        // layer_to_bottom
        var layerToBottom = new ViewEnabledAction(LAYER_TO_BOTTOM,
            comp -> comp.getActiveHolder().moveActiveLayerToBottom());
        layerToBottom.setToolTip(i18n.getString("layer_to_bottom_tt"));
        sub.add(layerToBottom, CTRL_ALT_PAGE_DOWN);

        sub.addSeparator();

        // raise layer selection
        var raiseLayerSelection = new ViewEnabledAction(RAISE_LAYER_SELECTION,
            comp -> comp.getActiveHolder().selectLayerAbove());
        raiseLayerSelection.setToolTip(i18n.getString("raise_layer_selection_tt"));
        sub.add(raiseLayerSelection, PAGE_UP);

        var lowerLayerSelection = new ViewEnabledAction(LOWER_LAYER_SELECTION,
            comp -> comp.getActiveHolder().selectLayerBelow());
        lowerLayerSelection.setToolTip(i18n.getString("lower_layer_selection_tt"));
        sub.add(lowerLayerSelection, PAGE_DOWN);

        sub.addSeparator();

        sub.add(new ViewEnabledAction(i18n.getString("isolate"),
            Composition::isolateActiveTopLevelLayer));

        return sub;
    }

    private static JMenu createLayerTransformSubmenu() {
        var sub = new PMenu("Transform");

        // rotate
        sub.add(ANGLE_90.createLayerTransformAction(), "layer_rot_90");
        sub.add(ANGLE_180.createLayerTransformAction(), "layer_rot_180");
        sub.add(ANGLE_270.createLayerTransformAction(), "layer_rot_270");

        sub.addSeparator();

        // flip
        sub.add(HORIZONTAL.createLayerTransformAction(), "layer_flip_hor");
        sub.add(VERTICAL.createLayerTransformAction(), "layer_flip_ver");

        return sub;
    }

    private static JMenu createLayerMaskSubmenu(ResourceBundle i18n) {
        PMenu sub = new PMenu(i18n.getString("layer_mask"));

        // add all-white layer mask (reveal all)
        sub.add(new RestrictedLayerAction(i18n.getString("lm_add_white"), NO_LAYER_MASK) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.addMask(REVEAL_ALL);
            }
        });

        // add all-black layer mask (hide all)
        sub.add(new RestrictedLayerAction(i18n.getString("lm_add_black"), NO_LAYER_MASK) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.addMask(HIDE_ALL);
            }
        });

        // add mask based on the selection
        sub.add(new RestrictedLayerAction(i18n.getString("lm_add_from_sel"), NO_LAYER_MASK) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.addMask(REVEAL_SELECTION);
            }
        });

        // add mask based on the alpha channel
        sub.add(new RestrictedLayerAction(i18n.getString("lm_add_from_transp"), NO_LAYER_MASK) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.addMask(FROM_TRANSPARENCY);
            }
        });

        // add mask from grayscale version of layer
        sub.add(new RestrictedLayerAction(i18n.getString("lm_add_from_layer"), NO_LAYER_MASK) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.addMask(FROM_LAYER);
            }
        });

        // add mask from color range
        sub.add(new ViewEnabledAction(
            MaskFromColorRangePanel.NAME + "...",
            MaskFromColorRangePanel::showInDialog));

        sub.addSeparator();

        // delete layer mask
        sub.add(new RestrictedLayerAction(i18n.getString("lm_delete"), HAS_LAYER_MASK) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.deleteMask(true);
            }
        });

        // apply layer mask
        sub.add(new RestrictedLayerAction(i18n.getString("lm_apply"), HAS_LAYER_MASK) {
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
        MaskViewMode.VIEW_MASK.addToMenuBar(sub);
        MaskViewMode.EDIT_MASK.addToMenuBar(sub);
        MaskViewMode.RUBYLITH.addToMenuBar(sub);

        return sub;
    }

    private static JMenu createTextLayerSubmenu(ResourceBundle i18n) {
        PMenu sub = new PMenu(i18n.getString("text_layer"));

        // add new text layer
        sub.add(new ViewEnabledAction(i18n.getString("tl_new") + "...",
            TextLayer::createNew), T);

        var isTextLayer = new LayerClassRestriction(TextLayer.class, "text layer");

        // edit the active text layer
        sub.add(new RestrictedLayerAction(i18n.getString("tl_edit") + "...", isTextLayer) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.edit();
            }
        }, CTRL_T);

        // rasterize the active text layer
        sub.add(new RestrictedLayerAction(i18n.getString("tl_rasterize"), isTextLayer) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.replaceWithRasterized();
            }
        });

        // create a selection from the text of a text layer
        sub.add(new RestrictedLayerAction(i18n.getString("tl_sel"), isTextLayer) {
            @Override
            public void onActiveLayer(Layer layer) {
                ((TextLayer) layer).createSelectionFromText();
            }
        });

        return sub;
    }

    private static JMenu createLayerGroupsSubmenu() {
        PMenu sub = new PMenu("Layer Groups");

        sub.add(new ViewEnabledAction("New Empty Group",
            comp -> comp.getHolderForNewLayers().addEmptyGroup()), CTRL_G);

        sub.add(new ViewEnabledAction("Convert Visible to Group",
            comp -> comp.getHolderForGrouping().convertVisibleLayersToGroup()), CTRL_SHIFT_G);

        sub.add(new ViewEnabledAction("Ungroup",
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

        sub.add(new ViewEnabledAction("Convert Layer to Smart Object",
            comp -> comp.getActiveLayer().replaceWithSmartObject()));

        sub.add(new ViewEnabledAction("Convert All to Smart Object",
            Composition::replaceWithSmartObject));

        sub.add(new ViewEnabledAction("Convert Visible to Smart Object",
            Composition::convertVisibleLayersToSmartObject), CTRL_SHIFT_L);
        sub.addSeparator();

        var isSmartObject = new LayerClassRestriction(SmartObject.class, "smart object");

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

        sub.add(new ViewEnabledAction("Edit All Nested Contents",
            comp -> comp.forEachNestedSmartObject(SmartObject::edit)), CTRL_ALT_O);

        sub.add(new RestrictedLayerAction("Edit Smart Filter", isSmartObject) {
            @Override
            public void onActiveLayer(Layer layer) {
                ((SmartObject) layer).editSelectedSmartFilter();
            }
        }, CTRL_SHIFT_E);

        sub.add(new ViewEnabledAction("Add Linked...", comp -> {
            File file = FileChoosers.getSupportedOpenFile();
            if (file == null) {
                return; // the user canceled the dialog
            }

            FileIO.loadCompAsync(file)
                .thenAcceptAsync(content ->
                    comp.add(new SmartObject(file, comp, content)), Threads.onEDT)
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

        sub.add(new ViewEnabledAction("New Color Fill Layer...",
            ColorFillLayer::createNew));

        var isColorFillLayer = new LayerClassRestriction(ColorFillLayer.class, "color fill layer");

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

        sub.add(new ViewEnabledAction("New Gradient Fill Layer...",
            GradientFillLayer::createNew), CTRL_ALT_G);

        var isGradientFillLayer = new LayerClassRestriction(GradientFillLayer.class, "gradient fill layer");

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

        sub.add(new ViewEnabledAction("New Shape Layer...",
            ShapesLayer::createNew), CTRL_ALT_S);

        var isShapeLayer = new LayerClassRestriction(ShapesLayer.class, "shape layer");

        sub.add(new RestrictedLayerAction("Rasterize Shape Layer", isShapeLayer) {
            @Override
            public void onActiveLayer(Layer layer) {
                layer.replaceWithRasterized();
            }
        });

        return sub;
    }

    private static JMenu createSelectMenu(ResourceBundle i18n) {
        PMenu selectMenu = new PMenu(i18n.getString("select"), 'S');

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

    private static JMenu createImageMenu(ResourceBundle i18n) {
        PMenu imageMenu = new PMenu(i18n.getString("image"), 'I');

        // selection crop
        imageMenu.add(SelectionActions.getCrop());

        String cropToContentText = i18n.getString("crop_to_content");
        imageMenu.add(new ViewEnabledAction(cropToContentText, Crop::contentCrop));

        imageMenu.addSeparator();

        // resize
        String resizeText = i18n.getString("resize");
        imageMenu.add(new ViewEnabledAction(resizeText + "...",
            comp -> ResizePanel.showInDialog(comp, resizeText)), CTRL_ALT_I);

        String duplicateText = i18n.getString("duplicate");
        imageMenu.add(new ViewEnabledAction(duplicateText,
            comp -> addNew(comp.copy(CopyType.DUPLICATE_COMP, true))));

        if (Features.enableImageMode) {
            imageMenu.add(createModeSubmenu());
        }

        imageMenu.addSeparator();

        imageMenu.add(EnlargeCanvas.createDialogAction(i18n.getString("enlarge_canvas")));

        var fitCanvasToLayers = new ViewEnabledAction(
            i18n.getString("fit_canvas_to_layers"),
            Composition::fitCanvasToLayers);
        fitCanvasToLayers.setToolTip(i18n.getString("fit_canvas_to_layers_tt"));
        imageMenu.add(fitCanvasToLayers);

        imageMenu.add(new ViewEnabledAction(
            i18n.getString("layer_to_canvas_size"),
            Composition::activeLayerToCanvasSize));

        imageMenu.addSeparator();

        // rotate
        imageMenu.add(new Rotate(ANGLE_90), "comp_rot_90");
        imageMenu.add(new Rotate(ANGLE_180), "comp_rot_180");
        imageMenu.add(new Rotate(ANGLE_270), "comp_rot_270");

        imageMenu.addSeparator();

        // flip
        imageMenu.add(new Flip(HORIZONTAL), "comp_flip_hor");
        imageMenu.add(new Flip(VERTICAL), "comp_flip_ver");

        return imageMenu;
    }

    private static JMenu createModeSubmenu() {
        PMenu sub = new PMenu("Mode");

        var radioGroup = new ButtonGroup();
        for (ImageMode mode : ImageMode.values()) {
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
        colorsMenu.addFilter(Equalize.NAME, Equalize::new);

        colorsMenu.add(createExtractChannelsSubmenu());
        colorsMenu.add(createReduceColorsSubmenu());
        colorsMenu.add(createFillSubmenu());

        return colorsMenu;
    }

    private static JMenu createExtractChannelsSubmenu() {
        PMenu sub = new PMenu("Extract Channels");

        sub.addFilter(ExtractChannel.NAME, ExtractChannel::new);

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

    private static JMenu createFilterMenu(ResourceBundle i18n) {
        PMenu filterMenu = new PMenu(i18n.getString("filter"), 'T');

        String findFilterText = i18n.getString("find_filter");
        filterMenu.add(new ViewEnabledAction(findFilterText + "...",
            comp -> {
                FilterAction action = FilterSearchPanel.showInDialog(findFilterText);
                if (action != null) {
                    action.actionPerformed(null);
                }
            }), F3);

        filterMenu.add(RepeatLast.REPEAT_LAST_ACTION, CTRL_F);
        filterMenu.add(RepeatLast.SHOW_LAST_ACTION, CTRL_ALT_F);

        filterMenu.addSeparator();

        filterMenu.add(createArtisticSubmenu(i18n));
        filterMenu.add(createBlurSharpenSubmenu(i18n));
        filterMenu.add(createDisplaceSubmenu(i18n));
        filterMenu.add(createDistortSubmenu(i18n));
        filterMenu.add(createFindEdgesSubmenu(i18n));

        File gmicExe = Utils.findExecutable(AppPreferences.gmicDirName, "gmic");
        if (gmicExe != null) {
            GMICFilter.GMIC_PATH = gmicExe;
            filterMenu.add(createGMICSubmenu());
        }

        filterMenu.add(createLightSubmenu(i18n));
        filterMenu.add(createNoiseSubmenu(i18n));
        filterMenu.add(createOtherSubmenu());
        filterMenu.add(createRenderSubmenu(i18n));
        filterMenu.add(createTransitionsSubmenu());

        return filterMenu;
    }

    private static JMenu createArtisticSubmenu(ResourceBundle i18n) {
        PMenu sub = new PMenu(i18n.getString("artistic"));

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

        sub.add(createHalftoneSubmenu(i18n));

        return sub;
    }

    private static JMenu createHalftoneSubmenu(ResourceBundle i18n) {
        PMenu sub = new PMenu(i18n.getString("halftone"));

        sub.addFilter(JHDotsHalftone.NAME, JHDotsHalftone::new);
        sub.addFilter(JHStripedHalftone.NAME, JHStripedHalftone::new);
        sub.addFilter(JHConcentricHalftone.NAME, JHConcentricHalftone::new);
        sub.addFilter(JHColorHalftone.NAME, JHColorHalftone::new);
        sub.addFilter(JHDither.NAME, JHDither::new);

        return sub;
    }

    private static JMenu createBlurSharpenSubmenu(ResourceBundle i18n) {
        PMenu sub = new PMenu(i18n.getString("blur")
            + "/" + i18n.getString("sharpen"));

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

    private static JMenu createDisplaceSubmenu(ResourceBundle i18n) {
        PMenu sub = new PMenu(i18n.getString("displace"));

        sub.addFilter(DisplacementMap.NAME, DisplacementMap::new);
        sub.addFilter(DrunkVision.NAME, DrunkVision::new);
        sub.addFilter(GridKaleidoscope.NAME, GridKaleidoscope::new);
        sub.addFilter(JHKaleidoscope.NAME, JHKaleidoscope::new);
        sub.addFilter(Mirror.NAME, Mirror::new);
        sub.addFilter(JHOffset.NAME, JHOffset::new);
        sub.addFilter(Slice.NAME, Slice::new);
//        sub.addFilter(SlippingTiles.NAME, SlippingTiles::new);
        sub.addFilter(TileSeamless.NAME, TileSeamless::new);
        sub.addFilter(JHVideoFeedback.NAME, JHVideoFeedback::new);

        return sub;
    }

    private static JMenu createDistortSubmenu(ResourceBundle i18n) {
        PMenu sub = new PMenu(i18n.getString("distort"));

        sub.addFilter(CircleToSquare.NAME, CircleToSquare::new);
        sub.addFilter(JHPerspective.NAME, JHPerspective::new);
        sub.addFilter(JHSwirlPinchBulge.NAME, JHSwirlPinchBulge::new);

        sub.addSeparator();

        sub.addFilter(JHLensOverImage.NAME, JHLensOverImage::new);
        sub.addFilter(Magnify.NAME, Magnify::new);

        sub.addSeparator();

        sub.addFilter(AngularWaves.NAME, AngularWaves::new);
        sub.addFilter(RadialWaves.NAME, RadialWaves::new);
        sub.addFilter(JHTurbulentDistortion.NAME, JHTurbulentDistortion::new);
        sub.addFilter(JHUnderWater.NAME, JHUnderWater::new);
        sub.addFilter(JHWaterRipple.NAME, JHWaterRipple::new);
        sub.addFilter(JHWaves.NAME, JHWaves::new);

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

    private static JMenu createFindEdgesSubmenu(ResourceBundle i18n) {
        PMenu sub = new PMenu(i18n.getString("find_edges"));

        sub.addFilter(Canny.NAME, Canny::new);
        sub.addFilter(JHConvolutionEdge.NAME, JHConvolutionEdge::new);
        sub.addFilter(JHDifferenceOfGaussians.NAME, JHDifferenceOfGaussians::new);

        String laplacianFilterName = "Laplacian";
        sub.addNoGrayForwardingFilter(laplacianFilterName,
            () -> new LaplaceFilter(laplacianFilterName));

        return sub;
    }

    private static JMenu createGMICSubmenu() {
        PMenu sub = new PMenu("G'MIC");

        sub.add(createGMICArtistictSubmenu());
        sub.add(createGMICBlurSharpenSubmenu());
        
        sub.addFilter(GMICCommand.NAME, GMICCommand::new);
        sub.addFilter(LightGlow.NAME, LightGlow::new);
        sub.addFilter(LocalNormalization.NAME, LocalNormalization::new);
        sub.addFilter(Stroke.NAME, Stroke::new);
        sub.addFilter(Vibrance.NAME, Vibrance::new);

        return sub;
    }

    private static JMenu createGMICArtistictSubmenu() {
        PMenu sub = new PMenu("Artistic");

        sub.addFilter(Bokeh.NAME, Bokeh::new);
        sub.addFilter(BoxFitting.NAME, BoxFitting::new);
        sub.addFilter(Brushify.NAME, Brushify::new);
        sub.addFilter(Cubism.NAME, Cubism::new);
        sub.addFilter(HuffmanGlitches.NAME, HuffmanGlitches::new);
        sub.addFilter(Random3DObjects.NAME, Random3DObjects::new);
        sub.addFilter(Rodilius.NAME, Rodilius::new);
        sub.addFilter(GMICVoronoi.NAME, GMICVoronoi::new);

        return sub;
    }

    private static JMenu createGMICBlurSharpenSubmenu() {
        PMenu sub = new PMenu("Blur/Sharpen");

        sub.addFilter(AnisothropicSmoothing.NAME, AnisothropicSmoothing::new);
        sub.addFilter(BilateralSmoothing.NAME, BilateralSmoothing::new);
//        sub.addFilter(KuwaharaSmoothing.NAME, KuwaharaSmoothing::new);

        return sub;
    }

    private static JMenu createLightSubmenu(ResourceBundle i18n) {
        PMenu sub = new PMenu(i18n.getString("light"));

        sub.addFilter(BumpMap.NAME, BumpMap::new);
        sub.addFilter(Flashlight.NAME, Flashlight::new);
        sub.addFilter(JHGlint.NAME, JHGlint::new);
        sub.addFilter(JHGlow.NAME, JHGlow::new);
        sub.addFilter(JHRays.NAME, JHRays::new);
        sub.addFilter(JHSparkle.NAME, JHSparkle::new);

        return sub;
    }

    private static JMenu createNoiseSubmenu(ResourceBundle i18n) {
        PMenu sub = new PMenu(i18n.getString("noise"));

        sub.addFilter(Kuwahara.NAME, Kuwahara::new);

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

        sub.addFilter(CommandLineFilter.NAME, CommandLineFilter::new);
        sub.addFilter(JHDropShadow.NAME, JHDropShadow::new);
        sub.addFilter(Morphology.NAME, Morphology::new);
        sub.addFilter(RandomFilter.NAME, RandomFilter::new);

        // the text as filter is still useful for batch operations
        sub.addFilter(TextFilter.NAME, TextFilter::new);

        sub.addFilter(TransformLayer.NAME, TransformLayer::new);

        sub.addSeparator();

        sub.add(Convolve.createFilterAction(3));
        sub.add(Convolve.createFilterAction(5));

        sub.addSeparator();

        sub.addFilter(ChannelToTransparency.NAME, ChannelToTransparency::new);
        sub.addFilterWithoutGUI(JHInvertTransparency.NAME, JHInvertTransparency::new);

        return sub;
    }

    private static JMenu createRenderSubmenu(ResourceBundle i18n) {
        PMenu sub = new PMenu(i18n.getString("render"));

        sub.addFilter(Clouds.NAME, Clouds::new);
        sub.addFilter(JHPlasma.NAME, JHPlasma::new);
        sub.addFilter(OrganicNoise.NAME, OrganicNoise::new);
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

        sub.add(createRenderCurvesSubmenu());
        sub.add(createRenderFractalsSubmenu(i18n));
        sub.add(createRenderGeometrySubmenu());

        return sub;
    }

    private static JMenu createRenderCurvesSubmenu() {
        PMenu sub = new PMenu("Curves");

        sub.addFilter(CircleWeave.NAME, CircleWeave::new);
        sub.addFilter(FlowerOfLife.NAME, FlowerOfLife::new);
        sub.addFilter(Grid.NAME, Grid::new);
        sub.addFilter(Lissajous.NAME, Lissajous::new);
        sub.addFilter(LSystems.NAME, LSystems::new);
        sub.addFilter(SpiderWeb.NAME, SpiderWeb::new);
        sub.addFilter(Spiral.NAME, Spiral::new);
        sub.addFilter(Spirograph.NAME, Spirograph::new);

        return sub;
    }

    private static JMenu createRenderFractalsSubmenu(ResourceBundle i18n) {
        PMenu sub = new PMenu(i18n.getString("fractals"));

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
        sub.addFilter(Cubes.NAME, Cubes::new);
        sub.addFilter(Penrose.NAME, Penrose::new);
        sub.addFilter(Rose.NAME, Rose::new);
        sub.addFilter(Starburst.NAME, Starburst::new);
        sub.addFilter(Stripes.NAME, Stripes::new);
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

    private static JMenu createViewMenu(PixelitorWindow pw, ResourceBundle i18n) {
        PMenu viewMenu = new PMenu(i18n.getString("view"), 'V');

        viewMenu.add(ZoomMenu.INSTANCE);

        viewMenu.addSeparator();

        // show history
        String showHistoryText = i18n.getString("show_history");
        viewMenu.add(new ViewEnabledAction(showHistoryText + "...",
            comp -> History.showHistoryDialog()));

        // show navigator
        String showNavigatorText = i18n.getString("show_navigator");
        viewMenu.add(new ViewEnabledAction(showNavigatorText + "...",
            comp -> Navigator.showInDialog(comp.getView())));

        viewMenu.addSeparator();

        viewMenu.add(createColorVariationsSubmenu(pw));

        // color palette
        String colorPaletteText = i18n.getString("color_palette");
        viewMenu.add(new TaskAction(colorPaletteText + "...", () ->
            PalettePanel.showDialog(pw, new FullPalette(colorPaletteText),
                ColorSwatchClickHandler.STANDARD)));

        // static palette
        viewMenu.add(new TaskAction("Static Palette", () ->
            PalettePanel.showStaticPaletteDialog(pw, "Static Palette")));

        viewMenu.addSeparator();

        WorkSpace workSpace = pw.getWorkSpace();
        viewMenu.add(workSpace.getStatusBarAction());
        viewMenu.add(workSpace.getHistogramsAction(), F6);
        viewMenu.add(workSpace.getLayersAction(), F7);
        viewMenu.add(workSpace.getToolsAction());
        viewMenu.add(workSpace.getAllAction(), F8);

        // reset workspace
        viewMenu.add(new TaskAction(i18n.getString("reset_ws"), pw::resetDefaultWorkspace));

        // show pixel grid
        var showPixelGridMI = new OpenViewEnabledCheckBoxMenuItem(i18n.getString("show_pixel_grid"));
        showPixelGridMI.addActionListener(e ->
            View.setPixelGridVisible(showPixelGridMI.getState()));
        viewMenu.add(showPixelGridMI);

        viewMenu.addSeparator();

        // add horizontal guide
        String addHorGuideText = i18n.getString("add_hor_guide");
        viewMenu.add(new ViewEnabledAction(addHorGuideText + "...",
            comp -> AddSingleGuidePanel.showDialog(comp.getView(), true, addHorGuideText)));

        // add vertical guide
        String addVerGuideText = i18n.getString("add_ver_guide");
        viewMenu.add(new ViewEnabledAction(addVerGuideText + "...",
            comp -> AddSingleGuidePanel.showDialog(comp.getView(), false, addVerGuideText)));

        // add grid guides
        String addGridGuidesText = i18n.getString("add_grid_guides");
        viewMenu.add(new ViewEnabledAction(addGridGuidesText + "...",
            comp -> AddGridGuidesPanel.showAddGridDialog(comp.getView(), addGridGuidesText)));

        // clear guides
        viewMenu.add(new ViewEnabledAction(i18n.getString("clear_guides"),
            Composition::clearGuides));

        viewMenu.addSeparator();

        viewMenu.add(createArrangeWindowsSubmenu());

        return viewMenu;
    }

    private static JMenu createColorVariationsSubmenu(PixelitorWindow pw) {
        PMenu variations = new PMenu("Color Variations");
        variations.add(new TaskAction("Foreground...", () ->
            PalettePanel.showVariationsDialog(pw, true)));
        variations.add(new TaskAction(
            "HSB Mix Foreground with Background...", () ->
            PalettePanel.showHSBMixDialog(pw, true)));
        variations.add(new TaskAction(
            "RGB Mix Foreground with Background...", () ->
            PalettePanel.showRGBMixDialog(pw, true)));

        variations.addSeparator();

        variations.add(new TaskAction("Background...", () ->
            PalettePanel.showVariationsDialog(pw, false)));
        variations.add(new TaskAction(
            "HSB Mix Background with Foreground...", () ->
            PalettePanel.showHSBMixDialog(pw, false)));
        variations.add(new TaskAction(
            "RGB Mix Background with Foreground...", () ->
            PalettePanel.showRGBMixDialog(pw, false)));
        return variations;
    }

    private static JMenu createArrangeWindowsSubmenu() {
        PMenu sub = new PMenu("Arrange Windows");

        var cascadeAction = new TaskAction("Cascade", ImageArea::cascadeWindows);
        cascadeAction.setEnabled(ImageArea.isActiveMode(FRAMES));
        sub.add(cascadeAction);

        var tileAction = new TaskAction("Tile", ImageArea::tileWindows);
        tileAction.setEnabled(ImageArea.isActiveMode(FRAMES));
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

        var isSmartObject = new LayerClassRestriction(SmartObject.class, "smart object");
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

        return developMenu;
    }

    private static JMenu createDebugSubmenu() {
        PMenu sub = new PMenu("Debug");

        sub.add(new ViewEnabledAction("Run comp.checkInvariants()",
            Composition::checkInvariants));

        sub.add(new TaskAction("Copy Internal State to Clipboard",
            Debug::copyInternalState), CTRL_ALT_D);

        sub.add(new ViewEnabledAction("Debug Active Composite Image",
            comp -> Debug.debugImage(comp.getCompositeImage(), "Composite of " + comp.getDebugName())));

        sub.add(new DrawableAction("Debug ImageLayer Images") {
            @Override
            protected void process(Drawable dr) {
                dr.debugImages();
            }
        });

        sub.add(new ViewEnabledAction("Enable Mouse Debugging",
            comp -> GlobalEvents.enableMouseEventDebugging(false)));

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

        sub.add(new TaskAction("Debug Copy Brush",
            () -> CopyBrush.setDebugBrushImage(true)));

        sub.addFilter(PorterDuff.NAME, PorterDuff::new);

        sub.add(new TaskAction("Debug All Comp Names",
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
        sub.addFilter(PoissonDiskTester.NAME, PoissonDiskTester::new);
        sub.addFilter(KMeans.NAME, KMeans::new);

        return sub;
    }

    private static JMenu createManualSubmenu(PixelitorWindow pw) {
        PMenu sub = new PMenu("Manual");

        sub.add(new TaskAction("repaint() the main window", pw::repaint));

        sub.add(new ViewEnabledAction("repaint() on the active image",
            comp -> repaintActive()));

        sub.add(new TaskAction("revalidate() the main window", () ->
            pw.getContentPane().revalidate()));

        sub.add(new TaskAction("Themes.updateAllComponents()", Themes::updateAllComponents));

        sub.add(new ViewEnabledAction("update() on the active image",
            Composition::update));

        sub.add(new ViewEnabledAction("update() on the active holder",
            comp -> comp.getActiveHolder().update()));

        sub.add(new ViewEnabledAction("update() on the active layer",
            comp -> comp.getActiveLayer().update()));

        sub.addSeparator();

        sub.add(new TaskAction("Change UI", ImageArea::toggleUI));

        sub.add(new ViewEnabledAction("Export with ImageMagick",
            comp -> {
                BufferedImage img = comp.getCompositeImage();
                ImageMagick.exportImage(img, new File("out.webp"), ExportSettings.DEFAULTS);
            }), CTRL_ALT_E);

        sub.add(new ViewEnabledAction(
            "Reset Active Layer Translation",
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

        sub.add(new ViewEnabledAction("Update Histograms",
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

        sub.add(new TaskAction("Create Splash Image",
            SplashImageCreator::createSplashComp), CTRL_K);

        sub.add(new TaskAction("Save Many Splash Images...", () ->
            SplashImageCreator.saveSplashImages(64)));

        return sub;
    }

    private static JMenu createTestSubmenu() {
        PMenu sub = new PMenu("Test");

        sub.add(new TaskAction("Create All Filters", Filters::createAllFilters));

        sub.addFilter(ParamTestFilter.NAME, ParamTestFilter::new);

        sub.add(new TaskAction("Random GUI Test", () ->
            RandomGUITest.get().start()), CTRL_R);

        sub.add(new ViewEnabledAction(
            "Save in All Formats...",
            FileIO::saveInAllFormats));

        sub.addSeparator();

        sub.add(new ViewEnabledAction("Add All Smart Filters",
            Debug::addAllSmartFilters));

        sub.add(new TaskAction("Test Filter Constructors",
            Filters::testFilterConstructors));

        sub.add(new TaskAction("Serialize All Filters",
            Debug::serializeAllFilters));
        sub.add(new TaskAction("Deserialize All Filters",
            Debug::deserializeAllFilters));

        return sub;
    }

    private static JMenu createHelpMenu(PixelitorWindow pw, ResourceBundle i18n) {
        PMenu helpMenu = new PMenu(GUIText.HELP, 'H');

        String tipOfTheDayText = i18n.getString("tip_of_the_day");
        helpMenu.add(new TaskAction(tipOfTheDayText, () ->
            TipsOfTheDay.showTips(pw, true)));

        helpMenu.addSeparator();

        if (Features.enableExperimental) {
            helpMenu.add(new TaskAction(GMICFilterCreator.TITLE + "...",
                GMICFilterCreator::showInDialog));
        }

        // report an issue
        helpMenu.add(new OpenInBrowserAction(i18n.getString("report_issue") + "...",
            "https://github.com/lbalazscs/Pixelitor/issues"));

        // internal state
        String internalStateText = i18n.getString("internal_state");
        helpMenu.add(new TaskAction(internalStateText + "...",
            () -> Debug.showInternalState(internalStateText)));

        // check for updates
        helpMenu.add(new TaskAction(i18n.getString("check_updates") + "...",
            UpdatesCheck::checkForUpdates));

        // about
        String aboutText = i18n.getString("about");
        helpMenu.add(new TaskAction(aboutText, () ->
            AboutDialog.showDialog(aboutText)));

        return helpMenu;
    }
}
