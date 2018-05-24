/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.levels;

import pixelitor.filters.gui.FilterParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.PreviewExecutor;

import java.util.ArrayList;
import java.util.List;

import static pixelitor.filters.levels.EditedChannelsType.B;
import static pixelitor.filters.levels.EditedChannelsType.G;
import static pixelitor.filters.levels.EditedChannelsType.GB;
import static pixelitor.filters.levels.EditedChannelsType.R;
import static pixelitor.filters.levels.EditedChannelsType.RB;
import static pixelitor.filters.levels.EditedChannelsType.RG;
import static pixelitor.filters.levels.EditedChannelsType.RGB;

public class LevelsModel {
    private final OneChannelLevelsModel rgbModel;
    private final OneChannelLevelsModel rModel;
    private final OneChannelLevelsModel gModel;
    private final OneChannelLevelsModel bModel;
    private final OneChannelLevelsModel rgModel;
    private final OneChannelLevelsModel gbModel;
    private final OneChannelLevelsModel rbModel;
    private final Levels filter;
    private PreviewExecutor executor;

    /**
     * Contains the sub-models in the order they should appear in
     * the GUI
     */
    private final OneChannelLevelsModel[] subModels;

    public LevelsModel(Levels filter) {
        this.filter = filter;
        this.rgbModel = new OneChannelLevelsModel(RGB, this);
        this.rModel = new OneChannelLevelsModel(R, this);
        this.gModel = new OneChannelLevelsModel(G, this);
        this.bModel = new OneChannelLevelsModel(B, this);
        this.rgModel = new OneChannelLevelsModel(RG, this);
        this.rbModel = new OneChannelLevelsModel(RB, this);
        this.gbModel = new OneChannelLevelsModel(GB, this);

        subModels = new OneChannelLevelsModel[]{
                rgbModel, rModel, gModel, bModel, rgModel, rbModel, gbModel
        };
    }

    public void setExecutor(PreviewExecutor previewExecutor) {
        this.executor = previewExecutor;
    }

    public void adjustmentChanged() {
        GrayScaleLookup rgb = rgbModel.getLookup();

        GrayScaleLookup r = rModel.getLookup();
        GrayScaleLookup g = gModel.getLookup();
        GrayScaleLookup b = bModel.getLookup();

        GrayScaleLookup rg = rgModel.getLookup();
        GrayScaleLookup gb = gbModel.getLookup();
        GrayScaleLookup rb = rbModel.getLookup();

        RGBLookup unifiedLookup = new RGBLookup(rgb, r, g, b, rg, rb, gb);
        filter.setRGBLookup(unifiedLookup);
        executor.runFilterPreview();
    }

    public void resetToDefaultSettings() {
        for (OneChannelLevelsModel model : subModels) {
            model.resetToDefaults();
        }

        adjustmentChanged();
    }

    public OneChannelLevelsModel[] getSubModels() {
        return subModels;
    }

    public ParamSet getParamSet() {
        List<FilterParam> params = new ArrayList<>();
        for (OneChannelLevelsModel subModel : subModels) {
            params.add(subModel.getInputBlack());
            params.add(subModel.getInputWhite());
            params.add(subModel.getOutputBlack());
            params.add(subModel.getOutputWhite());
        }

        return new ParamSet(params);
    }
}
