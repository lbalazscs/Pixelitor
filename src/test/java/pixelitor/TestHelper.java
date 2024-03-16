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

package pixelitor;

import org.mockito.stubbing.Answer;
import pixelitor.colors.FgBgColorSelector;
import pixelitor.colors.FgBgColors;
import pixelitor.compactions.Resize;
import pixelitor.filters.Filter;
import pixelitor.filters.Invert;
import pixelitor.filters.painters.TextSettings;
import pixelitor.gui.View;
import pixelitor.history.History;
import pixelitor.layers.*;
import pixelitor.selection.Selection;
import pixelitor.testutils.WithTranslation;
import pixelitor.tools.KeyModifiers;
import pixelitor.tools.MouseButton;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.gui.ToolSettingsPanel;
import pixelitor.tools.gui.ToolSettingsPanelContainer;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static java.awt.event.MouseEvent.MOUSE_DRAGGED;
import static java.awt.event.MouseEvent.MOUSE_MOVED;
import static java.awt.event.MouseEvent.MOUSE_PRESSED;
import static java.awt.event.MouseEvent.MOUSE_RELEASED;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.*;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.layers.LayerMaskAddType.REVEAL_ALL;
import static pixelitor.layers.MaskViewMode.NORMAL;
import static pixelitor.tools.move.MoveMode.MOVE_LAYER_ONLY;

public class TestHelper {
    public static final int TEST_WIDTH = 20;
    public static final int TEST_HEIGHT = 10;
    private static Composition currentComp;
    private static Selection currentSel;

    private TestHelper() {
    }

    /**
     * Creates a real (non-mocked) composition with a layer of the given class
     */
    public static Composition createRealComp(Class<? extends Layer> layerClass) {
        return createRealComp(layerClass, TEST_WIDTH, TEST_HEIGHT);
    }

    public static Composition createRealComp(Class<? extends Layer> layerClass, int width, int height) {
        Composition comp = createEmptyComp(width, height, true);
        Layer layer = createLayerOfClass(layerClass, comp);
        comp.addLayerNoUI(layer);
        return comp;
    }

    public static Composition createEmptyComp() {
        return createEmptyComp(TEST_WIDTH, TEST_HEIGHT, true);
    }

    private static Composition createEmptyComp(int width, int height, boolean addMockView) {
        var comp = Composition.createEmpty(width, height, ImageMode.RGB);
        comp.setName("Test");
        comp.createDebugName();

        if (addMockView) {
            setupMockViewFor(comp);
        }

        return comp;
    }

    public static Composition createMockComp() {
        var comp = mock(Composition.class);
        var canvas = new Canvas(TEST_WIDTH, TEST_HEIGHT);

        when(comp.isOpen()).thenReturn(true);
        when(comp.checkInvariants()).thenReturn(true); // for assertions
        when(comp.getCanvas()).thenReturn(canvas);
        when(comp.getCanvasBounds()).thenReturn(
            new Rectangle(0, 0, TEST_WIDTH, TEST_HEIGHT));
        when(comp.getCanvasWidth()).thenReturn(TEST_WIDTH);
        when(comp.getCanvasHeight()).thenReturn(TEST_HEIGHT);

        View view = createMockViewWithoutComp();
        when(view.getComp()).thenReturn(comp);
        when(view.getCanvas()).thenReturn(canvas);

        when(comp.getView()).thenReturn(view);

        currentSel = null;
        // when setSelectionRef() is called on the mock, then store the received
        // Selection argument in the currentSel field
        doAnswer(invocation -> {
            currentSel = (Selection) invocation.getArguments()[0];
            return null;
        }).when(comp).setSelectionRef(any(Selection.class));

        // when getSelection() is called on the mock, then return the currentSel field
        when(comp.getSelection()).thenAnswer((Answer<Selection>) invocation -> currentSel);
        when(comp.hasSelection()).thenAnswer((Answer<Boolean>) invocation -> currentSel != null);

        return comp;
    }

    public static Composition createComp(int numLayers, boolean addMasks) {
        Composition comp = createComp(numLayers, addMasks, true);
        assert comp.checkInvariants();
        return comp;
    }

