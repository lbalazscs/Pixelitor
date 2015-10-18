package pixelitor;

import pixelitor.history.AddToHistory;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.selection.Selection;
import pixelitor.selection.SelectionInteraction;
import pixelitor.selection.SelectionType;
import pixelitor.testutils.WithTranslation;
import pixelitor.utils.UpdateGUI;
import pixelitor.utils.test.Assertions;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * Wraps a Composition in order to provide a assertions
 * and test helper methods.
 */
public class CompTester {
    private final Composition comp;

    // all coordinates and distances must be even here because of the resize test
    private final Rectangle standardTestSelectionShape = new Rectangle(4, 4, 8, 4);

    public CompTester(Composition comp) {
        this.comp = comp;
    }

    public void checkDirty(boolean expectedValue) {
        assertThat(comp.isDirty()).isEqualTo(expectedValue);
    }

    public void checkSelectionBounds(Rectangle expected) {
        Selection selection = comp.getSelectionOrNull();
        Rectangle shapeBounds = selection.getShapeBounds();
        assertThat(shapeBounds).isEqualTo(expected);
    }

    public void checkLayers(String expected) {
        assertThat(comp.toLayerNamesString()).isEqualTo(expected);
        comp.checkInvariant();
    }

    public void checkActiveLayerTranslation(int tx, int ty) {
        ContentLayer layer = (ContentLayer) comp.getActiveLayer();
        assertEquals("tx", tx, layer.getTranslationX());
        assertEquals("ty", ty, layer.getTranslationY());
    }

    public void checkActiveLayerAndMaskImageSize(int w, int h) {
        ImageLayer layer = (ImageLayer) comp.getActiveLayer();
        BufferedImage image = layer.getImage();
        assertEquals("width", w, image.getWidth());
        assertEquals("height", h, image.getHeight());

        if (layer.hasMask()) {
            BufferedImage maskImage = layer.getMask().getImage();
            assertEquals("mask width", w, maskImage.getWidth());
            assertEquals("mask height", h, maskImage.getHeight());
        }
    }

    public void checkCanvasSize(int width, int height) {
        int canvasWidth = comp.getCanvasWidth();
        int canvasHeight = comp.getCanvasHeight();
        assertThat(canvasWidth).isEqualTo(width);
        assertThat(canvasHeight).isEqualTo(height);
    }

    public void moveLayer(boolean makeDuplicateLayer, int relativeX, int relativeY) {
        comp.startMovement(makeDuplicateLayer);
        comp.moveActiveContentRelative(relativeX, relativeY);
        comp.endMovement();
    }

    public void addRectangleSelection(int x, int y, int width, int height) {
        Rectangle rect = new Rectangle(x, y, width, height);
        addRectangleSelection(rect);
    }

    public void addRectangleSelection(Rectangle rect) {
        comp.startSelection(SelectionType.RECTANGLE, SelectionInteraction.ADD);
        comp.getSelection().get().setShape(rect);
    }

    public void setStandardTestTranslationToAllLayers(WithTranslation translation) {
        int nrLayers = comp.getNrLayers();
        for (int i = 0; i < nrLayers; i++) {
            Layer layer = comp.getLayer(i);
            if (layer instanceof ContentLayer) {
                ContentLayer contentLayer = (ContentLayer) layer;

                // should be used on layers without translation
                int tx = contentLayer.getTranslationX();
                assert tx == 0 : "tx = " + tx + " on " + contentLayer.getName();
                int ty = contentLayer.getTranslationY();
                assert ty == 0 : "ty = " + ty + " on " + contentLayer.getName();

                setStandardTestTranslation(contentLayer, translation);
            }
        }
    }

    public void setStandardTestTranslation(ContentLayer layer, WithTranslation translation) {
        // Composition only allows to move the active layer
        // so if the given layer is not active, we need to activate it temporarily
        Layer activeLayerBefore = comp.getActiveLayer();
        boolean activeLayerChanged = false;
        if (layer != activeLayerBefore) {
            comp.setActiveLayer(layer, AddToHistory.NO);
            activeLayerChanged = true;
        }

        assert Assertions.translationIs(layer, 0, 0);

        translation.moveLayer(this);

        int expectedTX = translation.getExpectedTX();
        int expectedTY = translation.getExpectedTY();
        assert Assertions.translationIs(layer, expectedTX, expectedTY);

        if (activeLayerChanged) {
            comp.setActiveLayer(activeLayerBefore, AddToHistory.NO);
        }
    }

    public void setStandardTestSelection() {
        addRectangleSelection(standardTestSelectionShape);
    }

    public Rectangle getStandardTestSelectionShape() {
        return standardTestSelectionShape;
    }

    public void deleteActiveLayer() {
        comp.deleteLayer(comp.getActiveLayer(), AddToHistory.YES, UpdateGUI.NO);
    }
}
