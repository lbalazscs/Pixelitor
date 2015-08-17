package pixelitor.testutils;

import pixelitor.CompTester;

/**
 * Whether there is a translation present when a test runs
 */
public enum WithTranslation {
    YES {
        @Override
        public void init(CompTester tester) {
            tester.setStandardTestTranslationToAllLayers();
        }
    }, NO {
        @Override
        public void init(CompTester tester) {
            // do nothing
        }
    };

    public abstract void init(CompTester tester);
}
