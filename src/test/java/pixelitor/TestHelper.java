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

package pixelitor;

import org.mockito.MockingDetails;
import pixelitor.colors.FgBgColorSelector;
import pixelitor.colors.FgBgColors;
import pixelitor.filters.Invert;
import pixelitor.filters.painters.TextSettings;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.history.History;
import pixelitor.layers.AdjustmentLayer;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.TextLayer;
import pixelitor.selection.Selection;
import pixelitor.testutils.WithTranslation;
import pixelitor.tools.Alt;
import pixelitor.tools.Ctrl;
import pixelitor.tools.MouseButton;
import pixelitor.tools.PMouseEvent;
import pixelitor.tools.Shift;
import pixelitor.utils.Messages;
import pixelitor.utils.test.Assertions;

import javax.swing.*;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;
import static pixelitor.layers.LayerMaskAddType.REVEAL_ALL;
import static pixelitor.layers.MaskViewMode.NORMAL;

public class TestHelper {
    private static final int TEST_WIDTH = 20;
    private static final int TEST_HEIGHT = 10;
    private static boolean initialized = false;

    // all coordinates and distances must be even here because of the resize test
    private static final Rectangle standardTestSelectionShape = new Rectangle(4, 4, 8, 4);

    static {
        initTesting();
    }

    private TestHelper() {
    }

    /**
     * Initialize some static methods that must be done only once
     */
    public static void initTesting() {
        if (!initialized) {
            setupMockFgBgSelector();
            Messages.setMessageHandler(new TestMessageHandler());
            History.setUndoLevels(10);
            initialized = true;
        }
    }

    private static void setupMockFgBgSelector() {
        FgBgColorSelector fgBgColorSelector = mock(FgBgColorSelector.class);
        when(fgBgColorSelector.getFgColor()).thenReturn(Color.BLACK);
        when(fgBgColorSelector.getBgColor()).thenReturn(Color.WHITE);
        FgBgColors.setSelector(fgBgColorSelector);
    }

    public static ImageLayer createImageLayer(String layerName, Composition comp) {
        BufferedImage image = createImage();
        ImageLayer layer = new ImageLayer(comp, image, layerName, null);
//        comp.addLayerNoGUI(layer);

        return layer;
    }

    public static TextLayer createTextLayer(Composition comp, String name) {
        TextLayer textLayer = new TextLayer(comp, name);
        textLayer.setSettings(TextSettings.createRandomSettings(new Random()));
        return textLayer;
    }

    public static Composition createEmptyComposition() {
        Composition comp = Composition.createEmpty(TEST_WIDTH, TEST_HEIGHT);
        setupAnICFor(comp);

        comp.setName("Test");

        return comp;
    }

    public static Composition createMockComposition() {
        Composition comp = mock(Composition.class);

        Canvas canvas = new Canvas(TEST_WIDTH, TEST_HEIGHT);
        when(comp.getCanvas()).thenReturn(canvas);
        when(comp.getCanvasImBounds()).thenReturn(new Rectangle(0, 0, TEST_WIDTH, TEST_HEIGHT));
        when(comp.getCanvasImWidth()).thenReturn(TEST_WIDTH);
        when(comp.getCanvasImHeight()).thenReturn(TEST_HEIGHT);

        ImageComponent ic = mock(ImageComponent.class);
        when(ic.getComp()).thenReturn(comp);
        when(ic.isMock()).thenReturn(true);
        when(ic.getMaskViewMode()).thenReturn(NORMAL);
        when(comp.getIC()).thenReturn(ic);

        when(comp.getSelection()).thenReturn(null);

        return comp;
    }

    public static Composition create2LayerComposition(boolean addMasks) {
        Composition c = createEmptyComposition();

        ImageLayer layer1 = createImageLayer("layer 1", c);
        ImageLayer layer2 = createImageLayer("layer 2", c);

        c.addLayerNoGUI(layer1);
        c.addLayerNoGUI(layer2);

        if (addMasks) {
            layer1.addMask(REVEAL_ALL);
            layer2.addMask(REVEAL_ALL);
        }

        // TODO it should not be necessary to call
        // separately for both layers
        NORMAL.activate(layer1);
        NORMAL.activate(layer2);

        assert layer2 == c.getActiveLayer();
        assert layer1 == c.getLayer(0);
        assert layer2 == c.getLayer(1);

        c.setDirty(false);

        return c;
    }

    public static BufferedImage createImage() {
        return new BufferedImage(TEST_WIDTH, TEST_HEIGHT, BufferedImage.TYPE_INT_ARGB);
    }

    public static Graphics2D createGraphics() {
        return createImage().createGraphics();
    }

    public static Layer createLayerOfClass(Class layerClass, Composition comp) {
        Layer layer;
        if (layerClass.equals(ImageLayer.class)) {
            layer = new ImageLayer(comp, "layer 1");
        } else if (layerClass.equals(TextLayer.class)) {
            layer = createTextLayer(comp, "layer 1");
        } else if (layerClass.equals(AdjustmentLayer.class)) {
            layer = new AdjustmentLayer(comp, "layer 1", new Invert());
        } else {
            throw new IllegalStateException();
        }
        return layer;
    }