    public static Composition createComp(int numLayers, boolean addMasks, boolean addMockView) {
        var comp = createEmptyComp(TEST_WIDTH, TEST_HEIGHT, addMockView);

        for (int i = 0; i < numLayers; i++) {
            var layer = createEmptyImageLayer(comp, "layer " + (i + 1));
            comp.addLayerNoUI(layer);
            if (addMasks) {
                layer.addMask(REVEAL_ALL);
            }
            if (i == numLayers - 1) {
                NORMAL.activate(layer);
                assert layer == comp.getActiveLayer();
            }
            assert layer == comp.getLayer(i);
        }
        assert comp.getNumLayers() == numLayers;
        assert comp.checkInvariants();

        comp.setDirty(false);

        return comp;
    }

    public static Layer createLayerOfClass(Class<? extends Layer> layerClass, Composition comp) {
        Layer layer;
        if (layerClass == ImageLayer.class) {
            layer = createEmptyImageLayer(comp, "layer 1");
        } else if (layerClass == TextLayer.class) {
            layer = createTextLayer(comp, "layer 1");
        } else if (layerClass == AdjustmentLayer.class) {
            layer = createAdjustmentLayer(comp, "layer 1", new Invert());
        } else if (layerClass == ColorFillLayer.class) {
            layer = createColorFillLayer(comp, "layer 1");
        } else if (layerClass == GradientFillLayer.class) {
            layer = createGradientFillLayer(comp, "layer 1");
        } else if (layerClass == ShapesLayer.class) {
            layer = createShapesLayer(comp, "layer 1");
        } else if (layerClass == SmartObject.class) {
            layer = createSmartObject(comp, "layer 1");
        } else if (layerClass == SmartFilter.class) {
            layer = createSmartFilter(comp, "layer 1");
        } else {
            throw new IllegalStateException("unexpected class " + layerClass.getSimpleName());
        }
        if (!layer.hasUI()) {
            layer.createUI();
        }
        return layer;
    }

    private static Layer createShapesLayer(Composition comp, String name) {
        return new ShapesLayer(comp, name);
    }

    public static ImageLayer createEmptyImageLayer(Composition comp, String name) {
        ImageLayer layer = ImageLayer.createEmpty(comp, name);
        layer.createUI();
        return layer;
    }

    public static ImageLayer createImageLayer(Composition comp, BufferedImage img, String name) {
        var layer = new ImageLayer(comp, img, name);
        layer.createUI();
        return layer;
    }

    public static TextLayer createTextLayer(Composition comp, String name) {
        TextSettings settings = new TextSettings();
        settings.randomize();
        var textLayer = new TextLayer(comp, name, settings);

        // ensure that the font is not too big for the tiny test layer
        Font smallFont = settings.getFont().deriveFont(Font.PLAIN, 10.0f);
        settings.setFont(smallFont);
        textLayer.applySettings(settings);

        textLayer.createUI();
        return textLayer;
    }

    public static AdjustmentLayer createAdjustmentLayer(Composition comp, String name, Filter filter) {
        var layer = new AdjustmentLayer(comp, name, filter);
        layer.createUI();
        return layer;
    }

    public static SmartObject createSmartObject(Composition comp, String name) {
        Composition content = createComp(1, false, true);
        SmartObject layer = new SmartObject(comp, content);
        layer.setName(name, false);
        layer.createUI();
        return layer;
    }

    public static ColorFillLayer createColorFillLayer(Composition comp, String name) {
        return new ColorFillLayer(comp, name, Color.WHITE);
    }

    public static GradientFillLayer createGradientFillLayer(Composition comp, String name) {
        return new GradientFillLayer(comp, name);
    }

    public static SmartFilter createSmartFilter(Composition comp, String name) {
        SmartObject so = createSmartObject(comp, "smart object");
        SmartFilter smartFilter = new SmartFilter(new Invert(), comp, so);
        so.addSmartFilter(smartFilter, false, false);
        smartFilter.setName(name, false);
        return smartFilter;
    }

    public static ShapesLayer createEmptyShapesLayer(Composition comp, String name) {
        ShapesLayer layer = new ShapesLayer(comp, name);
        layer.createUI();
        return layer;
    }

    public static BufferedImage createImage() {
        return new BufferedImage(TEST_WIDTH, TEST_HEIGHT, BufferedImage.TYPE_INT_ARGB);
    }

    public static Graphics2D createGraphics() {
        return createImage().createGraphics();
    }

