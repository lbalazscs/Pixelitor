package pixelitor.filters.comp;

import pixelitor.CompTester;

enum WithTranslation {
    YES {
        @Override
        void init(CompTester tester) {
            tester.setStandardTestTranslationToAllLayers();
        }
    }, NO {
        @Override
        void init(CompTester tester) {
            // do nothing
        }
    };

    abstract void init(CompTester tester);
}
