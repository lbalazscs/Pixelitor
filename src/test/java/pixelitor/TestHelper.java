/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.colors.FgBgColorSelector;
import pixelitor.colors.FgBgColors;
import pixelitor.compactions.Resize;
import pixelitor.filters.Filter;
import pixelitor.filters.Invert;
import pixelitor.filters.painters.TextSettings;
import pixelitor.gui.View;
import pixelitor.history.History;
import pixelitor.history.HistoryChecker;
import pixelitor.layers.*;
import pixelitor.selection.Selection;
import pixelitor.testutils.WithTranslation;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.gui.ToolSettingsPanel;
import pixelitor.tools.gui.ToolSettingsPanelContainer;
import pixelitor.utils.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyDouble;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.colors.Colors.toPackedARGB;
import static pixelitor.layers.LayerMaskAddType.REVEAL_ALL;
import static pixelitor.layers.MaskViewMode.NORMAL;
import static pixelitor.tools.move.MoveMode.MOVE_LAYER_ONLY;

/**
 * Static utility methods for the unit tests.
 */
public class TestHelper {
    // default dimensions for test compositions
    public static final int TEST_WIDTH = 20;
    public static final int TEST_HEIGHT = 10;

    private static final String DEFAULT_LAYER_NAME = "layer 1";

    private static Composition currentComp;
    private static Selection currentSel;

    private static HistoryChecker historyChecker;

    private TestHelper() {
    }

    /**
     * Creates a real (non-mocked) composition with a layer of the given class
     */
    public static Composition createRealComp(String name, Class<? extends Layer> layerClass) {
        return createRealComp(name, layerClass, TEST_WIDTH, TEST_HEIGHT);
    }

    public static Composition createRealComp(String name, Class<? extends Layer> layerClass, int width, int height) {
        Composition comp = createEmptyComp(name, width, height, true);
        Layer layer = createLayer(layerClass, comp);
        comp.addLayerWithoutUI(layer);
        return comp;
    }

    public static Composition createEmptyComp(String name) {
        return createEmptyComp(name, TEST_WIDTH, TEST_HEIGHT, true);
    }

    private static Composition createEmptyComp(String name, int width, int height, boolean addMockView) {
        var comp = Composition.createEmpty(width, height, ImageMode.RGB);
        comp.setName(name);
        comp.createDebugName();

        if (addMockView) {
            setupMockViewFor(comp);
        }

        return comp;
    }

    public static Composition createMockComp(String name) {
        var comp = mock(Composition.class);

        when(comp.getName()).thenReturn(name);
        when(comp.isOpen()).thenReturn(true);
        when(comp.checkInvariants()).thenReturn(true); // for assertions

        var canvas = new Canvas(TEST_WIDTH, TEST_HEIGHT);
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
        // when setSelection() is called, store the argument in the currentSel field
        doAnswer(invocation -> {
            currentSel = invocation.getArgument(0);
            return null;
        }).when(comp).setSelection(any(Selection.class));

        // when getSelection() is called, return the currentSel field
        when(comp.getSelection()).thenAnswer(invocation -> currentSel);
        when(comp.hasSelection()).thenAnswer(invocation -> currentSel != null);

        return comp;
    }

    public static Composition createComp(String name, int numLayers, boolean addMasks) {
        Composition comp = createComp(name, numLayers, addMasks, true);
        assertThat(comp)
            .isNotDirty()
            .invariantsAreOK();
        return comp;
    }

    public static Composition createComp(String name, int numLayers, boolean addMasks, boolean addMockView) {
        var comp = createEmptyComp(name, TEST_WIDTH, TEST_HEIGHT, addMockView);

        for (int i = 0; i < numLayers; i++) {
            var layer = createEmptyImageLayer(comp, "layer " + (i + 1));
            comp.addLayerWithoutUI(layer);
            if (addMasks) {
                layer.addMask(REVEAL_ALL);
                History.undoRedo("Add Layer Mask");
            }
            if (i == numLayers - 1) {
                NORMAL.activate(layer);
                assert layer == comp.getActiveLayer();
            }
            assert layer == comp.getLayer(i);
        }

        assertThat(comp)
            .numLayersIs(numLayers)
            .invariantsAreOK();

        comp.setDirty(false);

        return comp;
    }

