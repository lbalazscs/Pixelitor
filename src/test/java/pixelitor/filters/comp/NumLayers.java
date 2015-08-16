package pixelitor.filters.comp;

import pixelitor.CompTester;

enum NumLayers {
    ONE {
        @Override
        void init(CompTester tester) {
            // remove one layer so that we have undo
            tester.removeActiveLayer();
        }
    }, MORE {
        @Override
        void init(CompTester tester) {

        }
    };

    abstract void init(CompTester tester);
}
