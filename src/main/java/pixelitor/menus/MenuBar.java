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
import pixelitor.filters.util.FilterSearchPanel;
import pixelitor.filters.util.Filters;
import pixelitor.gui.*;
import pixelitor.gui.utils.RestrictedLayerAction;
import pixelitor.gui.utils.TaskAction;
import pixelitor.gui.utils.Themes;
import pixelitor.guides.AddGridGuidesPanel;
import pixelitor.guides.AddSingleGuidePanel;
import pixelitor.history.History;
import pixelitor.history.RedoAction;
import pixelitor.history.UndoAction;
import pixelitor.io.*;
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
import static pixelitor.layers.LayerMoveAction.LAYER_TO_BOTTOM;
import static pixelitor.layers.LayerMoveAction.LAYER_TO_TOP;
import static pixelitor.layers.LayerMoveAction.LOWER_LAYER_SELECTION;
import static pixelitor.layers.LayerMoveAction.MOVE_LAYER_DOWN;
import static pixelitor.layers.LayerMoveAction.MOVE_LAYER_UP;
import static pixelitor.layers.LayerMoveAction.RAISE_LAYER_SELECTION;
import static pixelitor.layers.MaskInitMethod.FROM_LAYER;
import static pixelitor.layers.MaskInitMethod.FROM_TRANSPARENCY;
import static pixelitor.layers.MaskInitMethod.HIDE_ALL;
import static pixelitor.layers.MaskInitMethod.REVEAL_ALL;
import static pixelitor.layers.MaskInitMethod.REVEAL_SELECTION;
import static pixelitor.utils.Keys.*;

/**
 * The main menu bar of the app window.
 */
public class MenuBar extends JMenuBar {
    private final PixelitorWindow pw;
    private final ResourceBundle i18n;

    public MenuBar(PixelitorWindow pw) {
        this.pw = pw;
        i18n = Texts.getResources();

        add(createFileMenu());
        add(createEditMenu());
        add(createLayerMenu());
        add(createSelectMenu());
        add(createImageMenu());
        add(createColorMenu());
        add(createFilterMenu());
        add(createViewMenu());

        if (AppMode.isDevelopment()) {
            add(createDevelopMenu());
        }

        add(createHelpMenu());

        Filters.finishedRegistering();
    }