    public static Layer createLayer(Class<? extends Layer> layerClass, Composition comp) {
        Layer layer = switch (layerClass.getSimpleName()) {
            case "ImageLayer" -> createEmptyImageLayer(comp, DEFAULT_LAYER_NAME);
            case "TextLayer" -> createTextLayer(comp, DEFAULT_LAYER_NAME);
            case "AdjustmentLayer" -> createAdjustmentLayer(comp, DEFAULT_LAYER_NAME, new Invert());
            case "ColorFillLayer" -> createColorFillLayer(comp, DEFAULT_LAYER_NAME);
            case "GradientFillLayer" -> createGradientFillLayer(comp, DEFAULT_LAYER_NAME);
            case "ShapesLayer" -> createShapesLayer(comp, DEFAULT_LAYER_NAME);
            case "SmartObject" -> createSmartObject(comp, DEFAULT_LAYER_NAME);
            case "SmartFilter" -> createSmartFilter(comp, DEFAULT_LAYER_NAME);
            default -> throw new IllegalStateException("unexpected class " + layerClass.getSimpleName());
        };

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

    private static SmartObject createSmartObject(Composition comp, String name) {
        Composition content = createComp(comp.getName() + " Content", 1, false, true);
        SmartObject so = new SmartObject(comp, content);
        so.setName(name, false);
        so.createUI();
        return so;
    }

    private static ColorFillLayer createColorFillLayer(Composition comp, String name) {
        return new ColorFillLayer(comp, name, Color.WHITE);
    }

    private static GradientFillLayer createGradientFillLayer(Composition comp, String name) {
        return new GradientFillLayer(comp, name);
    }

    private static SmartFilter createSmartFilter(Composition comp, String name) {
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

        // The view should be able to return the *new* composition
        // after a replaceComp call, so it always returns the currentComp
        // field, which is initially set to comp.
        // This trick works only if there's a single comp per test.
        currentComp = comp;

        // this consumer updates currentComp when replaceComp() is called on the mock view
        Consumer<Composition> updateCurrentComp = newComp -> {
            currentComp = newComp;
            currentComp.setView(view);
        };

        doAnswer(invocation -> {
            updateCurrentComp.accept(invocation.getArgument(0));
            return null;
        }).when(view).replaceComp(any(Composition.class));

        doAnswer(invocation -> {
            updateCurrentComp.accept(invocation.getArgument(0));
            return null;
        }).when(view).replaceComp(any(Composition.class), any(MaskViewMode.class), anyBoolean());

        // when getComp() or getCanvas() is called, delegate to the currentComp field
        when(view.getComp()).thenAnswer(invocation -> currentComp);
        when(view.getCanvas()).thenAnswer(invocation -> currentComp.getCanvas());

        comp.setView(view);

        // set it to active only after the comp is set, because
        // the active view should return non-null in view.getComp()
        Views.setActiveView(view, false);

        return view;
    }

    public static View createMockViewWithoutComp() {
        View view = mock(View.class);

        when(view.checkInvariants()).thenCallRealMethod();
        when(view.componentToImageSpace(any(Point2D.class))).then(returnsFirstArg());
        when(view.componentToImageSpace(any(Rectangle2D.class))).then(returnsFirstArg());

        // can't just return the argument because this method returns
        // a Rectangle (a subclass) from a Rectangle2D (a superclass)
        when(view.imageToComponentSpace(any(Rectangle2D.class))).thenAnswer(invocation -> {
            Rectangle2D in = invocation.getArgument(0);
            return new Rectangle((int) in.getX(), (int) in.getY(), (int) in.getWidth(), (int) in.getHeight());
        });

        when(view.componentXToImageSpace(anyDouble())).then(returnsFirstArg());
        when(view.componentYToImageSpace(anyDouble())).then(returnsFirstArg());
        when(view.imageXToComponentSpace(anyDouble())).then(returnsFirstArg());
        when(view.imageYToComponentSpace(anyDouble())).then(returnsFirstArg());
        when(view.getZoomScale()).thenReturn(1.0);
        when(view.getLocationOnScreen()).thenReturn(new Point(0, 0));

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
            comp.setSelection(new Selection(shape, comp.getView()));
        } else {
            comp.createSelectionFrom(shape);
        }
    }

