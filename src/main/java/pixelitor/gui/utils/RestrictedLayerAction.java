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
import pixelitor.utils.Messages;
import pixelitor.utils.Utils;

import static java.lang.String.format;

/**
 * An action that can run only when the active layer has a specific type
 */
public abstract class RestrictedLayerAction extends OpenImageEnabledAction {
    /**
     * On which layer types is a {@link RestrictedLayerAction} allowed to run
     */
    public static interface Condition {
        boolean isAllowed(Layer layer);

        String getErrorMessage(Layer layer);

        String getErrorTitle();

        default void showErrorMessage(Layer layer) {
            Messages.showInfo(getErrorTitle(), getErrorMessage(layer));
        }

        public Condition ALWAYS = new Condition() {
            @Override
            public boolean isAllowed(Layer layer) {
                return true;
            }

            @Override
            public String getErrorMessage(Layer layer) {
                return null;
            }

            @Override
            public String getErrorTitle() {
                return null;
            }
        };

        public Condition HAS_LAYER_MASK = new Condition() {
            @Override
            public boolean isAllowed(Layer layer) {
                return layer.hasMask();
            }

            @Override
            public String getErrorMessage(Layer layer) {
                return format("The layer \"%s\" has no layer mask.", layer.getName());
            }

            @Override
            public String getErrorTitle() {
                return "No layer mask";
            }
        };

        public Condition NO_LAYER_MASK = new Condition() {
            @Override
            public boolean isAllowed(Layer layer) {
                return !layer.hasMask();
            }

            @Override
            public String getErrorMessage(Layer layer) {
                return format("The layer \"%s\" already has a layer mask.", layer.getName());
            }

            @Override
            public String getErrorTitle() {
                return "Has layer mask";
            }
        };

        public record ClassCondition(Class<? extends Layer> clazz, String desc) implements Condition {
            @Override
            public boolean isAllowed(Layer layer) {
                return layer.getClass() == clazz;
            }

            @Override
            public String getErrorMessage(Layer layer) {
                return format("<html>The layer <b>%s</b> isn't a %s, it's %s.",
                    layer.getName(), desc, Utils.addArticle(layer.getTypeStringLC()));
            }

            @Override
            public String getErrorTitle() {
                return "Not a " + desc;
            }
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