    private JMenu createFileMenu() {
        // TODO localize the mnemonic
        PMenu fileMenu = new PMenu(i18n, "file", 'F');

        // new image
        fileMenu.add(NewImage.getAction(), CTRL_N);

        fileMenu.add(new TaskAction(i18n.getString("open") + "...",
            FileChoosers::openAsync), CTRL_O);

        fileMenu.add(RecentFilesMenu.INSTANCE);

        fileMenu.addSeparator();

        fileMenu.addViewEnabled(i18n, "save",
            comp -> FileIO.save(comp, false), CTRL_S);

        fileMenu.addViewEnabledDialog(i18n, "save_as",
            comp -> FileIO.save(comp, true), CTRL_SHIFT_S);

        String exportOptimizedText = i18n.getString("export_optimized_jpeg");
        fileMenu.addViewEnabled(
            exportOptimizedText + "...",
            comp -> OptimizedJpegExportPanel.showInDialog(comp, exportOptimizedText));

        fileMenu.add(createImageMagickSubmenu());

        fileMenu.addSeparator();

        fileMenu.addViewEnabledDialog(i18n, "export_layer_animation", LayerAnimation::showExportDialog);

        fileMenu.add(new DrawableAction(i18n.getString("export_tweening_animation"),
            dr -> new TweenWizard(dr).start(pw)));

        fileMenu.addSeparator();

        // reload
        fileMenu.addViewEnabled(i18n, "reload",
            comp -> comp.getView().reloadCompAsync(), F12);

        fileMenu.addViewEnabledDialog(i18n, "show_metadata", MetaDataPanel::showInDialog);

        fileMenu.addSeparator();

        // close
        fileMenu.add(CLOSE_ACTIVE_ACTION, CTRL_W);

        // close all
        fileMenu.add(CLOSE_ALL_ACTION, CTRL_ALT_W);

        fileMenu.addSeparator();

        fileMenu.add(new PrintAction(i18n), CTRL_P);

        fileMenu.add(createAutomateSubmenu());

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

    private JMenu createImageMagickSubmenu() {
        PMenu imMenu = new PMenu("ImageMagick");

        imMenu.addViewEnabledDialog(i18n, "im_export", ImageMagick::export);
        imMenu.add(new TaskAction(i18n.getString("im_import") + "...", ImageMagick::importComposition));

        return imMenu;
    }

    private JMenu createAutomateSubmenu() {
        PMenu automateMenu = new PMenu(i18n, "automate");

        automateMenu.add(new DrawableAction(i18n.getString("auto_paint"),
            AutoPaint::showDialog));

        String batchFilterText = i18n.getString("batch_filter");
        automateMenu.add(new DrawableAction(batchFilterText,
            dr -> new BatchFilterWizard(dr, batchFilterText).start(pw)));

        String batchResizeText = i18n.getString("batch_resize");
        automateMenu.add(new TaskAction(
            batchResizeText + "...",
            () -> BatchResize.showDialog(batchResizeText)));

        automateMenu.addViewEnabledDialog(i18n, "export_layers_to_png", FileIO::exportLayersToPNGAsync);

        return automateMenu;
    }

    private JMenu createEditMenu() {
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

    private JMenu createLayerMenu() {
        var layersMenu = new PMenu(i18n.getString("layer"), 'L');

        layersMenu.add(AddNewLayerAction.INSTANCE);
        layersMenu.add(DeleteActiveLayerAction.INSTANCE, DELETE);
        layersMenu.add(DuplicateLayerAction.INSTANCE, CTRL_J);

        layersMenu.addSeparator();

        // merge down
        layersMenu.addViewEnabled(GUIText.MERGE_DOWN, GUIText.MERGE_DOWN_TT,
            Composition::mergeActiveLayerDown, CTRL_E);

        // flatten image
        layersMenu.addViewEnabled(i18n.getString("flatten_image"), i18n.getString("flatten_image_tt"),
            Composition::flattenImage);

        // new layer from visible
        layersMenu.addViewEnabled(i18n.getString("new_from_visible"), i18n.getString("new_from_visible_tt"),
            Composition::addNewLayerFromVisible, CTRL_SHIFT_ALT_E);

        layersMenu.add(createLayerStackSubmenu());
        layersMenu.add(createLayerTransformSubmenu());
        layersMenu.add(createLayerMaskSubmenu());

        if (Features.enableExperimental) {
            layersMenu.addSeparator();

            layersMenu.add(createAdjustmentLayersSubmenu());
            layersMenu.add(createColorFillLayerSubmenu());
            layersMenu.add(createGradientFillLayerSubmenu());
            layersMenu.add(createLayerGroupsSubmenu());
            layersMenu.add(createShapeLayerSubmenu());
            layersMenu.add(createSmartObjectSubmenu());
        }

        layersMenu.add(createTextLayerSubmenu());

        return layersMenu;
    }

    private JMenu createLayerStackSubmenu() {
        var sub = new PMenu(i18n, "layer_stack");

        sub.add(MOVE_LAYER_UP, CTRL_PAGE_UP);

        sub.add(MOVE_LAYER_DOWN, CTRL_PAGE_DOWN);

        // layer to top
        sub.addViewEnabled(LAYER_TO_TOP, i18n.getString("layer_to_top_tt"),
            comp -> comp.getActiveHolder().moveActiveLayerToTop(), CTRL_ALT_PAGE_UP);

        // layer_to_bottom
        sub.addViewEnabled(LAYER_TO_BOTTOM, i18n.getString("layer_to_bottom_tt"),
            comp -> comp.getActiveHolder().moveActiveLayerToBottom(), CTRL_ALT_PAGE_DOWN);

        sub.addSeparator();

        // raise layer selection
        sub.addViewEnabled(RAISE_LAYER_SELECTION, i18n.getString("raise_layer_selection_tt"),
            comp -> comp.getActiveHolder().raiseLayerSelection(), PAGE_UP);

        // lower layer selection
        sub.addViewEnabled(LOWER_LAYER_SELECTION, i18n.getString("lower_layer_selection_tt"),
            comp -> comp.getActiveHolder().lowerLayerSelection(), PAGE_DOWN);

        sub.addSeparator();

        sub.addViewEnabled(i18n.getString("isolate"), Composition::isolateActiveTopLevelLayer);

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

    private JMenu createLayerMaskSubmenu() {
        PMenu sub = new PMenu(i18n, "layer_mask");

        // add all-white layer mask (reveal all)
        sub.add(new RestrictedLayerAction(i18n.getString("lm_add_white"), NO_LAYER_MASK,
            layer -> layer.addMask(REVEAL_ALL)));

        // add all-black layer mask (hide all)
        sub.add(new RestrictedLayerAction(i18n.getString("lm_add_black"), NO_LAYER_MASK,
            layer -> layer.addMask(HIDE_ALL)));

        // add mask based on the selection
        sub.add(new RestrictedLayerAction(i18n.getString("lm_add_from_sel"), NO_LAYER_MASK,
            layer -> layer.addMask(REVEAL_SELECTION)));

        // add mask based on the alpha channel
        sub.add(new RestrictedLayerAction(i18n.getString("lm_add_from_transp"), NO_LAYER_MASK,
            layer -> layer.addMask(FROM_TRANSPARENCY)));

        // add mask from grayscale version of layer
        sub.add(new RestrictedLayerAction(i18n.getString("lm_add_from_layer"), NO_LAYER_MASK,
            layer -> layer.addMask(FROM_LAYER)));

        // add mask from color range
        sub.addViewEnabled(MaskFromColorRangePanel.NAME + "...", MaskFromColorRangePanel::showInDialog);

        sub.addSeparator();

        // delete layer mask
        sub.add(new RestrictedLayerAction(i18n.getString("lm_delete"), HAS_LAYER_MASK,
            layer -> layer.deleteMask(true)));

        // apply layer mask
        sub.add(new RestrictedLayerAction(i18n.getString("lm_apply"), HAS_LAYER_MASK, layer -> {
            if (!(layer instanceof ImageLayer)) {
                Messages.showNotImageLayerError(layer);
                return;
            }

            ((ImageLayer) layer).applyLayerMask(true);

            // not necessary, as the result looks the same, but still
            // useful because bugs would be spotted early
            layer.update();
        }));

        sub.addSeparator();

        MaskViewMode.NORMAL.addToMainMenu(sub);
        MaskViewMode.VIEW_MASK.addToMainMenu(sub);
        MaskViewMode.EDIT_MASK.addToMainMenu(sub);
        MaskViewMode.RUBYLITH.addToMainMenu(sub);

        return sub;
    }

    private JMenu createTextLayerSubmenu() {
        PMenu sub = new PMenu(i18n, "text_layer");

        // add new text layer
        sub.addViewEnabled(i18n.getString("tl_new") + "...", TextLayer::createNew, T);

        var isTextLayer = new LayerClassRestriction(TextLayer.class, "text layer");

        // edit the active text layer
        sub.add(new RestrictedLayerAction(i18n.getString("tl_edit") + "...", isTextLayer,
            Layer::edit), CTRL_T);

        // rasterize the active text layer
        sub.add(new RestrictedLayerAction(i18n.getString("tl_rasterize"), isTextLayer,
            Layer::replaceWithRasterized));

        // create a selection from the text of a text layer
        sub.add(new RestrictedLayerAction(i18n.getString("tl_sel"), isTextLayer,
            layer -> ((TextLayer) layer).createSelectionFromText()));

        return sub;
    }

    private static JMenu createLayerGroupsSubmenu() {
        PMenu sub = new PMenu("Layer Groups");

        sub.addViewEnabled("New Empty Group",
            comp -> comp.getHolderForNewLayers().addEmptyGroup(), CTRL_G);

        sub.addViewEnabled("Convert Visible to Group",
            comp -> comp.getHolderForGrouping().convertVisibleLayersToGroup(), CTRL_SHIFT_G);

        sub.addViewEnabled("Ungroup",
            comp -> comp.getActiveLayer().unGroup(), CTRL_U);

        return sub;
    }

    private static JMenu createAdjustmentLayersSubmenu() {
        PMenu sub = new PMenu("Adjustment Layer");

        for (Action action : AddAdjLayerAction.getActions()) {
            sub.add(action);
        }

        return sub;
    }

    private static JMenu createSmartObjectSubmenu() {
        PMenu sub = new PMenu("Smart Object");

        sub.addViewEnabled("Convert Layer to Smart Object",
            comp -> comp.getActiveLayer().replaceWithSmartObject());

        sub.addViewEnabled("Convert Visible to Smart Object",
            Composition::convertVisibleLayersToSmartObject, CTRL_SHIFT_L);

        sub.addSeparator();

        var isSmartObject = new LayerClassRestriction(SmartObject.class, "smart object");

        sub.add(new RestrictedLayerAction("Rasterize Smart Object", isSmartObject,
            Layer::replaceWithRasterized));

        sub.add(new RestrictedLayerAction("Edit Contents", isSmartObject,
            Layer::edit));

        sub.addViewEnabled("Edit All Nested Contents",
            comp -> comp.forEachNestedSmartObject(SmartObject::edit), CTRL_ALT_O);

        sub.add(new RestrictedLayerAction("Edit Smart Filter", isSmartObject,
            layer -> ((SmartObject) layer).editSelectedSmartFilter()), CTRL_SHIFT_E);

        sub.addViewEnabled("Add Linked...", Composition::addLinkedSmartObject);

        sub.add(new RestrictedLayerAction("Clone", isSmartObject,
            layer -> layer.getComp().shallowDuplicate((SmartObject) layer)));

        return sub;
    }

    private static JMenu createColorFillLayerSubmenu() {
        PMenu sub = new PMenu("Color Fill Layer");

        sub.addViewEnabled("New Color Fill Layer...", ColorFillLayer::createNew);

        var isColorFillLayer = new LayerClassRestriction(ColorFillLayer.class, "color fill layer");

        sub.add(new RestrictedLayerAction("Edit Color Fill Layer...", isColorFillLayer,
            Layer::edit));

        sub.add(new RestrictedLayerAction("Rasterize Color Fill Layer", isColorFillLayer,
            Layer::replaceWithRasterized));

        return sub;
    }

    private static JMenu createGradientFillLayerSubmenu() {
        PMenu sub = new PMenu("Gradient Fill Layer");

        sub.addViewEnabled("New Gradient Fill Layer...", GradientFillLayer::createNew, CTRL_ALT_G);

        var isGradientFillLayer = new LayerClassRestriction(GradientFillLayer.class, "gradient fill layer");

        sub.add(new RestrictedLayerAction("Rasterize Gradient Fill Layer", isGradientFillLayer,
            Layer::replaceWithRasterized));

        return sub;
    }

    private static JMenu createShapeLayerSubmenu() {
        PMenu sub = new PMenu("Shape Layer");

        sub.addViewEnabled("New Shape Layer...", ShapesLayer::createNew, CTRL_ALT_S);

        sub.add(new RestrictedLayerAction("Rasterize Shape Layer",
            new LayerClassRestriction(ShapesLayer.class, "shape layer"),
            Layer::replaceWithRasterized));

        return sub;
    }

    private JMenu createSelectMenu() {
        PMenu selectMenu = new PMenu(i18n, "select", 'S');

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

    private JMenu createImageMenu() {
        PMenu imageMenu = new PMenu(i18n, "image", 'I');

        // selection crop
        imageMenu.add(SelectionActions.getCrop());

        String cropToContentText = i18n.getString("crop_to_content");
        imageMenu.addViewEnabled(cropToContentText, Crop::contentCrop);

        imageMenu.addSeparator();

        // resize
        String resizeText = i18n.getString("resize");
        imageMenu.addViewEnabled(resizeText + "...",
            comp -> ResizePanel.showInDialog(comp, resizeText), CTRL_ALT_I);

        imageMenu.addViewEnabled(i18n, "duplicate",
            comp -> addNew(comp.copy(CopyType.DUPLICATE_COMP, true)));

        if (Features.enableImageMode) {
            imageMenu.add(createModeSubmenu());
        }

        imageMenu.addSeparator();

        imageMenu.add(EnlargeCanvas.createDialogAction(i18n.getString("enlarge_canvas")));

        imageMenu.addViewEnabled(i18n.getString("fit_canvas_to_layers"),
            i18n.getString("fit_canvas_to_layers_tt"),
            Composition::fitCanvasToLayers);

        imageMenu.addViewEnabled(i18n.getString("layer_to_canvas_size"), Composition::activeLayerToCanvasSize);

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

    private JMenu createFilterMenu() {
        PMenu filterMenu = new PMenu(i18n, "filter", 'T');

        String findFilterText = i18n.getString("find_filter");
        filterMenu.addViewEnabled(findFilterText + "...",
            comp -> FilterSearchPanel.startWithDialog(findFilterText), F3);

        filterMenu.add(RepeatLast.REPEAT_LAST_ACTION, CTRL_F);
        filterMenu.add(RepeatLast.SHOW_LAST_ACTION, CTRL_ALT_F);

        filterMenu.addSeparator();

        filterMenu.add(createArtisticSubmenu());
        filterMenu.add(createBlurSharpenSubmenu());
        filterMenu.add(createDisplaceSubmenu());
        filterMenu.add(createDistortSubmenu());
        filterMenu.add(createFindEdgesSubmenu());

        File gmicExe = FileUtils.findExecutable(AppPreferences.gmicDirName, "gmic");
        if (gmicExe != null) {
            GMICFilter.GMIC_PATH = gmicExe;
            filterMenu.add(createGMICSubmenu());
        }

        filterMenu.add(createLightSubmenu());
        filterMenu.add(createNoiseSubmenu());
        filterMenu.add(createOtherSubmenu());
        filterMenu.add(createRenderSubmenu());
        filterMenu.add(createTransitionsSubmenu());

        return filterMenu;
    }

    private JMenu createArtisticSubmenu() {
        PMenu sub = new PMenu(i18n, "artistic");

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

    private JMenu createHalftoneSubmenu() {
        PMenu sub = new PMenu(i18n, "halftone");

        sub.addFilter(JHDotsHalftone.NAME, JHDotsHalftone::new);
        sub.addFilter(JHStripedHalftone.NAME, JHStripedHalftone::new);
        sub.addFilter(JHConcentricHalftone.NAME, JHConcentricHalftone::new);
        sub.addFilter(JHColorHalftone.NAME, JHColorHalftone::new);
        sub.addFilter(JHDither.NAME, JHDither::new);

        return sub;
    }

    private JMenu createBlurSharpenSubmenu() {
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

    private JMenu createDisplaceSubmenu() {
        PMenu sub = new PMenu(i18n, "displace");

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

    private JMenu createDistortSubmenu() {
        PMenu sub = new PMenu(i18n, "distort");

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

    private JMenu createFindEdgesSubmenu() {
        PMenu sub = new PMenu(i18n, "find_edges");

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

        sub.add(createGMICArtisticSubmenu());
        sub.add(createGMICBlurSharpenSubmenu());
        
        sub.addFilter(GMICCommand.NAME, GMICCommand::new);
        sub.addFilter(LightGlow.NAME, LightGlow::new);
        sub.addFilter(LocalNormalization.NAME, LocalNormalization::new);
        sub.addFilter(Stroke.NAME, Stroke::new);
        sub.addFilter(Vibrance.NAME, Vibrance::new);

        return sub;
    }

    private static JMenu createGMICArtisticSubmenu() {
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

    private JMenu createLightSubmenu() {
        PMenu sub = new PMenu(i18n, "light");

        sub.addFilter(BumpMap.NAME, BumpMap::new);
        sub.addFilter(Flashlight.NAME, Flashlight::new);
        sub.addFilter(JHGlint.NAME, JHGlint::new);
        sub.addFilter(JHGlow.NAME, JHGlow::new);
        sub.addFilter(JHRays.NAME, JHRays::new);
        sub.addFilter(JHSparkle.NAME, JHSparkle::new);

        return sub;
    }

    private JMenu createNoiseSubmenu() {
        PMenu sub = new PMenu(i18n, "noise");

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

    private JMenu createRenderSubmenu() {
        PMenu sub = new PMenu(i18n, "render");

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
        sub.add(createRenderFractalsSubmenu());
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

    private JMenu createRenderFractalsSubmenu() {
        PMenu sub = new PMenu(i18n, "fractals");

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

    private JMenu createViewMenu() {
        PMenu viewMenu = new PMenu(i18n, "view", 'V');

        viewMenu.add(ZoomMenu.INSTANCE);

        viewMenu.addSeparator();

        // show history
        String showHistoryText = i18n.getString("show_history");
        viewMenu.addViewEnabled(showHistoryText + "...", comp -> History.showHistoryDialog());

        // show navigator
        viewMenu.addViewEnabledDialog(i18n, "show_navigator",
            comp -> Navigator.showInDialog(comp.getView()));

        viewMenu.addSeparator();

        viewMenu.add(createColorVariationsSubmenu());

        // color palette
        String colorPaletteText = i18n.getString("color_palette");
        viewMenu.add(new TaskAction(colorPaletteText + "...", () ->
            PalettePanel.showDialog(pw, new FullPalette(colorPaletteText),
                ColorSwatchClickHandler.STANDARD)));

        // static palette (TODO unfinished feature)
        viewMenu.add(new TaskAction("Static Palette...", () ->
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
        viewMenu.addViewEnabled(addHorGuideText + "...",
            comp -> AddSingleGuidePanel.showDialog(comp.getView(), true, addHorGuideText));

        // add vertical guide
        String addVerGuideText = i18n.getString("add_ver_guide");
        viewMenu.addViewEnabled(addVerGuideText + "...",
            comp -> AddSingleGuidePanel.showDialog(comp.getView(), false, addVerGuideText));

        // add grid guides
        String addGridGuidesText = i18n.getString("add_grid_guides");
        viewMenu.addViewEnabled(addGridGuidesText + "...",
            comp -> AddGridGuidesPanel.showAddGridDialog(comp.getView(), addGridGuidesText));

        // clear guides
        viewMenu.addViewEnabled(i18n, "clear_guides", Composition::clearGuides);

        viewMenu.addSeparator();

        viewMenu.add(createArrangeWindowsSubmenu());

        return viewMenu;
    }

    private JMenu createColorVariationsSubmenu() {
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

    private JMenu createDevelopMenu() {
        PMenu developMenu = new PMenu("Develop", 'D');

        developMenu.add(createDebugSubmenu());
        developMenu.add(createExperimentalSubmenu());
        developMenu.add(createManualSubmenu());
        developMenu.add(createSplashSubmenu());
        developMenu.add(createTestSubmenu());

        var isSmartObject = new LayerClassRestriction(SmartObject.class, "smart object");

        developMenu.add(new RestrictedLayerAction("Edit 0", isSmartObject,
            layer -> ((SmartObject) layer).getSmartFilter(0).edit()));

        developMenu.add(new RestrictedLayerAction("Edit 1", isSmartObject,
            layer -> ((SmartObject) layer).getSmartFilter(1).edit()));

        developMenu.add(new RestrictedLayerAction("Edit 2", isSmartObject,
            layer -> ((SmartObject) layer).getSmartFilter(2).edit()));
        
        return developMenu;
    }

    private static JMenu createDebugSubmenu() {
        PMenu sub = new PMenu("Debug");

        sub.addViewEnabled("Run comp.checkInvariants()", Composition::checkInvariants);

        sub.add(new TaskAction("Copy Internal State to Clipboard",
            Debug::copyInternalState), CTRL_ALT_D);

        sub.addViewEnabled("Debug Active Composite Image",
            comp -> Debug.debugImage(comp.getCompositeImage(), "Composite of " + comp.getDebugName()));

        sub.add(new DrawableAction("Debug ImageLayer Images",
            Drawable::debugImages));

        sub.addViewEnabled("Enable Mouse Debugging", comp -> GlobalEvents.enableMouseEventDebugging());

        sub.add(new RestrictedLayerAction("Debug Layer Mask", HAS_LAYER_MASK, layer -> {
            ImageLayer imageLayer = (ImageLayer) layer;
            Debug.debugImage(imageLayer.getImage(), "layer image");

            if (imageLayer.hasMask()) {
                LayerMask layerMask = imageLayer.getMask();
                BufferedImage maskImage = layerMask.getImage();
                Debug.debugImage(maskImage, "mask image");

                BufferedImage transparencyImage = layerMask.getTransparencyImage();
                Debug.debugImage(transparencyImage, "transparency image");
            }
        }));
        
        sub.add(new DrawableAction("Debug getCanvasSizedSubImage()",
            dr -> Debug.debugImage(dr.getCanvasSizedSubImage())));
        
        sub.add(new TaskAction("Debug Copy Brush",
            () -> CopyBrush.setDebugBrushImage(true)));

        sub.addFilter(PorterDuff.NAME, PorterDuff::new);

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

    private JMenu createManualSubmenu() {
        PMenu sub = new PMenu("Manual");

        sub.add(new TaskAction("repaint() the main window", pw::repaint));

        sub.addViewEnabled("repaint() on the active image", comp -> repaintActive());

        sub.add(new TaskAction("revalidate() the main window", () ->
            pw.getContentPane().revalidate()));

        sub.add(new TaskAction("Themes.refreshComponentUIs()", Themes::refreshComponentUIs));

        sub.addViewEnabled("update() on the active image", Composition::update);

        sub.addViewEnabled("update() on the active holder",
            comp -> comp.getActiveHolder().update());

        sub.addViewEnabled("update() on the active layer",
            comp -> comp.getActiveLayer().update());

        sub.addSeparator();

        sub.add(new TaskAction("Change UI", ImageArea::toggleUI));

        sub.addViewEnabled("Export with ImageMagick",
            comp -> {
                BufferedImage img = comp.getCompositeImage();
                ImageMagick.exportImage(img, new File("out.webp"), ExportSettings.DEFAULTS);
            }, CTRL_ALT_E);

        sub.addViewEnabled("Reset Active Layer Translation",
            comp -> {
                Layer layer = comp.getActiveLayer();
                if (layer instanceof ContentLayer contentLayer) {
                    contentLayer.setTranslation(0, 0);
                }
                if (layer.hasMask()) {
                    layer.getMask().setTranslation(0, 0);
                }
                comp.update();
            });

        sub.addViewEnabled("Update Histograms", HistogramsPanel::updateFrom);

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

        sub.addViewEnabled("Save in All Formats...", FileIO::saveInAllFormats);

        sub.addSeparator();

        sub.addViewEnabled("Add All Smart Filters", Debug::addAllSmartFilters);

        sub.add(new TaskAction("Test Filter Constructors",
            Filters::testFilterConstructors));

        sub.add(new TaskAction("Serialize All Filters",
            Debug::serializeAllFilters));
        sub.add(new TaskAction("Deserialize All Filters",
            Debug::deserializeAllFilters));

        return sub;
    }

    private JMenu createHelpMenu() {
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
