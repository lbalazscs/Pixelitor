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

import pixelitor.ImageComponent;
import pixelitor.ImageComponents;
import pixelitor.layers.Layer;
import pixelitor.layers.TextLayer;
import pixelitor.utils.Dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;

abstract class MenuAction extends AbstractAction {
    public enum AllowedLayerType {
        ANY(null) {
            @Override
            boolean isAllowed(Layer layer) {
                return true;
            }

            @Override
            public String getErrorMessage(Layer layer) {
                return null;
            }
        }, HAS_LAYER_MASK("No layer mask") {
            @Override
            boolean isAllowed(Layer layer) {
                return layer.hasMask();
            }

            @Override
            public String getErrorMessage(Layer layer) {
                return String.format("The layer \"%s\" has no layer mask.", layer.getName());
            }
        }, IS_TEXT_LAYER("Not text layer") {
            @Override
            boolean isAllowed(Layer layer) {
                return (layer instanceof TextLayer);
            }

            @Override
            public String getErrorMessage(Layer layer) {
                return String.format("The layer \"%s\" is not a text layer.", layer.getName());
            }
        };

        private final String errorTitle;

        AllowedLayerType(String errorTitle) {
            this.errorTitle = errorTitle;
        }

        abstract boolean isAllowed(Layer layer);

        public abstract String getErrorMessage(Layer layer);

        public String getErrorTitle() {
            return errorTitle;
        }
    }

    private final AllowedLayerType layerType;

    public MenuAction(String name) {
        super(name);
        layerType = AllowedLayerType.ANY;
    }

    public MenuAction(String name, AllowedLayerType layerType) {
        super(name);
        this.layerType = layerType;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            if (layerType == AllowedLayerType.ANY) {
                onClick();
            } else {
                ImageComponent ic = ImageComponents.getActiveImageComponent();
                Layer activeLayer = ic.getComp().getActiveLayer();
                if (layerType.isAllowed(activeLayer)) {
                    onClick();
                } else {
                    String errorTitle = layerType.getErrorTitle();
                    String errorMessage = layerType.getErrorMessage(activeLayer);
                    Dialogs.showInfoDialog(errorTitle, errorMessage);
                }
            }
        } catch (Exception ex) {
            Dialogs.showExceptionDialog(ex);
        }
    }

    abstract void onClick();
}
