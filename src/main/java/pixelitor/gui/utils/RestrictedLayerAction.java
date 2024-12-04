/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Composition;
import pixelitor.layers.Layer;
import pixelitor.utils.Messages;
import pixelitor.utils.Utils;

import static java.lang.String.format;

/**
 * An action that can run only when the active layer has a specific type.
 */
public abstract class RestrictedLayerAction extends AbstractViewEnabledAction {
    /**
     * On which layer types is a {@link RestrictedLayerAction} allowed to run
     */
    public interface LayerRestriction {
        boolean allows(Layer layer);

        String getErrorMessage(Layer layer);

        String getErrorTitle();

        default void showErrorMessage(Layer layer) {
            Messages.showInfo(getErrorTitle(), getErrorMessage(layer));
        }

        LayerRestriction ALLOW_ALL = new LayerRestriction() {
            @Override
            public boolean allows(Layer layer) {
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

        LayerRestriction HAS_LAYER_MASK = new LayerRestriction() {
            @Override
            public boolean allows(Layer layer) {
                return layer.hasMask();
            }

            @Override
            public String getErrorMessage(Layer layer) {
                return format("The layer \"%s\" has no layer mask.", layer.getName());
            }

            @Override
            public String getErrorTitle() {
                return "No Layer Mask";
            }
        };

        LayerRestriction NO_LAYER_MASK = new LayerRestriction() {
            @Override
            public boolean allows(Layer layer) {
                return !layer.hasMask();
            }

            @Override
            public String getErrorMessage(Layer layer) {
                return format("The layer \"%s\" already has a layer mask.", layer.getName());
            }

            @Override
            public String getErrorTitle() {
                return "Has Layer Mask";
            }
        };

        record LayerClassRestriction(Class<? extends Layer> clazz, String desc) implements LayerRestriction {
            @Override
            public boolean allows(Layer layer) {
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

    private final LayerRestriction restriction;

    protected RestrictedLayerAction(String name, LayerRestriction restriction) {
        super(name);
        this.restriction = restriction;
    }

    @Override
    protected void onClick(Composition comp) {
        Layer activeLayer = comp.getActiveLayer();
        if (restriction.allows(activeLayer)) {
            onActiveLayer(activeLayer);
        } else {
            restriction.showErrorMessage(activeLayer);
        }
    }

    protected abstract void onActiveLayer(Layer layer);
}
