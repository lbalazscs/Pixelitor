/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.assertions;

import org.assertj.core.api.Assertions;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.filters.RandomFilterSource;
import pixelitor.filters.gui.FilterParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.gui.utils.NamedAction;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.TextLayer;
import pixelitor.selection.Selection;
import pixelitor.tools.SelectionTool;
import pixelitor.tools.pen.Path;
import pixelitor.tools.pen.PenTool;
import pixelitor.tools.pen.SubPath;
import pixelitor.tools.transform.TransformBox;
import pixelitor.tools.util.DraggablePoint;

import java.awt.image.BufferedImage;

@SuppressWarnings("ExtendsUtilityClass")
public class PixelitorAssertions extends Assertions {
    public static CompositionAssert assertThat(Composition actual) {
        return new CompositionAssert(actual);
    }

    public static CanvasAssert assertThat(Canvas actual) {
        return new CanvasAssert(actual);
    }

    public static SelectionAssert assertThat(Selection actual) {
        return new SelectionAssert(actual);
    }

    public static RandomFilterSourceAssert assertThat(RandomFilterSource actual) {
        return new RandomFilterSourceAssert(actual);
    }

    public static DraggablePointAssert assertThat(DraggablePoint actual) {
        return new DraggablePointAssert(actual);
    }

    public static SubPathAssert assertThat(SubPath actual) {
        return new SubPathAssert(actual);
    }

    public static PathAssert assertThat(Path actual) {
        return new PathAssert(actual);
    }

    public static PenToolAssert assertThat(PenTool actual) {
        return new PenToolAssert(actual);
    }

    public static SelectionToolAssert assertThat(SelectionTool actual) {
        return new SelectionToolAssert(actual);
    }

    public static NamedActionAssert assertThat(NamedAction actual) {
        return new NamedActionAssert(actual);
    }

    @SuppressWarnings("unchecked")
    public static LayerAssert<?, ?> assertThat(Layer actual) {
        return new LayerAssert<>(actual, LayerAssert.class);
    }

    @SuppressWarnings("unchecked")
    public static ContentLayerAssert<?, ?> assertThat(ContentLayer actual) {
        return new ContentLayerAssert<>(actual, ContentLayerAssert.class);
    }

    public static ImageLayerAssert assertThat(ImageLayer actual) {
        return new ImageLayerAssert(actual);
    }

    public static TextLayerAssert assertThat(TextLayer actual) {
        return new TextLayerAssert(actual);
    }

    public static BufferedImageAssert assertThat(BufferedImage actual) {
        return new BufferedImageAssert(actual);
    }

    @SuppressWarnings("unchecked")
    public static FilterParamAssert<?, ?> assertThat(FilterParam actual) {
        return new FilterParamAssert<>(actual, FilterParamAssert.class);
    }

    public static IntChoiceParamAssert assertThat(IntChoiceParam actual) {
        return new IntChoiceParamAssert(actual);
    }

    public static TransformBoxAssert assertThat(TransformBox actual) {
        return new TransformBoxAssert(actual);
    }
}