    public static View setupMockViewFor(Composition comp) {
        View view = createMockViewWithoutComp();

//        when(view.getComp()).thenReturn(comp);

        // The view should be able to return the *new* composition
        // after a replaceComp call, therefore it always returns the
        // currentComp field, which initially is set to comp.
        // This trick works only if there's a single comp per test!
        currentComp = comp;

        // when replaceComp() is called on the mock, then store the received
        // Composition argument in the currentComp field
        doAnswer(invocation -> {
            currentComp = (Composition) invocation.getArguments()[0];
            currentComp.setView(view);
            return null;
        }).when(view).replaceComp(any(Composition.class));

        doAnswer(invocation -> {
            currentComp = (Composition) invocation.getArguments()[0];
            currentComp.setView(view);
            return null;
        }).when(view).replaceComp(any(Composition.class), any(MaskViewMode.class), anyBoolean());

        // when getComp() is called on the mock, then return the currentComp field
        when(view.getComp()).thenAnswer((Answer<Composition>) invocation -> currentComp);

        comp.setView(view);

        // set it to active only after the comp is set
        // because the active view should return non-null in view.getComp()
        Views.setActiveView(view, false);

        return view;
    }

    public static View createMockViewWithoutComp() {
        View view = mock(View.class);

        when(view.componentToImageSpace(any(Point2D.class))).then(returnsFirstArg());
        when(view.componentToImageSpace(any(Rectangle2D.class))).then(returnsFirstArg());

//        Mockito.doCallRealMethod().when(view).replaceComp(any(Composition.class));
//        Mockito.doCallRealMethod().when(view).replaceComp(any(Composition.class), any(MaskViewMode.class), anyBoolean());

        // can't just return the argument because this method returns a
        // Rectangle (subclass) from a Rectangle2D (superclass)
        when(view.imageToComponentSpace(any(Rectangle2D.class))).thenAnswer(invocation -> {
            Rectangle2D in = invocation.getArgument(0);
            return new Rectangle(
                (int) in.getX(), (int) in.getY(),
                (int) in.getWidth(), (int) in.getHeight());
        });

        when(view.componentXToImageSpace(anyDouble())).then(returnsFirstArg());
        when(view.componentYToImageSpace(anyDouble())).then(returnsFirstArg());
        when(view.imageXToComponentSpace(anyDouble())).then(returnsFirstArg());
        when(view.imageYToComponentSpace(anyDouble())).then(returnsFirstArg());
        when(view.getScaling()).thenReturn(1.0);

        Point fakeLocationOnScreen = new Point(0, 0);
        when(view.getLocationOnScreen()).thenReturn(fakeLocationOnScreen);

        Cursor cursor = Cursor.getDefaultCursor();
        when(view.getCursor()).thenReturn(cursor);

        JViewport parent = new JViewport();
        when(view.getParent()).thenReturn(parent);

        when(view.isMock()).thenReturn(true);
        when(view.getMaskViewMode()).thenReturn(NORMAL);

        return view;
    }

    public static MockFilter createMockFilter(String name) {
        return new MockFilter(name);
    }

    public static void setSelection(Composition comp, Shape shape) {
        if (mockingDetails(comp).isMock()) {
            Selection selection = new Selection(shape, comp.getView());
            when(comp.getSelection()).thenReturn(selection);
            when(comp.hasSelection()).thenReturn(true);
        } else {
            comp.createSelectionFrom(shape);
        }
    }

    public static void move(Composition comp,
                            boolean makeDuplicateLayer, int relX, int relY) {
        comp.startMovement(MOVE_LAYER_ONLY, makeDuplicateLayer);
//        if(makeDuplicateLayer) {
//            comp.getActiveLayer().setUI(new TestLayerUI());
//        }
        comp.moveActiveContent(MOVE_LAYER_ONLY, relX, relY);
        comp.endMovement(MOVE_LAYER_ONLY);
    }

    public static void setTranslation(Composition comp,
                                      ContentLayer layer,
                                      WithTranslation translation) {
        // Composition only allows to move the active layer
        // so if the given layer is not active, activate it temporarily
        Layer activeLayerBefore = comp.getActiveLayer();
        boolean activeLayerChanged = false;
        if (layer != activeLayerBefore) {
            comp.setActiveLayer(layer);
            activeLayerChanged = true;
        }

        // should be used on layers without translation
        assertThat(layer).translationIs(0, 0);

        translation.move(comp);

        int expectedTX = translation.getExpectedTX();
        int expectedTY = translation.getExpectedTY();
        assertThat(layer).translationIs(expectedTX, expectedTY);

        if (activeLayerChanged) {
            comp.setActiveLayer(activeLayerBefore);
        }
    }

