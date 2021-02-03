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

package pixelitor.gui.utils;

import pixelitor.OpenImages;
import pixelitor.layers.Layer;
import pixelitor.layers.TextLayer;
import pixelitor.utils.Messages;

import static java.lang.String.format;

/**
 * An action that can run only when the active layer has a specific type
 */
public abstract class RestrictedLayerAction extends OpenImageEnabledAction {
    /**
     * On which layer types is a {@link RestrictedLayerAction} allowed to run
     */
    public enum Condition {
        ALWAYS(null) {
            @Override
            boolean isAllowed(Layer layer) {
                return true;
            }

            @Override
            String getErrorMessage(Layer layer) {
                return null;
            }
        }, HAS_LAYER_MASK("No layer mask") {
            @Override
            boolean isAllowed(Layer layer) {
                return layer.hasMask();
            }

            @Override
            String getErrorMessage(Layer layer) {
                return format("The layer \"%s\" has no layer mask.", layer.getName());
            }
        }, NO_LAYER_MASK("Has layer mask") {
            @Override
            boolean isAllowed(Layer layer) {
                return !layer.hasMask();
            }

            @Override
            String getErrorMessage(Layer layer) {
                return format("The layer \"%s\" already has a layer mask.", layer.getName());
            }
        }, IS_TEXT_LAYER("Not a text layer") {
            @Override
            boolean isAllowed(Layer layer) {
                return layer instanceof TextLayer;
            }

            @Override
            String getErrorMessage(Layer layer) {
                return format("The layer \"%s\" is not a text layer.", layer.getName());
            }
        };

        private final String errorDialogTitle;

        Condition(String errorDialogTitle) {
            this.errorDialogTitle = errorDialogTitle;
        }

        abstract boolean isAllowed(Layer layer);

        abstract String getErrorMessage(Layer layer);

        public void showErrorMessage(Layer layer) {
            Messages.showInfo(errorDialogTitle, getErrorMessage(layer));
        }
    }

    private final Condition layerType;

    protected RestrictedLayerAction(String name, Condition layerType) {
        super(name);
        this.layerType = layerType;
    }

    @Override
    public void onClick() {
        Layer activeLayer = OpenImages.getActiveLayer();
        if (layerType.isAllowed(activeLayer)) {
            onActiveLayer(activeLayer);
        } else {
            layerType.showErrorMessage(activeLayer);
        }
    }

    public abstract void onActiveLayer(Layer layer);
}