    public static void move(Composition comp, int relX, int relY,
                            boolean makeDuplicateLayer) {
        comp.prepareMovement(MOVE_LAYER_ONLY, makeDuplicateLayer);
        comp.moveActiveContent(MOVE_LAYER_ONLY, relX, relY);
        comp.finalizeMovement(MOVE_LAYER_ONLY);

        History.undoRedo("Move Layer");
    }

    public static void setTranslation(Composition comp,
                                      ContentLayer layer,
                                      WithTranslation translation) {
        // Composition only allows moving the active layer,
        // so if the given layer is not active, activate it temporarily
        Layer activeLayerBefore = comp.getActiveLayer();
        boolean activeLayerChanged = false;
        if (layer != activeLayerBefore) {
            comp.setActiveLayer(layer);
            activeLayerChanged = true;
        }

        // should be used on layers without translation
        assertThat(layer).hasNoTranslation();

        translation.move(comp);

        assertThat(layer).translationIs(translation.getExpectedValue());

        if (activeLayerChanged) {
            comp.setActiveLayer(activeLayerBefore);
        }
    }

    public static Composition resize(Composition comp, int targetWidth, int targetHeight) {
        assert comp.getView() != null;
        return new Resize(targetWidth, targetHeight).process(comp).join();
    }

    /**
     * Asserts that the history contains exactly the given edit names.
     */
    public static void assertHistoryEditsAre(String... values) {
        List<String> edits = History.getEditNames();
        assertThat(edits).containsExactly(values);
    }

    public static void initToolSettings(Tool tool, ResourceBundle resources) throws InvocationTargetException, InterruptedException {
        if (!tool.hasSettingsPanel()) {
            tool.setSettingsPanel(new ToolSettingsPanel());
            SwingUtilities.invokeAndWait(() -> tool.initSettingsPanel(resources));
        }
    }

    private static void setupMockFgBgSelector() {
        var fgBgColorSelector = mock(FgBgColorSelector.class);
        when(fgBgColorSelector.getFgColor()).thenReturn(Color.BLACK);
        when(fgBgColorSelector.getBgColor()).thenReturn(Color.WHITE);
        FgBgColors.setUI(fgBgColorSelector);
    }

    public static BufferedImage create1x1Image(Color c) {
        return create1x1Image(c.getAlpha(), c.getRed(), c.getGreen(), c.getBlue());
    }

    public static BufferedImage create1x1Image(int a, int r, int g, int b) {
        BufferedImage img = ImageUtils.createSysCompatibleImage(1, 1);
        img.setRGB(0, 0, toPackedARGB(a, r, g, b));
        return img;
    }

    public static void setUnitTestingMode() {
        setUnitTestingMode(false);
    }

    /**
     * Configures the application for unit testing.
     */
    public static void setUnitTestingMode(boolean checkHistory) {
        if (checkHistory) {
            historyChecker = new HistoryChecker();
        } else {
            historyChecker = null; // clear previous checker
        }
        History.setChecker(historyChecker);

        if (AppMode.isUnitTesting()) {
            return; // already in unit testing mode
        }
        AppMode.setUnitTestingMode();

        if (Texts.getResources() == null) {
            Texts.init(); // needed for view initialization
        }
        Views.clear();

        Utils.ensureAssertionsEnabled();
        Utils.preloadUnitTestFontNames();

        Language.setActive(Language.ENGLISH);
        Messages.setHandler(new TestMessageHandler());

        // make sure that the active tool is not null
        Tools.setActiveTool(Tools.BRUSH);

        History.setUndoLevels(15);

        Layer.uiFactory = TestLayerUI::new;
        ToolSettingsPanelContainer.setInstance(mock(ToolSettingsPanelContainer.class));
        setupMockFgBgSelector();
    }

    public static void verifyAndClearHistory() {
        historyChecker.verifyAndClear();
        History.clear();
    }

    public static void setMaxUntestedEdits(int max) {
        historyChecker.setMaxUntestedEdits(max);
    }
}
