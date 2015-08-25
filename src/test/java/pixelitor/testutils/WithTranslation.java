package pixelitor.testutils;

import pixelitor.CompTester;

/**
 * Whether there is a translation present when a test runs
 */
public enum WithTranslation {
    NO(0, 0) {
        @Override
        public void init(CompTester tester) {
            // do nothing
        }

        @Override
        public void moveLayer(CompTester compTester) {

        }
    }, YES(-4, -4) {
        @Override
        public void init(CompTester tester) {
            tester.setStandardTestTranslationToAllLayers(this);
        }

        @Override
        public void moveLayer(CompTester compTester) {
            compTester.moveLayer(false, 2, 2);
            compTester.moveLayer(false, -4, -4);
        }
    };

    private final int expectedTX;
    private final int expectedTY;

    WithTranslation(int expectedTX, int expectedTY) {
        this.expectedTX = expectedTX;
        this.expectedTY = expectedTY;
    }

    public abstract void init(CompTester tester);

    public abstract void moveLayer(CompTester compTester);

    public int getExpectedTX() {
        return expectedTX;
    }

    public int getExpectedTY() {
        return expectedTY;
    }
}