    public static Composition resize(Composition comp, int targetWidth, int targetHeight) {
        assert comp.getView() != null;
        return new Resize(targetWidth, targetHeight)
            .process(comp)
            .join();
    }

    public static PMouseEvent createPEvent(int x, int y, int id,
                                           View view) {
        return createPEvent(x, y, id, KeyModifiers.NONE, MouseButton.LEFT, view);
    }

    public static PMouseEvent createPEvent(int x, int y, int id,
                                           KeyModifiers keys,
                                           MouseButton mouseButton, View view) {
        MouseEvent e = createEvent(x, y, id, keys, mouseButton, view);
        return new PMouseEvent(e, view);
    }

    public static MouseEvent createEvent(int x, int y, int id,
                                         KeyModifiers keys,
                                         MouseButton mouseButton,
                                         View view) {
        int modifiers = 0;
        modifiers = keys.modify(modifiers);
        modifiers = mouseButton.modify(modifiers);
        //noinspection MagicConstant
        return new MouseEvent(view, id, System.currentTimeMillis(),
            modifiers, x, y, 1, mouseButton == MouseButton.RIGHT);
    }

    public static void press(int x, int y, View view) {
        press(x, y, KeyModifiers.NONE, view);
    }

    public static void press(int x, int y,
                             KeyModifiers keys, View view) {
        MouseEvent e = createEvent(x, y, MOUSE_PRESSED,
            keys, MouseButton.LEFT, view);
        Tools.EventDispatcher.mousePressed(e, view);
    }

    public static void drag(int x, int y, View view) {
        drag(x, y, KeyModifiers.NONE, view);
    }

    public static void drag(int x, int y,
                            KeyModifiers keys, View view) {
        MouseEvent e = createEvent(x, y, MOUSE_DRAGGED,
            keys, MouseButton.LEFT, view);
        Tools.EventDispatcher.mouseDragged(e, view);
    }

    public static void release(int x, int y, View view) {
        release(x, y, KeyModifiers.NONE, view);
    }

    public static void release(int x, int y,
                               KeyModifiers keys, View view) {
        MouseEvent e = createEvent(x, y, MOUSE_RELEASED,
            keys, MouseButton.LEFT, view);
        Tools.EventDispatcher.mouseReleased(e, view);
    }

    public static void move(int x, int y, View view) {
        move(x, y, KeyModifiers.NONE, view);
    }

    public static void move(int x, int y,
                            KeyModifiers keys, View view) {
        MouseEvent e = createEvent(x, y, MOUSE_MOVED,
            keys, MouseButton.LEFT, view);
        Tools.EventDispatcher.mouseMoved(e, view);
    }

    public static void assertHistoryEditsAre(String... values) {
        List<String> edits = History.getEditNames();
        assertThat(edits).containsExactly(values);
    }

    public static void initTool(Tool tool) throws InvocationTargetException, InterruptedException {
        if (!tool.hasSettingsPanel()) {
            tool.setSettingsPanel(new ToolSettingsPanel());
            SwingUtilities.invokeAndWait(tool::initSettingsPanel);
        }
    }

    public static void setUnitTestingMode() {
        if (Texts.getResources() == null) {
            Texts.init(); // needed for the views initialization
        }
        Views.reinitialize();

        if (GUIMode.isUnitTesting()) {
            // unit testing mode is already set
            return;
        }
        GUIMode.setUnitTestingMode();

        Utils.ensureAssertionsEnabled();
        Utils.preloadUnitTestFontNames();

        Language.setCurrent(Language.ENGLISH);
        Messages.setHandler(new TestMessageHandler());

        // make sure that the current tool is not null
        Tools.setCurrentTool(Tools.BRUSH);

        History.setUndoLevels(15);

        Layer.uiFactory = TestLayerUI::new;
        ToolSettingsPanelContainer.setInstance(mock(ToolSettingsPanelContainer.class));
        setupMockFgBgSelector();
    }

    private static void setupMockFgBgSelector() {
        var fgBgColorSelector = mock(FgBgColorSelector.class);
        when(fgBgColorSelector.getFgColor()).thenReturn(Color.BLACK);
        when(fgBgColorSelector.getBgColor()).thenReturn(Color.WHITE);
        FgBgColors.setUI(fgBgColorSelector);
    }
}