    public static PMouseEvent createEvent(ImageComponent ic, int id, Alt alt, Ctrl ctrl, Shift shift, MouseButton mouseButton, int x, int y) {
        int modifiers = 0;
        modifiers = alt.modify(modifiers);
        modifiers = ctrl.modify(modifiers);
        modifiers = shift.modify(modifiers);
        modifiers = mouseButton.modify(modifiers);
        boolean popupTrigger = false;
        if (mouseButton == MouseButton.RIGHT) {
            popupTrigger = true;
        }
        //noinspection MagicConstant
        MouseEvent e = new MouseEvent(ic,
                id,
                System.currentTimeMillis(),
                modifiers,
                x,
                y,
                1, // click count
                popupTrigger
        );
        return new PMouseEvent(e, ic);
    }

    public static ImageComponent setupAnActiveICFor(Composition comp) {
        ImageComponent ic = setupAnICFor(comp);
        ImageComponents.setActiveIC(ic, false);
        return ic;
    }

    public static ImageComponent setupAnICFor(Composition comp) {
        ImageComponent ic = createICWithoutComp();

        when(ic.activeIsDrawable()).thenAnswer(
                invocation -> comp.activeIsDrawable());

        when(ic.getComp()).thenReturn(comp);

        comp.setIC(ic);

        return ic;
    }

    public static ImageComponent createICWithoutComp() {
        ImageComponent ic = mock(ImageComponent.class);

        when(ic.componentToImageSpace(any())).then(returnsFirstArg());

        // can't just return the argument because this method returns a
        // Rectangle (subclass) from a Rectangle2D (superclass)
        when(ic.imageToComponentSpace(any())).thenAnswer(invocation -> {
            Rectangle2D in = invocation.getArgument(0);
            return new Rectangle((int) in.getX(), (int) in.getY(), (int) in.getWidth(), (int) in.getHeight());
        });

        when(ic.componentXToImageSpace(anyDouble())).then(returnsFirstArg());
        when(ic.componentYToImageSpace(anyDouble())).then(returnsFirstArg());
        when(ic.imageXToComponentSpace(anyDouble())).then(returnsFirstArg());
        when(ic.imageYToComponentSpace(anyDouble())).then(returnsFirstArg());

        Point fakeLocationOnScreen = new Point(0, 0);
        when(ic.getLocationOnScreen()).thenReturn(fakeLocationOnScreen);

        Cursor cursor = Cursor.getDefaultCursor();
        when(ic.getCursor()).thenReturn(cursor);

        JViewport parent = new JViewport();
        when(ic.getParent()).thenReturn(parent);

        when(ic.isMock()).thenReturn(true);
        when(ic.getMaskViewMode()).thenReturn(NORMAL);

        return ic;
    }

    public static void addSelectionRectTo(Composition comp, int x, int y, int width, int height) {
        Rectangle shape = new Rectangle(x, y, width, height);
        MockingDetails mockingDetails = mockingDetails(comp);
        if (mockingDetails.isMock()) {
            Selection selection = new Selection(shape, comp.getIC());
            when(comp.getSelection()).thenReturn(selection);
            when(comp.hasSelection()).thenReturn(true);
        } else {
            comp.createSelectionFromShape(shape);
        }
    }

    public static void moveLayer(Composition comp, boolean makeDuplicateLayer, int relX, int relY) {
        comp.startMovement(makeDuplicateLayer);
        comp.moveActiveContentRelative(relX, relY);
        comp.endMovement();
    }

    private static void addRectangleSelection(Composition comp, Rectangle rect) {
//        comp.startSelection(SelectionType.RECTANGLE, SelectionInteraction.ADD);
        Selection selection = new Selection(rect, comp.getIC());
        comp.setNewSelection(selection);
    }

    public static void setStandardTestSelection(Composition comp) {
        addRectangleSelection(comp, standardTestSelectionShape);
    }

    public static Rectangle getStandardTestSelectionShape() {
        return standardTestSelectionShape;
    }

    public static void checkSelectionBounds(Composition comp, Rectangle expected) {
        Selection selection = comp.getSelection();
        Rectangle shapeBounds = selection.getShapeBounds();
        assertThat(shapeBounds).isEqualTo(expected);
    }

    public static void setStandardTestTranslationToAllLayers(Composition comp, WithTranslation translation) {
        comp.forEachContentLayer(contentLayer -> {
            // should be used on layers without translation
            int tx = contentLayer.getTX();
            assert tx == 0 : "tx = " + tx + " on " + contentLayer.getName();
            int ty = contentLayer.getTY();
            assert ty == 0 : "ty = " + ty + " on " + contentLayer.getName();

            setStandardTestTranslation(comp, contentLayer, translation);
        });
    }

    public static void setStandardTestTranslation(Composition comp, ContentLayer layer, WithTranslation translation) {
        // Composition only allows to move the active layer
        // so if the given layer is not active, we need to activate it temporarily
        Layer activeLayerBefore = comp.getActiveLayer();
        boolean activeLayerChanged = false;
        if (layer != activeLayerBefore) {
            comp.setActiveLayer(layer, false);
            activeLayerChanged = true;
        }

        assert Assertions.translationIs(layer, 0, 0);

        translation.moveLayer(comp);

        int expectedTX = translation.getExpectedTX();
        int expectedTY = translation.getExpectedTY();
        assert Assertions.translationIs(layer, expectedTX, expectedTY);

        if (activeLayerChanged) {
            comp.setActiveLayer(activeLayerBefore, false);
        }
    }
}
