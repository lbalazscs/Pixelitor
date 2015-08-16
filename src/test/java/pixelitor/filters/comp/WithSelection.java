package pixelitor.filters.comp;

import pixelitor.CompTester;

import java.awt.Rectangle;

enum WithSelection {
    YES {
        @Override
        void init(CompTester tester) {
            tester.setStandardTestSelection();
            Rectangle selectionShape = tester.getStandardTestSelectionShape();
            tester.checkSelectionBounds(selectionShape);
        }
    }, NO {
        @Override
        void init(CompTester tester) {
            // do nothing
        }
    };

    abstract void init(CompTester tester);
}
