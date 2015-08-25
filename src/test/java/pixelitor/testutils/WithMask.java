package pixelitor.testutils;

import pixelitor.Composition;
import pixelitor.history.AddToHistory;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMaskAddType;

public enum WithMask {
    YES {
        @Override
        public void init(Layer layer) {
            if (!layer.hasMask()) {
                layer.addMask(LayerMaskAddType.REVEAL_ALL);
            }
        }
    }, NO {
        @Override
        public void init(Layer layer) {
            if (layer.hasMask()) {
                layer.deleteMask(AddToHistory.NO, false);
            }
        }
    };

    public abstract void init(Layer layer);

    public void init(Composition comp) {
        int nrLayers = comp.getNrLayers();
        for (int i = 0; i < nrLayers; i++) {
            Layer layer = comp.getLayer(i);
            init(layer);
        }
    }
}
