/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.TextLayer;
import pixelitor.selection.Selection;
import pixelitor.tools.Alt;
import pixelitor.tools.Ctrl;
import pixelitor.tools.Mouse;
import pixelitor.tools.Shift;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;
import static pixelitor.layers.LayerMaskAddType.REVEAL_ALL;
import static pixelitor.layers.MaskViewMode.NORMAL;

public class TestHelper {
    private static final int TEST_WIDTH = 20;
    private static final int TEST_HEIGHT = 10;
    private static final Component eventSource = new JPanel();
    private static boolean initialized = false;

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
        FgBgColors.setGUI(fgBgColorSelector);
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
        when(comp.getCanvasBounds()).thenReturn(new Rectangle(0, 0, TEST_WIDTH, TEST_HEIGHT));
        when(comp.getCanvasWidth()).thenReturn(TEST_WIDTH);
        when(comp.getCanvasHeight()).thenReturn(TEST_HEIGHT);

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

    public static MouseEvent createEvent(int id, Alt alt, Ctrl ctrl, Shift shift, Mouse mouse, int x, int y) {
        int modifiers = 0;
        modifiers = alt.modify(modifiers);
        modifiers = ctrl.modify(modifiers);
        modifiers = shift.modify(modifiers);
        modifiers = mouse.modify(modifiers);
        boolean popupTrigger = false;
        if (mouse == Mouse.RIGHT) {
            popupTrigger = true;
        }
        //noinspection MagicConstant
        return new MouseEvent(eventSource,
                id,
                System.currentTimeMillis(),
                modifiers,
                x,
                y,
                1, // click count
                popupTrigger
        );
    }

    public static ImageComponent setupAnActiveICFor(Composition comp) {
        ImageComponent ic = setupAnICFor(comp);
        ImageComponents.setActiveIC(ic, false);
        return ic;
    }

    public static ImageComponent setupAnICFor(Composition comp) {
        ImageComponent ic = mock(ImageComponent.class);

        when(ic.fromComponentToImageSpace(anyObject())).then(returnsFirstArg());
        when(ic.fromImageToComponentSpace(anyObject())).thenAnswer(invocation -> {
            Rectangle2D in = invocation.getArgumentAt(0, Rectangle2D.class);
            return new Rectangle((int) in.getX(), (int) in.getY(), (int) in.getWidth(), (int) in.getHeight());
        });

        Cursor cursor = Cursor.getDefaultCursor();
        when(ic.getCursor()).thenReturn(cursor);

        when(ic.activeIsImageLayerOrMask()).thenAnswer(
                invocation -> comp.activeIsImageLayerOrMask());

        JViewport parent = new JViewport();
        when(ic.getParent()).thenReturn(parent);

        when(ic.getComp()).thenReturn(comp);
        when(ic.isMock()).thenReturn(true);
        when(ic.getMaskViewMode()).thenReturn(NORMAL);

        comp.setIC(ic);

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
}
