package pixelitor.testutils;

import pixelitor.CompTester;

/**
 * The number of layers which is present when a test runs
 */
public enum NumLayers {
    ONE(true) {
        @Override
        public void init(CompTester tester) {
            // remove one layer so that we have undo
            tester.removeActiveLayer();
        }
    }, MORE(false) {
        @Override
        public void init(CompTester tester) {

        }
    };

    private boolean canUndo;

    NumLayers(boolean canUndo) {
        this.canUndo = canUndo;
    }

    public abstract void init(CompTester tester);

    public boolean canUndo() {
        return canUndo;
    }
}
